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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The HyperedgeRerouter class is used for optimally routing hyperedges.
 * Corresponds to hyperedge.h/hyperedge.cpp in C++.
 * A hyperedge is a set of connectors and junctions that form a tree
 * connecting multiple endpoints. The HyperedgeRerouter allows you to
 * register sets of terminals (ConnEnd objects) and then route them
 * as a group, potentially creating and deleting junctions and connectors
 * to produce an optimal tree.
 */
public class HyperedgeRerouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HyperedgeRerouter.class);

    // C++: ConnEndListVector m_terminals_vector
    private final List<List<ConnEnd>> m_terminals_vector = new ArrayList<>();
    // C++: JunctionRefVector m_root_junction_vector
    private final List<JunctionRef> m_root_junction_vector = new ArrayList<>();

    // C++: JunctionRefListVector m_new_junctions_vector
    private final List<List<JunctionRef>> m_new_junctions_vector = new ArrayList<>();
    // C++: JunctionRefListVector m_deleted_junctions_vector
    private final List<List<JunctionRef>> m_deleted_junctions_vector = new ArrayList<>();
    // C++: ConnRefListVector m_new_connectors_vector
    private final List<List<ConnRef>> m_new_connectors_vector = new ArrayList<>();
    // C++: ConnRefListVector m_deleted_connectors_vector
    private final List<List<ConnRef>> m_deleted_connectors_vector = new ArrayList<>();
    // C++: VertexSetVector m_terminal_vertices_vector
    private final List<Set<VertInf>> m_terminal_vertices_vector = new ArrayList<>();
    // C++: VertexList m_added_vertices
    private final List<VertInf> m_added_vertices = new ArrayList<>();

    private Router m_router;

    HyperedgeRerouter() {
    }

    void setRouter(Router router) {
        m_router = router;
    }

    /**
     * Registers a hyperedge to be fully rerouted the next time the router
     * processes a transaction.
     * Translated from HyperedgeRerouter::registerHyperedgeForRerouting(ConnEndList)
     * in hyperedge.cpp lines 51-58.
     */
    public void registerHyperedgeForRerouting(List<ConnEnd> terminals) {
        m_terminals_vector.add(new ArrayList<>(terminals));
        m_root_junction_vector.add(null);
    }

    /**
     * Registers a hyperedge to be fully rerouted the next time the router
     * processes a transaction, given a junction that is part of the hyperedge.
     * Translated from HyperedgeRerouter::registerHyperedgeForRerouting(JunctionRef*)
     * in hyperedge.cpp lines 60-67.
     */
    public void registerHyperedgeForRerouting(JunctionRef junction) {
        m_terminals_vector.add(new ArrayList<>());
        m_root_junction_vector.add(junction);
    }

    /**
     * Returns the number of registered hyperedges.
     * Translated from HyperedgeRerouter::count() in hyperedge.cpp lines 69-72.
     */
    public int count() {
        return m_terminals_vector.size();
    }

    /**
     * Returns a HyperedgeNewAndDeletedObjectLists detailing the lists of
     * junctions and connectors created and deleted during hyperedge rerouting.
     * Translated from HyperedgeRerouter::newAndDeletedObjectLists(size_t)
     * in hyperedge.cpp lines 74-87.
     */
    public HyperedgeNewAndDeletedObjectLists newAndDeletedObjectLists(int index) {
        assert index <= count();

        HyperedgeNewAndDeletedObjectLists result = new HyperedgeNewAndDeletedObjectLists();

        result.newJunctionList.addAll(m_new_junctions_vector.get(index));
        result.deletedJunctionList.addAll(m_deleted_junctions_vector.get(index));
        result.newConnectorList.addAll(m_new_connectors_vector.get(index));
        result.deletedConnectorList.addAll(m_deleted_connectors_vector.get(index));

        return result;
    }

    /**
     * Follow connected junctions and connectors from the given connector to
     * determine the hyperedge topology, saving objects to the deleted-objects
     * vectors as we go.
     * Translated from HyperedgeRerouter::findAttachedObjects(size_t, ConnRef*, JunctionRef*, ConnRefSet&)
     * in hyperedge.cpp lines 128-172.
     */
    private boolean findAttachedObjects(int index, ConnRef connector,
            JunctionRef ignore, Set<ConnRef> hyperedgeConns) {
        boolean validHyperedge = false;

        connector.assignConnectionPinVisibility(true);

        m_deleted_connectors_vector.get(index).add(connector);
        hyperedgeConns.add(connector);

        Obstacle[] anchors = connector.endpointAnchors();
        JunctionRef jFirst = (anchors[0] instanceof JunctionRef) ? (JunctionRef) anchors[0] : null;
        JunctionRef jSecond = (anchors[1] instanceof JunctionRef) ? (JunctionRef) anchors[1] : null;

        if (jFirst != null) {
            // If attached to a junction and not one we've explored, then continue.
            if (jFirst != ignore) {
                validHyperedge |= findAttachedObjects(index, jFirst, connector, hyperedgeConns);
            }
        } else {
            // If its an endpoint, then record the vertex for this endpoint.
            assert connector.m_src_vert != null;
            m_terminal_vertices_vector.get(index).add(connector.m_src_vert);
        }

        if (jSecond != null) {
            // If attached to a junction and not one we've explored, then continue.
            if (jSecond != ignore) {
                validHyperedge |= findAttachedObjects(index, jSecond, connector, hyperedgeConns);
            }
        } else {
            // If its an endpoint, then record the vertex for this endpoint.
            assert connector.m_dst_vert != null;
            m_terminal_vertices_vector.get(index).add(connector.m_dst_vert);
        }
        return validHyperedge;
    }

    /**
     * Follow connected junctions and connectors from the given junction to
     * determine the hyperedge topology, saving objects to the deleted-objects
     * vectors as we go.
     * Translated from HyperedgeRerouter::findAttachedObjects(size_t, JunctionRef*, ConnRef*, ConnRefSet&)
     * in hyperedge.cpp lines 178-206.
     */
    private boolean findAttachedObjects(int index, JunctionRef junction,
            ConnRef ignore, Set<ConnRef> hyperedgeConns) {
        boolean validHyperedge = false;

        m_deleted_junctions_vector.get(index).add(junction);

        List<ConnRef> connectors = junction.attachedConnectors();

        if (connectors.size() > 2) {
            // A valid hyperedge must have at least one junction with three
            // connectors attached, i.e., more than two endpoints.
            validHyperedge = true;
        }

        for (ConnRef curr : connectors) {
            if (curr == ignore) {
                continue;
            }

            assert curr != null;
            validHyperedge |= findAttachedObjects(index, curr, junction, hyperedgeConns);
        }
        return validHyperedge;
    }

    /**
     * Populate the deleted-object vectors with all the connectors and junctions
     * that form the registered hyperedges. Then return the set of all these
     * connectors so they can be ignored for individual rerouting.
     * Translated from HyperedgeRerouter::calcHyperedgeConnectors()
     * in hyperedge.cpp lines 212-274.
     */
    Set<ConnRef> calcHyperedgeConnectors() {
        assert m_router != null;

        Set<ConnRef> allRegisteredHyperedgeConns = new HashSet<>();

        // Clear the deleted-object vectors. We populate them here if necessary.
        m_deleted_junctions_vector.clear();
        m_deleted_connectors_vector.clear();
        m_terminal_vertices_vector.clear();
        m_added_vertices.clear();

        int numHyperedges = count();
        for (int i = 0; i < numHyperedges; ++i) {
            m_deleted_junctions_vector.add(new ArrayList<>());
            m_deleted_connectors_vector.add(new ArrayList<>());
            m_terminal_vertices_vector.add(new HashSet<>());
        }

        // Populate the deleted-object vectors.
        for (int i = 0; i < numHyperedges; ++i) {
            if (m_root_junction_vector.get(i) != null) {
                // Follow objects attached to junction to find the hyperedge.
                boolean valid = findAttachedObjects(i, m_root_junction_vector.get(i), null,
                        allRegisteredHyperedgeConns);
                if (!valid) {
                    LOGGER.error("Warning: Hyperedge {} registered with HyperedgeRerouter is invalid and will be ignored.", i);
                    // Hyperedge is invalid. Clear the terminals and other info
                    // so it will be ignored, and rerouted as a normal set of connectors.
                    m_terminals_vector.get(i).clear();
                    m_terminal_vertices_vector.get(i).clear();
                    m_deleted_junctions_vector.get(i).clear();
                    m_deleted_connectors_vector.get(i).clear();
                }
                continue;
            }

            // Alternatively, we have a set of ConnEnds, so store the
            // corresponding terminals
            for (ConnEnd it : m_terminals_vector.get(i)) {
                Pair<Boolean, VertInf> maybeNewVertex = it.getHyperedgeVertex(m_router);
                assert maybeNewVertex.second != null;
                m_terminal_vertices_vector.get(i).add(maybeNewVertex.second);

                if (maybeNewVertex.first) {
                    // This is a newly created vertex. Remember it so we can
                    // free it and its visibility edges later.
                    m_added_vertices.add(maybeNewVertex.second);
                }
            }
        }

        // Return these connectors that don't require rerouting.
        return allRegisteredHyperedgeConns;
    }

    /**
     * Performs the actual rerouting of all registered hyperedges using the MTST algorithm.
     * Translated from HyperedgeRerouter::performRerouting()
     * in hyperedge.cpp lines 277-384.
     */
    void performRerouting() {
        assert m_router != null;

        m_new_junctions_vector.clear();
        m_new_connectors_vector.clear();

        int numHyperedges = count();
        for (int i = 0; i < numHyperedges; ++i) {
            m_new_junctions_vector.add(new ArrayList<>());
            m_new_connectors_vector.add(new ArrayList<>());
        }

        // For each hyperedge...
        for (int i = 0; i < numHyperedges; ++i) {
            if (m_terminal_vertices_vector.get(i).isEmpty()) {
                // Invalid hyperedge, ignore.
                continue;
            }

            // Execute the MTST method to find good junction positions and an
            // initial path. A hyperedge tree will be built for the new route.
            Map<JunctionRef, HyperedgeTreeNode> hyperedgeTreeJunctions = new HashMap<>();
            MinimumTerminalSpanningTree mtst = new MinimumTerminalSpanningTree(m_router,
                    m_terminal_vertices_vector.get(i), hyperedgeTreeJunctions);

            // The preferred MTST construction method.
            // Slightly slower, better quality results.
            mtst.constructInterleaved();

            HyperedgeTreeNode treeRoot = mtst.rootJunction();
            assert treeRoot != null;

            // Fill in connector information and join them to junctions of endpoints
            // of original connectors.
            treeRoot.addConns(null, m_router,
                    m_deleted_connectors_vector.get(i), null);

            // Output the list of new junctions and connectors from hyperedge tree.
            treeRoot.listJunctionsAndConnectors(null, m_new_junctions_vector.get(i),
                    m_new_connectors_vector.get(i));

            // Write paths from the hyperedge tree back into individual
            // connector routes.
            for (int pass = 0; pass < 2; ++pass) {
                treeRoot.writeEdgesToConns(null, pass);
            }

            // Tell the router that we are deleting the objects used for the
            // previous path for the hyperedge.
            for (ConnRef curr : m_deleted_connectors_vector.get(i)) {
                // Clear visibility assigned for connection pins.
                curr.assignConnectionPinVisibility(false);
                m_router.deleteConnector(curr);
            }
            for (JunctionRef curr : m_deleted_junctions_vector.get(i)) {
                m_router.deleteJunction(curr);
            }

            // Free the temporary hyperedge tree representation.
            treeRoot.deleteEdgesExcept(null);
        }

        // Clear the input to this class, so that new objects can be registered
        // for rerouting for the next time that transaction is processed.
        m_terminals_vector.clear();
        m_root_junction_vector.clear();

        // Free temporarily added vertices.
        for (VertInf curr : m_added_vertices) {
            curr.removeFromGraph();
            m_router.vertices.removeVertex(curr);
        }
        m_added_vertices.clear();
    }
}
