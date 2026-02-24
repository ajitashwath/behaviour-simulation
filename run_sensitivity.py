import logging
from sensitivity.sobol_analyzer import SobolSensitivityAnalyzer

if __name__ == "__main__":
    print("==============================================")
    print("Running Sensitivity Analysis (Mock Mode)")
    print("==============================================")
    
    import os
    
    jar_path = "simulation-engine/target/simulation-engine-1.0-SNAPSHOT.jar"
    if os.path.exists(jar_path):
        print(f"✅ Found Simulation JAR: {jar_path}")
        print("Running in REAL mode (spark-submit)...")
    else:
        print(f"⚠️ JAR not found at {jar_path}")
        print("Running in MOCK mode (synthetic data)...")
        jar_path = "missing.jar"

    analyzer = SobolSensitivityAnalyzer(simulation_jar_path=jar_path)
    
    samples = analyzer.generate_samples(n=128)
    
    Si = analyzer.run_sensitivity_analysis(samples, metric_name='polarizationIndex')
    
    print("\n--- Sensitivity Indices for Polarization ---")
    print(f"{'Parameter':<25} {'S1 (Direct)':<15} {'ST (Total)':<15}")
    print("-" * 55)
    
    for i, name in enumerate(analyzer.problem['names']):
        s1 = Si['S1'][i]
        st = Si['ST'][i]
        print(f"{name:<25} {s1:.4f}          {st:.4f}")

    output_plot = "sensitivity_polarization.png"
    analyzer.plot_indices(Si, 'Polarization Index', output_plot)
    print(f"\nPlot saved to: {output_plot}")
    print("\nDone! logic verification complete.")
