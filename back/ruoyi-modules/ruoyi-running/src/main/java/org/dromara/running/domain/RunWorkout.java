package org.dromara.running.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 单次运动记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("run_workout")
public class RunWorkout extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;
    private Long userId;
    private String clientWorkoutId;
    private String sportType;
    private String sportName;
    private String recordSource;
    private String status;
    private Integer activeFlag;
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
    private String rankingProvinceCode;
    private String rankingProvinceName;
    private String rankingCityCode;
    private String rankingCityName;
    private Long routeId;
    private BigDecimal startLatitude;
    private BigDecimal startLongitude;
    private BigDecimal endLatitude;
    private BigDecimal endLongitude;
    private Integer pointCount;
    private Integer invalidPointCount;
    private Integer lastTrackSeq;
    private Date lastPointTime;
    private String finishRequestId;
    private String cityResolveStatus;
    private Integer cityResolveAttempts;
    private Date cityResolveNextAt;
    private Date cityResolvedAt;
    private String cityResolveError;

    @Version
    private Long version;

    @TableLogic
    private Long delFlag;
}
