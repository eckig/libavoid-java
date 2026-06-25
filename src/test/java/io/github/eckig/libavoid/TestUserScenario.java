package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Reproduces the user's exact scenario:
 * - shapeBufferDistance = 1.5 * 12 = 18
 * - Two boxes with a right→left connector
 * - Boxes close together (gap < 2 * shapeBufferDistance)
 *
 * From the debug log:
 *   Box1: (24,24)-(224,174)   src-pin: (224, y) ConnDirRight
 *   Box2: (252,48)-(403,174)  tgt-pin: (252, y) ConnDirLeft
 *   Gap = 28px, buffer = 18 → routing boxes overlap by 8px
 */
public class TestUserScenario {

    private Router buildRouter() {
        Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setTransactionUse(true);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 1000);
        router.setRoutingPenalty(Router.RoutingParameter.anglePenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.crossingPenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.clusterCrossingPenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.fixedSharedPathPenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.portDirectionPenalty, 0);
        router.setRoutingParameter(Router.RoutingParameter.shapeBufferDistance, 18); // 1.5 * 12
        router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 5);
        router.setRoutingPenalty(Router.RoutingParameter.reverseDirectionPenalty, 0);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalSegmentsConnectedToShapes, false);
        router.setRoutingOption(Router.RoutingOption.penaliseOrthogonalSharedPathsAtConnEnds, false);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalTouchingColinearSegments, true);
        router.setRoutingOption(Router.RoutingOption.performUnifyingNudgingPreprocessingStep, true);
        router.setRoutingOption(Router.RoutingOption.nudgeSharedPathsWithCommonEndPoint, true);
        return router;
    }

    /**
     * Box1: (24,24)-(224,174), Box2: (252,48)-(403,174)
     * src-pin: right side of Box1 at y=99 (midpoint), ConnDirRight
     * tgt-pin: left side of Box2 at y=111 (midpoint), ConnDirLeft
     * Gap = 28px, buffer = 18
     */
    @Test
    public void testCloseBoxesRightToLeft() {
        Router router = buildRouter();

        // Box1: x=24, y=24, w=200, h=150
        Rectangle box1 = new Rectangle(new Point(24, 24), new Point(224, 174));
        new ShapeRef(router, box1, 1);

        // Box2: x=252, y=48, w=151, h=126
        Rectangle box2 = new Rectangle(new Point(252, 48), new Point(403, 174));
        new ShapeRef(router, box2, 2);

        // src: right side of Box1 at midpoint y
        double srcY = 24 + 150.0 / 2; // = 99
        ConnEnd src = new ConnEnd(new Point(224, srcY), ConnDirFlag.ConnDirRight);

        // tgt: left side of Box2 at midpoint y
        double tgtY = 48 + 126.0 / 2; // = 111
        ConnEnd tgt = new ConnEnd(new Point(252, tgtY), ConnDirFlag.ConnDirLeft);

        ConnRef conn = new ConnRef(router, src, tgt);
        conn.setRoutingType(ConnType.Orthogonal);

        router.processTransaction();

        Polygon route = conn.displayRoute();
        List<Point> pts = route.ps;

        System.out.println("testCloseBoxesRightToLeft route:");
        for (Point p : pts) {
            System.out.println("  " + p.x + ", " + p.y);
        }

        // The route must start at src and end at tgt
        Point first = pts.getFirst();
        Point last = pts.getLast();
        System.out.println("Expected start: (224, " + srcY + "), got: (" + first.x + ", " + first.y + ")");
        System.out.println("Expected end:   (252, " + tgtY + "), got: (" + last.x + ", " + last.y + ")");

        // Last segment must arrive from the left (prev.x < last.x)
        if (pts.size() >= 2) {
            Point prev = pts.get(pts.size() - 2);
            System.out.println("Last segment: (" + prev.x + ", " + prev.y + ") -> (" + last.x + ", " + last.y + ")");
            System.out.println("Last segment arrives from left: " + (prev.x < last.x));
        }
    }

    /**
     * Same as above but with the exact coordinates from the debug log:
     * src=(224, 24) ConnDirRight, tgt=(252, 48) ConnDirLeft
     * (top-right of Box1 → top-left of Box2)
     */
    @Test
    public void testExactLogCoordinates() {
        Router router = buildRouter();

        Rectangle box1 = new Rectangle(new Point(24, 24), new Point(224, 174));
        new ShapeRef(router, box1, 1);

        Rectangle box2 = new Rectangle(new Point(252, 48), new Point(403, 174));
        new ShapeRef(router, box2, 2);

        // Exact coordinates from the log
        ConnEnd src = new ConnEnd(new Point(224, 24), ConnDirFlag.ConnDirRight);
        ConnEnd tgt = new ConnEnd(new Point(252, 48), ConnDirFlag.ConnDirLeft);

        ConnRef conn = new ConnRef(router, src, tgt);
        conn.setRoutingType(ConnType.Orthogonal);

        router.processTransaction();

        Polygon route = conn.displayRoute();
        List<Point> pts = route.ps;

        System.out.println("testExactLogCoordinates route:");
        for (Point p : pts) {
            System.out.println("  " + p.x + ", " + p.y);
        }

        Point first = pts.getFirst();
        Point last = pts.getLast();
        System.out.println("Expected start: (224, 24), got: (" + first.x + ", " + first.y + ")");
        System.out.println("Expected end:   (252, 48), got: (" + last.x + ", " + last.y + ")");

        if (pts.size() >= 2) {
            Point prev = pts.get(pts.size() - 2);
            System.out.println("Last segment: (" + prev.x + ", " + prev.y + ") -> (" + last.x + ", " + last.y + ")");
            System.out.println("Last segment arrives from left: " + (prev.x < last.x));
        }
    }
}
