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
 * The Point class defines a point in the plane.
 * Points consist of an x and y value. They may also have an ID and vertex
 * number associated with them.
 */
public class Point implements Comparable<Point> {

    public static final int XDIM = 0;
    public static final int YDIM = 1;

    /** Constant value representing an unassigned vertex number. */
    public static final short kUnassignedVertexNumber = 8;

    /** The x position. */
    public double x;
    /** The y position. */
    public double y;
    /** The ID associated with this point. */
    public int id;
    /** The vertex number associated with this point. */
    public short vn;

    public Point() {
        this.x = 0;
        this.y = 0;
        this.id = 0;
        this.vn = kUnassignedVertexNumber;
    }

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
        this.id = 0;
        this.vn = kUnassignedVertexNumber;
    }

    public Point(Point other) {
        this.x = other.x;
        this.y = other.y;
        this.id = other.id;
        this.vn = other.vn;
    }

    public boolean equals(Point rhs, double epsilon) {
        return (Math.abs(x - rhs.x) < epsilon) && (Math.abs(y - rhs.y) < epsilon);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Point rhs)) return false;
        return x == rhs.x && y == rhs.y;
    }

    // hashCode and compareTo treat -0.0 like 0.0 (adding 0.0 turns -0.0 into 0.0), consistent with equals (==),
    // as in C++ (operator== and operator< compare with == and <).

    @Override
    public int hashCode() {
        return 31 * (31 + Double.hashCode(x + 0.0)) + Double.hashCode(y + 0.0);
    }

    @Override
    public int compareTo(Point rhs) {
        if (x == rhs.x) {
            return Double.compare(y + 0.0, rhs.y + 0.0);
        }
        return Double.compare(x + 0.0, rhs.x + 0.0);
    }

    /** Returns the x or y value of the point, given the dimension. */
    public double get(int dimension) {
        return (dimension == 0) ? x : y;
    }

    /** Sets the x or y value of the point, given the dimension. */
    public void set(int dimension, double value) {
        if (dimension == 0) x = value; else y = value;
    }

    public Point add(Point rhs) {
        return new Point(x + rhs.x, y + rhs.y);
    }

    public Point subtract(Point rhs) {
        return new Point(x - rhs.x, y - rhs.y);
    }

    @Override
    public String toString() {
        return "Point(" + x + ", " + y + ")";
    }
}
