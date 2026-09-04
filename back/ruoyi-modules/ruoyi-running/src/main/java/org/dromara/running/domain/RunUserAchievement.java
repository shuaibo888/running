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

/** 用户成就解锁事实。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("run_user_achievement")
public class RunUserAchievement extends TenantEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;
    private Long userId;
    private Long achievementId;
    private Date unlockedAt;
    private BigDecimal achievedValue;
    private Long sourceWorkoutId;
    @TableLogic
    private Long delFlag;
}
