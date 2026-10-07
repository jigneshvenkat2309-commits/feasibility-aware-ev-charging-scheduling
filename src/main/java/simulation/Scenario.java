package simulation;

/** Immutable experimental condition. */
public final class Scenario {
    private final String scenarioName;
    private final int numberOfEVs;
    private final int numberOfChargers;
    private final int simulationDurationMinutes;
    private final String demandLevel;
    private final String uncertaintyLevel;
    private final boolean burstArrivalEnabled;
    private final boolean chargerFailureEnabled;
    private final long randomSeed;
    private final double minBatteryCapacityKWh;
    private final double maxBatteryCapacityKWh;
    private final double minChargingPowerKW;
    private final double maxChargingPowerKW;
    private final double chargerPowerKW;

    public Scenario(String name, int evs, int chargers, int duration, String demand, String uncertainty, boolean burst,
            boolean failure, long seed) {
        this(name, evs, chargers, duration, demand, uncertainty, burst, failure, seed, 40, 80, 3, 11, 11);
    }

    public Scenario(String name, int evs, int chargers, int duration, String demand, String uncertainty, long seed) {
        this(name, evs, chargers, duration, demand, uncertainty, false, false, seed);
    }

    public Scenario(String name, int evs, int chargers, int duration, String demand, String uncertainty, String burst,
            String failure, long seed, java.util.List<Double> evRanges, java.util.List<Double> chargerRanges) {
        this(name, evs, chargers, duration, demand, uncertainty, Boolean.parseBoolean(burst),
                Boolean.parseBoolean(failure), seed, evRanges.get(0), evRanges.get(1),
                evRanges.size() > 2 ? evRanges.get(2) : 3, evRanges.size() > 3 ? evRanges.get(3) : 11,
                chargerRanges.get(0));
    }

    public Scenario(String name, int evs, int chargers, int duration, String demand, String uncertainty, boolean burst,
            boolean failure, long seed, double minBattery, double maxBattery, double minPower, double maxPower,
            double chargerPower) {
        if (name == null || evs < 0 || chargers <= 0 || duration <= 0 || minBattery <= 0 || maxBattery < minBattery
                || minPower <= 0 || maxPower < minPower || chargerPower <= 0)
            throw new IllegalArgumentException("Invalid scenario");
        scenarioName = name;
        numberOfEVs = evs;
        numberOfChargers = chargers;
        simulationDurationMinutes = duration;
        demandLevel = demand;
        uncertaintyLevel = uncertainty;
        burstArrivalEnabled = burst;
        chargerFailureEnabled = failure;
        randomSeed = seed;
        minBatteryCapacityKWh = minBattery;
        maxBatteryCapacityKWh = maxBattery;
        minChargingPowerKW = minPower;
        maxChargingPowerKW = maxPower;
        this.chargerPowerKW = chargerPower;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public int getNumberOfEVs() {
        return numberOfEVs;
    }

    public int getNumberOfChargers() {
        return numberOfChargers;
    }

    public int getSimulationDuration() {
        return simulationDurationMinutes;
    }

    public int getSimulationDurationMinutes() {
        return simulationDurationMinutes;
    }

    public String getDemandLevel() {
        return demandLevel;
    }

    public String getUncertaintyLevel() {
        return uncertaintyLevel;
    }

    public boolean isBurstArrivalEnabled() {
        return burstArrivalEnabled;
    }

    public boolean isArrivalBurstSetting() {
        return burstArrivalEnabled;
    }

    public boolean isChargerFailureEnabled() {
        return chargerFailureEnabled;
    }

    public boolean isChargerFailureSetting() {
        return chargerFailureEnabled;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public double getMinBatteryCapacityKWh() {
        return minBatteryCapacityKWh;
    }

    public double getMaxBatteryCapacityKWh() {
        return maxBatteryCapacityKWh;
    }

    public double getMinChargingPowerKW() {
        return minChargingPowerKW;
    }

    public double getMaxChargingPowerKW() {
        return maxChargingPowerKW;
    }

    public double getChargerPowerKW() {
        return chargerPowerKW;
    }
}