import logging
import pandas as pd
import sys
import os

# Add project root to path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from calibration.data_loader import SyntheticDataLoader
from calibration.abc_engine import ABCEngine

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def mock_simulation_runner(params: dict) -> dict:
    """
    Mock runner for development/testing loop.
    Simulates finding a "true" parameter set.
    """
    # True parameters we're trying to find
    true_params = {
        'emotionRegressionRate': 0.05,
        'contentInfluenceWeight': 0.15,
        'socialInfluenceWeight': 0.10
    }
    
    # Calculate distance from true params
    simulated_stats = {}
    
    # Mood distribution depends heavily on emotionRegressionRate
    # If reg rate is high, folks are more neutral. If low, more polarized.
    reg_rate = params.get('emotionRegressionRate', 0.05)
    
    # Simple heuristic
    if reg_rate > 0.1:
        simulated_stats['mood_distribution'] = [0.1, 0.8, 0.1]
    elif reg_rate < 0.02:
        simulated_stats['mood_distribution'] = [0.4, 0.2, 0.4]
    else:
        # Near optimal
        simulated_stats['mood_distribution'] = [0.22, 0.48, 0.30] 
        
    # Polarization depends on influence weights
    content_w = params.get('contentInfluenceWeight', 0.15)
    social_w = params.get('socialInfluenceWeight', 0.10)
    
    simulated_stats['polarization_index'] = 0.45 + (content_w - 0.15) + (social_w - 0.10)
    
    return simulated_stats

if __name__ == "__main__":
    logger.info("========================================")
    logger.info("Starting Calibration Pipeline (Mock Mode)")
    logger.info("========================================")
    
    # 1. Load Data
    logger.info("Loading empirical data (Synthetic)...")
    loader = SyntheticDataLoader("path/to/data.csv")
    observed_stats = loader.compute_summary_statistics()
    logger.info(f"Target Stats: {observed_stats}")
    
    # 2. Init Engine
    logger.info("Initializing ABC Engine...")
    engine = ABCEngine(observed_stats, mock_simulation_runner)
    
    # 3. Run Calibration
    logger.info("Running calibration loop...")
    accepted_params_df = engine.run_calibration(n_samples=50, tolerance=0.15)
    
    # 4. Results
    logger.info(f"\nAccepted Parameters ({len(accepted_params_df)} samples):")
    if not accepted_params_df.empty:
        print(accepted_params_df.describe())
        accepted_params_df.to_csv("calibration_results.csv", index=False)
        logger.info("Results saved to calibration_results.csv")
    else:
        logger.warning("No parameters accepted. Try increasing tolerance.")
    
    logger.info("Calibration pipeline Complete.")
