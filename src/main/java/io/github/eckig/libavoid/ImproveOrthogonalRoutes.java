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

import io.github.eckig.libavoid.scanline.Scanline;
import io.github.eckig.libavoid.scanline.ShiftSegment;
import io.github.eckig.libavoid.vpsc.Constraint;
import io.github.eckig.libavoid.vpsc.IncSolver;
import io.github.eckig.libavoid.vpsc.Variable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Faithful translation of the ImproveOrthogonalRoutes class from orthogonal.cpp.
 * This performs segment nudging using VPSC (Variable Placement with Separation
 * Constraints) to spread overlapping connector segments apart.
 */
public class ImproveOrthogonalRoutes {

    static final double CHANNEL_MAX = 100000000;

    private final Router m_router;
    private final Map<Point, PtOrder> m_point_orders;
    private final Set<UnsignedPair> m_shared_path_connectors_with_common_endpoints;
    private final List<ShiftSegment> m_segment_list;

    public ImproveOrthogonalRoutes(Router router) {
        this.m_router = router;
        this.m_point_orders = new HashMap<>();
        this.m_shared_path_connectors_with_common_endpoints = new HashSet<>();
        this.m_segment_list = new ArrayList<>();
    }

    /**
     * Main entry point - corresponds to ImproveOrthogonalRoutes::execute()
     */
    public void execute() {
        m_shared_path_connectors_with_common_endpoints.clear();

        // Simplify routes.
        simplifyOrthogonalRoutes();

        // Do Unifying first, by itself.
        if (m_router.routingOption(Router.RoutingOption.performUnifyingNudgingPreprocessingStep) &&
                (m_router.routingParameter(Router.RoutingParameter.fixedSharedPathPenalty) == 0)) {
            for (int dimension = 0; dimension < 2; ++dimension) {
                boolean justUnifying = true;
                m_segment_list.clear();
                buildOrthogonalNudgingSegments(m_router, dimension, m_segment_list);
                Scanline.buildOrthogonalChannelInfo(m_router, dimension, m_segment_list);
                nudgeOrthogonalRoutes(dimension, justUnifying);
            }
        }

        // Do the Nudging and centring.
        for (int dimension = 0; dimension < 2; ++dimension) {
            m_point_orders.clear();
            // Build nudging info.
            buildOrthogonalNudgingOrderInfo();

            // Do the centring and nudging.
            m_segment_list.clear();
            buildOrthogonalNudgingSegments(m_router, dimension, m_segment_list);
            Scanline.buildOrthogonalChannelInfo(m_router, dimension, m_segment_list);
            nudgeOrthogonalRoutes(dimension, false);
        }

        // Resimplify all the display routes that may have been split.
        simplifyOrthogonalRoutes();
    }

    // =========================================================================
    // simplifyOrthogonalRoutes
    // =========================================================================

    private void simplifyOrthogonalRoutes() {
        for (ConnRef conn : m_router.m_connectors) {
            if (conn.routingType() != ConnType.Orthogonal) {
                continue;
            }
            conn.set_route(conn.displayRoute().simplify());
        }
    }

    // =========================================================================
    // buildOrthogonalNudgingOrderInfo
    // =========================================================================

    private void buildOrthogonalNudgingOrderInfo() {
        simplifyOrthogonalRoutes();

        boolean buildSharedPathInfo =
            !m_router.routingOption(Router.RoutingOption.nudgeSharedPathsWithCommonEndPoint) &&
                m_shared_path_connectors_with_common_endpoints.isEmpty();

        // Make a vector of the connector list, for convenience.
        List<ConnRef> connRefs = new ArrayList<>(m_router.m_connectors);

        // Make a temporary copy of all the connector displayRoutes.
        List<Polygon> connRoutes = new ArrayList<>(connRefs.size());
        for (ConnRef connRef : connRefs) {
            connRoutes.add(new Polygon(connRef.displayRoute()));
        }

        // Do segment splitting.
        for (int ind1 = 0; ind1 < connRefs.size(); ++ind1) {
            ConnRef conn = connRefs.get(ind1);
            if (conn.routingType() != ConnType.Orthogonal) {
                continue;
            }
            for (int ind2 = 0; ind2 < connRefs.size(); ++ind2) {
                if (ind1 == ind2) {
                    continue;
                }
                ConnRef conn2 = connRefs.get(ind2);
                if (conn2.routingType() != ConnType.Orthogonal) {
                    continue;
                }
                Polygon route = connRoutes.get(ind1);
                Polygon route2 = connRoutes.get(ind2);
                ConnectorCrossings.splitBranchingSegments(route2, true, route, 0);
            }
        }

        for (int ind1 = 0; ind1 < connRefs.size(); ++ind1) {
            ConnRef conn = connRefs.get(ind1);
            if (conn.routingType() != ConnType.Orthogonal) {
                continue;
            }
            for (int ind2 = ind1 + 1; ind2 < connRefs.size(); ++ind2) {
                ConnRef conn2 = connRefs.get(ind2);
                if (conn2.routingType() != ConnType.Orthogonal) {
                    continue;
                }
                Polygon route = connRoutes.get(ind1);
                Polygon route2 = connRoutes.get(ind2);
                int crossingFlags = 0;
                ConnectorCrossings cross = new ConnectorCrossings(route2, true, route, conn2, conn);
                cross.pointOrders = m_point_orders;
                for (int i = 1; i < route.size(); ++i) {
                    boolean finalSegment = ((i + 1) == route.size());
                    cross.countForSegment(i, finalSegment);
                    crossingFlags |= cross.crossingFlags;
                }

                if (buildSharedPathInfo &&
                        (crossingFlags & ConnectorCrossings.CROSSING_SHARES_PATH_AT_END) != 0) {
                    m_shared_path_connectors_with_common_endpoints.add(
                            new UnsignedPair(conn.id(), conn2.id()));
                }
            }
        }
    }

    // =========================================================================
    // buildOrthogonalNudgingSegments - static helper
    // =========================================================================

    private static boolean insideRectBounds(Point point, Point rectMin, Point rectMax) {
        Point zero = new Point(0, 0);
        if (rectMin.equals(zero) && rectMax.equals(zero)) {
            return false;
        }
        for (int i = 0; i < 2; ++i) {
            if (point.get(i) < rectMin.get(i)) {
                return false;
            }
            if (point.get(i) > rectMax.get(i)) {
                return false;
            }
        }
        return true;
    }

    static void buildOrthogonalNudgingSegments(Router router,
            int dim, List<ShiftSegment> segmentList) {
        if (router.routingParameter(Router.RoutingParameter.segmentPenalty) == 0) {
            return;
        }
        boolean nudgeFinalSegments = router.routingOption(
                Router.RoutingOption.nudgeOrthogonalSegmentsConnectedToShapes);
        // shapeLimits: pairs of (min, max) for each obstacle
        List<Point> shapeLimitsMin = new ArrayList<>();
        List<Point> shapeLimitsMax = new ArrayList<>();
        if (nudgeFinalSegments) {
            double zeroBufferDist = 0.0;
            for (Obstacle obstacle : router.m_obstacles) {
                ShapeRef shape = (obstacle instanceof ShapeRef) ? (ShapeRef) obstacle : null;
                JunctionRef junction = (obstacle instanceof JunctionRef) ? (JunctionRef) obstacle : null;
                if (shape != null) {
                    Box bBox = shape.polygon().offsetBoundingBox(zeroBufferDist);
                    shapeLimitsMin.add(bBox.min);
                    shapeLimitsMax.add(bBox.max);
                } else if (junction != null) {
                    Point pos = junction.position();
                    shapeLimitsMin.add(pos);
                    shapeLimitsMax.add(pos);
                } else {
                    shapeLimitsMin.add(new Point(0, 0));
                    shapeLimitsMax.add(new Point(0, 0));
                }
            }
        }

        int altDim = (dim + 1) % 2;
        // For each connector.
        for (ConnRef conn : router.m_connectors) {
            if (conn.routingType() != ConnType.Orthogonal) {
                continue;
            }
            Polygon displayRoute = conn.displayRoute();
            // Determine all line segments that we are interested in shifting.
            for (int i = 1; i < displayRoute.size(); ++i) {
                if (displayRoute.ps.get(i - 1).get(dim) == displayRoute.ps.get(i).get(dim)) {
                    // It's a segment in the dimension we are processing.
                    int indexLow = i - 1;
                    int indexHigh = i;
                    if (displayRoute.ps.get(i - 1).get(altDim) ==
                            displayRoute.ps.get(i).get(altDim)) {
                        // Zero length segment, ignore.
                        continue;
                    } else if (displayRoute.ps.get(i - 1).get(altDim) >
                            displayRoute.ps.get(i).get(altDim)) {
                        indexLow = i;
                        indexHigh = i - 1;
                    }

                    double thisPos = displayRoute.ps.get(i).get(dim);

                    if ((i == 1) || ((i + 1) == displayRoute.size())) {
                        // Is first or last segment of route.
                        if (nudgeFinalSegments) {
                            double minLim = -CHANNEL_MAX;
                            double maxLim = CHANNEL_MAX;
                            int endsInShapes = 0;
                            for (int k = 0; k < shapeLimitsMin.size(); ++k) {
                                double shapeMin = shapeLimitsMin.get(k).get(dim);
                                double shapeMax = shapeLimitsMax.get(k).get(dim);
                                if (insideRectBounds(displayRoute.ps.get(i - 1),
                                        shapeLimitsMin.get(k), shapeLimitsMax.get(k))) {
                                    minLim = Math.max(minLim, shapeMin);
                                    maxLim = Math.min(maxLim, shapeMax);
                                    endsInShapes |= 0x01;
                                }
                                if (insideRectBounds(displayRoute.ps.get(i),
                                        shapeLimitsMin.get(k), shapeLimitsMax.get(k))) {
                                    minLim = Math.max(minLim, shapeMin);
                                    maxLim = Math.min(maxLim, shapeMax);
                                    endsInShapes |= 0x10;
                                }
                            }

                            if (endsInShapes == 0) {
                                double pos = displayRoute.ps.get(i - 1).get(dim);
                                double freeConnBuffer = 15;
                                minLim = Math.max(minLim, pos - freeConnBuffer);
                                maxLim = Math.min(maxLim, pos + freeConnBuffer);
                            }

                            if ((minLim == maxLim) || conn.hasFixedRoute()) {
                                segmentList.add(new NudgingShiftSegment(conn,
                                        indexLow, indexHigh, dim));
                            } else {
                                NudgingShiftSegment segment = new NudgingShiftSegment(
                                        conn, indexLow, indexHigh, false, false, dim,
                                        minLim, maxLim);
                                segment.finalSegment = true;
                                segment.endsInShape = (endsInShapes > 0);
                                if ((displayRoute.size() == 2) &&
                                        (endsInShapes == 0x11)) {
                                    segment.singleConnectedSegment = true;
                                }
                                segmentList.add(segment);
                            }
                        } else {
                            segmentList.add(new NudgingShiftSegment(conn,
                                    indexLow, indexHigh, dim));
                        }
                        continue;
                    }

                    // The segment probably has space to be shifted.
                    double minLim = -CHANNEL_MAX;
                    double maxLim = CHANNEL_MAX;

                    boolean isSBend = false;
                    boolean isZBend = false;

                    double prevPos = displayRoute.ps.get(i - 2).get(dim);
                    double nextPos = displayRoute.ps.get(i + 1).get(dim);
                    if (((prevPos < thisPos) && (nextPos > thisPos)) ||
                            ((prevPos > thisPos) && (nextPos < thisPos))) {
                        if (prevPos < thisPos) {
                            minLim = Math.max(minLim, prevPos);
                            maxLim = Math.min(maxLim, nextPos);
                            isZBend = true;
                        } else {
                            minLim = Math.max(minLim, nextPos);
                            maxLim = Math.min(maxLim, prevPos);
                            isSBend = true;
                        }
                    }

                    NudgingShiftSegment nss = new NudgingShiftSegment(conn,
                            indexLow, indexHigh, isSBend, isZBend, dim,
                            minLim, maxLim);
                    segmentList.add(nss);
                }
            }
        }
    }

    // =========================================================================
    // nudgeOrthogonalRoutes
    // =========================================================================

    private void nudgeOrthogonalRoutes(int dimension, boolean justUnifying) {
        boolean nudgeFinalSegments = m_router.routingOption(
                Router.RoutingOption.nudgeOrthogonalSegmentsConnectedToShapes);
        boolean nudgeSharedPathsWithCommonEnd = m_router.routingOption(
                Router.RoutingOption.nudgeSharedPathsWithCommonEndPoint);
        boolean nudgeFinalSegmentsFromSamePoint = m_router.routingOption(
                Router.RoutingOption.nudgeFinalSegmentsFromSamePoint);
        double baseSepDist = m_router.routingParameter(Router.RoutingParameter.idealNudgingDistance);
        assert baseSepDist >= 0;
        double reductionSteps = 10.0;

        // Do the actual nudging.
        List<ShiftSegment> currentRegion = new ArrayList<>();
        while (!m_segment_list.isEmpty()) {
            // Take a reference segment
            ShiftSegment currentSegment = m_segment_list.getFirst();
            // Then, find the segments that overlap this one.
            currentRegion.clear();
            currentRegion.add(currentSegment);
            m_segment_list.removeFirst();
            for (int curr = 0; curr < m_segment_list.size(); ) {
                boolean overlaps = false;
                for (ShiftSegment curr2 : currentRegion) {
                    if (m_segment_list.get(curr).overlapsWith(curr2, dimension)) {
                        overlaps = true;
                        break;
                    }
                }
                if (overlaps) {
                    currentRegion.add(m_segment_list.get(curr));
                    m_segment_list.remove(curr);
                    // Consider segments from the beginning.
                    curr = 0;
                } else {
                    ++curr;
                }
            }

            if (!justUnifying) {
                currentRegion = linesort(nudgeFinalSegments, currentRegion,
                        m_point_orders, dimension);
            }

            if (currentRegion.size() == 1) {
                if (currentRegion.getFirst().immovable() || justUnifying) {
                    continue;
                }
            }

            // Process these segments.
            List<Integer> freeIndexes = new ArrayList<>();
            List<Variable> vs = new ArrayList<>();
            List<Constraint> cs = new ArrayList<>();
            List<NudgingShiftSegment> prevVars = new ArrayList<>();
            double sepDist = baseSepDist;

            for (ShiftSegment currSegmentS : currentRegion) {
                NudgingShiftSegment currSeg = (NudgingShiftSegment) currSegmentS;

                // Create a solver variable for the position of this segment.
                currSeg.createSolverVariable(justUnifying);

                vs.add(currSeg.variable);
                int index = vs.size() - 1;

                if (justUnifying) {
                    if (currSeg.variable.weight == Variable.Weight.freeWeight) {
                        freeIndexes.add(index);
                    }
                    prevVars.add(currSeg);
                    continue;
                }

                // Constrain to channel boundary.
                if (!currSeg.fixed) {
                    if (currSeg.minSpaceLimit > -CHANNEL_MAX) {
                        vs.add(new Variable(Variable.Id.channelLeftID,
                                currSeg.minSpaceLimit, Variable.Weight.fixedWeight));
                        cs.add(new Constraint(vs.getLast(), vs.get(index), 0.0));
                    }
                }

                // Constrain position in relation to previously seen segments.
                for (NudgingShiftSegment prevSeg : prevVars) {
                    Variable prevVar = prevSeg.variable;

                    if (currSeg.overlapsWith(prevSeg, dimension) &&
                            (!(currSeg.fixed) || !(prevSeg.fixed))) {
                        double thisSepDist = sepDist;
                        boolean equality = false;
                        if (currSeg.shouldAlignWith(prevSeg, dimension)) {
                            thisSepDist = 0;
                            equality = true;
                        } else if (currSeg.canAlignWith(prevSeg)) {
                            thisSepDist = 0;
                        } else if (!nudgeSharedPathsWithCommonEnd &&
                                (m_shared_path_connectors_with_common_endpoints.contains(
                                        new UnsignedPair(currSeg.connRef.id(), prevSeg.connRef.id())))) {
                            thisSepDist = 0;
                            equality = true;
                        } else if (!nudgeFinalSegmentsFromSamePoint &&
                                currSeg.connRef != prevSeg.connRef &&
                                sharesFinalEndpoint(currSeg, prevSeg) &&
                                segmentsSharePath(currSeg, prevSeg, dimension)) {
                            thisSepDist = 0;
                            equality = true;
                        }

                        Constraint constraint = new Constraint(prevVar,
                                vs.get(index), thisSepDist, equality);
                        cs.add(constraint);
                    }
                }

                if (!currSeg.fixed) {
                    if (currSeg.maxSpaceLimit < CHANNEL_MAX) {
                        vs.add(new Variable(Variable.Id.channelRightID,
                                currSeg.maxSpaceLimit, Variable.Weight.fixedWeight));
                        cs.add(new Constraint(vs.get(index), vs.getLast(), 0.0));
                    }
                }

                prevVars.add(currSeg);
            }

            List<PotentialSegmentConstraint> potentialConstraints = new ArrayList<>();
            if (justUnifying) {
                for (int ci = 0; ci < freeIndexes.size(); ++ci) {
                    for (int ci2 = ci + 1; ci2 < freeIndexes.size(); ++ci2) {
                        potentialConstraints.add(new PotentialSegmentConstraint(
                                freeIndexes.get(ci), freeIndexes.get(ci2), vs));
                    }
                }
            }

            // Repeatedly try solving.
            boolean justAddedConstraint = false;
            boolean satisfied;

            // Track unsatisfied variable ranges for constraint gap rewriting.
            List<int[]> unsatisfiedRanges = new ArrayList<>(); // pairs of [first, second]

            do {
                IncSolver f = new IncSolver(vs, cs);
                f.solve();

                // Determine if the problem was satisfied.
                satisfied = true;
                unsatisfiedRanges.clear();
                for (int i = 0; i < vs.size(); ++i) {
                    if (vs.get(i).id != Variable.Id.freeSegmentID) {
                        if (Math.abs(vs.get(i).finalPosition -
                                vs.get(i).desiredPosition) > 0.0001) {
                            satisfied = false;

                            if (vs.get(i).id == Variable.Id.channelLeftID) {
                                if (unsatisfiedRanges.isEmpty() ||
                                        (unsatisfiedRanges.getLast()[0] !=
                                                unsatisfiedRanges.getLast()[1])) {
                                    unsatisfiedRanges.add(new int[]{i, i + 1});
                                }
                            } else if (vs.get(i).id == Variable.Id.channelRightID) {
                                if (unsatisfiedRanges.isEmpty()) {
                                    assert i > 0;
                                    unsatisfiedRanges.add(new int[]{i - 1, i});
                                } else {
                                    unsatisfiedRanges.getLast()[1] = i;
                                }
                            } else if (vs.get(i).id == Variable.Id.fixedSegmentID) {
                                if (unsatisfiedRanges.isEmpty()) {
                                    unsatisfiedRanges.add(new int[]{i, i});
                                } else {
                                    unsatisfiedRanges.getLast()[1] = i;
                                }
                            }
                        }
                    }
                }

                if (justUnifying) {
                    if (justAddedConstraint) {
                        assert !potentialConstraints.isEmpty();
                        if (!satisfied) {
                            potentialConstraints.removeFirst();
                            cs.removeLast();
                        } else {
                            PotentialSegmentConstraint pc = potentialConstraints.getFirst();
                            for (PotentialSegmentConstraint it : potentialConstraints) {
                                it.rewriteIndex(pc.index1, pc.index2);
                            }
                            potentialConstraints.removeFirst();
                        }
                    }
                    potentialConstraints.sort(Comparator.comparingDouble(PotentialSegmentConstraint::sepDistance));
                    justAddedConstraint = false;

                    while (!potentialConstraints.isEmpty() &&
                            !potentialConstraints.getFirst().stillValid()) {
                        potentialConstraints.removeFirst();
                    }

                    if (!potentialConstraints.isEmpty()) {
                        PotentialSegmentConstraint pc = potentialConstraints.getFirst();
                        assert pc.index1 != pc.index2;
                        cs.add(new Constraint(vs.get(pc.index1), vs.get(pc.index2),
                                0, true));
                        satisfied = false;
                        justAddedConstraint = true;
                    }
                } else {
                    if (!satisfied) {
                        assert !unsatisfiedRanges.isEmpty();
                        sepDist -= (baseSepDist / reductionSteps);

                        // Rewrite all the gap constraints to have the new
                        // reduced separation distance.
                        boolean withinUnsatisfiedGroup = false;
                        int rangeIdx = 0;
                        for (Constraint constraint : cs) {
                            if (rangeIdx >= unsatisfiedRanges.size()) break;
                            int[] range = unsatisfiedRanges.get(rangeIdx);

                            if (constraint.left == vs.get(range[0])) {
                                withinUnsatisfiedGroup = true;
                            }

                            if (withinUnsatisfiedGroup && (constraint.gap > 0)) {
                                constraint.gap = sepDist;
                            }

                            if (constraint.right == vs.get(range[1])) {
                                withinUnsatisfiedGroup = false;
                                rangeIdx++;
                            }
                        }
                    }
                }
            }
            while (!satisfied && (sepDist > 0.0001));

            if (satisfied) {
                for (ShiftSegment currSegmentS : currentRegion) {
                    NudgingShiftSegment segment = (NudgingShiftSegment) currSegmentS;
                    segment.updatePositionsFromSolver();
                }
            }
        }
    }

    // =========================================================================
    // linesort - insertion sort with partial comparability
    // =========================================================================

    private static List<ShiftSegment> linesort(boolean nudgeFinalSegments,
            List<ShiftSegment> origList, Map<Point, PtOrder> orders, int dimension) {
        // Cope with end segments that are getting moved and will line up with
        // other segments of the same connector. Merge them into a single NudgingShiftSegment.
        if (nudgeFinalSegments) {
            for (int ci = 0; ci < origList.size(); ++ci) {
                for (int oi = ci + 1; oi < origList.size(); ) {
                    NudgingShiftSegment currSeg = (NudgingShiftSegment) origList.get(ci);
                    NudgingShiftSegment otherSeg = (NudgingShiftSegment) origList.get(oi);
                    if (currSeg.shouldAlignWith(otherSeg, dimension)) {
                        currSeg.mergeWith(otherSeg, dimension);
                        origList.remove(oi);
                    } else {
                        ++oi;
                    }
                }
            }
        }

        List<ShiftSegment> resultList = new ArrayList<>();
        int origListSize = origList.size();
        int deferredN = 0;
        while (!origList.isEmpty()) {
            ShiftSegment segment = origList.removeFirst();

            // Find the insertion point in the resultList.
            boolean allComparable = true;
            int insertIdx = resultList.size(); // default: append
            for (int curr = 0; curr < resultList.size(); ++curr) {
                boolean[] comparable = new boolean[]{false};
                boolean lessThan = cmpLineOrder(segment, resultList.get(curr),
                        comparable, orders, dimension);
                allComparable &= comparable[0];

                if (comparable[0] && lessThan) {
                    insertIdx = curr;
                    break;
                }
            }

            if (resultList.isEmpty() || allComparable || (deferredN >= origListSize)) {
                resultList.add(insertIdx, segment);
                deferredN = 0;
                origListSize = origList.size();
            } else {
                origList.add(segment);
                deferredN++;
            }
        }

        return resultList;
    }

    /**
     * CmpLineOrder comparison function.
     * Returns true if lhs should come before rhs.
     * Sets comparable[0] to true if the two segments can be meaningfully ordered.
     */
    private static boolean cmpLineOrder(ShiftSegment lhsSuper, ShiftSegment rhsSuper,
            boolean[] comparable, Map<Point, PtOrder> orders, int dimension) {
        NudgingShiftSegment lhs = (NudgingShiftSegment) lhsSuper;
        NudgingShiftSegment rhs = (NudgingShiftSegment) rhsSuper;
        if (comparable != null) {
            comparable[0] = true;
        }
        Point lhsLow = lhs.lowPoint();
        Point rhsLow = rhs.lowPoint();
        int altDim = (dimension + 1) % 2;

        if (lhsLow.get(dimension) != rhsLow.get(dimension)) {
            return lhsLow.get(dimension) < rhsLow.get(dimension);
        }

        boolean[] oneIsFixed = new boolean[]{false};
        int lhsFixedOrder = lhs.fixedOrder(oneIsFixed);
        boolean lhsOneIsFixed = oneIsFixed[0];
        oneIsFixed[0] = false;
        int rhsFixedOrder = rhs.fixedOrder(oneIsFixed);
        boolean anyIsFixed = lhsOneIsFixed || oneIsFixed[0];
        if (anyIsFixed && (lhsFixedOrder != rhsFixedOrder)) {
            return lhsFixedOrder < rhsFixedOrder;
        }

        int lhsOrder = lhs.order();
        int rhsOrder = rhs.order();
        if (lhsOrder != rhsOrder) {
            return lhsOrder < rhsOrder;
        }

        // Need to index using the original point into the map, so find it.
        Point unchanged = (lhsLow.get(altDim) > rhsLow.get(altDim)) ? lhsLow : rhsLow;

        PtOrder lowOrder = orders.get(unchanged);
        if (lowOrder == null) {
            if (comparable != null) {
                comparable[0] = false;
            }
            return lhsLow.get(altDim) < rhsLow.get(altDim);
        }
        int lhsPos = lowOrder.positionFor(dimension, lhs.connRef);
        int rhsPos = lowOrder.positionFor(dimension, rhs.connRef);
        if ((lhsPos == -1) || (rhsPos == -1)) {
            if (comparable != null) {
                comparable[0] = false;
            }
            return lhsLow.get(altDim) < rhsLow.get(altDim);
        }
        return lhsPos < rhsPos;
    }

    // =========================================================================
    // segmentsSharePath - checks if two segments are on a shared path section
    // =========================================================================

    /**
     * Returns true if the two segments from different connectors lie on the same
     * shared path section — i.e. the connectors have not yet diverged at this point.
     *
     * Two segments share a path if they are at the same position in the nudging
     * dimension AND their alt-dimension ranges overlap (which is already guaranteed
     * by overlapsWith, but we double-check here for the common-endpoint case).
     *
     * The key insight: if two connectors share a start point and their current
     * segments are at the same position (before nudging), they are on a shared
     * path and should be kept together.
     */
    private static boolean segmentsSharePath(NudgingShiftSegment seg1,
            NudgingShiftSegment seg2, int dimension) {
        // They share a path if they are currently at the same position in the
        // nudging dimension (i.e. they haven't been nudged apart yet and are
        // actually collinear).
        double pos1 = seg1.lowPoint().get(dimension);
        double pos2 = seg2.lowPoint().get(dimension);
        return pos1 == pos2;
    }

    // =========================================================================
    // sharesFinalEndpoint - checks if two final segments share a connector endpoint
    // =========================================================================

    /**
     * Returns true if the two final segments belong to connectors that share
     * the exact same start or end point (i.e. they leave from or arrive at
     * the same coordinate).
     */
    private static boolean sharesFinalEndpoint(NudgingShiftSegment seg1,
            NudgingShiftSegment seg2) {
        Polygon route1 = seg1.connRef.displayRoute();
        Polygon route2 = seg2.connRef.displayRoute();
        if (route1.size() < 2 || route2.size() < 2) {
            return false;
        }
        Point src1 = route1.ps.getFirst();
        Point dst1 = route1.ps.getLast();
        Point src2 = route2.ps.getFirst();
        Point dst2 = route2.ps.getLast();

        return src1.equals(src2) || src1.equals(dst2) ||
               dst1.equals(src2) || dst1.equals(dst2);
    }

    // =========================================================================
    // UnsignedPair - canonical pair of unsigned ints
    // =========================================================================

    static class UnsignedPair {
        final int first;
        final int second;

        UnsignedPair(int ind1, int ind2) {
            this.first = Math.min(ind1, ind2);
            this.second = Math.max(ind1, ind2);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UnsignedPair that)) return false;
            return first == that.first && second == that.second;
        }

        @Override
        public int hashCode() {
            return 31 * first + second;
        }
    }

    // =========================================================================
    // PotentialSegmentConstraint
    // =========================================================================

    private static class PotentialSegmentConstraint {
        int index1;
        int index2;
        private final List<Variable> vs;

        PotentialSegmentConstraint(int index1, int index2, List<Variable> vs) {
            this.index1 = index1;
            this.index2 = index2;
            this.vs = vs;
        }

        double sepDistance() {
            if (!stillValid()) {
                return 0;
            }
            return Math.abs(vs.get(index1).finalPosition - vs.get(index2).finalPosition);
        }

        boolean stillValid() {
            return (index1 != index2);
        }

        void rewriteIndex(int oldIndex, int newIndex) {
            if (index1 == oldIndex) {
                index1 = newIndex;
            }
            if (index2 == oldIndex) {
                index2 = newIndex;
            }
        }
    }
}
