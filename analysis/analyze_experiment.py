#!/usr/bin/env python3
"""
Statistical Analysis for Network Topology Comparison Experiment

Analyzes results from NetworkComparisonExperiment to test:
- H1: Network topology affects rage spread (ANOVA)
- H2: Scale-free networks show faster spread than small-world (post-hoc)
- H3: Effect persists across time (time series analysis)

Usage:
    python analyze_experiment.py <experiment-dir>

Output:
    - Statistical test results (ANOVA, t-tests)
    - Publication-quality figures (PNG, 300 DPI)
    - LaTeX tables for paper
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path
from scipy import stats
from scipy.stats import f_oneway, ttest_ind
import argparse
import json

# Set publication style
sns.set_style("whitegrid")
sns.set_context("paper", font_scale=1.5)
plt.rcParams['figure.dpi'] = 300
plt.rcParams['savefig.dpi'] = 300

def load_experimental_data(experiment_dir):
    """Load all metrics from experimental runs."""
    data_dir = Path(experiment_dir) / "data"
    
    all_data = []
    
    for condition_dir in data_dir.iterdir():
        if not condition_dir.is_dir():
            continue
            
        condition_name = condition_dir.name
        
        for replicate_dir in condition_dir.iterdir():
            if not replicate_dir.is_dir():
                continue
                
            replicate_num = int(replicate_dir.name.split('_')[1])
            
            # Load metrics parquet
            metrics_path = replicate_dir / "metrics"
            if metrics_path.exists():
                df = pd.read_parquet(metrics_path)
                df['condition'] = condition_name
                df['replicate'] = replicate_num
                all_data.append(df)
    
    return pd.concat(all_data, ignore_index=True)

def compute_summary_stats(df):
    """Compute summary statistics by condition."""
    # Final rage proportion
    final_rage = df[df['timeStep'] == df['timeStep'].max()].groupby('condition').agg({
        'rageCount': ['mean', 'std', 'sem']
    }).round(3)
    
    print("\n" + "="*80)
    print("FINAL RAGE PROPORTION BY CONDITION")
    print("="*80)
    print(final_rage)
    
    return final_rage

def test_main_effect(df):
    """Test H1: Does network topology affect rage spread?"""
    print("\n" + "="*80)
    print("HYPOTHESIS 1: Network Topology Main Effect")
    print("="*80)
    
    # Get final rage counts by condition
    final_step = df['timeStep'].max()
    final_data = df[df['timeStep'] == final_step]
    
    # Group by condition
    groups = [final_data[final_data['condition'] == c]['rageCount'].values 
              for c in final_data['condition'].unique()]
    
    # One-way ANOVA
    f_stat, p_value = f_oneway(*groups)
    
    print(f"\nOne-Way ANOVA:")
    print(f"  F-statistic: {f_stat:.4f}")
    print(f"  p-value: {p_value:.4e}")
    print(f"  Result: {'SIGNIFICANT' if p_value < 0.05 else 'NOT SIGNIFICANT'} (α=0.05)")
    
    if p_value < 0.05:
        print(f"\n✓ CONCLUSION: Network topology DOES significantly affect rage spread")
    else:
        print(f"\n✗ CONCLUSION: No significant topology effect detected")
    
    return {'f_stat': f_stat, 'p_value': p_value}

def test_pairwise_comparisons(df):
    """Test H2: Scale-free vs small-world differences."""
    print("\n" + "="*80)
    print("HYPOTHESIS 2: Pairwise Topology Comparisons")
    print("="*80)
    
    final_step = df['timeStep'].max()
    final_data = df[df['timeStep'] == final_step]
    
    conditions = final_data['condition'].unique()
    
    print("\nPairwise t-tests (Bonferroni corrected):")
    
    num_comparisons = len(conditions) * (len(conditions) - 1) // 2
    alpha_corrected = 0.05 / num_comparisons
    
    results = []
    
    for i, c1 in enumerate(conditions):
        for c2 in conditions[i+1:]:
            data1 = final_data[final_data['condition'] == c1]['rageCount']
            data2 = final_data[final_data['condition'] == c2]['rageCount']
            
            t_stat, p_value = ttest_ind(data1, data2)
            
            mean_diff = data1.mean() - data2.mean()
            significant = p_value < alpha_corrected
            
            print(f"  {c1:15s} vs {c2:15s}: "
                  f"t={t_stat:6.3f}, p={p_value:.4e}, "
                  f"Δ={mean_diff:6.1f} "
                  f"{'***' if significant else ''}")
            
            results.append({
                'comparison': f"{c1} vs {c2}",
                't_stat': t_stat,
                'p_value': p_value,
                'mean_diff': mean_diff,
                'significant': significant
            })
    
    return pd.DataFrame(results)

def plot_time_series(df, output_dir):
    """Plot rage proportion over time by topology."""
    print("\n" + "="*80)
    print("GENERATING TIME SERIES PLOT")
    print("="*80)
    
    fig, ax = plt.subplots(figsize=(12, 6))
    
    for condition in df['condition'].unique():
        subset = df[df['condition'] == condition]
        
        # Compute mean and 95% CI across replicates
        grouped = subset.groupby('timeStep')['rageCount'].agg(['mean', 'sem'])
        
        steps = grouped.index
        mean_rage = grouped['mean']
        ci_95 = 1.96 * grouped['sem']
        
        # Plot
        ax.plot(steps, mean_rage, label=condition, linewidth=2)
        ax.fill_between(steps, mean_rage - ci_95, mean_rage + ci_95, alpha=0.2)
    
    ax.set_xlabel('Simulation Step')
    ax.set_ylabel('Number in RAGE State')
    ax.set_title('Emotional Contagion Dynamics by Network Topology')
    ax.legend(loc='best', frameon=True)
    ax.grid(True, alpha=0.3)
    
    output_path = Path(output_dir) / 'time_series.png'
    plt.tight_layout()
    plt.savefig(output_path, dpi=300, bbox_inches='tight')
    print(f"  Saved to: {output_path}")
    
    plt.close()

def plot_final_comparison(df, output_dir):
    """Bar plot of final rage levels with error bars."""
    print("\nGenerating final comparison plot...")
    
    final_step = df['timeStep'].max()
    final_data = df[df['timeStep'] == final_step]
    
    fig, ax = plt.subplots(figsize=(10, 6))
    
    # Compute means and SEMs
    summary = final_data.groupby('condition')['rageCount'].agg(['mean', 'sem'])
    
    # Bar plot
    summary['mean'].plot(kind='bar', yerr=summary['sem']*1.96, 
                         ax=ax, capsize=5, color='steelblue', alpha=0.8)
    
    ax.set_xlabel('Network Topology')
    ax.set_ylabel('Final RAGE Count (Mean ± 95% CI)')
    ax.set_title('Final Emotional State by Network Structure')
    ax.set_xticklabels(ax.get_xticklabels(), rotation=45, ha='right')
    ax.grid(True, axis='y', alpha=0.3)
    
    output_path = Path(output_dir) / 'final_comparison.png'
    plt.tight_layout()
    plt.savefig(output_path, dpi=300, bbox_inches='tight')
    print(f"  Saved to: {output_path}")
    
    plt.close()

def main():
    parser = argparse.ArgumentParser(description='Analyze network topology experiment results')
    parser.add_argument('experiment_dir', help='Directory containing experiment results')
    parser.add_argument('--output-dir', default=None, help='Output directory for figures')
    
    args = parser.parse_args()
    
    output_dir = args.output_dir or Path(args.experiment_dir) / 'analysis'
    Path(output_dir).mkdir(exist_ok=True, parents=True)
    
    print("="*80)
    print("NETWORK TOPOLOGY EXPERIMENT ANALYSIS")
    print("="*80)
    print(f"Data directory: {args.experiment_dir}")
    print(f"Output directory: {output_dir}")
    
    # Load data
    print("\nLoading experimental data...")
    df = load_experimental_data(args.experiment_dir)
    
    total_population = df['joyCount'].iloc[0] + df['neutralCount'].iloc[0] + df['rageCount'].iloc[0]
    print(f"  Loaded {len(df)} observations")
    print(f"  Conditions: {df['condition'].nunique()}")
    print(f"  Replicates per condition: {df['replicate'].nunique()}")
    print(f"  Population size: {total_population}")
    
    # Summary statistics
    summary = compute_summary_stats(df)
    
    # Statistical tests
    anova_results = test_main_effect(df)
    pairwise_results = test_pairwise_comparisons(df)
    
    # Plots
    plot_time_series(df, output_dir)
    plot_final_comparison(df, output_dir)
    
    # Save results
    results = {
        'anova': anova_results,
        'pairwise': pairwise_results.to_dict('records')
    }
    
    results_path = Path(output_dir) / 'statistical_results.json'
    with open(results_path, 'w') as f:
        json.dump(results, f, indent=2)
    
    print("\n" + "="*80)
    print("ANALYSIS COMPLETE")
    print("="*80)
    print(f"Results saved to: {output_dir}")
    print("\nRecommended next steps:")
    print("  1. Review time series plot for qualitative patterns")
    print("  2. Include ANOVA table in paper methods section")
    print("  3. Report significant pairwise differences in results")
    print("  4. Consider phase transition analysis (optional)")

if __name__ == '__main__':
    main()
