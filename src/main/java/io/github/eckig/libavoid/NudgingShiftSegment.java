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

import io.github.eckig.libavoid.scanline.ShiftSegment;
import io.github.eckig.libavoid.vpsc.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * A shift segment used in the nudging phase of orthogonal routing.
 * Translated from orthogonal.cpp NudgingShiftSegment class in libavoid C++.
 */
public class NudgingShiftSegment extends ShiftSegment {

    public ConnRef connRef;
    public Variable variable;
    public List<Integer> indexes;
    public boolean fixed;
    public boolean finalSegment;
    public boolean endsInShape;
    public boolean singleConnectedSegment;
    private final boolean sBend;
    private final boolean zBend;

    /**
     * Constructor for shiftable segments.
     * C++: NudgingShiftSegment(ConnRef *conn, size_t low, size_t high,
     *          bool isSBend, bool isZBend, size_t dim, double minLim, double maxLim)
     */
    public NudgingShiftSegment(ConnRef conn, int low, int high,
            boolean isSBend, boolean isZBend, int dim, double minLim, double maxLim) {
        super(dim);
        this.connRef = conn;
        this.variable = null;
        this.fixed = false;
        this.finalSegment = false;
        this.endsInShape = false;
        this.singleConnectedSegment = false;
        this.sBend = isSBend;
        this.zBend = isZBend;
        this.indexes = new ArrayList<>();
        indexes.add(low);
        indexes.add(high);
        this.minSpaceLimit = minLim;
        this.maxSpaceLimit = maxLim;
    }

    /**
     * Constructor for fixed segments.
     * C++: NudgingShiftSegment(ConnRef *conn, size_t low, size_t high, size_t dim)
     */
    public NudgingShiftSegment(ConnRef conn, int low, int high, int dim) {
        super(dim);
        this.connRef = conn;
        this.variable = null;
        this.fixed = true;
        this.finalSegment = false;
        this.endsInShape = false;
        this.singleConnectedSegment = false;
        this.sBend = false;
        this.zBend = false;
        this.indexes = new ArrayList<>();
        indexes.add(low);
        indexes.add(high);
        // This has no space to shift.
        this.minSpaceLimit = lowPoint().get(dim);
        this.maxSpaceLimit = lowPoint().get(dim);
    }

    @Override
    public Point lowPoint() {
        return connRef.displayRoute().ps.get(indexes.getFirst());
    }

    @Override
    public Point highPoint() {
        return connRef.displayRoute().ps.get(indexes.getLast());
    }

    public double nudgeDistance() {
        return connRef.router().routingParameter(Router.RoutingParameter.idealNudgingDistance);
    }

    @Override
    public boolean immovable() {
        return !zigzag();
    }

    /**
     * Create a VPSC solver variable for this segment's position.
     * C++: void createSolverVariable(const bool justUnifying)
     */
    public void createSolverVariable(boolean justUnifying) {
        boolean nudgeFinalSegments = connRef.router().routingOption(
                Router.RoutingOption.nudgeOrthogonalSegmentsConnectedToShapes);
        var varID = Variable.Id.freeSegmentID;
        double varPos = lowPoint().get(dimension);
        var weight = Variable.Weight.freeWeight;
        if (nudgeFinalSegments && finalSegment) {
            weight = Variable.Weight.strongWeight;
            if (singleConnectedSegment && !justUnifying) {
                weight = Variable.Weight.strongerWeight;
            }
        } else if (zigzag()) {
            // For zigzag bends, take the middle as ideal.
            varPos = minSpaceLimit + ((maxSpaceLimit - minSpaceLimit) / 2);
        } else if (fixed) {
            weight = Variable.Weight.fixedWeight;
            varID = Variable.Id.fixedSegmentID;
        } else if (!finalSegment) {
            weight = Variable.Weight.strongWeight;
        }
        variable = new Variable(varID, varPos, weight);
    }

    /**
     * Apply the solver result back to the connector route points.
     * C++: void updatePositionsFromSolver(const bool justUnifying)
     */
    public void updatePositionsFromSolver() {
        if (fixed) {
            return;
        }
        double newPos = variable.finalPosition;
        newPos = Math.max(newPos, minSpaceLimit);
        newPos = Math.min(newPos, maxSpaceLimit);

        for (int it = 0; it < indexes.size(); ++it) {
            int index = indexes.get(it);
            connRef.displayRoute().ps.get(index).set(dimension, newPos);
        }
    }

    /**
     * C++: int fixedOrder(bool& isFixed) const
     * Returns order and sets isFixed[0] if fixed.
     */
    public int fixedOrder(boolean[] isFixed) {
        double nudgeDist = nudgeDistance();
        double pos = lowPoint().get(dimension);
        boolean minLimited = ((pos - minSpaceLimit) < nudgeDist);
        boolean maxLimited = ((maxSpaceLimit - pos) < nudgeDist);

        if (fixed || (minLimited && maxLimited)) {
            isFixed[0] = true;
            return 0;
        } else if (minLimited) {
            return 1;
        } else if (maxLimited) {
            return -1;
        }
        return 0;
    }

    public int order() {
        if (lowC()) {
            return -1;
        } else if (highC()) {
            return 1;
        }
        return 0;
    }

    public boolean zigzag() {
        return sBend || zBend;
    }

    /**
     * This counts segments that are collinear and share an endpoint as
     * overlapping. This allows them to be nudged apart where possible.
     * C++: bool overlapsWith(const ShiftSegment *rhs, size_t dim) const
     */
    @Override
    public boolean overlapsWith(ShiftSegment rhsSuper, int dim) {
        NudgingShiftSegment rhs = (NudgingShiftSegment) rhsSuper;
        int altDim = (dim + 1) % 2;
        Point lowPt = lowPoint();
        Point highPt = highPoint();
        Point rhsLowPt = rhs.lowPoint();
        Point rhsHighPt = rhs.highPoint();

        if ((lowPt.get(altDim) < rhsHighPt.get(altDim)) &&
                (rhsLowPt.get(altDim) < highPt.get(altDim))) {
            // The segments overlap.
            if ((minSpaceLimit <= rhs.maxSpaceLimit) &&
                    (rhs.minSpaceLimit <= maxSpaceLimit)) {
                return true;
            }
        } else if ((lowPt.get(altDim) == rhsHighPt.get(altDim)) ||
                   (rhsLowPt.get(altDim) == highPt.get(altDim))) {
            boolean nudgeColinearSegments = connRef.router().routingOption(
                    Router.RoutingOption.nudgeOrthogonalTouchingColinearSegments);

            if ((minSpaceLimit <= rhs.maxSpaceLimit) &&
                    (rhs.minSpaceLimit <= maxSpaceLimit)) {
                if (connRef.router().routingParameter(
                        Router.RoutingParameter.fixedSharedPathPenalty) > 0) {
                    return true;
                } else if ((rhs.sBend && sBend) || (rhs.zBend && zBend)) {
                    return nudgeColinearSegments;
                } else if ((rhs.finalSegment && finalSegment) &&
                        (rhs.connRef == connRef)) {
                    return nudgeColinearSegments;
                }
            }
        }
        return false;
    }

    /**
     * These segments are allowed to drift into alignment but don't have to.
     * C++: bool canAlignWith(const NudgingShiftSegment *rhs, size_t dim) const
     */
    public boolean canAlignWith(NudgingShiftSegment rhs) {
        if (connRef != rhs.connRef) {
            return false;
        }
        return true;
    }

    /**
     * These segments should align with each other.
     * C++: bool shouldAlignWith(const ShiftSegment *rhs, size_t dim) const
     */
    public boolean shouldAlignWith(ShiftSegment rhsSuper, int dim) {
        NudgingShiftSegment rhs = (NudgingShiftSegment) rhsSuper;
        if ((connRef == rhs.connRef) && finalSegment &&
                rhs.finalSegment && overlapsWith(rhs, dim)) {
            if ((endsInShape && rhs.endsInShape) ||
                    (Math.abs(lowPoint().get(dim) - rhs.lowPoint().get(dim)) < 10)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Used for merging segments with end segments that should appear as
     * a single segment.
     * C++: void mergeWith(const ShiftSegment *rhs, size_t dim)
     */
    public void mergeWith(ShiftSegment rhsSuper, int dim) {
        minSpaceLimit = Math.max(minSpaceLimit, rhsSuper.minSpaceLimit);
        maxSpaceLimit = Math.min(maxSpaceLimit, rhsSuper.maxSpaceLimit);

        double segmentPos = lowPoint().get(dimension);
        double segment2Pos = rhsSuper.lowPoint().get(dimension);
        if (segment2Pos < segmentPos) {
            segmentPos -= ((segmentPos - segment2Pos) / 2.0);
        } else if (segment2Pos > segmentPos) {
            segmentPos += ((segment2Pos - segmentPos) / 2.0);
        }
        segmentPos = Math.max(minSpaceLimit, segmentPos);
        segmentPos = Math.min(maxSpaceLimit, segmentPos);

        NudgingShiftSegment rhs = (NudgingShiftSegment) rhsSuper;
        indexes.addAll(rhs.indexes);
        int altDim = (dim + 1) % 2;
        // Sort indexes by position in the alt dimension
        final ConnRef conn = connRef;
        final int sortDim = altDim;
        indexes.sort((a, b) -> {
            double posA = conn.displayRoute().ps.get(a).get(sortDim);
            double posB = conn.displayRoute().ps.get(b).get(sortDim);
            return Double.compare(posA, posB);
        });

        // Apply the new position to all points
        for (int it = 0; it < indexes.size(); ++it) {
            int index = indexes.get(it);
            connRef.displayRoute().ps.get(index).set(dimension, segmentPos);
        }
    }

    private boolean lowC() {
        if (!finalSegment && !zigzag() && !fixed &&
                (minSpaceLimit == lowPoint().get(dimension))) {
            return true;
        }
        return false;
    }

    private boolean highC() {
        if (!finalSegment && !zigzag() && !fixed &&
                (maxSpaceLimit == lowPoint().get(dimension))) {
            return true;
        }
        return false;
    }
}
