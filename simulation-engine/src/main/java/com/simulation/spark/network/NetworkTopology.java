package com.simulation.spark.network;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.io.Serializable;
import java.util.Map;

/**
 * Interface for generating social network structures.
 * 
 * All topologies must:
 * - Be reproducible (deterministic given seed)
 * - Scale to millions of nodes
 * - Produce connected graphs (no isolated nodes)
 * 
 * This interface enables research on network-embedded emotional contagion
 * by allowing systematic comparison of different network structures
 * (scale-free, small-world, random, etc.)
 */
public interface NetworkTopology extends Serializable {

    /**
     * Generate network edges.
     * 
     * @param spark          SparkSession for distributed computation
     * @param populationSize number of nodes
     * @param seed           random seed for reproducibility
     * @return DataFrame with schema: (source: long, target: long, weight: double)
     */
    Dataset<Row> generateEdges(SparkSession spark, int populationSize, long seed);

    /**
     * Compute structural properties of the generated network.
     * 
     * @param edges edge DataFrame from generateEdges()
     * @return map of metric names to values (e.g., "average_degree",
     *         "clustering_coefficient")
     */
    Map<String, Double> computeMetrics(Dataset<Row> edges);

    /**
     * Get expected average degree for this topology.
     * Used for validation and parameter tuning.
     */
    double getExpectedAverageDegree(int populationSize);

    /**
     * Get topology type name for logging and analysis.
     * Used in experimental output filenames and labels.
     */
    String getTopologyName();
}
