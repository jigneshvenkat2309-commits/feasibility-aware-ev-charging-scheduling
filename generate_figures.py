import os
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

# Working directory
os.chdir('c:/MY_TECH/EV_project/lightweight-adaptive-ev-simulator')
os.makedirs('results/figures', exist_ok=True)

# Standard IEEE publication formatting
plt.rcParams['font.family'] = 'serif'
plt.rcParams['font.size'] = 11
plt.rcParams['axes.labelsize'] = 12
plt.rcParams['axes.titlesize'] = 12
plt.rcParams['xtick.labelsize'] = 10
plt.rcParams['ytick.labelsize'] = 10
plt.rcParams['legend.fontsize'] = 10
plt.rcParams['figure.titlesize'] = 13

# Strict algorithm naming and styling
algo_order = ['FCFS', 'EDF', 'Greedy', 'Adaptive V1', 'Adaptive V2']
algo_colors = {
    'FCFS': '#1f77b4',
    'EDF': '#ff7f0e',
    'Greedy': '#2ca02c',
    'Adaptive V1': '#d62728',
    'Adaptive V2': '#9467bd'
}
algo_markers = {
    'FCFS': 's',
    'EDF': '^',
    'Greedy': 'v',
    'Adaptive V1': 'x',
    'Adaptive V2': 'o'
}

# -------------------------------------------------------------
# Figure 2: Cell-by-cell Adaptive V1 vs Adaptive V2
# -------------------------------------------------------------
df_cell = pd.read_csv('results/analysis/cell_comparison.csv')
df_v2 = pd.read_csv('results/analysis/adaptive_v2_full_results.csv')

v1_cells = df_cell[df_cell['algorithm'] == 'AdaptiveScheduler'].sort_values('cellIndex')['completionRate_mean'].values * 100
v2_cells = df_v2.groupby('cellIndex')['completionRate'].mean().values * 100
cells = np.arange(1, 49)

plt.figure(figsize=(9.5, 4.5))
plt.plot(cells, v1_cells, marker='x', color=algo_colors['Adaptive V1'], 
         label='Adaptive V1', linewidth=1.6, markersize=5)
plt.plot(cells, v2_cells, marker='o', color=algo_colors['Adaptive V2'], 
         label='Adaptive V2', linewidth=1.6, markersize=5)
plt.title('Cell-by-Cell Completion Rate Comparison (Adaptive V1 vs. Adaptive V2)')
plt.xlabel('Experimental Cell Index (1 to 48)')
plt.ylabel('Completion Rate (%)')
plt.grid(True, linestyle='--', alpha=0.6)
plt.legend(loc='lower left', framealpha=0.95)
plt.xlim(0, 49)
plt.ylim(10, 85)
plt.tight_layout()
plt.savefig('results/figures/adaptive_v1_vs_v2_completion.png', dpi=300)
plt.close()
print("Generated Fig. 2: adaptive_v1_vs_v2_completion.png")

# -------------------------------------------------------------
# Figure 3: Completion vs EV Count
# -------------------------------------------------------------
ev_counts = [50, 100, 250, 500]
data_comp = {
    'FCFS': [39.70, 37.83, 37.83, 37.57],
    'EDF': [50.47, 51.55, 53.16, 52.89],
    'Greedy': [37.57, 34.98, 35.42, 34.56],
    'Adaptive V1': [38.70, 36.85, 37.57, 37.67],
    'Adaptive V2': [59.87, 62.13, 62.74, 62.98]
}

plt.figure(figsize=(7, 4.5))
for algo in algo_order:
    plt.plot(ev_counts, data_comp[algo], marker=algo_markers[algo], 
             color=algo_colors[algo], label=algo, linewidth=2, markersize=6)
plt.title('Completion Rate vs EV Count')
plt.xlabel('EV Count')
plt.ylabel('Completion Rate (%)')
plt.ylim(25, 75)
plt.grid(True, linestyle='--', alpha=0.6)
plt.legend(loc='lower right', framealpha=0.95)
plt.tight_layout()
plt.savefig('results/figures/completion_vs_ev_count.png', dpi=300)
plt.close()
print("Generated Fig. 3: completion_vs_ev_count.png")

# -------------------------------------------------------------
# Figure 4: Completion by Demand
# -------------------------------------------------------------
demand_levels = ['LOW', 'NORMAL', 'HIGH']
comp_demand = {
    'FCFS': [54.69, 36.93, 23.08],
    'EDF': [64.63, 58.66, 32.76],
    'Greedy': [51.80, 34.06, 21.04],
    'Adaptive V1': [54.03, 36.52, 22.54],
    'Adaptive V2': [65.36, 67.53, 52.90]
}

x = np.arange(len(demand_levels))
width = 0.16

plt.figure(figsize=(8, 4.8))
for i, algo in enumerate(algo_order):
    plt.bar(x + (i - 2) * width, comp_demand[algo], width, 
            label=algo, color=algo_colors[algo], edgecolor='black', linewidth=0.5)

plt.title('Completion Rate by Demand Level')
plt.xlabel('Demand Level')
plt.ylabel('Completion Rate (%)')
plt.xticks(x, demand_levels)
plt.ylim(0, 80)
plt.grid(axis='y', linestyle='--', alpha=0.6)
plt.legend(loc='upper right', framealpha=0.95)
plt.tight_layout()
plt.savefig('results/figures/completion_by_demand.png', dpi=300)
plt.close()
print("Generated Fig. 4: completion_by_demand.png")

# -------------------------------------------------------------
# Figure 5: Completion Failure Comparison (OFF vs ON)
# -------------------------------------------------------------
fail_labels = ['OFF', 'ON']
comp_fail = {
    'FCFS': [41.15, 35.31],
    'EDF': [54.81, 49.23],
    'Greedy': [39.00, 32.27],
    'Adaptive V1': [41.07, 34.32],
    'Adaptive V2': [63.92, 59.94]
}

x_fail = np.arange(len(fail_labels))
plt.figure(figsize=(7.5, 4.8))
for i, algo in enumerate(algo_order):
    plt.bar(x_fail + (i - 2) * width, comp_fail[algo], width, 
            label=algo, color=algo_colors[algo], edgecolor='black', linewidth=0.5)

plt.title('Completion Rate by Charger Failure State')
plt.xlabel('Failure State')
plt.ylabel('Completion Rate (%)')
plt.xticks(x_fail, fail_labels)
plt.ylim(0, 75)
plt.grid(axis='y', linestyle='--', alpha=0.6)
plt.legend(loc='upper right', framealpha=0.95)
plt.tight_layout()
plt.savefig('results/figures/completion_failure_comparison.png', dpi=300)
plt.close()
print("Generated Fig. 5: completion_failure_comparison.png")

# -------------------------------------------------------------
# Figure 6: Extreme Stress Scenario Comparison
# -------------------------------------------------------------
stress_comp = [21.56, 28.56, 17.56, 19.56, 43.12]
x_stress = np.arange(len(algo_order))

plt.figure(figsize=(7.2, 4.5))
bars = plt.bar(x_stress, stress_comp, color=[algo_colors[a] for a in algo_order], 
               edgecolor='black', linewidth=0.6, width=0.55)
for bar in bars:
    yval = bar.get_height()
    plt.text(bar.get_x() + bar.get_width()/2.0, yval + 0.8, f'{yval:.1f}%', 
             ha='center', va='bottom', fontsize=9, fontweight='bold')

plt.title('Extreme Stress Scenario (500 EVs, HIGH Demand, HIGH Uncertainty, Failure ON)')
plt.xlabel('Scheduler')
plt.ylabel('Completion Rate (%)')
plt.xticks(x_stress, algo_order)
plt.ylim(0, 50)
plt.grid(axis='y', linestyle='--', alpha=0.6)
plt.tight_layout()
plt.savefig('results/figures/extreme_stress_comparison.png', dpi=300)
plt.close()
print("Generated Fig. 6: extreme_stress_comparison.png")

# -------------------------------------------------------------
# Figure 7: Runtime vs EV Count (nanoseconds)
# -------------------------------------------------------------
data_runtime_nanos = {
    'FCFS': [795298.3, 622895.0, 871960.0, 1488338.3],
    'EDF': [916205.0, 1240603.3, 3244306.7, 7074516.7],
    'Greedy': [414073.3, 555678.3, 846105.0, 1651911.7],
    'Adaptive V1': [454403.3, 568593.3, 824573.3, 1711750.0],
    'Adaptive V2': [1057550.0, 641161.7, 1350758.3, 2048136.7]
}

plt.figure(figsize=(7, 4.5))
for algo in algo_order:
    plt.plot(ev_counts, data_runtime_nanos[algo], marker=algo_markers[algo], 
             color=algo_colors[algo], label=algo, linewidth=2, markersize=6)
# Explicit single title without subtitle as required:
plt.title('Observed Scheduler Runtime vs EV Count')
plt.xlabel('EV Count')
plt.ylabel('Scheduler Runtime (ns)')
plt.yscale('log')
plt.grid(True, which='both', linestyle='--', alpha=0.6)
plt.legend(loc='upper left', framealpha=0.95)
plt.tight_layout()
plt.savefig('results/figures/runtime_vs_ev_count.png', dpi=300)
plt.close()
print("Generated Fig. 7: runtime_vs_ev_count.png")

# -------------------------------------------------------------
# Figure 8: Adaptive V2 vs EDF Completion
# -------------------------------------------------------------
plt.figure(figsize=(7, 4.5))
plt.plot(ev_counts, [50.47, 51.55, 53.16, 52.89], marker=algo_markers['EDF'], 
         color=algo_colors['EDF'], label='EDF', linewidth=2, markersize=6)
plt.plot(ev_counts, [59.87, 62.13, 62.74, 62.98], marker=algo_markers['Adaptive V2'], 
         color=algo_colors['Adaptive V2'], label='Adaptive V2', linewidth=2, markersize=6)
plt.title('Completion Rate: Adaptive V2 vs EDF')
plt.xlabel('EV Count')
plt.ylabel('Completion Rate (%)')
plt.ylim(40, 70)
plt.grid(True, linestyle='--', alpha=0.6)
plt.legend(loc='lower right', framealpha=0.95)
plt.tight_layout()
plt.savefig('results/figures/adaptive_vs_edf_completion.png', dpi=300)
plt.close()
print("Generated adaptive_vs_edf_completion.png")

# -------------------------------------------------------------
# Figure 9: PAR by Demand
# -------------------------------------------------------------
par_demand = {
    'FCFS': [1.30, 1.33, 1.63],
    'EDF': [1.30, 1.32, 1.55],
    'Greedy': [1.31, 1.31, 1.56],
    'Adaptive V1': [1.31, 1.31, 1.56],
    'Adaptive V2': [1.31, 1.31, 1.60]
}

plt.figure(figsize=(8, 4.8))
for i, algo in enumerate(algo_order):
    plt.bar(x + (i - 2) * width, par_demand[algo], width, 
            label=algo, color=algo_colors[algo], edgecolor='black', linewidth=0.5)

plt.title('Peak-to-Average Ratio (PAR) by Demand Level')
plt.xlabel('Demand Level')
plt.ylabel('Peak-to-Average Ratio (PAR)')
plt.xticks(x, demand_levels)
plt.ylim(1.0, 1.8)
plt.grid(axis='y', linestyle='--', alpha=0.6)
plt.legend(loc='upper left', framealpha=0.95)
plt.tight_layout()
plt.savefig('results/figures/par_by_demand.png', dpi=300)
plt.close()
print("Generated par_by_demand.png")

# -------------------------------------------------------------
# Figure 1: Architecture Diagram
# -------------------------------------------------------------
fig, ax = plt.subplots(figsize=(10, 8))
ax.axis('off')

boxes = [
    ("Scenario", (0.5, 0.9), "experiment"),
    ("EventGenerator", (0.5, 0.8), "simulation"),
    ("Simulation", (0.5, 0.7), "simulation"),
    ("Scheduler", (0.5, 0.6), "scheduler"),
    ("SchedulingDecision", (0.5, 0.5), "scheduler"),
    ("State Updates", (0.5, 0.4), "model"),
    ("Metrics", (0.5, 0.3), "metrics"),
    ("ExperimentResult", (0.5, 0.2), "experiment"),
    ("CSV Output", (0.5, 0.1), "experiment")
]

for i, (text, pos, layer) in enumerate(boxes):
    ax.text(pos[0], pos[1], f"{text}\n({layer})", ha='center', va='center', 
            bbox=dict(facecolor='lightblue', edgecolor='black', boxstyle='round,pad=1'), fontsize=12)
    if i < len(boxes) - 1:
        next_pos = boxes[i+1][1]
        ax.annotate('', xy=(next_pos[0], next_pos[1]+0.04), xytext=(pos[0], pos[1]-0.04),
                    arrowprops=dict(facecolor='black', shrink=0.05, width=2, headwidth=8))

plt.title('System Architecture and Data Flow', fontsize=16)
plt.savefig('results/figures/architecture_diagram.png', dpi=300)
plt.close()
print("Generated Fig. 1: architecture_diagram.png")

print("\nALL publication figures regenerated with 100% exact labels and formatting!")
