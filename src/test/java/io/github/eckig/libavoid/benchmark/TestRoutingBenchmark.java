package io.github.eckig.libavoid.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.github.eckig.libavoid.ConnRef;

/**
 * Routing benchmark: initial routing and re-routing after moving a node, for a set of scenes. Not part of the normal
 * build, run with {@code mvn -Pbenchmark test}.
 *
 * <p>
 * Results (best of several runs after warm-up) are printed and written to {@code target/benchmark/results.txt}. The
 * final routes of every scene are written to {@code target/benchmark/routes-<scene>.txt}, so two versions can be
 * compared byte for byte (copy the folder before switching versions, then diff).
 * </p>
 *
 * <p>
 * To add a real-world scene, dump it from the application in the format described in {@link Scene} and put the file into
 * {@code src/test/resources/benchmark/}.
 * </p>
 */
@Tag("benchmark")
class TestRoutingBenchmark {

    private static final int WARMUP = Integer.getInteger("benchmark.warmup", 2);
    private static final int RUNS = Integer.getInteger("benchmark.runs", 5);

    private static List<Scene> scenes() throws IOException {
        final List<Scene> scenes = new ArrayList<>();
        scenes.add(Scene.resource("graph-editor-138-nodes.txt"));
        scenes.add(Scene.grid(12, 8, 250, 3));
        scenes.add(Scene.grid(16, 10, 500, 3));
        scenes.add(Scene.random(40, 80, 1));
        scenes.add(Scene.random(60, 150, 2));
        return scenes;
    }

    @Test
    void run() throws IOException {
        final Path out = Path.of("target", "benchmark");
        Files.createDirectories(out);
        final List<String> report = new ArrayList<>();
        report.add(String.format("%-28s %8s %8s %12s %12s", "scene", "shapes", "conns", "initial ms", "move ms"));
        for (Scene scene : scenes()) {
            long bestInitial = Long.MAX_VALUE;
            long bestMove = Long.MAX_VALUE;
            Scene.Loaded last = null;
            for (int i = 0; i < WARMUP + RUNS; i++) {
                final Scene.Loaded loaded = scene.load();
                final long t0 = System.nanoTime();
                loaded.router().processTransaction();
                final long t1 = System.nanoTime();
                Scene.moveMiddleShape(loaded);
                final long t2 = System.nanoTime();
                loaded.router().processTransaction();
                final long t3 = System.nanoTime();
                if (i >= WARMUP) {
                    bestInitial = Math.min(bestInitial, t1 - t0);
                    bestMove = Math.min(bestMove, t3 - t2);
                }
                last = loaded;
            }
            report.add(String.format("%-28s %8d %8d %12d %12d", scene.name(), last.shapes().size(),
                    last.connectors().size(), bestInitial / 1_000_000, bestMove / 1_000_000));
            final List<String> routes = new ArrayList<>();
            for (ConnRef c : last.connectors()) {
                routes.add(c.displayRoute().ps.toString());
            }
            Files.write(out.resolve("routes-" + scene.name().replace(".txt", "") + ".txt"), routes);
        }
        report.forEach(System.out::println);
        Files.write(out.resolve("results.txt"), report);
    }
}
