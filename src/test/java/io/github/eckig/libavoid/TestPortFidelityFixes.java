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

    /** Integer options use the C++ ordinals (router.h), which differ from the Java enum order. */
    @Test
    void integerRoutingOptionsUseCppOrdinals() {
        final Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setRoutingOption(4, true);
        assertTrue(router.routingOption(Router.RoutingOption.performUnifyingNudgingPreprocessingStep));
        router.setRoutingOption(6, false);
        assertFalse(router.routingOption(Router.RoutingOption.nudgeSharedPathsWithCommonEndPoint));
        router.setRoutingOption(2, true);
        assertTrue(router.routingOption(Router.RoutingOption.penaliseOrthogonalSharedPathsAtConnEnds));

        // hyperedge options are not ported: ignored, and nothing else changes
        final boolean before = router.routingOption(Router.RoutingOption.nudgeFinalSegmentsFromSamePoint);
        router.setRoutingOption(5, !before);
        assertEquals(before, router.routingOption(Router.RoutingOption.nudgeFinalSegmentsFromSamePoint));

        assertThrows(IllegalArgumentException.class, () -> router.setRoutingOption(7, true));
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
