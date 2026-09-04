package org.dromara.running.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 成就定义、实时进度和解锁事实。 */
@Data
public class RunAchievementVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String achievementCode;
    private String achievementName;
    private String description;
    private String metricType;
    private BigDecimal thresholdValue;
    private String medalLevel;
    private String iconUrl;
    private Integer rewardPoints;
    private BigDecimal currentValue;
    private BigDecimal progressPercent;
    private Boolean unlocked;
    private Date unlockedAt;
    private BigDecimal achievedValue;
}
