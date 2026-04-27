package quantum.quantum;

/**
 * Represents a single photon with polarization state.
 *
 * Bases:
 *   '+' (Rectilinear): bit 0 → 0° (↔), bit 1 → 90° (↕)
 *   '×' (Diagonal):    bit 0 → 45° (⤢), bit 1 → 135° (⤡)
 */
public class Photon {

    public enum Basis { RECTILINEAR, DIAGONAL }

    private final int    bitValue;     // 0 or 1
    private final Basis  basis;        // '+' or '×'
    private final int    polarization; // 0, 45, 90, or 135 degrees

    public Photon(int bitValue, Basis basis) {
        if (bitValue != 0 && bitValue != 1)
            throw new IllegalArgumentException("bitValue must be 0 or 1");
        this.bitValue    = bitValue;
        this.basis       = basis;
        this.polarization = encodePolarization(bitValue, basis);
    }

    /** Returns polarization angle for the given bit+basis combination. */
    private int encodePolarization(int bit, Basis b) {
        if (b == Basis.RECTILINEAR) return bit == 0 ? 0  : 90;
        else                        return bit == 0 ? 45 : 135;
    }

    /**
     * Simulates quantum measurement of this photon in the given basis.
     *
     * Same basis  → deterministic: always returns original bit (100% accuracy).
     * Diff basis  → probabilistic: returns 0 or 1 each with 50% probability.
     */
    public int measure(Basis measureBasis) {
        if (measureBasis == this.basis) {
            return bitValue; // same basis — perfect measurement
        } else {
            // Different basis — quantum randomness
            return Math.random() < 0.5 ? 0 : 1;
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int   getBitValue()    { return bitValue; }
    public Basis getBasis()       { return basis; }
    public int   getPolarization(){ return polarization; }

    @Override
    public String toString() {
        String symbol = switch (polarization) {
            case 0   -> "↔";
            case 90  -> "↕";
            case 45  -> "⤢";
            case 135 -> "⤡";
            default  -> "?";
        };
        return String.format("Photon[bit=%d, basis=%s, pol=%d°%s]",
                bitValue,
                basis == Basis.RECTILINEAR ? "+" : "×",
                polarization, symbol);
    }
}
