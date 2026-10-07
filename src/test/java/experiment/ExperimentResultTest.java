package experiment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExperimentResultTest {

    @Test
    public void testCsvFormatting() {
        ExperimentResult result = new ExperimentResult("FCFS", "scen1", 100, 20, "NORMAL", "LOW", false, 720,
                42L, 42L, 0, 0, 10.5, 20.0, 0.95, 0.05, 95, 2.5, 0.8, 100.0, 50.0, 2.0, 500.0, 1000L, 10L);

        String csv = result.csv();
        assertTrue(csv.startsWith("FCFS,scen1,100,20,NORMAL,LOW,false,720,42,42,0,0,10.5,20.0,0.95,0.05,95,2.5"));
    }

    @Test
    public void testHeaderColumns() {
        String header = ExperimentResult.header();
        assertTrue(header.contains("demandLevel"));
        assertTrue(header.contains("baseSeed"));
        assertTrue(header.contains("derivedSeed"));
        assertTrue(header.contains("cellIndex"));
    }
}
