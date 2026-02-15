# Running Network Topology Experiments

This guide explains how to run the network topology comparison experiments for publishable research on emotional contagion.

## Quick Start: Pilot Test

Test all network topologies with a small population (1000 nodes, 20 steps) to verify everything works:

```bash
# Build the project
./mvnw clean package -DskipTests

# Run pilot experiment
java -cp simulation-engine/target/simulation-engine-1.0-SNAPSHOT.jar \
  com.simulation.spark.experiment.PilotExperiment \
  ./pilot-results
```

**Expected output:**
- 4 topology tests (all-to-all, Barabási-Albert, Watts-Strogatz, random)
- Results saved to `./pilot-results/`
- Runtime: ~5-10 minutes

**Validation checks:**
1. All 4 topologies complete without errors
2. Network edges saved to `<topology>/network/edges.parquet`
3. Metrics show rage spread differences between topologies

---

## Full Experiment: Publication Run

Run the complete experimental design (6 conditions × 10 replicates = 60 runs):

```bash
# Run full experiment (takes 2-4 hours)
java -cp simulation-engine/target/simulation-engine-1.0-SNAPSHOT.jar \
  com.simulation.spark.experiment.NetworkComparisonExperiment \
  ./experiment-results
```

### Experimental Conditions

| Condition | Network Type | Parameters | Expected Pattern |
|-----------|-------------|------------|------------------|
| `all_to_all` | ALL_TO_ALL | - | Baseline (unrealistic) |
| `ba_m2` | Barabási-Albert | m=2 | Sparse scale-free |
| `ba_m5` | Barabási-Albert | m=5 | Dense scale-free |
| `ws_p005` | Watts-Strogatz | k=6, p=0.05 | Strong small-world |
| `ws_p050` | Watts-Strogatz | k=6, p=0.50 | Weak small-world |
| `random_k6` | Random | k=6 | Erdős-Rényi baseline |

### Configuration

- **Population**: 10,000 humans
- **Steps**: 100 per run
- **Replicates**: 10 per condition (different seeds)
- **Output**: Labeled by condition and replicate

---

## Statistical Analysis

After experiments complete, run Python analysis:

```bash
# Install dependencies
pip install pandas numpy scipy matplotlib seaborn pyarrow

# Run analysis
python analysis/analyze_experiment.py ./experiment-results/network_comparison_<timestamp>
```

**Output:**
- `analysis/time_series.png` - Rage dynamics over time by topology
- `analysis/final_comparison.png` - Bar chart of final rage levels
- `analysis/statistical_results.json` - ANOVA and t-test results

### Statistical Tests

1. **One-Way ANOVA**: Tests H1 (topology affects rage spread)
2. **Pairwise t-tests** (Bonferroni corrected): Compares specific topologies
3. **Time series analysis**: Visualizes dynamics over time

**Expected findings:**
- ANOVA p < 0.001 (topology effect is significant)
- Barabási-Albert (m=5) shows fastest rage spread
- Watts-Strogatz (p=0.05) shows clustering effects

---

## Interpreting Results

### Key Metrics

From `metrics/*.parquet`:
- `rageCount` - Number of humans in RAGE state
- `joyCount` - Number in JOY state  
- `neutralCount` - Number in NEUTRAL state
- `totalInteractions` - Content consumption events

### Network Metrics

From `network/edges.parquet`:
- `num_edges` - Total edges in network
- `average_degree` - Mean connections per node
- `max_degree` - Hub size (important for scale-free)
- `degree_stddev` - Degree heterogeneity

### Publication Figures

Use generated plots directly in paper:
- **Figure 1**: Time series comparison (all topologies)
- **Figure 2**: Final rage levels (bar chart with error bars)
- **Table 1**: ANOVA results (F-statistic, p-value, effect size)
- **Table 2**: Pairwise comparisons (significant differences)

---

## Troubleshooting

### OutOfMemoryError

Increase Spark memory:
```bash
export SPARK_DRIVER_MEMORY=8g
export SPARK_EXECUTOR_MEMORY=8g
```

Or modify in experiment code:
```java
.config("spark.driver.memory", "16g")
.config("spark.executor.memory", "16g")
```

### Slow Performance

Reduce population or steps for faster iteration:
```java
private static final int POPULATION_SIZE = 5_000;  // instead of 10,000
private static final int NUM_STEPS = 50;          // instead of 100
```

### Missing Network Edges

If `network/edges.parquet` not found, check:
1. NetworkType is not ALL_TO_ALL
2. Initialization completed successfully
3. Storage path has write permissions

---

## Next Steps After Experiments

1. **Validate Results**
   - Check ANOVA p-value < 0.05
   - Verify scale-free shows fastest spread
   - Confirm time series matches theory

2. **Write Paper Sections**
   - Methods: Experimental design + parameters
   - Results: Statistical tests + figures
   - Discussion: Implications for platforms

3. **Prepare Submission**
   - Target: NeurIPS Workshop or ICWSM
   - Format: 4-8 pages depending on venue
   - Include: Open code repository + Docker image

4. **Optional Extensions**
   - Add RL recommenders
   - Test intervention policies
   - Calibrate to real Twitter data

---

## Research Checklist

Before submission, verify:

- [ ] All 60 runs completed successfully
- [ ] ANOVA shows significant topology effect (p < 0.05)
- [ ] At least one pairwise comparison is significant
- [ ] Figures are publication quality (300 DPI, clear labels)
- [ ] Code is documented and reproducible
- [ ] Results match theoretical predictions
- [ ] Limitations are acknowledged

**Estimated timeline:**
- Week 1: Run experiments (2-4 hours compute)
- Week 2: Analyze results + create figures
- Week 3-4: Write paper
- Week 5: Submit to workshop

Good luck with your research! 🚀
