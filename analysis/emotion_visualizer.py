"""
Visualization tools for continuous emotion model.

Provides functions to analyze and plot affect space dynamics:
- Affect distribution scatter plots
- Emotional trajectory animations
- Density heatmaps of affect space
- Time-series plots of valence/arousal
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from matplotlib.animation import FuncAnimation
from matplotlib.patches import Circle
import warnings
warnings.filterwarnings('ignore')

# Set style
sns.set_style("whitegrid")
plt.rcParams['figure.figsize'] = (10, 8)


def plot_affect_distribution(snapshot_path, output_path=None, title="Affect Distribution"):
    """
    Plot 2D scatter of population affectdistribution.
    
    Args:
        snapshot_path: Path to Parquet file with human state (must have valence, arousal columns)
        output_path: Optional path to save figure
        title: Plot title
    
    Returns:
        matplotlib Figure
    """
    df = pd.read_parquet(snapshot_path)
    
    if 'valence' not in df.columns or 'arousal' not in df.columns:
        raise ValueError("DataFrame must contain 'valence' and 'arousal' columns")
    
    fig, ax = plt.subplots(figsize=(10, 10))
    
    # Create scatter plot with density coloring
    scatter = ax.scatter(
        df['valence'], 
        df['arousal'],
        alpha=0.5,
        c=df['mood'].map({'JOY': 'gold', 'NEUTRAL': 'gray', 'RAGE': 'crimson'}),
        s=20,
        edgecolors='none'
    )
    
    # Add quadrant labels
    ax.text(0.7, 0.7, "Joy\n(pleasant, activated)", ha='center', fontsize=12, alpha=0.6)
    ax.text(-0.7, 0.7, "Anger\n(unpleasant, activated)", ha='center', fontsize=12, alpha=0.6)
    ax.text(-0.7, -0.7, "Sadness\n(unpleasant, deactivated)", ha='center', fontsize=12, alpha=0.6)
    ax.text(0.7, -0.7, "Calm\n(pleasant, deactivated)", ha='center', fontsize=12, alpha=0.6)
    
    # Draw axes
    ax.axhline(0, color='black', linewidth=0.5, linestyle='--', alpha=0.3)
    ax.axvline(0, color='black', linewidth=0.5, linestyle='--', alpha=0.3)
    
    # Draw affect circle (typical range)
    circle = Circle((0, 0), 1.0, fill=False, edgecolor='black', linewidth=1, linestyle=':', alpha=0.3)
    ax.add_patch(circle)
    
    # Labels and styling
    ax.set_xlabel("Valence (displeasure → pleasure)", fontsize=14)
    ax.set_ylabel("Arousal (deactivation → activation)", fontsize=14)
    ax.set_title(title, fontsize=16, fontweight='bold')
    ax.set_xlim(-1.1, 1.1)
    ax.set_ylim(-1.1, 1.1)
    ax.set_aspect('equal')
    ax.grid(True, alpha=0.3)
    
    # Add statistics
    stats_text = f"N = {len(df)}\n"
    stats_text += f"Mean Valence: {df['valence'].mean():.2f}\n"
    stats_text += f"Mean Arousal: {df['arousal'].mean():.2f}\n"
    stats_text += f"Mood: {df['mood'].value_counts().to_dict()}"
    ax.text(0.02, 0.98, stats_text, transform=ax.transAxes, 
            fontsize=10, verticalalignment='top',
            bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))
    
    plt.tight_layout()
    
    if output_path:
        plt.savefig(output_path, dpi=300, bbox_inches='tight')
        print(f"Saved affect distribution plot to {output_path}")
    
    return fig


def plot_affect_heatmap(snapshot_path, output_path=None, bins=20):
    """
    Plot density heatmap of affect space.
    
    Args:
        snapshot_path: Path to Parquet file with human state
        output_path: Optional path to save figure
        bins: Number of bins for 2D histogram
    
    Returns:
        matplotlib Figure
    """
    df = pd.read_parquet(snapshot_path)
    
    fig, ax = plt.subplots(figsize=(10, 10))
    
    # Create 2D histogram
    h = ax.hist2d(
        df['valence'], 
        df['arousal'],
        bins=bins,
        range=[[-1, 1], [-1, 1]],
        cmap='YlOrRd',
        cmin=1  # Don't show zero bins
    )
    
    # Colorbar
    plt.colorbar(h[3], ax=ax, label='Population Density')
    
    # Add quadrant labels
    ax.text(0.5, 0.5, "Joy", ha='center', fontsize=14, fontweight='bold', color='white')
    ax.text(-0.5, 0.5, "Anger", ha='center', fontsize=14, fontweight='bold', color='white')
    ax.text(-0.5, -0.5, "Sadness", ha='center', fontsize=14, fontweight='bold', color='white')
    ax.text(0.5, -0.5, "Calm", ha='center', fontsize=14, fontweight='bold', color='white')
    
    # Draw axes
    ax.axhline(0, color='black', linewidth=1, linestyle='--', alpha=0.5)
    ax.axvline(0, color='black', linewidth=1, linestyle='--', alpha=0.5)
    
    # Labels
    ax.set_xlabel("Valence", fontsize=14)
    ax.set_ylabel("Arousal", fontsize=14)
    ax.set_title("Affect Space Density Heatmap", fontsize=16, fontweight='bold')
    ax.set_xlim(-1, 1)
    ax.set_ylim(-1, 1)
    ax.set_aspect('equal')
    
    plt.tight_layout()
    
    if output_path:
        plt.savefig(output_path, dpi=300, bbox_inches='tight')
        print(f"Saved affect heatmap to {output_path}")
    
    return fig


def plot_affect_trajectory(states_dir, human_ids=None, n_random=10, output_path=None):
    """
    Plot emotional trajectories for selected humans over time.
    
    Args:
        states_dir: Directory containing state Parquet files (step_*.parquet)
        human_ids: List of specific human IDs to plot (None = random sample)
        n_random: Number of random humans to plot if human_ids is None
        output_path: Optional path to save figure
    
    Returns:
        matplotlib Figure
    """
    import glob
    import os
    
    # Load all steps
    state_files = sorted(glob.glob(os.path.join(states_dir, "step_*.parquet")))
    if not state_files:
        raise ValueError(f"No state files found in {states_dir}")
    
    # Collect trajectories
    trajectories = {}
    for file in state_files:
        step_df = pd.read_parquet(file)
        step_num = int(os.path.basename(file).split('_')[1].split('.')[0])
        
        if human_ids is None and len(trajectories) == 0:
            # First step: sample random humans
            human_ids = step_df['humanId'].sample(n=min(n_random, len(step_df))).tolist()
        
        for hid in human_ids:
            human_row = step_df[step_df['humanId'] == hid]
            if not human_row.empty:
                if hid not in trajectories:
                    trajectories[hid] = {'steps': [], 'valence': [], 'arousal': []}
                trajectories[hid]['steps'].append(step_num)
                trajectories[hid]['valence'].append(human_row['valence'].values[0])
                trajectories[hid]['arousal'].append(human_row['arousal'].values[0])
    
    # Plot trajectories
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(16, 7))
    
    # Left: Affect space with trajectories
    colors = plt.cm.tab10(np.linspace(0, 1, len(trajectories)))
    
    for i, (hid, traj) in enumerate(trajectories.items()):
        ax1.plot(traj['valence'], traj['arousal'], 
                alpha=0.6, linewidth=2, color=colors[i], 
                marker='o', markersize=3, label=f"Human {hid}")
        # Mark start and end
        ax1.scatter(traj['valence'][0], traj['arousal'][0], 
                   s=100, marker='s', color=colors[i], edgecolors='black', linewidths=2, zorder=10)
        ax1.scatter(traj['valence'][-1], traj['arousal'][-1], 
                   s=100, marker='*', color=colors[i], edgecolors='black', linewidths=2, zorder=10)
    
    ax1.axhline(0, color='black', linewidth=0.5, linestyle='--', alpha=0.3)
    ax1.axvline(0, color='black', linewidth=0.5, linestyle='--', alpha=0.3)
    ax1.set_xlabel("Valence", fontsize=12)
    ax1.set_ylabel("Arousal", fontsize=12)
    ax1.set_title("Emotional Trajectories in Affect Space", fontsize=14, fontweight='bold')
    ax1.set_xlim(-1.1, 1.1)
    ax1.set_ylim(-1.1, 1.1)
    ax1.set_aspect('equal')
    ax1.legend(fontsize=8, loc='upper left', bbox_to_anchor=(1, 1))
    ax1.grid(True, alpha=0.3)
    
    # Right: Time series of valence and arousal
    for i, (hid, traj) in enumerate(trajectories.items()):
        ax2.plot(traj['steps'], traj['valence'], 
                alpha=0.7, linewidth=2, color=colors[i], linestyle='-', label=f"H{hid} valence")
        ax2.plot(traj['steps'], traj['arousal'], 
                alpha=0.7, linewidth=2, color=colors[i], linestyle='--', label=f"H{hid} arousal")
    
    ax2.axhline(0, color='black', linewidth=0.5, linestyle='--', alpha=0.3)
    ax2.set_xlabel("Simulation Step", fontsize=12)
    ax2.set_ylabel("Affect Value", fontsize=12)
    ax2.set_title("Emotional Dynamics Over Time", fontsize=14, fontweight='bold')
    ax2.set_ylim(-1.1, 1.1)
    ax2.grid(True, alpha=0.3)
    
    plt.tight_layout()
    
    if output_path:
        plt.savefig(output_path, dpi=300, bbox_inches='tight')
        print(f"Saved trajectory plot to {output_path}")
    
    return fig


def plot_emotion_summary(states_dir, output_path=None):
    """
    Plot summary statistics of affect over time.
    
    Args:
        states_dir: Directory containing state Parquet files
        output_path: Optional path to save figure
    
    Returns:
        matplotlib Figure
    """
    import glob
    import os
    
    # Load all steps
    state_files = sorted(glob.glob(os.path.join(states_dir, "step_*.parquet")))
    
    stats = {
        'step': [],
        'mean_valence': [],
        'std_valence': [],
        'mean_arousal': [],
        'std_arousal': [],
        'joy_fraction': [],
        'neutral_fraction': [],
        'rage_fraction': []
    }
    
    for file in state_files:
        step_df = pd.read_parquet(file)
        step_num = int(os.path.basename(file).split('_')[1].split('.')[0])
        
        stats['step'].append(step_num)
        stats['mean_valence'].append(step_df['valence'].mean())
        stats['std_valence'].append(step_df['valence'].std())
        stats['mean_arousal'].append(step_df['arousal'].mean())
        stats['std_arousal'].append(step_df['arousal'].std())
        
        mood_counts = step_df['mood'].value_counts()
        total = len(step_df)
        stats['joy_fraction'].append(mood_counts.get('JOY', 0) / total)
        stats['neutral_fraction'].append(mood_counts.get('NEUTRAL', 0) / total)
        stats['rage_fraction'].append(mood_counts.get('RAGE', 0) / total)
    
    stats_df = pd.DataFrame(stats)
    
    #Plot
    fig, axes = plt.subplots(2, 2, figsize=(14, 10))
    
    # Mean valence over time
    ax = axes[0, 0]
    ax.plot(stats_df['step'], stats_df['mean_valence'], linewidth=2, color='steelblue')
    ax.fill_between(stats_df['step'], 
                     stats_df['mean_valence'] - stats_df['std_valence'],
                     stats_df['mean_valence'] + stats_df['std_valence'],
                     alpha=0.3, color='steelblue')
    ax.axhline(0, color='black', linewidth=0.5, linestyle='--', alpha=0.3)
    ax.set_xlabel("Step")
    ax.set_ylabel("Mean Valence")
    ax.set_title("Valence Dynamics")
    ax.grid(True, alpha=0.3)
    
    # Mean arousal over time
    ax = axes[0, 1]
    ax.plot(stats_df['step'], stats_df['mean_arousal'], linewidth=2, color='coral')
    ax.fill_between(stats_df['step'],
                     stats_df['mean_arousal'] - stats_df['std_arousal'],
                     stats_df['mean_arousal'] + stats_df['std_arousal'],
                     alpha=0.3, color='coral')
    ax.axhline(0, color='black', linewidth=0.5, linestyle='--', alpha=0.3)
    ax.set_xlabel("Step")
    ax.set_ylabel("Mean Arousal")
    ax.set_title("Arousal Dynamics")
    ax.grid(True, alpha=0.3)
    
    # Mood fractions over time
    ax = axes[1, 0]
    ax.plot(stats_df['step'], stats_df['joy_fraction'], linewidth=2, label='Joy', color='gold')
    ax.plot(stats_df['step'], stats_df['neutral_fraction'], linewidth=2, label='Neutral', color='gray')
    ax.plot(stats_df['step'], stats_df['rage_fraction'], linewidth=2, label='Rage', color='crimson')
    ax.set_xlabel("Step")
    ax.set_ylabel("Fraction")
    ax.set_title("Mood Distribution Over Time")
    ax.legend()
    ax.grid(True, alpha=0.3)
    
    # Affect space density evolution
    ax = axes[1, 1]
    ax.scatter(stats_df['mean_valence'], stats_df['mean_arousal'], 
              c=stats_df['step'], cmap='viridis', s=50, alpha=0.8)
    ax.plot(stats_df['mean_valence'], stats_df['mean_arousal'], 
           linewidth=1, alpha=0.5, color='black')
    ax.axhline(0, color='black', linewidth=0.5, linestyle='--', alpha=0.3)
    ax.axvline(0, color='black', linewidth=0.5, linestyle='--', alpha=0.3)
    ax.set_xlabel("Mean Valence")
    ax.set_ylabel("Mean Arousal")
    ax.set_title("Population Affect Trajectory")
    ax.grid(True, alpha=0.3)
    plt.colorbar(ax.collections[0], ax=ax, label='Step')
    
    plt.suptitle("Continuous Emotion Model: Summary Statistics", fontsize=16, fontweight='bold', y=1.00)
    plt.tight_layout()
    
    if output_path:
        plt.savefig(output_path, dpi=300, bbox_inches='tight')
        print(f"Saved emotion summary to {output_path}")
    
    return fig


if __name__ == "__main__":
    import sys
    
    if len(sys.argv) < 2:
        print("Usage: python emotion_visualizer.py <snapshot_path_or_states_dir>")
        sys.exit(1)
    
    path = sys.argv[1]
    
    if path.endswith('.parquet'):
        # Single snapshot
        plot_affect_distribution(path, output_path="affect_distribution.png")
        plot_affect_heatmap(path, output_path="affect_heatmap.png")
    else:
        # Directory of states
        plot_affect_trajectory(path, output_path="affect_trajectories.png")
        plot_emotion_summary(path, output_path="emotion_summary.png")
    
    plt.show()
