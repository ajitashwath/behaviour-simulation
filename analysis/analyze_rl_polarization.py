import pandas as pd
import glob
import os
import matplotlib.pyplot as plt
import seaborn as sns

def load_metrics(base_dir, condition):
    all_data = []
    pattern = os.path.join(base_dir, condition, "replicate_*", "metrics", "*.parquet")
    files = glob.glob(pattern)
    
    for f in files:
        # Extract replicate number from path
        parts = f.split(os.sep)
        replicate = [p for p in parts if p.startswith("replicate_")][0]
        replicate_idx = int(replicate.split("_")[1])
        
        df = pd.read_parquet(f)
        df['replicate'] = replicate_idx
        df['condition'] = condition
        all_data.append(df)
        
    if not all_data:
        return pd.DataFrame()
        
    return pd.concat(all_data, ignore_index=True)

def analyze():
    # Find the most recent experiment 
    rl_pilot_dir = "e:/behaviour-simulation/rl-pilot"
    experiments = [d for d in os.listdir(rl_pilot_dir) if d.startswith("rl_comparison_")]
    if not experiments:
        print("No pilot runs found.")
        return
        
    # Get the latest directory based on name (sorts correctly due to ISO format dates)
    latest_run = sorted(experiments)[-1]
    base_dir = os.path.join(rl_pilot_dir, latest_run, "data")
    print(f"Analyzing data from: {base_dir}")
    
    # Load both conditions
    random_df = load_metrics(base_dir, "baseline_random")
    ts_df = load_metrics(base_dir, "rl_thompson_sampling")
    
    if random_df.empty or ts_df.empty:
        print("Missing data for one or both conditions.")
        return
        
    combined = pd.concat([random_df, ts_df], ignore_index=True)
    
    # Calculate summary statistics across replicates at the final step
    max_step = combined['timeStep'].max()
    final_step_data = combined[combined['timeStep'] == max_step]
    
    summary = final_step_data.groupby('condition')['polarizationIndex'].agg(['mean', 'std']).reset_index()
    print("\n--- Final Polarization Summary (Step 50) ---")
    print(summary.to_string(index=False))
    
    # Statistical significance using simple t-test
    from scipy import stats
    random_pol = final_step_data[final_step_data['condition'] == 'baseline_random']['polarizationIndex']
    ts_pol = final_step_data[final_step_data['condition'] == 'rl_thompson_sampling']['polarizationIndex']
    t_stat, p_val = stats.ttest_ind(random_pol, ts_pol)
    print(f"\nT-test assuming unequal variance (Welch's): p-value = {p_val:.4f}")
    if p_val < 0.05:
        print("Result: Statistically significant difference in polarization between conditions.")
    else:
        print("Result: No statistically significant difference.")

    # Plotting
    plt.figure(figsize=(10, 6))
    
    # Plot average over time
    time_series = combined.groupby(['condition', 'timeStep'])['polarizationIndex'].mean().reset_index()
    sns.lineplot(data=combined, x='timeStep', y='polarizationIndex', hue='condition', errorbar='sd')
    
    plt.title('Polarization Over Time: RL vs Baseline')
    plt.xlabel('Simulation Step')
    plt.ylabel('Average Polarization')
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig('rl_polarization_comparison.png')
    print("\nSaved comparison plot to rl_polarization_comparison.png")

if __name__ == "__main__":
    analyze()
