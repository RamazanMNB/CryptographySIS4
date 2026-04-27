package quantum.quantum;

import java.util.*;

/**
 * Cascade Error Correction Algorithm (Brassard & Salvail, 1994).
 *
 * Cascade is an interactive error correction protocol used in real QKD systems.
 * It iteratively finds and corrects bit errors in the sifted key through
 * parity checks and binary search (BISECT).
 *
 * Algorithm Overview:
 *   Pass 1: Block size k1 ≈ 0.73 / errorRate
 *   Pass 2: k2 = 2 * k1
 *   Pass 3: k3 = 4 * k1
 *   Pass 4: k4 = 8 * k1
 *
 *   For each pass:
 *   1. Randomly permute the key
 *   2. Split into blocks of size k
 *   3. Compare parities — if mismatch, run BISECT to find the error
 *   4. After fixing an error, cascade back to check all previous passes
 *      because fixing one error can reveal another in a previous block
 */
public class CascadeCorrection {

    private static final int NUM_PASSES = 4;

    private final List<Integer> alice;   // reference (Alice's key)
    private final List<Integer> bob;     // key to be corrected (Bob's key)
    private final int keyLen;
    private final Random rng = new Random();

    private int errorsBefore;
    private int errorsAfter;

    /** Permutations used in each pass (needed for cascade back-tracking). */
    private final int[][] perms = new int[NUM_PASSES][];

    /** Bob's corrected key in each pass permutation order. */
    private final int[][] bobPerm = new int[NUM_PASSES][];

    public CascadeCorrection(List<Integer> alice, List<Integer> bob) {
        this.alice  = new ArrayList<>(alice);
        this.bob    = new ArrayList<>(bob);
        this.keyLen = alice.size();
    }

    // =========================================================================
    // PUBLIC
    // =========================================================================

    /**
     * Runs Cascade and returns the corrected version of Bob's key.
     */
    public List<Integer> correct() {
        errorsBefore = countErrors(alice, bob);

        // Estimate error rate (add small floor to avoid division by zero)
        double estRate = errorsBefore > 0
                ? (double) errorsBefore / keyLen
                : 0.05;

        // Initial block size: k1 = 0.73 / p (practical formula)
        int k1 = Math.max(2, (int) Math.ceil(0.73 / estRate));

        // Working copy of Bob's key (index = original position)
        int[] bobKey = new int[keyLen];
        for (int i = 0; i < keyLen; i++) bobKey[i] = bob.get(i);

        for (int pass = 0; pass < NUM_PASSES; pass++) {
            int blockSize = k1 * (1 << pass);  // k1, 2k1, 4k1, 8k1
            int[] perm = randomPermutation(keyLen);
            perms[pass] = perm;

            // Apply permutation to create permuted view of keys
            int[] alicePerm = applyPerm(alice, perm);
            bobPerm[pass]   = applyPermArr(bobKey, perm);

            // Split into blocks and check parities
            for (int start = 0; start < keyLen; start += blockSize) {
                int end = Math.min(start + blockSize, keyLen);
                int aliceParity = parity(alicePerm, start, end);
                int bobParity   = parity(bobPerm[pass], start, end);

                if (aliceParity != bobParity) {
                    // Error found — BISECT to locate it
                    int errorPos = bisect(alicePerm, bobPerm[pass], start, end);
                    if (errorPos >= 0) {
                        // Flip the bit in the permuted array
                        bobPerm[pass][errorPos] ^= 1;
                        // Map back to original position and fix in bobKey
                        int origPos = perm[errorPos];
                        bobKey[origPos] ^= 1;
                        // Cascade: re-check blocks in all previous passes
                        cascade(bobKey, origPos, pass);
                    }
                }
            }

            // Sync bobPerm[pass] with corrected bobKey
            bobPerm[pass] = applyPermArr(bobKey, perm);
        }

        // Build result list
        List<Integer> result = new ArrayList<>(keyLen);
        for (int bit : bobKey) result.add(bit);

        errorsAfter = countErrors(alice, result);
        return result;
    }

    // =========================================================================
    // CASCADE BACK-TRACKING
    // =========================================================================

    /**
     * After fixing an error at origPos, re-examine all blocks in passes 0..currentPass-1
     * that contain origPos. If a block now has a parity error, fix it via BISECT.
     */
    private void cascade(int[] bobKey, int origPos, int currentPass) {
        for (int p = 0; p < currentPass; p++) {
            // Find where origPos maps to in this pass's permutation
            int permPos = -1;
            for (int i = 0; i < perms[p].length; i++) {
                if (perms[p][i] == origPos) { permPos = i; break; }
            }
            if (permPos < 0) continue;

            int k = (int) Math.pow(2, p);  // block size for pass p
            // Approx block start (simplified)
            int blockStart = (permPos / k) * k;
            int blockEnd   = Math.min(blockStart + k, keyLen);

            // Rebuild permuted arrays with current bobKey
            int[] alicePerm = applyPerm(alice, perms[p]);
            bobPerm[p]      = applyPermArr(bobKey, perms[p]);

            int aliceParity = parity(alicePerm, blockStart, blockEnd);
            int bobParity   = parity(bobPerm[p], blockStart, blockEnd);

            if (aliceParity != bobParity) {
                int ePos = bisect(alicePerm, bobPerm[p], blockStart, blockEnd);
                if (ePos >= 0) {
                    bobPerm[p][ePos] ^= 1;
                    int orig2 = perms[p][ePos];
                    bobKey[orig2] ^= 1;
                    // Recurse (limited depth to avoid infinite loops)
                    if (currentPass > 1) cascade(bobKey, orig2, p);
                }
            }
        }
    }

    // =========================================================================
    // BISECT (binary search for error within block)
    // =========================================================================

    /**
     * Binary search within [start, end) to find the position of the single error.
     * Returns -1 if no error found (parity matched after all, shouldn't happen).
     */
    private int bisect(int[] alice, int[] bob, int start, int end) {
        if (end - start == 1) {
            return alice[start] != bob[start] ? start : -1;
        }
        int mid = (start + end) / 2;
        int aliceLeft = parity(alice, start, mid);
        int bobLeft   = parity(bob,   start, mid);
        if (aliceLeft != bobLeft) {
            return bisect(alice, bob, start, mid);
        } else {
            return bisect(alice, bob, mid, end);
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private int parity(int[] arr, int start, int end) {
        int p = 0;
        for (int i = start; i < end; i++) p ^= arr[i];
        return p;
    }

    private int[] randomPermutation(int n) {
        Integer[] arr = new Integer[n];
        for (int i = 0; i < n; i++) arr[i] = i;
        List<Integer> list = Arrays.asList(arr);
        Collections.shuffle(list, rng);
        int[] perm = new int[n];
        for (int i = 0; i < n; i++) perm[i] = list.get(i);
        return perm;
    }

    private int[] applyPerm(List<Integer> src, int[] perm) {
        int[] out = new int[perm.length];
        for (int i = 0; i < perm.length; i++) out[i] = src.get(perm[i]);
        return out;
    }

    private int[] applyPermArr(int[] src, int[] perm) {
        int[] out = new int[perm.length];
        for (int i = 0; i < perm.length; i++) out[i] = src[perm[i]];
        return out;
    }

    private int countErrors(List<Integer> a, List<Integer> b) {
        int count = 0;
        for (int i = 0; i < Math.min(a.size(), b.size()); i++)
            if (!a.get(i).equals(b.get(i))) count++;
        return count;
    }

    private int countErrors(List<Integer> a, int[] b) {
        int count = 0;
        for (int i = 0; i < Math.min(a.size(), b.length); i++)
            if (a.get(i) != b[i]) count++;
        return count;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public int getErrorsBefore() { return errorsBefore; }
    public int getErrorsAfter()  { return errorsAfter; }
}
