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
 * A ConnRef represents a connector in the routing scene.
 * Corresponds to connector.h/connector.cpp in C++.
 */
public class ConnRef {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnRef.class);

    // -----------------------------------------------------------------------
    // Fields — match C++ ConnRef private members
    // -----------------------------------------------------------------------

    Router m_router;
    int m_id;
    ConnType m_type;
    boolean[] m_reroute_flag_ptr;   // pointer into ConnRerouteFlagDelegate
    boolean m_needs_reroute_flag;
    boolean m_false_path;
    boolean m_needs_repaint;
    boolean m_active;
    boolean m_initialised;
    boolean m_hate_crossings;
    boolean m_has_fixed_route;
    Polygon m_route;
    Polygon m_display_route;
    double m_route_dist;
    // m_connrefs_pos: not needed in Java (List handles removal by reference)
    VertInf m_src_vert;
    VertInf m_dst_vert;
    VertInf m_start_vert;
    // m_callback_func / m_connector: adapted for Java
    Runnable m_callback_func;
    Object m_connector;
    ConnEnd m_src_connend;
    ConnEnd m_dst_connend;
    List<VertInf> m_checkpoint_vertices;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Constructs a connector with no endpoints specified.
     * Corresponds to ConnRef::ConnRef(Router *router, const unsigned int id).
     */
    public ConnRef(Router router, int id) {
        m_router = router;
        m_type = router.validConnType();
        m_reroute_flag_ptr = null;
        m_needs_reroute_flag = true;
        m_false_path = false;
        m_needs_repaint = false;
        m_active = false;
        m_hate_crossings = false;
        m_has_fixed_route = false;
        m_route_dist = 0;
        m_src_vert = null;
        m_dst_vert = null;
        m_start_vert = null;
        m_callback_func = null;
        m_connector = null;
        m_src_connend = null;
        m_dst_connend = null;
        m_checkpoint_vertices = new ArrayList<>();

        m_id = m_router.assignId(id);
        m_route = new Polygon();
        m_display_route = new Polygon();
        m_reroute_flag_ptr = m_router.m_conn_reroute_flags.addConn(this);
    }

    /**
     * Constructs a connector with no endpoints specified (auto-assigned ID).
     */
    public ConnRef(Router router) {
        this(router, 0);
    }

    /**
     * Constructs a connector with endpoints specified.
     * Corresponds to ConnRef::ConnRef(Router *router, const ConnEnd& src, const ConnEnd& dst, const unsigned int id).
     */
    public ConnRef(Router router, ConnEnd src, ConnEnd dst, int id) {
        m_router = router;
        m_type = router.validConnType();
        m_reroute_flag_ptr = null;
        m_needs_reroute_flag = true;
        m_false_path = false;
        m_needs_repaint = false;
        m_active = false;
        m_hate_crossings = false;
        m_has_fixed_route = false;
        m_route_dist = 0;
        m_src_vert = null;
        m_dst_vert = null;
        m_start_vert = null;
        m_callback_func = null;
        m_connector = null;
        m_src_connend = null;
        m_dst_connend = null;
        m_checkpoint_vertices = new ArrayList<>();

        m_id = m_router.assignId(id);
        m_route = new Polygon();
        m_display_route = new Polygon();
        m_reroute_flag_ptr = m_router.m_conn_reroute_flags.addConn(this);

        // Set endpoint values.
        setEndpoints(src, dst);
    }

    /**
     * Constructs a connector with endpoints specified (auto-assigned ID).
     */
    public ConnRef(Router router, ConnEnd src, ConnEnd dst) {
        this(router, src, dst, 0);
    }

    /**
     * Constructs a connector with free-floating point endpoints.
     * Corresponds to the C++ ConnRef constructor taking Point endpoints.
     */
    public ConnRef(Router router, Point src, Point dst, int id) {
        this(router, new ConnEnd(src), new ConnEnd(dst), id);
    }

    /**
     * Constructs a connector with free-floating point endpoints and an
     * auto-assigned ID.
     */
    public ConnRef(Router router, Point src, Point dst) {
        this(router, src, dst, 0);
    }

    public ConnRef(Router router, Point src, ConnEnd dst, int id) {
        this(router, new ConnEnd(src), dst, id);
    }

    public ConnRef(Router router, Point src, ConnEnd dst) {
        this(router, src, dst, 0);
    }

    public ConnRef(Router router, ConnEnd src, Point dst, int id) {
        this(router, src, new ConnEnd(dst), id);
    }

    public ConnRef(Router router, ConnEnd src, Point dst) {
        this(router, src, dst, 0);
    }

    /**
     * Destructor logic — call when removing a connector from the router.
     * Corresponds to ConnRef::~ConnRef().
     */
    void destroy() {
        m_router.m_conn_reroute_flags.removeConn(this);
        m_router.removeObjectFromQueuedActions(this);

        freeRoutes();

        if (m_src_vert != null) {
            m_src_vert.removeFromGraph();
            m_router.vertices.removeVertex(m_src_vert);
            m_src_vert = null;
        }
        if (m_src_connend != null) {
            m_src_connend.disconnect();
            m_src_connend.freeActivePin();
            m_src_connend = null;
        }

        if (m_dst_vert != null) {
            m_dst_vert.removeFromGraph();
            m_router.vertices.removeVertex(m_dst_vert);
            m_dst_vert = null;
        }
        if (m_dst_connend != null) {
            m_dst_connend.disconnect();
            m_dst_connend.freeActivePin();
            m_dst_connend = null;
        }

        // Clear checkpoint vertices.
        for (VertInf v : m_checkpoint_vertices) {
            v.removeFromGraph(true);
            m_router.vertices.removeVertex(v);
        }
        m_checkpoint_vertices.clear();

        if (m_active) {
            makeInactive();
        }
    }

    // -----------------------------------------------------------------------
    // Public API methods
    // -----------------------------------------------------------------------

    /**
     * Returns the routing type for this connector.
     * Corresponds to ConnRef::routingType().
     */
    public ConnType routingType() {
        return m_type;
    }

    /**
     * Sets the routing type for this connector.
     * Corresponds to ConnRef::setRoutingType(ConnType).
     */
    public void setRoutingType(ConnType type) {
        type = m_router.validConnType(type);
        if (m_type != type) {
            m_type = type;
            makePathInvalid();
            m_router.modifyConnector(this);
        }
    }

    /**
     * Sets both endpoints for this connector.
     * Corresponds to ConnRef::setEndpoints(ConnEnd&, ConnEnd&).
     */
    public void setEndpoints(ConnEnd srcPoint, ConnEnd dstPoint) {
        m_router.modifyConnector(this, VertID.src, srcPoint);
        m_router.modifyConnector(this, VertID.tar, dstPoint);
    }

    public void setEndpoints(Point srcPoint, Point dstPoint) {
        setEndpoints(new ConnEnd(srcPoint), new ConnEnd(dstPoint));
    }

    /**
     * Sets the endpoint using an int type (VertID.src or VertID.tar).
     * Corresponds to ConnRef::setEndpoint(unsigned int, ConnEnd&).
     */
    public void setEndpoint(int type, ConnEnd connEnd) {
        m_router.modifyConnector(this, type, connEnd);
    }

    public void setEndpoint(int type, Point point) {
        setEndpoint(type, new ConnEnd(point));
    }

    /**
     * Sets just the source endpoint.
     * Corresponds to ConnRef::setSourceEndpoint(ConnEnd&).
     */
    public void setSourceEndpoint(ConnEnd srcPoint) {
        m_router.modifyConnector(this, VertID.src, srcPoint);
    }

    public void setSourceEndpoint(Point srcPoint) {
        setSourceEndpoint(new ConnEnd(srcPoint));
    }

    /**
     * Sets just the destination endpoint.
     * Corresponds to ConnRef::setDestEndpoint(ConnEnd&).
     */
    public void setDestEndpoint(ConnEnd dstPoint) {
        m_router.modifyConnector(this, VertID.tar, dstPoint);
    }

    public void setDestEndpoint(Point dstPoint) {
        setDestEndpoint(new ConnEnd(dstPoint));
    }

    /**
     * Returns the ID of this connector.
     * Corresponds to ConnRef::id().
     */
    public int id() {
        return m_id;
    }

    /**
     * Returns the router this connector belongs to.
     * Corresponds to ConnRef::router().
     */
    public Router router() {
        return m_router;
    }

    /**
     * Returns whether this connector needs repainting.
     * Corresponds to ConnRef::needsRepaint().
     */
    public boolean needsRepaint() {
        return m_needs_repaint;
    }

    /**
     * Returns the raw route (debug version, not post-processed).
     * Corresponds to ConnRef::route().
     */
    public Polygon route() {
        return m_route;
    }

    /**
     * Returns the display route (simplified, post-processed).
     * Corresponds to ConnRef::displayRoute().
     */
    public Polygon displayRoute() {
        if (m_display_route.empty()) {
            // No displayRoute is set. Simplify the current route to get it.
            m_display_route = m_route.simplify();
        }
        return m_display_route;
    }

    /**
     * Sets a callback to be called when the connector is rerouted.
     * Corresponds to ConnRef::setCallback(void (*cb)(void *), void *ptr).
     */
    public void setCallback(Runnable callback) {
        m_callback_func = callback;
    }

    /**
     * Sets the routing type for this connector.
     * Corresponds to ConnRef::setRoutingType(ConnType).
     */
    public void setHateCrossings(boolean value) {
        m_hate_crossings = value;
    }

    /**
     * Returns whether this connector hates crossings.
     * Corresponds to ConnRef::doesHateCrossings().
     */
    public boolean doesHateCrossings() {
        return m_hate_crossings;
    }

    /**
     * Returns possible destination pin points.
     * Corresponds to ConnRef::possibleDstPinPoints().
     */
    public List<Point> possibleDstPinPoints() {
        List<Point> points = new ArrayList<>();
        if (m_dst_connend != null) {
            points = m_dst_connend.possiblePinPoints();
        }
        return points;
    }

    /**
     * Splits the connector at the given segment, creating a new junction and
     * a second connector.
     * Corresponds to ConnRef::splitAtSegment(size_t segmentN).
     */
    public Pair<JunctionRef, ConnRef> splitAtSegment(int segmentN) {
        ConnRef newConn = null;
        JunctionRef newJunction = null;

        if (m_display_route.size() > segmentN) {
            // Position the junction at the midpoint of the desired segment.
            Point junctionPos = midpoint(m_display_route.at(segmentN - 1),
                    m_display_route.at(segmentN));

            // Create the new junction.
            newJunction = new JunctionRef(router(), junctionPos);
            router().addJunction(newJunction);
            newJunction.preferOrthogonalDimension(
                    (m_display_route.at(segmentN - 1).x ==
                        m_display_route.at(segmentN).x) ? 1 : 0); // YDIM=1, XDIM=0

            // Create a new connection routing from the junction to the original
            // connector's endpoint.
            ConnEnd newConnSrc = new ConnEnd(newJunction);
            ConnEnd newConnDst = new ConnEnd(m_dst_connend);
            newConn = new ConnRef(router(), newConnSrc, newConnDst);

            // Reroute the endpoint of the original connector to attach to the
            // new junction.
            ConnEnd oldConnDst = new ConnEnd(newJunction);
            this.setDestEndpoint(oldConnDst);
        }

        Pair<JunctionRef, ConnRef> result = new Pair<>();
        result.first = newJunction;
        result.second = newConn;
        return result;
    }

    /**
     * Sets a fixed user-specified route for this connector.
     * Corresponds to ConnRef::setFixedRoute(PolyLine&).
     */
    public void setFixedRoute(Polygon route) {
        if (route.size() >= 2) {
            // Set endpoints based on the fixed route in case the
            // fixed route is later cleared.
            setEndpoints(new ConnEnd(route.ps.getFirst()), new ConnEnd(route.ps.get(route.size() - 1)));
        }
        m_has_fixed_route = true;
        m_route = new Polygon(route);
        m_display_route = m_route.simplify();
        m_router.registerSettingsChange();
    }

    /**
     * Sets a fixed existing route for this connector (keeps current endpoints).
     * Corresponds to ConnRef::setFixedExistingRoute().
     */
    public void setFixedExistingRoute() {
        assert m_route.size() >= 2;
        m_has_fixed_route = true;
        m_router.registerSettingsChange();
    }

    /**
     * Returns whether this connector has a fixed route.
     * Corresponds to ConnRef::hasFixedRoute().
     */
    public boolean hasFixedRoute() {
        return m_has_fixed_route;
    }

    /**
     * Clears the fixed route, allowing automatic rerouting.
     * Corresponds to ConnRef::clearFixedRoute().
     */
    public void clearFixedRoute() {
        m_has_fixed_route = false;
        makePathInvalid();
        m_router.registerSettingsChange();
    }

    /**
     * Returns an array of [srcConnEnd, dstConnEnd] for this connector.
     * Used by HyperedgeTreeNode and HyperedgeTreeEdge.
     */
    public ConnEnd[] endpointConnEndsArray() {
        ConnEnd[] result = new ConnEnd[2];
        result[0] = new ConnEnd();
        result[1] = new ConnEnd();
        getConnEndForEndpointVertex(m_src_vert, result, 0);
        getConnEndForEndpointVertex(m_dst_vert, result, 1);
        return result;
    }

    /**
     * 1-arg version of getConnEndForEndpointVertex — returns ConnEnd or null.
     * Used by HyperedgeTreeEdge.
     */
    public ConnEnd getConnEndForEndpointVertex(VertInf vertex) {
        ConnEnd[] result = new ConnEnd[]{null};
        if (vertex == m_src_vert) {
            result[0] = new ConnEnd();
            getConnEndForEndpointVertex(m_src_vert, result, 0);
        } else if (vertex == m_dst_vert) {
            result[0] = new ConnEnd();
            getConnEndForEndpointVertex(m_dst_vert, result, 0);
        }
        return result[0];
    }

    /**
     * Returns ConnEnds specifying what this connector is attached to.
     * Corresponds to ConnRef::endpointConnEnds().
     */
    public Pair<ConnEnd, ConnEnd> endpointConnEnds() {
        ConnEnd[] endpoints = new ConnEnd[2];
        endpoints[0] = new ConnEnd();
        endpoints[1] = new ConnEnd();
        getConnEndForEndpointVertex(m_src_vert, endpoints, 0);
        getConnEndForEndpointVertex(m_dst_vert, endpoints, 1);
        Pair<ConnEnd, ConnEnd> endpointResult = new Pair<>();
        endpointResult.first = endpoints[0];
        endpointResult.second = endpoints[1];
        return endpointResult;
    }

    /**
     * Returns the source endpoint vertex in the visibility graph.
     * Corresponds to ConnRef::src().
     */
    public VertInf src() {
        return m_src_vert;
    }

    /**
     * Returns the destination endpoint vertex in the visibility graph.
     * Corresponds to ConnRef::dst().
     */
    public VertInf dst() {
        return m_dst_vert;
    }

    // -----------------------------------------------------------------------
    // Package-private / internal methods
    // -----------------------------------------------------------------------

    /**
     * Returns the start vertex for pathfinding (may differ from src for rubber-band routing).
     * Corresponds to ConnRef::start().
     */
    VertInf start() {
        return m_start_vert;
    }

    /**
     * Returns whether this connector is initialised (active).
     * Corresponds to ConnRef::isInitialised().
     */
    public boolean isInitialised() {
        return m_active;
    }

    /**
     * Uninitialises this connector (removes vertices, makes inactive).
     * Corresponds to ConnRef::unInitialise().
     * TODO why not used
     */
    void unInitialise() {
        m_router.vertices.removeVertex(m_src_vert);
        m_router.vertices.removeVertex(m_dst_vert);
        makeInactive();
    }

    /**
     * Removes this connector's vertices from the visibility graph.
     * Corresponds to ConnRef::removeFromGraph().
     * TODO why not used
     */
    void removeFromGraph() {
        if (m_src_vert != null) {
            m_src_vert.removeFromGraph();
        }
        if (m_dst_vert != null) {
            m_dst_vert.removeFromGraph();
        }
    }

    /**
     * Adds this connector to the router's connector list.
     * Corresponds to ConnRef::makeActive().
     */
    void makeActive() {
        assert !m_active;
        // Add to connRefs list.
        m_router.m_connectors.addFirst(this);
        m_active = true;
    }

    /**
     * Removes this connector from the router's connector list.
     * Corresponds to ConnRef::makeInactive().
     */
    void makeInactive() {
        assert m_active;
        // Remove from connRefs list.
        m_router.m_connectors.remove(this);
        m_active = false;
    }

    /**
     * Frees active pins for both endpoints.
     * Corresponds to ConnRef::freeActivePins().
     */
    void freeActivePins() {
        if (m_src_connend != null) {
            m_src_connend.freeActivePin();
        }
        if (m_dst_connend != null) {
            m_dst_connend.freeActivePin();
        }
    }

    /**
     * Clears the route and display route.
     * Corresponds to ConnRef::freeRoutes().
     */
    void freeRoutes() {
        m_route = new Polygon();
        m_display_route = new Polygon();
    }

    /**
     * Returns a reference to the route (for modification).
     * Corresponds to ConnRef::routeRef().
     */
    Polygon routeRef() {
        return m_route;
    }

    /**
     * Sets the display route from a given route.
     * Corresponds to ConnRef::set_route(PolyLine&).
     */
    void set_route(Polygon route) {
        if (m_display_route == route) {
            LOGGER.error("Error: Trying to update libavoid route with itself.");
            return;
        }
        m_display_route.ps = new ArrayList<>(route.ps.size());
        for (Point point : route.ps) {
            m_display_route.ps.add(new Point(point));
        }
    }

    /**
     * Marks the path as invalid, requiring rerouting.
     * Corresponds to ConnRef::makePathInvalid().
     */
    public void makePathInvalid() {
        m_needs_reroute_flag = true;
    }

    /**
     * Calls the registered callback, if any.
     * Corresponds to ConnRef::performCallback().
     */
    void performCallback() {
        if (m_callback_func != null) {
            m_callback_func.run();
        }
    }

    /**
     * Returns whether this connector needs rerouting.
     */
    boolean needsReroute() {
        return m_needs_reroute_flag;
    }

    /**
     * Updates one endpoint of the connector.
     * Corresponds to ConnRef::updateEndPoint(unsigned int type, ConnEnd& connEnd).
     */
    void updateEndPoint(int type, ConnEnd connEnd) {
        common_updateEndPoint(type, connEnd);

        if (m_has_fixed_route) {
            // Don't need to continue and compute visibility if route is fixed.
            return;
        }

        if (m_router.m_allows_polyline_routing) {
            boolean knownNew = true;
            boolean genContains = true;
            if (type == VertID.src) {
                boolean dummySrc = m_src_connend != null && m_src_connend.isPinConnection();
                if (!dummySrc) {
                    // Only generate visibility if not attached to a pin.
                    Visibility.vertexVisibility(m_src_vert, m_dst_vert, knownNew, genContains);
                }
            } else {
                boolean dummyDst = m_dst_connend != null && m_dst_connend.isPinConnection();
                if (!dummyDst) {
                    // Only generate visibility if not attached to a pin.
                    Visibility.vertexVisibility(m_dst_vert, m_src_vert, knownNew, genContains);
                }
            }
        }
    }

    /**
     * Core endpoint update — creates/resets VertInf vertices and manages ConnEnd lifecycle.
     * Corresponds to ConnRef::common_updateEndPoint(unsigned int type, ConnEnd connEnd).
     */
    void common_updateEndPoint(int type, ConnEnd connEnd) {
        Point point = connEnd.position();

        // The connEnd is a copy of a ConnEnd that will get disconnected,
        // so don't leave it looking like it is still connected.
        connEnd.m_conn_ref = null;

        if (!m_active) {
            makeActive();
        }

        VertInf altered;

        int properties = VertID.PROP_ConnPoint;
        if (connEnd.isPinConnection()) {
            properties |= VertID.PROP_DummyPinHelper;
        }
        VertID ptID = new VertID(m_id, (short)type, (short)properties);
        if (type == VertID.src) {
            if (m_src_vert != null) {
                m_src_vert.reset(ptID, point);
            } else {
                m_src_vert = new VertInf(m_router, ptID, point);
            }
            m_src_vert.visDirections = connEnd.directions();

            if (m_src_connend != null) {
                m_src_connend.disconnect();
                m_src_connend.freeActivePin();
                m_src_connend = null;
            }
            if (connEnd.isPinConnection()) {
                m_src_connend = new ConnEnd(connEnd);
                m_src_connend.connect(this);
                // Don't need this to have visibility since we won't
                // be connecting to it.
                m_src_vert.visDirections = ConnDirFlag.ConnDirNone;
            }

            altered = m_src_vert;
        } else { // type == VertID.tar
            if (m_dst_vert != null) {
                m_dst_vert.reset(ptID, point);
            } else {
                m_dst_vert = new VertInf(m_router, ptID, point);
            }
            m_dst_vert.visDirections = connEnd.directions();

            if (m_dst_connend != null) {
                m_dst_connend.disconnect();
                m_dst_connend.freeActivePin();
                m_dst_connend = null;
            }
            if (connEnd.isPinConnection()) {
                m_dst_connend = new ConnEnd(connEnd);
                m_dst_connend.connect(this);
                // Don't need this to have visibility since we won't
                // be connecting to it.
                m_dst_vert.visDirections = ConnDirFlag.ConnDirNone;
            }

            altered = m_dst_vert;
        }

        // XXX: Seems to be faster to just remove the edges and recreate
        boolean isConn = true;
        altered.removeFromGraph(isConn);

        makePathInvalid();
        m_router.setStaticGraphInvalidated(true);
    }

    /**
     * Given the start or end vertex of a connector, returns the ConnEnd that
     * can be used to reproduce that endpoint.
     * Corresponds to ConnRef::getConnEndForEndpointVertex(VertInf *vertex, ConnEnd& connEnd).
     */
    void getConnEndForEndpointVertex(VertInf vertex, ConnEnd[] result, int idx) {
        if (vertex == null) {
            LOGGER.error("Warning: In ConnRef::getConnEndForEndpointVertex(): ConnEnd for connector {} is uninitialised.", id());
            return;
        }

        if (vertex == m_src_vert) {
            if (m_src_connend != null) {
                result[idx] = new ConnEnd(m_src_connend);
            } else {
                result[idx] = new ConnEnd(
                        new Point(m_src_vert.point.x, m_src_vert.point.y),
                        m_src_vert.visDirections);
            }
        } else if (vertex == m_dst_vert) {
            if (m_dst_connend != null) {
                result[idx] = new ConnEnd(m_dst_connend);
            } else {
                result[idx] = new ConnEnd(
                        new Point(m_dst_vert.point.x, m_dst_vert.point.y),
                        m_dst_vert.visDirections);
            }
        }
    }

    /**
     * Returns the obstacles anchoring the source and destination endpoints.
     * Corresponds to ConnRef::endpointAnchors().
     */
    Obstacle[] endpointAnchors() {
        Obstacle srcAnchor = null;
        Obstacle dstAnchor = null;

        if (m_src_connend != null) {
            srcAnchor = m_src_connend.m_anchor_obj;
        }
        if (m_dst_connend != null) {
            dstAnchor = m_dst_connend.m_anchor_obj;
        }
        if (srcAnchor == null && dstAnchor == null) {
            return null;
        }
        return new Obstacle[]{srcAnchor, dstAnchor};
    }

    /**
     * Assigns or removes connection pin visibility for this connector.
     * When connect is true, temporary visibility edges are added to the graph
     * connecting the connector's endpoints to their respective connection pins.
     * When false, those edges are removed.
     * Corresponds to ConnRef::assignConnectionPinVisibility(bool connect).
     */
    Pair<Boolean, Boolean> assignConnectionPinVisibility(boolean connect) {
        // XXX This is kind of a hack for connection pins.  Probably we want to
        //     not use m_src_vert and m_dst_vert.  For the moment we will clear
        //     their visibility and give them visibility to the pins.
        boolean dummySrc = m_src_connend != null && m_src_connend.isPinConnection();
        if (dummySrc) {
            m_src_vert.removeFromGraph();
            if (connect) {
                m_src_connend.assignPinVisibilityTo(m_src_vert, m_dst_vert);
            }
        }
        boolean dummyDst = m_dst_connend != null && m_dst_connend.isPinConnection();
        if (dummyDst) {
            m_dst_vert.removeFromGraph();
            if (connect) {
                m_dst_connend.assignPinVisibilityTo(m_dst_vert, m_src_vert);
            }
        }

        return new Pair<>(dummySrc, dummyDst);
    }

    /**
     * Generates a path for this connector using the visibility graph and A* search.
     * Corresponds to ConnRef::generatePath().
     */
    boolean generatePath() {
        if (!m_false_path && !m_needs_reroute_flag) {
            // This connector is up to date.
            return false;
        }

        if (m_dst_vert == null || m_src_vert == null) {
            // Connector is not fully initialised.
            return false;
        }

        m_false_path = false;
        m_needs_reroute_flag = false;

        m_start_vert = m_src_vert;

        // Some connectors may attach to connection pins, which means they route
        // to the closest of multiple pins on a shape.  How we handle this is to
        // add a dummy vertex as the source or target vertex.  This is then given
        // visibility to each of the possible pins and tiny distance.  Here we
        // assign this visibility by adding edges to the visibility graph that we
        // later remove.
        Pair<Boolean, Boolean> isDummyAtEnd = assignConnectionPinVisibility(true);

        if (m_router.RubberBandRouting && route().size() > 0) {
            if (isDummyAtEnd.first) {
                Point firstPoint = new Point(m_src_vert.point);
                firstPoint.id = m_src_vert.id.objID;
                firstPoint.vn = m_src_vert.id.vn;
                Polygon existingRoute = routeRef();
                existingRoute.ps.addFirst(firstPoint);
            }
        }

        List<Point> path = new ArrayList<>();
        List<VertInf> vertices = new ArrayList<>();
        generateStandardPath(path, vertices);

        assert vertices.size() >= 2;
        assert vertices.getFirst() == src();
        assert vertices.getLast() == dst();
        assert m_reroute_flag_ptr != null;

        for (int i = 1; i < vertices.size(); ++i) {
        if (m_router.InvisibilityGrph && (m_type == ConnType.Polyline)) {
                // TODO: Again, we could know this edge without searching.
                EdgeInf edge = EdgeInf.existingEdge(vertices.get(i - 1), vertices.get(i));
                if (edge != null) {
                    edge.addConn(m_reroute_flag_ptr);
                }
            } else {
                m_false_path = true;
            }
        }

        for (int i = 1; i < vertices.size(); ++i) {
            VertInf vertex = vertices.get(i);
            // vertex may be null for synthetic fallback bend points.
            assert vertex == null || vertex.pathNext == null ||
                (!vertex.pathNext.point.equals(vertex.point)) || vertex.pathNext.id.isConnPt() ||
                vertex.id.isConnPt() ||
                Math.abs(vertex.pathNext.id.vn - vertex.id.vn) != 2;
        }

        // Get rid of dummy ShapeConnectionPin bridging points at beginning
        // and end of path.
        int pathBegin = 0;
        int pathEnd = path.size();
        if (path.size() > 2 && isDummyAtEnd.first) {
            pathBegin = 1;
            m_src_connend.usePinVertex(vertices.get(1));
        }
        if (path.size() > 2 && isDummyAtEnd.second) {
            pathEnd = path.size() - 1;
            m_dst_connend.usePinVertex(vertices.get(vertices.size() - 2));
        }
        List<Point> clippedPath = path.subList(pathBegin, pathEnd);

        // Clear visibility edges added for connection pins dummy vertices.
        assignConnectionPinVisibility(false);

        freeRoutes();
        m_route.ps = new ArrayList<>(clippedPath);

        return true;
    }

    /**
     * Generates a standard path (no checkpoints).
     * Corresponds to ConnRef::generateStandardPath(vector<Point>&, vector<VertInf*>&).
     */
    private void generateStandardPath(List<Point> path, List<VertInf> vertices) {
        VertInf tar = m_dst_vert;
        int existingPathStart = 0;
        Polygon currRoute = route();
        if (m_router.RubberBandRouting) {
            if (currRoute.size() > 2) {
                if (m_src_vert.point.equals(currRoute.ps.getFirst())) {
                    existingPathStart = currRoute.size() - 2;
                    assert existingPathStart != 0;
                    Point pnt = currRoute.at(existingPathStart);
                    VertID vID = new VertID(pnt.id, pnt.vn);

                    m_start_vert = m_router.vertices.getVertexByID(vID);
                    assert m_start_vert != null;
                }
            }
        }

        int pathlen = 0;
        while (pathlen == 0) {
            MakePath.search(this, src(), dst(), start());
            pathlen = dst().pathLeadsBackTo(src());
            if (pathlen < 2) {
                if (existingPathStart == 0) {
                    break;
                }
                existingPathStart--;
                Point pnt = currRoute.at(existingPathStart);
                int props = (existingPathStart > 0) ? 0 : VertID.PROP_ConnPoint;
                VertID vID = new VertID(pnt.id, pnt.vn, (short)props);

                m_start_vert = m_router.vertices.getVertexByID(vID);
                assert m_start_vert != null;
            } else if (m_router.RubberBandRouting) {
                // found.
                boolean unwind = false;

                VertInf prior = null;
                for (VertInf curr = tar; curr != m_start_vert.pathNext;
                        curr = curr.pathNext) {
                    if (!validateBendPoint(curr.pathNext, curr, prior)) {
                        unwind = true;
                        break;
                    }
                    prior = curr;
                }
                if (unwind) {
                    if (existingPathStart == 0) {
                        break;
                    }
                    existingPathStart--;
                    Point pnt = currRoute.at(existingPathStart);
                    int props = (existingPathStart > 0) ? 0 : VertID.PROP_ConnPoint;
                    VertID vID = new VertID(pnt.id, pnt.vn, (short)props);

                    m_start_vert = m_router.vertices.getVertexByID(vID);
                    assert m_start_vert != null;

                    pathlen = 0;
                }
            }
        }

        if (pathlen < 2) {
            // There is no valid path.
            // C++ uses db_printf here, which is suppressed in normal builds.
            m_needs_reroute_flag = true;

            if (m_type == ConnType.Orthogonal) {
                // For orthogonal routing, build an L-shaped fallback path so the
                // connector at least looks reasonable instead of a diagonal line.
                Point srcPoint = new Point(m_src_vert.point);
                srcPoint.id = m_src_vert.id.objID;
                srcPoint.vn = m_src_vert.id.vn;
                Point dstPoint = new Point(tar.point);
                dstPoint.id = tar.id.objID;
                dstPoint.vn = tar.id.vn;
                Point midPoint = new Point(srcPoint.x, dstPoint.y);
                midPoint.id = 0;
                midPoint.vn = Point.kUnassignedVertexNumber;

                path.add(srcPoint);
                path.add(midPoint);
                path.add(dstPoint);
                vertices.add(m_src_vert);
                vertices.add(null);
                vertices.add(tar);
                return;
            }

            // Polyline fallback: direct line (existing C++ behaviour).
            pathlen = 2;
            tar.pathNext = m_src_vert;
        }

        // Resize path and vertices
        for (int k = 0; k < pathlen; k++) {
            path.add(null);
            vertices.add(null);
        }

        int j = pathlen - 1;
        for (VertInf i = tar; i != m_src_vert; i = i.pathNext) {
            Point pt = new Point(i.point);
            pt.id = i.id.objID;
            pt.vn = i.id.vn;
            path.set(j, pt);
            vertices.set(j, i);
            j--;
        }
        vertices.set(0, m_src_vert);
        Point srcPt = new Point(m_src_vert.point);
        srcPt.id = m_src_vert.id.objID;
        srcPt.vn = m_src_vert.id.vn;
        path.set(0, srcPt);
    }

    // -----------------------------------------------------------------------
    // Static utility methods
    // -----------------------------------------------------------------------

    /**
     * Computes the midpoint between two points.
     * Corresponds to midpoint() free function in connector.cpp.
     */
    static Point midpoint(Point a, Point b) {
        Point mid = new Point();
        mid.x = (a.x + b.x) / 2.0;
        mid.y = (a.y + b.y) / 2.0;
        return mid;
    }

    /**
     * Validates a bend point on a path to check it does not form a zigzag corner.
     * a, b, c are consecutive points on the path. d and e are b's neighbours,
     * forming the shape corner d-b-e.
     * Corresponds to validateBendPoint() free function in connector.cpp.
     */
    static boolean validateBendPoint(VertInf aInf, VertInf bInf, VertInf cInf) {
        if (bInf.id.isConnectionPin() || bInf.id.isConnCheckpoint()) {
            // We shouldn't check connection pins or checkpoints.
            return true;
        }
        boolean bendOkay = true;

        if ((aInf == null) || (cInf == null)) {
            // Not a bendpoint, i.e., the end of the connector, so don't test.
            return bendOkay;
        }

        VertInf dInf = bInf.shPrev;
        VertInf eInf = bInf.shNext;
        assert dInf != null;
        assert eInf != null;

        Point a = aInf.point;
        Point b = bInf.point;
        Point c = cInf.point;
        Point d = dInf.point;
        Point e = eInf.point;

        if (a.equals(b) || b.equals(c)) {
            return bendOkay;
        }

        // Check angle:
        int abc = Geometry.vecDir(a, b, c);

        if (abc != 0) {
            assert Geometry.vecDir(d, b, e) > 0;
            int abe = Geometry.vecDir(a, b, e);
            int abd = Geometry.vecDir(a, b, d);
            int bce = Geometry.vecDir(b, c, e);
            int bcd = Geometry.vecDir(b, c, d);

            bendOkay = false;
            if (abe > 0) {
                if ((abc > 0) && (abd >= 0) && (bce >= 0)) {
                    bendOkay = true;
                }
            } else if (abd < 0) {
                if (abc < 0 && bcd <= 0) {
                    bendOkay = true;
                }
            }
        }
        return bendOkay;
    }

    // -----------------------------------------------------------------------
    // Compatibility methods (used by Router.java and other classes)
    // -----------------------------------------------------------------------

    /**
     * Returns the source ConnEnd (for compatibility with existing code).
     */
    public ConnEnd sourceEndpoint() {
        ConnEnd result = new ConnEnd();
        ConnEnd[] arr = new ConnEnd[]{result};
        getConnEndForEndpointVertex(m_src_vert, arr, 0);
        return arr[0];
    }

    /**
     * Returns the destination ConnEnd (for compatibility with existing code).
     */
    public ConnEnd destEndpoint() {
        ConnEnd result = new ConnEnd();
        ConnEnd[] arr = new ConnEnd[]{result};
        getConnEndForEndpointVertex(m_dst_vert, arr, 0);
        return arr[0];
    }

    // ensureEndpointVertices() removed — no-op invented method.
    // Vertices are created in common_updateEndPoint() during processTransaction().

    /**
     * Disconnect from anchors and clean up (compatibility method).
     * TODO why not used
     */
    void disconnect() {
        if (m_src_connend != null) {
            m_src_connend.disconnect();
        }
        if (m_dst_connend != null) {
            m_dst_connend.disconnect();
        }
    }
}
