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
import java.util.List;
import java.util.PriorityQueue;

import static io.github.eckig.libavoid.Point.XDIM;
import static io.github.eckig.libavoid.Point.YDIM;

/**
 * Translated from makepath.h / makepath.cpp.
 * A* pathfinding on the visibility graph.
 */
public class MakePath {

    // Directions for estimated orthogonal cost, as bitflags.
    private static final int CostDirectionN = 1;
    private static final int CostDirectionE = 2;
    private static final int CostDirectionS = 4;
    private static final int CostDirectionW = 8;

    // ANode comparator for priority queue (min-heap by f value)
    static final Comparator<ANode> ANODE_CMP = (a, b) -> {
        if (Math.abs(a.f - b.f) > 0.0000001) {
            return Double.compare(a.f, b.f);
        }
        if (a.timeStamp != b.timeStamp) {
            // Higher timestamp preferred (forward direction)
            return Integer.compare(b.timeStamp, a.timeStamp);
        }
        return 0;
    };

    // --- Static helpers ---

    private static double dot(Point l, Point r) {
        return (l.x * r.x) + (l.y * r.y);
    }

    private static double crossLength(Point l, Point r) {
        return (l.x * r.y) - (l.y * r.x);
    }

    private static double angleBetween(Point p1, Point p2, Point p3) {
        if ((p1.x == p2.x && p1.y == p2.y) || (p2.x == p3.x && p2.y == p3.y)) {
            return Math.PI;
        }
        Point v1 = new Point(p1.x - p2.x, p1.y - p2.y);
        Point v2 = new Point(p3.x - p2.x, p3.y - p2.y);
        return Math.abs(Math.atan2(crossLength(v1, v2), dot(v1, v2)));
    }

    private static int dimDirection(double difference) {
        if (difference > 0) return 1;
        else if (difference < 0) return -1;
        return 0;
    }

    static int orthogonalDirection(Point a, Point b) {
        int result = 0;
        if (b.y > a.y) result |= CostDirectionS;
        else if (b.y < a.y) result |= CostDirectionN;
        if (b.x > a.x) result |= CostDirectionE;
        else if (b.x < a.x) result |= CostDirectionW;
        return result;
    }

    static int orthogonalDirectionsCount(int directions) {
        int count = 0;
        if ((directions & CostDirectionN) != 0) ++count;
        if ((directions & CostDirectionE) != 0) ++count;
        if ((directions & CostDirectionS) != 0) ++count;
        if ((directions & CostDirectionW) != 0) ++count;
        return count;
    }

    static int dirRight(int direction) {
        if (direction == CostDirectionN) return CostDirectionE;
        if (direction == CostDirectionE) return CostDirectionS;
        if (direction == CostDirectionS) return CostDirectionW;
        if (direction == CostDirectionW) return CostDirectionN;
        return direction;
    }

    static int dirLeft(int direction) {
        if (direction == CostDirectionN) return CostDirectionW;
        if (direction == CostDirectionE) return CostDirectionN;
        if (direction == CostDirectionS) return CostDirectionE;
        if (direction == CostDirectionW) return CostDirectionS;
        return direction;
    }

    static int dirReverse(int direction) {
        if (direction == CostDirectionN) return CostDirectionS;
        if (direction == CostDirectionE) return CostDirectionW;
        if (direction == CostDirectionS) return CostDirectionN;
        if (direction == CostDirectionW) return CostDirectionE;
        return direction;
    }

    // C++ bends() function
    static int bends(Point curr, int currDir, Point dest, int destDir) {
        int currToDestDir = orthogonalDirection(curr, dest);
        int reverseDestDir = dirReverse(destDir);
        boolean currDirPerpendicularToDestDir =
                (currDir == dirLeft(destDir)) || (currDir == dirRight(destDir));

        if ((currDir == destDir) && (currToDestDir == currDir)) return 0;
        if (currDirPerpendicularToDestDir && (currToDestDir == (destDir | currDir))) return 1;
        if (currDirPerpendicularToDestDir && (currToDestDir == currDir)) return 1;
        if (currDirPerpendicularToDestDir && (currToDestDir == destDir)) return 1;
        if (currDir == destDir && (currToDestDir & reverseDestDir) == 0) return 2;
        if ((currDir == reverseDestDir) && (currToDestDir != destDir) && (currToDestDir != currDir)) return 2;
        if (currDirPerpendicularToDestDir && currToDestDir != (destDir | currDir)) return 3;
        if (currDir == reverseDestDir) return 4;
        if ((currDir == destDir) && ((currToDestDir & reverseDestDir) != 0)) return 4;
        return 0;
    }

    // C++ estimatedCostSpecific
    private static double estimatedCostSpecific(ConnRef lineRef, Point last,
            Point curr, VertInf costTar, int costTarDirs) {
        Point costTarPoint = costTar.point;

        if (lineRef.routingType() != ConnType.Orthogonal) {
            return Geometry.euclideanDist(curr, costTarPoint);
        }

        double dist = Geometry.manhattanDist(curr, costTarPoint);
        int bendCount = 0;
        double xmove = costTarPoint.x - curr.x;
        double ymove = costTarPoint.y - curr.y;

        if (last == null) {
            if (xmove != 0 && ymove != 0) {
                bendCount += 1;
            }
        } else if (dist > 0) {
            int currDir = orthogonalDirection(last, curr);
            if (currDir > 0 && orthogonalDirectionsCount(currDir) == 1) {
                bendCount = 10;
                if ((costTarDirs & CostDirectionN) != 0) {
                    bendCount = Math.min(bendCount, bends(curr, currDir, costTarPoint, CostDirectionN));
                }
                if ((costTarDirs & CostDirectionE) != 0) {
                    bendCount = Math.min(bendCount, bends(curr, currDir, costTarPoint, CostDirectionE));
                }
                if ((costTarDirs & CostDirectionS) != 0) {
                    bendCount = Math.min(bendCount, bends(curr, currDir, costTarPoint, CostDirectionS));
                }
                if ((costTarDirs & CostDirectionW) != 0) {
                    bendCount = Math.min(bendCount, bends(curr, currDir, costTarPoint, CostDirectionW));
                }
            }
        }
        double penalty = bendCount * lineRef.router().routingParameter(Router.RoutingParameter.segmentPenalty);
        return dist + penalty;
    }

    // C++ cost() function
    static double cost(ConnRef lineRef, double dist, VertInf inf2,
            VertInf inf3, ANode inf1Node) {
        boolean isOrthogonal = (lineRef.routingType() == ConnType.Orthogonal);
        VertInf inf1 = (inf1Node != null) ? inf1Node.inf : null;
        double result = dist;
        Polygon connRoute = new Polygon();

        Router router = inf2._router;
        if (inf1 != null) {
            double angle_penalty = router.routingParameter(Router.RoutingParameter.anglePenalty);
            double segmt_penalty = router.routingParameter(Router.RoutingParameter.segmentPenalty);

            if (angle_penalty > 0 || segmt_penalty > 0) {
                Point p1 = inf1.point;
                Point p2 = inf2.point;
                Point p3 = inf3.point;

                double rad = Math.PI - angleBetween(p1, p2, p3);

                if (rad > 0 && !isOrthogonal) {
                    double xval = rad * 10 / Math.PI;
                    double yval = xval * Math.log10(xval + 1) / 10.5;
                    result += (angle_penalty * yval);
                }

                if (rad == Math.PI) {
                    result += (2 * segmt_penalty);
                } else if (rad > 0) {
                    result += segmt_penalty;
                }
            }
        }

        // This penalty penalises route segments that head in a direction opposite
        // of the direction(s) toward the target point.
        double reversePenalty = router.routingParameter(Router.RoutingParameter.reverseDirectionPenalty);
        if (reversePenalty > 0) {
            Point srcPoint = lineRef.src().point;
            Point dstPoint = lineRef.dst().point;
            int xDir = dimDirection(dstPoint.x - srcPoint.x);
            int yDir = dimDirection(dstPoint.y - srcPoint.y);

            boolean doesReverse = xDir != 0 && -xDir == dimDirection(inf3.point.x - inf2.point.x);
            if (yDir != 0 && -yDir == dimDirection(inf3.point.y - inf2.point.y)) {
                doesReverse = true;
            }
            if (doesReverse) {
                result += reversePenalty;
            }
        }

        if (!router.isInCrossingPenaltyReroutingStage()) {
            // Return here if we are not in the post-processing stage
            return result;
        }

        double crossing_penalty = router.routingParameter(Router.RoutingParameter.crossingPenalty);
        double shared_path_penalty = router.routingParameter(Router.RoutingParameter.fixedSharedPathPenalty);
        if (shared_path_penalty > 0 || crossing_penalty > 0) {
            if (connRoute.empty()) {
                constructPolygonPath(connRoute, inf2, inf3, inf1Node);
            }
            if (connRoute.empty()) {
                return result;
            }

            for (ConnRef connRef : router.m_connectors) {
                if (connRef.id() == lineRef.id()) {
                    continue;
                }
                Polygon route2 = connRef.displayRoute();
                if (route2.empty()) {
                    continue;
                }

                boolean isConn = true;
                Polygon dynamic_route2 = new Polygon(route2);
                Polygon dynamic_conn_route = new Polygon(connRoute);
                boolean finalSegment = (inf3.point.equals(lineRef.dst().point));
                ConnectorCrossings cross = new ConnectorCrossings(dynamic_route2, isConn,
                        dynamic_conn_route, connRef, lineRef);
                cross.checkForBranchingSegments = true;
                cross.countForSegment(connRoute.size() - 1, finalSegment);

                if ((cross.crossingFlags & ConnectorCrossings.CROSSING_SHARES_PATH) != 0 &&
                        (cross.crossingFlags & ConnectorCrossings.CROSSING_SHARES_FIXED_SEGMENT) != 0 &&
                        (router.routingOption(Router.RoutingOption.penaliseOrthogonalSharedPathsAtConnEnds) ||
                                (cross.crossingFlags & ConnectorCrossings.CROSSING_SHARES_PATH_AT_END) == 0)) {
                    // Penalise unnecessary shared paths in the middle of connectors.
                    result += shared_path_penalty;
                }
                result += (cross.crossingCount * crossing_penalty);
            }
        }

        return result;
    }

    // C++ constructPolygonPath
    static void constructPolygonPath(Polygon connRoute, VertInf inf2,
            VertInf inf3, ANode inf1Node) {
        boolean simplified = true;

        int routeSize = 2;
        for (ANode curr = inf1Node; curr != null; curr = curr.prevNode) {
            routeSize += 1;
        }

        // Resize ps to routeSize
        connRoute.ps.clear();
        for (int i = 0; i < routeSize; i++) {
            connRoute.ps.add(new Point(0, 0));
        }
        int arraySize = routeSize;
        connRoute.ps.set(routeSize - 1, inf3.point);
        connRoute.ps.set(routeSize - 2, inf2.point);
        routeSize -= 3;

        for (ANode curr = inf1Node; curr != null; curr = curr.prevNode) {
            boolean isConnectionPin = curr.inf.id.isConnectionPin();

            if (!simplified) {
                connRoute.ps.set(routeSize, curr.inf.point);
                routeSize -= 1;
                if (isConnectionPin) break;
                continue;
            }

            if (curr == inf1Node ||
                    Geometry.vecDir(curr.inf.point, connRoute.ps.get(routeSize + 1),
                            connRoute.ps.get(routeSize + 2)) != 0) {
                connRoute.ps.set(routeSize, curr.inf.point);
                routeSize -= 1;
            } else {
                connRoute.ps.set(routeSize + 1, curr.inf.point);
            }

            if (isConnectionPin) break;
        }

        int diff = routeSize + 1;
        if (diff > 0) {
            for (int i = diff; i < arraySize; ++i) {
                connRoute.ps.set(i - diff, connRoute.ps.get(i));
            }
            int newSize = arraySize - diff;
            while (connRoute.ps.size() > newSize) {
                connRoute.ps.removeLast();
            }
        }
    }

    private static boolean pointAlignedWithOneOf(Point point, List<Point> points, int dim) {
        for (Point p : points) {
            if (point.get(dim) == p.get(dim)) {
                return true;
            }
        }
        return false;
    }

    // --- Main A* search ---
    public static void search(ConnRef lineRef, VertInf src, VertInf tar, VertInf start) {
        boolean isOrthogonal = (lineRef.routingType() == ConnType.Orthogonal);

        if (start == null) {
            start = src;
        }

        // Determine cost targets for heuristic
        List<VertInf> costTargets = new ArrayList<>();
        List<Integer> costTargetsDirections = new ArrayList<>();
        List<Double> costTargetsDisplacements = new ArrayList<>();

        if (isOrthogonal && tar.id.isConnPt() && !tar.id.isConnCheckpoint()) {
            for (EdgeInf edge : tar.orthogVisList) {
                VertInf other = edge.otherVert(tar);
                if (other.id.isConnectionPin()) {
                    for (EdgeInf edge2 : other.orthogVisList) {
                        VertInf other2 = edge2.otherVert(other);
                        if (other2 == tar || other2.point.equals(tar.point)) continue;
                        int thisDirs = orthogonalDirection(other2.point, other.point);
                        double displacement = Geometry.manhattanDist(other2.point, other.point);
                        costTargets.add(other2);
                        costTargetsDirections.add(thisDirs);
                        costTargetsDisplacements.add(displacement);
                    }
                    continue;
                }
                int thisDirs = orthogonalDirection(other.point, tar.point);
                double displacement = Geometry.manhattanDist(other.point, tar.point);
                costTargets.add(other);
                costTargetsDirections.add(thisDirs);
                costTargetsDisplacements.add(displacement);
            }
        }

        if (costTargets.isEmpty()) {
            costTargets.add(tar);
            costTargetsDirections.add(CostDirectionN | CostDirectionE | CostDirectionS | CostDirectionW);
            costTargetsDisplacements.add(0.0);
        }

        // Possible endpoints for orthogonal routing optimization
        List<Point> endPoints = new ArrayList<>();
        if (isOrthogonal) {
            endPoints.addAll(lineRef.possibleDstPinPoints());
        }
        endPoints.add(tar.point);

        // Heap of pending nodes — we use lazy deletion to avoid O(N) PriorityQueue.remove().
        // Nodes with a stale generation (< the generation recorded on the vertex) are skipped.
        PriorityQueue<ANode> pending = new PriorityQueue<>(1000, ANODE_CMP);

        // Dirty-vertex list: only vertices actually touched during this search need cleanup.
        List<VertInf> dirtyVertices = new ArrayList<>();

        ANode bestNode = null;
        int timestamp = 1;

        Router router = lineRef.router();
        // C++ makepath.cpp:1084-1175
        if (router.RubberBandRouting && (start != src)) {
            // Walk the existing connector route, creating ANodes along the
            // path to bootstrap the A* search.
            assert router.IgnoreRegions;

            Polygon currRoute = lineRef.route();
            VertInf last = null;
            int rIndx = 0;
            while (last != start) {
                Point pnt = currRoute.at(rIndx);
                short props = (rIndx > 0) ? 0 : VertID.PROP_ConnPoint;
                VertID vID = new VertID(pnt.id, pnt.vn, props);

                VertInf curr = router.vertices.getVertexByID(vID);
                assert curr != null;

                ANode node = new ANode(curr, timestamp++);
                if (last == null) {
                    node.inf = src;
                    node.g = 0;
                    node.h = estimatedCost(lineRef, null, node.inf.point,
                            costTargets, costTargetsDirections, costTargetsDisplacements);
                    node.f = node.g + node.h;
                } else {
                    double edgeDist = isOrthogonal
                            ? Geometry.manhattanDist(bestNode.inf.point, curr.point)
                            : Geometry.euclideanDist(bestNode.inf.point, curr.point);

                    node.g = bestNode.g + cost(lineRef, edgeDist, bestNode.inf,
                            node.inf, bestNode.prevNode);

                    // Calculate the Heuristic.
                    node.h = estimatedCost(lineRef, bestNode.inf.point,
                            node.inf.point, costTargets, costTargetsDirections,
                            costTargetsDisplacements);

                    // The A* formula
                    node.f = node.g + node.h;

                    // Point parent to last bestNode
                    node.prevNode = bestNode;
                }

                if (curr != start) {
                    bestNode = node;
                    bestNode.inf.aStarDoneNodes.add(bestNode);
                } else {
                    pending.add(node);
                    node.inf.aStarPendingNodes.add(node);
                }

                rIndx++;
                last = curr;
            }
        } else {
            // Handle checkpoint routing: if start already has pathNext
            if (start.pathNext != null) {
                bestNode = new ANode(start.pathNext, timestamp++);
                bestNode.inf.aStarDoneNodes.add(bestNode);
            }

            // Create start node
            ANode startNode = new ANode(src, timestamp++);
            startNode.g = 0;
            startNode.h = estimatedCost(lineRef, null, src.point, costTargets, costTargetsDirections, costTargetsDisplacements);
            startNode.f = startNode.g + startNode.h;
            startNode.prevNode = bestNode;

            pending.add(startNode);
            src.aStarPendingNodes.add(startNode);
        }

        tar.pathNext = null;

        // Each vertex gets an aStarGeneration counter. When we "add" a node to the
        // done set we increment the generation on the vertex. Stale nodes popped from
        // the heap (generation mismatch) are simply skipped — this replaces the O(N)
        // pending.remove() calls with O(log N) heap operations (lazy deletion).
        //
        // aStarBestG tracks the best known g-cost for a vertex so we can decide
        // whether a newly discovered path is an improvement without scanning lists.

        while (!pending.isEmpty()) {
            bestNode = pending.poll();
            VertInf bestNodeInf = bestNode.inf;

            // Lazy-deletion: skip nodes that have already been superseded.
            if (bestNode.timeStamp < bestNodeInf.aStarSettledTimestamp) {
                continue;
            }

            // Mark this vertex as settled (done).
            bestNodeInf.aStarSettledTimestamp = bestNode.timeStamp;
            bestNodeInf.aStarDoneNodes.add(bestNode);
            // Track vertices we touch so we only clean those up later.
            if (!bestNodeInf.aStarVisited) {
                bestNodeInf.aStarVisited = true;
                dirtyVertices.add(bestNodeInf);
            }

            if (bestNodeInf == tar) {
                // Found the target! Set up pathNext pointers.
                for (ANode curr = bestNode; curr.prevNode != null; curr = curr.prevNode) {
                    curr.inf.pathNext = curr.prevNode.inf;
                }
                break;
            }

            VertInf prevInf = (bestNode.prevNode != null) ? bestNode.prevNode.inf : null;

            // Check adjacent points via visibility edges.
            // For orthogonal routing the edge order matters for tie-breaking, but
            // sorting on EVERY expansion is very expensive. We sort once here per
            // expansion (unavoidable for correctness of tie-breaking), but we avoid
            // allocating a new ArrayList by reusing a field on ANode in future;
            // for now the allocation is kept but the sort is the same as before.
            List<EdgeInf> visList = isOrthogonal ?
                    bestNodeInf.orthogVisList : bestNodeInf.visList;

            if (isOrthogonal) {
                final VertInf lastV = prevInf;
                ArrayList<EdgeInf> sortedEdges = new ArrayList<>(visList);
                sortedEdges.sort((u, v) -> {
                    if (u.isOrthogonal() && v.isOrthogonal()) {
                        return u.rotationLessThan(lastV, v) ? -1 :
                               (v.rotationLessThan(lastV, u) ? 1 : 0);
                    }
                    int uFirst = Math.min(u.vert1().nodeId, u.vert2().nodeId);
                    int vFirst = Math.min(v.vert1().nodeId, v.vert2().nodeId);
                    if (uFirst != vFirst) return Integer.compare(uFirst, vFirst);
                    int uSecond = Math.max(u.vert1().nodeId, u.vert2().nodeId);
                    int vSecond = Math.max(v.vert1().nodeId, v.vert2().nodeId);
                    return Integer.compare(uSecond, vSecond);
                });
                visList = sortedEdges;
            }

            for (EdgeInf edge : visList) {

                VertInf nodeInf = edge.otherVert(bestNodeInf);

                VertInf prevInf2 = (bestNode.prevNode != null) ? bestNode.prevNode.inf : null;

                // Don't look back along the segment we came from.
                if (prevInf2 != null && prevInf2 == nodeInf) continue;

                // Skip connection pins unless connected to target or source.
                if (nodeInf.id.isConnectionPin() && !nodeInf.id.isConnCheckpoint()) {
                    if (!((bestNodeInf == lineRef.src()) && lineRef.src().id.isDummyPinHelper()) &&
                        !(nodeInf.hasNeighbour(lineRef.dst(), isOrthogonal) != null &&
                          lineRef.dst().id.isDummyPinHelper())) {
                        continue;
                    }
                } else if (nodeInf.id.isConnPt()) {
                    if (nodeInf != tar) continue;
                }

                // Orthogonal routing optimisation: skip turns that don't lead
                // to shape edges or target alignment.
                if (isOrthogonal && !edge.isDummyConnection()) {
                    Point bestPt = bestNodeInf.point;
                    Point nextPt = nodeInf.point;

                    boolean notInlineX = prevInf2 != null && (prevInf2.point.x != bestPt.x);
                    boolean notInlineY = prevInf2 != null && (prevInf2.point.y != bestPt.y);
                    if (bestPt.x == nextPt.x && notInlineX && !notInlineY &&
                            bestPt.get(YDIM) != src.point.get(YDIM)) {
                        if (nextPt.y < bestPt.y) {
                            if ((bestNodeInf.orthogVisPropFlags & VertInf.YL_EDGE) == 0 &&
                                    !pointAlignedWithOneOf(bestPt, endPoints, XDIM)) {
                                continue;
                            }
                        } else if (nextPt.y > bestPt.y) {
                            if ((bestNodeInf.orthogVisPropFlags & VertInf.YH_EDGE) == 0 &&
                                    !pointAlignedWithOneOf(bestPt, endPoints, XDIM)) {
                                continue;
                            }
                        }
                    }
                    if (bestPt.y == nextPt.y && notInlineY && !notInlineX &&
                            bestPt.get(XDIM) != src.point.get(XDIM)) {
                        if (nextPt.x < bestPt.x) {
                            if ((bestNodeInf.orthogVisPropFlags & VertInf.XL_EDGE) == 0 &&
                                    !pointAlignedWithOneOf(bestPt, endPoints, YDIM)) {
                                continue;
                            }
                        } else if (nextPt.x > bestPt.x) {
                            if ((bestNodeInf.orthogVisPropFlags & VertInf.XH_EDGE) == 0 &&
                                    !pointAlignedWithOneOf(bestPt, endPoints, YDIM)) {
                                continue;
                            }
                        }
                    }
                }

                double edgeDist = edge.getDist();
                if (edgeDist == 0) continue;

                // C++ makepath.cpp:1397-1406 — skip invalid bend points for polyline routing.
                if (!isOrthogonal &&
                        (!lineRef.router().RubberBandRouting || (start == src)) &&
                        !ConnRef.validateBendPoint(prevInf, bestNodeInf, nodeInf)) {
                    continue;
                }

                // Check if at a cost target.
                boolean atCostTarget = false;
                for (VertInf ct : costTargets) {
                    if (bestNode.inf == ct) {
                        atCostTarget = true;
                        break;
                    }
                }

                double nodeG;
                double nodeH;
                if (atCostTarget && (nodeInf.id.isConnectionPin() || nodeInf == tar)) {
                    nodeG = bestNode.g;
                    nodeH = 0;
                } else {
                    nodeH = (nodeInf == tar) ? 0 :
                            estimatedCost(lineRef, bestNodeInf.point, nodeInf.point,
                                    costTargets, costTargetsDirections, costTargetsDisplacements);
                    nodeG = nodeInf.id.isDummyPinHelper() ? bestNode.g :
                            bestNode.g + cost(lineRef, edgeDist, bestNodeInf,
                                    nodeInf, bestNode.prevNode);
                }
                double nodeF = nodeG + nodeH;

                // Lazy-deletion approach: only add a new node if it improves upon
                // the best g-cost we have seen for this vertex so far.
                // We skip the old O(N) pending-list scan entirely.
                //
                // For correctness with the parent-based duplicate check from the
                // original code we keep aStarPendingNodes as a small per-vertex list,
                // but we only scan it to decide whether to add (not to remove).
                boolean bNodeFound = false;
                for (ANode pendingNode : nodeInf.aStarPendingNodes) {
                    if ((pendingNode.prevNode == bestNode) ||
                            (pendingNode.prevNode != null &&
                             pendingNode.prevNode.inf == bestNodeInf)) {
                        if (nodeG < pendingNode.g) {
                            // Instead of removing the old node from the heap (O(N)),
                            // we update it in-place and re-insert. The old entry is
                            // now stale — it will be skipped by lazy-deletion when popped.
                            pendingNode.g = nodeG;
                            pendingNode.h = nodeH;
                            pendingNode.f = nodeF;
                            pendingNode.prevNode = bestNode;
                            pendingNode.timeStamp = timestamp++;
                            // Increment the settled-timestamp threshold so any earlier
                            // copy already in the heap gets skipped on pop.
                            nodeInf.aStarSettledTimestamp = pendingNode.timeStamp - 1;
                            pending.add(pendingNode);
                        }
                        bNodeFound = true;
                        break;
                    }
                }

                if (!bNodeFound) {
                    // Check done set (same parent check as original).
                    for (ANode doneNode : nodeInf.aStarDoneNodes) {
                        if (doneNode.prevNode != null &&
                                ((bestNode == doneNode.prevNode) ||
                                 (bestNode.prevNode != null &&
                                  bestNode.prevNode.inf == doneNode.prevNode.inf))) {
                            bNodeFound = true;
                            break;
                        }
                    }
                }

                if (!bNodeFound) {
                    ANode node = new ANode(nodeInf, timestamp++);
                    node.prevNode = bestNode;
                    node.g = nodeG;
                    node.h = nodeH;
                    node.f = nodeF;
                    pending.add(node);
                    nodeInf.aStarPendingNodes.add(node);
                    if (!nodeInf.aStarVisited) {
                        nodeInf.aStarVisited = true;
                        dirtyVertices.add(nodeInf);
                    }
                }
            }
        }

        // Cleanup: only clear the vertices we actually visited (dirty list),
        // instead of walking the entire vertex linked-list.
        for (VertInf k : dirtyVertices) {
            k.aStarDoneNodes.clear();
            k.aStarPendingNodes.clear();
            k.aStarVisited = false;
            k.aStarSettledTimestamp = 0;
        }
    }

    // Estimated cost using all cost targets
    private static double estimatedCost(ConnRef lineRef, Point last, Point curr,
            List<VertInf> costTargets, List<Integer> costTargetsDirections,
            List<Double> costTargetsDisplacements) {
        double estimate = Double.MAX_VALUE;
        for (int i = 0; i < costTargets.size(); ++i) {
            double iEstimate = estimatedCostSpecific(lineRef, last, curr,
                    costTargets.get(i), costTargetsDirections.get(i));
            iEstimate += costTargetsDisplacements.get(i);
            estimate = Math.min(estimate, iEstimate);
        }
        return estimate;
    }
}
