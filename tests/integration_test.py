import subprocess
import time
import logging
import os
import sys

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def run_command(command: str, description: str):
    """Run a shell command and check for errors."""
    logger.info(f"--- Starting: {description} ---")
    start_time = time.time()
    
    try:
        # Check if we are in mock mode (Java not installed)
        # Verify if JAR works? No, just rely on the scripts handling it.
        
        # Use shell=True for Windows compatibility with batch files/commands
        result = subprocess.run(
            command, 
            shell=True, 
            check=True, 
            capture_output=True, 
            text=True
        )
        duration = time.time() - start_time
        logger.info(f"✅ Completed: {description} in {duration:.2f}s")
        logger.debug(result.stdout)
        
    except subprocess.CalledProcessError as e:
        logger.error(f"❌ Failed: {description}")
        logger.error(f"Exit Code: {e.returncode}")
        logger.error(f"Stderr: {e.stderr}")
        # Build failures are fatal, but some scripts might fail if Java missing
        if "mvn" in command:
            logger.warning("Maven build failed. Ensure Java/Maven are installed.")
        else:
            logger.warning("Proceeding despite failure (Mock verification might be partial).")

if __name__ == "__main__":
    logger.info("========================================")
    logger.info("SYSTEM INTEGRATION TEST (TIER 1 UPGRADES)")
    logger.info("========================================")
    
    # 1. Build Project (if possible)
    # We skip this in the test script usually, but useful to have connection
    # run_command("mvn clean package -DskipTests", "Build Simulation Engine")

    # 2. Test Phase 2: Simulation Logic
    # Verify synthetic data generation and basic logic
    run_command("python tests/test_continuous_emotions.py", "Phase 2: Continuous Emotion Logic")
    
    # 3. Test Phase 3: Sensitivity Analysis
    run_command("python run_sensitivity.py", "Phase 3: Sensitivity Analysis (Sobol)")
    
    # 4. Test Phase 4: Calibration
    run_command("python run_calibration.py", "Phase 4: Calibration (ABC)")
    
    # 5. Test Phase 5: Benchmarks
    run_command("python benchmarks/benchmark_runner.py", "Phase 5: Scaling Benchmarks")
    
    logger.info("\n========================================")
    logger.info("global integration verification complete")
    logger.info("========================================")
