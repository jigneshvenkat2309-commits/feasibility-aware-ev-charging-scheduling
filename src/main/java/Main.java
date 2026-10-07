import experiment.ExperimentMatrix;
import experiment.ExperimentResult;
import experiment.ExperimentRunner;
import experiment.ExperimentTask;
import scheduler.AdaptiveScheduler;
import scheduler.EDF;
import scheduler.FCFS;
import scheduler.Greedy;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        ExperimentMatrix matrix = new ExperimentMatrix(
                List.of(50, 100, 250, 500),
                List.of("LOW", "NORMAL", "HIGH"),
                List.of("LOW", "HIGH"),
                List.of(false, true),
                20, 720, 42L, 5);

        List<ExperimentTask> tasks = matrix.generateTasks();
        
        ExperimentRunner runner = new ExperimentRunner(tasks,
                List.of(new FCFS(), new EDF(), new Greedy(), new AdaptiveScheduler()));
        List<ExperimentResult> results = runner.run();
        runner.exportCsv(results, Path.of("results", "experiment_results.csv"));
        
        results.stream().limit(4)
                .forEach(result -> System.out.printf("%s: completion=%.3f, wait=%.2f min, energy=%.2f kWh%n",
                        result.algorithmName(), result.completionRate(), result.averageWaitingTime(),
                        result.totalEnergyKWh()));
        System.out.println("Total runs completed: " + results.size());
        System.out.println("CSV: results/experiment_results.csv");
    }
}