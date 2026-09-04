package org.dromara.running.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;

/** 可运营配置的里程碑成就。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("run_achievement")
public class RunAchievement extends TenantEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;
    private String achievementCode;
    private String achievementName;
    private String description;
    private String metricType;
    private BigDecimal thresholdValue;
    private String medalLevel;
    private String iconUrl;
    private Integer rewardPoints;
    private String status;
    private Integer sortOrder;
    @TableLogic
    private Long delFlag;
}
