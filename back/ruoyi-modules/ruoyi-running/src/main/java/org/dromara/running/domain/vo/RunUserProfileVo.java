package org.dromara.running.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.running.domain.RunUserProfile;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * 当前登录用户运动档案视图。
 */
@Data
@AutoMapper(target = RunUserProfile.class)
public class RunUserProfileVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Long avatarOssId;
    private String gender;
    private LocalDate birthDate;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private String provinceCode;
    private String provinceName;
    private String cityCode;
    private String cityName;
    private Boolean profileCompleted;
    private Date createTime;
    private Date updateTime;
}
