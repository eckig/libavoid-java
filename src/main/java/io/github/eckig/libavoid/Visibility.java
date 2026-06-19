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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Translated from visibility.cpp — polyline visibility computation.
 *
 * The Visibility Sweep technique is based upon the method described
 * in Section 5.2 of:
 *     Lee, D.-T. (1978). Proximity and reachability in the plane.,
 *     PhD thesis, Department of Electrical Engineering,
 *     University of Illinois, Urbana, IL.
 *
 * Author(s): Michael Wybrow
 */
public class Visibility {

    private static final int AHEAD =  1;
    private static final int BEHIND = -1;

    // -----------------------------------------------------------------------
    // PointPair — helper for sorting vertices by angle and distance
    // Translated from visibility.cpp lines 171-208
    // -----------------------------------------------------------------------

    static class PointPair implements Comparable<PointPair> {
        VertInf vInf;
        double angle;
        double distance;
        Point centerPoint;

        PointPair(Point centerPoint, VertInf inf) {
            this.vInf = inf;
            this.centerPoint = centerPoint;
            Point diff = new Point(inf.point.x - centerPoint.x, inf.point.y - centerPoint.y);
            this.angle = Geometry.rotationalAngle(diff);
            this.distance = Geometry.euclideanDist(centerPoint, inf.point);
        }

        @Override
        public int compareTo(PointPair rhs) {
            // Firstly order by angle.
            if (angle == rhs.angle) {
                // If the points are collinear, then order them in increasing
                // distance from the point we are sweeping around.
                if (distance == rhs.distance) {
                    // If comparing two points at the same physical position,
                    // then order them by their VertIDs.
                    return vInf.id.compareTo(rhs.vInf.id);
                }
                return Double.compare(distance, rhs.distance);
            }
            return Double.compare(angle, rhs.angle);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PointPair rhs)) return false;
            return compareTo(rhs) == 0;
        }

        @Override
        public int hashCode() {
            return vInf.id.hashCode();
        }
    }

    // -----------------------------------------------------------------------
    // EdgePair — helper for tracking edges in the sweep line data structure
    // Translated from visibility.cpp lines 213-314
    // -----------------------------------------------------------------------

    static class EdgePair implements Comparable<EdgePair> {
        VertInf vInf1;
        VertInf vInf2;
        double dist1;
        double dist2;
        double angle;
        double angleDist;
        Point centerPoint;

        EdgePair(PointPair p1, VertInf v) {
            this.vInf1 = p1.vInf;
            this.vInf2 = v;
            this.dist1 = p1.distance;
            this.dist2 = Geometry.euclideanDist(v.point, p1.centerPoint);
            this.angle = p1.angle;
            this.angleDist = p1.distance;
            this.centerPoint = p1.centerPoint;
        }

        @Override
        public int compareTo(EdgePair rhs) {
            // angle must be equal when comparing in the sweep list
            if (angleDist == rhs.angleDist) {
                return Double.compare(dist2, rhs.dist2);
            }
            return Double.compare(angleDist, rhs.angleDist);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof EdgePair rhs)) return false;
            return ((vInf1.id.equals(rhs.vInf1.id) && vInf2.id.equals(rhs.vInf2.id)) ||
                    (vInf1.id.equals(rhs.vInf2.id) && vInf2.id.equals(rhs.vInf1.id)));
        }

        @Override
        public int hashCode() {
            // Order-independent hash
            return vInf1.id.hashCode() ^ vInf2.id.hashCode();
        }

        void setNegativeAngle() {
            angle = -1.0;
        }

        void setCurrAngle(PointPair p) {
            if (p.vInf.point.x == vInf1.point.x && p.vInf.point.y == vInf1.point.y) {
                angleDist = dist1;
                angle = p.angle;
            } else if (p.vInf.point.x == vInf2.point.x && p.vInf.point.y == vInf2.point.y) {
                angleDist = dist2;
                angle = p.angle;
            } else if (p.angle != angle) {
                // p.angle > angle (asserted in C++)
                angle = p.angle;
                var result = Geometry.rayIntersectPoint(vInf1.point, vInf2.point, centerPoint, p.vInf.point);
                if (result.first != Geometry.Intersection.DO_INTERSECT) {
                    // This can happen with points that appear to have the
                    // same angle but are at slightly different positions
                    angleDist = Math.min(dist1, dist2);
                } else {
                    Point pp = result.second;
                    angleDist = Geometry.euclideanDist(pp, centerPoint);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // sweepVisible — determines if a point is visible from the sweep center
    // Translated from visibility.cpp lines 350-433
    // -----------------------------------------------------------------------

    private static boolean sweepVisible(List<EdgePair> T, PointPair point,
                                        Set<Integer> onBorderIDs, int[] blocker) {
        if (T.isEmpty()) {
            // No blocking edges.
            return true;
        }

        Router router = point.vInf._router;
        boolean visible = true;

        // Find the first edge that doesn't share an endpoint with the point
        Iterator<EdgePair> closestIt = T.iterator();
        EdgePair closest = null;
        while (closestIt.hasNext()) {
            EdgePair ep = closestIt.next();
            if ((point.vInf.point.x == ep.vInf1.point.x && point.vInf.point.y == ep.vInf1.point.y) ||
                    (point.vInf.point.x == ep.vInf2.point.x && point.vInf.point.y == ep.vInf2.point.y)) {
                // If the ray intersects just the endpoint of a
                // blocking edge then ignore that edge.
                continue;
            }
            closest = ep;
            break;
        }
        if (closest == null) {
            return true;
        }

        if (point.vInf.id.isConnPt()) {
            // It's a connector endpoint, so we have to ignore
            // edges of containing shapes for determining visibility.
            Set<Integer> rss = router.contains.get(point.vInf.id);
            if (rss == null) rss = new HashSet<>();

            // Find the first non-containing edge
            EdgePair nonContaining = null;
            for (EdgePair ep : T) {
                if ((point.vInf.point.x == ep.vInf1.point.x && point.vInf.point.y == ep.vInf1.point.y) ||
                        (point.vInf.point.x == ep.vInf2.point.x && point.vInf.point.y == ep.vInf2.point.y)) {
                    continue;
                }
                if (!rss.contains(ep.vInf1.id.objID)) {
                    // This is not a containing edge so do the normal test and then stop.
                    if (point.distance > ep.angleDist) {
                        visible = false;
                    } else if (point.distance == ep.angleDist &&
                            onBorderIDs.contains(ep.vInf1.id.objID)) {
                        // Touching, but centerPoint is on another edge of
                        // this shape, so count as blocking.
                        visible = false;
                    }
                    nonContaining = ep;
                    break;
                }
                // This was a containing edge, so consider the next along.
            }
            if (!visible && nonContaining != null) {
                blocker[0] = nonContaining.vInf1.id.objID;
            }
        } else {
            // Just test to see if this point is closer than the closest
            // edge blocking this ray.
            if (point.distance > closest.angleDist) {
                visible = false;
            } else if (point.distance == closest.angleDist &&
                    onBorderIDs.contains(closest.vInf1.id.objID)) {
                // Touching, but centerPoint is on another edge of
                // this shape, so count as blocking.
                visible = false;
            }
            if (!visible) {
                blocker[0] = closest.vInf1.id.objID;
            }
        }

        return visible;
    }

    // -----------------------------------------------------------------------
    // vertexSweep — core rotational plane sweep algorithm
    // Translated from visibility.cpp lines 436-671
    // -----------------------------------------------------------------------

    static void vertexSweep(VertInf vert) {
        Router router = vert._router;

        VertID centerID = vert.id;
        Point centerPoint = vert.point;

        // List of shape (and maybe endpt) vertices, except p
        // Sort list, around
        TreeSet<PointPair> v = new TreeSet<>();

        // Initialise the vertex list
        Set<Integer> ss = router.contains.get(centerID);
        if (ss == null) ss = new HashSet<>();
        VertInf beginVert = router.vertices.connsBegin();
        VertInf endVert = router.vertices.end();
        for (VertInf inf = beginVert; inf != endVert; inf = inf.lstNext) {
            if (inf == vert) {
                // Don't include the center point itself.
                continue;
            } else if (inf.id.equals(VertInf.dummyOrthogID)) {
                // Don't include orthogonal dummy vertices.
                continue;
            }

            if (centerID.isConnPt() && ss.contains(inf.id.objID) &&
                    !inf.id.isConnPt()) {
                // Don't include edge points of containing shapes.
                continue;
            }

            if (inf.id.isConnPt()) {
                // Add connector endpoint.
                if (centerID.isConnPt()) {
                    if (inf.id.isConnectionPin()) {
                        v.add(new PointPair(centerPoint, inf));
                    } else if (centerID.isConnectionPin()) {
                        // Connection pins have visibility to everything.
                        v.add(new PointPair(centerPoint, inf));
                    } else if (inf.id.objID == centerID.objID) {
                        // Center is an endpoint, so only include the other
                        // endpoints or checkpoints from the matching connector.
                        v.add(new PointPair(centerPoint, inf));
                    }
                } else {
                    // Center is a shape vertex, so add all endpoint vertices.
                    v.add(new PointPair(centerPoint, inf));
                }
            } else {
                // Add shape vertex.
                v.add(new PointPair(centerPoint, inf));
            }
        }
        Set<Integer> onBorderIDs = new HashSet<>();

        // Add edges to T that intersect the initial ray.
        List<EdgePair> e = new ArrayList<>();
        Point xaxis = new Point(Double.MAX_VALUE, vert.point.y);
        for (PointPair t : v) {
            VertInf k = t.vInf;

            VertInf kPrev = k.shPrev;
            VertInf kNext = k.shNext;
            if (kPrev != null && kPrev != vert &&
                    (Geometry.vecDir(vert.point, xaxis, kPrev.point) == AHEAD)) {
                if (Geometry.segmentIntersect(vert.point, xaxis, kPrev.point, k.point)) {
                    EdgePair intPair = new EdgePair(t, kPrev);
                    e.add(intPair);
                }
                if (Geometry.pointOnLine(kPrev.point, k.point, vert.point)) {
                    // Record that centerPoint is on an obstacle line.
                    onBorderIDs.add(k.id.objID);
                }
            } else if (kNext != null && kNext != vert &&
                    (Geometry.vecDir(vert.point, xaxis, kNext.point) == AHEAD)) {
                if (Geometry.segmentIntersect(vert.point, xaxis, kNext.point, k.point)) {
                    EdgePair intPair = new EdgePair(t, kNext);
                    e.add(intPair);
                }
                if (Geometry.pointOnLine(kNext.point, k.point, vert.point)) {
                    // Record that centerPoint is on an obstacle line.
                    onBorderIDs.add(k.id.objID);
                }
            }
        }
        for (EdgePair c : e) {
            c.setNegativeAngle();
        }

        // Start the actual sweep.
        for (PointPair t : v) {
            VertInf currInf = t.vInf;
            VertID currID = currInf.id;
            Point currPt = currInf.point;

            double currDist = t.distance;

            EdgeInf edge = EdgeInf.existingEdge(vert, currInf);
            if (edge == null) {
                edge = new EdgeInf(vert, currInf);
            }

            for (EdgePair c : e) {
                c.setCurrAngle(t);
            }
            // Sort the sweep edge list
            ArrayList<EdgePair> eSorted = new ArrayList<>(e);
            eSorted.sort(null);
            e.clear();
            e.addAll(eSorted);

            // Check visibility.
            int[] blockerArr = {0};
            boolean currVisible = sweepVisible(e, t, onBorderIDs, blockerArr);
            int blocker = blockerArr[0];

            boolean cone1 = true, cone2 = true;
            if (!centerID.isConnPt()) {
                cone1 = Geometry.inValidRegion(router.IgnoreRegions,
                        vert.shPrev.point, centerPoint,
                        vert.shNext.point, currInf.point);
            }
            if (!currInf.id.isConnPt()) {
                cone2 = Geometry.inValidRegion(router.IgnoreRegions,
                        currInf.shPrev.point, currInf.point,
                        currInf.shNext.point, centerPoint);
            }

            if (!cone1 || !cone2) {
                if (router.InvisibilityGrph) {
                    edge.addBlocker(0);
                }
            } else {
                if (currVisible) {
                    edge.setDist(currDist);
                } else if (router.InvisibilityGrph) {
                    edge.addBlocker(blocker);
                }
            }

            if (!currID.isConnPt()) {
                // This is a shape edge

                if (currInf.shPrev != vert) {
                    Point prevPt = currInf.shPrev.point;
                    int prevDir = Geometry.vecDir(centerPoint, currPt, prevPt);
                    EdgePair prevPair = new EdgePair(t, currInf.shPrev);

                    if (prevDir == BEHIND) {
                        e.remove(prevPair);
                    } else if (prevDir == AHEAD) {
                        e.addFirst(prevPair);
                    }
                }

                if (currInf.shNext != vert) {
                    Point nextPt = currInf.shNext.point;
                    int nextDir = Geometry.vecDir(centerPoint, currPt, nextPt);
                    EdgePair nextPair = new EdgePair(t, currInf.shNext);

                    if (nextDir == BEHIND) {
                        e.remove(nextPair);
                    } else if (nextDir == AHEAD) {
                        e.addFirst(nextPair);
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // vertexVisibility — computes visibility for a single vertex (endpoint)
    // Translated from visibility.cpp lines 115-163
    // -----------------------------------------------------------------------

    /**
     * Computes visibility for a single vertex (endpoint).
     * Uses either Lee's algorithm (sweep) or naive approach.
     *
     * @param point      The vertex to compute visibility for.
     * @param partner    The partner vertex (other endpoint of connector), or null.
     * @param knownNew   Whether the edges are known to be new (not yet in graph).
     * @param genContains Whether to generate the contains set for this point.
     */
    public static void vertexVisibility(VertInf point, VertInf partner,
            boolean knownNew, boolean genContains) {
        Router router = point._router;
        VertID pID = point.id;

        // Make sure we're only doing ptVis for endpoints.
        assert pID.isConnPt();

        if (!router.InvisibilityGrph) {
            point.removeFromGraph(true);
        }

        if (genContains && pID.isConnPt()) {
            router.generateContains(point);
        }

        if (router.UseLeesAlgorithm) {
            vertexSweep(point);
        } else {
            VertInf shapesEnd = router.vertices.end();
            for (VertInf k = router.vertices.connsBegin(); k != shapesEnd; k = k.lstNext) {
                if (k.id.equals(VertInf.dummyOrthogID)) {
                    // Don't include orthogonal dummy vertices.
                    continue;
                } else if (k.id.isConnPt() && !k.id.isConnectionPin() &&
                        !(k.id.isConnCheckpoint() && k.id.objID == pID.objID)) {
                    // Include connection pins, but not connectors.
                    // Also include checkpoints with same ID as sweep point.
                    continue;
                }
                EdgeInf.checkEdgeVisibility(point, k, knownNew);
            }
            if (partner != null) {
                EdgeInf.checkEdgeVisibility(point, partner, knownNew);
            }
        }
    }
}
