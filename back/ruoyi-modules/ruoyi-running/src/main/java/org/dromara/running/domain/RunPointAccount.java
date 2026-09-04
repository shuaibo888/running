package org.dromara.running.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.time.LocalDate;

/** 用户积分账户。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("run_point_account")
public class RunPointAccount extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;
    private Long userId;
    private Integer balance;
    private Integer totalEarned;
    private Integer currentCheckinStreak;
    private LocalDate lastCheckinDate;

    @Version
    private Long version;

    @TableLogic
    private Long delFlag;
}
