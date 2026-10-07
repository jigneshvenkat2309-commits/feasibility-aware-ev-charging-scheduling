package experiment;

import metrics.*;

public record ExperimentResult(String algorithmName, String scenarioName, int numberOfEVs,
        int numberOfChargers, String demandLevel, String uncertaintyLevel, boolean chargerFailureEnabled, int horizon,
        long baseSeed, long derivedSeed, int trialNumber, int cellIndex,
        double averageWaitingTime, double maximumWaitingTime, double completionRate,
        double deadlineViolationRate, int throughput, double averageQueueLength, double chargerUtilization,
        double peakLoadKW, double averageLoadKW, double par, double totalEnergyKWh, long totalSchedulerRuntimeNanos,
        long averageSchedulerRuntimeNanos) {
    public static String header() {
        return "algorithm,scenario,evCount,chargerCount,demandLevel,uncertaintyLevel,chargerFailureEnabled,horizon,baseSeed,derivedSeed,trialNumber,cellIndex,averageWaitingTime,maximumWaitingTime,completionRate,deadlineViolationRate,throughput,averageQueueLength,chargerUtilization,peakLoadKW,averageLoadKW,PAR,totalEnergyKWh,totalSchedulerRuntimeNanos,averageSchedulerRuntimeNanos";
    }

    public String csv() {
        return String.join(",", algorithmName, scenarioName, Integer.toString(numberOfEVs),
                Integer.toString(numberOfChargers), demandLevel, uncertaintyLevel,
                Boolean.toString(chargerFailureEnabled), Integer.toString(horizon), Long.toString(baseSeed),
                Long.toString(derivedSeed), Integer.toString(trialNumber), Integer.toString(cellIndex),
                Double.toString(averageWaitingTime), Double.toString(maximumWaitingTime), Double.toString(completionRate),
                Double.toString(deadlineViolationRate), Integer.toString(throughput),
                Double.toString(averageQueueLength), Double.toString(chargerUtilization), Double.toString(peakLoadKW),
                Double.toString(averageLoadKW), Double.toString(par), Double.toString(totalEnergyKWh),
                Long.toString(totalSchedulerRuntimeNanos), Long.toString(averageSchedulerRuntimeNanos));
    }
}