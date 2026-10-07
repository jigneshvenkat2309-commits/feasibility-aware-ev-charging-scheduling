# Lightweight Adaptive EV Charging Scheduling Simulator

## 1. Overview
This project provides a discrete-event simulation framework for evaluating Electric Vehicle (EV) charging scheduling algorithms under varying conditions of scale, demand urgency, arrival uncertainty, and hardware failure. 

## 2. Research Question
How can a lightweight, computationally efficient scheduling algorithm maximize EV charging completion rates and minimize deadline violations in constrained, uncertain environments compared to traditional sorting-based approaches like FCFS, EDF, or Greedy?

## 3. Key Idea
The AdaptiveScheduler aims to achieve high completion rates by avoiding computationally expensive `O(N log N)` re-sorting at every timestep. Instead, it computes a composite priority score combining urgency (deadline), waiting time (fairness), and remaining energy requirement using continuous O(N) evaluation. 

## 4. System Architecture
- **Model**: `EV`, `Charger`, `Station`, `Event`
- **Scheduler**: Interface `Scheduler`, mapping available chargers to waiting EVs
- **Simulation**: 1-minute discrete event step logic (`Simulation`, `EventGenerator`, `Scenario`)
- **Metrics**: Wait times, completion rate, deadline violations, throughput, computational runtime
- **Experiment Framework**: Matrix evaluation of parameters and deterministic seed generation

## 5. Scheduling Algorithms
The simulator evaluates the following baseline algorithms:
- **FCFS (First-Come, First-Served)**: Basic FIFO queue.
- **EDF (Earliest Deadline First)**: Prioritizes EVs with the nearest departure time.
- **Greedy**: Prioritizes EVs with the highest energy deficit.
- **AdaptiveScheduler (V1 & V2)**: Lightweight heuristic scheduling.

## 6. AdaptiveScheduler V1 → V2 refinement
During empirical validation, AdaptiveScheduler V1 demonstrated a priority inversion flaw where mathematically doomed EVs (impossible to reach target SOC before departure) saturated the urgency score and monopolized chargers. 

AdaptiveScheduler V2 introduces an O(1) feasibility check per candidate assignment: 
`requiredChargingTime = remainingEnergy / min(EV_max_power, charger_max_power)`.
If `requiredChargingTime > remainingTime (departureTime - currentTime)`, the EV is flagged as infeasible and strictly deprioritized using a deterministic deficit penalty.

## 7. Simulation Model
The simulator models a 720-minute operational horizon. Time advances in discrete 1-minute steps. Chargers supply energy capped by the minimum of the charger's output capacity and the EV's acceptance limit. Charging stations can experience stochastic charger failures that force connected EVs back into the queue.

## 8. Experimental Design
The rigorous evaluation matrix consists of 48 distinct cells:
- **EV Counts**: 50, 100, 250, 500
- **Chargers**: Fixed at N / 5
- **Demand Levels**: LOW, NORMAL, HIGH
- **Uncertainty Levels**: LOW, HIGH
- **Hardware Failures**: ON (25% failure from min 180 to 300), OFF
Each cell is simulated across 5 independent trials with deterministic seeding for fairness.

## 9. Main Results
The following descriptive results reflect the aggregate performance across the tested simulation environment:

**Adaptive V2 (Final Proposed Scheduler)**:
- Overall completion = 61.93%
- Deadline violation = 24.95%
- Average wait = 70.85 min
- Scheduler runtime = 1.27 ms

**EDF**:
- Overall completion = 52.02%
- Deadline violation = 34.61%
- Average wait = 67.19 min
- Scheduler runtime = 3.12 ms

**V2 Comparison**:
- Higher completion rate than V1 in 48/48 cells
- Higher completion rate than EDF in 42/48 cells
- Higher completion rate than FCFS in 47/48 cells
- Higher completion rate than Greedy in 48/48 cells

**Extreme Stress Scenario (500 EVs, High Demand, High Uncertainty, Failures ON)**:
- Adaptive V2 completion: 43.12%
- EDF completion: 28.56%
- Adaptive V1 completion: 19.56%

*(Note: Results are based on 5 trials per cell. Conclusions are descriptive for this specific experimental configuration. No claims of theoretical optimality or statistical significance are made.)*

## 10. How to Build
The project relies on Maven for dependency management and test execution.
**Requirements:**
- Java 25 or higher
- Apache Maven

**Compilation:**
```bash
mvn clean compile
```

## 11. How to Run
To run the baseline experiment framework (which outputs the results matrix to CSV):
```bash
mvn exec:java -Dexec.mainClass="Main"
```
*(Note: To run tests, use `mvn test`. Ensure Maven is properly configured in your system environment PATH, as it may not be available by default in all audit shells.)*

## 12. Results Location
Research artifacts are systematically preserved:
- `results/experiment_results.csv`: Original 960-run baseline experiment data (V1, FCFS, EDF, Greedy)
- `results/analysis/adaptive_v2_full_results.csv`: Separate 240-run V2 evaluation data
- `results/analysis/`: Contains analysis summaries, data slicing, and audit logs
- `results/figures/`: Auto-generated research charts and plots

The baseline results are securely preserved, and V2 data is stored alongside it for transparent comparison.

## 13. Project Limitations
- The findings rely on 5 trials per experimental cell, limiting statistical power.
- The model uses linear continuous charging without simulating complex non-linear battery degradation curves (e.g., tapering above 80% SOC).
- Vehicle arrival models are synthetic and may not perfectly mirror real-world charging behaviors in all municipalities.
- Pricing logic and grid-level demand-response constraints are outside the scope of this phase.

## 14. Future Work
- Incorporating non-linear battery charging profiles.
- Testing with real-world, localized EV arrival datasets.
- Integrating a grid pricing signal to minimize operational charging costs.
- Expanding the trial count to enable robust statistical significance testing.
