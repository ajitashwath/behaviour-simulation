import numpy as np
from scipy import stats
import pandas as pd
import json

def validate_thompson_sampling(n_simulations=1000, n_trials=1000, seed=42):
    """
    Validates the Gaussian approximation of the Beta distribution used in the 
    Spark implementation of Thompson Sampling.
    
    In the Spark job, Beta(alpha, beta) is approximated as:
    score = (alpha / (alpha + beta)) + sqrt(alpha + beta) * 0.1 * N(0,1)
    
    We compare this against true sampling from Beta(alpha, beta).
    """
    np.random.seed(seed)
    
    # Test cases representing different states of the bandit
    test_cases = [
        {"name": "Cold Start", "alpha": 1, "beta": 1},
        {"name": "Early Stages", "alpha": 5, "beta": 5},
        {"name": "Early High Eng", "alpha": 10, "beta": 2},
        {"name": "Early Low Eng", "alpha": 2, "beta": 10},
        {"name": "Mid Stages", "alpha": 50, "beta": 50},
        {"name": "Late Stages", "alpha": 500, "beta": 500},
        {"name": "Late High Eng", "alpha": 800, "beta": 200},
    ]
    
    results = []
    
    for case in test_cases:
        alpha = case["alpha"]
        beta = case["beta"]
        
        # True Beta Sampling
        true_samples = stats.beta.rvs(alpha, beta, size=n_simulations)
        
        # Gaussian Approximation Sampling (from Spark job logic)
        mean = alpha / (alpha + beta)
        exploration_rate = 0.1
        noise = np.random.normal(0, 1, size=n_simulations)
        exploration_bonus = np.sqrt(alpha + beta) * exploration_rate
        approx_samples = mean + exploration_bonus * noise
        
        # KS Test to measure distribution divergence
        ks_stat, p_value = stats.ks_2samp(true_samples, approx_samples)
        
        results.append({
            "State": case["name"],
            "Alpha": alpha,
            "Beta": beta,
            "KS-Stat": round(ks_stat, 4),
            "P-Value": round(p_value, 4)
        })
        
    df_results = pd.DataFrame(results)
    print("=== KS Test Comparison ===")
    print(df_results.to_string(index=False))
    
    # Simulating a multi-armed bandit Decision Divergence
    # 10 arms randomly picking true vs approx
    divergence_count = 0
    total_decisions = 10000
    
    # Generate 10 random arms
    arms_alpha = np.random.randint(1, 100, 10)
    arms_beta = np.random.randint(1, 100, 10)
    
    for _ in range(total_decisions):
        # Sample true
        true_scores = [stats.beta.rvs(a, b) for a, b in zip(arms_alpha, arms_beta)]
        best_true = np.argmax(true_scores)
        
        # Sample approx
        approx_scores = []
        for a, b in zip(arms_alpha, arms_beta):
            mean = a / (a + b)
            noise = np.random.normal(0, 1)
            approx_scores.append(mean + np.sqrt(a + b) * 0.1 * noise)
            
        best_approx = np.argmax(approx_scores)
        
        if best_true != best_approx:
            divergence_count += 1
            
    divergence_rate = divergence_count / total_decisions
    print("\n=== Decision Divergence ===")
    print(f"Total Decisions Simulated: {total_decisions}")
    print(f"Mismatched Decisions: {divergence_count}")
    print(f"Divergence Rate: {divergence_rate * 100:.2f}%")
    
    if divergence_rate < 0.10: # Reasonable threshold for 10% tolerance in distributed environment
        print("Conclusion: The Gaussian approximation is robust and acceptable for the Spark execution environment.")
    else:
        print("Conclusion: The Gaussian approximation deviates significantly and should be rewritten if true TS is required.")

if __name__ == "__main__":
    validate_thompson_sampling()
