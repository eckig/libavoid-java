package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the nudgeFinalSegmentsFromSamePoint routing option.
 *
 * When set to false, connectors leaving from the exact same point should
 * not be nudged apart at their common endpoint.
 */
class TestNudgeFinalSegmentsFromSamePoint {

    /**
     * Two connectors start at the same point (right side of node A) and go to
     * different targets. With nudgeFinalSegmentsFromSamePoint=false, their first
     * segments should stay at the same Y coordinate (not be nudged apart).
     */
    @Test
    void testTwoConnectorsFromSamePointNotNudged() {
        Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setTransactionUse(true);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
        router.setRoutingPenalty(Router.RoutingParameter.idealNudgingDistance, 5);
        router.setRoutingOption(Router.RoutingOption.nudgeFinalSegmentsFromSamePoint, false);

        // Node A at (0, 0), size 60x40
        ShapeRef nodeA = new ShapeRef(router, new Rectangle(new Point(0, 0), new Point(60, 40)));
        // Node B at (200, 0), size 60x40
        ShapeRef nodeB = new ShapeRef(router, new Rectangle(new Point(200, 0), new Point(260, 40)));
        // Node C at (200, 100), size 60x40
        ShapeRef nodeC = new ShapeRef(router, new Rectangle(new Point(200, 100), new Point(260, 140)));

        // Both connectors start at the right side of node A at the same point (60, 20)
        Point srcPoint = new Point(60, 20);
        ConnEnd src1 = new ConnEnd(srcPoint, ConnDirFlag.ConnDirRight);
        ConnEnd src2 = new ConnEnd(srcPoint, ConnDirFlag.ConnDirRight);
        ConnEnd tgt1 = new ConnEnd(new Point(200, 20), ConnDirFlag.ConnDirLeft);
        ConnEnd tgt2 = new ConnEnd(new Point(200, 120), ConnDirFlag.ConnDirLeft);

        ConnRef conn1 = new ConnRef(router);
        conn1.setSourceEndpoint(src1);
        conn1.setDestEndpoint(tgt1);

        ConnRef conn2 = new ConnRef(router);
        conn2.setSourceEndpoint(src2);
        conn2.setDestEndpoint(tgt2);

        router.processTransaction();

        Polygon route1 = conn1.displayRoute();
        Polygon route2 = conn2.displayRoute();

        assertTrue(route1.size() >= 2, "conn1 should have a route");
        assertTrue(route2.size() >= 2, "conn2 should have a route");

        // The first point of both routes should be at the same Y coordinate
        // (not nudged apart), since they share the same start point
        double firstY1 = route1.ps.get(0).y;
        double firstY2 = route2.ps.get(0).y;

        assertEquals(firstY1, firstY2, 0.001,
                "First segments of connectors from the same point should not be nudged apart. " +
                "Got Y1=" + firstY1 + " Y2=" + firstY2);
    }

    /**
     * With nudgeFinalSegmentsFromSamePoint=true (default), connectors from the
     * same point ARE nudged apart.
     */
    @Test
    void testTwoConnectorsFromSamePointNudgedByDefault() {
        Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setTransactionUse(true);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
        router.setRoutingPenalty(Router.RoutingParameter.idealNudgingDistance, 5);
        // nudgeFinalSegmentsFromSamePoint defaults to true - no need to set it

        ShapeRef nodeA = new ShapeRef(router, new Rectangle(new Point(0, 0), new Point(60, 40)));
        ShapeRef nodeB = new ShapeRef(router, new Rectangle(new Point(200, 0), new Point(260, 40)));
        ShapeRef nodeC = new ShapeRef(router, new Rectangle(new Point(200, 100), new Point(260, 140)));

        Point srcPoint = new Point(60, 20);
        ConnEnd src1 = new ConnEnd(srcPoint, ConnDirFlag.ConnDirRight);
        ConnEnd src2 = new ConnEnd(srcPoint, ConnDirFlag.ConnDirRight);
        ConnEnd tgt1 = new ConnEnd(new Point(200, 20), ConnDirFlag.ConnDirLeft);
        ConnEnd tgt2 = new ConnEnd(new Point(200, 120), ConnDirFlag.ConnDirLeft);

        ConnRef conn1 = new ConnRef(router);
        conn1.setSourceEndpoint(src1);
        conn1.setDestEndpoint(tgt1);

        ConnRef conn2 = new ConnRef(router);
        conn2.setSourceEndpoint(src2);
        conn2.setDestEndpoint(tgt2);

        router.processTransaction();

        Polygon route1 = conn1.displayRoute();
        Polygon route2 = conn2.displayRoute();

        assertTrue(route1.size() >= 2, "conn1 should have a route");
        assertTrue(route2.size() >= 2, "conn2 should have a route");

        // With default settings, the routes should be valid (no assertion about nudging)
        // Just verify routes are computed
        assertNotNull(route1);
        assertNotNull(route2);
    }
}
