package quantum.quantum;

import quantum.quantum.CascadeCorrection;

import java.util.*;

/**
 * Full BB84 QKD Protocol implementation with step-by-step data capture.
 *
 * Phases:
 *   1. Quantum Transmission
 *   2. Basis Reconciliation
 *   3. Error Checking
 *   4. Privacy Amplification (XOR pairs, or Cascade if enabled)
 */
public class BB84Protocol {

    private final SimulationConfig cfg;
    private final Random rng = new Random();

    // Step data collected during run
    private final List<StepData> steps = new ArrayList<>();

    public BB84Protocol(SimulationConfig cfg) {
        this.cfg = cfg;
    }

    /** Convenience constructor. */
    public BB84Protocol(int numPhotons, boolean eveEnabled) {
        this(new SimulationConfig(numPhotons, eveEnabled));
    }

    // =========================================================================
    // PUBLIC: Run full protocol
    // =========================================================================

    public BB84Result run() {
        steps.clear();
        BB84Result result = new BB84Result();
        result.totalPhotons = cfg.numPhotons;
        result.evePresent   = cfg.eveEnabled;

        // ── Phase 1+2+3 in one pass (collect per-photon data) ─────────────────
        int[] aliceBits  = new int[cfg.numPhotons];
        String[] aliceBases = new String[cfg.numPhotons];
        int[] alicePols  = new int[cfg.numPhotons];
        String[] eveBases   = new String[cfg.numPhotons];
        int[] eveMeasured   = new int[cfg.numPhotons];
        int[] eveResentPols = new int[cfg.numPhotons];
        String[] bobBases   = new String[cfg.numPhotons];
        int[] bobMeasured   = new int[cfg.numPhotons];

        // ── Phase 1: Generate and transmit photons ────────────────────────────
        Photon[] photons = new Photon[cfg.numPhotons];
        for (int i = 0; i < cfg.numPhotons; i++) {
            int bit = rng.nextInt(2);
            Photon.Basis basis = randomBasis();
            photons[i]     = new Photon(bit, basis);
            aliceBits[i]   = bit;
            aliceBases[i]  = basis == Photon.Basis.RECTILINEAR ? "+" : "×";
            alicePols[i]   = photons[i].getPolarization();
        }

        // ── Eve intercepts ────────────────────────────────────────────────────
        if (cfg.eveEnabled) {
            int eveMatch = 0;
            for (int i = 0; i < cfg.numPhotons; i++) {
                Photon.Basis eveBasis = randomBasis();
                eveBases[i]    = eveBasis == Photon.Basis.RECTILINEAR ? "+" : "×";
                eveMeasured[i] = photons[i].measure(eveBasis);
                Photon resent  = new Photon(eveMeasured[i], eveBasis);
                eveResentPols[i] = resent.getPolarization();
                photons[i] = resent;   // replace with Eve's photon
                if (eveBasis == (aliceBases[i].equals("+") ? Photon.Basis.RECTILINEAR : Photon.Basis.DIAGONAL))
                    eveMatch++;
            }
            result.eveBasisMatchRate = (double) eveMatch / cfg.numPhotons;
        }

        // ── Bob measures ──────────────────────────────────────────────────────
        for (int i = 0; i < cfg.numPhotons; i++) {
            Photon.Basis bBasis = randomBasis();
            bobBases[i]    = bBasis == Photon.Basis.RECTILINEAR ? "+" : "×";
            int measured   = photons[i].measure(bBasis);
            if (cfg.channelNoise > 0 && rng.nextDouble() < cfg.channelNoise)
                measured = 1 - measured;
            bobMeasured[i] = measured;
        }

        // ── Phase 2: Basis Reconciliation ─────────────────────────────────────
        List<Integer> siftedAlice = new ArrayList<>();
        List<Integer> siftedBob   = new ArrayList<>();
        boolean[] inSifted = new boolean[cfg.numPhotons];
        int matchCount = 0;
        for (int i = 0; i < cfg.numPhotons; i++) {
            if (aliceBases[i].equals(bobBases[i])) {
                siftedAlice.add(aliceBits[i]);
                siftedBob.add(bobMeasured[i]);
                inSifted[i] = true;
                matchCount++;
            }
        }
        result.siftedKeyLength = siftedAlice.size();
        result.basisMatchRate  = (double) matchCount / cfg.numPhotons;

        // ── Phase 3: Error Checking ────────────────────────────────────────────
        int siftedLen  = siftedAlice.size();
        int sampleSize = (int) Math.ceil(siftedLen * cfg.errorCheckFraction);
        sampleSize     = Math.max(1, Math.min(sampleSize, siftedLen));

        List<Integer> siftedIndices = new ArrayList<>();
        for (int i = 0; i < siftedLen; i++) siftedIndices.add(i);
        Collections.shuffle(siftedIndices, rng);
        Set<Integer> checkSet = new HashSet<>(siftedIndices.subList(0, sampleSize));

        int errors = 0;
        for (int idx : checkSet)
            if (!siftedAlice.get(idx).equals(siftedBob.get(idx))) errors++;

        result.errorCheckSampleSize = sampleSize;
        result.errorsFound = errors;
        result.errorRate   = (double) errors / sampleSize;

        // Build final working key (without check bits)
        List<Integer> workAlice = new ArrayList<>();
        List<Integer> workBob   = new ArrayList<>();
        for (int i = 0; i < siftedLen; i++) {
            if (!checkSet.contains(i)) {
                workAlice.add(siftedAlice.get(i));
                workBob.add(siftedBob.get(i));
            }
        }

        // Track which photon indices were used for check
        boolean[] usedForCheck = new boolean[cfg.numPhotons];
        int siftedCounter = 0;
        Set<Integer> checkPhotonIndices = new HashSet<>();
        for (int i = 0; i < cfg.numPhotons; i++) {
            if (inSifted[i]) {
                if (checkSet.contains(siftedCounter)) {
                    usedForCheck[i] = true;
                    checkPhotonIndices.add(i);
                }
                siftedCounter++;
            }
        }

        if (result.errorRate > cfg.errorThreshold) {
            result.aborted = true;
            buildSteps(cfg.numPhotons, aliceBits, aliceBases, alicePols,
                    eveBases, eveMeasured, eveResentPols,
                    bobBases, bobMeasured, inSifted, usedForCheck,
                    siftedAlice, siftedBob, checkSet);
            return result;
        }

        // ── Phase 4: Privacy Amplification ────────────────────────────────────
        if (cfg.useCascade) {
            CascadeCorrection cascade = new CascadeCorrection(workAlice, workBob);
            workBob = cascade.correct();
            result.cascadeUsed = true;
            result.cascadeErrorsBefore = cascade.getErrorsBefore();
            result.cascadeErrorsAfter  = cascade.getErrorsAfter();
        }

        result.aliceFinalKey  = xorAmplify(workAlice);
        result.bobFinalKey    = xorAmplify(workBob);
        result.finalKeyLength = result.aliceFinalKey.size();
        result.aborted        = false;
        result.keyEfficiency  = (double) result.finalKeyLength / cfg.numPhotons * 100.0;

        // ── Build step-by-step data ────────────────────────────────────────────
        // Map sifted key errors back to photon indices
        boolean[] isError = new boolean[cfg.numPhotons];
        siftedCounter = 0;
        for (int i = 0; i < cfg.numPhotons; i++) {
            if (inSifted[i]) {
                int si = siftedCounter++;
                if (si < siftedAlice.size() && !siftedAlice.get(si).equals(siftedBob.get(si)))
                    isError[i] = true;
            }
        }

        buildSteps(cfg.numPhotons, aliceBits, aliceBases, alicePols,
                eveBases, eveMeasured, eveResentPols,
                bobBases, bobMeasured, inSifted, usedForCheck,
                siftedAlice, siftedBob, checkSet);

        return result;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void buildSteps(int n, int[] aliceBits, String[] aliceBases, int[] alicePols,
                             String[] eveBases, int[] eveMeasured, int[] eveResentPols,
                             String[] bobBases, int[] bobMeasured,
                             boolean[] inSifted, boolean[] usedForCheck,
                             List<Integer> siftedAlice, List<Integer> siftedBob,
                             Set<Integer> checkSet) {
        int sc = 0;
        for (int i = 0; i < Math.min(n, 200); i++) { // cap at 200 for UI
            boolean match    = aliceBases[i].equals(bobBases[i]);
            boolean error    = false;
            if (inSifted[i] && sc < siftedAlice.size()) {
                error = !siftedAlice.get(sc).equals(siftedBob.get(sc));
                sc++;
            }
            steps.add(new StepData(i,
                    aliceBits[i], aliceBases[i], alicePols[i],
                    cfg.eveEnabled,
                    cfg.eveEnabled ? eveBases[i] : "-",
                    cfg.eveEnabled ? eveMeasured[i] : -1,
                    cfg.eveEnabled ? eveResentPols[i] : -1,
                    bobBases[i], bobMeasured[i],
                    match, inSifted[i],
                    usedForCheck != null && usedForCheck[i],
                    error));
        }
    }

    private List<Integer> xorAmplify(List<Integer> bits) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i + 1 < bits.size(); i += 2)
            out.add(bits.get(i) ^ bits.get(i + 1));
        return out;
    }

    private Photon.Basis randomBasis() {
        return rng.nextBoolean() ? Photon.Basis.RECTILINEAR : Photon.Basis.DIAGONAL;
    }

    public List<StepData> getSteps() { return Collections.unmodifiableList(steps); }
}
