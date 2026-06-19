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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The ConnEnd class represents different possible endpoints for connectors.
 * Corresponds to ConnEnd in connend.h/connend.cpp.
 */
public class ConnEnd {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnEnd.class);

    ConnEndType m_type;
    Point m_point;
    int m_directions; // ConnDirFlags
    int m_connection_pin_class_id;
    Obstacle m_anchor_obj;
    ConnRef m_conn_ref;
    ShapeConnectionPin m_active_pin;

    /** Default constructor - empty ConnEnd. */
    public ConnEnd() {
        m_type = ConnEndType.ConnEndEmpty;
        m_point = new Point(0, 0);
        m_directions = ConnDirFlag.ConnDirAll;
        m_connection_pin_class_id = ShapeConnectionPin.CONNECTIONPIN_UNSET;
        m_anchor_obj = null;
        m_conn_ref = null;
        m_active_pin = null;
    }

    /** Constructs a ConnEnd from a free-floating point. */
    public ConnEnd(Point point) {
        m_type = ConnEndType.ConnEndPoint;
        m_point = new Point(point);
        m_directions = ConnDirFlag.ConnDirAll;
        m_connection_pin_class_id = ShapeConnectionPin.CONNECTIONPIN_UNSET;
        m_anchor_obj = null;
        m_conn_ref = null;
        m_active_pin = null;
    }

    /** Constructs a ConnEnd from a free-floating point with visibility directions. */
    public ConnEnd(Point point, int visDirs) {
        m_type = ConnEndType.ConnEndPoint;
        m_point = new Point(point);
        m_directions = visDirs;
        m_connection_pin_class_id = ShapeConnectionPin.CONNECTIONPIN_UNSET;
        m_anchor_obj = null;
        m_conn_ref = null;
        m_active_pin = null;
    }

    /** Constructs a ConnEnd attached to a shape pin. */
    public ConnEnd(ShapeRef shapeRef, int connectionPinClassID) {
        m_type = ConnEndType.ConnEndShapePin;
        m_point = new Point(0, 0);
        m_directions = ConnDirFlag.ConnDirAll;
        m_connection_pin_class_id = connectionPinClassID;
        m_anchor_obj = shapeRef;
        m_conn_ref = null;
        m_active_pin = null;

        assert m_anchor_obj != null;
        assert m_connection_pin_class_id > 0;

        m_point = m_anchor_obj.position();
        assert m_connection_pin_class_id != ShapeConnectionPin.CONNECTIONPIN_UNSET;
    }

    /** Constructs a ConnEnd attached to the centre pin of a shape. */
    public ConnEnd(ShapeRef shapeRef) {
        this(shapeRef, ShapeConnectionPin.CONNECTIONPIN_CENTRE);
    }

    /** Copy constructor. */
    public ConnEnd(ConnEnd other) {
        m_type = other.m_type;
        m_point = other.m_point != null ? new Point(other.m_point) : null;
        m_directions = other.m_directions;
        m_connection_pin_class_id = other.m_connection_pin_class_id;
        m_anchor_obj = other.m_anchor_obj;
        m_conn_ref = other.m_conn_ref;
        m_active_pin = other.m_active_pin;
    }

    /** Constructs a ConnEnd attached to a junction. */
    public ConnEnd(JunctionRef junctionRef) {
        m_type = ConnEndType.ConnEndJunction;
        m_directions = ConnDirFlag.ConnDirAll;
        m_connection_pin_class_id = ShapeConnectionPin.CONNECTIONPIN_CENTRE;
        m_anchor_obj = junctionRef;
        m_conn_ref = null;
        m_active_pin = null;

        assert m_anchor_obj != null;
        m_point = m_anchor_obj.position();
    }

    public ConnEndType type() {
        return m_type;
    }

    /**
     * Returns the position of this ConnEnd.
     * If attached to a pin, returns the pin's position.
     * If attached to an anchor object, returns its position.
     * Otherwise returns the stored point.
     * Corresponds to ConnEnd::position() in C++.
     */
    public Point position() {
        if (m_active_pin != null) {
            return m_active_pin.position();
        } else if (m_anchor_obj != null) {
            return m_anchor_obj.position();
        } else {
            return new Point(m_point);
        }
    }

    /**
     * Returns the visibility directions for this ConnEnd.
     * If attached to a pin, returns the pin's directions.
     * Otherwise returns the stored directions.
     * Corresponds to ConnEnd::directions() in C++.
     */
    public int directions() {
        if (m_active_pin != null) {
            return m_active_pin.directions();
        } else {
            return m_directions;
        }
    }

    public ShapeRef shape() {
        if (m_anchor_obj instanceof ShapeRef) {
            return (ShapeRef) m_anchor_obj;
        }
        return null;
    }

    public JunctionRef junction() {
        if (m_anchor_obj instanceof JunctionRef) {
            return (JunctionRef) m_anchor_obj;
        }
        return null;
    }

    public boolean isPinConnection() {
        return (m_type == ConnEndType.ConnEndShapePin) || (m_type == ConnEndType.ConnEndJunction);
    }

    /**
     * Returns the endpoint type (VertID.src or VertID.tar) based on which
     * end of the connector this ConnEnd represents.
     * Corresponds to ConnEnd::endpointType() in C++.
     */
    public int endpointType() {
        assert m_conn_ref != null;
        return (m_conn_ref.m_dst_connend == this) ? VertID.tar : VertID.src;
    }

    /**
     * Creates the connection between a connector and a shape/junction.
     * Corresponds to ConnEnd::connect(ConnRef*) in C++.
     */
    void connect(ConnRef conn) {
        assert isPinConnection();
        assert m_anchor_obj != null;
        assert m_conn_ref == null;

        m_anchor_obj.addFollowingConnEnd(this);
        m_conn_ref = conn;
    }

    /**
     * Removes the connection between a connector and a shape/junction.
     * Corresponds to ConnEnd::disconnect(bool) in C++.
     */
    void disconnect(boolean shapeDeleted) {
        if (m_conn_ref == null) {
            // Not connected.
            return;
        }

        m_point = position();
        if (m_anchor_obj != null) {
            m_anchor_obj.removeFollowingConnEnd(this);
        }
        m_conn_ref = null;

        if (shapeDeleted) {
            // Turn this into a manual ConnEnd.
            m_point = position();
            m_anchor_obj = null;
            m_type = ConnEndType.ConnEndPoint;
            m_connection_pin_class_id = ShapeConnectionPin.CONNECTIONPIN_UNSET;
        }
    }

    void disconnect() {
        disconnect(false);
    }

    /**
     * Marks this ConnEnd as using a particular ShapeConnectionPin.
     * Corresponds to ConnEnd::usePin(ShapeConnectionPin*) in C++.
     */
    void usePin(ShapeConnectionPin pin) {
        assert m_active_pin == null;

        m_active_pin = pin;
        if (m_active_pin != null) {
            m_active_pin.setActiveConn(this);
        }
    }

    /**
     * Marks this ConnEnd as using a particular ShapeConnectionPin's vertex,
     * identified by matching the pin vertex.
     * Corresponds to ConnEnd::usePinVertex(VertInf*) in C++.
     */
    void usePinVertex(VertInf pinVert) {
        assert m_active_pin == null;

        for (ShapeConnectionPin pin : m_anchor_obj.m_connection_pins) {
            if (pin.m_vertex == pinVert) {
                usePin(pin);
                break;
            }
        }
    }

    /**
     * Marks this ConnEnd as no longer using the active ShapeConnectionPin.
     * Corresponds to ConnEnd::freeActivePin() in C++.
     */
    void freeActivePin() {
        if (m_active_pin != null) {
            m_active_pin.releaseActiveConn(this);
        }
        m_active_pin = null;
    }

    /**
     * Returns possible pin points for this ConnEnd.
     * Corresponds to ConnEnd::possiblePinPoints() in C++.
     */
    List<Point> possiblePinPoints() {
        if (m_anchor_obj == null || m_connection_pin_class_id == ShapeConnectionPin.CONNECTIONPIN_UNSET) {
            return new ArrayList<>();
        }
        return m_anchor_obj.possiblePinPoints(m_connection_pin_class_id);
    }

    /**
     * Assigns pin visibility to a dummy vertex representing all the possible pins
     * for this pinClassId.
     * Corresponds to ConnEnd::assignPinVisibilityTo() in C++.
     * Translated from connend.cpp lines 274-370.
     */
    void assignPinVisibilityTo(VertInf dummyConnectionVert, VertInf targetVert) {
        int validPinCount = 0;

        assert m_anchor_obj != null;
        assert m_connection_pin_class_id != ShapeConnectionPin.CONNECTIONPIN_UNSET;

        Router router = m_anchor_obj.router();
        for (ShapeConnectionPin currPin : m_anchor_obj.m_connection_pins)
        {
            if ((currPin.m_class_id == m_connection_pin_class_id) &&
                    (!currPin.m_exclusive || currPin.m_connend_users.isEmpty()))
            {
                double routingCost = currPin.m_connection_cost;
                // adjTargetPt = targetVert->point - currPin->m_vertex->point
                Point adjTargetPt = new Point(
                        targetVert.point.x - currPin.m_vertex.point.x,
                        targetVert.point.y - currPin.m_vertex.point.y);
                double angle = rotationalAngle(adjTargetPt);
                boolean inVisibilityRange = false;

                if (angle <= 45 || angle >= 315)
                {
                    if ((currPin.directions() & ConnDirFlag.ConnDirRight) != 0)
                    {
                        inVisibilityRange = true;
                    }
                }
                if (angle >= 45 && angle <= 135)
                {
                    if ((currPin.directions() & ConnDirFlag.ConnDirDown) != 0)
                    {
                        inVisibilityRange = true;
                    }
                }
                if (angle >= 135 && angle <= 225)
                {
                    if ((currPin.directions() & ConnDirFlag.ConnDirLeft) != 0)
                    {
                        inVisibilityRange = true;
                    }
                }
                if (angle >= 225 && angle <= 315)
                {
                    if ((currPin.directions() & ConnDirFlag.ConnDirUp) != 0)
                    {
                        inVisibilityRange = true;
                    }
                }
                if (!inVisibilityRange)
                {
                    routingCost += router.routingParameter(Router.RoutingParameter.portDirectionPenalty);
                }

                if (router.m_allows_orthogonal_routing)
                {
                    // This has same ID and is either unconnected or not
                    // exclusive, so give it visibility.
                    EdgeInf edge = new EdgeInf(dummyConnectionVert,
                            currPin.m_vertex, true);
                    // XXX Can't use a zero cost due to assumptions elsewhere in code.
                    edge.setDist(Geometry.manhattanDist(dummyConnectionVert.point,
                                currPin.m_vertex.point) +
                            Math.max(0.001, routingCost));
                }

                if (router.m_allows_polyline_routing)
                {
                    // This has same ID and is either unconnected or not
                    // exclusive, so give it visibility.
                    EdgeInf edge = new EdgeInf(dummyConnectionVert,
                            currPin.m_vertex, false);
                    // XXX Can't use a zero cost due to assumptions elsewhere in code.
                    edge.setDist(Geometry.euclideanDist(dummyConnectionVert.point,
                                currPin.m_vertex.point) +
                            Math.max(0.001, routingCost));
                }

                // Increment the number of valid pins for this ConnEnd connection.
                validPinCount++;
            }
        }

        if (validPinCount == 0)
        {
            // There should be at least one pin, otherwise we will have
            // problems finding connector routes.
            LOGGER.error("Warning: In ConnEnd::assignPinVisibilityTo(): ConnEnd for connector {} can't connect to shape {} since it has no pins with class id of {}",
                    (m_conn_ref != null ? m_conn_ref.id() : -1),
                    m_anchor_obj.id(),
                    m_connection_pin_class_id);
        }
    }

    /**
     * Returns the rotational angle (0-360) of a point from the origin.
     * Corresponds to rotationalAngle() in geometry.cpp lines 611-639.
     */
    private static double rotationalAngle(Point p) {
        if (p.y == 0)
        {
            return ((p.x < 0) ? 180 : 0);
        }
        else if (p.x == 0)
        {
            return ((p.y < 0) ? 270 : 90);
        }

        double ang = Math.atan(p.y / p.x);
        ang = (ang * 180) / Math.PI;

        if (p.x < 0)
        {
            ang += 180;
        }
        else if (p.y < 0)
        {
            ang += 360;
        }

        return ang;
    }

    /**
     * Returns a (addedVertex, VertInf) pair for use during hyperedge rerouting.
     * If this ConnEnd is attached to a shape/junction, finds the matching pin vertex.
     * Otherwise creates a new temporary vertex at m_point.
     * Translated from ConnEnd::getHyperedgeVertex in connend.cpp.
     */
    Pair<Boolean, VertInf> getHyperedgeVertex(Router router) {
        boolean addedVertex = false;
        VertInf vertex = null;

        if (m_anchor_obj != null) {
            for (ShapeConnectionPin currPin : m_anchor_obj.m_connection_pins) {
                if ((currPin.m_class_id == m_connection_pin_class_id) &&
                        (!currPin.m_exclusive || currPin.m_connend_users.isEmpty())) {
                    vertex = currPin.m_vertex;
                }
            }
            assert vertex != null;
        } else {
            VertID id = new VertID(0, Point.kUnassignedVertexNumber, VertID.PROP_ConnPoint);
            vertex = new VertInf(router, id, m_point);
            vertex.visDirections = m_directions;
            addedVertex = true;

            if (router.m_allows_polyline_routing) {
                Visibility.vertexVisibility(vertex, null, true, true);
            }
        }

        return new Pair<>(addedVertex, vertex);
    }
}

