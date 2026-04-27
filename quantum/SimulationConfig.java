package quantum.quantum;

/**
 * Holds all user-configurable parameters for the simulation.
 */
public class SimulationConfig {
    public int    numPhotons        = 500;
    public double errorCheckFraction = 0.10;
    public double errorThreshold     = 0.11;
    public boolean eveEnabled        = false;
    public double  channelNoise      = 0.0;
    public boolean useCascade        = false;

    public SimulationConfig() {}

    public SimulationConfig(int numPhotons, boolean eveEnabled) {
        this.numPhotons  = numPhotons;
        this.eveEnabled  = eveEnabled;
    }
}