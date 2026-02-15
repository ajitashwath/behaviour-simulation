# Quick Start Guide

## 1. Validate Network Generation (30 seconds)

```bash
# Build
./mvnw clean package -DskipTests

# Run validator
java -cp simulation-engine/target/simulation-engine-1.0-SNAPSHOT.jar \
  com.simulation.spark.network.NetworkValidator
```

**Expected:** All 3 topologies validate with network metrics printed.

## 2. Run Pilot Experiment (5-10 minutes)

```bash
java -cp simulation-engine/target/simulation-engine-1.0-SNAPSHOT.jar \
  com.simulation.spark.experiment.PilotExperiment ./pilot-results
```

**Expected:** 4 topologies complete, results in `./pilot-results/`

## 3. Run Full Experiment (2-4 hours)

```bash
java -cp simulation-engine/target/simulation-engine-1.0-SNAPSHOT.jar \
  com.simulation.spark.experiment.NetworkComparisonExperiment ./experiment-results
```

## 4. Analyze Results

```bash
pip install pandas numpy scipy matplotlib seaborn pyarrow
python analysis/analyze_experiment.py ./experiment-results/network_comparison_<timestamp>
```

See [EXPERIMENTS.md](EXPERIMENTS.md) for full details.
