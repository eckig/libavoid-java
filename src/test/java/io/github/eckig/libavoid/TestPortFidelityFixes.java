package io.github.eckig.libavoid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Regression tests for deviations from the C++ original found in an audit.
 */
class TestPortFidelityFixes {

    /** A given shape ID is recorded, so automatically assigned IDs never repeat it (C++ Router::assignId). */
    @Test
    void automaticIdsDoNotRepeatGivenId() {
        final Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        final Set<Integer> ids = new HashSet<>();
        ids.add(new ShapeRef(router, new Rectangle(new Point(0, 0), new Point(10, 10)), 5).id());
        for (int i = 0; i < 6; i++) {
            final int id = new ShapeRef(router, new Rectangle(new Point(20 * i, 50), new Point(20 * i + 10, 60))).id();
            assertTrue(ids.add(id), "duplicate id " + id);
        }
    }

    /** A relative move only takes effect when the transaction is processed (C++ copies the polygon). */
    @Test
    void relativeMoveDoesNotChangeShapeBeforeTransaction() {
        final Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setTransactionUse(true);
        final ShapeRef shape = new ShapeRef(router, new Rectangle(new Point(0, 0), new Point(10, 10)));
        router.processTransaction();

        router.moveShape(shape, 100, 0);
        assertTrue(shape.polygon().at(0).x < 50, "shape moved before processTransaction");

        router.processTransaction();
        for (int i = 0; i < shape.polygon().size(); i++) {
            assertTrue(shape.polygon().at(i).x >= 100, "shape not moved after processTransaction");
        }
    }

    /**
     * A* search: alternative parents of a vertex must not be discarded (C++ makepath.cpp keeps one node per parent
     * and checks the done set against the current vertex). Before the fix this connector got 6 bends instead of 4.
     */
    @Test
    void astarFindsRouteWithFewestBends() {
        final Random r = new Random(19);
        final Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setTransactionUse(true);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 1000);
        router.setRoutingParameter(Router.RoutingParameter.shapeBufferDistance, 15);
        router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 5);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalTouchingColinearSegments, true);
        router.setRoutingOption(Router.RoutingOption.performUnifyingNudgingPreprocessingStep, true);
        router.setRoutingOption(Router.RoutingOption.nudgeSharedPathsWithCommonEndPoint, true);
        router.setRoutingOption(Router.RoutingOption.nudgeFinalSegmentsFromSamePoint, false);

        final List<double[]> boxes = new ArrayList<>();
        outer:
        for (int i = 0; i < 120 && boxes.size() < 40; i++) {
            final double x = r.nextInt(30) * 40, y = r.nextInt(30) * 40, w = 60 + r.nextInt(4) * 20, h = 40 + r.nextInt(3) * 20;
            for (double[] o : boxes) {
                if (x < o[0] + o[2] + 30 && o[0] < x + w + 30 && y < o[1] + o[3] + 30 && o[1] < y + h + 30) {
                    continue outer;
                }
            }
            boxes.add(new double[] {x, y, w, h});
            new ShapeRef(router, new Rectangle(new Point(x, y), new Point(x + w, y + h)));
        }
        final List<ConnRef> connectors = new ArrayList<>();
        for (int k = 0; k < 60; k++) {
            final double[] s = boxes.get(r.nextInt(boxes.size()));
            final double[] d = boxes.get(r.nextInt(boxes.size()));
            if (s == d) {
                continue;
            }
            final ConnRef c = new ConnRef(router);
            c.setSourceEndpoint(end(s, r.nextInt(4)));
            c.setDestEndpoint(end(d, r.nextInt(4)));
            connectors.add(c);
        }
        router.processTransaction();

        final int bends = connectors.get(32).displayRoute().size() - 2;
        assertTrue(bends <= 4, "bends: " + bends);
    }

    /** Deleting a shape destroys its connection pins, so their vertices leave the graph (C++ ~Obstacle). */
    @Test
    void deletingShapeRemovesItsPins() {
        final Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setTransactionUse(true);
        final int before = router.vertices.connsSize();
        final ShapeRef shape = new ShapeRef(router, new Rectangle(new Point(0, 0), new Point(100, 100)));
        new ShapeConnectionPin(shape, 1, ShapeConnectionPin.ATTACH_POS_CENTRE, 0, true, 0, ConnDirFlag.ConnDirUp);
        new ShapeConnectionPin(shape, 1, 1, ShapeConnectionPin.ATTACH_POS_CENTRE, true, 0, ConnDirFlag.ConnDirRight);
        router.processTransaction();
        assertEquals(before + 2, router.vertices.connsSize(), "pin vertices");

        router.deleteShape(shape);
        router.processTransaction();
        assertEquals(before, router.vertices.connsSize(), "pin vertices left after deleting the shape");
    }

    /** A shape added and deleted in the same transaction never becomes part of the scene. */
    @Test
    void addThenDeleteInSameTransactionDoesNotResurrect() {
        final Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setTransactionUse(true);
        final ShapeRef shape = new ShapeRef(router, new Rectangle(new Point(0, 0), new Point(10, 10)));
        router.deleteShape(shape);
        router.processTransaction();

        assertFalse(router.m_obstacles.contains(shape));
    }

    /** Moving a shape deleted in the same transaction is a programming error (C++ asserts). */
    @Test
    void moveAfterDeleteIsRejected() {
        final Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setTransactionUse(true);
        final ShapeRef shape = new ShapeRef(router, new Rectangle(new Point(0, 0), new Point(10, 10)));
        router.processTransaction();
        router.deleteShape(shape);

        assertThrows(IllegalStateException.class, () -> router.moveShape(shape, 5, 5));
    }

    /** Deleted connectors are not kept reachable by the router. */
    @Test
    void deletedConnectorIsReleased() {
        final Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        final ConnRef conn = new ConnRef(router, new ConnEnd(new Point(0, 0)), new ConnEnd(new Point(100, 100)));
        router.processTransaction();
        assertTrue(router.m_conn_reroute_flags.contains(conn));

        router.deleteConnector(conn);
        router.processTransaction();
        assertFalse(router.m_conn_reroute_flags.contains(conn), "deleted connector still referenced");
    }

    /** Cancelling an add also removes the vertices its pins (and a junction's centre pin) already created. */
    @Test
    void addThenDeleteLeavesNoPinVertices() {
        final Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setTransactionUse(true);
        final int before = router.vertices.connsSize();

        final ShapeRef shape = new ShapeRef(router, new Rectangle(new Point(0, 0), new Point(100, 100)));
        new ShapeConnectionPin(shape, 1, ShapeConnectionPin.ATTACH_POS_CENTRE, 0, true, 0, ConnDirFlag.ConnDirUp);
        router.deleteShape(shape);
        final JunctionRef junction = new JunctionRef(router, new Point(200, 200));
        router.deleteJunction(junction);
        router.processTransaction();

        assertEquals(before, router.vertices.connsSize());
    }

    /** Moving a junction deleted in the same transaction is rejected like for shapes. */
    @Test
    void junctionMoveAfterDeleteIsRejected() {
        final Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setTransactionUse(true);
        final JunctionRef junction = new JunctionRef(router, new Point(0, 0));
        router.processTransaction();
        router.deleteJunction(junction);

        assertThrows(IllegalStateException.class, () -> router.moveJunction(junction, new Point(5, 5)));
    }

    /** -0.0 and 0.0 are the same coordinate: equals, hashCode and compareTo must agree (C++ compares with ==). */
    @Test
    void negativeZeroIsSameCoordinate() {
        final Point a = new Point(0.0, -0.0);
        final Point b = new Point(-0.0, 0.0);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(0, a.compareTo(b));
        assertEquals(1, new HashSet<>(List.of(a, b)).size());
        assertEquals(1, new java.util.TreeSet<>(List.of(a, b)).size());
    }

    private static ConnEnd end(final double[] s, final int side) {
        final double cx = s[0] + s[2] / 2, cy = s[1] + s[3] / 2;
        return switch (side) {
            case 0 -> new ConnEnd(new Point(s[0] + s[2], cy), ConnDirFlag.ConnDirRight);
            case 1 -> new ConnEnd(new Point(s[0], cy), ConnDirFlag.ConnDirLeft);
            case 2 -> new ConnEnd(new Point(cx, s[1]), ConnDirFlag.ConnDirUp);
            default -> new ConnEnd(new Point(cx, s[1] + s[3]), ConnDirFlag.ConnDirDown);
        };
    }
}
