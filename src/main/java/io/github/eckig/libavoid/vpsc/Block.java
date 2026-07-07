/*
 * libavoid - Fast, Incremental, Object-avoiding Line Router
 *
 * Copyright (C) 2004-2011  Monash University
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * See the file LICENSE.LGPL distributed with the library.
 *
 * Licensees holding a valid commercial license may use this file in
 * accordance with the commercial license agreement provided with the
 * library.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * Author(s):  Michael Wybrow
 *
 */

package io.github.eckig.libavoid.vpsc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A Block of variables in the VPSC solver. A block contains one or more
 * variables with the invariant that all constraints inside a block are
 * satisfied by keeping the variables fixed relative to one another.
 * Translated from vpsc.h/vpsc.cpp in libavoid C++.
 */
public class Block {
    public List<Variable> vars;
    public double posn;
    public PositionStats ps;
    public boolean deleted;
    public long timeStamp;

    // Parent container, that holds the blockTimeCtr.
    Blocks blocks;

    // -----------------------------------------------------------------------
    // Cached scratch arrays — reused across all DFS calls on this block.
    // The arrays are sized to the block capacity and grown lazily.
    // All methods that use them must reset entries they wrote before returning.
    // This eliminates per-call array allocation in the hot split_path /
    // compute_dfdv_impl / reset_active_lm / populateSplitBlock paths.
    // -----------------------------------------------------------------------
    private static final int INITIAL_SCRATCH = 8;

    // DFS scratch arrays — allocated lazily on first ensureScratch() call.
    // Blocks that are never used as DFS roots (e.g. freshly-split single-variable
    // blocks that get merged immediately) pay zero allocation cost.
    private Variable[]    scratch_sVar    = null;
    private Variable[]    scratch_sParent = null;
    private Constraint[]  scratch_sCon    = null;
    private boolean[]     scratch_sOut    = null;

    // Visit-order arrays (used by compute_dfdv_impl and split_path)
    private Variable[]    scratch_order       = null;
    private int[]         scratch_parentIdx   = null;
    private Constraint[]  scratch_viaCon      = null;
    private boolean[]     scratch_viaOut      = null;

    // dfdv values (used by compute_dfdv_impl)
    private double[]      scratch_dfdv        = null;

    /** Ensures all primary scratch arrays are at least {@code cap} long. */
    private void ensureScratch(int cap) {
        if (scratch_sVar == null) {
            int newCap = Math.max(cap, INITIAL_SCRATCH);
            scratch_sVar    = new Variable[newCap];
            scratch_sParent = new Variable[newCap];
            scratch_sCon    = new Constraint[newCap];
            scratch_sOut    = new boolean[newCap];
            scratch_order      = new Variable[newCap];
            scratch_parentIdx  = new int[newCap];
            scratch_viaCon     = new Constraint[newCap];
            scratch_viaOut     = new boolean[newCap];
            scratch_dfdv       = new double[newCap];
        } else if (scratch_sVar.length < cap) {
            int newCap = Math.max(cap, scratch_sVar.length * 2);
            scratch_sVar    = new Variable[newCap];
            scratch_sParent = new Variable[newCap];
            scratch_sCon    = new Constraint[newCap];
            scratch_sOut    = new boolean[newCap];
            scratch_order      = new Variable[newCap];
            scratch_parentIdx  = new int[newCap];
            scratch_viaCon     = new Constraint[newCap];
            scratch_viaOut     = new boolean[newCap];
            scratch_dfdv       = new double[newCap];
        }
    }

    public Block(Blocks blocks, Variable v) {
        this.vars = new ArrayList<>();
        this.posn = 0;
        this.deleted = false;
        this.timeStamp = 0;
        this.blocks = blocks;
        this.ps = new PositionStats();
        if (v != null) {
            v.offset = 0;
            addVariable(v);
        }
    }

    public Block(Blocks blocks) {
        this(blocks, (Variable) null);
    }

    /**
     * Constructor with a capacity hint for the vars list.
     * Used by split() to pre-size the ArrayList and avoid resizing during
     * populateSplitBlock (the add/gro bars visible in the profiler flamegraph).
     */
    Block(Blocks blocks, int varsCapacity) {
        this.vars = new ArrayList<>(varsCapacity);
        this.posn = 0;
        this.deleted = false;
        this.timeStamp = 0;
        this.blocks = blocks;
        this.ps = new PositionStats();
    }

    public void addVariable(Variable v) {
        v.block = this;
        vars.add(v);
        if (ps.A2 == 0) ps.scale = v.scale;
        ps.addVariable(v);
        posn = (ps.AD - ps.AB) / ps.A2;
        assert !Double.isNaN(posn);
    }

    public void updateWeightedPosition() {
        ps.AB = ps.AD = ps.A2 = 0;
        for (Variable v : vars) {
            ps.addVariable(v);
        }
        posn = (ps.AD - ps.AB) / ps.A2;
        assert !Double.isNaN(posn);
    }

    public Block merge(Block b, Constraint c) {
        double dist = c.right.offset - c.left.offset - c.gap;
        Block l = c.left.block;
        Block r = c.right.block;
        if (l.vars.size() < r.vars.size()) {
            r.mergeBlock(l, c, dist);
        } else {
            l.mergeBlock(r, c, -dist);
        }
        return b.deleted ? this : b;
    }

    /**
     * Merges b into this block across c.
     * Can be either a right merge or a left merge.
     * @param b block to merge into this
     * @param c constraint being merged
     * @param dist separation required to satisfy c
     */
    public void mergeBlock(Block b, Constraint c, double dist) {
        c.activate();
        for (Variable v : b.vars) {
            v.offset += dist;
            addVariable(v);
        }
        posn = (ps.AD - ps.AB) / ps.A2;
        assert !Double.isNaN(posn);
        b.deleted = true;
    }

    /**
     * Computes the derivative of v and the lagrange multipliers
     * of v's active constraints (as the post-order sum of those below).
     * Does not backtrack over u.
     * Also records the constraint with minimum lagrange multiplier in min_lm[0].
     *
     * Iterative post-order DFS: eliminates recursive Java stack frames to avoid
     * JVM stack overhead and improve JIT inlining in deep constraint graphs.
     * Uses cached scratch arrays (ensureScratch) to avoid per-call allocation.
     */
    double compute_dfdv(Variable v, Variable u, Constraint[] min_lm) {
        return compute_dfdv_impl(v, u, min_lm);
    }

    /**
     * Variant without min_lm tracking (used by findMinLMBetween → split_path path).
     */
    double compute_dfdv(Variable v, Variable u) {
        return compute_dfdv_impl(v, u, null);
    }

    /**
     * Shared iterative post-order DFS implementation.
     * min_lm may be null when min-lm tracking is not required.
     *
     * Uses a plain double[] indexed by a sequential per-Variable scratch index
     * (Variable.dfdvIndex) instead of an IdentityHashMap, eliminating all
     * hashing, boxing, and resize overhead.
     * Scratch arrays are cached on the Block instance to avoid per-call allocation.
     */
    private double compute_dfdv_impl(Variable root, Variable blockedParent, Constraint[] min_lm) {
        int capacity = vars.size();
        ensureScratch(capacity);

        // Use cached arrays
        Variable[]   order         = scratch_order;
        int[]        parentIdx     = scratch_parentIdx;
        Constraint[] viaConstraint = scratch_viaCon;
        boolean[]    viaOut        = scratch_viaOut;
        Variable[]   sVar          = scratch_sVar;
        Variable[]   sParent       = scratch_sParent;
        Constraint[] sCon          = scratch_sCon;
        boolean[]    sOut          = scratch_sOut;

        int stackSize = 0;

        // Push root
        sVar[stackSize]    = root;
        sParent[stackSize] = blockedParent;
        sCon[stackSize]    = null;
        sOut[stackSize]    = false;
        stackSize++;

        int nodeCount = 0;

        while (stackSize > 0) {
            stackSize--;
            Variable v       = sVar[stackSize];
            Variable parent  = sParent[stackSize];
            Constraint via   = sCon[stackSize];
            boolean isOut    = sOut[stackSize];

            int idx = nodeCount++;
            v.dfdvIndex = idx;
            order[idx] = v;
            parentIdx[idx] = (parent == null || parent == blockedParent) ? -1 : parent.dfdvIndex;
            viaConstraint[idx] = via;
            viaOut[idx] = isOut;

            // Push activeOut children: v --c--> child
            List<Constraint> activeOut = v.activeOut;
            for (int i = activeOut.size() - 1; i >= 0; i--) {
                Constraint c = activeOut.get(i);
                if (c.right != parent) {
                    if (stackSize == sVar.length) {
                        int newLen = stackSize * 2;
                        sVar    = scratch_sVar    = Arrays.copyOf(sVar,    newLen);
                        sParent = scratch_sParent = Arrays.copyOf(sParent, newLen);
                        sCon    = scratch_sCon    = Arrays.copyOf(sCon,    newLen);
                        sOut    = scratch_sOut    = Arrays.copyOf(sOut,    newLen);
                    }
                    sVar[stackSize]    = c.right;
                    sParent[stackSize] = v;
                    sCon[stackSize]    = c;
                    sOut[stackSize]    = true;
                    stackSize++;
                }
            }
            // Push activeIn children: child --c--> v
            List<Constraint> activeIn = v.activeIn;
            for (int i = activeIn.size() - 1; i >= 0; i--) {
                Constraint c = activeIn.get(i);
                if (c.left != parent) {
                    if (stackSize == sVar.length) {
                        int newLen = stackSize * 2;
                        sVar    = scratch_sVar    = Arrays.copyOf(sVar,    newLen);
                        sParent = scratch_sParent = Arrays.copyOf(sParent, newLen);
                        sCon    = scratch_sCon    = Arrays.copyOf(sCon,    newLen);
                        sOut    = scratch_sOut    = Arrays.copyOf(sOut,    newLen);
                    }
                    sVar[stackSize]    = c.left;
                    sParent[stackSize] = v;
                    sCon[stackSize]    = c;
                    sOut[stackSize]    = false;
                    stackSize++;
                }
            }
        }

        // Ensure dfdv array is large enough (may be smaller than order array)
        if (scratch_dfdv.length < nodeCount) {
            scratch_dfdv = new double[Math.max(nodeCount, scratch_dfdv.length * 2)];
        }
        double[] dfdv = scratch_dfdv;
        for (int i = 0; i < nodeCount; i++) {
            dfdv[i] = order[i].dfdv();
        }

        // Post-order: process in reverse pre-order (leaves first, root last)
        for (int i = nodeCount - 1; i >= 0; i--) {
            Variable v = order[i];
            double result = dfdv[i] / v.scale;

            Constraint via = viaConstraint[i];
            if (via != null) {
                int pi = parentIdx[i];
                if (viaOut[i]) {
                    via.lm = result;
                    dfdv[pi] += via.lm * via.left.scale;
                } else {
                    via.lm = -result;
                    dfdv[pi] -= via.lm * via.right.scale;
                }
                if (min_lm != null && !via.equality && (min_lm[0] == null || via.lm < min_lm[0].lm)) {
                    min_lm[0] = via;
                }
            }
        }

        double rootResult = dfdv[0] / root.scale;

        // Reset scratch indices
        for (int i = 0; i < nodeCount; i++) {
            order[i].dfdvIndex = -1;
        }

        return rootResult;
    }

    private static final Constraint TRUE = new Constraint(null, null, 0);

    /**
     * Search for the constraint with the smallest lm on the path from lv to rv.
     *
     * Iterative DFS using cached scratch arrays to avoid per-call allocation.
     * Variable.visited is used as the "already-enqueued" flag (reset after use).
     * Variable.dfdvIndex stores the sequential visit index for path reconstruction.
     */
    Constraint split_path(Variable r, Variable v, Variable u, boolean desperation) {
        int capacity = vars.size();
        ensureScratch(capacity);

        Variable[]   sVar       = scratch_sVar;
        Variable[]   sParent    = scratch_sParent;
        Constraint[] sCon       = scratch_sCon;
        boolean[]    sOut       = scratch_sOut;
        Variable[]   visitOrder = scratch_order;
        int[]        visitPIdx  = scratch_parentIdx;
        Constraint[] visitCon   = scratch_viaCon;

        int visitCount = 0;
        int stackSize  = 0;

        // Seed with start node v
        v.dfdvIndex         = visitCount;
        visitOrder[visitCount] = v;
        visitPIdx[visitCount]  = -1;
        visitCon[visitCount]   = null;
        visitCount++;
        v.visited = true;

        sVar[stackSize]    = v;
        sParent[stackSize] = u;
        sCon[stackSize]    = null;
        sOut[stackSize]    = false;
        stackSize++;

        Constraint result = null;

        outer:
        while (stackSize > 0) {
            stackSize--;
            Variable cur    = sVar[stackSize];
            Variable parent = sParent[stackSize];

            // Explore activeIn edges: left --c--> cur  → go to c.left
            List<Constraint> activeIn = cur.activeIn;
            for (int i = 0, n = activeIn.size(); i < n; i++) {
                Constraint c = activeIn.get(i);
                Variable next = c.left;
                if (next == parent) continue;
                if (next == r) {
                    if (!c.equality) {
                        if (!desperation) { result = TRUE; break outer; }
                        Constraint best = c;
                        int pi = cur.dfdvIndex;
                        while (pi >= 0) {
                            Constraint via = visitCon[pi];
                            if (via != null && !via.equality && via.lm < best.lm) best = via;
                            pi = visitPIdx[pi];
                        }
                        result = best; break outer;
                    } else {
                        result = TRUE; break outer;
                    }
                }
                if (!next.visited) {
                    next.visited = true;
                    next.dfdvIndex         = visitCount;
                    visitOrder[visitCount] = next;
                    visitPIdx[visitCount]  = cur.dfdvIndex;
                    visitCon[visitCount]   = c;
                    visitCount++;

                    if (stackSize == sVar.length) {
                        int newLen = stackSize * 2;
                        sVar    = scratch_sVar    = Arrays.copyOf(sVar,    newLen);
                        sParent = scratch_sParent = Arrays.copyOf(sParent, newLen);
                        sCon    = scratch_sCon    = Arrays.copyOf(sCon,    newLen);
                        sOut    = scratch_sOut    = Arrays.copyOf(sOut,    newLen);
                    }
                    sVar[stackSize]    = next;
                    sParent[stackSize] = cur;
                    sCon[stackSize]    = c;
                    sOut[stackSize]    = false;
                    stackSize++;
                }
            }

            // Explore activeOut edges: cur --c--> right  → go to c.right
            List<Constraint> activeOut = cur.activeOut;
            for (int i = 0, n = activeOut.size(); i < n; i++) {
                Constraint c = activeOut.get(i);
                Variable next = c.right;
                if (next == parent) continue;
                if (next == r) {
                    if (!c.equality) {
                        if (!desperation) { result = c; break outer; }
                        Constraint best = c;
                        int pi = cur.dfdvIndex;
                        while (pi >= 0) {
                            Constraint via = visitCon[pi];
                            if (via != null && !via.equality && via.lm < best.lm) best = via;
                            pi = visitPIdx[pi];
                        }
                        result = best; break outer;
                    } else {
                        result = TRUE; break outer;
                    }
                }
                if (!next.visited) {
                    next.visited = true;
                    next.dfdvIndex         = visitCount;
                    visitOrder[visitCount] = next;
                    visitPIdx[visitCount]  = cur.dfdvIndex;
                    visitCon[visitCount]   = c;
                    visitCount++;

                    if (stackSize == sVar.length) {
                        int newLen = stackSize * 2;
                        sVar    = scratch_sVar    = Arrays.copyOf(sVar,    newLen);
                        sParent = scratch_sParent = Arrays.copyOf(sParent, newLen);
                        sCon    = scratch_sCon    = Arrays.copyOf(sCon,    newLen);
                        sOut    = scratch_sOut    = Arrays.copyOf(sOut,    newLen);
                    }
                    sVar[stackSize]    = next;
                    sParent[stackSize] = cur;
                    sCon[stackSize]    = c;
                    sOut[stackSize]    = true;
                    stackSize++;
                }
            }
        }

        // Reset scratch state on all visited nodes
        for (int i = 0; i < visitCount; i++) {
            visitOrder[i].visited  = false;
            visitOrder[i].dfdvIndex = -1;
        }

        return result;
    }

    Constraint split_path(Variable r, Variable v) {
        return split_path(r, v, null, false);
    }

    /**
     * Iterative DFS to zero lm on all active constraints reachable from start.
     * Uses cached scratch arrays to avoid per-call allocation.
     */
    void reset_active_lm(Variable start, Variable blockedParent) {
        int capacity = vars.size();
        ensureScratch(capacity);

        Variable[] sVar    = scratch_sVar;
        Variable[] sParent = scratch_sParent;
        int stackSize = 0;

        sVar[stackSize]    = start;
        sParent[stackSize] = blockedParent;
        stackSize++;

        while (stackSize > 0) {
            stackSize--;
            Variable v      = sVar[stackSize];
            Variable parent = sParent[stackSize];

            List<Constraint> activeOut = v.activeOut;
            for (int i = 0, n = activeOut.size(); i < n; i++) {
                Constraint c = activeOut.get(i);
                if (c.right != parent) {
                    c.lm = 0;
                    if (stackSize == sVar.length) {
                        int newLen = stackSize * 2;
                        sVar    = scratch_sVar    = Arrays.copyOf(sVar,    newLen);
                        sParent = scratch_sParent = Arrays.copyOf(sParent, newLen);
                    }
                    sVar[stackSize]    = c.right;
                    sParent[stackSize] = v;
                    stackSize++;
                }
            }
            List<Constraint> activeIn = v.activeIn;
            for (int i = 0, n = activeIn.size(); i < n; i++) {
                Constraint c = activeIn.get(i);
                if (c.left != parent) {
                    c.lm = 0;
                    if (stackSize == sVar.length) {
                        int newLen = stackSize * 2;
                        sVar    = scratch_sVar    = Arrays.copyOf(sVar,    newLen);
                        sParent = scratch_sParent = Arrays.copyOf(sParent, newLen);
                    }
                    sVar[stackSize]    = c.left;
                    sParent[stackSize] = v;
                    stackSize++;
                }
            }
        }
    }

    /**
     * Finds the constraint with the minimum lagrange multiplier.
     */
    public Constraint findMinLM() {
        Constraint[] min_lm = {null};
        reset_active_lm(vars.getFirst(), null);
        compute_dfdv(vars.getFirst(), null, min_lm);
        return min_lm[0];
    }

    /**
     * Finds the constraint to split on the path from lv to rv.
     *
     * The reset_active_lm + compute_dfdv calls are omitted here: split_path is
     * always called with desperation=false, so it never reads lm values — it
     * only checks c.equality. Removing those two O(N) traversals eliminates
     * ~65% of the work that was previously visible inside splitBetween.
     */
    public Constraint findMinLMBetween(Variable lv, Variable rv) {
        final var min_lm = split_path(rv, lv);
        if (min_lm == null || min_lm == TRUE) {
            return null;
        }
        return min_lm;
    }

    /**
     * Populates block b by traversing the active constraint tree adding variables.
     *
     * Optimisations vs. the previous version:
     *  - Iterative DFS (no recursion) using the source block's cached scratch arrays.
     *  - Variables are registered on b (v.block = b, vars.add(v), ps accumulation)
     *    without recomputing posn after every single variable.  posn is computed
     *    once at the end, saving (N-1) divisions per call.
     */
    void populateSplitBlock(Block b, Variable startV, Variable startU) {
        int capacity = vars.size();
        ensureScratch(capacity);

        Variable[] sVar    = scratch_sVar;
        Variable[] sParent = scratch_sParent;
        int stackSize = 0;

        sVar[stackSize]    = startV;
        sParent[stackSize] = startU;
        stackSize++;

        PositionStats bps = b.ps;

        while (stackSize > 0) {
            stackSize--;
            Variable v      = sVar[stackSize];
            Variable parent = sParent[stackSize];

            // Register v into block b without updating posn yet
            v.block = b;
            b.vars.add(v);
            if (bps.A2 == 0) bps.scale = v.scale;
            bps.addVariable(v);

            for (int i = 0, n = v.activeIn.size(); i < n; i++) {
                Constraint c = v.activeIn.get(i);
                if (c.left != parent) {
                    if (stackSize == sVar.length) {
                        int newLen = stackSize * 2;
                        sVar    = scratch_sVar    = Arrays.copyOf(sVar,    newLen);
                        sParent = scratch_sParent = Arrays.copyOf(sParent, newLen);
                    }
                    sVar[stackSize]    = c.left;
                    sParent[stackSize] = v;
                    stackSize++;
                }
            }
            for (int i = 0, n = v.activeOut.size(); i < n; i++) {
                Constraint c = v.activeOut.get(i);
                if (c.right != parent) {
                    if (stackSize == sVar.length) {
                        int newLen = stackSize * 2;
                        sVar    = scratch_sVar    = Arrays.copyOf(sVar,    newLen);
                        sParent = scratch_sParent = Arrays.copyOf(sParent, newLen);
                    }
                    sVar[stackSize]    = c.right;
                    sParent[stackSize] = v;
                    stackSize++;
                }
            }
        }

        // Compute posn once for the entire populated block
        b.posn = (bps.AD - bps.AB) / bps.A2;
        assert !Double.isNaN(b.posn);
    }

    /**
     * Returns true if there is a directed path of active constraints from u to v.
     *
     * Iterative BFS/DFS replaces the previously recursive implementation to
     * eliminate JVM stack-frame overhead on long active-constraint chains.
     * Uses the block's cached scratch arrays (only sVar/sParent needed).
     */
    public boolean isActiveDirectedPathBetween(Variable u, Variable v) {
        if (u == v) return true;
        int capacity = Math.max(vars.size(), INITIAL_SCRATCH);
        ensureScratch(capacity);

        Variable[] stack   = scratch_sVar;
        int stackSize = 0;

        stack[stackSize++] = u;

        while (stackSize > 0) {
            Variable cur = stack[--stackSize];
            List<Constraint> activeOut = cur.activeOut;
            for (int i = 0, n = activeOut.size(); i < n; i++) {
                Variable next = activeOut.get(i).right;
                if (next == v) return true;
                if (stackSize == stack.length) {
                    stack = scratch_sVar = Arrays.copyOf(stack, stackSize * 2);
                }
                stack[stackSize++] = next;
            }
        }
        return false;
    }

    /**
     * Split this block because of a violated constraint between vl and vr.
     * Returns the split constraint.
     */
    public Constraint splitBetween(Variable vl, Variable vr, Block[] result) {
        Constraint c = findMinLMBetween(vl, vr);
        if (c != null) {
            Block[] lr = new Block[2];
            split(lr, c);
            result[0] = lr[0]; // lb
            result[1] = lr[1]; // rb
            deleted = true;
        }
        return c;
    }

    /**
     * Creates two new blocks, l and r, and splits this block across constraint c.
     * The new blocks' vars lists are pre-sized to this block's size to avoid
     * ArrayList resizing during populateSplitBlock (visible as add/gro in profiler).
     */
    public void split(Block[] result, Constraint c) {
        c.deactivate();
        int hint = vars.size();
        Block l = new Block(blocks, hint);
        populateSplitBlock(l, c.left, c.right);
        Block r = new Block(blocks, hint);
        populateSplitBlock(r, c.right, c.left);
        result[0] = l;
        result[1] = r;
    }

    /**
     * Computes the cost (squared euclidean distance from desired positions).
     */
    public double cost() {
        double c = 0;
        for (Variable v : vars) {
            double diff = v.position() - v.desiredPosition;
            c += v.weight.weight * diff * diff;
        }
        return c;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Block(posn=").append(posn).append("):");
        for (Variable v : vars) {
            sb.append(" ").append(v);
        }
        if (deleted) {
            sb.append(" Deleted!");
        }
        return sb.toString();
    }
}
