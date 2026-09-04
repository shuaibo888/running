package org.dromara.running.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/** 单日已结算运动聚合点。 */
@Data
public class RunWorkoutTrendPointVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String trendDate;
    private Long workoutCount;
    private Long totalElapsedSeconds;
    private BigDecimal totalDistanceMeters;
    private BigDecimal totalCaloriesKcal;
}
