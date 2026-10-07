package experiment;

import model.EV;
import scheduler.Scheduler;
import simulation.EventGenerator;
import simulation.Scenario;
import simulation.Simulation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/** Runs schedulers against equivalent immutable input data and exports CSV. */
public class ExperimentRunner {
    private final List<ExperimentTask> tasks;
    private final List<Scheduler> schedulers;

    public ExperimentRunner(List<ExperimentTask> tasks, List<Scheduler> schedulers) {
        this.tasks = List.copyOf(tasks);
        this.schedulers = List.copyOf(schedulers);
    }

    public List<ExperimentResult> run() {
        List<ExperimentResult> results = new ArrayList<>();
        for (ExperimentTask task : tasks) {
            Scenario scenario = task.scenario();
            List<EV> base = new EventGenerator(scenario).generateEVs();
            for (Scheduler scheduler : schedulers) {
                List<EV> copy = copy(base);
                Simulation simulation = new Simulation(scenario, scheduler, copy);
                simulation.run();
                results.add(result(simulation, task));
            }
        }
        return results;
    }

    private List<EV> copy(List<EV> source) {
        List<EV> result = new ArrayList<>();
        for (EV ev : source)
            result.add(new EV(ev.getId(), ev.getArrivalTime(), ev.getExpectedDepartureTime(),
                    ev.getBatteryCapacityKWh(), ev.getInitialSOC(), ev.getTargetSOC(), ev.getMaxChargingPowerKW()));
        return result;
    }

    private ExperimentResult result(Simulation s, ExperimentTask task) {
        var service = s.getServiceMetrics();
        var energy = s.getEnergyMetrics();
        var compute = s.getComputationalMetrics();
        var scenario = task.scenario();
        return new ExperimentResult(s.getSchedulerName(), scenario.getScenarioName(), scenario.getNumberOfEVs(),
                scenario.getNumberOfChargers(), scenario.getDemandLevel(), scenario.getUncertaintyLevel(),
                scenario.isChargerFailureEnabled(), scenario.getSimulationDurationMinutes(), task.baseSeed(),
                task.derivedSeed(), task.trialNumber(), task.cellIndex(), service.getAverageWaitingTime(),
                service.getMaxWaitingTime(), service.getCompletionRate(), service.getDeadlineViolationRate(),
                service.getThroughput(), service.getAverageQueueLength(), energy.getChargerUtilization(),
                energy.getPeakLoadKW(), energy.getAverageLoadKW(), energy.getPAR(), energy.getTotalEnergyKWh(),
                compute.getTotalSchedulerRuntimeNanos(), compute.getAverageSchedulerRuntimeNanos());
    }

    public void exportCsv(List<ExperimentResult> results, Path path) throws IOException {
        if (path.getParent() != null)
            Files.createDirectories(path.getParent());
        List<String> lines = new ArrayList<>();
        boolean writeHeader = !Files.exists(path);
        if (writeHeader) {
            lines.add(ExperimentResult.header());
        }
        results.forEach(r -> lines.add(r.csv()));
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public void runExperiments() {
        run();
    }
}