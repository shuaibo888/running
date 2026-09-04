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
import java.time.LocalDate;

/**
 * 小程序用户运动档案对象 run_user_profile。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("run_user_profile")
public class RunUserProfile extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(value = "id")
    private Long id;

    /** 关联平台登录用户 ID，不允许由客户端修改。 */
    private Long userId;

    /** 昵称。 */
    private String nickname;

    /** 头像地址。 */
    private String avatarUrl;

    /** 头像对应的对象存储记录 ID。 */
    private Long avatarOssId;

    /** 性别：0 未知，1 男，2 女。 */
    private String gender;

    /** 出生日期，用于后续运动消耗估算。 */
    private LocalDate birthDate;

    /** 身高，单位厘米。 */
    private BigDecimal heightCm;

    /** 体重，单位千克。 */
    private BigDecimal weightKg;

    /** 常驻省份行政区划编码。 */
    private String provinceCode;

    /** 常驻省份名称。 */
    private String provinceName;

    /** 常驻城市行政区划编码。 */
    private String cityCode;

    /** 常驻城市名称。 */
    private String cityName;

    /** 档案是否完整。 */
    private Boolean profileCompleted;

    /** 乐观锁版本号。 */
    @Version
    private Long version;

    /** 逻辑删除标志。 */
    @TableLogic
    private Long delFlag;
}
