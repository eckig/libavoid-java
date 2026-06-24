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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A dynamic Polygon, to which points can be easily added and removed.
 * Also used as PolyLine (typedef in C++).
 */
public class Polygon extends AbstractPolygon {

    /** An ID for the polygon. */
    public int _id;

    /** A vector of the points that make up the Polygon. */
    public List<Point> ps;

    /** If used, denotes whether the corresponding point in ps is
     *  a move-to operation or a Bezier curve-to. */
    public List<Character> ts;

    public Polygon() {
        _id = 0;
        ps = new ArrayList<>();
        ts = new ArrayList<>();
    }

    public Polygon(int n) {
        _id = 0;
        ps = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ps.add(new Point());
        }
        ts = new ArrayList<>();
    }

    public Polygon(AbstractPolygon poly) {
        _id = poly.id();
        ps = new ArrayList<>(poly.size());
        for (int i = 0; i < poly.size(); ++i) {
            ps.add(new Point(poly.at(i)));
        }
        ts = new ArrayList<>();
    }

    /** Copy constructor for Polygon specifically. */
    public Polygon(Polygon other) {
        _id = other._id;
        ps = new ArrayList<>(other.ps.size());
        for (Point p : other.ps) {
            ps.add(new Point(p));
        }
        ts = new ArrayList<>(other.ts);
    }

    @Override
    public void clear() {
        ps.clear();
        ts.clear();
    }

    @Override
    public boolean empty() {
        return ps.isEmpty();
    }

    @Override
    public int size() {
        return ps.size();
    }

    @Override
    public int id() {
        return _id;
    }

    @Override
    public Point at(int index) {
        return ps.get(index);
    }

    public void setPoint(int index, Point point) {
        ps.set(index, point);
    }

    public Polygon simplify() {
        Polygon simplified = new Polygon(this);

        int j = 2;
        while (j < simplified.size()) {
            if (Geometry.vecDir(simplified.ps.get(j - 2), simplified.ps.get(j - 1),
                    simplified.ps.get(j)) == 0) {
                simplified.ps.remove(j - 1);
            } else {
                ++j;
            }
        }

        return simplified;
    }

    public void translate(double xDist, double yDist) {
        for (Point p : ps) {
            p.x += xDist;
            p.y += yDist;
        }
    }

    /**
     * Value-based equality: two Polygons are equal if they have the same
     * points in the same order.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Polygon other)) return false;
        return ps.equals(other.ps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ps);
    }
}
