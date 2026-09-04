package org.dromara.running.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/** 单次有效运动覆盖的城市快照。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("run_workout_city")
public class RunWorkoutCity extends TenantEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;
    private Long workoutId;
    private Long userId;
    private String provinceCode;
    private String provinceName;
    private String cityCode;
    private String cityName;
    private BigDecimal sampleLatitude;
    private BigDecimal sampleLongitude;
    private Date resolvedAt;

    @TableLogic
    private Long delFlag;
}
