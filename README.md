# Emotional Contagion# Behavior Simulation Platform

## New Feature: Richer Emotion Model (v2.0)

We have introduced a **continuous 2D affect space** (Valence × Arousal) to replace the discrete mood system. This allows for more realistic emotion dynamics, including mixed states and intensity-weighted contagion.

### Enabling Continuous Emotions
To use the new model, set `useContinuousEmotions = true` in your simulation configuration or via command line arguments.

**Parameters:**
- `useContinuousEmotions`: Enable continuous model (default: `false` for backward compatibility)
- `emotionRegressionRate`: Rate at which emotions return to baseline (default: `0.05`)
- `arousalDecayRate`: Additional decay rate for arousal (default: `0.10`)
- `contentInfluenceWeight`: Impact of content on emotions (default: `0.15`)
- `socialInfluenceWeight`: Impact of neighbors on emotions (default: `0.10`)

### Visualization Tools
We provide Python tools to visualize the affect space dynamics:

```bash
# Install dependencies
pip install -r requirements.txt

# Visualize a single simulation step (Snapshot)
python analysis/emotion_visualizer.py data/state/step_00050.parquet

# Visualize evolution over time (Time Series)
python analysis/emotion_visualizer.py data/state/
```

Outputs include:
- **Affect Distribution**: 2D scatter plot of population emotions.
- **Density Heatmap**: Concentration of emotions in the Valence-Arousal space.
- **Trajectories**: Emotional paths of individual agents over time.
- **Summary Statistics**: Mean valence/arousal and mood fraction evolution.

---

## What This Is

A Spark-based simulator for studying how social network structure affects emotional contagion. Built for **publishable research** on platform dynamics and polarization.

## Status: ✅ Ready for Experiments

**Completed:**
- ✅ 3 Network topologies (scale-free, small-world, random)
- ✅ Network-based exposure with social proof dynamics
- ✅ Experimental framework (6 conditions × 10 replicates)
- ✅ Statistical analysis pipeline (ANOVA, plots)
- ✅ Clean compilation

## Quick Start

### 1. Validate (30 sec)
```bash
java -cp simulation-engine/target/simulation-engine-1.0-SNAPSHOT.jar \
  com.simulation.spark.network.NetworkValidator
```

### 2. Pilot Test (10 min)
```bash
java -cp simulation-engine/target/simulation-engine-1.0-SNAPSHOT.jar \
  com.simulation.spark.experiment.PilotExperiment ./pilot
```

### 3. Full Experiment (3 hours)
```bash
java -cp simulation-engine/target/simulation-engine-1.0-SNAPSHOT.jar \
  com.simulation.spark.experiment.NetworkComparisonExperiment ./results
```

### 4. Analyze
```bash
python analysis/analyze_experiment.py ./results/network_comparison_*
```

## Research Question

**"How does network structure mediate emotional contagion?"**

- **H1**: Scale-free networks amplify rage faster (hubs = superspreaders)
- **H2**: Small-world networks show clustering effects (echo chambers)
- **H3**: Effect persists across time (structural determinism)

## Publication Path

1. Run experiments (~3 hours)
2. Analyze results (ANOVA p < 0.001 expected)
3. Write 4-8 page paper
4. Submit to NeurIPS Workshop or ICWSM

**Timeline:** 4-6 weeks to submission

## Documentation

- **[QUICKSTART.md](QUICKSTART.md)** - Commands to run
- **[EXPERIMENTS.md](EXPERIMENTS.md)** - Full experimental guide  
- **[walkthrough.md](artifacts/walkthrough.md)** - Technical implementation details
- **[AGENTS.md](AGENTS.md)** - Original task breakdown

## Architecture

```
SimulationParams → NetworkTopology → Edges DataFrame
                                    ↓
Humans + Content → NetworkExposureStep → Exposures
                                    ↓
                           ReactionStep → MoodContagion
```

## Key Components

| Component | Purpose |
|-----------|---------|
| `BarabasiAlbertNetwork` | Scale-free (power-law degree distribution) |
| `WattsStrogatzNetwork` | Small-world (high clustering + short paths) |
| `RandomGraph` | Baseline control (Erdős-Rényi) |
| `NetworkExposureStep` | Social proof diffusion |
| `NetworkComparisonExperiment` | 6×10 factorial design |
| `analyze_experiment.py` | Statistical testing + figures |

## Requirements

- Java 17+
- Apache Spark 3.5
- Maven
- Python 3.8+ (for analysis)

## Research Output

**Data:** 60 simulation runs × 100 steps × 10K nodes = 60M observations

**Figures:**
- Time series (rage spread by topology)
- Final comparison (bar chart with CI)
- Network visualizations (optional)  

**Statistics:**
- ANOVA F-test (main effect)
- Pairwise t-tests (Bonferroni corrected)
- Effect sizes (η²)

## Next Steps

Choose your path:

**Fast Track (2 weeks):**
- Run experiments
- Simple analysis
- Workshop paper

**Full Track (8-10 weeks):**
- Add RL recommenders
- Calibrate to real data
- Full conference paper

---

**Status:** Phase 1 & 2 Complete | Ready to run experiments | 2026-02-15
