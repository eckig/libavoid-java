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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Base class for ShapeRef and JunctionRef.
 * Corresponds to obstacle.h/obstacle.cpp in C++.
 *
 * An Obstacle represents either a shape or junction in the routing scene.
 * It has a polygon boundary, connection pins, a per-obstacle circular linked
 * list of VertInf nodes (m_first_vert/m_last_vert), and tracks which ConnEnds
 * are following (connected to) it.
 */
public abstract class Obstacle {

    protected Router m_router;
    protected int m_id;
    protected Polygon m_polygon;
    protected VertInf m_first_vert;
    protected VertInf m_last_vert;
    Set<ShapeConnectionPin> m_connection_pins;
    Set<ConnEnd> m_following_conn_ends;

    /**
     * Obstacle constructor.
     * Corresponds to Obstacle::Obstacle(Router*, Polygon, unsigned int) in C++.
     *
     * Creates the per-obstacle circular VertInf chain from the routing polygon.
     */
    protected Obstacle(Router router, Polygon poly, int id) {
        m_router = router;
        m_id = (id != 0) ? id : router.assignId();
        m_polygon = new Polygon(poly);
        m_polygon._id = m_id;
        m_first_vert = null;
        m_last_vert = null;
        m_connection_pins = new TreeSet<>();
        m_following_conn_ends = Collections.newSetFromMap(new IdentityHashMap<>());

        // Build the circular per-obstacle VertInf chain from the routing polygon.
        VertID i = new VertID(m_id, (short) 0);
        Polygon routingPoly = routingPolygon();
        boolean addToRouterNow = false;
        VertInf last = null;
        VertInf node = null;
        for (int pt_i = 0; pt_i < routingPoly.size(); pt_i++) {
            node = new VertInf(m_router, i, routingPoly.ps.get(pt_i), addToRouterNow);

            if (m_first_vert == null) {
                m_first_vert = node;
            } else {
                node.shPrev = last;
                last.shNext = node;
            }

            last = node;
            i.postIncrement();
        }
        m_last_vert = node;

        if (m_last_vert != null && m_first_vert != null) {
            m_last_vert.shNext = m_first_vert;
            m_first_vert.shPrev = m_last_vert;
        }
    }

    public int id() {
        return m_id;
    }

    public Polygon polygon() {
        return m_polygon;
    }

    public Router router() {
        return m_router;
    }

    /**
     * Returns the position of this obstacle. For shapes, this is the centre
     * of the bounding box. For junctions, this is the junction position.
     * Corresponds to pure virtual Obstacle::position() in C++.
     */
    public abstract Point position();

    /**
     * Returns the first VertInf in the per-obstacle circular chain.
     * Corresponds to Obstacle::firstVert() in C++.
     */
    public VertInf firstVert() {
        return m_first_vert;
    }

    /**
     * Returns the last VertInf in the per-obstacle circular chain.
     * Corresponds to Obstacle::lastVert() in C++.
     */
    public VertInf lastVert() {
        return m_last_vert;
    }

    /**
     * Updates the polygon boundary of this obstacle and resets VertInf positions.
     * Corresponds to Obstacle::setNewPoly(const Polygon&) in C++.
     */
    public void setNewPoly(Polygon poly) {
        assert m_first_vert != null;
        assert m_polygon.size() == poly.size();

        m_polygon = new Polygon(poly);
        m_polygon._id = m_id;
        Polygon routingPoly = routingPolygon();

        VertInf curr = m_first_vert;
        for (int pt_i = 0; pt_i < routingPoly.size(); pt_i++) {
            // Reset with the new polygon point.
            curr.reset(routingPoly.ps.get(pt_i));
            curr.pathNext = null;

            curr = curr.shNext;
        }
        assert curr == m_first_vert;

        // Update pin positions.
        for (ShapeConnectionPin pin : m_connection_pins) {
            pin.updatePosition(m_polygon);
        }
    }

    void addFollowingConnEnd(ConnEnd connEnd) {
        m_following_conn_ends.add(connEnd);
    }

    void removeFollowingConnEnd(ConnEnd connEnd) {
        m_following_conn_ends.remove(connEnd);
    }

    /**
     * Adds a connection pin to this obstacle.
     * Corresponds to Obstacle::addConnectionPin(ShapeConnectionPin*) in C++.
     */
    int addConnectionPin(ShapeConnectionPin pin) {
        m_connection_pins.add(pin);
        m_router.modifyConnectionPin(pin);
        return m_connection_pins.size();
    }

    void removeConnectionPin(ShapeConnectionPin pin) {
        m_connection_pins.remove(pin);
        m_router.modifyConnectionPin(pin);
    }

    /**
     * Makes this obstacle active: adds to router's obstacle list and adds
     * all VertInf nodes to the router's vertex list.
     * Corresponds to Obstacle::makeActive() in C++.
     */
    public void makeActive() {

        // Add to router's obstacle list.
        if (!m_router.m_obstacles.contains(this)) {
            m_router.m_obstacles.addFirst(this);
        }

        // Add points to vertex list.
        VertInf it = m_first_vert;
        do {
            VertInf tmp = it;
            it = it.shNext;
            m_router.vertices.addVertex(tmp);
        } while (it != m_first_vert);

    }

    /**
     * Makes this obstacle inactive: removes from router's obstacle list,
     * removes all VertInf nodes from vertex list, disconnects following ConnEnds.
     * Corresponds to Obstacle::makeInactive() in C++.
     */
    public void makeInactive() {

        // Remove from router's obstacle list.
        m_router.m_obstacles.remove(this);

        // Remove points from vertex list.
        VertInf it = m_first_vert;
        do {
            VertInf tmp = it;
            it = it.shNext;
            m_router.vertices.removeVertex(tmp);
        } while (it != m_first_vert);


        // Turn attached ConnEnds into manual points.
        // C++ obstacle.cpp:176-182
        boolean deletedShape = true;
        while (!m_following_conn_ends.isEmpty()) {
            ConnEnd connEnd = m_following_conn_ends.iterator().next();
            connEnd.disconnect(deletedShape);
        }
    }

    /**
     * Updates polyline visibility for all connection pins.
     * Corresponds to Obstacle::updatePinPolyLineVisibility() in C++.
     */
    void updatePinPolyLineVisibility() {
        for (ShapeConnectionPin pin : m_connection_pins) {
            pin.updateVisibility();
        }
    }

    /**
     * Computes polyline visibility for this obstacle using the naive algorithm.
     * Translated from Obstacle::computeVisibilityNaive() in visibility.cpp line 52.
     */
    void computeVisibilityNaive() {
        if (!router().InvisibilityGrph) {
            // Clear shape from graph.
            removeFromGraph();
        }

        VertInf shapeBegin = firstVert();
        VertInf shapeEnd = lastVert().lstNext;

        VertInf pointsBegin = router().vertices.connsBegin();
        for (VertInf curr = shapeBegin; curr != shapeEnd; curr = curr.lstNext) {
            boolean knownNew = true;

            // First Half: vertices before curr in the list
            for (VertInf j = pointsBegin; j != curr; j = j.lstNext) {
                if (j.id.equals(VertInf.dummyOrthogID)) {
                    // Don't include orthogonal dummy vertices.
                    continue;
                }
                EdgeInf.checkEdgeVisibility(curr, j, knownNew);
            }

            // Second Half: vertices after shapeEnd
            VertInf pointsEnd = router().vertices.end();
            for (VertInf k = shapeEnd; k != pointsEnd; k = k.lstNext) {
                if (k.id.equals(VertInf.dummyOrthogID)) {
                    // Don't include orthogonal dummy vertices.
                    continue;
                }
                EdgeInf.checkEdgeVisibility(curr, k, knownNew);
            }
        }
    }

    /**
     * Computes polyline visibility for this obstacle using the sweep algorithm.
     * Translated from Obstacle::computeVisibilitySweep() in visibility.cpp line 97.
     */
    void computeVisibilitySweep() {
        if (!router().InvisibilityGrph) {
            // Clear shape from graph.
            removeFromGraph();
        }

        VertInf startIter = firstVert();
        VertInf endIter = lastVert().lstNext;

        for (VertInf i = startIter; i != endIter; i = i.lstNext) {
            Visibility.vertexSweep(i);
        }
    }

    /**
     * Removes all VertInf nodes of this obstacle from the visibility graph.
     * Corresponds to Obstacle::removeFromGraph() in C++.
     * Iterates the per-obstacle circular chain via shNext (not lstNext).
     */
    void removeFromGraph() {
        if (m_first_vert == null) {
            return;
        }
        boolean isConnPt = false;
        VertInf curr = m_first_vert;
        do {
            VertInf tmp = curr;
            curr = curr.shNext;
            tmp.removeFromGraph(isConnPt);
        } while (curr != m_first_vert);
    }

    /**
     * Returns the bounding box of this obstacle expanded by the router's
     * shapeBufferDistance routing parameter.
     * Corresponds to Obstacle::routingBox() in C++.
     */
    public Box routingBox() {
        double bufferSpace = m_router.routingParameter(Router.RoutingParameter.shapeBufferDistance);
        return m_polygon.offsetBoundingBox(bufferSpace);
    }

    /**
     * Returns the polygon of this obstacle expanded by the router's
     * shapeBufferDistance routing parameter.
     * Corresponds to Obstacle::routingPolygon() in C++.
     */
    public Polygon routingPolygon() {
        double bufferSpace = m_router.routingParameter(Router.RoutingParameter.shapeBufferDistance);
        return m_polygon.offsetPolygon(bufferSpace);
    }

    /**
     * Returns a list of ConnRefs attached to this obstacle.
     * Corresponds to Obstacle::attachedConnectors() in C++.
     */
    public List<ConnRef> attachedConnectors() {
        List<ConnRef> attachedConns = new ArrayList<>();
        for (ConnEnd connEnd : m_following_conn_ends) {
            assert connEnd.m_conn_ref != null;
            attachedConns.add(connEnd.m_conn_ref);
        }
        return attachedConns;
    }

    /**
     * Returns possible pin points for the given pin class ID.
     * Corresponds to Obstacle::possiblePinPoints(unsigned int) in C++.
     */
    public List<Point> possiblePinPoints(int pinClassId) {
        List<Point> points = new ArrayList<>();
        for (ShapeConnectionPin pin : m_connection_pins) {
            if ((pin.m_class_id == pinClassId) &&
                    (!pin.m_exclusive || pin.m_connend_users.isEmpty())) {
                points.add(new Point(pin.m_vertex.point));
            }
        }
        return points;
    }
}
