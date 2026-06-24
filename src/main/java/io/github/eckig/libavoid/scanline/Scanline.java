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

package io.github.eckig.libavoid.scanline;

import io.github.eckig.libavoid.Box;
import io.github.eckig.libavoid.JunctionRef;
import io.github.eckig.libavoid.Obstacle;
import io.github.eckig.libavoid.Point;
import io.github.eckig.libavoid.Router;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * Scanline utility functions for building orthogonal channel info
 * and connector route checkpoint caches.
 * Translated from scanline.cpp in libavoid C++.
 */
public final class Scanline {

    private Scanline() {}

    /**
     * Comparator for events: sort by position, then type, then node id.
     * C++: int compare_events(const void *a, const void *b)
     */
    public static int compareEvents(Event ea, Event eb) {
        if (ea.pos != eb.pos) {
            return Double.compare(ea.pos, eb.pos);
        }
        if (ea.type != eb.type) {
            return ea.type.ordinal() - eb.type.ordinal();
        }
        return Integer.compare(ea.v.nodeId, eb.v.nodeId);
    }

    /**
     * Processes sweep events used to determine each horizontal and vertical
     * line segment in a connector's channel of visibility.
     * C++: static void processShiftEvent(NodeSet& scanline, Event *e, size_t dim, unsigned int pass)
     */
    private static void processShiftEvent(TreeSet<Node> scanline, Event e, int dim, int pass) {
        Node v = e.v;

        if ((pass == 3 && (e.type == EventType.Open || e.type == EventType.SegOpen))) {
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

        if ((pass == 4 && (e.type == EventType.Open || e.type == EventType.SegOpen)) ||
            (pass == 1 && (e.type == EventType.SegClose || e.type == EventType.Close))) {
            if (v.ss != null) {
                // As far as we can see.
                double minLimit = v.firstObstacleAbove(dim);
                double maxLimit = v.firstObstacleBelow(dim);

                v.ss.minSpaceLimit = Math.max(minLimit, v.ss.minSpaceLimit);
                v.ss.maxSpaceLimit = Math.min(maxLimit, v.ss.maxSpaceLimit);
            } else {
                v.markShiftSegmentsAbove(dim);
                v.markShiftSegmentsBelow(dim);
            }
        }

        if (pass == 2 && (e.type == EventType.SegClose || e.type == EventType.Close)) {
            // Clean up neighbour pointers.
            Node l = v.firstAbove;
            Node r = v.firstBelow;
            if (l != null) {
                l.firstBelow = v.firstBelow;
            }
            if (r != null) {
                r.firstAbove = v.firstAbove;
            }
            scanline.remove(v);
        }
    }

    /**
     * Build orthogonal channel info by doing a sweep to determine space
     * for shifting segments.
     * C++: void buildOrthogonalChannelInfo(Router *router, const size_t dim, ShiftSegmentList& segmentList)
     */
    public static void buildOrthogonalChannelInfo(Router router, int dim, List<ShiftSegment> segmentList) {
        if (segmentList.isEmpty()) {
            return;
        }

        int altDim = (dim + 1) % 2;

        // Set up the events for the sweep.
        List<Event> events = new ArrayList<>();

        for (Obstacle obstacle : router.m_obstacles) {
            JunctionRef junction = (obstacle instanceof JunctionRef) ? (JunctionRef) obstacle : null;
            if (junction != null && !junction.positionFixed()) {
                // Junctions that are free to move are not treated as obstacles.
                continue;
            }
            Box bBox = obstacle.routingBox();
            Point bMin = bBox.min;
            Point bMax = bBox.max;
            double mid = bMin.get(dim) + ((bMax.get(dim) - bMin.get(dim)) / 2);
            Node node = new Node(obstacle, mid);
            events.add(new Event(EventType.Open, node, bMin.get(altDim)));
            events.add(new Event(EventType.Close, node, bMax.get(altDim)));
        }

        for (ShiftSegment seg : segmentList) {
            Point lowPt = seg.lowPoint();
            Point highPt = seg.highPoint();

            assert lowPt.get(dim) == highPt.get(dim);
            assert lowPt.get(altDim) < highPt.get(altDim);
            Node node = new Node(seg, lowPt.get(dim));
            events.add(new Event(EventType.SegOpen, node, lowPt.get(altDim)));
            events.add(new Event(EventType.SegClose, node, highPt.get(altDim)));
        }

        events.sort(Scanline::compareEvents);

        // Process the sweep.
        // We do multiple passes over sections of the list so we can add relevant
        // entries to the scanline that might follow, before processing them.
        Comparator<Node> nodeComparator = (u, w) -> {
            if (u.pos != w.pos) {
                return Double.compare(u.pos, w.pos);
            }
            return Integer.compare(u.nodeId, w.nodeId);
        };
        TreeSet<Node> scanline = new TreeSet<>(nodeComparator);

        int totalEvents = events.size();
        double thisPos = (totalEvents > 0) ? events.getFirst().pos : 0;
        int posStartIndex = 0;
        int posFinishIndex;

        for (int i = 0; i <= totalEvents; ++i) {
            if ((i == totalEvents) || (events.get(i).pos != thisPos)) {
                posFinishIndex = i;
                for (int pass = 2; pass <= 4; ++pass) {
                    for (int j = posStartIndex; j < posFinishIndex; ++j) {
                        processShiftEvent(scanline, events.get(j), dim, pass);
                    }
                }

                if (i == totalEvents) {
                    break;
                }

                thisPos = events.get(i).pos;
                posStartIndex = i;
            }

            // Do the first sweep event handling
            processShiftEvent(scanline, events.get(i), dim, 1);
        }
        assert scanline.isEmpty();
    }
}
