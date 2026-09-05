package org.dromara.running.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 可独立审计和测试的运动结算公式。
 */
public final class RunWorkoutCalculator {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal RUNNING_CALORIE_FACTOR = new BigDecimal("1.036");
    private static final double EARTH_RADIUS_METERS = 6_371_008.8D;

    private RunWorkoutCalculator() {
    }

    public static BigDecimal runningCalories(BigDecimal weightKg, BigDecimal distanceMeters) {
        if (weightKg == null || weightKg.signum() <= 0
            || distanceMeters == null || distanceMeters.signum() <= 0) {
            return ZERO;
        }
        return weightKg.multiply(distanceMeters)
            .multiply(RUNNING_CALORIE_FACTOR)
            .divide(BigDecimal.valueOf(1000L), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal metCalories(BigDecimal weightKg, BigDecimal metValue, int durationMinutes) {
        if (weightKg == null || weightKg.signum() <= 0
            || metValue == null || metValue.signum() <= 0 || durationMinutes <= 0) {
            return ZERO;
        }
        return metValue.multiply(weightKg)
            .multiply(BigDecimal.valueOf(durationMinutes))
            .divide(BigDecimal.valueOf(60L), 2, RoundingMode.HALF_UP);
    }

    public static Integer averagePaceSeconds(int elapsedSeconds, BigDecimal distanceMeters) {
        if (elapsedSeconds <= 0 || distanceMeters == null || distanceMeters.compareTo(BigDecimal.TEN) < 0) {
            return null;
        }
        return BigDecimal.valueOf(elapsedSeconds)
            .multiply(BigDecimal.valueOf(1000L))
            .divide(distanceMeters, 0, RoundingMode.HALF_UP)
            .intValue();
    }

    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double latDelta = Math.toRadians(lat2 - lat1);
        double lngDelta = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDelta / 2D) * Math.sin(latDelta / 2D)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lngDelta / 2D) * Math.sin(lngDelta / 2D);
        return EARTH_RADIUS_METERS * 2D * Math.atan2(Math.sqrt(a), Math.sqrt(1D - a));
    }
}
