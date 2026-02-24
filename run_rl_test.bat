@echo off
"C:\Users\AJIT ASHWATH R\AppData\Local\Java\jdk-17\bin\java.exe" --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED -cp simulation-engine\target\simulation-engine-1.0.0-SNAPSHOT.jar com.simulation.spark.experiment.RLComparisonExperiment ./rl-test > rl_out.log 2>&1
echo Done.
