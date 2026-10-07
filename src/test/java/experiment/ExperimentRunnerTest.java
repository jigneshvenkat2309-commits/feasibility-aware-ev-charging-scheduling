package experiment;

import org.junit.jupiter.api.Test;
import scheduler.FCFS;
import scheduler.Scheduler;
import simulation.Scenario;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class ExperimentRunnerTest {

    @Test
    public void testExportCsvAppendsCorrectly() throws IOException {
        Path tempFile = Files.createTempFile("test_results", ".csv");
        Files.delete(tempFile); // Delete so exportCsv sees it as non-existent
        tempFile.toFile().deleteOnExit();

        Scenario scenario = new Scenario("s1", 10, 5, 100, "LOW", "LOW", false, false, 42L);
        ExperimentTask task = new ExperimentTask(scenario, 42L, 42L, 0, 0);
        ExperimentRunner runner = new ExperimentRunner(List.of(task), List.of(new FCFS()));
        
        List<ExperimentResult> results = runner.run();
        
        // First export: should write header + 1 data row
        runner.exportCsv(results, tempFile);
        List<String> lines1 = Files.readAllLines(tempFile);
        assertEquals(2, lines1.size());
        
        // Second export: should append 1 data row without rewriting header
        runner.exportCsv(results, tempFile);
        List<String> lines2 = Files.readAllLines(tempFile);
        assertEquals(3, lines2.size());
        assertEquals(lines1.get(0), lines2.get(0)); // Header unchanged
    }

    @Test
    public void testFairnessAndIsolation() {
        Scenario scenario = new Scenario("s1", 20, 5, 100, "LOW", "LOW", false, false, 42L);
        ExperimentTask task = new ExperimentTask(scenario, 42L, 42L, 0, 0);
        
        // Run with two identical schedulers
        Scheduler s1 = new FCFS();
        Scheduler s2 = new FCFS();
        ExperimentRunner runner = new ExperimentRunner(List.of(task), List.of(s1, s2));
        
        List<ExperimentResult> results = runner.run();
        assertEquals(2, results.size());
        
        // Both schedulers should receive identical conditions and yield identical completion rates and waiting times
        ExperimentResult res1 = results.get(0);
        ExperimentResult res2 = results.get(1);
        
        assertEquals(res1.completionRate(), res2.completionRate());
        assertEquals(res1.averageWaitingTime(), res2.averageWaitingTime());
        assertEquals(res1.throughput(), res2.throughput());
    }
}
