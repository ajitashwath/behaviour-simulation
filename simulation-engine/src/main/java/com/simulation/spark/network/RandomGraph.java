package com.simulation.spark.network;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.util.*;

/**
 * Erdős-Rényi Random Graph (Baseline Control)
 * 
 * Each possible edge exists independently with probability p.
 * Serves as null model for comparison with structured topologies.
 * 
 * Research Relevance:
 * - Control condition for network experiments
 * - No structure, pure randomness
 * - Poisson degree distribution
 * - Low clustering, short paths
 * 
 * Algorithm:
 * 1. For each pair of nodes (i, j) where i < j
 * 2. Add edge with probability p
 * 3. Results in expected degree = p * (n-1)
 * 
 * Reference: Erdős & Rényi (1959), "On random graphs"
 */
public class RandomGraph implements NetworkTopology {

    private static final long serialVersionUID = 1L;

    private final double edgeProbability; // Probability each edge exists
    private final int targetAvgDegree; // Target average degree (used to compute p)

    /**
     * Constructor using target average degree.
     * Edge probability is computed as p = k / (n-1) where k = target degree
     * 
     * @param targetAvgDegree Target average degree (typical: 4-10)
     */
    public RandomGraph(int targetAvgDegree) {
        if (targetAvgDegree < 1) {
            throw new IllegalArgumentException("targetAvgDegree must be >= 1");
        }
        this.targetAvgDegree = targetAvgDegree;
        this.edgeProbability = -1; // Will be computed based on population size
    }

    @Override
    public Dataset<Row> generateEdges(SparkSession spark, int populationSize, long seed) {
        // Compute edge probability if not set
        double p = (edgeProbability > 0)
                ? edgeProbability
                : (double) targetAvgDegree / (populationSize - 1);

        Random random = new Random(seed);
        List<Edge> edges = new ArrayList<>();

        // Generate edges with probability p
        for (int i = 0; i < populationSize; i++) {
            for (int j = i + 1; j < populationSize; j++) {
                if (random.nextDouble() < p) {
                    edges.add(new Edge((long) i, (long) j, 1.0));
                    edges.add(new Edge((long) j, (long) i, 1.0)); // Undirected
                }
            }
        }

        // Convert to DataFrame
        StructType schema = new StructType()
                .add("source", DataTypes.LongType, false)
                .add("target", DataTypes.LongType, false)
                .add("weight", DataTypes.DoubleType, false);

        List<Row> rows = new ArrayList<>(edges.size());
        for (Edge e : edges) {
            rows.add(org.apache.spark.sql.RowFactory.create(e.source, e.target, e.weight));
        }

        return spark.createDataFrame(rows, schema);
    }

    @Override
    public Map<String, Double> computeMetrics(Dataset<Row> edges) {
        Map<String, Double> metrics = new HashMap<>();

        // Basic stats
        long numEdges = edges.count();
        long numNodes = edges.select("source").union(edges.select("target"))
                .distinct()
                .count();

        double avgDegree = (double) numEdges / numNodes;
        metrics.put("num_nodes", (double) numNodes);
        metrics.put("num_edges", (double) numEdges);
        metrics.put("average_degree", avgDegree);

        // Degree distribution statistics
        Dataset<Row> degreeStats = edges
                .groupBy("source")
                .agg(org.apache.spark.sql.functions.count("target").as("degree"))
                .agg(
                        org.apache.spark.sql.functions.max("degree").as("max_degree"),
                        org.apache.spark.sql.functions.min("degree").as("min_degree"),
                        org.apache.spark.sql.functions.stddev("degree").as("degree_stddev"));

        Row stats = degreeStats.first();
        metrics.put("max_degree", (double) stats.getLong(0));
        metrics.put("min_degree", (double) stats.getLong(1));
        if (stats.get(2) != null) {
            metrics.put("degree_stddev", stats.getDouble(2));
        }

        return metrics;
    }

    @Override
    public double getExpectedAverageDegree(int populationSize) {
        if (targetAvgDegree > 0) {
            return targetAvgDegree;
        } else {
            return edgeProbability * (populationSize - 1);
        }
    }

    @Override
    public String getTopologyName() {
        if (targetAvgDegree > 0) {
            return "Random_k" + targetAvgDegree;
        } else {
            return String.format("Random_p%.3f", edgeProbability);
        }
    }

    private static class Edge {
        final long source;
        final long target;
        final double weight;

        Edge(long source, long target, double weight) {
            this.source = source;
            this.target = target;
            this.weight = weight;
        }
    }
}
