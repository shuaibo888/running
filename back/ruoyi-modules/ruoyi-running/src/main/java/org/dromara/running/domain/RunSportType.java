package org.dromara.running.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;

/** 可运营运动类型与卡路里规则。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("run_sport_type")
public class RunSportType extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;
    private String sportCode;
    private String sportName;
    private String trackingMode;
    private BigDecimal metValue;
    private String calorieAlgorithm;
    private Integer minDurationMinutes;
    private Integer maxDurationMinutes;
    private String status;
    private Integer sortOrder;

    @TableLogic
    private Long delFlag;
}
