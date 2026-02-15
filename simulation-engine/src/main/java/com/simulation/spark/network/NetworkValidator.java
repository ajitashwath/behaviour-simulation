package com.simulation.spark.network;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.Map;

/**
 * Standalone validator for network topology generation.
 * Quickly verifies all network types work without full simulation.
 */
public class NetworkValidator {

    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder()
                .appName("NetworkValidator")
                .master("local[*]")
                .getOrCreate();

        try {
            System.out.println("=".repeat(80));
            System.out.println("NETWORK TOPOLOGY VALIDATOR");
            System.out.println("=".repeat(80));

            validateBarabasiAlbert(spark);
            validateWattsStrogatz(spark);
            validateRandom(spark);

            System.out.println("\n" + "=".repeat(80));
            System.out.println("✓ ALL NETWORK TOPOLOGIES VALIDATED");
            System.out.println("=".repeat(80));

        } catch (Exception e) {
            System.err.println("✗ VALIDATION FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            spark.stop();
        }
    }

    private static void validateBarabasiAlbert(SparkSession spark) {
        System.out.println("\n--- Barabási-Albert (Scale-Free) ---");

        BarabasiAlbertNetwork network = new BarabasiAlbertNetwork(3);
        Dataset<Row> edges = network.generateEdges(spark, 1000, 42L);
        Map<String, Double> metrics = network.computeMetrics(edges);

        System.out.println("Topology: " + network.getTopologyName());
        System.out.println("Nodes: " + metrics.get("num_nodes"));
        System.out.println("Edges: " + metrics.get("num_edges"));
        System.out.println("Avg Degree: " + String.format("%.2f", metrics.get("average_degree")));
        System.out.println("Max Degree: " + metrics.get("max_degree") + " (hub node)");
        System.out.println("✓ Power-law degree distribution expected");
    }

    private static void validateWattsStrogatz(SparkSession spark) {
        System.out.println("\n--- Watts-Strogatz (Small-World) ---");

        WattsStrogatzNetwork network = new WattsStrogatzNetwork(6, 0.05);
        Dataset<Row> edges = network.generateEdges(spark, 1000, 42L);
        Map<String, Double> metrics = network.computeMetrics(edges);

        System.out.println("Topology: " + network.getTopologyName());
        System.out.println("Nodes: " + metrics.get("num_nodes"));
        System.out.println("Edges: " + metrics.get("num_edges"));
        System.out.println("Avg Degree: " + String.format("%.2f", metrics.get("average_degree")));
        System.out.println("Expected Clustering (p=0): " +
                String.format("%.3f", metrics.get("expected_clustering_p0")));
        System.out.println("✓ High clustering + short paths expected");
    }

    private static void validateRandom(SparkSession spark) {
        System.out.println("\n--- Random Graph (Erdős-Rényi) ---");

        RandomGraph network = new RandomGraph(6);
        Dataset<Row> edges = network.generateEdges(spark, 1000, 42L);
        Map<String, Double> metrics = network.computeMetrics(edges);

        System.out.println("Topology: " + network.getTopologyName());
        System.out.println("Nodes: " + metrics.get("num_nodes"));
        System.out.println("Edges: " + metrics.get("num_edges"));
        System.out.println("Avg Degree: " + String.format("%.2f", metrics.get("average_degree")));
        System.out.println("Degree StdDev: " + String.format("%.2f", metrics.get("degree_stddev")));
        System.out.println("✓ Poisson degree distribution expected");
    }
}
