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
import java.util.List;

/**
 * Variable Placement with Separation Constraints incremental solver.
 * Translated from vpsc.h/vpsc.cpp in libavoid C++.
 *
 * <p>The inactive constraint list is split into two sub-lists:
 * <ul>
 *   <li>{@code inactiveEq}   — equality constraints; checked first in
 *       {@link #mostViolated()} since any equality immediately wins.</li>
 *   <li>{@code inactiveIneq} — inequality constraints; scanned for minimum
 *       slack only when no equality is pending.</li>
 * </ul>
 * This eliminates the per-element {@code c.equality} branch from the hot
 * inequality scan loop and lets the equality check short-circuit without
 * touching the (typically much larger) inequality list at all.</p>
 */
public class IncSolver {
    public int splitCnt;
    protected Blocks bs;
    protected int m;
    protected final List<Constraint> cs;
    protected int n;
    protected final List<Variable> vs;
    protected boolean needsScaling;

    /** Inactive equality constraints — checked first by {@link #mostViolated()}. */
    private final List<Constraint> inactiveEq;
    /** Inactive inequality constraints — scanned for minimum slack. */
    private final List<Constraint> inactiveIneq;

    private static final double ZERO_UPPERBOUND = -1e-10;
    private static final double LAGRANGIAN_TOLERANCE = -1e-4;

    public IncSolver(List<Variable> vs, List<Constraint> cs) {
        this.m = cs.size();
        this.cs = cs;
        this.n = vs.size();
        this.vs = vs;
        this.needsScaling = false;

        for (int i = 0; i < n; i++) {
            vs.get(i).in.clear();
            vs.get(i).out.clear();
            vs.get(i).activeIn.clear();
            vs.get(i).activeOut.clear();
            needsScaling |= (vs.get(i).scale != 1);
        }
        for (int i = 0; i < m; i++) {
            Constraint c = cs.get(i);
            c.left.out.add(c);
            c.right.in.add(c);
            c.needsScaling = needsScaling;
        }
        bs = new Blocks(vs);

        inactiveEq   = new ArrayList<>();
        inactiveIneq = new ArrayList<>(m);
        for (Constraint c : cs) {
            c.active = false;
            if (c.equality) inactiveEq.add(c);
            else             inactiveIneq.add(c);
        }
    }

    /**
     * Resets the solver for another solve pass over the same variable and
     * constraint lists, without allocating new objects.
     *
     * Only the constraint gaps may have changed between iterations (as done
     * by the nudgeOrthogonalRoutes do/while loop).  Everything else —
     * variables, constraints, their graph wiring — stays structurally
     * identical, so we only need to:
     *  1. Re-clear and re-wire in/out/activeIn/activeOut on every variable.
     *  2. Reset c.active on every constraint.
     *  3. Reinitialize the Blocks structure (one Block per variable).
     *  4. Refill the inactive lists from cs.
     */
    public void reset() {
        needsScaling = false;
        for (int i = 0; i < n; i++) {
            Variable v = vs.get(i);
            v.in.clear();
            v.out.clear();
            v.activeIn.clear();
            v.activeOut.clear();
            needsScaling |= (v.scale != 1);
        }
        for (int i = 0; i < m; i++) {
            Constraint c = cs.get(i);
            c.left.out.add(c);
            c.right.in.add(c);
            c.needsScaling = needsScaling;
        }
        bs.reinitialize(vs);

        inactiveEq.clear();
        inactiveIneq.clear();
        for (Constraint c : cs) {
            c.active = false;
            if (c.equality) inactiveEq.add(c);
            else             inactiveIneq.add(c);
        }
    }

    /**
     * Stores the relative positions of the variables in their finalPosition field.
     */
    protected void copyResult() {
        for (Variable v : vs) {
            v.finalPosition = v.position();
            assert !Double.isNaN(v.finalPosition);
        }
    }

    /**
     * Incremental version of satisfy that allows refinement after blocks are
     * moved.
     * - move blocks to new positions
     * - repeatedly merge across most violated constraint until no more
     * violated constraints exist
     * <p>
     * C++ IncSolver::satisfy() from vpsc.cpp line 274
     */
    public void satisfy() {
        splitBlocks();
        Constraint v;
        while ((v = mostViolated()) != null &&
                (v.equality || (v.slack() < ZERO_UPPERBOUND && !v.active))) {
            Block lb = v.left.block;
            Block rb = v.right.block;
            if (lb != rb) {
                lb.merge(rb, v);
            } else {
                // constraint is within block, need to split first
                Block[] result = splitResult;
                Constraint splitConstraint = lb.splitBetween(v.left, v.right, result);
                if (splitConstraint != null) {
                    addToInactive(splitConstraint);
                } else {
                    v.unsatisfiable = true;
                    continue;
                }
                Block newLb = result[0];
                Block newRb = result[1];
                if (v.slack() >= 0) {
                    // v was satisfied by the above split!
                    addToInactive(v);
                    bs.insert(newLb);
                    bs.insert(newRb);
                } else {
                    bs.insert(newLb.merge(newRb, v));
                }
            }
        }
        bs.cleanup();
        for (int i = 0; i < m; i++) {
            v = cs.get(i);
            if (v.slack() < ZERO_UPPERBOUND) {
                throw new RuntimeException("Unsatisfied constraint: " + v);
            }
        }
        copyResult();
    }

    /** Routes a newly-inactive constraint to the correct sub-list. */
    private void addToInactive(Constraint c) {
        if (c.equality) inactiveEq.add(c);
        else             inactiveIneq.add(c);
    }

    /**
     * Returns and removes the most-violated (or first equality) inactive
     * constraint, or {@code null} if no constraint is violated.
     *
     * <p>Equality constraints are checked first: if {@code inactiveEq} is
     * non-empty the first entry is popped and returned immediately (O(1),
     * swap-remove), without touching {@code inactiveIneq} at all.</p>
     *
     * <p>Otherwise {@code inactiveIneq} is scanned linearly for the entry with
     * the smallest {@link Constraint#slack()} (= most violated).  The branch-free
     * inner loop has no {@code c.equality} test — that check has been hoisted
     * out by splitting the list.</p>
     *
     * <p>C++ IncSolver::mostViolated from vpsc.cpp line 431.</p>
     */
    private Constraint mostViolated() {
        // --- Equality constraints have unconditional priority ---
        if (!inactiveEq.isEmpty()) {
            int last = inactiveEq.size() - 1;
            Constraint eq = inactiveEq.get(last);
            inactiveEq.remove(last);
            return eq;
        }

        // --- Scan inequality constraints for minimum slack ---
        double slackForMostViolated = Double.MAX_VALUE;
        Constraint mv = null;
        int lSize = inactiveIneq.size();
        int deleteIndex = lSize;

        for (int index = 0; index < lSize; index++) {
            Constraint c = inactiveIneq.get(index);
            double slack = c.slack();
            if (slack < slackForMostViolated) {
                slackForMostViolated = slack;
                mv = c;
                deleteIndex = index;
            }
        }

        if (deleteIndex < lSize &&
                slackForMostViolated < ZERO_UPPERBOUND && !mv.active) {
            // Swap-remove: O(1) deletion without shifting the list
            inactiveIneq.set(deleteIndex, inactiveIneq.get(lSize - 1));
            inactiveIneq.remove(lSize - 1);
        } else {
            mv = null; // Not violated — return null to stop the satisfy loop
        }
        return mv;
    }

    /**
     * Solve: satisfy then iterate until cost stabilises.
     * C++ IncSolver::solve() from vpsc.cpp line 243
     */
    public void solve() {
        satisfy();
        double lastCost = Double.MAX_VALUE;
        double cost = bs.cost();
        while (Math.abs(cost - lastCost) > 0.0001) {
            satisfy();
            lastCost = cost;
            cost = bs.cost();
        }
        copyResult();
    }

    public void moveBlocks() {
        int length = bs.size();
        for (int i = 0; i < length; i++) {
            Block b = bs.at(i);
            b.updateWeightedPosition();
        }
    }

    // Reusable result array for split() calls — avoids a new Block[2] allocation
    // on every iteration of the splitBlocks loop.
    private final Block[] splitResult = new Block[2];

    public void splitBlocks() {
        moveBlocks();
        splitCnt = 0;
        // Split each block if necessary on min LM
        int length = bs.size();
        for (int i = 0; i < length; i++) {
            Block b = bs.at(i);
            Constraint v = b.findMinLM();
            if (v != null && v.lm < LAGRANGIAN_TOLERANCE) {
                splitCnt++;
                Block[] result = splitResult;
                b.split(result, v);
                Block l = result[0];
                Block r = result[1];
                l.updateWeightedPosition();
                r.updateWeightedPosition();
                bs.insert(l);
                bs.insert(r);
                b.deleted = true;
                // Split constraints are always inequalities (equality constraints
                // are never split by findMinLM, which skips c.equality)
                inactiveIneq.add(v);
            }
        }
        bs.cleanup();
    }
}
