package quantum.quantum;

/**
 * Captures the state of a single photon transmission step,
 * used for the step-by-step visualization mode.
 */
public class StepData {
    public final int index;

    // Alice
    public final int    aliceBit;
    public final String aliceBasis;   // "+" or "×"
    public final int    alicePol;     // 0, 45, 90, 135

    // Eve (may be null if not enabled)
    public final boolean evePresent;
    public final String  eveBasis;
    public final int     eveMeasured;
    public final int     eveResentPol;

    // Bob
    public final String bobBasis;
    public final int    bobMeasured;

    // Reconciliation
    public final boolean basesMatch;

    // After all photons: per-bit info
    public final boolean inSiftedKey;     // was kept after reconciliation
    public final boolean usedForCheck;    // used in error checking
    public final boolean isError;         // was it an error bit

    public StepData(int index,
                    int aliceBit, String aliceBasis, int alicePol,
                    boolean evePresent, String eveBasis, int eveMeasured, int eveResentPol,
                    String bobBasis, int bobMeasured,
                    boolean basesMatch, boolean inSiftedKey,
                    boolean usedForCheck, boolean isError) {
        this.index       = index;
        this.aliceBit    = aliceBit;
        this.aliceBasis  = aliceBasis;
        this.alicePol    = alicePol;
        this.evePresent  = evePresent;
        this.eveBasis    = eveBasis;
        this.eveMeasured = eveMeasured;
        this.eveResentPol = eveResentPol;
        this.bobBasis    = bobBasis;
        this.bobMeasured = bobMeasured;
        this.basesMatch  = basesMatch;
        this.inSiftedKey = inSiftedKey;
        this.usedForCheck = usedForCheck;
        this.isError     = isError;
    }

    /** Returns the polarization symbol for the given angle. */
    public static String polSymbol(int deg) {
        return switch (deg) {
            case 0   -> "↔";
            case 90  -> "↕";
            case 45  -> "↗";
            case 135 -> "↘";
            default  -> "?";
        };
    }
}