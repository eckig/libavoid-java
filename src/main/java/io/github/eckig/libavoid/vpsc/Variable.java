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
 * VPSC Variable - represents a variable in the Variable Placement with
 * Separation Constraints problem.
 * Translated from vpsc.h/vpsc.cpp in libavoid C++.
 */
public class Variable {

    public enum Id {
        freeSegmentID,
        fixedSegmentID,
        channelLeftID,
        channelRightID
    }

    public enum Weight {
        freeWeight(0.00001),
        strongWeight(0.001),
        strongerWeight(1.0),
        fixedWeight(100000.0);

        public final double weight;
        Weight(double weight) {
            this.weight = weight;
        }
    }

    public final Id id;
    public double desiredPosition;
    public double finalPosition;
    public final Weight weight; // how much the variable wants to be at its desired position
    public final double scale; // translates variable to another space
    public double offset;
    public Block block;
    public boolean visited;
    public boolean fixedDesiredPosition;
    /** Scratch index assigned during iterative DFS traversals; -1 when not in use. */
    public int dfdvIndex = -1;
    public List<Constraint> in = new ArrayList<>();
    public List<Constraint> out = new ArrayList<>();

    // Active-constraint sublists: only constraints where c.active == true.
    // Maintained by Constraint.activate() / Constraint.deactivate().
    // Used by Block's recursive tree-walking methods instead of scanning
    // the full in/out lists and filtering via canFollowLeft/canFollowRight.
    public List<Constraint> activeIn = new ArrayList<>();
    public List<Constraint> activeOut = new ArrayList<>();

    public Variable(Id id, double desiredPos, Weight weight, double scale) {
        this.id = id;
        this.desiredPosition = desiredPos;
        this.weight = weight;
        this.scale = scale;
        this.offset = 0;
        this.block = null;
        this.visited = false;
        this.fixedDesiredPosition = false;
    }

    public Variable(Id id, double desiredPos, Weight weight) {
        this(id, desiredPos, weight, 1.0);
    }

    public double dfdv() {
        return 2.0 * weight.weight * (position() - desiredPosition);
    }

    public double position() {
        return (block.ps.scale * block.posn + offset) / scale;
    }

    double unscaledPosition() {
        assert block.ps.scale == 1;
        assert scale == 1;
        return block.posn + offset;
    }

    @Override
    public String toString() {
        return "Variable(" + id + ", desired=" + desiredPosition + 
               ", final=" + finalPosition + ", weight=" + weight + ")";
    }
}
