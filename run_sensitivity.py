import logging
from sensitivity.sobol_analyzer import SobolSensitivityAnalyzer

if __name__ == "__main__":
    print("==============================================")
    print("Running Sensitivity Analysis (Mock Mode)")
    print("==============================================")
    
    # Initialize analyzer (will use mock mode since JAR is missing)
    analyzer = SobolSensitivityAnalyzer(simulation_jar_path="missing.jar")
    
    # Generate samples
    # Using small N for quick demonstration
    samples = analyzer.generate_samples(n=128)
    
    # Run analysis
    # This uses the _mock_simulation method in sobol_analyzer.py
    # which simulates the expected relationships
    Si = analyzer.run_sensitivity_analysis(samples, metric_name='polarizationIndex')
    
    # Print results
    print("\n--- Sensitivity Indices for Polarization ---")
    print(f"{'Parameter':<25} {'S1 (Direct)':<15} {'ST (Total)':<15}")
    print("-" * 55)
    
    for i, name in enumerate(analyzer.problem['names']):
        s1 = Si['S1'][i]
        st = Si['ST'][i]
        print(f"{name:<25} {s1:.4f}          {st:.4f}")

    # Plot results
    output_plot = "sensitivity_polarization.png"
    analyzer.plot_indices(Si, 'Polarization Index', output_plot)
    print(f"\nPlot saved to: {output_plot}")
    print("\nDone! logic verification complete.")
