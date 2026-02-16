from setuptools import setup, find_packages

setup(
    name="behaviour-simulation",
    version="2.0.0",
    description="Emotion contagion simulation with calibration and sensitivity analysis",
    packages=find_packages(),
    install_requires=[
        "pandas>=2.0.0",
        "numpy>=1.24.0",
        "matplotlib>=3.7.0",
        "seaborn>=0.12.0",
        "scipy>=1.10.0",
        "SALib>=1.4.7",
        "transformers>=4.30.0",
        "torch>=2.0.0",
        "openai>=1.0.0",
        "pyabc>=0.11.0",
        "psutil>=5.9.0",
        "tqdm>=4.65.0",
    ],
    python_requires=">=3.9",
)
