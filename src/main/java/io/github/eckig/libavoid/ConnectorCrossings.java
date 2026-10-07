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
import java.util.Map;
import java.util.Set;

/**
 * Computes crossing and shared-path information between two connector routes.
 * Corresponds to ConnectorCrossings in connector.h/connector.cpp in C++.
 */
class ConnectorCrossings {

    // Crossing flag constants (from connector.h)
    static final int CROSSING_NONE = 0;
    static final int CROSSING_TOUCHES = 1;
    static final int CROSSING_SHARES_PATH = 2;
    static final int CROSSING_SHARES_PATH_AT_END = 4;
    static final int CROSSING_SHARES_FIXED_SEGMENT = 8;

    Polygon poly;
    boolean polyIsConn;
    Polygon conn;
    boolean checkForBranchingSegments;
    ConnRef polyConnRef;
    ConnRef connConnRef;

    int crossingCount;
    int crossingFlags;
    Map<Point, PtOrder> pointOrders;
    Set<Point> crossingPoints;          // C++ connector.h:530
    List<List<Point>> sharedPaths;      // C++ connector.h:533
    double firstSharedPathAtEndLength;
    double secondSharedPathAtEndLength;

    ConnectorCrossings(Polygon poly, boolean polyIsConn, Polygon conn,
                       ConnRef polyConnRef, ConnRef connConnRef) {
        this.poly = poly;
        this.polyIsConn = polyIsConn;
        this.conn = conn;
        this.checkForBranchingSegments = false;
        this.polyConnRef = polyConnRef;
        this.connConnRef = connConnRef;
        clear();
    }

    void clear() {
        crossingCount = 0;
        crossingFlags = CROSSING_NONE;
        firstSharedPathAtEndLength = Double.MAX_VALUE;
        secondSharedPathAtEndLength = Double.MAX_VALUE;
    }

    /**
     * Works out if the segment conn[cIndex-1]--conn[cIndex] really crosses poly.
     */
    // scratch arrays of countForSegment, reused across calls
    private Point[] m_c_path = new Point[16];
    private Point[] m_p_path = new Point[16];

    void countForSegment(int cIndex, boolean finalSegment) {
        clear();

        boolean polyIsOrthogonal = (polyConnRef != null &&
                (polyConnRef.routingType() == ConnType.Orthogonal));
        boolean connIsOrthogonal = (connConnRef != null &&
                (connConnRef.routingType() == ConnType.Orthogonal));

        boolean polyIsFixed = (polyConnRef != null && polyConnRef.hasFixedRoute());
        boolean connIsFixed = (connConnRef != null && connConnRef.hasFixedRoute());

        if (checkForBranchingSegments || polyIsFixed || connIsFixed ||
                !polyIsOrthogonal || !connIsOrthogonal) {
            double epsilon = Math.ulp(1.0);
            int conn_pn = conn.size();
            double tolerance = (!polyIsConn) ? epsilon : 0.0;
            splitBranchingSegments(poly, polyIsConn, conn, tolerance);
            cIndex += (conn.size() - conn_pn);
        }
        assert cIndex >= 1;
        assert cIndex < conn.size();

        int poly_size = poly.size();
        Point a1 = conn.ps.get(cIndex - 1);
        Point a2 = conn.ps.get(cIndex);

        int max_path_size = Math.min(poly_size, conn.size());
        if (m_c_path.length < max_path_size) {
            m_c_path = new Point[Math.max(max_path_size, m_c_path.length * 2)];
            m_p_path = new Point[m_c_path.length];
        }
        Point[] c_path = m_c_path;
        Point[] p_path = m_p_path;

        for (int j = (polyIsConn ? 1 : 0); j < poly_size; ++j) {
            Point b1 = poly.ps.get(((j - 1) + poly_size) % poly_size);
            Point b2 = poly.ps.get(j);

            int size = 0;
            boolean converging = false;
            boolean a1_eq_b1 = a1.equals(b1);
            boolean a2_eq_b1 = a2.equals(b1);
            boolean a2_eq_b2 = a2.equals(b2);
            boolean a1_eq_b2 = a1.equals(b2);

            if ((a1_eq_b1 && a2_eq_b2) || (a2_eq_b1 && a1_eq_b2)) {
                if (finalSegment) {
                    converging = true;
                } else {
                    continue;
                }
            } else if (a2_eq_b1 || a2_eq_b2 || a1_eq_b2) {
                continue;
            }

            if (a1_eq_b1 || converging) {
                if (!converging) {
                    if (polyIsConn && (j == 1)) {
                        continue;
                    }
                    Point b0 = poly.ps.get(((j - 2) + poly_size) % poly_size);
                    if (a2.equals(b0)) {
                        continue;
                    }
                }

                boolean shared_path = false;
                boolean p_dir_back = false;
                int p_dir = 0;
                int trace_c = 0;
                int trace_p = 0;

                if (converging) {
                    p_dir_back = a2_eq_b2;
                    p_dir = p_dir_back ? -1 : 1;
                    trace_c = cIndex;
                    trace_p = j;
                    if (!p_dir_back) {
                        if (finalSegment) trace_p--;
                        else trace_c--;
                    }
                    shared_path = true;
                } else if (cIndex >= 2) {
                    Point b0 = poly.ps.get(((j - 2) + poly_size) % poly_size);
                    Point a0 = conn.ps.get(cIndex - 2);
                    if (a0.equals(b2) || a0.equals(b0)) {
                        p_dir_back = a0.equals(b0);
                        p_dir = p_dir_back ? -1 : 1;
                        trace_c = cIndex;
                        trace_p = p_dir_back ? j : j - 2;
                        shared_path = true;
                    }
                }

                if (shared_path) {
                    crossingFlags |= CROSSING_SHARES_PATH;
                    while ((trace_c >= 0) && (!polyIsConn || ((trace_p >= 0) && (trace_p < poly_size)))) {
                        int index_p = ((trace_p + (2 * poly_size)) % poly_size);
                        c_path[size] = conn.ps.get(trace_c);
                        p_path[size] = poly.ps.get(index_p);
                        ++size;
                        if ((size > 1) && (!conn.ps.get(trace_c).equals(poly.ps.get(index_p)))) {
                            break;
                        }
                        trace_c--;
                        trace_p += p_dir;
                    }

                    boolean front_same = c_path[0].equals(p_path[0]);
                    boolean back_same = c_path[size - 1].equals(p_path[size - 1]);

                    boolean terminatesAtJunction = false;
                    if (polyConnRef != null && connConnRef != null && (front_same || back_same)) {
                        ConnEnd connSrc = connConnRef.sourceEndpoint();
                        ConnEnd connDst = connConnRef.destEndpoint();
                        JunctionRef connJunction;
                        ConnEnd polySrc = polyConnRef.sourceEndpoint();
                        ConnEnd polyDst = polyConnRef.destEndpoint();
                        JunctionRef polyJunction;

                        connJunction = front_same ? connDst.junction() : connSrc.junction();
                        boolean use_first = back_same;
                        if (p_dir_back) use_first = !use_first;
                        polyJunction = use_first ? polyDst.junction() : polySrc.junction();
                        terminatesAtJunction = (connJunction != null && (connJunction == polyJunction));
                    }

                    // C++ connector.cpp:1984-1996
                    if (sharedPaths != null) {
                        // Store a copy of the shared path
                        int start = front_same ? 0 : 1;
                        int limit = size - (back_same ? 0 : 1);
                        List<Point> sPath = new ArrayList<>(limit - start);
                        for (int i = start; i < limit; ++i) {
                            sPath.add(new Point(c_path[i]));
                        }
                        sharedPaths.add(sPath);
                    }

                    if (polyIsOrthogonal && connIsOrthogonal) {
                        int startPt = front_same ? 0 : 1;
                        int endPt = size - (back_same ? 1 : 2);
                        for (int dim = 0; dim < 2; ++dim) {
                            if (c_path[startPt].get(dim) == c_path[endPt].get(dim)) {
                                double pos = c_path[startPt].get(dim);
                                if (((pos == poly.ps.getFirst().get(dim)) || (pos == poly.ps.get(poly_size - 1).get(dim))) &&
                                    ((pos == conn.ps.getFirst().get(dim)) || (pos == conn.ps.get(cIndex).get(dim))) &&
                                    (!terminatesAtJunction)) {
                                    crossingFlags |= CROSSING_SHARES_FIXED_SEGMENT;
                                }
                            }
                        }
                        if (!front_same && !back_same) {
                            for (int dim = 0; dim < 2; ++dim) {
                                int end = size - 1;
                                int altDim = (dim + 1) % 2;
                                if (c_path[1].get(altDim) == c_path[end - 1].get(altDim)) {
                                    double posBeg = c_path[1].get(dim);
                                    double posEnd = c_path[end - 1].get(dim);
                                    if ((posBeg == c_path[0].get(dim)) && (posBeg == p_path[0].get(dim)) &&
                                        (posEnd == c_path[end].get(dim)) && (posEnd == p_path[end].get(dim))) {
                                        if (posInlineWithConnEndSegs(posBeg, dim, conn, poly) &&
                                            posInlineWithConnEndSegs(posEnd, dim, conn, poly)) {
                                            crossingFlags |= CROSSING_SHARES_FIXED_SEGMENT;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    int startCornerSide = 1;
                    int endCornerSide;
                    if (!front_same) startCornerSide = Geometry.cornerSide(c_path[0], c_path[1], c_path[2], p_path[0]);
                    if (!back_same) endCornerSide = Geometry.cornerSide(c_path[size - 3], c_path[size - 2], c_path[size - 1], p_path[size - 1]);
                    else endCornerSide = startCornerSide;
                    if (front_same) startCornerSide = endCornerSide;

                    if (endCornerSide != startCornerSide) {
                        crossingCount += 1;
                        // C++ connector.cpp:2141-2144
                        if (crossingPoints != null) {
                            crossingPoints.add(new Point(c_path[1]));
                        }
                    }

                    if (front_same || back_same) {
                        crossingFlags |= CROSSING_SHARES_PATH_AT_END;
                        double straightModifier = 200;
                        firstSharedPathAtEndLength = secondSharedPathAtEndLength = pathLength(c_path, p_path, size);
                        if (back_same && (size > 2)) {
                            if (Geometry.vecDir(p_path[0], p_path[1], p_path[2]) == 0) firstSharedPathAtEndLength -= straightModifier;
                            if (Geometry.vecDir(c_path[0], c_path[1], c_path[2]) == 0) secondSharedPathAtEndLength -= straightModifier;
                        } else if (front_same && (size > 2)) {
                            if (Geometry.vecDir(p_path[size - 3], p_path[size - 2], p_path[size - 1]) == 0) firstSharedPathAtEndLength -= straightModifier;
                            if (Geometry.vecDir(c_path[size - 3], c_path[size - 2], c_path[size - 1]) == 0) secondSharedPathAtEndLength -= straightModifier;
                        }
                    } else if (polyIsOrthogonal && connIsOrthogonal) {
                        int cStartDir = Geometry.vecDir(c_path[0], c_path[1], c_path[2]);
                        int pStartDir = Geometry.vecDir(p_path[0], p_path[1], p_path[2]);
                        if ((cStartDir != 0) && (cStartDir == -pStartDir)) startCornerSide = -cStartDir;
                        else {
                            int cEndDir = Geometry.vecDir(c_path[size - 3], c_path[size - 2], c_path[size - 1]);
                            int pEndDir = Geometry.vecDir(p_path[size - 3], p_path[size - 2], p_path[size - 1]);
                            if ((cEndDir != 0) && (cEndDir == -pEndDir)) startCornerSide = -cEndDir;
                        }
                    }

                    if (pointOrders != null) {
                        boolean reversed = false;
                        int startPt = front_same ? 0 : 1;
                        assert size > (startPt + 1);
                        if (startCornerSide > 0) reversed = !reversed;
                        int prevDir = 0;
                        int adj_size = size - (back_same ? 0 : 1);
                        for (int i = startPt; i < adj_size; ++i) {
                            Point an = c_path[i];
                            Point bn = p_path[i];
                            assert an.equals(bn);
                            if (i > startPt) {
                                Point ap = c_path[i - 1];
                                Point bp = p_path[i - 1];
                                int thisDir = segDir(ap, an);
                                if (prevDir == 0) {
                                    if (thisDir > 0) reversed = !reversed;
                                } else if (thisDir != prevDir) reversed = !reversed;
                                int orientation = (ap.x == an.x) ? 0 : 1;
                                PtOrder ptOrderAn = pointOrders.computeIfAbsent(an, _ -> new PtOrder());
                                ptOrderAn.addOrderedPoints(orientation, new Pair<>(bn, polyConnRef), new Pair<>(an, connConnRef), reversed);
                                assert ap.equals(bp);
                                PtOrder ptOrderAp = pointOrders.computeIfAbsent(ap, _ -> new PtOrder());
                                ptOrderAp.addOrderedPoints(orientation, new Pair<>(bp, polyConnRef), new Pair<>(ap, connConnRef), reversed);
                                prevDir = thisDir;
                            }
                        }
                    }
                    crossingFlags |= CROSSING_TOUCHES;
                } else if (cIndex >= 2) {
                    Point b0 = poly.ps.get(((j - 2) + poly_size) % poly_size);
                    Point a0 = conn.ps.get(cIndex - 2);
                    if (Geometry.cornerSide(a0, a1, a2, b0) != Geometry.cornerSide(a0, a1, a2, b2)) {
                        crossingCount += 1;
                        // C++ connector.cpp:2372-2375
                        if (crossingPoints != null) {
                            crossingPoints.add(new Point(a1));
                        }
                    }
                    if (pointOrders != null && polyIsOrthogonal && connIsOrthogonal) {
                        boolean reversedX = ((a0.x < a1.x) || (a2.x < a1.x));
                        boolean reversedY = ((a0.y < a1.y) || (a2.y < a1.y));
                        PtOrder ptOrderB1 = pointOrders.computeIfAbsent(b1, _ -> new PtOrder());
                        ptOrderB1.addOrderedPoints(0, new Pair<>(b1, polyConnRef), new Pair<>(a1, connConnRef), !reversedX);
                        ptOrderB1.addOrderedPoints(1, new Pair<>(b1, polyConnRef), new Pair<>(a1, connConnRef), !reversedY);
                    }
                    crossingFlags |= CROSSING_TOUCHES;
                }
            } else {
                if (polyIsOrthogonal && connIsOrthogonal) continue;
                var result = Geometry.segmentIntersectPoint(a1, a2, b1, b2);
                if (result.first == Geometry.Intersection.DO_INTERSECT) {
                    Point cPt = result.second;
                    if (!polyIsConn && (a1.equals(cPt) || a2.equals(cPt) || b1.equals(cPt) || b2.equals(cPt))) continue;
                    crossingCount += 1;
                    // C++ connector.cpp:2468-2471
                    if (crossingPoints != null) {
                        crossingPoints.add(new Point(cPt));
                    }
                }
            }
        }
    }

    private static double pathLength(Point[] c_path, Point[] p_path, int size) {
        double length = 0;
        for (int ind = 1; ind < size; ++ind) {
            if (c_path[ind - 1].equals(p_path[ind - 1]) && c_path[ind].equals(p_path[ind])) {
                length += Geometry.manhattanDist(c_path[ind - 1], c_path[ind]);
            }
        }
        return length;
    }

    private static boolean posInlineWithConnEndSegs(double pos, int dim, Polygon conn, Polygon poly) {
        int pLast = poly.size() - 1;
        int cLast = conn.size() - 1;
        return ((((pos == poly.ps.get(0).get(dim)) && (pos == poly.ps.get(1).get(dim))) ||
                 ((pos == poly.ps.get(pLast).get(dim)) && (pos == poly.ps.get(pLast - 1).get(dim)))) &&
                (((pos == conn.ps.get(0).get(dim)) && (pos == conn.ps.get(1).get(dim))) ||
                 ((pos == conn.ps.get(cLast).get(dim)) && (pos == conn.ps.get(cLast - 1).get(dim)))));
    }

    static void splitBranchingSegments(Polygon poly, boolean polyIsConn, Polygon conn, double tolerance) {
        for (int i = 1; i < conn.ps.size(); ++i) {
            for (int j = 0; j < poly.ps.size(); ) {
                if (polyIsConn && (j == 0)) {
                    ++j;
                    continue;
                }
                Point c0 = conn.ps.get(i - 1);
                Point c1 = conn.ps.get(i);
                Point p0 = (j == 0) ? poly.ps.getLast() : poly.ps.get(j - 1);
                Point p1 = poly.ps.get(j);

                if ((i == 1) && Geometry.pointOnLine(p0, p1, c0, tolerance)) {
                    c0.vn = midVertexNumber(p0, p1, c0);
                    poly.ps.add(j, new Point(c0));
                    if (j > 0) --j;
                    continue;
                }
                if (Geometry.pointOnLine(p0, p1, c1, tolerance)) {
                    c1.vn = midVertexNumber(p0, p1, c1);
                    poly.ps.add(j, new Point(c1));
                    if (j > 0) --j;
                    continue;
                }
                if (polyIsConn && (j == 1) && Geometry.pointOnLine(c0, c1, p0, tolerance)) {
                    p0.vn = midVertexNumber(c0, c1, p0);
                    conn.ps.add(i, new Point(p0));
                    continue;
                }
                if (Geometry.pointOnLine(c0, c1, p1, tolerance)) {
                    p1.vn = midVertexNumber(c0, c1, p1);
                    conn.ps.add(i, new Point(p1));
                }
                ++j;
            }
        }
    }

    static int segDir(Point p1, Point p2) {
        int result = 1;
        if (p1.x == p2.x) {
            if (p2.y > p1.y) result = -1;
        } else if (p1.y == p2.y) {
            if (p2.x < p1.x) result = -1;
        }
        return result;
    }

    private static short midVertexNumber(Point p0, Point p1, Point c) {
        if (c.vn != Point.kUnassignedVertexNumber) return c.vn;
        if ((p0.vn >= 4) && (p0.vn < Point.kUnassignedVertexNumber)) return p0.vn;
        if ((p1.vn >= 4) && (p1.vn < Point.kUnassignedVertexNumber)) return p1.vn;
        if ((p0.vn < 4) && (p1.vn < 4)) {
            if (p0.vn != p1.vn) return p0.vn;
            short vnMid = p0.vn;
            return (short) (vnMid + 4);
        }
        assert (p0.x == p1.x) || (p0.y == p1.y);
        if (p0.vn != Point.kUnassignedVertexNumber) {
            if (p0.x == p1.x) {
                if ((p0.vn == 2) || (p0.vn == 3)) return 6;
                return 4;
            } else {
                if ((p0.vn == 0) || (p0.vn == 3)) return 7;
                return 5;
            }
        } else if (p1.vn != Point.kUnassignedVertexNumber) {
            if (p0.x == p1.x) {
                if ((p1.vn == 2) || (p1.vn == 3)) return 6;
                return 4;
            } else {
                if ((p1.vn == 0) || (p1.vn == 3)) return 7;
                return 5;
            }
        }
        return Point.kUnassignedVertexNumber;
    }
}
