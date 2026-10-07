package io.github.eckig.libavoid.vpsc;

/**
 * Scratch arrays for the iterative DFS traversals of {@link Block}. One instance is shared by all blocks of a
 * {@link Blocks} container, so the arrays are allocated once per solver instead of once per block (blocks are created
 * and split very frequently).
 */
final class Scratch {

    private static final int INITIAL_CAPACITY = 64;

    // DFS stack
    Variable[]   sVar      = new Variable[INITIAL_CAPACITY];
    Variable[]   sParent   = new Variable[INITIAL_CAPACITY];
    Constraint[] sCon      = new Constraint[INITIAL_CAPACITY];
    boolean[]    sOut      = new boolean[INITIAL_CAPACITY];

    // visit order (used by compute_dfdv_impl and split_path)
    Variable[]   order     = new Variable[INITIAL_CAPACITY];
    int[]        parentIdx = new int[INITIAL_CAPACITY];
    Constraint[] viaCon    = new Constraint[INITIAL_CAPACITY];
    boolean[]    viaOut    = new boolean[INITIAL_CAPACITY];

    // dfdv values (used by compute_dfdv_impl)
    double[]     dfdv      = new double[INITIAL_CAPACITY];

    /**
     * Ensures all arrays can hold at least {@code cap} entries.
     */
    void ensure(int cap) {
        // the DFS methods grow the stack arrays in place, some only partially: re-align them here
        if (order.length >= cap && sVar.length >= cap && sParent.length == sVar.length
                && sCon.length == sVar.length && sOut.length == sVar.length) {
            return;
        }
        if (order.length < cap) {
            final int newCap = Math.max(cap, order.length * 2);
            order     = new Variable[newCap];
            parentIdx = new int[newCap];
            viaCon    = new Constraint[newCap];
            viaOut    = new boolean[newCap];
            dfdv      = new double[Math.max(newCap, dfdv.length)];
        }
        // re-align the stack arrays (some DFS methods grow only sVar/sParent in place)
        final int stackCap = Math.max(cap, sVar.length);
        sVar      = sVar.length == stackCap ? sVar : new Variable[stackCap];
        sParent   = new Variable[stackCap];
        sCon      = new Constraint[stackCap];
        sOut      = new boolean[stackCap];
    }
}
