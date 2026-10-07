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

import io.github.eckig.libavoid.scanline.Event;
import io.github.eckig.libavoid.scanline.EventType;
import io.github.eckig.libavoid.scanline.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Translated from orthogonal.cpp in libavoid C++.
 * Contains generateStaticOrthogonalVisGraph() and improveOrthogonalRoutes().
 */
final class OrthogonalRouter {

    // ScanVisDirFlag constants
    static final int VisDirNone = 0;
    static final int VisDirUp = 1;
    static final int VisDirDown = 2;

    private OrthogonalRouter() {}

    // =========================================================================
    // getPosVertInfDirections
    // =========================================================================

    static int getPosVertInfDirections(VertInf v, int dim) {
        if (dim == Point.XDIM) {
            int dirs = v.visDirections & (ConnDirFlag.ConnDirLeft | ConnDirFlag.ConnDirRight);
            if (dirs == (ConnDirFlag.ConnDirLeft | ConnDirFlag.ConnDirRight)) {
                return (VisDirDown | VisDirUp);
            } else if (dirs == ConnDirFlag.ConnDirLeft) {
                return VisDirDown;
            } else if (dirs == ConnDirFlag.ConnDirRight) {
                return VisDirUp;
            }
        } else if (dim == Point.YDIM) {
            int dirs = v.visDirections & (ConnDirFlag.ConnDirDown | ConnDirFlag.ConnDirUp);
            if (dirs == (ConnDirFlag.ConnDirDown | ConnDirFlag.ConnDirUp)) {
                return (VisDirDown | VisDirUp);
            } else if (dirs == ConnDirFlag.ConnDirDown) {
                return VisDirUp;
            } else if (dirs == ConnDirFlag.ConnDirUp) {
                return VisDirDown;
            }
        }
        return VisDirNone;
    }

    // =========================================================================
    // PosVertInf - position + vertex + direction info
    // =========================================================================

    static class PosVertInf implements Comparable<PosVertInf> {
        double pos;
        VertInf vert;
        int dirs;

        PosVertInf(double p, VertInf vI, int d) {
            this.pos = p;
            this.vert = vI;
            this.dirs = d;
        }

        PosVertInf(double p, VertInf vI) {
            this(p, vI, VisDirNone);
        }

        @Override
        public int compareTo(PosVertInf rhs) {
            if (pos != rhs.pos) {
                return Double.compare(pos, rhs.pos);
            }
            // We only need one dummy orthog vertex at each position,
            // so multiples can be seen as equal here.
            if (vert.id.equals(VertInf.dummyOrthogID) && rhs.vert.id.equals(VertInf.dummyOrthogID)) {
                return 0;
            }
            if (!vert.id.equals(rhs.vert.id)) {
                return vert.id.compareTo(rhs.vert.id);
            }
            return Integer.compare(dirs, rhs.dirs);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PosVertInf)) return false;
            return compareTo((PosVertInf) o) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos, vert.id, dirs);
        }
    }

    // =========================================================================
    // CmpVertInf - comparator for VertInf in ordered sets
    // =========================================================================

    static final Comparator<VertInf> CMP_VERT_INF = (u, v) -> {
        if (u.point.x != v.point.x) {
            return Double.compare(u.point.x, v.point.x);
        } else if (u.point.y != v.point.y) {
            return Double.compare(u.point.y, v.point.y);
        }
        return Integer.compare(u.nodeId, v.nodeId);
    };

    // =========================================================================
    // LineSegment
    // =========================================================================

    static class LineSegment implements Comparable<LineSegment> {
        double begin;
        double finish;
        double pos;
        boolean shapeSide;
        TreeSet<VertInf> vertInfs;
        TreeSet<PosVertInf> breakPoints;

        LineSegment(double b, double f, double p, boolean ss, VertInf bvi, VertInf fvi) {
            this.begin = b;
            this.finish = f;
            this.pos = p;
            this.shapeSide = ss;
            this.vertInfs = new TreeSet<>(CMP_VERT_INF);
            this.breakPoints = new TreeSet<>();
            assert begin <= finish;
            if (bvi != null) vertInfs.add(bvi);
            if (fvi != null) vertInfs.add(fvi);
        }

        LineSegment(double b, double f, double p) {
            this(b, f, p, false, null, null);
        }

        LineSegment(double bf, double p, VertInf bfvi) {
            this.begin = bf;
            this.finish = bf;
            this.pos = p;
            this.shapeSide = false;
            this.vertInfs = new TreeSet<>(CMP_VERT_INF);
            this.breakPoints = new TreeSet<>();
            if (bfvi != null) vertInfs.add(bfvi);
        }

        @Override
        public int compareTo(LineSegment rhs) {
            if (begin != rhs.begin) return Double.compare(begin, rhs.begin);
            if (pos != rhs.pos) return Double.compare(pos, rhs.pos);
            if (finish != rhs.finish) return Double.compare(finish, rhs.finish);
            return 0;
        }

        boolean overlaps(LineSegment rhs) {
            if ((begin == rhs.begin) && (pos == rhs.pos) && (finish == rhs.finish)) {
                return true;
            }
            if (pos == rhs.pos) {
                if (((begin >= rhs.begin) && (begin <= rhs.finish)) ||
                    ((rhs.begin >= begin) && (rhs.begin <= finish))) {
                    return true;
                }
            }
            return false;
        }

        void mergeVertInfs(LineSegment segment) {
            begin = Math.min(begin, segment.begin);
            finish = Math.max(finish, segment.finish);
            vertInfs.addAll(segment.vertInfs);
        }

        VertInf beginVertInf() {
            if (vertInfs.isEmpty()) return null;
            VertInf inf = vertInfs.first();
            if (((inf.point.y == begin) && (inf.point.x == pos)) ||
                ((inf.point.x == begin) && (inf.point.y == pos))) {
                return inf;
            }
            return null;
        }

        VertInf finishVertInf() {
            if (vertInfs.isEmpty()) return null;
            VertInf inf = vertInfs.last();
            if (((inf.point.y == finish) && (inf.point.x == pos)) ||
                ((inf.point.x == finish) && (inf.point.y == pos))) {
                return inf;
            }
            return null;
        }

        VertInf commitPositionX(Router router, double posX) {
            VertInf found = null;
            for (VertInf v : vertInfs) {
                if (v.point.x == posX) {
                    found = v;
                    break;
                }
            }
            if (found == null) {
                found = new VertInf(router, VertInf.dummyOrthogID, new Point(posX, pos));
                vertInfs.add(found);
            }
            return found;
        }

        void horiCommitBegin(Router router, VertInf vert) {
            if (vert != null) {
                vertInfs.add(vert);
            }
            if (vertInfs.isEmpty() || (vertInfs.first().point.x != begin)) {
                if (begin != -Double.MAX_VALUE) {
                    vertInfs.add(new VertInf(router, VertInf.dummyOrthogID, new Point(begin, pos)));
                }
            }
        }

        void horiCommitBegin(Router router) {
            horiCommitBegin(router, null);
        }

        void horiCommitFinish(Router router, VertInf vert) {
            if (vert != null) {
                vertInfs.add(vert);
            }
            if (vertInfs.isEmpty() || (vertInfs.last().point.x != finish)) {
                if (finish != Double.MAX_VALUE) {
                    vertInfs.add(new VertInf(router, VertInf.dummyOrthogID, new Point(finish, pos)));
                }
            }
        }

        void horiCommitFinish(Router router) {
            horiCommitFinish(router, null);
        }

        // Returns the first of the intersection points occurring at finishPos.
        VertInf addSegmentsUpTo(double finishPos) {
            VertInf firstIntersectionPt = null;
            for (VertInf v : vertInfs) {
                if (v.point.x > finishPos) break;
                breakPoints.add(new PosVertInf(v.point.x, v,
                        getPosVertInfDirections(v, Point.XDIM)));
                if (firstIntersectionPt == null && v.point.x == finishPos) {
                    firstIntersectionPt = v;
                }
            }
            return firstIntersectionPt;
        }

        void addEdgeHorizontal(Router router) {
            horiCommitBegin(router);
            horiCommitFinish(router);
            addSegmentsUpTo(finish);
        }

        void setLongRangeVisibilityFlags(int dim) {
            // First, travel in one direction
            boolean seenConnPt = false;
            boolean seenShapeEdge = false;
            for (PosVertInf nvert : breakPoints) {
                int mask = 0;
                if (dim == Point.XDIM) {
                    if (seenConnPt) mask |= VertInf.XL_CONN;
                    if (seenShapeEdge) mask |= VertInf.XL_EDGE;
                } else {
                    if (seenConnPt) mask |= VertInf.YL_CONN;
                    if (seenShapeEdge) mask |= VertInf.YL_EDGE;
                }
                nvert.vert.orthogVisPropFlags |= mask;
                if (nvert.vert.id.isConnPt()) seenConnPt = true;
                if (nvert.vert.id.isOrthShapeEdge()) seenShapeEdge = true;
            }
            // Then in the other direction
            seenConnPt = false;
            seenShapeEdge = false;
            for (PosVertInf rvert : breakPoints.descendingSet()) {
                int mask = 0;
                if (dim == Point.XDIM) {
                    if (seenConnPt) mask |= VertInf.XH_CONN;
                    if (seenShapeEdge) mask |= VertInf.XH_EDGE;
                } else {
                    if (seenConnPt) mask |= VertInf.YH_CONN;
                    if (seenShapeEdge) mask |= VertInf.YH_EDGE;
                }
                rvert.vert.orthogVisPropFlags |= mask;
                if (rvert.vert.id.isConnPt()) seenConnPt = true;
                if (rvert.vert.id.isOrthShapeEdge()) seenShapeEdge = true;
            }
        }

        TreeSet<VertInf> addEdgeHorizontalTillIntersection(Router router, LineSegment vertLine) {
            TreeSet<VertInf> intersectionSet = new TreeSet<>(CMP_VERT_INF);
            horiCommitBegin(router);
            commitPositionX(router, vertLine.pos);
            addSegmentsUpTo(vertLine.pos);

            // Add the intersection points to intersectionSet.
            // Find vertInfs at vertLine.pos position
            for (VertInf v : vertInfs) {
                if (v.point.x == vertLine.pos) {
                    intersectionSet.add(v);
                } else if (v.point.x > vertLine.pos) {
                    break;
                }
            }

            // Adjust segment to remove processed portion.
            begin = vertLine.pos;
            // Remove all vertices before the intersection
            vertInfs.headSet(intersectionSet.isEmpty() ? vertInfs.first() : intersectionSet.first(), false).clear();

            return intersectionSet;
        }

        void insertBreakpointsBegin(Router router, LineSegment vertLine) {
            VertInf vert = null;
            if (pos == vertLine.begin && vertLine.beginVertInf() != null) {
                vert = vertLine.beginVertInf();
            } else if (pos == vertLine.finish && vertLine.finishVertInf() != null) {
                vert = vertLine.finishVertInf();
            }
            horiCommitBegin(router, vert);

            for (VertInf v : vertInfs) {
                if (v.point.x == begin) {
                    vertLine.breakPoints.add(new PosVertInf(pos, v,
                            getPosVertInfDirections(v, Point.YDIM)));
                }
            }
        }

        void insertBreakpointsFinish(Router router, LineSegment vertLine) {
            VertInf vert = null;
            if (pos == vertLine.begin && vertLine.beginVertInf() != null) {
                vert = vertLine.beginVertInf();
            } else if (pos == vertLine.finish && vertLine.finishVertInf() != null) {
                vert = vertLine.finishVertInf();
            }
            horiCommitFinish(router, vert);

            for (VertInf v : vertInfs) {
                if (v.point.x == finish) {
                    vertLine.breakPoints.add(new PosVertInf(pos, v,
                            getPosVertInfDirections(v, Point.YDIM)));
                }
            }
        }

        void generateVisibilityEdgesFromBreakpointSet(Router router, int dim) {
            if (breakPoints.isEmpty() || (breakPoints.first().pos > begin)) {
                if (begin == -Double.MAX_VALUE) {
                    assert !breakPoints.isEmpty();
                    begin = breakPoints.first().pos;
                } else {
                    Point point = new Point(pos, pos);
                    point.set(dim, begin);
                    VertInf vert = new VertInf(router, VertInf.dummyOrthogID, point);
                    breakPoints.add(new PosVertInf(begin, vert));
                }
            }
            if (breakPoints.isEmpty() || (breakPoints.last().pos < finish)) {
                if (finish == Double.MAX_VALUE) {
                    finish = breakPoints.last().pos;
                } else {
                    Point point = new Point(pos, pos);
                    point.set(dim, finish);
                    VertInf vert = new VertInf(router, VertInf.dummyOrthogID, point);
                    breakPoints.add(new PosVertInf(finish, vert));
                }
            }

            setLongRangeVisibilityFlags(dim);

            final boolean orthogonal = true;
            // Convert breakPoints to an ArrayList for indexed access,
            // faithfully mirroring the C++ iterator pattern.
            ArrayList<PosVertInf> bpList = new ArrayList<>(breakPoints);
            if (bpList.isEmpty()) return;

            int lastIdx = 0;
            int vertIdx = 0;
            // C++: for (vert = last = breakPoints.begin(); vert != breakPoints.end();)
            while (vertIdx < bpList.size()) {
                int firstPrevIdx = lastIdx;
                PosVertInf vert = bpList.get(vertIdx);
                PosVertInf last = bpList.get(lastIdx);

                // C++: while (last->vert->point[dim] != vert->vert->point[dim])
                while (last.vert.point.get(dim) != vert.vert.point.get(dim)) {
                    if (vert.vert.id.isConnPt() && last.vert.id.isConnPt()) {
                        // Walk backward from last to find non-connector endpoint
                        int sideIdx = lastIdx;
                        while (bpList.get(sideIdx).vert.id.isConnPt()) {
                            if (sideIdx == 0) break;
                            --sideIdx;
                        }
                        boolean canSeeDown = (vert.dirs & VisDirDown) != 0;
                        if (canSeeDown && !bpList.get(sideIdx).vert.id.isConnPt()) {
                            EdgeInf edge = new EdgeInf(bpList.get(sideIdx).vert, vert.vert, orthogonal);
                            edge.setDist(vert.vert.point.get(dim) - bpList.get(sideIdx).vert.point.get(dim));
                        }

                        // Walk forward from vert to find non-connector endpoint
                        int sideAfterIdx = vertIdx;
                        while (sideAfterIdx < bpList.size() && bpList.get(sideAfterIdx).vert.id.isConnPt()) {
                            ++sideAfterIdx;
                        }
                        boolean canSeeUp = (last.dirs & VisDirUp) != 0;
                        if (canSeeUp && sideAfterIdx < bpList.size()) {
                            EdgeInf edge = new EdgeInf(last.vert, bpList.get(sideAfterIdx).vert, orthogonal);
                            edge.setDist(bpList.get(sideAfterIdx).vert.point.get(dim) - last.vert.point.get(dim));
                        }
                    }

                    // The normal case.
                    boolean generateEdge = true;
                    if (last.vert.id.isConnPt() && (last.dirs & VisDirUp) == 0) {
                        generateEdge = false;
                    } else if (vert.vert.id.isConnPt() && (vert.dirs & VisDirDown) == 0) {
                        generateEdge = false;
                    }
                    if (generateEdge) {
                        EdgeInf edge = new EdgeInf(last.vert, vert.vert, orthogonal);
                        edge.setDist(vert.vert.point.get(dim) - last.vert.point.get(dim));
                    }

                    // C++: ++last;
                    ++lastIdx;
                    last = bpList.get(lastIdx);
                }

                // C++: ++vert;
                ++vertIdx;

                // C++: if ((vert != end) && (last->point[dim] == vert->point[dim]))
                if (vertIdx < bpList.size() &&
                        bpList.get(lastIdx).vert.point.get(dim) == bpList.get(vertIdx).vert.point.get(dim)) {
                    // Still looking at same pair, reset prev pointer
                    lastIdx = firstPrevIdx;
                }
                // else: vert moved to new position group, last is in right place
            }
        }
    }

    // =========================================================================
    // SegmentListWrapper
    // =========================================================================

    static class SegmentListWrapper {
        final List<LineSegment> list = new ArrayList<>();

        LineSegment insert(LineSegment segment) {
            LineSegment found = null;
            for (int idx = 0; idx < list.size(); ++idx) {
                LineSegment curr = list.get(idx);
                if (curr.overlaps(segment)) {
                    if (found != null) {
                        // C++ merges the first overlapping segment into this
                        // later one, erases the earlier segment, then keeps
                        // this later segment as the found segment.
                        curr.mergeVertInfs(found);
                        int foundIdx = list.indexOf(found);
                        list.remove(foundIdx);
                        if (foundIdx < idx) {
                            --idx;
                        }
                        found = curr;
                    } else {
                        curr.mergeVertInfs(segment);
                        found = curr;
                    }
                }
            }
            if (found == null) {
                list.add(segment);
                return list.getLast();
            }
            return found;
        }
    }

    // =========================================================================
    // intersectSegments
    // =========================================================================

    static void intersectSegments(Router router, List<LineSegment> segments,
            LineSegment vertLine) {
        assert !segments.isEmpty();
        for (Iterator<LineSegment> it = segments.iterator(); it.hasNext(); ) {
            LineSegment horiLine = it.next();

            boolean inVertSegRegion = ((vertLine.begin <= horiLine.pos) &&
                                       (vertLine.finish >= horiLine.pos));

            if (vertLine.pos < horiLine.begin) {
                continue;
            } else if (vertLine.pos == horiLine.begin) {
                if (inVertSegRegion) {
                    horiLine.insertBreakpointsBegin(router, vertLine);
                }
            } else if (vertLine.pos == horiLine.finish) {
                if (inVertSegRegion) {
                    horiLine.addEdgeHorizontal(router);
                    horiLine.insertBreakpointsFinish(router, vertLine);
                    horiLine.generateVisibilityEdgesFromBreakpointSet(router, Point.XDIM);
                    it.remove();
                    continue;
                }
            } else if (vertLine.pos > horiLine.finish) {
                horiLine.addEdgeHorizontal(router);
                horiLine.generateVisibilityEdgesFromBreakpointSet(router, Point.XDIM);
                it.remove();
                continue;
            } else {
                assert vertLine.pos > horiLine.begin;
                assert vertLine.pos < horiLine.finish;

                if (inVertSegRegion) {
                    TreeSet<VertInf> intersectionVerts =
                            horiLine.addEdgeHorizontalTillIntersection(router, vertLine);
                    for (VertInf v : intersectionVerts) {
                        vertLine.breakPoints.add(new PosVertInf(horiLine.pos, v,
                                getPosVertInfDirections(v, Point.YDIM)));
                    }
                }
            }
        }

        vertLine.generateVisibilityEdgesFromBreakpointSet(router, Point.YDIM);
    }

    // =========================================================================
    // processEventVert
    // =========================================================================

    static void processEventVert(Router router, TreeSet<Node> scanline,
            SegmentListWrapper segments, Event e, int pass) {
        Node v = e.v;

        if (((pass == 1) && (e.type == EventType.Open)) ||
            ((pass == 2) && (e.type == EventType.ConnPoint))) {
            boolean added = scanline.add(v);
            assert added;

            // Work out neighbours
            Node higher = scanline.higher(v);
            Node lower = scanline.lower(v);
            if (lower != null) {
                v.firstAbove = lower;
                lower.firstBelow = v;
            }
            if (higher != null) {
                v.firstBelow = higher;
                higher.firstAbove = v;
            }
        }

        if (pass == 2) {
            if ((e.type == EventType.Open) || (e.type == EventType.Close)) {
                double lineY = (e.type == EventType.Open) ? v.min[Point.YDIM] : v.max[Point.YDIM];

                double minShape = v.min[Point.XDIM];
                double maxShape = v.max[Point.XDIM];
                double[] limits = new double[4];
                v.findFirstPointAboveAndBelow(Point.XDIM, lineY, limits);
                double minLimit = limits[0];
                double maxLimit = limits[1];
                double minLimitMax = limits[2];
                double maxLimitMin = limits[3];

                if (minLimitMax >= maxLimitMin) {
                    VertInf vI1 = new VertInf(router, VertInf.dummyOrthogShapeID,
                            new Point(minShape, lineY));
                    VertInf vI2 = new VertInf(router, VertInf.dummyOrthogShapeID,
                            new Point(maxShape, lineY));

                    if (minLimit < minShape) {
                        segments.insert(new LineSegment(minLimit, minShape, lineY,
                                true, null, vI1));
                    }
                    segments.insert(new LineSegment(minShape, maxShape, lineY,
                            true, vI1, vI2));
                    if (maxShape < maxLimit) {
                        segments.insert(new LineSegment(maxShape, maxLimit, lineY,
                                true, vI2, null));
                    }
                } else {
                    if ((minLimitMax > minLimit) && (minLimitMax >= minShape)) {
                        LineSegment line = segments.insert(
                                new LineSegment(minLimit, minLimitMax, lineY, true, null, null));
                        VertInf vI1 = new VertInf(router, VertInf.dummyOrthogShapeID,
                                new Point(minShape, lineY));
                        line.vertInfs.add(vI1);
                    }
                    if ((maxLimitMin < maxLimit) && (maxLimitMin <= maxShape)) {
                        LineSegment line = segments.insert(
                                new LineSegment(maxLimitMin, maxLimit, lineY, true, null, null));
                        VertInf vI2 = new VertInf(router, VertInf.dummyOrthogShapeID,
                                new Point(maxShape, lineY));
                        line.vertInfs.add(vI2);
                    }
                }
            } else if (e.type == EventType.ConnPoint) {
                VertInf centreVert = e.v.c;
                Point cp = centreVert.point;

                double minLimit = v.firstPointAbove(Point.XDIM);
                double maxLimit = v.firstPointBelow(Point.XDIM);
                boolean inShape = v.isInsideShape(Point.XDIM);

                LineSegment line1 = null, line2 = null;
                if ((centreVert.visDirections & ConnDirFlag.ConnDirLeft) != 0 && (minLimit < cp.x)) {
                    line1 = segments.insert(new LineSegment(minLimit, cp.x, e.pos,
                            true, null, centreVert));
                }
                if ((centreVert.visDirections & ConnDirFlag.ConnDirRight) != 0 && (cp.x < maxLimit)) {
                    line2 = segments.insert(new LineSegment(cp.x, maxLimit, e.pos,
                            true, centreVert, null));
                    line1 = null;
                }
                if (line1 == null && line2 == null) {
                    segments.insert(new LineSegment(cp.x, e.pos, centreVert));
                }

                if (!inShape) {
                    if (line1 != null || line2 != null) {
                        VertInf cent = new VertInf(router, VertInf.dummyOrthogID, cp);
                        if (line1 != null) line1.vertInfs.add(cent);
                        if (line2 != null) line2.vertInfs.add(cent);
                    }
                }
            }
        }

        if (((pass == 3) && (e.type == EventType.Close)) ||
            ((pass == 2) && (e.type == EventType.ConnPoint))) {
            Node l = v.firstAbove, r = v.firstBelow;
            if (l != null) l.firstBelow = v.firstBelow;
            if (r != null) r.firstAbove = v.firstAbove;
            scanline.remove(v);
        }
    }

    // =========================================================================
    // processEventHori
    // =========================================================================

    static void processEventHori(Router router, TreeSet<Node> scanline,
            SegmentListWrapper segments, Event e, int pass) {
        Node v = e.v;

        if (((pass == 1) && (e.type == EventType.Open)) ||
            ((pass == 2) && (e.type == EventType.ConnPoint))) {
            boolean added = scanline.add(v);
            assert added;

            Node higher = scanline.higher(v);
            Node lower = scanline.lower(v);
            if (lower != null) {
                v.firstAbove = lower;
                lower.firstBelow = v;
            }
            if (higher != null) {
                v.firstBelow = higher;
                higher.firstAbove = v;
            }
        }

        if (pass == 2) {
            if ((e.type == EventType.Open) || (e.type == EventType.Close)) {
                double lineX = (e.type == EventType.Open) ? v.min[Point.XDIM] : v.max[Point.XDIM];

                double minShape = v.min[Point.YDIM];
                double maxShape = v.max[Point.YDIM];
                double[] limits = new double[4];
                v.findFirstPointAboveAndBelow(Point.YDIM, lineX, limits);
                double minLimit = limits[0];
                double maxLimit = limits[1];
                double minLimitMax = limits[2];
                double maxLimitMin = limits[3];

                if (minLimitMax >= maxLimitMin) {
                    LineSegment line = segments.insert(
                            new LineSegment(minLimit, maxLimit, lineX));
                    VertInf vI1 = new VertInf(router, VertInf.dummyOrthogShapeID,
                            new Point(lineX, minShape));
                    VertInf vI2 = new VertInf(router, VertInf.dummyOrthogShapeID,
                            new Point(lineX, maxShape));
                    line.vertInfs.add(vI1);
                    line.vertInfs.add(vI2);
                } else {
                    if ((minLimitMax > minLimit) && (minLimitMax >= minShape)) {
                        LineSegment line = segments.insert(
                                new LineSegment(minLimit, minLimitMax, lineX));
                        VertInf vI1 = new VertInf(router, VertInf.dummyOrthogShapeID,
                                new Point(lineX, minShape));
                        line.vertInfs.add(vI1);
                    }
                    if ((maxLimitMin < maxLimit) && (maxLimitMin <= maxShape)) {
                        LineSegment line = segments.insert(
                                new LineSegment(maxLimitMin, maxLimit, lineX));
                        VertInf vI2 = new VertInf(router, VertInf.dummyOrthogShapeID,
                                new Point(lineX, maxShape));
                        line.vertInfs.add(vI2);
                    }
                }
            } else if (e.type == EventType.ConnPoint) {
                VertInf centreVert = e.v.c;
                Point cp = centreVert.point;

                double minLimit = v.firstPointAbove(Point.YDIM);
                double maxLimit = v.firstPointBelow(Point.YDIM);

                if ((centreVert.visDirections & ConnDirFlag.ConnDirUp) != 0 && (minLimit < cp.y)) {
                    segments.insert(new LineSegment(minLimit, cp.y, e.pos));
                }
                if ((centreVert.visDirections & ConnDirFlag.ConnDirDown) != 0 && (cp.y < maxLimit)) {
                    segments.insert(new LineSegment(cp.y, maxLimit, e.pos));
                }
            }
        }

        if (((pass == 3) && (e.type == EventType.Close)) ||
            ((pass == 2) && (e.type == EventType.ConnPoint))) {
            Node l = v.firstAbove, r = v.firstBelow;
            if (l != null) l.firstBelow = v.firstBelow;
            if (r != null) r.firstAbove = v.firstAbove;
            scanline.remove(v);
        }
    }

    // =========================================================================
    // fixConnectionPointVisibilityOnOutsideOfVisibilityGraph
    // =========================================================================

    static void fixConnectionPointVisibilityOnOutsideOfVisibilityGraph(
            Event[] events, int totalEvents, int addedVisibility) {
        if (totalEvents > 0) {
            double firstPos = events[0].pos;
            int index = 0;
            while (index < totalEvents) {
                if (events[index].pos > firstPos) break;
                if (events[index].v.c != null) {
                    events[index].v.c.visDirections |= addedVisibility;
                }
                ++index;
            }
            index = 0;
            double lastPos = events[totalEvents - 1].pos;
            while (index < totalEvents) {
                int revIndex = totalEvents - 1 - index;
                if (events[revIndex].pos < lastPos) break;
                if (events[revIndex].v.c != null) {
                    events[revIndex].v.c.visDirections |= addedVisibility;
                }
                ++index;
            }
        }
    }

    // =========================================================================
    // generateStaticOrthogonalVisGraph
    // =========================================================================

    static void generateStaticOrthogonalVisGraph(Router router) {
        final int n = router.m_obstacles.size();
        final int cpn = router.vertices.connsSize();
        // Set up the events for the vertical sweep.
        int totalEvents = (2 * n) + cpn;
        Event[] events = new Event[totalEvents];
        int ctr = 0;
        Iterator<Obstacle> obstacleIt = router.m_obstacles.iterator();
        for (int i = 0; i < n; i++) {
            Obstacle obstacle = obstacleIt.next();
            if (obstacle instanceof JunctionRef junction) {
                if (!junction.positionFixed()) {
                    totalEvents -= 2;
                    continue;
                }
            }

            Box bbox = obstacle.routingBox();
            double midX = bbox.min.x + ((bbox.max.x - bbox.min.x) / 2);
            Node v = new Node(obstacle, midX);
            events[ctr++] = new Event(EventType.Open, v, bbox.min.y);
            events[ctr++] = new Event(EventType.Close, v, bbox.max.y);
        }

        for (VertInf curr = router.vertices.connsBegin();
             curr != null && (curr != router.vertices.shapesBegin());
             curr = curr.lstNext) {
            if (curr.visDirections == ConnDirFlag.ConnDirNone) {
                --totalEvents;
                continue;
            }
            Point point = curr.point;
            Node v = new Node(curr, point.x);
            events[ctr++] = new Event(EventType.ConnPoint, v, point.y);
        }

        // Trim events array if some were skipped
        if (ctr < events.length) {
            events = Arrays.copyOf(events, ctr);
            totalEvents = ctr;
        }

        Arrays.sort(events, 0, totalEvents, (a, b) -> {
            if (a.pos != b.pos) return Double.compare(a.pos, b.pos);
            return Integer.compare(a.type.ordinal(), b.type.ordinal());
        });

        fixConnectionPointVisibilityOnOutsideOfVisibilityGraph(events, totalEvents,
                (ConnDirFlag.ConnDirLeft | ConnDirFlag.ConnDirRight));

        // Process the vertical sweep -- creating candidate horizontal edges.
        SegmentListWrapper segments = new SegmentListWrapper();
        TreeSet<Node> scanline = new TreeSet<>((a, b) -> {
            if (a.pos != b.pos) return Double.compare(a.pos, b.pos);
            return Integer.compare(a.nodeId, b.nodeId);
        });

        double thisPos = (totalEvents > 0) ? events[0].pos : 0;
        int posStartIndex = 0;
        int posFinishIndex;
        for (int i = 0; i <= totalEvents; ++i) {

            if ((i == totalEvents) || (events[i].pos != thisPos)) {
                posFinishIndex = i;
                for (int pass = 2; pass <= 3; ++pass) {
                    for (int j = posStartIndex; j < posFinishIndex; ++j) {
                        processEventVert(router, scanline, segments, events[j], pass);
                    }
                }

                if (i == totalEvents) break;

                thisPos = events[i].pos;
                posStartIndex = i;
            }

            final int pass = 1;
            processEventVert(router, scanline, segments, events[i], pass);
        }
        assert scanline.isEmpty();

        segments.list.sort(null);

        // Set up the events for the horizontal sweep.
        SegmentListWrapper vertSegments = new SegmentListWrapper();
        ctr = 0;
        obstacleIt = router.m_obstacles.iterator();
        for (int i = 0; i < n; i++) {
            Obstacle obstacle = obstacleIt.next();
            if (obstacle instanceof JunctionRef junction) {
                if (!junction.positionFixed()) {
                    continue;
                }
            }
            Box bbox = obstacle.routingBox();
            double midY = bbox.min.y + ((bbox.max.y - bbox.min.y) / 2);
            Node v = new Node(obstacle, midY);
            events[ctr++] = new Event(EventType.Open, v, bbox.min.x);
            events[ctr++] = new Event(EventType.Close, v, bbox.max.x);
        }
        for (VertInf curr = router.vertices.connsBegin();
             curr != null && (curr != router.vertices.shapesBegin());
             curr = curr.lstNext) {
            if (curr.visDirections == ConnDirFlag.ConnDirNone) {
                continue;
            }
            Point point = curr.point;
            Node v = new Node(curr, point.y);
            events[ctr++] = new Event(EventType.ConnPoint, v, point.x);
        }
        totalEvents = ctr;
        Arrays.sort(events, 0, totalEvents, (a, b) -> {
            if (a.pos != b.pos) return Double.compare(a.pos, b.pos);
            return Integer.compare(a.type.ordinal(), b.type.ordinal());
        });

        fixConnectionPointVisibilityOnOutsideOfVisibilityGraph(events, totalEvents,
                (ConnDirFlag.ConnDirUp | ConnDirFlag.ConnDirDown));

        // Process the horizontal sweep -- creating vertical visibility edges.
        thisPos = (totalEvents > 0) ? events[0].pos : 0;
        posStartIndex = 0;
        for (int i = 0; i <= totalEvents; ++i) {

            if ((i == totalEvents) || (events[i].pos != thisPos)) {
                posFinishIndex = i;
                for (int pass = 2; pass <= 3; ++pass) {
                    for (int j = posStartIndex; j < posFinishIndex; ++j) {
                        processEventHori(router, scanline, vertSegments, events[j], pass);
                    }
                }

                vertSegments.list.sort(null);
                for (LineSegment curr : vertSegments.list) {
                    intersectSegments(router, segments.list, curr);
                }
                vertSegments.list.clear();

                if (i == totalEvents) break;

                thisPos = events[i].pos;
                posStartIndex = i;
            }

            final int pass = 1;
            processEventHori(router, scanline, vertSegments, events[i], pass);
        }
        assert scanline.isEmpty();

        // Add portions of horizontal lines that are after the final vertical position.
        for (Iterator<LineSegment> it = segments.list.iterator(); it.hasNext(); ) {
            LineSegment horiLine = it.next();
            horiLine.addEdgeHorizontal(router);
            horiLine.generateVisibilityEdgesFromBreakpointSet(router, Point.XDIM);
            it.remove();
        }
    }
}
