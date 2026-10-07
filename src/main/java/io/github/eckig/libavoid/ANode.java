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

package io.github.eckig.libavoid;

/**
 * A* search node — translated from makepath.cpp ANode struct.
 */
public class ANode {
    public VertInf inf;
    public double g;  // cost so far
    public double h;  // heuristic
    public double f;  // f = g + h
    public ANode prevNode;
    public int timeStamp;
    /** Set when this node was replaced by a cheaper one; stale nodes are skipped when polled. */
    public boolean stale;

    public ANode(VertInf vinf, int time) {
        this.inf = vinf;
        this.g = 0;
        this.h = 0;
        this.f = 0;
        this.prevNode = null;
        this.timeStamp = time;
    }

    public ANode() {
        this(null, -1);
    }
}
