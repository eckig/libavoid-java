package io.github.eckig.libavoid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Regression tests for the Java-only routing extensions (not in C++): retry of failed connectors without shape buffer
 * and the enforced approach direction for ConnDirLeft destinations.
 */
class TestJavaRoutingExtensions {

    private static void assertOrthogonal(final Polygon route) {
        for (int i = 1; i < route.size(); i++) {
            final Point a = route.at(i - 1);
            final Point b = route.at(i);
            assertTrue(a.x == b.x || a.y == b.y, "diagonal segment " + a + " -> " + b);
        }
    }

    /**
     * The target node is inside a room whose only door (10 px) is narrower than twice the shape buffer: the connector
     * fails with the buffer and is routed through the door by the retry without buffer.
     */
    @Test
    void failedConnectorIsRoutedThroughGapNarrowerThanBuffer() {
        final Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setRoutingParameter(Router.RoutingParameter.shapeBufferDistance, 20);
        new ShapeRef(router, new Rectangle(new Point(-100, 100), new Point(0, 150)));
        new ShapeRef(router, new Rectangle(new Point(300, 100), new Point(400, 150)));
        // room walls, door in the left wall at y 200..210
        new ShapeRef(router, new Rectangle(new Point(200, 0), new Point(210, 200)));
        new ShapeRef(router, new Rectangle(new Point(200, 210), new Point(210, 250)));
        new ShapeRef(router, new Rectangle(new Point(200, 0), new Point(500, 10)));
        new ShapeRef(router, new Rectangle(new Point(200, 240), new Point(500, 250)));
        new ShapeRef(router, new Rectangle(new Point(490, 0), new Point(500, 250)));
        final ConnRef conn = new ConnRef(router, new ConnEnd(new Point(0, 125), ConnDirFlag.ConnDirRight),
                new ConnEnd(new Point(300, 125), ConnDirFlag.ConnDirLeft));
        router.processTransaction();

        final Polygon route = conn.displayRoute();
        assertOrthogonal(route);
        boolean throughDoor = false;
        for (int i = 1; i < route.size(); i++) {
            final Point a = route.at(i - 1);
            final Point b = route.at(i);
            if (a.y == b.y && a.y >= 200 && a.y <= 210 && Math.min(a.x, b.x) < 200 && Math.max(a.x, b.x) > 210) {
                throughDoor = true;
            }
        }
        assertTrue(throughDoor, "route should pass the door: " + route.ps);
    }

    /** A destination that only allows ConnDirLeft is approached horizontally from the left. */
    @Test
    void connDirLeftDestinationIsApproachedFromTheLeft() {
        final Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setRoutingParameter(Router.RoutingParameter.shapeBufferDistance, 20);
        new ShapeRef(router, new Rectangle(new Point(300, 0), new Point(400, 50)));
        new ShapeRef(router, new Rectangle(new Point(0, 200), new Point(100, 250)));
        final ConnRef conn = new ConnRef(router, new ConnEnd(new Point(350, 50), ConnDirFlag.ConnDirDown),
                new ConnEnd(new Point(0, 225), ConnDirFlag.ConnDirLeft));
        router.processTransaction();

        final Polygon route = conn.displayRoute();
        assertOrthogonal(route);
        final Point last = route.at(route.size() - 1);
        final Point prev = route.at(route.size() - 2);
        assertEquals(new Point(0, 225), last);
        assertTrue(prev.y == last.y && prev.x < last.x, "should arrive horizontally from the left: " + route.ps);
    }

    /** On a 2-point route the source point must not move when a detour is inserted (audit item D6). */
    @Test
    void approachDetourKeepsSourcePoint() {
        final List<Point> pts = new ArrayList<>(List.of(new Point(100, 0), new Point(100, 50)));
        ConnRef.enforceLastSegmentFromLeft(pts, 10);

        assertEquals(new Point(100, 0), pts.getFirst(), "source moved");
        assertEquals(new Point(100, 50), pts.getLast());
        for (int i = 1; i < pts.size(); i++) {
            assertTrue(pts.get(i - 1).x == pts.get(i).x || pts.get(i - 1).y == pts.get(i).y, "diagonal: " + pts);
        }
        final Point prev = pts.get(pts.size() - 2);
        assertTrue(prev.y == 50 && prev.x < 100, "should arrive from the left: " + pts);
    }

    private static double length(final Polygon route) {
        double length = 0;
        for (int i = 1; i < route.size(); i++) {
            length += Math.abs(route.at(i).x - route.at(i - 1).x) + Math.abs(route.at(i).y - route.at(i - 1).y);
        }
        return length;
    }

    private static void assertOutside(final Polygon route, final double x0, final double y0, final double x1,
            final double y1) {
        for (int i = 1; i < route.size(); i++) {
            final Point a = route.at(i - 1);
            final Point b = route.at(i);
            final double mx = (a.x + b.x) / 2;
            final double my = (a.y + b.y) / 2;
            assertTrue(!(mx > x0 && mx < x1 && my > y0 && my < y1), "segment " + a + " -> " + b + " crosses a node");
        }
    }

    /**
     * Known limitation: with a gap smaller than the shape buffer (here 10 px vs. 15 px), the target's approach
     * direction leaves no room, the raw route goes back and forth at the target, and after simplification and nudging
     * the vertical segment ends up inside the source node.
     */
    @org.junit.jupiter.api.Disabled("known limitation: gap smaller than shape buffer, see Javadoc")
    @Test
    void connectorsBetweenVeryCloseNodesStayOutsideNodes() {
        final Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setRoutingParameter(Router.RoutingParameter.shapeBufferDistance, 15);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 1000);
        new ShapeRef(router, new Rectangle(new Point(0, 0), new Point(100, 80)));
        new ShapeRef(router, new Rectangle(new Point(110, 60), new Point(210, 140)));
        final ConnRef conn = new ConnRef(router, new ConnEnd(new Point(100, 20), ConnDirFlag.ConnDirRight),
                new ConnEnd(new Point(110, 80), ConnDirFlag.ConnDirLeft));
        router.processTransaction();

        assertOutside(conn.displayRoute(), 0, 0, 100, 80);
    }

    /**
     * Two nodes closer together than twice the shape buffer: with the buffer, the router finds only long detours around
     * both nodes. These are routed again without buffer: short, orthogonal and not crossing the nodes (the case behind
     * the original fallback in commit 7a7f282).
     */
    @Test
    void connectorsBetweenCloseNodesAreShort() {
        for (final double gap : new double[] {20, 30}) {
            final Router router = new Router(Router.RouterFlag.OrthogonalRouting);
            router.setRoutingParameter(Router.RoutingParameter.shapeBufferDistance, 15);
            router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 1000);
            router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 5);
            new ShapeRef(router, new Rectangle(new Point(0, 0), new Point(100, 80)));
            new ShapeRef(router, new Rectangle(new Point(100 + gap, 60), new Point(200 + gap, 140)));
            final java.util.List<ConnRef> conns = new java.util.ArrayList<>();
            for (int k = 0; k < 3; k++) {
                conns.add(new ConnRef(router, new ConnEnd(new Point(100, 20 + 20 * k), ConnDirFlag.ConnDirRight),
                        new ConnEnd(new Point(100 + gap, 80 + 20 * k), ConnDirFlag.ConnDirLeft)));
            }
            router.processTransaction();

            for (ConnRef conn : conns) {
                final Polygon route = conn.displayRoute();
                assertOrthogonal(route);
                assertTrue(length(route) <= gap + 60 + 1, "gap " + gap + ": detour " + route.ps);
                assertOutside(route, 0, 0, 100, 80);
                assertOutside(route, 100 + gap, 60, 200 + gap, 140);
            }
        }
    }

    /** A legitimate detour (U-shaped obstacle between the end points) is kept, not replaced by a fake route. */
    @Test
    void legitimateDetourIsKept() {
        final Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setRoutingParameter(Router.RoutingParameter.shapeBufferDistance, 10);
        // U opening to the left around the source; the target is directly right of it
        new ShapeRef(router, new Rectangle(new Point(-20, -100), new Point(300, -80)));
        new ShapeRef(router, new Rectangle(new Point(280, -80), new Point(300, 80)));
        new ShapeRef(router, new Rectangle(new Point(-20, 80), new Point(300, 100)));
        final ConnRef conn = new ConnRef(router, new ConnEnd(new Point(0, 0), ConnDirFlag.ConnDirAll),
                new ConnEnd(new Point(400, 0), ConnDirFlag.ConnDirAll));
        router.processTransaction();

        final Polygon route = conn.displayRoute();
        assertOrthogonal(route);
        assertTrue(length(route) > 1.5 * 400, "should be the real detour: " + route.ps);
        assertOutside(route, -20, -100, 300, -80);
        assertOutside(route, 280, -80, 300, 80);
        assertOutside(route, -20, 80, 300, 100);
    }

    /** The fallback route (no path found) is orthogonal and uses the allowed directions at both ends. */
    @Test
    void fallbackRouteIsOrthogonalAndRespectsDirections() {
        final int[] dirs = {ConnDirFlag.ConnDirUp, ConnDirFlag.ConnDirDown, ConnDirFlag.ConnDirLeft,
                ConnDirFlag.ConnDirRight, ConnDirFlag.ConnDirAll};
        final Point[] targets = {new Point(200, 100), new Point(-200, 100), new Point(200, -100), new Point(0, 300),
                new Point(5, 0)};
        final Point src = new Point(0, 0);
        for (int sd : dirs) {
            for (int td : dirs) {
                for (Point dst : targets) {
                    final java.util.List<Point> pts = new java.util.ArrayList<>();
                    pts.add(src);
                    pts.addAll(ConnRef.fallbackBends(src, sd, dst, td, 10));
                    pts.add(dst);
                    final String msg = sd + "->" + td + " to " + dst + ": " + pts;
                    for (int i = 1; i < pts.size(); i++) {
                        assertTrue(pts.get(i - 1).x == pts.get(i).x || pts.get(i - 1).y == pts.get(i).y, msg);
                    }
                    assertTrue(leaves(pts.get(0), pts.get(1), sd), "source direction " + msg);
                    assertTrue(leaves(pts.get(pts.size() - 1), pts.get(pts.size() - 2), td), "target direction " + msg);
                }
            }
        }
    }

    /** Whether the segment from end to next leaves end in an allowed direction. */
    private static boolean leaves(final Point end, final Point next, final int dirs) {
        final int flag = next.x > end.x ? ConnDirFlag.ConnDirRight : next.x < end.x ? ConnDirFlag.ConnDirLeft
                : next.y > end.y ? ConnDirFlag.ConnDirDown : ConnDirFlag.ConnDirUp;
        return (dirs & flag) != 0;
    }
}
