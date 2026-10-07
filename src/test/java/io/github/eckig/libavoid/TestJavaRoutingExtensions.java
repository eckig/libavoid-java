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
}
