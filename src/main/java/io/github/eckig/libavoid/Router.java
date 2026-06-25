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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The Router class represents a libavoid router instance.
 *
 * Usually you would keep a separate Router instance for each diagram
 * or layout you have open in your application.
 *
 * Corresponds to router.h/router.cpp in C++.
 */
public class Router {

    // -----------------------------------------------------------------------
    // Inner enums (translated from C++ enums in router.h)
    // -----------------------------------------------------------------------

    /**
     * Flags that can be passed to the router during initialisation
     * to specify options.
     * Corresponds to C++ enum RouterFlag.
     */
    public enum RouterFlag {
        /** This option specifies that the router should maintain the
         *  structures necessary to allow poly-line connector routing. */
        PolyLineRouting(1),
        /** This option specifies that the router should maintain the
         *  structures necessary to allow orthogonal connector routing. */
        OrthogonalRouting(2);

        public final int flag;

        RouterFlag(int flag) {
            this.flag = flag;
        }
    }

    /**
     * Types of routing parameters and penalties that can be used to
     * tailor the style and improve the quality of the connector
     * routes produced.
     * Corresponds to C++ enum RoutingParameter.
     */
    public enum RoutingParameter {
        /**
         * This penalty is applied for each segment in the connector path beyond the first.
         * This should always normally be set when doing orthogonal routing to prevent step-like connector paths.
         * <p>
         * <b>Note</b>
         * This penalty must be set (i.e., be greater than zero) in order for orthogonal connector nudging to be
         * performed, since this requires reasonable initial routes.
         */
        segmentPenalty,
        /**
         * This penalty is applied in its full amount to tight acute bends in the connector path.
         * A smaller portion of the penalty is applied for slight bends, i.e., where the bend is close to 180 degrees.
         * This is useful for polyline routing where there is some evidence that tighter corners are worse for
         * readability, but that slight bends might not be so bad, especially when smoothed by curves.
         */
        anglePenalty,
        /**
         * This penalty is applied whenever a connector path crosses another connector path.
         * It takes shared paths into consideration and the penalty is only applied if there is an actual crossing.
         * <p>
         * <b>Note</b>
         * This penalty is still experimental! It is not recommended for normal use.
         */
        crossingPenalty,
        /**
         * This penalty is applied whenever a connector path crosses a cluster boundary.
         * <p>
         * <b>Note</b>
         * This penalty is still experimental! It is not recommended for normal use.
         * This penalty is very slow.
         */
        clusterCrossingPenalty,
        /**
         * This penalty is applied whenever a connector path shares some segments with an immovable portion of an
         * existing connector route (such as the first or last segment of a connector).
         * <p>
         * <b>Note</b>
         * This penalty is still experimental! It is not recommended for normal use.
         */
        fixedSharedPathPenalty,
        /**
         * This penalty is applied to port selection choice when the other end of the connector being routed does not
         * appear in any of the 90 degree visibility cones centered on the visibility directions for the port.
         * <p>
         * <b>Note</b>
         * This penalty is still experimental! It is not recommended for normal use.
         * This penalty is very slow.
         */
        portDirectionPenalty,
        /**
         * This parameter defines the spacing distance that will be added to the sides of each shape when determining
         * obstacle sizes for routing. This controls how closely connectors pass shapes, and can be used to prevent
         * connectors overlapping with shape boundaries. By default, this distance is set to a value of 0.
         */
        shapeBufferDistance,
        /**
         * This parameter defines the spacing distance that will be used for nudging apart overlapping corners and line
         * segments of connectors. By default, this distance is set to a value of 4.
         */
        idealNudgingDistance,
        /**
         * This penalty is applied whenever a connector path travels in the direction opposite of the destination from
         * the source endpoint. By default this penalty is set to zero. This shouldn't be needed in most cases but can
         * be useful if you use penalties such as crossingPenalty which cause connectors to loop around obstacles.
         */
        reverseDirectionPenalty
    }

    /**
     * Types of routing options that can be enabled.
     * Corresponds to C++ enum RoutingOption.
     */
    public enum RoutingOption {
        /**
         * This option causes the final segments of connectors, which are attached to shapes, to be nudged apart.
         * Usually these segments are fixed, since they are considered to be attached to ports.
         *
         * <p>Defaults to false.
         *
         * <p>This option also causes routes running through the same checkpoint to be nudged apart.
         *
         * <p>This option has no effect if nudgeSharedPathsWithCommonEndPoint is set to false,
         *
         * <p><b>Note</b>
         * This will allow routes to be nudged up to the bounds of shapes.
         */
        nudgeOrthogonalSegmentsConnectedToShapes,
        /**
         * This option penalises and attempts to reroute orthogonal shared connector paths terminating at a common
         * junction or shape connection pin. When multiple connector paths enter or leave the same side of a junction
         * (or shape pin), the router will attempt to reroute these to different sides of the junction or different
         * shape pins.
         *
         * <p>Defaults to false.
         *
         * <p>This option depends on the fixedSharedPathPenalty penalty having been set.
         *
         * See also {@link RoutingParameter#fixedSharedPathPenalty}
         * <p><b>Note</b>
         * This option is still experimental! It is not recommended for normal use.
         */
        penaliseOrthogonalSharedPathsAtConnEnds,
        /**
         * This option can be used to control whether collinear line segments that touch just at their ends will be
         * nudged apart. The overlap will usually be resolved in the other dimension, so this is not usually required.
         *
         * <p>Defaults to false.
         */
        nudgeOrthogonalTouchingColinearSegments,
        /**
         * This option can be used to control whether the router performs a preprocessing step before orthogonal
         * nudging where is tries to unify segments and centre them in free space. This generally results in better
         * quality ordering and nudging.
         *
         * <p>Defaults to true.
         *
         * <p>You may wish to turn this off for large examples where it can be very slow and will make little difference.
         */
        performUnifyingNudgingPreprocessingStep,
        /**
         * This option determines whether intermediate segments of connectors that are attached to common endpoints
         * will be nudged apart. Usually these segments get nudged apart, but you may want to turn this off if you
         * would prefer that entire shared paths terminating at a common end point should overlap.
         *
         * <p>Defaults to true.
         */
        nudgeSharedPathsWithCommonEndPoint,
        /**
         * This option determines whether the first/last (final) segments of connectors that share the exact same
         * start or end point will be nudged apart. When set to false, connectors leaving from the same point will
         * not be spread apart by nudging at their common endpoint, but may still diverge once they take different
         * paths.
         *
         * <p>Defaults to true.
         */
        nudgeFinalSegmentsFromSamePoint
    }

    // -----------------------------------------------------------------------
    // Internal ActionType enum (translated from C++ enum ActionType in actioninfo.h)
    // -----------------------------------------------------------------------

    enum ActionType {
        ShapeMove,
        ShapeAdd,
        ShapeRemove,
        JunctionMove,
        JunctionAdd,
        JunctionRemove,
        ConnChange,
        ConnectionPinChange
    }

    // -----------------------------------------------------------------------
    // Internal ActionInfo class (translated from C++ ActionInfo in actioninfo.h/cpp)
    // -----------------------------------------------------------------------

    static class ActionInfo implements Comparable<ActionInfo> {
        ActionType type;
        Object objPtr;
        Polygon newPoly;
        Point newPosition;
        boolean firstMove;
        List<Pair<Integer, ConnEnd>> conns;

        ActionInfo(ActionType t, ShapeRef s, Polygon p, boolean firstMove) {
            this.type = t;
            this.objPtr = s;
            this.newPoly = (p != null) ? new Polygon(p) : new Polygon();
            this.newPosition = new Point();
            this.firstMove = firstMove;
            this.conns = new ArrayList<>();
            assert type == ActionType.ShapeMove;
        }

        ActionInfo(ActionType t, ShapeRef s) {
            this.type = t;
            this.objPtr = s;
            this.newPoly = new Polygon();
            this.newPosition = new Point();
            this.firstMove = false;
            this.conns = new ArrayList<>();
            assert type == ActionType.ShapeAdd || type == ActionType.ShapeRemove ||
                    type == ActionType.ShapeMove;
        }

        ActionInfo(ActionType t, JunctionRef j, Point p) {
            this.type = t;
            this.objPtr = j;
            this.newPoly = new Polygon();
            this.newPosition = (p != null) ? new Point(p) : new Point();
            this.firstMove = false;
            this.conns = new ArrayList<>();
            assert type == ActionType.JunctionMove;
        }

        ActionInfo(ActionType t, JunctionRef j) {
            this.type = t;
            this.objPtr = j;
            this.newPoly = new Polygon();
            this.newPosition = new Point();
            this.firstMove = false;
            this.conns = new ArrayList<>();
            assert type == ActionType.JunctionAdd || type == ActionType.JunctionRemove ||
                    type == ActionType.JunctionMove;
        }

        ActionInfo(ActionType t, ConnRef c) {
            this.type = t;
            this.objPtr = c;
            this.newPoly = new Polygon();
            this.newPosition = new Point();
            this.firstMove = false;
            this.conns = new ArrayList<>();
            assert type == ActionType.ConnChange;
        }

        ActionInfo(ActionType t, ShapeConnectionPin pin) {
            this.type = t;
            this.objPtr = pin;
            this.newPoly = new Polygon();
            this.newPosition = new Point();
            this.firstMove = false;
            this.conns = new ArrayList<>();
            assert type == ActionType.ConnectionPinChange;
        }

        Obstacle obstacle() {
            assert type == ActionType.ShapeMove || type == ActionType.ShapeAdd ||
                    type == ActionType.ShapeRemove || type == ActionType.JunctionMove ||
                    type == ActionType.JunctionAdd || type == ActionType.JunctionRemove;
            return (Obstacle) objPtr;
        }

        ShapeRef shape() {
            if (objPtr instanceof ShapeRef s) {
                return s;
            }
            return null;
        }

        JunctionRef junction() {
            if (objPtr instanceof JunctionRef j) {
                return j;
            }
            return null;
        }

        ConnRef conn() {
            assert type == ActionType.ConnChange;
            if (objPtr instanceof ConnRef c) {
                return c;
            }
            return null;
        }

        void addConnEndUpdate(int type, ConnEnd connEnd, boolean connPinMoveUpdate) {
            // If the same type is already queued, update it.
            for (int i = 0; i < conns.size(); i++) {
                var entry = conns.get(i);
                if (entry.first == type) {
                    if (!connPinMoveUpdate) {
                        // Defensive copy: C++ stores ConnEnd by value; Java must
                        // copy to avoid mutation by later disconnect() calls.
                        conns.set(i, new Pair<>(type, new ConnEnd(connEnd)));
                    }
                    return;
                }
            }
            // Defensive copy: C++ stores ConnEnd by value in the pair.
            conns.add(new Pair<>(type, new ConnEnd(connEnd)));
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof ActionInfo o)) return false;
            return (type == o.type) && (objPtr == o.objPtr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, System.identityHashCode(objPtr));
        }

        @Override
        public int compareTo(ActionInfo other) {
            // C++ actioninfo.cpp:161-183
            if (type != other.type) {
                return Integer.compare(type.ordinal(), other.type.ordinal());
            }
            if (type == ActionType.ConnChange) {
                return Integer.compare(conn().id(), other.conn().id());
            } else if (type == ActionType.ConnectionPinChange) {
                return Integer.compare(
                        System.identityHashCode(objPtr),
                        System.identityHashCode(other.objPtr));
            } else {
                return Integer.compare(obstacle().id(), other.obstacle().id());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Internal ConnRerouteFlagDelegate class
    // (translated from C++ ConnRerouteFlagDelegate in router.h/router.cpp)
    // -----------------------------------------------------------------------

    static class ConnRerouteFlagDelegate {
        private final List<Pair<ConnRef, boolean[]>> m_mapping;

        ConnRerouteFlagDelegate() {
            m_mapping = new ArrayList<>();
        }

        boolean[] addConn(ConnRef conn) {
            boolean[] flag = new boolean[]{false};
            m_mapping.add(new Pair<>(conn, flag));
            return flag;
        }

        void removeConn(ConnRef conn) {
            for (var entry : m_mapping) {
                if (entry.first == conn) {
                    entry.second = null;
                }
            }
        }

        void alertConns() {
            for (var entry : m_mapping) {
                if (entry.first != null && entry.second != null && entry.second[0]) {
                    entry.second[0] = false;
                    entry.first.makePathInvalid();
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    public static final double chooseSensibleParamValue = -1;

    // -----------------------------------------------------------------------
    // Public fields (matching C++ header: these are public in C++)
    // -----------------------------------------------------------------------

    /** List of obstacles (shapes and junctions) in the router. */
    public List<Obstacle> m_obstacles;

    // Visibility graph vertex list (translated from C++ Router::vertices)
    public VertInfList vertices = new VertInfList();

    // Visibility graph edge lists (translated from C++ Router)
    public EdgeList visGraph = new EdgeList();
    public EdgeList visOrthogGraph = new EdgeList();
    public EdgeList invisGraph = new EdgeList();

    /** List of connectors in the router. Also exposed as m_connectors for existing code. */
    public List<ConnRef> m_connectors;

    // ContainsMap: maps VertID -> set of obstacle IDs that contain that vertex.
    // Corresponds to C++ ContainsMap contains and ContainsMap enclosingClusters.
    public Map<VertID, Set<Integer>> contains = new HashMap<>();
    public Map<VertID, Set<Integer>> enclosingClusters = new HashMap<>();

    // Routing options flags:
    public boolean PartialTime;
    public boolean SimpleRouting;

    // Poly-line routing options:
    public boolean IgnoreRegions;
    public boolean UseLeesAlgorithm;
    public boolean InvisibilityGrph;

    // General routing options:
    public boolean SelectiveReroute;

    public boolean PartialFeedback;
    public boolean RubberBandRouting;

    // Instrumentation:
    public int st_checked_edges;

    // -----------------------------------------------------------------------
    // Private fields (matching C++ header)
    // -----------------------------------------------------------------------

    private final List<ActionInfo> actionList;
    private int m_largest_assigned_id;
    private boolean m_consolidate_actions;
    private final double[] m_routing_parameters;
    private final boolean[] m_routing_options;

    ConnRerouteFlagDelegate m_conn_reroute_flags;

    // Progress tracking and transaction cancelling.
    private boolean m_abort_transaction;

    // Overall modes:
    boolean m_allows_polyline_routing;
    boolean m_allows_orthogonal_routing;

    private boolean m_static_orthogonal_graph_invalidated;
    private boolean m_in_crossing_rerouting_stage;

    private boolean m_settings_changes;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Constructor for router instance.
     *
     * @param flags One or more RouterFlag options to control the behaviour
     *              of the router. Can be a single RouterFlag enum value or
     *              a bitwise OR of RouterFlag.flag values.
     */
    public Router(int flags) {
        m_obstacles = new ArrayList<>();
        m_connectors = new ArrayList<>();

        PartialTime = false;
        SimpleRouting = false;

        // Poly-line algorithm options:
        IgnoreRegions = true;
        UseLeesAlgorithm = true;
        InvisibilityGrph = true;

        // General algorithm options:
        SelectiveReroute = true;
        PartialFeedback = false;
        RubberBandRouting = false;

        // Instrumentation:
        st_checked_edges = 0;

        m_largest_assigned_id = 0;
        m_consolidate_actions = true;

        // Mode options:
        m_allows_polyline_routing = false;
        m_allows_orthogonal_routing = false;
        m_static_orthogonal_graph_invalidated = true;
        m_in_crossing_rerouting_stage = false;
        m_settings_changes = false;

        actionList = new ArrayList<>();

        // At least one of the Routing modes must be set.
        assert (flags & (RouterFlag.PolyLineRouting.flag | RouterFlag.OrthogonalRouting.flag)) != 0;

        if ((flags & RouterFlag.PolyLineRouting.flag) != 0) {
            m_allows_polyline_routing = true;
        }
        if ((flags & RouterFlag.OrthogonalRouting.flag) != 0) {
            m_allows_orthogonal_routing = true;
        }

        m_routing_parameters = new double[RoutingParameter.values().length];
        Arrays.fill(m_routing_parameters, 0.0);
        m_routing_parameters[RoutingParameter.segmentPenalty.ordinal()] = 10;
        m_routing_parameters[RoutingParameter.clusterCrossingPenalty.ordinal()] = 4000;
        m_routing_parameters[RoutingParameter.idealNudgingDistance.ordinal()] = 4.0;

        m_routing_options = new boolean[RoutingOption.values().length];
        m_routing_options[RoutingOption.nudgeOrthogonalSegmentsConnectedToShapes.ordinal()] = false;
        m_routing_options[RoutingOption.penaliseOrthogonalSharedPathsAtConnEnds.ordinal()] = false;
        m_routing_options[RoutingOption.nudgeOrthogonalTouchingColinearSegments.ordinal()] = false;
        m_routing_options[RoutingOption.performUnifyingNudgingPreprocessingStep.ordinal()] = true;
        m_routing_options[RoutingOption.nudgeSharedPathsWithCommonEndPoint.ordinal()] = true;
        m_routing_options[RoutingOption.nudgeFinalSegmentsFromSamePoint.ordinal()] = true;

        m_conn_reroute_flags = new ConnRerouteFlagDelegate();

        m_abort_transaction = false;
    }

    /**
     * Convenience constructor accepting a single RouterFlag enum value.
     */
    public Router(RouterFlag flag) {
        this(flag.flag);
    }

    // -----------------------------------------------------------------------
    // Transaction use
    // -----------------------------------------------------------------------

    /**
     * Allows setting of the behaviour of the router in regard
     * to transactions.
     */
    public void setTransactionUse(boolean transactions) {
        m_consolidate_actions = transactions;
    }

    /**
     * Reports whether the router groups actions into transactions.
     */
    public boolean transactionUse() {
        return m_consolidate_actions;
    }

    // -----------------------------------------------------------------------
    // processTransaction / processActions
    // -----------------------------------------------------------------------

    /**
     * Finishes the current transaction and processes all the
     * queued object changes efficiently.
     *
     * @return A boolean value describing whether there were any actions
     *         to process.
     */
    public boolean processTransaction() {
        // If SimpleRouting, then don't update here.
        if ((actionList.isEmpty() && !m_settings_changes) || SimpleRouting) {
            return false;
        }
        m_settings_changes = false;

        processActions();

        m_static_orthogonal_graph_invalidated = true;
        rerouteAndCallbackConnectors();

        return true;
    }

    /**
     * Processes the actions list for the transaction.
     * You shouldn't need to call this. Instead use processTransaction().
     * Translated from Router::processActions() in router.cpp line 465.
     */
    public void processActions() {
        boolean notPartialTime = !(PartialFeedback && PartialTime);
        boolean seenShapeMovesOrDeletes = false;

        m_abort_transaction = false;

        List<Integer> deletedObstacles = new ArrayList<>();
        Collections.sort(actionList);

        // First pass: process remove and move actions (remove old positions).
        for (ActionInfo actInf : new ArrayList<>(actionList)) {
            if (!((actInf.type == ActionType.ShapeRemove) || (actInf.type == ActionType.ShapeMove) ||
                    (actInf.type == ActionType.JunctionRemove) || (actInf.type == ActionType.JunctionMove))) {
                // Not a move or remove action, so don't do anything.
                continue;
            }
            seenShapeMovesOrDeletes = true;

            Obstacle obstacle = actInf.obstacle();
            ShapeRef shape = actInf.shape();
            JunctionRef junction = actInf.junction();
            boolean isMove = (actInf.type == ActionType.ShapeMove) ||
                    (actInf.type == ActionType.JunctionMove);
            boolean first_move = actInf.firstMove;

            int pid = obstacle.id();

            // o  Remove entries related to this shape's vertices
            obstacle.removeFromGraph();

            if (SelectiveReroute && (!isMove || notPartialTime || first_move)) {
                markPolylineConnectorsNeedingReroutingForDeletedObstacle(obstacle);
            }

            adjustContainsWithDel(pid);

            if (isMove) {
                if (shape != null) {
                    shape.moveAttachedConns(actInf.newPoly);
                } else if (junction != null) {
                    junction.moveAttachedConns(actInf.newPosition);
                }
            }

            // Ignore this shape for visibility.
            obstacle.makeInactive();

            if (!isMove) {
                // Free deleted obstacle.
                deletedObstacles.add(obstacle.id());
                m_obstacles.remove(obstacle);
            }
        }

        // After first pass: update polyline visibility graph for moves/deletes.
        if (seenShapeMovesOrDeletes && m_allows_polyline_routing) {
            if (InvisibilityGrph) {
                // Check edges for obstacles that were moved or removed.
                for (ActionInfo actInf : actionList) {
                    if ((actInf.type == ActionType.ShapeMove) || (actInf.type == ActionType.JunctionMove)) {
                        // o  Check all edges that were blocked by moved obstacle.
                        checkAllBlockedEdges(actInf.obstacle().id());
                    }
                }
                for (int deletedId : deletedObstacles) {
                    // o  Check all edges that were blocked by deleted obstacle.
                    checkAllBlockedEdges(deletedId);
                }
            } else {
                // check all edges not in graph
                checkAllMissingEdges();
            }
        }

        // Second pass: process add and move actions (add new positions).
        for (ActionInfo actInf : new ArrayList<>(actionList)) {
            if (!((actInf.type == ActionType.ShapeAdd) || (actInf.type == ActionType.ShapeMove) ||
                    (actInf.type == ActionType.JunctionAdd) || (actInf.type == ActionType.JunctionMove))) {
                // Not a move or add action, so don't do anything.
                continue;
            }

            Obstacle obstacle = actInf.obstacle();
            ShapeRef shape = actInf.shape();
            JunctionRef junction = actInf.junction();
            Polygon newPoly = actInf.newPoly;
            boolean isMove = (actInf.type == ActionType.ShapeMove) ||
                    (actInf.type == ActionType.JunctionMove);

            int pid = obstacle.id();

            // Restore this shape for visibility.
            obstacle.makeActive();

            if (isMove) {
                if (shape != null) {
                    shape.setNewPoly(newPoly);
                } else {
                    junction.setPosition(actInf.newPosition);
                }
            }
            Polygon shapePoly = obstacle.routingPolygon();

            adjustContainsWithAdd(shapePoly, pid);

            if (m_allows_polyline_routing) {
                // o  Check all visibility edges to see if this one shape blocks them.
                if (!isMove || notPartialTime) {
                    newBlockingShape(shapePoly, pid);
                }

                // o  Calculate visibility for the new vertices.
                if (UseLeesAlgorithm) {
                    obstacle.computeVisibilitySweep();
                } else {
                    obstacle.computeVisibilityNaive();
                }
                obstacle.updatePinPolyLineVisibility();
            }
        }

        // Update connector endpoints.
        for (ActionInfo actInf : new ArrayList<>(actionList)) {
            if (actInf.type != ActionType.ConnChange) {
                continue;
            }
            for (var conn : actInf.conns) {
                actInf.conn().updateEndPoint(conn.first, conn.second);
            }
        }

        // Clear the actionList.
        actionList.clear();
    }

    // -----------------------------------------------------------------------
    // Shape operations
    // -----------------------------------------------------------------------

    /**
     * Add a shape to the router scene. Called by ShapeRef constructor.
     */
    void addShape(ShapeRef shape) {
        // There shouldn't be remove events or move events for the same shape
        // already in the action list.
        ActionInfo addInfo = new ActionInfo(ActionType.ShapeAdd, shape);

        if (!actionList.contains(addInfo)) {
            actionList.add(addInfo);
        }

        if (!m_consolidate_actions) {
            processTransaction();
        }
    }

    /**
     * Delete a shape from the router scene.
     */
    public void deleteShape(ShapeRef shape) {
        // Delete any ShapeMove entries for this shape in the action list.
        ActionInfo moveInfo = new ActionInfo(ActionType.ShapeMove, shape);
        actionList.remove(moveInfo);

        // Add the ShapeRemove entry.
        ActionInfo remInfo = new ActionInfo(ActionType.ShapeRemove, shape);
        if (!actionList.contains(remInfo)) {
            actionList.add(remInfo);
        }

        if (!m_consolidate_actions) {
            processTransaction();
        }
    }

    /**
     * Move or resize an existing shape within the router scene.
     */
    public void moveShape(ShapeRef shape, Polygon newPoly) {
        moveShape(shape, newPoly, false);
    }

    /**
     * Move or resize an existing shape within the router scene.
     */
    public void moveShape(ShapeRef shape, Polygon newPoly, boolean first_move) {
        // Check if there's already an Add for this shape
        ActionInfo addInfo = new ActionInfo(ActionType.ShapeAdd, shape);
        int addIdx = actionList.indexOf(addInfo);
        if (addIdx >= 0) {
            // The Add is enough, no need for the Move action too.
            actionList.get(addIdx).shape().setNewPoly(newPoly);
            return;
        }

        ActionInfo moveInfo = new ActionInfo(ActionType.ShapeMove, shape, newPoly, first_move);
        int moveIdx = actionList.indexOf(moveInfo);

        if (moveIdx >= 0) {
            // Just update the ActionInfo with the second polygon, but
            // leave the firstMove setting alone.
            actionList.get(moveIdx).newPoly = newPoly;
        } else {
            actionList.add(moveInfo);
        }

        if (!m_consolidate_actions) {
            processTransaction();
        }
    }

    /**
     * Move an existing shape within the router scene by a relative distance.
     */
    public void moveShape(ShapeRef shape, double xDiff, double yDiff) {
        ActionInfo moveInfo = new ActionInfo(ActionType.ShapeMove, shape, new Polygon(), false);
        int foundIdx = actionList.indexOf(moveInfo);

        Polygon newPoly;
        if (foundIdx >= 0) {
            // The shape already has a queued move, so use that shape position.
            newPoly = actionList.get(foundIdx).newPoly;
        } else {
            // Just use the existing position.
            newPoly = shape.polygon();
        }
        newPoly.translate(xDiff, yDiff);

        moveShape(shape, newPoly);
    }

    // -----------------------------------------------------------------------
    // Junction operations
    // -----------------------------------------------------------------------

    /**
     * Add a junction to the router scene. Called by JunctionRef constructor.
     */
    void addJunction(JunctionRef junction) {
        ActionInfo addInfo = new ActionInfo(ActionType.JunctionAdd, junction);

        if (!actionList.contains(addInfo)) {
            actionList.add(addInfo);
        }

        if (!m_consolidate_actions) {
            processTransaction();
        }
    }

    /**
     * Remove a junction from the router scene.
     */
    public void deleteJunction(JunctionRef junction) {
        // Delete any JunctionMove entries for this junction in the action list.
        ActionInfo moveInfo = new ActionInfo(ActionType.JunctionMove, junction);
        actionList.remove(moveInfo);

        // Add the JunctionRemove entry.
        ActionInfo remInfo = new ActionInfo(ActionType.JunctionRemove, junction);
        if (!actionList.contains(remInfo)) {
            actionList.add(remInfo);
        }

        if (!m_consolidate_actions) {
            processTransaction();
        }
    }

    /**
     * Move an existing junction within the router scene.
     */
    public void moveJunction(JunctionRef junction, Point newPosition) {
        // Check if there's already an Add for this junction
        ActionInfo addInfo = new ActionInfo(ActionType.JunctionAdd, junction);
        int addIdx = actionList.indexOf(addInfo);
        if (addIdx >= 0) {
            // The Add is enough, no need for the Move action too.
            actionList.get(addIdx).junction().setPosition(newPosition);
            return;
        }

        ActionInfo moveInfo = new ActionInfo(ActionType.JunctionMove, junction, newPosition);
        int moveIdx = actionList.indexOf(moveInfo);

        if (moveIdx >= 0) {
            // Just update the ActionInfo with the second position.
            actionList.get(moveIdx).newPosition = newPosition;
        } else {
            actionList.add(moveInfo);
        }

        if (!m_consolidate_actions) {
            processTransaction();
        }
    }

    /**
     * Move an existing junction within the router scene by a relative distance.
     */
    public void moveJunction(JunctionRef junction, double xDiff, double yDiff) {
        ActionInfo moveInfo = new ActionInfo(ActionType.JunctionMove, junction, new Point());
        int foundIdx = actionList.indexOf(moveInfo);

        Point newPosition;
        if (foundIdx >= 0) {
            // The junction already has a queued move, so use that position.
            newPosition = actionList.get(foundIdx).newPosition;
        } else {
            // Just use the existing position.
            newPosition = junction.position();
        }
        newPosition.x += xDiff;
        newPosition.y += yDiff;

        moveJunction(junction, newPosition);
    }

    // -----------------------------------------------------------------------
    // Connector operations
    // -----------------------------------------------------------------------

    // addConnector(ConnRef) removed — dead code with 0 callers.
    // C++ adds connectors via ConnRef::makeActive(), which is already
    // faithfully translated in ConnRef.java.

    /**
     * Remove a connector from the router scene.
     */
    public void deleteConnector(ConnRef connector) {
        connector.destroy();
    }

    void modifyConnector(ConnRef conn, int type, ConnEnd connEnd) {
        modifyConnector(conn, type, connEnd, false);
    }

    void modifyConnector(ConnRef conn, int type, ConnEnd connEnd, boolean connPinMoveUpdate) {
        ActionInfo modInfo = new ActionInfo(ActionType.ConnChange, conn);

        int foundIdx = actionList.indexOf(modInfo);
        if (foundIdx < 0) {
            // Matching action not found, so add.
            // Defensive copy: C++ stores ConnEnd by value; Java must
            // copy to avoid mutation by later disconnect() calls.
            modInfo.conns.add(new Pair<>(type, new ConnEnd(connEnd)));
            actionList.add(modInfo);
        } else {
            // Update the found action as necessary.
            actionList.get(foundIdx).addConnEndUpdate(type, connEnd, connPinMoveUpdate);
        }

        if (!m_consolidate_actions) {
            processTransaction();
        }
    }

    void modifyConnector(ConnRef conn) {
        ActionInfo modInfo = new ActionInfo(ActionType.ConnChange, conn);

        if (!actionList.contains(modInfo)) {
            actionList.add(modInfo);
        }

        if (!m_consolidate_actions) {
            processTransaction();
        }
    }

    void modifyConnectionPin(ShapeConnectionPin pin) {
        ActionInfo modInfo = new ActionInfo(ActionType.ConnectionPinChange, pin);

        if (!actionList.contains(modInfo)) {
            actionList.add(modInfo);
        }

        if (!m_consolidate_actions) {
            processTransaction();
        }
    }

    // -----------------------------------------------------------------------
    // ID management
    // -----------------------------------------------------------------------

    /**
     * Returns the object ID used for automatically generated objects.
     */
    public int newObjectId() {
        return m_largest_assigned_id + 1;
    }

    /**
     * Assigns an ID, using the suggested ID if non-zero, otherwise generating one.
     */
    int assignId(int suggestedId) {
        // If the suggestedId is zero, then we assign the object the next
        // smallest unassigned ID, otherwise we trust the ID given is unique.
        int assignedId = (suggestedId == 0) ? newObjectId() : suggestedId;

        // Have the router record if this ID is larger than the largest assigned ID.
        m_largest_assigned_id = Math.max(m_largest_assigned_id, assignedId);

        return assignedId;
    }

    /**
     * Assigns the next available ID.
     */
    int assignId() {
        return assignId(0);
    }

    // -----------------------------------------------------------------------
    // Routing parameters and options
    // -----------------------------------------------------------------------

    /**
     * Sets values for routing parameters using integer parameter ordinal.
     * This overload supports tests that pass C++ enum ordinals directly.
     */
    public void setRoutingParameter(int parameterOrdinal, double value) {
        RoutingParameter[] params = RoutingParameter.values();
        for (RoutingParameter p : params) {
            if (p.ordinal() == parameterOrdinal) {
                setRoutingParameter(p, value);
                return;
            }
        }
    }

    /**
     * Sets routing options using integer option ordinal.
     * This overload supports tests that pass C++ enum ordinals directly.
     */
    public void setRoutingOption(int optionOrdinal, boolean value) {
        RoutingOption[] options = RoutingOption.values();
        for (RoutingOption o : options) {
            if (o.ordinal() == optionOrdinal) {
                setRoutingOption(o, value);
                return;
            }
        }
    }

    /**
     * Convenience method to set the orthogonal nudging distance.
     * Corresponds to a common pattern used in test code.
     */
    public void setOrthogonalNudgeDistance(double distance) {
        setRoutingParameter(RoutingParameter.idealNudgingDistance, distance);
    }

    /**
     * Sets values for routing parameters, including routing penalties.
     */
    public void setRoutingParameter(RoutingParameter parameter, double value) {
        if (value < 0) {
            // Set some sensible parameter value for the parameter being 'active'.
            switch (parameter) {
                case segmentPenalty:
                    m_routing_parameters[parameter.ordinal()] = 50;
                    break;
                case fixedSharedPathPenalty:
                    m_routing_parameters[parameter.ordinal()] = 110;
                    break;
                case anglePenalty:
                    m_routing_parameters[parameter.ordinal()] = 50;
                    break;
                case crossingPenalty:
                    m_routing_parameters[parameter.ordinal()] = 200;
                    break;
                case clusterCrossingPenalty:
                    m_routing_parameters[parameter.ordinal()] = 4000;
                    break;
                case idealNudgingDistance:
                    m_routing_parameters[parameter.ordinal()] = 4.0;
                    break;
                case portDirectionPenalty:
                    m_routing_parameters[parameter.ordinal()] = 100;
                    break;
                default:
                    m_routing_parameters[parameter.ordinal()] = 50;
                    break;
            }
        } else {
            m_routing_parameters[parameter.ordinal()] = value;
        }
        m_settings_changes = true;
    }

    /**
     * Sets values for routing parameters with default sensible value.
     */
    public void setRoutingParameter(RoutingParameter parameter) {
        setRoutingParameter(parameter, chooseSensibleParamValue);
    }

    /**
     * Returns the current value for a particular routing parameter.
     */
    public double routingParameter(RoutingParameter parameter) {
        return m_routing_parameters[parameter.ordinal()];
    }

    /**
     * Turn specific routing options on or off.
     */
    public void setRoutingOption(RoutingOption option, boolean value) {
        m_routing_options[option.ordinal()] = value;
        m_settings_changes = true;
    }

    /**
     * Returns the current state for a specific routing option.
     */
    public boolean routingOption(RoutingOption option) {
        return m_routing_options[option.ordinal()];
    }

    /**
     * Sets or removes penalty values that are applied during connector routing.
     * This is a convenience wrapper for the setRoutingParameter() method.
     */
    public void setRoutingPenalty(RoutingParameter penType, double penVal) {
        setRoutingParameter(penType, penVal);
    }

    /**
     * Sets or removes penalty values using integer parameter ordinal.
     * This overload supports tests that pass C++ enum ordinals directly.
     */
    public void setRoutingPenalty(int penTypeOrdinal, double penVal) {
        setRoutingParameter(penTypeOrdinal, penVal);
    }

    /**
     * Sets penalty with sensible default value.
     */
    public void setRoutingPenalty(RoutingParameter penType) {
        setRoutingParameter(penType, chooseSensibleParamValue);
    }

    /**
     * Registers that settings have changed.
     */
    public void registerSettingsChange() {
        m_settings_changes = true;
    }

    // -----------------------------------------------------------------------
    // Crossing penalty rerouting stage
    // -----------------------------------------------------------------------

    public boolean isInCrossingPenaltyReroutingStage() {
        return m_in_crossing_rerouting_stage;
    }

    // -----------------------------------------------------------------------
    // Rerouting and callbacks
    // -----------------------------------------------------------------------

    /**
     * It's intended this function is called after visibility changes
     * resulting from shape movement have happened. It will alert
     * rerouted connectors (via a callback) that they need to be redrawn.
     */
    private void rerouteAndCallbackConnectors() {
        List<ConnRef> reroutedConns = new ArrayList<>();

        m_conn_reroute_flags.alertConns();

        // Updating the orthogonal visibility graph if necessary.
        regenerateStaticBuiltGraph();

        for (ConnRef conn : m_connectors) {
            conn.freeActivePins();
        }

        for (ConnRef connector : new ArrayList<>(m_connectors)) {

            if (connector.hasFixedRoute()) {
                // We don't reroute connectors with fixed routes.
                continue;
            }

            // C++ router.cpp:973-974
            connector.m_needs_repaint = false;
            boolean rerouted = connector.generatePath();
            if (rerouted) {
                reroutedConns.add(connector);
            }
        }

        // Retry failed connectors with shapeBufferDistance=0 so that routes
        // through tight gaps (smaller than the buffer) still get a valid
        // orthogonal path instead of a useless straight-line fallback.
        List<ConnRef> failedConns = new ArrayList<>();
        for (ConnRef conn : reroutedConns) {
            if (conn.m_needs_reroute_flag) {
                failedConns.add(conn);
            }
        }
        if (!failedConns.isEmpty()) {
            double originalBuffer = m_routing_parameters[RoutingParameter.shapeBufferDistance.ordinal()];
            m_routing_parameters[RoutingParameter.shapeBufferDistance.ordinal()] = 0;
            m_static_orthogonal_graph_invalidated = true;
            regenerateStaticBuiltGraph();

            for (ConnRef conn : failedConns) {
                conn.m_needs_reroute_flag = true;
                conn.generatePath();
            }

            // Restore original buffer and rebuild the graph so that all
            // subsequent steps (crossing detection, nudging) use the correct buffer.
            m_routing_parameters[RoutingParameter.shapeBufferDistance.ordinal()] = originalBuffer;
            m_static_orthogonal_graph_invalidated = true;
            regenerateStaticBuiltGraph();
        }

        // Find and reroute crossing connectors if crossing penalties are set.
        improveCrossings();

        // Perform centring and nudging for orthogonal routes.
        ImproveOrthogonalRoutes improver = new ImproveOrthogonalRoutes(this);
        improver.execute();

        // Post-nudging: enforce that connectors with a ConnDirLeft destination
        // arrive at the destination from the left (horizontal last segment).
        for (ConnRef conn : reroutedConns) {
            conn.enforceDestinationApproachDirection();
        }

        // Alert connectors that they need redrawing.
        for (ConnRef conn : reroutedConns) {
            conn.m_needs_repaint = true;
            conn.performCallback();
        }
    }

    // -----------------------------------------------------------------------
    // Endpoint resolution
    // -----------------------------------------------------------------------

    // resolveEndpoint(ConnEnd) removed — invented method with 0 callers.
    // Use ConnEnd.position() instead (faithful C++ translation).

    /**
     * Estimate of connector cost used for tie-breaking in crossing resolution.
     * Translated from cheapEstimatedCost() in router.cpp.
     */
    private static double cheapEstimatedCost(ConnRef lineRef) {
        boolean isPolyLine = (lineRef.routingType() == ConnType.Polyline);
        Polygon route = lineRef.displayRoute();
        double length = 0;
        for (int i = 1; i < route.size(); ++i) {
            Point a = route.ps.get(i - 1);
            Point b = route.ps.get(i);
            double segmentLength = isPolyLine ?
                    Geometry.euclideanDist(a, b) : Geometry.manhattanDist(a, b);
            length += segmentLength;
        }
        return length - (route.size() + 1);
    }

    /**
     * Maintains groups of crossing connectors and provides methods for
     * finding minimal sets to reroute.
     * Translated from CrossingConnectorsInfo in router.cpp lines 1087-1354.
     */
    private static class CrossingConnectorsInfo {
        // Each element is a group of connectors that cross each other.
        // Within each group, a map from connector -> set of connectors it crosses.
        private final List<Map<ConnRef, Set<ConnRef>>> pairsSetList = new ArrayList<>();

        void addCrossing(ConnRef conn1, ConnRef conn2) {
            Map<ConnRef, Set<ConnRef>> group = groupForCrossingConns(conn1, conn2);
            group.computeIfAbsent(conn1, _ -> new HashSet<>()).add(conn2);
            group.computeIfAbsent(conn2, _ -> new HashSet<>()).add(conn1);
        }

        /**
         * Returns lists of ConnCostRef groups for rerouting.
         * Translated from crossingSetsListToRemoveCrossingsFromGroups() in router.cpp.
         */
        List<List<ConnCostRef>> getCrossingGroups() {
            List<List<ConnCostRef>> crossingSetsList = new ArrayList<>();

            for (Map<ConnRef, Set<ConnRef>> pairsSet : pairsSetList) {
                Map<ConnRef, ConnCostRef> crossingSet = new HashMap<>();

                // C++ router.cpp:1120 — Set of exclusive pins that crossing-causing connectors attach to.
                Set<Long> exclusivePins = new HashSet<>();

                ConnCostRef candidate;
                while ((candidate = removeConnectorWithMostCrossings(pairsSet)) != null) {
                    crossingSet.putIfAbsent(candidate.conn(), candidate);

                    // C++ router.cpp:1131-1147 — Track exclusive pins for crossing-causing connectors.
                    Pair<ConnEnd, ConnEnd> ends = candidate.conn.endpointConnEnds();
                    ShapeConnectionPin pin = ends.first.m_active_pin;
                    if (pin != null && pin.isExclusive()) {
                        int[] pinIds = pin.ids();
                        exclusivePins.add(((long) pinIds[0] << 32) | (pinIds[1] & 0xFFFFFFFFL));
                    }
                    pin = ends.second.m_active_pin;
                    if (pin != null && pin.isExclusive()) {
                        int[] pinIds = pin.ids();
                        exclusivePins.add(((long) pinIds[0] << 32) | (pinIds[1] & 0xFFFFFFFFL));
                    }
                }

                // C++ router.cpp:1152-1191 — Add non-crossing connectors sharing exclusive pins.
                for (Map.Entry<ConnRef, Set<ConnRef>> entry : pairsSet.entrySet()) {
                    ConnRef conn = entry.getKey();
                    Pair<ConnEnd, ConnEnd> ends = conn.endpointConnEnds();
                    ShapeConnectionPin pin = ends.first.m_active_pin;
                    if (pin != null && pin.isExclusive()) {
                        int[] pinIds = pin.ids();
                        long key = ((long) pinIds[0] << 32) | (pinIds[1] & 0xFFFFFFFFL);
                        if (exclusivePins.contains(key)) {
                            crossingSet.putIfAbsent(conn, new ConnCostRef(0, conn));
                            continue;
                        }
                    }
                    pin = ends.second.m_active_pin;
                    if (pin != null && pin.isExclusive()) {
                        int[] pinIds = pin.ids();
                        long key = ((long) pinIds[0] << 32) | (pinIds[1] & 0xFFFFFFFFL);
                        if (exclusivePins.contains(key)) {
                            crossingSet.putIfAbsent(conn, new ConnCostRef(0, conn));
                        }
                    }
                }

                if (!crossingSet.isEmpty()) {
                    crossingSetsList.add(new ArrayList<>(crossingSet.values()));
                }
            }
            return crossingSetsList;
        }

        boolean connsKnownToCross(ConnRef conn1, ConnRef conn2) {
            int idx1 = groupIndexForConn(conn1);
            int idx2 = groupIndexForConn(conn2);
            if (idx1 == idx2 && idx1 != -1) {
                Map<ConnRef, Set<ConnRef>> pairsSet = pairsSetList.get(idx1);
                Set<ConnRef> crossSet = pairsSet.get(conn1);
                return crossSet != null && crossSet.contains(conn2);
            }
            return false;
        }

        private ConnCostRef removeConnectorWithMostCrossings(
                Map<ConnRef, Set<ConnRef>> pairsSet) {
            ConnRef candidateConnector = null;
            int candidateCrossingCount = 0;
            double candidateEstimatedCost = 0;

            for (Map.Entry<ConnRef, Set<ConnRef>> entry : pairsSet.entrySet()) {
                int crossings = entry.getValue().size();
                if (crossings == 0) {
                    continue;
                }
                double cost = cheapEstimatedCost(entry.getKey());
                if ((crossings > candidateCrossingCount) ||
                        ((crossings == candidateCrossingCount) &&
                                (cost > candidateEstimatedCost))) {
                    candidateConnector = entry.getKey();
                    candidateCrossingCount = crossings;
                    candidateEstimatedCost = cost;
                }
            }

            if (candidateConnector == null) {
                return null;
            }

            // Remove the candidate from the group
            Set<ConnRef> connSet = pairsSet.get(candidateConnector);
            if (connSet != null) {
                for (ConnRef other : connSet) {
                    Set<ConnRef> otherSet = pairsSet.get(other);
                    if (otherSet != null) {
                        otherSet.remove(candidateConnector);
                    }
                }
                connSet.clear();
            }

            return new ConnCostRef(candidateCrossingCount, candidateConnector);
        }

        private int groupIndexForConn(ConnRef conn) {
            for (int i = 0; i < pairsSetList.size(); i++) {
                if (pairsSetList.get(i).containsKey(conn)) {
                    return i;
                }
            }
            return -1;
        }

        private Map<ConnRef, Set<ConnRef>> groupForCrossingConns(
                ConnRef conn1, ConnRef conn2) {
            int idx1 = groupIndexForConn(conn1);
            int idx2 = groupIndexForConn(conn2);

            if (idx1 == -1 && idx2 == -1) {
                // Neither are part of a group. Create one.
                Map<ConnRef, Set<ConnRef>> newGroup = new HashMap<>();
                pairsSetList.add(newGroup);
                return newGroup;
            } else if (idx1 != -1 && idx2 == -1) {
                return pairsSetList.get(idx1);
            } else if (idx1 == -1 && idx2 != -1) {
                return pairsSetList.get(idx2);
            } else if (idx1 != idx2) {
                // Two different groups — merge them.
                Map<ConnRef, Set<ConnRef>> group1 = pairsSetList.get(idx1);
                Map<ConnRef, Set<ConnRef>> group2 = pairsSetList.get(idx2);
                for (Map.Entry<ConnRef, Set<ConnRef>> entry : group2.entrySet()) {
                    group1.merge(entry.getKey(), entry.getValue(), (a, b) -> {
                        a.addAll(b);
                        return a;
                    });
                }
                pairsSetList.remove(idx2);
                return group1;
            } else {
                // Same group already.
                return pairsSetList.get(idx1);
            }
        }
    }

    /** A pair of (cost, ConnRef) for ordering connectors during rerouting. */
    private record ConnCostRef(double cost, ConnRef conn) {}

    private void improveCrossings() {
        double crossing_penalty = routingParameter(RoutingParameter.crossingPenalty);
        double shared_path_penalty = routingParameter(RoutingParameter.fixedSharedPathPenalty);
        if (crossing_penalty == 0 && shared_path_penalty == 0) {
            // No penalties, return.
            return;
        }

        // Information on crossing connector groups.
        CrossingConnectorsInfo crossingConnInfo = new CrossingConnectorsInfo();

        // Find crossings and reroute connectors.
        m_in_crossing_rerouting_stage = true;

        List<ConnRef> connList = new ArrayList<>(m_connectors);
        for (int ii = 0; ii < connList.size(); ii++) {
            ConnRef connI = connList.get(ii);
            // Progress reporting and continuation check.
            if (m_abort_transaction) {
                m_in_crossing_rerouting_stage = false;
                return;
            }

            Polygon iRoute = connI.route();
            if (iRoute.size() == 0) {
                // Rerouted hyperedges will have an empty route.
                continue;
            }

            for (int jj = ii + 1; jj < connList.size(); jj++) {
                ConnRef connJ = connList.get(jj);

                if (crossingConnInfo.connsKnownToCross(connI, connJ)) {
                    // We already know both these have crossings.
                    continue;
                }

                // Determine if this pair cross.
                Polygon jRoute = connJ.route();
                if (jRoute.size() == 0) continue;

                ConnectorCrossings cross = new ConnectorCrossings(
                        iRoute, true, jRoute, connI, connJ);
                for (int jInd = 1; jInd < jRoute.size(); ++jInd) {
                    boolean finalSegment = ((jInd + 1) == jRoute.size());
                    cross.countForSegment(jInd, finalSegment);

                    if ((shared_path_penalty > 0) &&
                            (cross.crossingFlags & ConnectorCrossings.CROSSING_SHARES_PATH) != 0 &&
                            (cross.crossingFlags & ConnectorCrossings.CROSSING_SHARES_FIXED_SEGMENT) != 0 &&
                            (m_routing_options[RoutingOption.penaliseOrthogonalSharedPathsAtConnEnds.ordinal()] ||
                                    (cross.crossingFlags & ConnectorCrossings.CROSSING_SHARES_PATH_AT_END) == 0)) {
                        // We are penalising fixedSharedPaths and there is one.
                        crossingConnInfo.addCrossing(connI, connJ);
                        break;
                    } else if ((crossing_penalty > 0) && (cross.crossingCount > 0)) {
                        // We are penalising crossings and this is a crossing.
                        crossingConnInfo.addCrossing(connI, connJ);
                        break;
                    }
                }
            }
        }

        // Find the list of connector sets that need to be rerouted.
        List<List<ConnCostRef>> crossingConnsGroups =
                crossingConnInfo.getCrossingGroups();

        // Reroute via two passes for each group:
        //  1) clear existing routes and free pin assignments
        //  2) compute new routes
        for (List<ConnCostRef> crossingSet : crossingConnsGroups) {
            // Sort from lowest to highest cost.
            List<ConnCostRef> orderedConnList = new ArrayList<>(crossingSet);
            orderedConnList.sort(Comparator.comparingDouble(ConnCostRef::cost));

            // Perform rerouting of this set of connectors.
            for (int pass = 0; pass < 2; ++pass) {
                for (ConnCostRef connCostRef : orderedConnList) {
                    ConnRef conn = connCostRef.conn();
                    if (pass == 0) {

                        // Mark the fixed shared path as being invalid.
                        conn.makePathInvalid();

                        // Free the previous path, so it is not noticed by
                        // other connectors during rerouting.
                        conn.freeRoutes();

                        // Free pin assignments.
                        conn.freeActivePins();
                    } else if (pass == 1) {
                        // Progress reporting and continuation check.
                        if (m_abort_transaction) {
                            m_in_crossing_rerouting_stage = false;
                            return;
                        }

                        // Recompute this path.
                        conn.generatePath();
                    }
                }
            }
        }
        m_in_crossing_rerouting_stage = false;
    }

    // -----------------------------------------------------------------------
    // Contains / clusters management
    // -----------------------------------------------------------------------

    // Translated from Router::generateContains() in router.cpp line 1674.
    void generateContains(VertInf pt) {
        contains.put(pt.id, new HashSet<>());
        enclosingClusters.put(pt.id, new HashSet<>());

        // Don't count points on the border as being inside.
        boolean countBorder = false;

        // Compute enclosing shapes.
        for (Obstacle obstacle : m_obstacles) {
            if (Geometry.inPoly(obstacle.routingPolygon(), pt.point, countBorder)) {
                contains.get(pt.id).add(obstacle.id());
            }
        }
    }

    // Translated from Router::adjustContainsWithAdd() in router.cpp line 1729.
    private void adjustContainsWithAdd(Polygon poly, int p_shape) {
        // Don't count points on the border as being inside.
        boolean countBorder = false;

        for (VertInf k = vertices.connsBegin(); k != vertices.shapesBegin(); k = k.lstNext) {
            if (Geometry.inPoly(poly, k.point, countBorder)) {
                contains.computeIfAbsent(k.id, _ -> new HashSet<>()).add(p_shape);
            }
        }
    }

    // Translated from Router::adjustContainsWithDel() in router.cpp line 1745.
    private void adjustContainsWithDel(int p_shape) {
        for (Set<Integer> set : contains.values()) {
            set.remove(p_shape);
        }
    }

    // -----------------------------------------------------------------------
    // Visibility graph blocking (WP3)
    // -----------------------------------------------------------------------

    // Translated from Router::newBlockingShape() in router.cpp line 1551.
    void newBlockingShape(Polygon poly, int pid) {
        // Check all visibility edges to see if this one shape blocks them.
        EdgeInf iter = visGraph.begin();
        while (iter != null) {
            EdgeInf tmp = iter;
            iter = iter.lstNext;

            if (tmp.getDist() != 0) {
                VertID eID1 = tmp.ids()[0];
                VertID eID2 = tmp.ids()[1];
                Point e1 = tmp.points()[0];
                Point e2 = tmp.points()[1];
                boolean blocked = false;

                boolean countBorder = false;
                boolean ep_in_poly1 = eID1.isConnPt() ? Geometry.inPoly(poly, e1, countBorder) : false;
                boolean ep_in_poly2 = eID2.isConnPt() ? Geometry.inPoly(poly, e2, countBorder) : false;
                if (ep_in_poly1 || ep_in_poly2) {
                    // Don't check edges that have a connector endpoint
                    // and are inside the shape being added.
                    continue;
                }

                boolean[] seenIntersectionAtEndpoint = {false};
                for (int pt_i = 0; pt_i < poly.size(); ++pt_i) {
                    int pt_n = (pt_i == (poly.size() - 1)) ? 0 : pt_i + 1;
                    Point pi = poly.ps.get(pt_i);
                    Point pn = poly.ps.get(pt_n);
                    if (Geometry.segmentShapeIntersect(e1, e2, pi, pn, seenIntersectionAtEndpoint)) {
                        blocked = true;
                        break;
                    }
                }
                if (blocked) {
                    tmp.alertConns();
                    if (InvisibilityGrph) {
                        tmp.addBlocker(pid);
                    } else {
                        tmp.remove();
                    }
                }
            }
        }
    }

    // Translated from Router::checkAllBlockedEdges() in router.cpp line 1616.
    void checkAllBlockedEdges(int pid) {
        assert InvisibilityGrph;

        EdgeInf iter = invisGraph.begin();
        while (iter != null) {
            EdgeInf tmp = iter;
            iter = iter.lstNext;

            if (tmp.blocker() == -1) {
                tmp.alertConns();
                tmp.checkVis();
            } else if (tmp.blocker() == pid) {
                tmp.checkVis();
            }
        }
    }

    // Translated from Router::checkAllMissingEdges() in router.cpp line 1638.
    void checkAllMissingEdges() {
        assert !InvisibilityGrph;

        VertInf first = vertices.connsBegin();
        VertInf pend = vertices.end();

        for (VertInf i = first; i != pend; i = i.lstNext) {
            VertID iID = i.id;

            // Check remaining, earlier vertices
            for (VertInf j = first; j != i; j = j.lstNext) {
                VertID jID = j.id;
                if (iID.isConnPt() && !iID.isConnectionPin() &&
                        (iID.objID != jID.objID)) {
                    // Don't keep visibility between edges of different conns
                    continue;
                }

                // See if the edge is already there?
                boolean found = (EdgeInf.existingEdge(i, j) != null);

                if (!found) {
                    // Didn't already exist, check.
                    boolean knownNew = true;
                    EdgeInf.checkEdgeVisibility(i, j, knownNew);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Graph management
    // -----------------------------------------------------------------------

    // C++ router.cpp:431-449
    public void regenerateStaticBuiltGraph() {
        if (m_static_orthogonal_graph_invalidated) {
            if (m_allows_orthogonal_routing) {
                destroyOrthogonalVisGraph();
                OrthogonalRouter.generateStaticOrthogonalVisGraph(this);
            }
            m_static_orthogonal_graph_invalidated = false;
        }
    }

    public void destroyOrthogonalVisGraph() {
        // Remove orthogonal visibility graph edges.
        visOrthogGraph.clear();

        // Remove the now orphaned vertices.
        // C++ router.cpp:414-427
        VertInf curr = vertices.shapesBegin();
        while (curr != null) {
            if (curr.orphaned() && curr.id.equals(VertInf.dummyOrthogID)) {
                curr = vertices.removeVertex(curr);
                continue;
            }
            curr = curr.lstNext;
        }
    }

    public void setStaticGraphInvalidated(boolean invalidated) {
        m_static_orthogonal_graph_invalidated = invalidated;
    }

    // -----------------------------------------------------------------------
    // Connector type validation
    // -----------------------------------------------------------------------

    public ConnType validConnType(ConnType select) {
        if (select != null && select != ConnType.None) {
            if (select == ConnType.Orthogonal && m_allows_orthogonal_routing) {
                return ConnType.Orthogonal;
            } else if (select == ConnType.Polyline && m_allows_polyline_routing) {
                return ConnType.Polyline;
            }
        }

        if (m_allows_polyline_routing) {
            return ConnType.Polyline;
        } else if (m_allows_orthogonal_routing) {
            return ConnType.Orthogonal;
        }
        return ConnType.None;
    }

    public ConnType validConnType() {
        return validConnType(ConnType.None);
    }


    // -----------------------------------------------------------------------
    // Polyline connector rerouting heuristic
    // -----------------------------------------------------------------------

    void markPolylineConnectorsNeedingReroutingForDeletedObstacle(
            Obstacle obstacle) {
        if (RubberBandRouting) {
            // When rubber-band routing, we do not reroute connectors that
            // may have a better route, only invalid connectors.
            return;
        }

        // For each connector...
        for (ConnRef conn : m_connectors) {
            if (conn.route().empty()) {
                // Ignore uninitialised connectors.
                continue;
            } else if (conn.needsReroute()) {
                // Already marked, so skip.
                continue;
            } else if (conn.routingType() != ConnType.Polyline) {
                // This test only works for polyline connectors, so skip others.
                continue;
            }

            Polygon route = conn.route();
            Point start = route.ps.getFirst();
            Point end = route.ps.get(route.size() - 1);

            // C++ router.cpp:1805 — use precomputed route distance
            double conndist = conn.m_route_dist;

            // For each vertex pair of the obstacle polygon...
            Polygon obsPoly = obstacle.polygon();
            for (int pt_i = 0; pt_i < obsPoly.size(); ++pt_i) {
                int pt_n = (pt_i == (obsPoly.size() - 1)) ? 0 : pt_i + 1;
                Point p1 = obsPoly.ps.get(pt_i);
                Point p2 = obsPoly.ps.get(pt_n);

                double offy;
                double a, b, c, d;
                double min, max;

                if (p1.y == p2.y) {
                    offy = p1.y;
                    a = start.x;
                    b = start.y - offy;
                    c = end.x;
                    d = end.y - offy;
                    min = Math.min(p1.x, p2.x);
                    max = Math.max(p1.x, p2.x);
                } else if (p1.x == p2.x) {
                    offy = p1.x;
                    a = start.y;
                    b = start.x - offy;
                    c = end.y;
                    d = end.x - offy;
                    min = Math.min(p1.y, p2.y);
                    max = Math.max(p1.y, p2.y);
                } else {
                    // Need to do rotation
                    Point n_p2 = new Point(p2.x - p1.x, p2.y - p1.y);
                    Point n_start = new Point(start.x - p1.x, start.y - p1.y);
                    Point n_end = new Point(end.x - p1.x, end.y - p1.y);

                    double theta = 0 - Math.atan2(n_p2.y, n_p2.x);

                    Point r_p1 = new Point(0, 0);
                    Point r_p2 = new Point(n_p2);
                    Point rStart = new Point(n_start);
                    Point rEnd = new Point(n_end);

                    double cosv = Math.cos(theta);
                    double sinv = Math.sin(theta);

                    r_p2.x = cosv * n_p2.x - sinv * n_p2.y;
                    r_p2.y = cosv * n_p2.y + sinv * n_p2.x;
                    rStart.x = cosv * n_start.x - sinv * n_start.y;
                    rStart.y = cosv * n_start.y + sinv * n_start.x;
                    rEnd.x = cosv * n_end.x - sinv * n_end.y;
                    rEnd.y = cosv * n_end.y + sinv * n_end.x;

                    r_p2.y = 0;

                    offy = r_p1.y;
                    a = rStart.x;
                    b = rStart.y - offy;
                    c = rEnd.x;
                    d = rEnd.y - offy;
                    min = Math.min(r_p1.x, r_p2.x);
                    max = Math.max(r_p1.x, r_p2.x);

                    start = rStart;
                    end = rEnd;
                }

                double x;
                if ((b + d) == 0) {
                    d = d * -1;
                }

                if ((b == 0) && (d == 0)) {
                    if (((a < min) && (c < min)) ||
                            ((a > max) && (c > max))) {
                        x = a;
                    } else {
                        continue;
                    }
                } else {
                    x = ((b * c) + (a * d)) / (b + d);
                }

                x = Math.max(min, x);
                x = Math.min(max, x);

                Point xp;
                if (p1.x == p2.x) {
                    xp = new Point(offy, x);
                } else {
                    xp = new Point(x, offy);
                }

                double e1 = Geometry.euclideanDist(start, xp);
                double e2 = Geometry.euclideanDist(xp, end);
                double estdist = e1 + e2;

                if (estdist < conndist) {
                    conn.makePathInvalid();
                    break;
                }
            }
        }
    }

    // routeDistance(Polygon) removed — C++ reads conn->m_route_dist directly (router.cpp:1805)

    // -----------------------------------------------------------------------
    // Remove object from queued actions
    // -----------------------------------------------------------------------

    void removeObjectFromQueuedActions(Object object) {
        actionList.removeIf(curr -> curr.objPtr == object);
    }

    // -----------------------------------------------------------------------
    // Testing and debugging methods
    // -----------------------------------------------------------------------

    public boolean existsOrthogonalFixedSegmentOverlap() {
        return existsOrthogonalFixedSegmentOverlap(false);
    }

    public boolean existsOrthogonalFixedSegmentOverlap(boolean atEnds) {
        List<ConnRef> connList = new ArrayList<>(m_connectors);
        for (int i = 0; i < connList.size(); i++) {
            ConnRef ci = connList.get(i);
            // Use copies so splitBranchingSegments does not modify the actual display routes.
            Polygon iRoute = new Polygon(ci.displayRoute());
            for (int j = i + 1; j < connList.size(); j++) {
                ConnRef cj = connList.get(j);
                Polygon jRoute = new Polygon(cj.displayRoute());
                ConnectorCrossings cross = new ConnectorCrossings(
                        iRoute, true, jRoute, ci, cj);
                cross.checkForBranchingSegments = true;
                for (int jInd = 1; jInd < jRoute.size(); ++jInd) {
                    boolean finalSegment = ((jInd + 1) == jRoute.size());
                    cross.countForSegment(jInd, finalSegment);

                    if ((cross.crossingFlags & ConnectorCrossings.CROSSING_SHARES_PATH) != 0 &&
                            (cross.crossingFlags & ConnectorCrossings.CROSSING_SHARES_FIXED_SEGMENT) != 0 &&
                            (atEnds ||
                                    (cross.crossingFlags & ConnectorCrossings.CROSSING_SHARES_PATH_AT_END) == 0)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean existsOrthogonalTouchingPaths() {
        List<ConnRef> connList = new ArrayList<>(m_connectors);
        for (int i = 0; i < connList.size(); i++) {
            Polygon iRoute = new Polygon(connList.get(i).displayRoute());
            for (int j = i + 1; j < connList.size(); j++) {
                Polygon jRoute = new Polygon(connList.get(j).displayRoute());
                ConnectorCrossings cross = new ConnectorCrossings(
                        iRoute, true, jRoute, connList.get(i), connList.get(j));
                cross.checkForBranchingSegments = true;
                for (int jInd = 1; jInd < jRoute.size(); ++jInd) {
                    boolean finalSegment = ((jInd + 1) == jRoute.size());
                    cross.countForSegment(jInd, finalSegment);

                    if ((cross.crossingFlags & ConnectorCrossings.CROSSING_TOUCHES) != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public int existsCrossings() {
        return existsCrossings(false);
    }

    public int existsCrossings(boolean optimisedForConnectorType) {
        int count = 0;
        List<ConnRef> connList = new ArrayList<>(m_connectors);
        for (int i = 0; i < connList.size(); i++) {
            Polygon iRoute = new Polygon(connList.get(i).displayRoute());
            for (int j = i + 1; j < connList.size(); j++) {
                Polygon jRoute = new Polygon(connList.get(j).displayRoute());
                ConnRef iConn = optimisedForConnectorType ? connList.get(i) : null;
                ConnRef jConn = optimisedForConnectorType ? connList.get(j) : null;
                ConnectorCrossings cross = new ConnectorCrossings(
                        iRoute, true, jRoute, iConn, jConn);
                cross.checkForBranchingSegments = true;
                for (int jInd = 1; jInd < jRoute.size(); ++jInd) {
                    boolean finalSegment = ((jInd + 1) == jRoute.size());
                    cross.countForSegment(jInd, finalSegment);
                    count += cross.crossingCount;
                }
            }
        }
        return count;
    }

    public int existsInvalidOrthogonalPaths() {
        int count = 0;
        for (ConnRef conn : m_connectors) {
            if (conn.routingType() == ConnType.Orthogonal) {
                Polygon iRoute = conn.displayRoute();
                for (int iInd = 1; iInd < iRoute.size(); ++iInd) {
                    if ((iRoute.at(iInd - 1).x != iRoute.at(iInd).x) &&
                            (iRoute.at(iInd - 1).y != iRoute.at(iInd).y)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
