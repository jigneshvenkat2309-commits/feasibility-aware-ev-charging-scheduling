package metrics;

public class EnergyMetrics {
    private double totalEnergyKWh;
    private double loadSum;
    private double peakLoad;
    private int samples;
    private int busyChargerMinutes;
    private int chargerCount;
    private int duration;

    public void configure(int chargers, int simulationDuration) {
        chargerCount = chargers;
        duration = simulationDuration;
    }

    public void observe(double load, double energy, int busyChargers) {
        totalEnergyKWh += energy;
        loadSum += load;
        peakLoad = Math.max(peakLoad, load);
        busyChargerMinutes += busyChargers;
        samples++;
    }

    public double getTotalEnergyKWh() {
        return totalEnergyKWh;
    }

    public double getTotalEnergy() {
        return totalEnergyKWh;
    }

    public double getPeakLoadKW() {
        return peakLoad;
    }

    public double getPeakLoad() {
        return peakLoad;
    }

    public double getAverageLoadKW() {
        return samples == 0 ? 0 : loadSum / samples;
    }

    public double getAverageLoad() {
        return getAverageLoadKW();
    }

    public double getPAR() {
        return getAverageLoadKW() == 0 ? 0 : peakLoad / getAverageLoadKW();
    }

    public double getPeakToAverageRatio() {
        return getPAR();
    }

    public double getChargerUtilization() {
        return chargerCount == 0 || duration == 0 ? 0 : (double) busyChargerMinutes / (chargerCount * duration);
    }

    public double getTotalChargerUtilization() {
        return getChargerUtilization();
    }
}