# Research Platform Roadmap

## ✅ Completed (Phases 1-2 + RL Core)

### Phase 1: Network Integration
- ✅ 3 network topologies (Barabási-Albert, Watts-Strogatz, Random)
- ✅ Network-based exposure with social proof
- ✅ Network edge generation and persistence
- ✅ Network metrics computation

### Phase 2: Experimental Framework
- ✅ PilotExperiment (quick validation)
- ✅ NetworkComparisonExperiment (6×10 factorial design)
- ✅ Statistical analysis pipeline (ANOVA, plots)
- ✅ Complete documentation (EXPERIMENTS.md, QUICKSTART.md)

### Phase 3A: RL Recommender (Core)
- ✅ Thompson Sampling recommender (Bayesian bandit)
- ✅ Random baseline recommender
- ✅ Polarization metrics (polarizationIndex, extremismRate, fragmentationIndex)
- ✅ Engagement tracking (avgEngagement, engagementGini)
- ✅ Configuration infrastructure

---

## 🚧 Immediate Next Steps (1-3 hours)

### Complete RL Integration

**Priority 1: SimulationJob Integration** (~1 hour)
```java
// In SimulationJob.java
private Recommender recommender;

// In initializePopulation()
if (params.getRecommenderType() != RecommenderType.NONE) {
    recommender = createRecommender();
}

// In executeStep()
if (recommender != null) {
    Dataset<Row> rankedContent = recommender.rankContent(
        humansDF, contentDF, previousInteractions, currentStep);
    // Use rankedContent in exposure logic
}
```

**Files to modify:**
1. `SimulationJob.java` - Add recommender field and integration
2. `NetworkExposureStep.java` - Accept ranked content scores
3. Create `createRecommender()` helper method

**Priority 2: RL Comparison Experiment** (~30 min)
```java
// New file: RLComparisonExperiment.java
// 2 conditions: Thompson vs Random
// 10 replicates each
// Measure: polarizationIndex over time
```

**Priority 3: Test Run** (~1 hour)
```bash
# Pilot test
java -cp ... RLComparisonExperiment ./rl-pilot

# Analyze
python analysis/analyze_rl_amplification.py ./rl-pilot
```

---

## 📊 Research Tracks (Choose 1-2)

### Track A: Complete RL Study (2-3 weeks)
Finish algorithmic amplification research:

1. **Week 1: Implementation**
   - [ ] Complete SimulationJob integration
   - [ ] Create RLComparisonExperiment
   - [ ] Run pilot (1K nodes)
   - [ ] Debug and validate

2. **Week 2: Experimentation**
   - [ ] Run full experiment (10K nodes × 100 steps × 20 runs)
   - [ ] Statistical analysis (Thompson vs Random t-test)
   - [ ] Create publication figures

3. **Week 3: Writing**
   - [ ] Write methods section (RL algorithm)
   - [ ] Write results (polarization amplification)
   - [ ] Combine with network topology results
   - [ ] Submit to ICWSM or WWW

**Expected Contribution:** "Network structure AND algorithms jointly determine polarization"

---

### Track B: Individual Differences (3-4 weeks)

Add personality traits to study heterogeneous populations:

1. **Model Extension**
   - [ ] Add Big Five traits to Human model
   - [ ] Map traits → emotion regulation (neuroticism → rumination)
   - [ ] Implement regulation strategies (suppression, reappraisal)

2. **Experimentation**
   - [ ] Generate trait distributions (from empirical data)
   - [ ] Run simulations with trait-based regulation
   - [ ] Measure: trait × topology interactions

3. **Analysis**
   - [ ] Mixed-effects models (traits as covariates)
   - [ ] Identify vulnerable populations (high neuroticism)

**Expected Contribution:** "Personality moderates network effects on contagion"

---

### Track C: Platform Interventions (2-3 weeks)

Test policy interventions to reduce polarization:

1. **Intervention Types**
   - [ ] FrictionIntervention (delay before sharing RAGE content)
   - [ ] DiversityNudge (recommend opposite-emotion content)
   - [ ] CooldownPeriod (timeout after extreme reactions)

2. **Experimental Design**
   - [ ] Factorial: 2³ = 8 conditions
   - [ ] Compare to baseline (no intervention)
   - [ ] Measure: reduction in polarizationIndex

3. **Causal Inference**
   - [ ] Difference-in-differences estimation
   - [ ] Effect size by network topology
   - [ ] Cost-benefit analysis

**Expected Contribution:** "Evidence-based interventions for platform designers"

---

## 🔬 Validation & Calibration (2-3 weeks)

Make model predictions match real-world data:

### Empirical Data Collection
- [ ] Scrape Twitter emotion transitions (public API)
- [ ] Label emotional content (GPT-4 or manual coding)
- [ ] Compute transition matrices (JOY→RAGE, etc.)

### Calibration
- [ ] Implement ABC (Approximate Bayesian Computation)
- [ ] Fit model parameters to match empirical distributions
- [ ] Validate on held-out data

### Sensitivity Analysis
- [ ] Compute Sobol indices (which parameters matter most?)
- [ ] Test robustness to parameter variations

**Expected Contribution:** "Calibrated model with predictive validity"

---

## 📝 Publication Path

### Option 1: Fast Workshop Paper (4 weeks)
**Target:** NeurIPS Workshop on Generative AI and Society

**Scope:**
- Network topology effects only (Phase 1-2)
- 4-6 pages
- Limited experiments (BA vs WS vs Random)

**Timeline:**
- Week 1: Run experiments
- Week 2: Analysis and figures
- Week 3-4: Writing and submission

---

### Option 2: Full Conference Paper (8-10 weeks)
**Target:** ICWSM, WWW, or Nature Communications

**Scope:**
- Network + RL + validation
- 8-12 pages
- All experiments (topology × algorithm)
- Empirical calibration

**Timeline:**
- Week 1-3: Complete RL integration + experiments
- Week 4-5: Validation and calibration
- Week 6-7: Statistical analysis
- Week 8-10: Paper writing and submission

---

### Option 3: Journal Article (12-16 weeks)
**Target:** PNAS, Science Advances, Nature Human Behaviour

**Scope:**
- All tracks (network + RL + traits + interventions)
- Comprehensive validation
- Policy implications
- 15-20 pages + supplement

**Timeline:**
- Month 1: RL + interventions
- Month 2: Validation + calibration
- Month 3: All experiments
- Month 4: Writing + revisions

---

## 🎯 Recommended Path

**For Maximum Impact in Minimum Time:**

1. **Complete RL Integration** (1 week)
   - Finish SimulationJob integration
   - Run RL comparison experiments
   - Validate polarization amplification

2. **Write Conference Paper** (3 weeks)
   - Target: ICWSM 2026 or WWW 2026
   - Contributions: (1) Network topology effects, (2) RL amplification
   - 8 pages + references

3. **Submit Code & Data** (1 week)
   - GitHub repository with DOI
   - Docker image for reproducibility
   - Example notebooks

**Total: 5 weeks to submission**

**Expected Outcome:**
- High-impact conference paper
- Open-source research platform
- Foundation for follow-up journal article

---

## 📋 Immediate Action Items

**This Week:**
1. [ ] Integrate recommenders into SimulationJob
2. [ ] Create RLComparisonExperiment
3. [ ] Run pilot test (1K nodes)
4. [ ] Verify polarization amplification

**Next Week:**
1. [ ] Run full RL experiments (10K nodes × 20 runs)
2. [ ] Statistical analysis (t-tests, effect sizes)
3. [ ] Create figures (polarization over time)

**Week 3-4:**
1. [ ] Run network topology experiments (if not done)
2. [ ] Combined analysis (network + algorithm effects)
3. [ ] Start paper writing

---

## 💡 Quick Wins (If Time-Constrained)

**Minimal Publishable Unit (2 weeks):**
- Network topology only (skip RL)
- BA vs Random (2 conditions instead of 6)
- 5 replicates instead of 10
- 4-page workshop paper

**OR**

**RL Amplification Only (2 weeks):**
- Thompson vs Random on all-to-all network
- Focus solely on algorithmic effects
- 4-page workshop paper

Both options are publishable and set up future full papers.

---

**Status:** Core platform complete, integration and experiments remain
**Next Milestone:** RL integration complete (Target: 1 week)
**Ultimate Goal:** Published research on network-embedded emotional contagion
