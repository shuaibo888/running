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
 * 微信身份映射对象 run_wechat_identity。
 *
 * <p>只保存不可逆摘要，不保存 session_key，也不把微信身份标识返回给小程序。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("run_wechat_identity")
public class RunWechatIdentity extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /** 微信小程序 AppID。 */
    private String appId;

    /** AppID 与 openid 组合后的 HMAC-SHA-256 摘要。 */
    private String openidHash;

    /** unionid 的 HMAC-SHA-256 摘要，微信未返回时为空。 */
    private String unionidHash;

    /** 关联的平台用户 ID。 */
    private Long userId;

    /** 最近一次微信登录时间。 */
    private Date lastLoginTime;

    @TableLogic
    private Long delFlag;
}
