package org.dromara.running.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 当前运动摘要。
 */
@Data
public class RunWorkoutVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String clientWorkoutId;
    private String sportType;
    private String sportName;
    private String recordSource;
    private String status;
    private Date startedAt;
    private Date pausedStartedAt;
    private Date finishedAt;
    private Integer elapsedSeconds;
    private Integer pausedSeconds;
    private BigDecimal distanceMeters;
    private BigDecimal caloriesKcal;
    private Integer avgPaceSeconds;
    private BigDecimal weightKg;
    private String calorieAlgorithm;
    private BigDecimal metValue;
    private Boolean rankingEligible;
    private Long routeId;
    private BigDecimal startLatitude;
    private BigDecimal startLongitude;
    private BigDecimal endLatitude;
    private BigDecimal endLongitude;
    private Integer pointCount;
    private Integer invalidPointCount;
    private Integer lastTrackSeq;
}
