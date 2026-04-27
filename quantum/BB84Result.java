package quantum.quantum;

import java.util.List;

public class BB84Result {
    public int    totalPhotons;
    public int    siftedKeyLength;
    public double basisMatchRate;
    public int    errorCheckSampleSize;
    public int    errorsFound;
    public double errorRate;
    public List<Integer> aliceFinalKey;
    public List<Integer> bobFinalKey;
    public int    finalKeyLength;
    public boolean cascadeUsed         = false;
    public int     cascadeErrorsBefore = 0;
    public int     cascadeErrorsAfter  = 0;
    public boolean aborted;
    public boolean evePresent;
    public double  keyEfficiency;
    public double  eveBasisMatchRate;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append("         BB84 Protocol Results\n");
        sb.append("═══════════════════════════════════════\n");
        sb.append(String.format("Total photons transmitted : %d%n",     totalPhotons));
        sb.append(String.format("Basis match rate          : %.1f%%%n", basisMatchRate * 100));
        sb.append(String.format("Sifted key length         : %d bits%n", siftedKeyLength));
        sb.append(String.format("Error check sample        : %d bits%n", errorCheckSampleSize));
        sb.append(String.format("Errors found              : %d%n",      errorsFound));
        sb.append(String.format("Error rate                : %.1f%%%n",  errorRate * 100));
        if (cascadeUsed) {
            sb.append(String.format("Cascade errors before     : %d%n", cascadeErrorsBefore));
            sb.append(String.format("Cascade errors after      : %d%n", cascadeErrorsAfter));
        }
        sb.append(String.format("Final key length          : %d bits%n", finalKeyLength));
        sb.append(String.format("Key efficiency            : %.1f%%%n",  keyEfficiency));
        sb.append(String.format("Eve present               : %s%n",      evePresent ? "YES" : "NO"));
        sb.append(String.format("Protocol status           : %s%n",
                aborted ? "ABORTED (eavesdropper detected)" : "SUCCESS"));
        if (evePresent)
            sb.append(String.format("Eve basis match rate      : %.1f%%%n", eveBasisMatchRate * 100));
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