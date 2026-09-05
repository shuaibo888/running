package org.dromara.running.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class RunWorkoutCalculatorTest {

    @Test
    void calculatesRunningCaloriesFromAuditableSnapshotValues() {
        assertEquals(new BigDecimal("321.16"), RunWorkoutCalculator.runningCalories(
            new BigDecimal("62"), new BigDecimal("5000")));
        assertEquals(new BigDecimal("0.00"), RunWorkoutCalculator.runningCalories(null, new BigDecimal("5000")));
        assertEquals(new BigDecimal("0.00"), RunWorkoutCalculator.runningCalories(new BigDecimal("62"), BigDecimal.ZERO));
    }

    @Test
    void calculatesMetCaloriesAndRejectsInvalidInputs() {
        assertEquals(new BigDecimal("114.00"), RunWorkoutCalculator.metCalories(
            new BigDecimal("60"), new BigDecimal("3.8"), 30));
        assertEquals(new BigDecimal("0.00"), RunWorkoutCalculator.metCalories(
            new BigDecimal("60"), new BigDecimal("3.8"), 0));
    }

    @Test
    void calculatesPaceOnlyForMeaningfulDistanceAndDuration() {
        assertEquals(360, RunWorkoutCalculator.averagePaceSeconds(1800, new BigDecimal("5000")));
        assertNull(RunWorkoutCalculator.averagePaceSeconds(0, new BigDecimal("5000")));
        assertNull(RunWorkoutCalculator.averagePaceSeconds(60, new BigDecimal("9.99")));
    }

    @Test
    void calculatesGcj02PointDistanceWithoutDependingOnMapProvider() {
        assertEquals(0D, RunWorkoutCalculator.haversineMeters(39.9D, 116.4D, 39.9D, 116.4D), 0.001D);
        double oneLongitudeDegreeInBeijing = RunWorkoutCalculator.haversineMeters(39.9D, 116.4D, 39.9D, 117.4D);
        assertTrue(oneLongitudeDegreeInBeijing > 85_000D && oneLongitudeDegreeInBeijing < 86_000D);
    }
}
