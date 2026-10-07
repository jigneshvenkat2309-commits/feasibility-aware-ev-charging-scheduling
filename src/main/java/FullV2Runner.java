import experiment.ExperimentMatrix;
import experiment.ExperimentResult;
import experiment.ExperimentRunner;
import experiment.ExperimentTask;
import scheduler.AdaptiveSchedulerV2;
import java.util.List;
import java.io.PrintWriter;
import java.io.FileWriter;

public class FullV2Runner {
    public static void main(String[] args) throws Exception {
        ExperimentMatrix matrix = new ExperimentMatrix(
                List.of(50, 100, 250, 500),
                List.of("LOW", "NORMAL", "HIGH"),
                List.of("LOW", "HIGH"),
                List.of(false, true),
                20, 720, 42L, 5);

        List<ExperimentTask> allTasks = matrix.generateTasks();
        
        System.out.println("Running " + allTasks.size() + " tasks for V2 Full Experiment...");
        
        ExperimentRunner runner = new ExperimentRunner(allTasks,
                List.of(new AdaptiveSchedulerV2()));
                
        List<ExperimentResult> results = runner.run();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter("results/analysis/adaptive_v2_full_results.csv"))) {
            writer.println(ExperimentResult.header());
            for (ExperimentResult r : results) {
                writer.println(r.csv());
            }
        }
        System.out.println("Full V2 Experiment completed successfully. File written.");
    }
}
