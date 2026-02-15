package com.simulation.spark.network;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NetworkTopologyTest {

    private static SparkSession spark;

    @BeforeAll
    static void setup() {
        spark = SparkSession.builder()
                .appName("NetworkTopologyTest")
                .master("local[2]")
                .getOrCreate();
    }

    @AfterAll
    static void teardown() {
        if (spark != null) {
            spark.stop();
        }
    }

    @Test
    void testBarabasiAlbert_generatesValidNetwork() {
        BarabasiAlbertNetwork network = new BarabasiAlbertNetwork(3);
        Dataset<Row> edges = network.generateEdges(spark, 100, 42L);

        assertNotNull(edges);
        assertTrue(edges.count() > 0);

        Map<String, Double> metrics = network.computeMetrics(edges);
        assertEquals(100.0, metrics.get("num_nodes"), 0.1);
        assertTrue(metrics.get("average_degree") >= 4.0); // Should be around 2*m
    }

    @Test
    void testWattsStrogatz_generatesValidNetwork() {
        WattsStrogatzNetwork network = new WattsStrogatzNetwork(6, 0.05);
        Dataset<Row> edges = network.generateEdges(spark, 100, 42L);

        assertNotNull(edges);
        assertTrue(edges.count() > 0);

        Map<String, Double> metrics = network.computeMetrics(edges);
        assertEquals(100.0, metrics.get("num_nodes"), 0.1);
        assertTrue(metrics.get("average_degree") >= 5.0); // Should be around k
    }

    @Test
    void testRandomGraph_generatesValidNetwork() {
        RandomGraph network = new RandomGraph(6);
        Dataset<Row> edges = network.generateEdges(spark, 100, 42L);

        assertNotNull(edges);
        assertTrue(edges.count() > 0);

        Map<String, Double> metrics = network.computeMetrics(edges);
        assertEquals(100.0, metrics.get("num_nodes"), 0.1);
    }

    @Test
    void testBarabasiAlbert_invalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> new BarabasiAlbertNetwork(0));
        assertThrows(IllegalArgumentException.class, () -> new BarabasiAlbertNetwork(11));
    }

    @Test
    void testWattsStrogatz_invalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> new WattsStrogatzNetwork(3, 0.1)); // k must be even
        assertThrows(IllegalArgumentException.class, () -> new WattsStrogatzNetwork(6, 1.5)); // p out of range
    }
}
