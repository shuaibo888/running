package org.dromara.running.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 用户已完成运动的可审计累计值。 */
@Data
public class RunWorkoutStatisticsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long workoutCount;
    private Long totalElapsedSeconds;
    private BigDecimal totalDistanceMeters;
    private BigDecimal totalCaloriesKcal;
    private Long cityCount;
    private Long longestConsecutiveDays;
    private Date firstWorkoutAt;
    private Date lastWorkoutAt;
}
