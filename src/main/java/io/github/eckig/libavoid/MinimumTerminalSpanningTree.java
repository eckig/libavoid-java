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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds a Minimum Terminal Spanning Tree (MTST) for hyperedge routing.
 * Translated from MinimumTerminalSpanningTree in mtst.h/mtst.cpp.
 *
 * Sequential Construction of the Minimum Terminal Spanning Tree is an
 * extended version of the method described in Section IV.B of:
 *     Long, J., Zhou, H., Memik, S.O. (2008). EBOARST: An efficient
 *     edge-based obstacle-avoiding rectilinear Steiner tree construction
 *     algorithm. IEEE Trans. on Computer-Aided Design of Integrated
 *     Circuits and Systems 27(12), pages 2169--2182.
 */
class MinimumTerminalSpanningTree {

    // C++: typedef std::list<VertexSet> VertexSetList
    // C++: typedef std::pair<EdgeInf *, VertInf *> LayeredOrthogonalEdge
    // C++: typedef std::list<LayeredOrthogonalEdge> LayeredOrthogonalEdgeList

    private final Router router;
    private final boolean isOrthogonal;
    private final Set<VertInf> terminals;
    private Set<VertInf> origTerminals;
    private final Map<JunctionRef, HyperedgeTreeNode> hyperedgeTreeJunctions;

    // C++: VertexNodeMap nodes — maps VertInf* to HyperedgeTreeNode*
    private final Map<VertInf, HyperedgeTreeNode> nodes = new HashMap<>();
    private HyperedgeTreeNode m_rootJunction;
    private final double bendPenalty;

    private final List<VertInf> extraVertices = new ArrayList<>();
    // C++: std::list<VertInf **> rootVertexPointers — in Java, list of 1-element arrays
    private final List<VertInf[]> rootVertexPointers = new ArrayList<>();

    // Vertex heap for extended Dijkstra's algorithm.
    // C++: std::vector<VertInf *> vHeap with HeapCmpVertInf (a->sptfDist > b->sptfDist = min-heap by sptfDist)
    private List<VertInf> vHeap = new ArrayList<>();

    // Bridging edge heap for the extended Kruskal's algorithm.
    // C++: std::vector<EdgeInf *> beHeap with CmpEdgeInf (a->mtstDist() > b->mtstDist() = min-heap by mtstDist)
    private List<EdgeInf> beHeap = new ArrayList<>();

    // C++: const VertID dimensionChangeVertexID(0, 42)
    private final VertID dimensionChangeVertexID;

    /**
     * Constructor.
     * Translated from MinimumTerminalSpanningTree::MinimumTerminalSpanningTree()
     * in mtst.cpp lines 76-87.
     */
    MinimumTerminalSpanningTree(Router router, Set<VertInf> terminals,
            Map<JunctionRef, HyperedgeTreeNode> hyperedgeTreeJunctions) {
        this.router = router;
        this.isOrthogonal = true;
        this.terminals = new HashSet<>(terminals);
        this.hyperedgeTreeJunctions = hyperedgeTreeJunctions;
        this.m_rootJunction = null;
        this.bendPenalty = 2000;
        this.dimensionChangeVertexID = new VertID(0, (short) 42);
    }

    /**
     * Returns the root junction node of the built hyperedge tree.
     * Translated from MinimumTerminalSpanningTree::rootJunction()
     * in mtst.cpp lines 98-101.
     */
    HyperedgeTreeNode rootJunction() {
        return m_rootJunction;
    }

    /**
     * Adds or finds a node in the hyperedge tree for the given vertex.
     * Creates a junction if the node is visited a second time.
     * Translated from MinimumTerminalSpanningTree::addNode()
     * in mtst.cpp lines 135-180.
     */
    private HyperedgeTreeNode addNode(VertInf vertex, HyperedgeTreeNode prevNode) {
        HyperedgeTreeNode node;

        // Do we already have a node for this vertex?
        HyperedgeTreeNode match = nodes.get(vertex);
        if (match == null) {
            // Not found. Create new node.
            HyperedgeTreeNode newNode = new HyperedgeTreeNode();
            newNode.point = vertex.point;
            // Remember it.
            nodes.put(vertex, newNode);
            node = newNode;
        } else {
            // Found.
            if (match.junction == null) {
                // Create a junction, if one has not already been created.
                match.junction = new JunctionRef(router, vertex.point);
                if (m_rootJunction == null) {
                    // Remember the first junction node, so we can use it to
                    // traverse the tree, adding and connecting connectors to
                    // junctions and endpoints.
                    m_rootJunction = match;
                }
                router.removeObjectFromQueuedActions(match.junction);
                match.junction.makeActive();
            }
            node = match;
        }

        if (prevNode != null) {
            // Join this node to the previous node.
            new HyperedgeTreeEdge(prevNode, node, null);
        }

        return node;
    }

    /**
     * Follows branches in a shortest path tree back to the root,
     * generating hyperedge tree nodes and branches as it goes.
     * Translated from MinimumTerminalSpanningTree::buildHyperedgeTreeToRoot()
     * in mtst.cpp lines 182-249.
     */
    private void buildHyperedgeTreeToRoot(VertInf currVert,
            HyperedgeTreeNode prevNode, VertInf prevVert, boolean markEdges) {
        if (prevNode != null && prevNode.junction != null) {
            // We've reached a junction, so stop.
            return;
        }

        assert currVert != null || !markEdges; // currVert may be null only if not marking

        // This method follows branches in a shortest path tree back to the
        // root, generating hyperedge tree nodes and branches as it goes.
        while (currVert != null) {
            // Add the node, if necessary.
            HyperedgeTreeNode currentNode = addNode(currVert, prevNode);

            if (markEdges) {
                EdgeInf edge = prevVert.hasNeighbour(currVert, isOrthogonal);
                if (edge == null && (currVert.id.vn == dimensionChangeVertexID.vn &&
                        currVert.id.objID == dimensionChangeVertexID.objID)) {
                    VertInf modCurr = (currVert.id.vn == dimensionChangeVertexID.vn &&
                            currVert.id.objID == dimensionChangeVertexID.objID) ?
                            currVert.m_orthogonalPartner : currVert;
                    VertInf modPrev = (prevVert.id.vn == dimensionChangeVertexID.vn &&
                            prevVert.id.objID == dimensionChangeVertexID.objID) ?
                            prevVert.m_orthogonalPartner : prevVert;
                    if (modPrev != null && modCurr != null) {
                        edge = modPrev.hasNeighbour(modCurr, isOrthogonal);
                    }
                }
            }

            if (currentNode.junction != null) {
                // We've reached a junction, so stop.
                break;
            }

            if (currVert.pathNext == null) {
                // This is a terminal of the hyperedge, mark the node with the
                // vertex representing the endpoint of the connector so we can
                // later use this to set the correct ConnEnd for the connector.
                currentNode.finalVertex = currVert;
            }

            if (currVert.id.isDummyPinHelper()) {
                // Note if we have an extra dummy vertex for connecting
                // to possible connection pins.
                currentNode.isPinDummyEndpoint = true;
            }

            prevNode = currentNode;
            prevVert = currVert;
            currVert = currVert.pathNext;
        }
    }

    /**
     * Follows branches in a shortest path tree back to the root,
     * resetting distances and rewriting tree root pointers.
     * Returns the old tree root pointer.
     * Translated from MinimumTerminalSpanningTree::resetDistsForPath()
     * in mtst.cpp lines 252-279.
     */
    private VertInf[] resetDistsForPath(VertInf currVert, VertInf[] newRootVertPtr) {
        assert currVert != null;

        // This method follows branches in a shortest path tree back to the
        // root, generating hyperedge tree nodes and branches as it goes.
        while (currVert != null) {
            if (currVert.sptfDist == 0) {
                VertInf[] oldTreeRootPtr = currVert.treeRootPointer();
                // We've reached a junction, so stop.
                rewriteRestOfHyperedge(currVert, newRootVertPtr);
                return oldTreeRootPtr;
            }

            currVert.sptfDist = 0;
            currVert.setTreeRootPointer(newRootVertPtr);

            terminals.add(currVert);

            currVert = currVert.pathNext;
        }

        // Shouldn't get here.
        assert false;
        return null;
    }

    /**
     * Returns or creates the orthogonal partner vertex for the given vertex.
     * Translated from MinimumTerminalSpanningTree::orthogonalPartner()
     * in mtst.cpp lines 523-541.
     */
    private VertInf orthogonalPartner(VertInf vert, double penalty) {
        if (penalty == 0) {
            penalty = bendPenalty;
        }
        if (vert.m_orthogonalPartner == null) {
            vert.m_orthogonalPartner = new VertInf(router,
                    dimensionChangeVertexID, vert.point, false);
            vert.m_orthogonalPartner.m_orthogonalPartner = vert;
            extraVertices.add(vert.m_orthogonalPartner);
            EdgeInf extraEdge = new EdgeInf(vert.m_orthogonalPartner, vert, isOrthogonal);
            extraEdge.setDist(penalty);
        }
        return vert.m_orthogonalPartner;
    }

    private VertInf orthogonalPartner(VertInf vert) {
        return orthogonalPartner(vert, 0);
    }

    /**
     * Removes invalid bridging edges from the bridging edge heap.
     * Translated from MinimumTerminalSpanningTree::removeInvalidBridgingEdges()
     * in mtst.cpp lines 543-575.
     */
    private void removeInvalidBridgingEdges() {
        // Look through the bridging edge heap for any now invalidated edges and
        // remove these by only copying valid edges to the beHeapNew array.
        List<EdgeInf> beHeapNew = new ArrayList<>(beHeap.size());
        for (EdgeInf e : beHeap) {
            VertInf[] ends = realVerticesCountingPartners(e);
            boolean valid = (ends[0].treeRoot() != ends[1].treeRoot()) &&
                    ends[0].treeRoot() != null && ends[1].treeRoot() != null &&
                    origTerminals.contains(ends[0].treeRoot()) &&
                    origTerminals.contains(ends[1].treeRoot());
            if (!valid) {
                // This is an invalid edge, don't copy it to beHeapNew.
                continue;
            }
            // Copy the other bridging edges to beHeapNew.
            beHeapNew.add(e);
        }
        // Replace beHeap with beHeapNew
        beHeap = beHeapNew;

        // Remake the bridging edge heap, since we've deleted many elements.
        // (beHeap is used as a manual heap in constructInterleaved)
    }

    /**
     * Returns the list of layered orthogonal edges from the given vertex,
     * excluding the direction back to prev.
     * Translated from MinimumTerminalSpanningTree::getOrthogonalEdgesFromVertex()
     * in mtst.cpp lines 577-631.
     */
    private List<Pair<EdgeInf, VertInf>> getOrthogonalEdgesFromVertex(VertInf vert, VertInf prev) {
        List<Pair<EdgeInf, VertInf>> edgeList = new ArrayList<>();

        assert vert != null;

        double penalty = (prev == null) ? 0.1 : 0;
        orthogonalPartner(vert, penalty);

        boolean isRealVert = !(vert.id.vn == dimensionChangeVertexID.vn &&
                vert.id.objID == dimensionChangeVertexID.objID);
        VertInf realVert = isRealVert ? vert : orthogonalPartner(vert);
        assert realVert != null;
        assert !(realVert.id.vn == dimensionChangeVertexID.vn &&
                realVert.id.objID == dimensionChangeVertexID.objID);

        List<EdgeInf> visList = isOrthogonal ? realVert.orthogVisList : realVert.visList;
        for (EdgeInf edge : visList) {
            VertInf other = edge.otherVert(realVert);

            if (other == orthogonalPartner(realVert)) {
                VertInf partner = isRealVert ? other : orthogonalPartner(other);
                if (partner != prev) {
                    edgeList.add(new Pair<>(edge, partner));
                }
                continue;
            }

            VertInf partner = isRealVert ? other : orthogonalPartner(other);
            assert partner != null;

            if (other.point.y == realVert.point.y) {
                if (isRealVert && (prev != partner)) {
                    edgeList.add(new Pair<>(edge, partner));
                }
            } else if (other.point.x == realVert.point.x) {
                if (!isRealVert && (prev != partner)) {
                    edgeList.add(new Pair<>(edge, partner));
                }
            } else {
                edgeList.add(new Pair<>(edge, other));
            }
        }

        return edgeList;
    }

    /**
     * Uses Interleaved construction of the MTST and SPTF (heuristic 2 from paper).
     * This is the preferred construction approach.
     * Translated from MinimumTerminalSpanningTree::constructInterleaved()
     * in mtst.cpp lines 633-830.
     */
    void constructInterleaved() {
        origTerminals = new HashSet<>(terminals);

        // Initialisation
        VertInf endVert = router.vertices.end();
        for (VertInf k = router.vertices.connsBegin(); k != endVert; k = k.lstNext) {
            k.sptfDist = Double.MAX_VALUE;
            k.pathNext = null;
            k.setTreeRootPointer(null);
            k.m_orthogonalPartner = null;
        }

        assert rootVertexPointers.isEmpty();
        for (VertInf t : terminals) {
            // This is a terminal, set a distance of zero.
            t.sptfDist = 0;
            rootVertexPointers.add(t.makeTreeRootPointer(t));
            vHeap.add(t);
        }

        // Make the vertex heap (min-heap by sptfDist).
        vHeap.sort(Comparator.comparingDouble(v -> v.sptfDist));

        // Shortest Path Terminal Forest construction
        while (!vHeap.isEmpty()) {
            // Take the lowest vertex from heap.
            VertInf u = vHeap.getFirst();

            // There should be no orphaned vertices.
            assert u.treeRoot() != null;
            assert u.pathNext != null || (u.sptfDist == 0);

            if (!beHeap.isEmpty() && u.sptfDist >= (0.5 * beHeap.getFirst().mtstDist())) {
                // Take the lowest cost edge.
                EdgeInf e = beHeap.getFirst();

                // Pop the lowest cost edge off of the heap.
                beHeap.removeFirst();
                rebuildBeHeap();

                commitToBridgingEdge(e);

                if (origTerminals.size() == 1) {
                    break;
                }

                removeInvalidBridgingEdges();
                rebuildBeHeap();

                // Don't pop this vertex, but continue.
                continue;
            }

            // Pop the lowest vertex off the heap.
            vHeap.removeFirst();
            rebuildVHeap();

            // For each edge from this vertex...
            List<Pair<EdgeInf, VertInf>> edgeList = getOrthogonalEdgesFromVertex(u, u.pathNext);
            for (Pair<EdgeInf, VertInf> edgePair : edgeList) {
                VertInf v = edgePair.second;
                EdgeInf e = edgePair.first;
                double edgeDist = e.getDist();

                // Assign a distance (length) of 1 for dummy visibility edges
                // which may not accurately reflect the real distance of the edge.
                if (v.id.isDummyPinHelper() || u.id.isDummyPinHelper()) {
                    edgeDist = 1;
                }

                // Don't do anything more here if this is an intra-tree edge that
                // would just bridge branches of the same tree.
                if (u.treeRoot() == v.treeRoot()) {
                    continue;
                }

                if (v.treeRoot() == null) {

                    // We have got to a node we haven't explored to from any tree.
                    v.sptfDist = (u.sptfDist + edgeDist);
                    v.pathNext = u;
                    v.setTreeRootPointer(u.treeRootPointer());
                    vHeap.add(v);
                    // This can change the cost of other vertices in the heap,
                    // so we need to remake it.
                    rebuildVHeap();
                } else {
                    // We have reached a node that has been reached already through
                    // a different tree. Set the MTST distance for the bridging
                    // edge and push it to the priority queue.
                    double cost = v.sptfDist + u.sptfDist + e.getDist();
                    boolean found = beHeap.contains(e);
                    if (!found) {
                        // We need to add the edge to the bridging edge heap.
                        e.setMtstDist(cost);
                        beHeap.add(e);
                        rebuildBeHeap();
                    } else {
                        // This edge is already in the bridging edge heap.
                        if (cost < e.mtstDist()) {
                            // Update the edge's mtstDist if we compute a lower
                            // cost than we had before.
                            e.setMtstDist(cost);
                            rebuildBeHeap();
                        }
                    }
                }
            }
        }
        assert origTerminals.size() == 1;

        // Free Root Vertex Points from all vertices.
        // In C++: free(*curr) — in Java, just clear the list (GC handles it).
        rootVertexPointers.clear();

        // Free the dummy nodes and edges created earlier.
        for (VertInf ptr : extraVertices) {
            ptr.removeFromGraph(false);
        }
        extraVertices.clear();
    }

    /**
     * Rewrites the tree root pointers for all vertices on the rest of a hyperedge path.
     * Translated from MinimumTerminalSpanningTree::rewriteRestOfHyperedge()
     * in mtst.cpp lines 886-911.
     */
    private void rewriteRestOfHyperedge(VertInf vert, VertInf[] newTreeRootPtr) {
        vert.setTreeRootPointer(newTreeRootPtr);

        List<Pair<EdgeInf, VertInf>> edgeList = getOrthogonalEdgesFromVertex(vert, null);
        for (Pair<EdgeInf, VertInf> edgePair : edgeList) {
            VertInf v = edgePair.second;

            if (v.treeRootPointer() == newTreeRootPtr) {
                // Already marked.
                continue;
            }

            if (v.sptfDist == 0) {
                // This is part of the rest of an existing hyperedge,
                // so mark it and continue.
                rewriteRestOfHyperedge(v, newTreeRootPtr);
            }
        }
    }

    /**
     * Returns the real vertices for an edge, counting orthogonal partners.
     * Translated from MinimumTerminalSpanningTree::realVerticesCountingPartners()
     * in mtst.cpp lines 964-988.
     */
    private VertInf[] realVerticesCountingPartners(EdgeInf edge) {
        VertInf v1 = edge.vert1();
        VertInf v2 = edge.vert2();

        VertInf[] realVertices = new VertInf[]{v1, v2};

        if (!(v1.id.vn == dimensionChangeVertexID.vn && v1.id.objID == dimensionChangeVertexID.objID) &&
                !(v2.id.vn == dimensionChangeVertexID.vn && v2.id.objID == dimensionChangeVertexID.objID) &&
                !v1.point.equals(v2.point) &&
                (v1.point.x == v2.point.x)) {
            if (v1.m_orthogonalPartner != null) {
                realVertices[0] = v1.m_orthogonalPartner;
            }
            if (v2.m_orthogonalPartner != null) {
                realVertices[1] = v2.m_orthogonalPartner;
            }
        }

        return realVertices;
    }

    /**
     * Commits to a bridging edge, building the hyperedge tree and pruning forests.
     * Translated from MinimumTerminalSpanningTree::commitToBridgingEdge()
     * in mtst.cpp lines 991-1092.
     */
    private void commitToBridgingEdge(EdgeInf e) {
        VertInf[] ends = realVerticesCountingPartners(e);
        VertInf treeRoot1 = ends[0].treeRoot();
        VertInf treeRoot2 = ends[1].treeRoot();
        // C++ uses std::min/std::max on raw pointers for a consistent ordering.
        // In Java, use VertID plus the stable vertex nodeId for a deterministic ordering.
        int cmp = treeRoot1.id.compareTo(treeRoot2.id);
        if (cmp == 0) {
            cmp = Integer.compare(treeRoot1.nodeId, treeRoot2.nodeId);
        }
        VertInf newRoot = (cmp <= 0) ? treeRoot1 : treeRoot2;
        VertInf oldRoot = (cmp <= 0) ? treeRoot2 : treeRoot1;

        // Connect this edge into the MTST by building HyperedgeTree nodes
        // and edges for this edge and the path back to the tree root.
        HyperedgeTreeNode node1 = null;
        HyperedgeTreeNode node2 = null;

        VertInf vert1 = ends[0];
        VertInf vert2 = ends[1];
        if (hyperedgeTreeJunctions != null) {
            node1 = addNode(vert1, null);
            node2 = addNode(vert2, node1);
        }

        buildHyperedgeTreeToRoot(vert1.pathNext, node1, vert1, true);
        buildHyperedgeTreeToRoot(vert2.pathNext, node2, vert2, true);

        // We are committing to a particular path and pruning back the shortest
        // path terminal forests from the roots of that path. We do this by
        // rewriting the treeRootPointers for all the points on the current
        // hyperedge path to newTreeRootPtr. The rest of the vertices in the
        // forest will be pruned by rewriting their treeRootPointer to nullptr.
        VertInf[] oldTreeRootPtr1 = vert1.treeRootPointer();
        VertInf[] oldTreeRootPtr2 = vert2.treeRootPointer();
        origTerminals.remove(oldRoot);
        VertInf[] newTreeRootPtr = vert1.makeTreeRootPointer(newRoot);
        rootVertexPointers.add(newTreeRootPtr);
        vert2.setTreeRootPointer(newTreeRootPtr);

        // Zero paths and rewrite the vertices on the hyperedge path to the
        // newTreeRootPtr. Also, add vertices on path to the terminal set.
        assert newRoot != null;
        resetDistsForPath(vert1, newTreeRootPtr);
        resetDistsForPath(vert2, newTreeRootPtr);

        // Prune the forests from the joined vertex sets by setting their
        // treeRootPointers to nullptr.
        assert oldTreeRootPtr1 != null;
        assert oldTreeRootPtr2 != null;
        oldTreeRootPtr1[0] = null;
        oldTreeRootPtr2[0] = null;

        // We have found the full hyperedge path when we have joined all the
        // terminal sets into one.
        if (origTerminals.size() == 1) {
            return;
        }

        // Remove newly orphaned vertices from vertex heap by only copying the
        // valid vertices to vHeapNew array which then replaces vHeap.
        List<VertInf> vHeapNew = new ArrayList<>(vHeap.size());
        for (VertInf v : vHeap) {
            if (v.treeRoot() == null) {
                // This is an orphaned vertex.
                continue;
            }
            // Copy the other vertices to vHeapNew.
            vHeapNew.add(v);
        }
        // Replace vHeap with vHeapNew
        vHeap = vHeapNew;

        // Reset all terminals to zero.
        for (VertInf v2 : terminals) {
            assert v2.sptfDist == 0;
            vHeap.add(v2);
        }

        // Rebuild the heap since some terminals will have had distances
        // rewritten as well as the orphaned vertices being removed.
        rebuildVHeap();
    }

    // ---- Heap helper methods ----
    // C++ uses std::make_heap/push_heap/pop_heap with custom comparators.
    // We maintain sorted lists as min-heaps (front = minimum).

    /**
     * Rebuilds vHeap as a min-heap sorted by sptfDist.
     */
    private void rebuildVHeap() {
        vHeap.sort(Comparator.comparingDouble(v -> v.sptfDist));
    }

    /**
     * Rebuilds beHeap as a min-heap sorted by mtstDist.
     */
    private void rebuildBeHeap() {
        beHeap.sort(Comparator.comparingDouble(EdgeInf::mtstDist));
    }
}
