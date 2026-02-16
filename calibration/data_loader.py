from abc import ABC, abstractmethod
import pandas as pd
import numpy as np
import logging

class DataLoader(ABC):
    """
    Abstract base class for loading empirical data for calibration.
    """
    def __init__(self, data_path: str):
        self.data_path = data_path
        self.logger = logging.getLogger(__name__)

    @abstractmethod
    def load_data(self) -> pd.DataFrame:
        """Load raw data into standard format."""
        pass

    @abstractmethod
    def compute_summary_statistics(self) -> dict:
        """
        Compute summary statistics for ABC.
        Returns:
            dict: {
                'mood_distribution': [p_neg, p_neu, p_pos],
                'transition_matrix': 3x3 array,
                'polarization_index': float
            }
        """
        pass

class SyntheticDataLoader(DataLoader):
    """
    Generates synthetic ground truth data when real data is unavailable.
    Useful for testing the calibration pipeline.
    """
    def load_data(self) -> pd.DataFrame:
        # Mock implementation
        return pd.DataFrame()

    def compute_summary_statistics(self) -> dict:
        self.logger.info("Generating synthetic target statistics...")
        return {
            'mood_distribution': np.array([0.2, 0.5, 0.3]),  # 20% Neg, 50% Neu, 30% Pos
            'transition_matrix': np.array([
                [0.7, 0.2, 0.1],
                [0.1, 0.8, 0.1],
                [0.1, 0.3, 0.6]
            ]),
            'polarization_index': 0.45
        }
