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

/** 用户在线路上的真实累计进度。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("run_user_route_progress")
public class RunUserRouteProgress extends TenantEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;
    private Long userId;
    private Long routeId;
    private BigDecimal accumulatedDistanceMeters;
    private Integer reachedNodeOrder;
    private Integer selectedFlag;
    private Long lastWorkoutId;
    @Version
    private Long version;
    @TableLogic
    private Long delFlag;
}
