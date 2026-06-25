package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the case where two boxes have a right→left connector and the
 * horizontal gap between them is smaller than the shapeBufferDistance, causing
 * the routing boxes to overlap. Without the fix the router would produce a huge
 * detour around both shapes instead of a direct L-shaped path.
 */
public class TestNarrowChannelConnector {

    /**
     * gap=5, shapeBufferDistance=10 → routing boxes overlap by 15.
     * Expected: simple L-shaped path staying between the boxes.
     */
    @Test
    public void testOverlappingRoutingBoxes() {
        Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalSegmentsConnectedToShapes, true);
        router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 20);
        router.setRoutingParameter(Router.RoutingParameter.shapeBufferDistance, 10);

        // Box A: x=[50,100], y=[30,70]  → right pin at (100, 50)
        Rectangle boxA = new Rectangle(new Point(50, 30), new Point(100, 70));
        new ShapeRef(router, boxA, 1);

        // Box B: x=[105,155], y=[50,90]  → left pin at (105, 70)
        // Physical gap = 5, shapeBufferDistance = 10 → routing boxes overlap
        Rectangle boxB = new Rectangle(new Point(105, 50), new Point(155, 90));
        new ShapeRef(router, boxB, 2);

        ConnEnd src = new ConnEnd(new Point(100, 50), ConnDirFlag.ConnDirRight);
        ConnEnd dst = new ConnEnd(new Point(105, 70), ConnDirFlag.ConnDirLeft);
        ConnRef conn = new ConnRef(router, src, dst);
        conn.setRoutingType(ConnType.Orthogonal);

        router.processTransaction();

        Polygon route = conn.displayRoute();
        List<Point> pts = route.ps;

        assertTrue(pts.size() >= 2, "Route should have at least 2 points, got: " + pts.size());

        Point first = pts.getFirst();
        Point last = pts.getLast();
        assertEquals(100.0, first.x, 1.0, "Route should start near x=100");
        assertEquals(50.0, first.y, 1.0, "Route should start near y=50");
        assertEquals(105.0, last.x, 1.0, "Route should end near x=105");
        assertEquals(70.0, last.y, 1.0, "Route should end near y=70");

        // The route must NOT be a huge detour: all x-coordinates must stay
        // within a reasonable range around the two boxes (not go to x=165 or x=40).
        for (Point p : pts) {
            assertTrue(p.x >= 49.0 && p.x <= 156.0,
                    "Route should not detour far outside the boxes, but got x=" + p.x
                    + ". Full route: " + pts);
        }
    }

    /**
     * gap=1, shapeBufferDistance=3 → routing boxes overlap.
     * Expected: simple L-shaped path, no detour.
     */
    @Test
    public void testVerySmallGapWithBuffer() {
        Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalSegmentsConnectedToShapes, true);
        router.setRoutingParameter(Router.RoutingParameter.shapeBufferDistance, 3);

        // Box A: x=[50,100], y=[30,70]  → right pin at (100, 50)
        Rectangle boxA = new Rectangle(new Point(50, 30), new Point(100, 70));
        new ShapeRef(router, boxA, 1);

        // Box B: x=[101,151], y=[50,90]  → left pin at (101, 70)
        // Physical gap = 1, shapeBufferDistance = 3 → routing boxes overlap
        Rectangle boxB = new Rectangle(new Point(101, 50), new Point(151, 90));
        new ShapeRef(router, boxB, 2);

        ConnEnd src = new ConnEnd(new Point(100, 50), ConnDirFlag.ConnDirRight);
        ConnEnd dst = new ConnEnd(new Point(101, 70), ConnDirFlag.ConnDirLeft);
        ConnRef conn = new ConnRef(router, src, dst);
        conn.setRoutingType(ConnType.Orthogonal);

        router.processTransaction();

        Polygon route = conn.displayRoute();
        List<Point> pts = route.ps;

        assertTrue(pts.size() >= 2, "Route should have at least 2 points, got: " + pts.size());

        Point first = pts.getFirst();
        Point last = pts.getLast();
        assertEquals(100.0, first.x, 1.0, "Route should start near x=100");
        assertEquals(50.0, first.y, 1.0, "Route should start near y=50");
        assertEquals(101.0, last.x, 1.0, "Route should end near x=101");
        assertEquals(70.0, last.y, 1.0, "Route should end near y=70");

        // The route must NOT be a huge detour.
        for (Point p : pts) {
            assertTrue(p.x >= 49.0 && p.x <= 152.0,
                    "Route should not detour far outside the boxes, but got x=" + p.x
                    + ". Full route: " + pts);
        }
    }

    /**
     * Normal case: gap=10, no shapeBufferDistance → should still work correctly.
     */
    @Test
    public void testNormalGapUnaffected() {
        Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalSegmentsConnectedToShapes, true);

        Rectangle boxA = new Rectangle(new Point(50, 30), new Point(100, 70));
        new ShapeRef(router, boxA, 1);
        Rectangle boxB = new Rectangle(new Point(110, 50), new Point(160, 90));
        new ShapeRef(router, boxB, 2);

        ConnEnd src = new ConnEnd(new Point(100, 50), ConnDirFlag.ConnDirRight);
        ConnEnd dst = new ConnEnd(new Point(110, 70), ConnDirFlag.ConnDirLeft);
        ConnRef conn = new ConnRef(router, src, dst);
        conn.setRoutingType(ConnType.Orthogonal);

        router.processTransaction();

        Polygon route = conn.displayRoute();
        List<Point> pts = route.ps;

        assertTrue(pts.size() >= 2, "Route should have at least 2 points");
        assertEquals(100.0, pts.getFirst().x, 1.0);
        assertEquals(50.0, pts.getFirst().y, 1.0);
        assertEquals(110.0, pts.getLast().x, 1.0);
        assertEquals(70.0, pts.getLast().y, 1.0);
    }
}
