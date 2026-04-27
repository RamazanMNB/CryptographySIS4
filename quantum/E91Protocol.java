package quantum.quantum;

import java.util.*;

/**
 * E91 Quantum Key Distribution Protocol (Ekert, 1991).
 *
 * Unlike BB84 which uses prepared photons, E91 uses ENTANGLED PAIRS.
 * A source emits pairs of photons in the singlet state — measuring
 * one instantly determines the correlated result of the other.
 *
 * Security is guaranteed by violation of Bell's inequality (CHSH):
 *   |S| ≤ 2  → classical/eavesdropped channel
 *   |S| ≈ 2√2 ≈ 2.828 → genuine quantum entanglement, channel is secure
 *
 * Measurement angles used (degrees):
 *   Alice: 0°,  45°,  90°
 *   Bob:   45°, 90°, 135°
 *
 * Key bits come from the subset where Alice and Bob chose the SAME angle.
 */
public class E91Protocol {

    // ── Measurement angle sets ────────────────────────────────────────────────
    // Alice measures at 0°, 45°, 90°
    private static final int[] ALICE_ANGLES = {0, 45, 90};
    // Bob measures at 45°, 90°, 135°
    private static final int[] BOB_ANGLES   = {45, 90, 135};

    private final int     numPairs;
    private final boolean eveEnabled;
    private final Random  rng = new Random();

    public E91Protocol(int numPairs, boolean eveEnabled) {
        this.numPairs   = numPairs;
        this.eveEnabled = eveEnabled;
    }

    // =========================================================================
    // PUBLIC: Run full E91 protocol
    // =========================================================================

    public E91Result run() {
        E91Result result = new E91Result();
        result.totalPairs  = numPairs;
        result.evePresent  = eveEnabled;

        // Each entry: [aliceAngle, bobAngle, aliceBit, bobBit]
        List<int[]> measurements = new ArrayList<>(numPairs);

        int aliceAngleIdx, bobAngleIdx;
        int aliceBit, bobBit;

        for (int i = 0; i < numPairs; i++) {
            // Both parties randomly choose a measurement angle
            aliceAngleIdx = rng.nextInt(ALICE_ANGLES.length);
            bobAngleIdx   = rng.nextInt(BOB_ANGLES.length);
            int aAngle = ALICE_ANGLES[aliceAngleIdx];
            int bAngle = BOB_ANGLES[bobAngleIdx];

            // Simulate entangled measurement
            if (eveEnabled) {
                // Eve disrupts entanglement — measurements become independent
                int[] bits = eveInterceptMeasure(aAngle, bAngle);
                aliceBit = bits[0];
                bobBit   = bits[1];
            } else {
                int[] bits = measureEntangledPair(aAngle, bAngle);
                aliceBit = bits[0];
                bobBit   = bits[1];
            }

            measurements.add(new int[]{aAngle, bAngle, aliceBit, bobBit});
        }

        // ── Sift key: keep pairs where angles are equal ────────────────────────
        List<Integer> aliceKey = new ArrayList<>();
        List<Integer> bobKey   = new ArrayList<>();
        // ── CHSH test pairs: angles differ (used for Bell test) ────────────────
        List<int[]> bellPairs  = new ArrayList<>();

        for (int[] m : measurements) {
            int aAngle = m[0], bAngle = m[1], aBit = m[2], bBit = m[3];
            if (aAngle == bAngle) {
                // Same angle → correlated bits → key material
                // In singlet state: perfect anti-correlation, so flip Bob's bit
                aliceKey.add(aBit);
                bobKey.add(1 - bBit); // anti-correlation → flip to agree
            } else {
                // Different angles → used for Bell inequality test
                bellPairs.add(m);
            }
        }

        result.siftedKeyLength = aliceKey.size();
        result.aliceFinalKey   = aliceKey;
        result.bobFinalKey     = bobKey;

        // ── Bell inequality (CHSH) check ──────────────────────────────────────
        result.chshValue       = computeCHSH(bellPairs);
        result.bellTestPassed  = Math.abs(result.chshValue) > 2.0;
        // If CHSH ≤ 2.0 → classical correlations only → eavesdropper detected
        result.aborted         = !result.bellTestPassed;

        // ── Key efficiency ────────────────────────────────────────────────────
        result.keyEfficiency = (double) result.siftedKeyLength / numPairs * 100.0;

        return result;
    }

    // =========================================================================
    // QUANTUM MECHANICS SIMULATION
    // =========================================================================

    /**
     * Simulates measuring an entangled singlet-state pair.
     *
     * Singlet state: |ψ⁻⟩ = (|01⟩ - |10⟩) / √2
     *
     * The correlation function for the singlet state is:
     *   E(α, β) = −cos(α − β)
     *
     * Alice measures at angle α, Bob at angle β.
     * The joint probability of (aliceBit=0, bobBit=1) = sin²((α−β)/2)
     */
    private int[] measureEntangledPair(int aliceAngleDeg, int bobAngleDeg) {
        double deltaRad = Math.toRadians(aliceAngleDeg - bobAngleDeg);

        // Alice measures first — random 50/50
        int aliceBit = rng.nextBoolean() ? 0 : 1;

        // Bob's outcome is correlated via singlet-state statistics
        // P(same result) = sin²(Δ/2)
        double probSame = Math.pow(Math.sin(deltaRad / 2), 2);
        int bobBit;
        if (rng.nextDouble() < probSame) {
            bobBit = aliceBit;          // same result
        } else {
            bobBit = 1 - aliceBit;      // opposite result
        }

        return new int[]{aliceBit, bobBit};
    }

    /**
     * When Eve intercepts, she breaks entanglement.
     * Measurements become INDEPENDENT classical random bits
     * → CHSH value collapses to ≤ 2.0 (detectable).
     */
    private int[] eveInterceptMeasure(int aliceAngleDeg, int bobAngleDeg) {
        // Eve measures in a random basis, destroys quantum correlations
        int aliceBit = rng.nextInt(2);
        int bobBit   = rng.nextInt(2); // completely independent
        return new int[]{aliceBit, bobBit};
    }

    // =========================================================================
    // BELL INEQUALITY (CHSH)
    // =========================================================================

    /**
     * Computes the CHSH parameter S from the test pairs.
     *
     * CHSH: S = E(a,b) − E(a,b') + E(a',b) + E(a',b')
     * where a=0°,a'=45°, b=45°,b'=90° (one of the valid combinations)
     *
     * |S| ≤ 2       → classical world (or eavesdropped)
     * |S| ≤ 2√2     → quantum world (genuine entanglement)
     * |S| ≈ 2√2     → maximally entangled, perfectly secure
     */
    private double computeCHSH(List<int[]> bellPairs) {
        // E(a, b) = ⟨A·B⟩ where A,B ∈ {+1, −1}
        // Angle pairs for CHSH: (0,45), (0,135), (90,45), (90,135)
        Map<String, double[]> correlators = new HashMap<>();
        String[] keys = {"0_45", "0_135", "90_45", "90_135"};
        for (String k : keys) correlators.put(k, new double[]{0.0, 0.0}); // [sum, count]

        for (int[] m : bellPairs) {
            int aAngle = m[0], bAngle = m[1];
            int aBit   = m[2], bBit   = m[3];
            String key = aAngle + "_" + bAngle;
            if (correlators.containsKey(key)) {
                // Convert bits {0,1} → spin values {+1,−1}
                int A = (aBit == 0) ? +1 : -1;
                int B = (bBit == 0) ? +1 : -1;
                double[] acc = correlators.get(key);
                acc[0] += A * B;
                acc[1] += 1;
            }
        }

        // E(a,b) = sum(A*B) / count
        double e0_45   = expectation(correlators.get("0_45"));
        double e0_135  = expectation(correlators.get("0_135"));
        double e90_45  = expectation(correlators.get("90_45"));
        double e90_135 = expectation(correlators.get("90_135"));

        // S = E(a,b) − E(a,b') + E(a',b) + E(a',b')
        return e0_45 - e0_135 + e90_45 + e90_135;
    }

    private double expectation(double[] acc) {
        return acc[1] == 0 ? 0.0 : acc[0] / acc[1];
    }

    // =========================================================================
    // RESULT CLASS
    // =========================================================================

    public static class E91Result {
        public int           totalPairs;
        public int           siftedKeyLength;
        public List<Integer> aliceFinalKey;
        public List<Integer> bobFinalKey;
        public double        chshValue;
        public boolean       bellTestPassed;  // |S| > 2.0
        public boolean       aborted;
        public boolean       evePresent;
        public double        keyEfficiency;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("═══════════════════════════════════════\n");
            sb.append("          E91 Protocol Results\n");
            sb.append("═══════════════════════════════════════\n");
            sb.append(String.format("Total entangled pairs     : %d%n",   totalPairs));
            sb.append(String.format("Sifted key length         : %d bits%n", siftedKeyLength));
            sb.append(String.format("Key efficiency            : %.1f%%%n", keyEfficiency));
            sb.append(String.format("CHSH value |S|            : %.4f  (max quantum: %.4f)%n",
                    Math.abs(chshValue), 2.0 * Math.sqrt(2)));
            sb.append(String.format("Bell test passed (|S|>2)  : %s%n",   bellTestPassed ? "YES ✓" : "NO ✗"));
            sb.append(String.format("Eve present               : %s%n",   evePresent ? "YES" : "NO"));
            sb.append(String.format("Protocol status           : %s%n",
                    aborted ? "⚠ ABORTED (Bell inequality violated — Eve detected)"
                            : "✓ SUCCESS"));
            if (!aborted && aliceFinalKey != null && !aliceFinalKey.isEmpty()) {
                int preview = Math.min(64, aliceFinalKey.size());
                sb.append("Final key (first 64 bits) : ");
                for (int i = 0; i < preview; i++) sb.append(aliceFinalKey.get(i));
                sb.append("\n");
            }
            sb.append("═══════════════════════════════════════\n");
            return sb.toString();
        }
    }
}
