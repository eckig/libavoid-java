package io.github.eckig.libavoid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.Test;

/**
 * The regions of nudging segments are built in the same order as in C++ (orthogonal.cpp, nudgeOrthogonalRoutes). The
 * order matters: later steps (linesort, unifying) depend on it.
 */
class TestNudgingRegionOrder {

    /** Direct port of the C++ loop: restart the scan from the beginning after each added segment. */
    private static <T> List<T> cppRegion(final List<T> remaining, final BiPredicate<T, T> overlaps) {
        final List<T> region = new ArrayList<>();
        region.add(remaining.removeFirst());
        int k = 0;
        while (k < remaining.size()) {
            boolean overlapping = false;
            for (T inRegion : region) {
                if (overlaps.test(remaining.get(k), inRegion)) {
                    overlapping = true;
                    break;
                }
            }
            if (overlapping) {
                region.add(remaining.remove(k));
                k = 0;
            } else {
                k++;
            }
        }
        return region;
    }

    /** Intervals [min, max]: overlap like collinear segments. */
    private static final BiPredicate<double[], double[]> OVERLAP = (a, b) -> a[0] <= b[1] && b[0] <= a[1];

    @Test
    void regionOrderMatchesCpp() {
        final Random random = new Random(1);
        for (int round = 0; round < 500; round++) {
            final List<double[]> segments = new ArrayList<>();
            for (int i = 0, n = 1 + random.nextInt(30); i < n; i++) {
                final double start = random.nextInt(200);
                segments.add(new double[] {start, start + 1 + random.nextInt(30)});
            }
            final List<double[]> cppRemaining = new ArrayList<>(segments);
            final List<double[]> javaRemaining = new ArrayList<>(segments);
            final List<double[]> region = new ArrayList<>();
            while (!cppRemaining.isEmpty()) {
                final List<double[]> expected = cppRegion(cppRemaining, OVERLAP);
                ImproveOrthogonalRoutes.nextRegion(javaRemaining, region, OVERLAP);
                assertEquals(expected, region, "round " + round);
                assertEquals(cppRemaining, javaRemaining, "round " + round);
            }
        }
    }

    /** The example from the audit: B overlaps only C, C and D overlap A. C++ order: A, C, B, D. */
    @Test
    void segmentOverlappingLaterMemberComesFirst() {
        final double[] a = {0, 10};
        final double[] b = {20, 30};
        final double[] c = {5, 25};
        final double[] d = {8, 9};
        final List<double[]> remaining = new ArrayList<>(List.of(a, b, c, d));
        final List<double[]> region = new ArrayList<>();
        ImproveOrthogonalRoutes.nextRegion(remaining, region, OVERLAP);

        assertEquals(List.of(a, c, b, d), region);
    }
}
