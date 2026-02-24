package com.simulation.common.dto;

import com.simulation.common.model.Metrics;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentResponse {

    private String experimentId;
    private String name;
    private String status;
    private long currentStep;
    private SimulationParams params;
    private Instant createdAt;
    private Instant updatedAt;

    private PopulationSummary populationSummary;

    private List<Metrics> metricsHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopulationSummary {
        private long totalHumans;
        private long joyCount;
        private long neutralCount;
        private long rageCount;
        private double averageAttention;
        private double averageFatigue;
        private long collapsedCount;
    }
}
