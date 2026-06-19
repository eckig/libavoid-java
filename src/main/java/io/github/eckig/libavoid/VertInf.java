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
import java.util.List;

/**
 * Translated from vertices.h / vertices.cpp — VertInf class.
 * A vertex in the visibility graph. Part of an intrusive linked list (VertInfList).
 */
public class VertInf {

    // Orthogonal visibility property flags (static constants from vertices.h)
    public static final int XL_EDGE = 1;
    public static final int XL_CONN = 2;
    public static final int XH_EDGE = 4;
    public static final int XH_CONN = 8;
    public static final int YL_EDGE = 16;
    public static final int YL_CONN = 32;
    public static final int YH_EDGE = 64;
    public static final int YH_CONN = 128;

    // Dummy IDs for orthogonal graph vertices (from vertices.h)
    public static final VertID dummyOrthogID = new VertID(0, (short) 0);
    public static final VertID dummyOrthogShapeID = new VertID(0, (short) 0, VertID.PROP_OrthShapeEdge);

    // --- Fields matching C++ VertInf ---
    public Router _router;
    public VertID id;
    public Point point;
    public VertInf lstPrev;      // linked list prev (VertInfList)
    public VertInf lstNext;      // linked list next (VertInfList)
    public VertInf shPrev;       // shape prev (circular per-shape list)
    public VertInf shNext;       // shape next (circular per-shape list)
    public List<EdgeInf> visList;
    public int visListSize;
    public List<EdgeInf> orthogVisList;
    public int orthogVisListSize;
    public List<EdgeInf> invisList;
    public int invisListSize;
    public VertInf pathNext;

    // MTST tree root support. In C++ this is VertInf** (pointer to pointer).
    // In Java we use a single-element array to allow shared mutable references.
    public VertInf m_orthogonalPartner;
    VertInf[] m_treeRoot;   // null, or a 1-element array holding the root
    public double sptfDist;

    public int visDirections;   // ConnDirFlags
    // A* node lists — used by makepath
    public List<ANode> aStarDoneNodes;
    public List<ANode> aStarPendingNodes;
    // Flags for orthogonal visibility properties
    public int orthogVisPropFlags;

    // Unique ID for deterministic ordering (replaces pointer comparison in C++)
    private static int nextNodeId = 0;
    public final int nodeId;

    public VertInf(Router router, VertID vid, Point vpoint) {
        this(router, vid, vpoint, true);
    }

    public VertInf(Router router, VertID vid, Point vpoint, boolean addToRouter) {
        this.nodeId = nextNodeId++;
        this._router = router;
        this.id = new VertID(vid);
        this.point = new Point(vpoint);
        this.lstPrev = null;
        this.lstNext = null;
        this.shPrev = null;
        this.shNext = null;
        this.visList = new ArrayList<>();
        this.visListSize = 0;
        this.orthogVisList = new ArrayList<>();
        this.orthogVisListSize = 0;
        this.invisList = new ArrayList<>();
        this.invisListSize = 0;
        this.pathNext = null;
        this.m_orthogonalPartner = null;
        this.m_treeRoot = null;
        this.sptfDist = 0;
        this.visDirections = ConnDirFlag.ConnDirNone;
        this.aStarDoneNodes = new ArrayList<>();
        this.aStarPendingNodes = new ArrayList<>();
        this.orthogVisPropFlags = 0;

        point.id = vid.objID;
        point.vn = vid.vn;

        if (addToRouter) {
            router.vertices.addVertex(this);
        }
    }

    // C++ hasNeighbour
    public EdgeInf hasNeighbour(VertInf target, boolean orthogonal) {
        List<EdgeInf> visEdgeList = orthogonal ? orthogVisList : visList;
        for (EdgeInf edge : visEdgeList) {
            if (edge.otherVert(this) == target) {
                return edge;
            }
        }
        return null;
    }

    // C++ Reset(VertID, Point)
    public void reset(VertID vid, Point vpoint) {
        id = new VertID(vid);
        point = new Point(vpoint);
        point.id = id.objID;
        point.vn = id.vn;
    }

    // C++ Reset(Point)
    public void reset(Point vpoint) {
        point = new Point(vpoint);
        point.id = id.objID;
        point.vn = id.vn;
    }

    // C++ orphaned() — true if not involved in any vis/invis graphs
    public boolean orphaned() {
        return visList.isEmpty() && invisList.isEmpty() && orthogVisList.isEmpty();
    }

    // C++ removeFromGraph
    public void removeFromGraph(boolean isConnVert) {
        if (isConnVert) {
            assert id.isConnPt();
        }

        // Remove all visibility edges
        while (!visList.isEmpty()) {
            EdgeInf edge = visList.getFirst();
            edge.alertConns();
            edge.remove(); // C++: delete edge (removes from both endpoint lists)
        }

        // Remove all orthogonal visibility edges
        while (!orthogVisList.isEmpty()) {
            EdgeInf edge = orthogVisList.getFirst();
            edge.alertConns();
            edge.remove();
        }

        // Remove all invisibility edges
        while (!invisList.isEmpty()) {
            EdgeInf edge = invisList.getFirst();
            edge.remove();
        }
    }

    public void removeFromGraph() {
        removeFromGraph(true);
    }

    // C++ directionFrom
    public int directionFrom(VertInf other) {
        double epsilon = 0.000001;
        Point diff = point.subtract(other.point);

        int directions = ConnDirFlag.ConnDirNone;
        if (diff.y > epsilon) {
            directions |= ConnDirFlag.ConnDirUp;
        }
        if (diff.y < -epsilon) {
            directions |= ConnDirFlag.ConnDirDown;
        }
        if (diff.x > epsilon) {
            directions |= ConnDirFlag.ConnDirRight;
        }
        if (diff.x < -epsilon) {
            directions |= ConnDirFlag.ConnDirLeft;
        }
        return directions;
    }

    // C++ setVisibleDirections
    public void setVisibleDirections(int directions) {
        for (EdgeInf edge : visList) {
            if (directions == ConnDirFlag.ConnDirAll) {
                edge.setDisabled(false);
            } else {
                VertInf otherVert = edge.otherVert(this);
                int direction = otherVert.directionFrom(this);
                boolean visible = (direction & directions) != 0;
                edge.setDisabled(!visible);
            }
        }

        for (EdgeInf edge : orthogVisList) {
            if (directions == ConnDirFlag.ConnDirAll) {
                edge.setDisabled(false);
            } else {
                VertInf otherVert = edge.otherVert(this);
                int direction = otherVert.directionFrom(this);
                boolean visible = (direction & directions) != 0;
                edge.setDisabled(!visible);
            }
        }
    }

    // C++ pathLeadsBackTo — number of points in path from this back to start, or 0
    public int pathLeadsBackTo(VertInf start) {
        int pathlen = 1;
        for (VertInf i = this; i != start; i = i.pathNext) {
            if (pathlen > 1 && i == this) {
                // Circular path — not found
                return 0;
            }
            pathlen++;
            if (i == null) {
                // Path not found
                return 0;
            }
            assert pathlen < 20000 : "Apparent infinite connector path";
        }
        return pathlen;
    }

    // --- Tree root pointer support (for MTST) ---
    // C++ uses VertInf** (pointer to pointer). In Java we share a 1-element array.
    public VertInf[] makeTreeRootPointer(VertInf root) {
        m_treeRoot = new VertInf[]{root};
        return m_treeRoot;
    }

    public VertInf treeRoot() {
        return (m_treeRoot != null) ? m_treeRoot[0] : null;
    }

    public VertInf[] treeRootPointer() {
        return m_treeRoot;
    }

    public void setTreeRootPointer(VertInf[] pointer) {
        m_treeRoot = pointer;
    }

    @Override
    public String toString() {
        return "VertInf{" + id + " @ " + point + "}";
    }
}
