# Behavior Simulation Platform

A production-ready platform for simulating large populations of synthetic humans interacting with content over time. Built with **Spring Boot** (control plane) and **Apache Spark** (simulation engine).

## What It Does

This system models:
- **Emotional Contagion** - How moods (JOY, NEUTRAL, RAGE) spread through populations
- **Attention Dynamics** - Decay and recovery of attention spans
- **Content Diffusion** - How content spreads based on emotional type
- **Fatigue Accumulation** - Engagement exhaustion over time
- **Outrage Amplification** - Asymmetric spread rates for different emotions

### Key Experiment

The platform demonstrates that **if rage spreads 10% faster than joy**, significant behavioral divergence emerges over time.

## Architecture

```
┌─────────────────────────────────────┐
│         Control Plane               │
│        (Spring Boot)                │
│  ┌──────────────────────────────┐   │
│  │   REST API                   │   │
│  │   POST /experiments          │   │
│  │   POST /experiments/{id}/step│   │
│  │   GET  /experiments/{id}/...│   │
│  └──────────────────────────────┘   │
│                │                    │
│                ▼                    │
│  ┌──────────────────────────────┐   │
│  │   Experiment Service         │   │
│  │   - Lifecycle management     │   │
│  │   - Spark job orchestration  │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────┐
│       Simulation Engine             │
│        (Apache Spark)               │
│  ┌──────────────────────────────┐   │
│  │   Step Pipeline              │   │
│  │   1. Exposure Computation    │   │
│  │   2. Reaction Calculation    │   │
│  │   3. Mood Contagion          │   │
│  │   4. Attention Decay         │   │
│  │   5. Fatigue Accumulation    │   │
│  │   6. Metrics Aggregation     │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────┐
│           Storage                   │
│  ┌──────────┐  ┌──────────────┐     │
│  │ Parquet  │  │  PostgreSQL  │     │
│  │ (State)  │  │  (Metadata)  │     │
│  └──────────┘  └──────────────┘     │
└─────────────────────────────────────┘
```

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker (optional, for containerized deployment)

### Build

```bash
# Clone and build
cd behaviour-simulation
mvn clean package -DskipTests
```

### Run Locally

```bash
# Start Spring Boot application
cd control-plane
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

### Run with Docker

```bash
docker-compose -f docker/docker-compose.yml up -d
```

## API Reference

### Create Experiment

```bash
curl -X POST http://localhost:8080/experiments \
  -H "Content-Type: application/json" \
  -d '{
    "name": "rage-divergence-test",
    "description": "Testing asymmetric emotion spread",
    "params": {
      "seed": 42,
      "populationSize": 10000,
      "rageSpreadMultiplier": 1.1,
      "joySpreadMultiplier": 1.0
    }
  }'
```

### Execute Steps

```bash
# Execute 10 simulation steps
curl -X POST http://localhost:8080/experiments/{id}/step \
  -H "Content-Type: application/json" \
  -d '{"steps": 10}'
```

### Get Current State

```bash
curl http://localhost:8080/experiments/{id}/state
```

### Get Metrics History

```bash
curl http://localhost:8080/experiments/{id}/metrics
```

## Simulation Model

### Human Attributes

| Attribute | Range | Description |
|-----------|-------|-------------|
| `mood` | JOY, NEUTRAL, RAGE | Current emotional state |
| `attentionSpan` | [0, 1] | Capacity for content consumption |
| `addictionCoeff` | [0, 1] | Tendency to seek engagement |
| `reactionProb` | [0, 1] | Likelihood of reacting to content |
| `fatigue` | [0, 1] | Accumulated exhaustion |

### Simulation Step

Each step:
1. **Exposure** - Determines which humans see which content
2. **Reaction** - Calculates whether humans react (based on attention, fatigue, content intensity)
3. **Mood Contagion** - Updates moods based on population distribution and spread multipliers
4. **Attention Decay** - Reduces attention, with recovery for low-attention humans
5. **Fatigue** - Accumulates from reactions, recovers during non-engagement

### Key Metrics

| Metric | Formula | Meaning |
|--------|---------|---------|
| **Mood Entropy** | `-Σ(p × log₂(p))` | Diversity of moods (0=polarized, 1.58=balanced) |
| **Dominance Ratio** | `max(count) / total` | Fraction in dominant mood |
| **Attention Gini** | Standard Gini | Inequality of attention (0=equal, 1=unequal) |
| **Rage R-value** | `new_rage / current_rage` | Reproduction rate (>1 = epidemic) |
| **Fatigue P95** | 95th percentile | System collapse indicator |

## Sample Experiment

### Rage vs Joy Divergence

```bash
# Create experiment with faster rage spread
curl -X POST http://localhost:8080/experiments \
  -H "Content-Type: application/json" \
  -d '{
    "name": "rage-joy-divergence",
    "params": {
      "seed": 42,
      "populationSize": 10000,
      "rageSpreadMultiplier": 1.1,
      "joySpreadMultiplier": 1.0
    }
  }'

# Run 50 steps
for i in {1..5}; do
  curl -X POST http://localhost:8080/experiments/{id}/step \
    -d '{"steps": 10}'
  sleep 2
done

# Get metrics
curl http://localhost:8080/experiments/{id}/metrics | jq .
```

**Expected Results:**
- Mood entropy decreases over time
- Rage proportion increases
- Dominance ratio approaches 0.6-0.7
- Fatigue levels increase

## Configuration

All parameters in `application.yml`:

```yaml
simulation:
  defaults:
    population-size: 10000
    content-count: 100
    rage-spread-multiplier: 1.0
    joy-spread-multiplier: 1.0
    attention-decay-rate: 0.05
    fatigue-accumulation-rate: 0.02
  storage:
    base-path: ./data/experiments
  spark:
    master: local[*]
```

## Extending the System

### Add New Emotion Types

1. Update `Mood` enum in `common/model/Mood.java`
2. Add spread multiplier in `SimulationParams`
3. Update `MoodContagionStep` transition logic

### Add Network Topology

1. Create `TopologyProvider` interface
2. Implement graph-based exposure in `ExposureStep`
3. Add topology parameter to `SimulationParams`

### Add New Metrics

1. Add fields to `Metrics.java`
2. Compute in `MetricsAggregator`
3. Expose via API response

## Project Structure

```
behaviour-simulation/
├── common/                    # Shared schemas and DTOs
│   └── src/main/java/.../
│       ├── model/            # Human, Content, Metrics, enums
│       └── dto/              # API request/response objects
├── control-plane/            # Spring Boot application
│   └── src/main/java/.../
│       ├── controller/       # REST endpoints
│       ├── service/          # Business logic
│       ├── entity/           # JPA entities
│       └── config/           # Configuration classes
├── simulation-engine/        # Spark jobs
│   └── src/main/java/.../
│       ├── step/             # Simulation step processors
│       └── metrics/          # Aggregation logic
└── docker/                   # Container configuration
```

## Requirements

- **Java**: 17+
- **Apache Spark**: 3.5.0
- **Spring Boot**: 3.2.2
- **Database**: H2 (dev) / PostgreSQL (prod)