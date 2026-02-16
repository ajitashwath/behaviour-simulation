import numpy as np
from SALib.sample import saltelli
from SALib.analyze import sobol
import pandas as pd
from typing import Dict, List, Any
import matplotlib.pyplot as plt
import subprocess
import json
import os
import logging
from tqdm import tqdm

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class SobolSensitivityAnalyzer:
    """
    Global sensitivity analysis using Sobol indices for the Behavior Simulation Platform.
    Focuses on Continuous Emotion Model parameters.
    """
    
    def __init__(self, simulation_jar_path: str = "target/simulation-engine-1.0-SNAPSHOT.jar"):
        self.simulation_jar_path = simulation_jar_path
        
        # Define problem for Continuous Emotion Model
        # We analyze sensitivity of outcomes to these key parameters
        self.problem = {
            'num_vars': 6,
            'names': [
                'emotionRegressionRate',    # How fast emotions return to baseline
                'arousalDecayRate',         # Additional decay for arousal
                'contentInfluenceWeight',   # Impact of content on emotions
                'socialInfluenceWeight',    # Impact of social circle
                'contentHalfLife',          # Content decay speed
                'initialExposureRate'       # Content discovery rate
            ],
            'bounds': [
                [0.01, 0.20],  # emotionRegressionRate
                [0.01, 0.20],  # arousalDecayRate
                [0.05, 0.50],  # contentInfluenceWeight
                [0.05, 0.50],  # socialInfluenceWeight
                [5.0, 50.0],   # contentHalfLife
                [0.05, 0.30]   # initialExposureRate
            ]
        }
    
    def generate_samples(self, n: int = 256) -> np.ndarray:
        """
        Generate Saltelli sample matrix.
        
        Args:
            n: Baseline sample size (power of 2 recommended).
               Total simulations = N * (2D + 2).
               For D=6, N=256 -> 3,584 simulations.
        """
        D = self.problem['num_vars']
        total_sims = n * (2 * D + 2)
        logger.info(f"Generating Saltelli samples: N={n}, D={D}")
        logger.info(f"Total simulations required: {total_sims}")
        
        # calc_second_order=True enables interaction analysis (S2)
        return saltelli.sample(self.problem, n, calc_second_order=True)
    
    def run_simulation(self, params_dict: Dict[str, float]) -> Dict[str, Any]:
        """
        Run single simulation with given parameters.
        Wrapper around Spark job submission.
        """
        # In a real scenario, this constructs a spark-submit command
        # For development/mocking purposes, we check if JAR exists
        if not os.path.exists(self.simulation_jar_path):
            logger.warning(f"Simulation JAR not found at {self.simulation_jar_path}. Running in MOCK mode.")
            return self._mock_simulation(params_dict)
            
        # TODO: Implement actual Spark submission
        # cmd = ['spark-submit', '--class', '...', self.simulation_jar_path, json.dumps(params_dict)]
        # result = subprocess.run(cmd, capture_output=True, text=True)
        # return json.loads(result.stdout)
        
        return self._mock_simulation(params_dict)

    def _mock_simulation(self, params: Dict[str, float]) -> Dict[str, Any]:
        """
        Mock simulation outputs based on parameters for testing the pipeline.
        Simulates plausible relationships (e.g., higher social weights -> higher polarization).
        """
        # Synthetic relationship: Polarization increases with social weight and content influence
        noise = np.random.normal(0, 0.05)
        polarization = (
            0.4 * params.get('socialInfluenceWeight', 0.1) + 
            0.3 * params.get('contentInfluenceWeight', 0.1) - 
            0.2 * params.get('emotionRegressionRate', 0.05) + 
            0.5 + noise
        )
        polarization = np.clip(polarization, 0, 1)
        
        return {
            "finalMetrics": {
                "polarizationIndex": polarization,
                "averageArousal": 0.5 + 0.5 * params.get('arousalDecayRate', 0.0),
                "moodStability": 1.0 - params.get('contentHalfLife', 10)/100.0
            }
        }

    def run_sensitivity_analysis(self, param_samples: np.ndarray, metric_name: str = 'polarizationIndex') -> Dict:
        """
        Execute simulations (or use cache) and compute Sobol indices.
        """
        logger.info(f"Running {len(param_samples)} simulations for analysis...")
        
        Y = np.zeros(len(param_samples))
        
        # Serial execution for now (can map to multiprocessing pool)
        for i, params_array in enumerate(tqdm(param_samples)):
            # Map array to dict
            params_dict = {
                name: float(val) 
                for name, val in zip(self.problem['names'], params_array)
            }
            
            # Run
            result = self.run_simulation(params_dict)
            Y[i] = result['finalMetrics'].get(metric_name, 0.0)
            
        logger.info("Computing Sobol indices...")
        Si = sobol.analyze(self.problem, Y, calc_second_order=True)
        
        return Si

    def plot_indices(self, Si: Dict, metric_name: str, output_path: str):
        """
        Visualize First-order (S1) and Total-order (ST) indices.
        """
        names = self.problem['names']
        
        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6))
        
        # First Order
        y_pos = np.arange(len(names))
        ax1.barh(y_pos, Si['S1'], xerr=Si['S1_conf'], align='center', color='skyblue')
        ax1.set_yticks(y_pos)
        ax1.set_yticklabels(names)
        ax1.set_xlabel('S1 (First-order Sensitivity)')
        ax1.set_title(f'{metric_name} - Direct Effects')
        ax1.invert_yaxis()
        
        # Total Order
        ax2.barh(y_pos, Si['ST'], xerr=Si['ST_conf'], align='center', color='orange')
        ax2.set_yticks(y_pos)
        ax2.set_yticklabels(names)
        ax2.set_xlabel('ST (Total-order Sensitivity)')
        ax2.set_title(f'{metric_name} - Total Effects (incl. interactions)')
        ax2.invert_yaxis()
        
        plt.tight_layout()
        plt.savefig(output_path)
        logger.info(f"Sensitivity plot saved to {output_path}")

if __name__ == "__main__":
    # Example usage
    analyzer = SobolSensitivityAnalyzer()
    
    # Generate small sample for testing
    samples = analyzer.generate_samples(n=32) 
    
    # Run analysis
    Si = analyzer.run_sensitivity_analysis(samples, metric_name='polarizationIndex')
    
    # Print results
    print("\nTotal Order Indices (ST):")
    for name, st in zip(analyzer.problem['names'], Si['ST']):
        print(f"{name}: {st:.4f}")
        
    # Plot
    analyzer.plot_indices(Si, 'polarizationIndex', 'sensitivity_plot.png')
