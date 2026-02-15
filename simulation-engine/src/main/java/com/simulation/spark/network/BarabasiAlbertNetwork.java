package com.simulation.spark.network;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.util.*;

import static org.apache.spark.sql.functions.*;

/**
 * Barabási-Albert Scale-Free Network Generator
 * 
 * Implements preferential attachment algorithm resulting in power-law degree
 * distribution.
 * Hub nodes emerge naturally, making this realistic for modeling social media
 * platforms.
 * 
 * Research Relevance:
 * - Models Facebook, Twitter, Reddit social graphs
 * - P(k) ~ k^(-γ) where γ ≈ 3
 * - Tests "superspreader" hypothesis in emotional contagion
 * 
 * Algorithm:
 * 1. Start with m0 = m+1 connected nodes
 * 2. Add nodes one at a time
 * 3. New node connects to m existing nodes with probability ∝ degree
 * 4. Results in scale-free topology
 * 
 * Reference: Barabási & Albert (1999), "Emergence of scaling in random
 * networks"
 */
public class BarabasiAlbertNetwork implements NetworkTopology {

    private static final long serialVersionUID = 1L;

    private final int m; // Number of edges to add per new node

    /**
     * @param m Number of edges each new node creates (typical: 2-5)
     *          Higher m → denser network, lower variance in degree distribution
     */
    public BarabasiAlbertNetwork(int m) {
        if (m < 1) {
            throw new IllegalArgumentException("m must be >= 1, got: " + m);
        }
        this.m = m;
    }

    @Override
    public Dataset<Row> generateEdges(SparkSession spark, int populationSize, long seed) {
        if (populationSize < m + 1) {
            throw new IllegalArgumentException(
                    "Population size must be >= m+1 = " + (m + 1) + ", got: " + populationSize);
        }

        // For large populations (>100K), use distributed generation
        // For now, use in-memory generation (sufficient for research experiments)
        return generateInMemory(spark, populationSize, seed);
    }

    /**
     * In-memory generation suitable for populations up to ~1M nodes.
     * For larger populations, would need distributed algorithm.
     */
    private Dataset<Row> generateInMemory(SparkSession spark, int populationSize, long seed) {
        Random random = new Random(seed);
        List<Edge> edges = new ArrayList<>();

        // Step 1: Initialize with complete graph of m+1 nodes
        int m0 = m + 1;
        for (int i = 0; i < m0; i++) {
            for (int j = i + 1; j < m0; j++) {
                edges.add(new Edge((long) i, (long) j, 1.0));
                edges.add(new Edge((long) j, (long) i, 1.0)); // Undirected
            }
        }

        // Degree tracker for preferential attachment
        Map<Integer, Integer> degrees = new HashMap<>();
        for (int i = 0; i < m0; i++) {
            degrees.put(i, m0 - 1); // Initial degree = m0-1
        }

        // Step 2: Add remaining nodes with preferential attachment
        for (int newNode = m0; newNode < populationSize; newNode++) {
            // Select m targets with probability proportional to degree
            Set<Integer> targets = selectTargets(degrees, m, random);

            for (int target : targets) {
                edges.add(new Edge((long) newNode, (long) target, 1.0));
                edges.add(new Edge((long) target, (long) newNode, 1.0)); // Undirected

                // Update degree
                degrees.put(target, degrees.get(target) + 1);
            }
            degrees.put(newNode, m);
        }

        // Step 3: Convert to DataFrame
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

    /**
     * Preferential attachment: select m targets with probability ∝ degree.
     * 
     * Uses cumulative distribution function (CDF) sampling:
     * - Build CDF from degree distribution
     * - Sample uniform random numbers
     * - Map to nodes via CDF lookup
     */
    private Set<Integer> selectTargets(Map<Integer, Integer> degrees, int m, Random random) {
        Set<Integer> selected = new HashSet<>();

        // Compute total degree for normalization
        int totalDegree = degrees.values().stream().mapToInt(Integer::intValue).sum();

        // Sample m distinct targets
        while (selected.size() < m) {
            double roll = random.nextDouble() * totalDegree;
            double cumulative = 0;

            for (Map.Entry<Integer, Integer> entry : degrees.entrySet()) {
                cumulative += entry.getValue();
                if (cumulative >= roll) {
                    selected.add(entry.getKey());
                    break;
                }
            }
        }

        return selected;
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

        // Degree distribution
        Dataset<Row> degreeDist = edges
                .groupBy("source")
                .agg(count("target").as("degree"))
                .groupBy("degree")
                .agg(count("*").as("count"))
                .orderBy("degree");

        // Fit power law: P(k) = C * k^(-gamma)
        double gamma = fitPowerLaw(degreeDist);
        metrics.put("power_law_exponent", gamma);

        // Degree statistics
        Dataset<Row> degreeStats = edges
                .groupBy("source")
                .agg(count("target").as("degree"))
                .agg(
                        max("degree").as("max_degree"),
                        min("degree").as("min_degree"),
                        stddev("degree").as("degree_stddev"));

        Row stats = degreeStats.first();
        metrics.put("max_degree", (double) stats.getLong(0));
        metrics.put("min_degree", (double) stats.getLong(1));
        if (stats.get(2) != null) {
            metrics.put("degree_stddev", stats.getDouble(2));
        }

        return metrics;
    }

    /**
     * Fit power law exponent using log-log linear regression.
     * 
     * Model: log(P(k)) = log(C) - γ * log(k)
     * Slope of regression line = -γ
     * 
     * Note: For publication-quality analysis, would use MLE estimator from
     * Clauset et al. (2009) "Power-law distributions in empirical data"
     */
    private double fitPowerLaw(Dataset<Row> degreeDist) {
        List<Row> data = degreeDist.collectAsList();

        double sumLogK = 0, sumLogP = 0, sumLogKLogP = 0, sumLogK2 = 0;
        int n = 0;

        for (Row row : data) {
            long degree = row.getLong(0);
            long count = row.getLong(1);

            if (degree > 1) { // Skip k=0,1 (noise in power law tail)
                double logK = Math.log(degree);
                double logP = Math.log(count);

                sumLogK += logK;
                sumLogP += logP;
                sumLogKLogP += logK * logP;
                sumLogK2 += logK * logK;
                n++;
            }
        }

        if (n < 2) {
            return Double.NaN;
        }

        // Linear regression slope
        double slope = (n * sumLogKLogP - sumLogK * sumLogP) /
                (n * sumLogK2 - sumLogK * sumLogK);

        return -slope; // Return positive exponent
    }

    @Override
    public double getExpectedAverageDegree(int populationSize) {
        return 2.0 * m; // Each node adds m edges (undirected = 2m degree)
    }

    @Override
    public String getTopologyName() {
        return "BarabasiAlbert_m" + m;
    }

    /**
     * Helper class for edge representation during generation
     */
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
