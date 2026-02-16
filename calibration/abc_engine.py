import numpy as np
from typing import Dict, List, Callable, Any
import logging
from scipy.stats import uniform
import pandas as pd
import json

# Use pyabc if available, else fallback to custom
try:
    import pyabc
    HAS_PYABC = True
except ImportError:
    HAS_PYABC = False

class ABCEngine:
    """
    Engine for Approximate Bayesian Computation.
    Estimates simulation parameters that best match observed summary statistics.
    """
    
    def __init__(self, observed_stats: dict, simulation_runner: Callable[[dict], dict]):
        self.observed_stats = observed_stats
        self.simulation_runner = simulation_runner
        self.logger = logging.getLogger(__name__)
        
        # Define priors (can be customized)
        self.priors = {
            'emotionRegressionRate': (0.01, 0.20),
            'contentInfluenceWeight': (0.05, 0.50),
            'socialInfluenceWeight': (0.05, 0.50)
        }

    def distance(self, sim_stats: dict, obs_stats: dict) -> float:
        """
        Compute weighted Euclidean distance between simulated and observed statistics.
        """
        dist = 0.0
        
        # Mood Distribution (KL Divergence or Euclidean)
        if 'mood_distribution' in sim_stats and 'mood_distribution' in obs_stats:
            p = np.array(sim_stats['mood_distribution'])
            q = np.array(obs_stats['mood_distribution'])
            # Simple Euclidean for now
            dist += 2.0 * np.linalg.norm(p - q)
            
        # Polarization
        if 'polarization_index' in sim_stats and 'polarization_index' in obs_stats:
            p_sim = sim_stats['polarization_index']
            p_obs = obs_stats['polarization_index']
            dist += 1.0 * abs(p_sim - p_obs)
            
        return dist

    def run_calibration(self, n_samples: int = 100, tolerance: float = 0.1) -> pd.DataFrame:
        """
        Run simple Rejection ABC (if pyabc not used/configured).
        """
        self.logger.info(f"Running Rejection ABC with N={n_samples}, eps={tolerance}")
        
        accepted_params = []
        distances = []
        
        for i in range(n_samples * 10): # Try up to 10x samples
            if len(accepted_params) >= n_samples:
                break
                
            # Sample from priors
            params = {
                k: np.random.uniform(v[0], v[1]) 
                for k, v in self.priors.items()
            }
            
            # Run simulation
            try:
                metrics = self.simulation_runner(params)
                
                # Check distance
                d = self.distance(metrics, self.observed_stats)
                
                if d < tolerance:
                    accepted_params.append(params)
                    distances.append(d)
                    
            except Exception as e:
                self.logger.warning(f"Simulation failed: {e}")
                
        df = pd.DataFrame(accepted_params)
        df['distance'] = distances
        return df

    def run_smc(self):
        """
        Run Sequential Monte Carlo ABC using discrete generations (Advanced).
        Requires pyabc.
        """
        if not HAS_PYABC:
            raise ImportError("pyabc not installed. Use run_calibration() instead.")
        
        self.logger.info("Running SMC-ABC...")
        # (Implementation placeholder for advanced usage)
        pass
