package org.dromara.running.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 手机号身份摘要映射 run_phone_identity。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("run_phone_identity")
public class RunPhoneIdentity extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /** 规范化手机号的 HMAC-SHA-256 摘要。 */
    private String phoneHash;

    /** 关联的平台用户 ID。 */
    private Long userId;

    /** 最近一次手机号登录时间。 */
    private Date lastLoginTime;

    @TableLogic
    private Long delFlag;
}
