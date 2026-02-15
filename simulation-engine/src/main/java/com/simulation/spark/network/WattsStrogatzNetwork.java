package com.simulation.spark.network;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.util.*;

/**
 * Watts-Strogatz Small-World Network Generator
 * 
 * Creates networks with high clustering and short path lengths,
 * modeling "six degrees of separation" phenomenon.
 * 
 * Research Relevance:
 * - Tests Watts & Strogatz (1998) small-world theory
 * - High clustering (local triangles) → echo chambers
 * - Short paths despite clustering → fast global spread
 * - Tunable via rewiring probability p
 * 
 * Algorithm:
 * 1. Start with ring lattice (each node → k nearest neighbors)
 * 2. Rewire each edge with probability p
 * 3. p=0 → regular lattice, p=1 → random graph
 * 4. Small-world emerges at p ∈ [0.01, 0.1]
 * 
 * Reference: Watts & Strogatz (1998), "Collective dynamics of 'small-world'
 * networks"
 */
public class WattsStrogatzNetwork implements NetworkTopology {

    private static final long serialVersionUID = 1L;

    private final int k; // Number of nearest neighbors (must be even)
    private final double p; // Rewiring probability [0, 1]

    /**
     * @param k Number of nearest neighbors in initial lattice (must be even,
     *          typical: 4-10)
     * @param p Rewiring probability (0 = regular lattice, 1 = random graph,
     *          typical: 0.01-0.1)
     */
    public WattsStrogatzNetwork(int k, double p) {
        if (k < 2 || k % 2 != 0) {
            throw new IllegalArgumentException("k must be even and >= 2, got: " + k);
        }
        if (p < 0 || p > 1) {
            throw new IllegalArgumentException("p must be in [0, 1], got: " + p);
        }
        this.k = k;
        this.p = p;
    }

    @Override
    public Dataset<Row> generateEdges(SparkSession spark, int populationSize, long seed) {
        if (populationSize < k + 1) {
            throw new IllegalArgumentException(
                    "Population size must be > k = " + k + ", got: " + populationSize);
        }

        Random random = new Random(seed);
        List<Edge> edges = new ArrayList<>();
        Set<String> edgeSet = new HashSet<>();

        // Step 1: Create ring lattice
        // Each node i connects to nodes [i+1, i+2, ..., i+k/2] (modulo n)
        for (int i = 0; i < populationSize; i++) {
            for (int j = 1; j <= k / 2; j++) {
                int target = (i + j) % populationSize;

                String edgeKey = edgeKey(i, target);
                if (!edgeSet.contains(edgeKey)) {
                    edges.add(new Edge((long) i, (long) target, 1.0));
                    edges.add(new Edge((long) target, (long) i, 1.0)); // Undirected
                    edgeSet.add(edgeKey);
                }
            }
        }

        // Step 2: Rewire edges with probability p
        List<Edge> finalEdges = new ArrayList<>();
        Set<String> finalEdgeSet = new HashSet<>();

        for (Edge edge : edges) {
            // Only process each undirected edge once
            if (edge.source >= edge.target) {
                continue;
            }

            if (random.nextDouble() < p) {
                // Rewire: choose new random target
                int source = (int) edge.source;
                int newTarget;
                String newEdgeKey;
                int attempts = 0;

                do {
                    newTarget = random.nextInt(populationSize);
                    newEdgeKey = edgeKey(source, newTarget);
                    attempts++;

                    // Avoid infinite loop in highly constrained scenarios
                    if (attempts > 100) {
                        // Keep original edge if can't find valid rewire target
                        newTarget = (int) edge.target;
                        newEdgeKey = edgeKey(source, newTarget);
                        break;
                    }
                } while (newTarget == source || finalEdgeSet.contains(newEdgeKey));

                // Add rewired edge
                finalEdges.add(new Edge((long) source, (long) newTarget, 1.0));
                finalEdges.add(new Edge((long) newTarget, (long) source, 1.0));
                finalEdgeSet.add(newEdgeKey);
            } else {
                // Keep original edge
                finalEdges.add(edge);
                finalEdges.add(new Edge(edge.target, edge.source, 1.0));
                finalEdgeSet.add(edgeKey((int) edge.source, (int) edge.target));
            }
        }

        // Convert to DataFrame
        StructType schema = new StructType()
                .add("source", DataTypes.LongType, false)
                .add("target", DataTypes.LongType, false)
                .add("weight", DataTypes.DoubleType, false);

        List<Row> rows = new ArrayList<>(finalEdges.size());
        for (Edge e : finalEdges) {
            rows.add(org.apache.spark.sql.RowFactory.create(e.source, e.target, e.weight));
        }

        return spark.createDataFrame(rows, schema);
    }

    private String edgeKey(int source, int target) {
        int min = Math.min(source, target);
        int max = Math.max(source, target);
        return min + "-" + max;
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

        // Expected clustering coefficient for WS network with p=0
        // Formula: C_WS(p=0) = (3(k-2)) / (4(k-1))
        double expectedClusteringP0 = (3.0 * (k - 2)) / (4.0 * (k - 1));
        metrics.put("expected_clustering_p0", expectedClusteringP0);

        // Actual clustering would require triangle counting (expensive)
        // For research, use GraphFrames.triangleCount() if needed

        return metrics;
    }

    @Override
    public double getExpectedAverageDegree(int populationSize) {
        return k;
    }

    @Override
    public String getTopologyName() {
        return String.format("WattsStrogatz_k%d_p%.3f", k, p);
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
