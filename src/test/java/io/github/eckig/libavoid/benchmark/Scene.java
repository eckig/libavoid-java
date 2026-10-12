package io.github.eckig.libavoid.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.github.eckig.libavoid.ConnDirFlag;
import io.github.eckig.libavoid.ConnEnd;
import io.github.eckig.libavoid.ConnRef;
import io.github.eckig.libavoid.Point;
import io.github.eckig.libavoid.Polygon;
import io.github.eckig.libavoid.Rectangle;
import io.github.eckig.libavoid.Router;
import io.github.eckig.libavoid.ShapeRef;

/**
 * A routing scene for benchmarks, in a simple text format (e.g. dumped from an application's router), one entry per line:
 * <pre>
 * param &lt;RoutingParameter&gt; &lt;value&gt;
 * option &lt;RoutingOption&gt; &lt;true|false&gt;
 * shape &lt;minX&gt; &lt;minY&gt; &lt;maxX&gt; &lt;maxY&gt;
 * conn &lt;srcX&gt; &lt;srcY&gt; &lt;srcConnDir&gt; &lt;dstX&gt; &lt;dstY&gt; &lt;dstConnDir&gt;
 * </pre>
 * Lines starting with {@code #} are comments.
 */
public record Scene(String name, List<String> lines) {

    /** Routing settings of a real graph editor application, used for the generated scenes. */
    private static final List<String> SETTINGS = List.of(
            "param segmentPenalty 1000", "param anglePenalty 0", "param crossingPenalty 0",
            "param fixedSharedPathPenalty 0", "param portDirectionPenalty 0", "param shapeBufferDistance 12",
            "param idealNudgingDistance 5", "param reverseDirectionPenalty 0",
            "option nudgeOrthogonalSegmentsConnectedToShapes false",
            "option penaliseOrthogonalSharedPathsAtConnEnds false",
            "option nudgeOrthogonalTouchingColinearSegments true",
            "option performUnifyingNudgingPreprocessingStep true",
            "option nudgeSharedPathsWithCommonEndPoint true", "option nudgeFinalSegmentsFromSamePoint false");

    /** A loaded scene: the router (transaction not yet processed), its shapes and connectors. */
    public record Loaded(Router router, List<ShapeRef> shapes, List<ConnRef> connectors) {
    }

    /** Loads a scene from the test resources ({@code /benchmark/<file>}). */
    public static Scene resource(final String file) throws IOException {
        try (InputStream in = Scene.class.getResourceAsStream("/benchmark/" + file)) {
            if (in == null) {
                throw new IOException("Scene not found: " + file);
            }
            return new Scene(file, new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList());
        }
    }

    /** Grid of equally sized nodes, connectors from right to left sides (reproducible). */
    public static Scene grid(final int cols, final int rows, final int connectors, final long seed) {
        final Random r = new Random(seed);
        final List<String> lines = new ArrayList<>(SETTINGS);
        final List<double[]> boxes = new ArrayList<>();
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                final double[] b = {i * 220, j * 160, 120, 60};
                boxes.add(b);
                lines.add(shape(b));
            }
        }
        addConnectors(r, boxes, connectors, lines);
        return new Scene("grid-" + cols + "x" + rows + "-" + connectors, lines);
    }

    /** Randomly placed, non-overlapping nodes of different sizes (reproducible). */
    public static Scene random(final int nodes, final int connectors, final long seed) {
        final Random r = new Random(seed);
        final List<String> lines = new ArrayList<>(SETTINGS);
        final List<double[]> boxes = new ArrayList<>();
        for (int i = 0; i < nodes * 10 && boxes.size() < nodes; i++) {
            final double[] b = {r.nextInt(40) * 40, r.nextInt(40) * 40, 60 + r.nextInt(4) * 24, 36 + r.nextInt(3) * 24};
            if (boxes.stream().noneMatch(o -> b[0] < o[0] + o[2] + 30 && o[0] < b[0] + b[2] + 30
                    && b[1] < o[1] + o[3] + 30 && o[1] < b[1] + b[3] + 30)) {
                boxes.add(b);
                lines.add(shape(b));
            }
        }
        addConnectors(r, boxes, connectors, lines);
        return new Scene("random-" + nodes + "-" + connectors + "-seed" + seed, lines);
    }

    private static String shape(final double[] b) {
        return "shape " + b[0] + " " + b[1] + " " + (b[0] + b[2]) + " " + (b[1] + b[3]);
    }

    private static void addConnectors(final Random r, final List<double[]> boxes, final int count,
            final List<String> lines) {
        for (int k = 0; k < count; k++) {
            final double[] s = boxes.get(r.nextInt(boxes.size()));
            final double[] t = boxes.get(r.nextInt(boxes.size()));
            if (s == t) {
                continue;
            }
            final double sy = s[1] + 12 + r.nextInt((int) Math.max(1, (s[3] - 12) / 12)) * 12;
            final double ty = t[1] + 12 + r.nextInt((int) Math.max(1, (t[3] - 12) / 12)) * 12;
            lines.add("conn " + (s[0] + s[2]) + " " + sy + " " + ConnDirFlag.ConnDirRight + " " + t[0] + " " + ty + " "
                    + ConnDirFlag.ConnDirLeft);
        }
    }

    /** Creates a router with this scene (call {@code processTransaction()} to route). */
    public Loaded load() {
        final Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setTransactionUse(true);
        final List<ShapeRef> shapes = new ArrayList<>();
        final List<ConnRef> connectors = new ArrayList<>();
        for (String line : lines) {
            final String[] t = line.trim().split("\\s+");
            if (t[0].isEmpty() || t[0].startsWith("#")) {
                continue;
            }
            switch (t[0]) {
                case "param" -> router.setRoutingParameter(Router.RoutingParameter.valueOf(t[1]), d(t[2]));
                case "option" -> router.setRoutingOption(Router.RoutingOption.valueOf(t[1]), Boolean.parseBoolean(t[2]));
                case "shape" -> shapes.add(new ShapeRef(router,
                        new Rectangle(new Point(d(t[1]), d(t[2])), new Point(d(t[3]), d(t[4])))));
                case "conn" -> connectors.add(new ConnRef(router,
                        new ConnEnd(new Point(d(t[1]), d(t[2])), Integer.parseInt(t[3])),
                        new ConnEnd(new Point(d(t[4]), d(t[5])), Integer.parseInt(t[6]))));
                default -> throw new IllegalArgumentException("Unknown scene line: " + line);
            }
        }
        return new Loaded(router, shapes, connectors);
    }

    /** Moves the middle shape by (24, 12), as a user drag would. */
    public static void moveMiddleShape(final Loaded scene) {
        final ShapeRef shape = scene.shapes().get(scene.shapes().size() / 2);
        final Polygon p = shape.polygon();
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < p.size(); i++) {
            minX = Math.min(minX, p.at(i).x);
            minY = Math.min(minY, p.at(i).y);
            maxX = Math.max(maxX, p.at(i).x);
            maxY = Math.max(maxY, p.at(i).y);
        }
        scene.router().moveShape(shape, new Rectangle(new Point(minX + 24, minY + 12), new Point(maxX + 24, maxY + 12)));
    }

    private static double d(final String s) {
        return Double.parseDouble(s);
    }
}
