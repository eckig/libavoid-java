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

import java.util.List;

/**
 * VPSC Constraint - represents a separation constraint between two variables:
 *   left.position() + gap <= right.position()
 * Translated from vpsc.h/vpsc.cpp in libavoid C++.
 */
public class Constraint implements Comparable<Constraint> {
    public Variable left;
    public Variable right;
    public double gap;
    public double lm; // lagrange multiplier
    public long timeStamp;
    public boolean active;
    public final boolean equality;
    public boolean unsatisfiable;
    public boolean needsScaling;
    public Object creator;

    public Constraint(Variable left, Variable right, double gap, boolean equality) {
        this.left = left;
        this.right = right;
        this.gap = gap;
        this.lm = 0;
        this.timeStamp = 0;
        this.active = false;
        this.equality = equality;
        this.unsatisfiable = false;
        this.needsScaling = true;
        this.creator = null;
    }

    public Constraint(Variable left, Variable right, double gap) {
        this(left, right, gap, false);
    }

    /**
     * Activates this constraint: sets active=true and adds it to the
     * activeOut list of the left variable and activeIn list of the right variable.
     */
    public void activate() {
        assert !active;
        active = true;
        left.activeOut.add(this);
        right.activeIn.add(this);
    }

    /**
     * Deactivates this constraint: sets active=false and removes it from the
     * activeOut list of the left variable and activeIn list of the right variable.
     *
     * Uses swap-remove (O(1)) instead of ArrayList.remove (O(n)) because the
     * activeIn/activeOut lists have no ordering requirement.
     */
    public void deactivate() {
        assert active;
        active = false;
        swapRemove(left.activeOut, this);
        swapRemove(right.activeIn, this);
    }

    /**
     * Removes {@code c} from {@code list} in O(1) by swapping it with the last
     * element and truncating. Safe when list order is irrelevant.
     */
    private static void swapRemove(List<Constraint> list, Constraint c) {
        int idx = list.indexOf(c);
        if (idx < 0) return;
        int last = list.size() - 1;
        if (idx != last) {
            list.set(idx, list.get(last));
        }
        list.remove(last);
    }

    public double slack() {
        if (unsatisfiable) {
            return Double.MAX_VALUE;
        }
        if (needsScaling) {
            return right.scale * right.position() - gap -
                    left.scale * left.position();
        }
        assert left.scale == 1;
        assert right.scale == 1;
        return right.unscaledPosition() - gap - left.unscaledPosition();
    }

    /**
     * Comparator for Java PriorityQueue (min-heap).
     * Translated from CompareConstraints::operator() in vpsc.cpp:1366-1387.
     * C++ std::priority_queue is a max-heap; operator() returning true means
     * "l has lower priority". The C++ "return sl > sr" means larger slack =
     * lower priority, so the most-violated constraint (smallest slack) is at
     * the top.
     * Java PriorityQueue is a min-heap; compareTo returning negative means
     * "this has higher priority (comes first)". We translate so that
     * smaller effective slack → negative compareTo → polled first.
     */
    @Override
    public int compareTo(Constraint other) {
        // C++ vpsc.cpp:1369-1376
        // Substitute -DBL_MAX when the block has been restructured since this
        // constraint was last processed, or when left and right are in the
        // same block (constraint is internal → stale).
        double sl =
                (left.block.timeStamp > this.timeStamp
                        || left.block == right.block)
                        ? -Double.MAX_VALUE : this.slack();
        double sr =
                (other.left.block.timeStamp > other.timeStamp
                        || other.left.block == other.right.block)
                        ? -Double.MAX_VALUE : other.slack();
        if (sl == sr) {
            // C++ vpsc.cpp:1377-1384 — deterministic tie-breaking by variable id.
            if (left.id == other.left.id) {
                return other.right.id.compareTo(right.id);
            }
            return other.left.id.compareTo(left.id);
        }
        // C++ vpsc.cpp:1386: return sl > sr;
        return Double.compare(sl, sr);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Constraint: var(").append(left.id).append(") ");
        if (gap < 0) {
            sb.append("- ").append(-gap);
        } else {
            sb.append("+ ").append(gap);
        }
        if (equality) {
            sb.append(" == ");
        } else {
            sb.append(" <= ");
        }
        sb.append("var(").append(right.id).append(")");
        return sb.toString();
    }
}
