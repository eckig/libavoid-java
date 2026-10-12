package io.github.eckig.libavoid.vpsc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Verifies the choice of the split constraint ({@link Block#findMinLMBetween}) and that the solver keeps all
 * constraints satisfied.
 */
class TestBlockSplitPath {

    /** Path step: the constraint and whether it is traversed in "out" direction (left to right). */
    private record Step(Constraint constraint, boolean out) {
    }

    /** Reference: the path of active constraints from v to r, or null if not reachable. */
    private static List<Step> path(final Variable v, final Variable r, final Variable u) {
        if (v == r) {
            return new ArrayList<>();
        }
        for (Constraint c : v.activeOut) {
            if (c.right != u) {
                final List<Step> rest = path(c.right, r, v);
                if (rest != null) {
                    rest.addFirst(new Step(c, true));
                    return rest;
                }
            }
        }
        for (Constraint c : v.activeIn) {
            if (c.left != u) {
                final List<Step> rest = path(c.left, r, v);
                if (rest != null) {
                    rest.addFirst(new Step(c, false));
                    return rest;
                }
            }
        }
        return null;
    }

    /** Expected split constraint: the non-equality "out" constraint on the path closest to rv. */
    private static Constraint expectedSplit(final Variable lv, final Variable rv) {
        final List<Step> path = path(lv, rv, null);
        if (path == null) {
            return null;
        }
        for (int i = path.size() - 1; i >= 0; i--) {
            final Step step = path.get(i);
            if (step.out() && !step.constraint().equality) {
                return step.constraint();
            }
        }
        return null;
    }

    private static List<Variable> variables(final Random random, final int n) {
        final List<Variable> vs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            vs.add(new Variable(Variable.Id.freeSegmentID, random.nextDouble() * 100,
                    Variable.Weight.values()[random.nextInt(3)]));
        }
        return vs;
    }

    /**
     * Random problems: the split constraint of random variable pairs of each resulting block matches the reference.
     */
    @Test
    void splitConstraintMatchesReference() {
        final Random random = new Random(42);
        int compared = 0;
        int nonTrivial = 0;
        for (int round = 0; round < 300; round++) {
            final int n = 4 + random.nextInt(20);
            final List<Variable> vs = variables(random, n);
            final List<Constraint> cs = new ArrayList<>();
            for (int i = 0; i < n * 2; i++) {
                final int a = random.nextInt(n);
                final int b = random.nextInt(n);
                if (a != b) {
                    cs.add(new Constraint(vs.get(a), vs.get(b), random.nextDouble() * 10, random.nextInt(10) == 0));
                }
            }
            try {
                new IncSolver(vs, cs).solve();
            } catch (RuntimeException e) {
                continue; // random problems may contain cycles that cannot be satisfied
            }

            final Set<Block> blocks = new HashSet<>();
            for (Variable v : vs) {
                blocks.add(v.block);
            }
            for (Block b : blocks) {
                if (b.vars.size() < 2) {
                    continue;
                }
                for (int k = 0; k < 5; k++) {
                    final Variable lv = b.vars.get(random.nextInt(b.vars.size()));
                    final Variable rv = b.vars.get(random.nextInt(b.vars.size()));
                    if (lv == rv) {
                        continue;
                    }
                    final Constraint expected = expectedSplit(lv, rv);
                    assertSame(expected, b.findMinLMBetween(lv, rv), "split constraint between " + lv + " and " + rv);
                    compared++;
                    if (expected != null) {
                        nonTrivial++;
                    }
                }
            }
        }
        assertTrue(compared > 500, "too few comparisons: " + compared);
        assertTrue(nonTrivial > 100, "too few non-trivial comparisons: " + nonTrivial);
    }

    /**
     * Random satisfiable problems (constraints only from lower to higher index, so no cycles): no constraint may be
     * marked unsatisfiable or be violated by the final positions.
     */
    @Test
    void satisfiableProblemsAreSolvedWithoutViolations() {
        final Random random = new Random(7);
        for (int round = 0; round < 200; round++) {
            final int n = 10 + random.nextInt(100);
            final List<Variable> vs = variables(random, n);
            final List<Constraint> cs = new ArrayList<>();
            for (int i = 0; i + 1 < n; i++) {
                cs.add(new Constraint(vs.get(i), vs.get(i + 1), random.nextDouble() * 4));
            }
            for (int i = 0; i < n; i++) {
                final int a = random.nextInt(n);
                final int b = random.nextInt(n);
                if (a < b) {
                    cs.add(new Constraint(vs.get(a), vs.get(b), random.nextDouble() * 20));
                }
            }
            new IncSolver(vs, cs).solve();
            for (Constraint c : cs) {
                assertFalse(c.unsatisfiable, "marked unsatisfiable: " + c);
                assertTrue(c.right.finalPosition - c.left.finalPosition >= c.gap - 1e-6, "violated: " + c);
            }
        }
    }

    /**
     * The path from lv to rv ends with an "in" constraint, but contains an "out" constraint: that one must be split.
     * Returning no constraint would wrongly mark the violated constraint as unsatisfiable.
     */
    @Test
    void pathEndingWithInConstraintStillFindsOutConstraint() {
        final Variable a = new Variable(Variable.Id.freeSegmentID, 0, Variable.Weight.strongerWeight);
        final Variable b = new Variable(Variable.Id.freeSegmentID, 0, Variable.Weight.strongerWeight);
        final Variable c = new Variable(Variable.Id.freeSegmentID, 0, Variable.Weight.strongerWeight);
        // a --ab--> b <--cb-- c : path a -> b (out), b -> c (in)
        final Constraint ab = new Constraint(a, b, 5);
        final Constraint cb = new Constraint(c, b, 5);
        new IncSolver(new ArrayList<>(List.of(a, b, c)), new ArrayList<>(List.of(ab, cb))).solve();
        assertSame(a.block, c.block, "all variables in one block");

        assertSame(ab, a.block.findMinLMBetween(a, c));
        assertFalse(ab.unsatisfiable || cb.unsatisfiable);
    }

    /**
     * Only "in" or equality constraints on the path: nothing can be split.
     */
    @Test
    void noCandidateOnPath() {
        final Variable a = new Variable(Variable.Id.freeSegmentID, 0, Variable.Weight.strongerWeight);
        final Variable b = new Variable(Variable.Id.freeSegmentID, 0, Variable.Weight.strongerWeight);
        final Constraint ba = new Constraint(b, a, 5);
        new IncSolver(new ArrayList<>(List.of(a, b)), new ArrayList<>(List.of(ba))).solve();
        assertSame(a.block, b.block);

        assertNull(a.block.findMinLMBetween(a, b), "path a -> b only traverses ba in 'in' direction");
        assertNotNull(a.block.findMinLMBetween(b, a));
    }

    /**
     * The last "out" constraint on the path is an equality: the one before it must be split.
     */
    @Test
    void equalityConstraintIsNotSplit() {
        final Variable a = new Variable(Variable.Id.freeSegmentID, 0, Variable.Weight.strongerWeight);
        final Variable b = new Variable(Variable.Id.freeSegmentID, 0, Variable.Weight.strongerWeight);
        final Variable c = new Variable(Variable.Id.freeSegmentID, 0, Variable.Weight.strongerWeight);
        final Constraint ab = new Constraint(a, b, 5);
        final Constraint bc = new Constraint(b, c, 5, true);
        new IncSolver(new ArrayList<>(List.of(a, b, c)), new ArrayList<>(List.of(ab, bc))).solve();
        assertSame(a.block, c.block);

        assertSame(ab, a.block.findMinLMBetween(a, c));
    }

    /**
     * Two solvers and a block larger than the initial scratch capacity: the shared scratch arrays must grow.
     */
    @Test
    void largeBlocksAndIndependentSolvers() {
        for (int s = 0; s < 2; s++) {
            final List<Variable> vs = new ArrayList<>();
            final List<Constraint> cs = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                vs.add(new Variable(Variable.Id.freeSegmentID, 0, Variable.Weight.strongWeight));
                if (i > 0) {
                    cs.add(new Constraint(vs.get(i - 1), vs.get(i), 1));
                }
            }
            new IncSolver(vs, cs).solve();
            assertEquals(1, new HashSet<>(vs.stream().map(v -> v.block).toList()).size(), "one block expected");
            for (Constraint c : cs) {
                assertFalse(c.unsatisfiable);
                assertTrue(c.right.finalPosition - c.left.finalPosition >= c.gap - 1e-6);
            }
        }
    }

    /**
     * Unifying nudging adds constraints between solver passes and calls reset() (C++ builds a new solver per pass):
     * after reset() the solver must be wired up like a new solver, including the constraints added in between.
     */
    @Test
    void resetIncludesConstraintsAddedAfterConstruction() {
        final Variable a = new Variable(Variable.Id.freeSegmentID, 0, Variable.Weight.strongWeight);
        final Variable b = new Variable(Variable.Id.freeSegmentID, 10, Variable.Weight.strongWeight);
        final Variable c = new Variable(Variable.Id.freeSegmentID, 20, Variable.Weight.strongWeight);
        final List<Variable> vs = new ArrayList<>(List.of(a, b, c));
        final List<Constraint> cs = new ArrayList<>(List.of(new Constraint(a, b, 5)));
        final IncSolver solver = new IncSolver(vs, cs);
        solver.solve();

        final Constraint added = new Constraint(b, c, 0, true);
        cs.add(added);
        solver.reset();

        assertTrue(b.out.contains(added) && c.in.contains(added), "added constraint not wired");
        solver.solve();
        assertEquals(b.finalPosition, c.finalPosition, 1e-6);
    }

    private static double cost(final List<Variable> vs) {
        double cost = 0;
        for (Variable v : vs) {
            final double d = v.finalPosition - v.desiredPosition;
            cost += v.weight.weight * d * d;
        }
        return cost;
    }

    private static List<Constraint> sparseConstraints(final Random random, final List<Variable> vs) {
        final List<Constraint> cs = new ArrayList<>();
        for (int i = 0; i < vs.size(); i++) {
            final int a = random.nextInt(vs.size());
            final int b = random.nextInt(vs.size());
            if (a < b) {
                cs.add(new Constraint(vs.get(a), vs.get(b), random.nextDouble() * 4));
            }
        }
        return cs;
    }

    /**
     * addConstraint() continues from the current solution (as unifying nudging does): the result satisfies all
     * constraints and has the same cost as a new solver over all constraints, within the solver's tolerance. The
     * positions themselves may differ where the cost is almost flat (low weights).
     */
    @Test
    void addConstraintContinuesToSameOptimum() {
        int compared = 0;
        for (int seed = 0; seed < 1000; seed++) {
            final Random random = new Random(seed);
            final int n = 5 + random.nextInt(25);
            final int a = random.nextInt(n - 1);
            final int b = a + 1 + random.nextInt(n - 1 - a);
            final long constraintSeed = random.nextLong();

            // from scratch, only feasible problems (as unifying only keeps satisfiable equalities)
            final List<Variable> vs = variables(new Random(seed * 31L), n);
            final List<Constraint> cs = sparseConstraints(new Random(constraintSeed), vs);
            cs.add(new Constraint(vs.get(a), vs.get(b), 0, true));
            try {
                new IncSolver(vs, cs).solve();
            } catch (RuntimeException e) {
                continue;
            }
            if (!satisfiesAll(cs)) {
                continue;
            }
            final double freshCost = cost(vs);

            // incrementally: solve without the equality, then add it
            final List<Variable> vs2 = variables(new Random(seed * 31L), n);
            final List<Constraint> cs2 = sparseConstraints(new Random(constraintSeed), vs2);
            final IncSolver incremental = new IncSolver(vs2, cs2);
            incremental.solve();
            cs2.add(new Constraint(vs2.get(a), vs2.get(b), 0, true));
            incremental.addConstraint(cs2.getLast());
            incremental.solve();

            assertTrue(satisfiesAll(cs2), "seed " + seed + ": constraint violated");
            assertEquals(freshCost, cost(vs2), 1e-3, "seed " + seed + ": cost differs");
            compared++;
        }
        assertTrue(compared > 500, "too few comparisons: " + compared);
    }

    /** Result of running a unifying-like sequence of added equalities. */
    private record ChainResult(List<Boolean> accepted, double cost) {
    }

    /**
     * Runs a unifying-like sequence: equalities are added one after another, an equality that cannot be satisfied is
     * removed again. {@code incremental} continues the solver after an accepted equality (as ImproveOrthogonalRoutes
     * does now), otherwise the solver is reset each time (previous behaviour). The result is the state after the last
     * accepted equality, as used by routing (the state of a rejected, infeasible pass is discarded).
     */
    private static ChainResult runChain(final long seed, final boolean incremental) {
        final Random random = new Random(seed);
        final int n = 6 + random.nextInt(20);
        final List<Variable> vs = variables(new Random(seed * 17L), n);
        final List<Constraint> cs = sparseConstraints(random, vs);
        final IncSolver solver = new IncSolver(vs, cs);
        solver.solve();
        final List<Boolean> accepted = new ArrayList<>();
        boolean previousRejected = false;
        for (int k = 0; k < 6; k++) {
            final int a = random.nextInt(n - 1);
            final int b = a + 1 + random.nextInt(n - 1 - a);
            cs.add(new Constraint(vs.get(a), vs.get(b), 0, true));
            final boolean incrementalPass = incremental && !previousRejected;
            boolean ok = attempt(solver, cs, incrementalPass);
            if (incrementalPass && (!ok || solver.markedUnsatisfiableInequality())) {
                // as ImproveOrthogonalRoutes: only clean incremental passes are kept, others are repeated from scratch
                solver.undoUnsatisfiableMarks();
                ok = attempt(solver, cs, false);
            }
            accepted.add(ok);
            previousRejected = !ok;
            if (!ok) {
                cs.removeLast();
            }
        }
        if (previousRejected) {
            // as in routing: after a rejection the next pass resets the solver without the rejected equality
            solver.reset();
            solver.solve();
        }
        return new ChainResult(accepted, cost(vs));
    }

    private static boolean attempt(final IncSolver solver, final List<Constraint> cs, final boolean incremental) {
        try {
            if (incremental) {
                solver.addConstraint(cs.getLast());
            } else {
                solver.reset();
            }
            solver.solve();
            return satisfiesAll(cs);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Continuing incrementally makes the same accept/reject decisions and reaches the same cost as resetting the
     * solver for every added equality, including sequences with rejections.
     */
    @Test
    void chainOfAddedConstraintsMatchesResetEachTime() {
        int rejections = 0;
        for (long seed = 0; seed < 300; seed++) {
            final ChainResult incremental = runChain(seed, true);
            final ChainResult reset = runChain(seed, false);
            assertEquals(reset.accepted(), incremental.accepted(), "seed " + seed + ": decisions differ");
            assertEquals(reset.cost(), incremental.cost(), 1e-3, "seed " + seed + ": cost differs");
            rejections += (int) reset.accepted().stream().filter(x -> !x).count();
        }
        assertTrue(rejections > 50, "too few rejections exercised: " + rejections);
    }

    private static boolean satisfiesAll(final List<Constraint> cs) {
        for (Constraint c : cs) {
            final double slack = c.right.finalPosition - c.gap - c.left.finalPosition;
            if (c.equality ? Math.abs(slack) > 1e-4 : slack < -1e-4) {
                return false;
            }
        }
        return true;
    }
}
