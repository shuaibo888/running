package org.dromara.running.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/** 可审计、带业务幂等键的积分流水。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("run_point_transaction")
public class RunPointTransaction extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;
    private Long userId;
    private String bizType;
    private String bizKey;
    private Integer deltaPoints;
    private Integer balanceAfter;
    private String title;
    private String description;
    private Date occurredAt;
    private Long sourceWorkoutId;

    @TableLogic
    private Long delFlag;
}
