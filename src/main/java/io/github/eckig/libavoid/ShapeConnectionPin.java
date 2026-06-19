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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

/**
 * A ShapeConnectionPin defines a point on a shape where connectors can attach.
 * Corresponds to connectionpin.h/connectionpin.cpp in C++.
 *
 * Pins can be exclusive (only one connector at a time) or shared.
 * Pin positions can be specified as proportional (0.0-1.0) or absolute offsets.
 */
public class ShapeConnectionPin implements Comparable<ShapeConnectionPin> {

    // Position constants matching C++ ATTACH_POS_* values
    public static final double ATTACH_POS_TOP = 0;
    public static final double ATTACH_POS_CENTRE = 0.5;
    public static final double ATTACH_POS_BOTTOM = 1.0;
    public static final double ATTACH_POS_LEFT = 0;
    public static final double ATTACH_POS_RIGHT = 1.0;
    /** Absolute offset: place at minimum coordinate. */
    public static final double ATTACH_POS_MIN_OFFSET = 0;
    /** Absolute offset: place at maximum coordinate. */
    public static final double ATTACH_POS_MAX_OFFSET = -1;

    // Sentinel values for pin class IDs matching C++ constants
    public static final int CONNECTIONPIN_UNSET = Integer.MAX_VALUE;
    public static final int CONNECTIONPIN_CENTRE = Integer.MAX_VALUE - 1;

    // kShapeConnectionPin = 9 (from C++ geomtypes.h)
    static final short kShapeConnectionPin = 9;

    int m_class_id;
    Obstacle m_shape;
    double m_x_portion_offset;
    double m_y_portion_offset;
    boolean m_proportional;
    double m_inside_offset;
    int m_visibility_directions;
    boolean m_exclusive;
    double m_connection_cost;
    Set<ConnEnd> m_connend_users;
    // Per-pin visibility vertex (corresponds to C++ m_vertex)
    VertInf m_vertex;

    /**
     * Creates a ShapeConnectionPin on the given shape.
     *
     * @param shape          The shape to attach the pin to.
     * @param classId        The class ID for this pin.
     * @param xPortionOffset The x position as a proportion (0-1) of the shape width,
     *                       or an absolute offset if proportional is false.
     * @param yPortionOffset The y position as a proportion (0-1) of the shape height,
     *                       or an absolute offset if proportional is false.
     * @param proportional   Whether the offsets are proportional.
     * @param insideOffset   Offset from the shape boundary towards the inside.
     * @param visDirs        Visibility directions for this pin.
     */
    public ShapeConnectionPin(ShapeRef shape, int classId, double xPortionOffset,
                               double yPortionOffset, boolean proportional,
                               double insideOffset, int visDirs) {
        assert shape != null;
        assert classId > 0;
        m_shape = shape;
        m_class_id = classId;
        m_x_portion_offset = xPortionOffset;
        m_y_portion_offset = yPortionOffset;
        m_proportional = proportional;
        m_inside_offset = insideOffset;
        m_visibility_directions = visDirs;
        m_exclusive = true; // C++ default: exclusive
        m_connection_cost = 0;
        m_connend_users = Collections.newSetFromMap(new IdentityHashMap<>());
        m_vertex = null;

        shape.addConnectionPin(this);

        Router router = shape.router();
        VertID id = new VertID(shape.id(), kShapeConnectionPin,
                (short)(VertID.PROP_ConnPoint | VertID.PROP_ConnectionPin));
        m_vertex = new VertInf(router, id, this.position());
        m_vertex.visDirections = this.directions();

        if (m_vertex.visDirections == ConnDirFlag.ConnDirAll)
        {
            // A pin with visibility in all directions is not exclusive by default.
            m_exclusive = false;
        }

        if (router.m_allows_polyline_routing)
        {
            Visibility.vertexVisibility(m_vertex, null, true, true);
        }
    }

    /**
     * Compatibility constructor (always proportional).
     * Matches the older C++ ShapeConnectionPin constructor without proportional flag.
     */
    public ShapeConnectionPin(ShapeRef shape, int classId, double xPortionOffset,
                               double yPortionOffset, double insideOffset, int visDirs) {
        this(shape, classId, xPortionOffset, yPortionOffset, true, insideOffset, visDirs);
    }

    /**
     * Constructor for junction connection pins.
     * Corresponds to ShapeConnectionPin(JunctionRef*, uint, ConnDirFlags) in C++.
     */
    public ShapeConnectionPin(JunctionRef junction, int classId, int visDirs) {
        m_shape = junction;
        m_class_id = classId;
        m_x_portion_offset = 0;
        m_y_portion_offset = 0;
        m_proportional = false;
        m_inside_offset = 0;
        m_visibility_directions = visDirs;
        m_exclusive = true;
        m_connection_cost = 0;
        m_connend_users = Collections.newSetFromMap(new IdentityHashMap<>());
        m_vertex = null;

        junction.addConnectionPin(this);

        // XXX These IDs should really be uniquely identifiable in case there
        //     are multiple pins on a shape. I think currently this case will
        //     break rubber-band routing.
        Router router = junction.router();
        VertID id = new VertID(junction.id(), kShapeConnectionPin,
                (short)(VertID.PROP_ConnPoint | VertID.PROP_ConnectionPin));
        m_vertex = new VertInf(router, id, junction.position());
        m_vertex.visDirections = visDirs;

        if (router.m_allows_polyline_routing)
        {
            Visibility.vertexVisibility(m_vertex, null, true, true);
        }
    }

    /**
     * Constructor for junction connection pins with default directions.
     */
    public ShapeConnectionPin(JunctionRef junction, int classId) {
        this(junction, classId, ConnDirFlag.ConnDirNone);
    }

    public void setExclusive(boolean exclusive) {
        m_exclusive = exclusive;
    }

    public boolean isExclusive() {
        return m_exclusive;
    }

    /**
     * Sets the connection cost for this pin. Used to prefer some pins over
     * others during routing.
     * Corresponds to ShapeConnectionPin::setConnectionCost(double) in C++.
     */
    public void setConnectionCost(double cost) {
        assert cost >= 0;
        m_connection_cost = cost;
    }

    /**
     * Returns the visibility directions for this pin.
     * If no explicit directions are set, auto-determines the direction
     * based on the pin's position relative to the shape boundary.
     * Corresponds to ShapeConnectionPin::directions() in C++.
     */
    public int directions() {
        int visDir = m_visibility_directions;
        if (m_visibility_directions == ConnDirFlag.ConnDirNone)
        {
            // None is set, use the defaults:
            if (m_x_portion_offset == ATTACH_POS_LEFT)
            {
                visDir |= ConnDirFlag.ConnDirLeft;
            }
            else if (m_x_portion_offset == ATTACH_POS_RIGHT)
            {
                visDir |= ConnDirFlag.ConnDirRight;
            }

            if (m_y_portion_offset == ATTACH_POS_TOP)
            {
                visDir |= ConnDirFlag.ConnDirUp;
            }
            else if (m_y_portion_offset == ATTACH_POS_BOTTOM)
            {
                visDir |= ConnDirFlag.ConnDirDown;
            }

            if (visDir == ConnDirFlag.ConnDirNone)
            {
                visDir = ConnDirFlag.ConnDirAll;
            }
        }
        return visDir;
    }

    /**
     * Returns the absolute position of this pin based on the shape's current polygon.
     */
    public Point position() {
        return position(m_shape.polygon());
    }

    /**
     * Returns the absolute position of this pin based on the given polygon.
     * Used when moving shapes to compute position with the new polygon.
     * Corresponds to ShapeConnectionPin::position(const Polygon&) in C++.
     */
    public Point position(Polygon poly) {
        if (m_shape instanceof JunctionRef) {
            return m_shape.position();
        }

        if (poly == null || poly.empty()) {
            poly = m_shape.polygon();
        }
        final Box shapeBox = poly.offsetBoundingBox(0);
        Point point = new Point();

        if (m_proportional)
        {
            // We want to place connection points exactly on the edges of shapes,
            // or possibly slightly inside them (if m_inside_offset is set).
            if (m_x_portion_offset == ATTACH_POS_LEFT)
            {
                point.x = shapeBox.min.x + m_inside_offset;
                point.vn = 6;
            }
            else if (m_x_portion_offset == ATTACH_POS_RIGHT)
            {
                point.x = shapeBox.max.x - m_inside_offset;
                point.vn = 4;
            }
            else
            {
                point.x = shapeBox.min.x + (m_x_portion_offset * shapeBox.width());
            }

            if (m_y_portion_offset == ATTACH_POS_TOP)
            {
                point.y = shapeBox.min.y + m_inside_offset;
                point.vn = 5;
            }
            else if (m_y_portion_offset == ATTACH_POS_BOTTOM)
            {
                point.y = shapeBox.max.y - m_inside_offset;
                point.vn = 7;
            }
            else
            {
                point.y = shapeBox.min.y + (m_y_portion_offset * shapeBox.height());
            }
        }
        else
        {
            // Using absolute offsets for connection pin position.
            if (m_x_portion_offset == ATTACH_POS_MIN_OFFSET)
            {
                point.x = shapeBox.min.x + m_inside_offset;
                point.vn = 6;
            }
            else if ((m_x_portion_offset == ATTACH_POS_MAX_OFFSET) ||
                     (m_x_portion_offset == shapeBox.width()))
            {
                point.x = shapeBox.max.x - m_inside_offset;
                point.vn = 4;
            }
            else
            {
                point.x = shapeBox.min.x + m_x_portion_offset;
            }

            if (m_y_portion_offset == ATTACH_POS_MIN_OFFSET)
            {
                point.y = shapeBox.min.y + m_inside_offset;
                point.vn = 5;
            }
            else if ((m_y_portion_offset == ATTACH_POS_MAX_OFFSET) ||
                     (m_y_portion_offset == shapeBox.height()))
            {
                point.y = shapeBox.max.y - m_inside_offset;
                point.vn = 7;
            }
            else
            {
                point.y = shapeBox.min.y + m_y_portion_offset;
            }
        }

        return point;
    }

    void setActiveConn(ConnEnd connEnd) {
        if (connEnd != null) {
            m_connend_users.add(connEnd);
        }
    }

    void releaseActiveConn(ConnEnd connEnd) {
        m_connend_users.remove(connEnd);
    }

    /**
     * Returns a pair of (containingObjectId, classId) for this pin.
     * Corresponds to ShapeConnectionPin::ids() in C++.
     */
    public int[] ids() {
        return new int[]{m_shape.id(), m_class_id};
    }

    int containingObjectId() {
        assert m_shape != null;
        return m_shape.id();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ShapeConnectionPin rhs)) {
            return false;
        }

        assert m_shape.router() == rhs.m_shape.router();

        if (containingObjectId() != rhs.containingObjectId()) {
            return false;
        }
        if (m_class_id != rhs.m_class_id) {
            return false;
        }
        if (m_visibility_directions != rhs.m_visibility_directions) {
            return false;
        }
        if (m_x_portion_offset != rhs.m_x_portion_offset) {
            return false;
        }
        if (m_y_portion_offset != rhs.m_y_portion_offset) {
            return false;
        }
        if (m_inside_offset != rhs.m_inside_offset) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(containingObjectId(), m_class_id,
                m_visibility_directions, m_x_portion_offset,
                m_y_portion_offset, m_inside_offset);
    }

    @Override
    public int compareTo(ShapeConnectionPin rhs) {
        assert m_shape.router() == rhs.m_shape.router();

        if (containingObjectId() != rhs.containingObjectId()) {
            return Integer.compare(containingObjectId(), rhs.containingObjectId());
        }
        if (m_class_id != rhs.m_class_id) {
            return Integer.compare(m_class_id, rhs.m_class_id);
        }
        if (m_visibility_directions != rhs.m_visibility_directions) {
            return Integer.compare(m_visibility_directions, rhs.m_visibility_directions);
        }
        if (m_x_portion_offset != rhs.m_x_portion_offset) {
            return Double.compare(m_x_portion_offset, rhs.m_x_portion_offset);
        }
        if (m_y_portion_offset != rhs.m_y_portion_offset) {
            return Double.compare(m_y_portion_offset, rhs.m_y_portion_offset);
        }
        if (m_inside_offset != rhs.m_inside_offset) {
            return Double.compare(m_inside_offset, rhs.m_inside_offset);
        }
        return 0;
    }

    /**
     * Updates the position and visibility of this pin's vertex.
     * Corresponds to ShapeConnectionPin::updatePositionAndVisibility() in C++.
     */
    void updatePositionAndVisibility() {
        m_vertex.reset(this.position());
        m_vertex.visDirections = this.directions();
        updateVisibility();
    }

    /**
     * Updates the position cache based on the new polygon.
     * Corresponds to ShapeConnectionPin::updatePosition(const Polygon&) in C++.
     */
    void updatePosition(Polygon newPoly) {
        m_vertex.reset(position(newPoly));
    }

    /**
     * Updates position for junction pins.
     * Corresponds to ShapeConnectionPin::updatePosition(const Point&) in C++.
     */
    void updatePosition(Point newPosition) {
        m_vertex.reset(newPosition);
    }

    /**
     * Updates polyline visibility edges for this pin's vertex.
     * Corresponds to ShapeConnectionPin::updateVisibility() in C++.
     *
     * In C++, this removes all edges from m_vertex then re-creates polyline
     * visibility edges if m_router->m_allows_polyline_routing is true.
     */
    void updateVisibility() {
        m_vertex.removeFromGraph();
        if (m_shape.router().m_allows_polyline_routing)
        {
            Visibility.vertexVisibility(m_vertex, null, true, true);
        }
    }

    /**
     * Disposes of this pin, performing the same cleanup as the C++ destructor
     * {@code ShapeConnectionPin::~ShapeConnectionPin()}.
     *
     * <ol>
     *   <li>Removes the pin from its containing shape or junction.</li>
     *   <li>Disconnects any {@link ConnEnd} objects currently using this pin.</li>
     *   <li>Removes the pin's visibility vertex from the graph and the
     *       router's vertex list.</li>
     * </ol>
     *
     * After calling this method the pin must not be used again.
     */
    public void dispose() {
        assert m_shape != null;

        // Remove from containing obstacle (shape or junction).
        m_shape.removeConnectionPin(this);

        // Disconnect connend users.
        while (!m_connend_users.isEmpty()) {
            ConnEnd connEnd = m_connend_users.iterator().next();
            connEnd.freeActivePin();
        }

        // Remove vertex from visibility graph.
        removeFromGraph();
    }

    /**
     * Removes this pin's vertex from the visibility graph and vertex list.
     * Called during pin destruction / dispose.
     * Corresponds to the vertex-cleanup part of the C++ destructor.
     */
    void removeFromGraph() {
        if (m_vertex != null)
        {
            m_vertex.removeFromGraph();
            m_shape.router().vertices.removeVertex(m_vertex);
            m_vertex = null;
        }
    }

    public int classId() {
        return m_class_id;
    }
}
