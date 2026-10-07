package io.github.eckig.libavoid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * The performance optimizations must not change any route.
 */
class TestPerformanceOptimizations {

    /** Routes of a random scene with many overlapping and many far apart connectors. */
    private static List<String> routes(final long seed) {
        final Random r = new Random(seed);
        final Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 1000);
        router.setRoutingParameter(Router.RoutingParameter.shapeBufferDistance, 15);
        router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 5);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalTouchingColinearSegments, true);
        router.setRoutingOption(Router.RoutingOption.performUnifyingNudgingPreprocessingStep, true);
        router.setRoutingOption(Router.RoutingOption.nudgeSharedPathsWithCommonEndPoint, true);
        final List<double[]> boxes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 5; j++) {
                final double[] b = {i * 220, j * 160};
                boxes.add(b);
                new ShapeRef(router, new Rectangle(new Point(b[0], b[1]), new Point(b[0] + 120, b[1] + 60)));
            }
        }
        final List<ConnRef> conns = new ArrayList<>();
        for (int k = 0; k < 120; k++) {
            final double[] s = boxes.get(r.nextInt(boxes.size()));
            final double[] t = boxes.get(r.nextInt(boxes.size()));
            if (s == t) {
                continue;
            }
            conns.add(new ConnRef(router, new ConnEnd(new Point(s[0] + 120, s[1] + 20 + r.nextInt(3) * 10),
                    ConnDirFlag.ConnDirRight), new ConnEnd(new Point(t[0], t[1] + 20 + r.nextInt(3) * 10),
                    ConnDirFlag.ConnDirLeft)));
        }
        router.processTransaction();
        final List<String> result = new ArrayList<>();
        for (ConnRef c : conns) {
            result.add(c.displayRoute().ps.toString());
        }
        return result;
    }

    @Test
    void skippingDisjointPairsDoesNotChangeRoutes() {
        for (long seed = 1; seed <= 3; seed++) {
            final List<String> optimized = routes(seed);
            ImproveOrthogonalRoutes.skipDisjointPairs = false;
            try {
                assertEquals(routes(seed), optimized, "seed " + seed);
            } finally {
                ImproveOrthogonalRoutes.skipDisjointPairs = true;
            }
        }
    }

    /** Touching boxes (e.g. a route ending where another starts) are not disjoint. */
    @Test
    void touchingBoxesAreNotDisjoint() {
        assertFalse(ImproveOrthogonalRoutes.disjoint(new double[] {0, 0, 10, 10}, new double[] {10, 0, 20, 10}));
        assertFalse(ImproveOrthogonalRoutes.disjoint(new double[] {0, 0, 10, 10}, new double[] {0, 10, 10, 20}));
        assertFalse(ImproveOrthogonalRoutes.disjoint(new double[] {0, 5, 10, 5}, new double[] {5, 0, 5, 10}));
        assertTrue(ImproveOrthogonalRoutes.disjoint(new double[] {0, 0, 10, 10}, new double[] {10.5, 0, 20, 10}));
    }

    /** hashCode without boxing: same values as before (Objects.hash of the coordinate bits). */
    @Test
    void pointHashCodeUnchanged() {
        final Random r = new Random(5);
        for (int i = 0; i < 1000; i++) {
            final double x = (r.nextInt(2000) - 1000) / 4.0;
            final double y = (r.nextInt(2000) - 1000) / 4.0;
            assertEquals(java.util.Objects.hash(Double.doubleToLongBits(x + 0.0), Double.doubleToLongBits(y + 0.0)),
                    new Point(x, y).hashCode());
        }
    }
}
