package org.dromara.running.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.running.domain.RunUserProfile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 当前登录用户运动档案保存参数。
 */
@Data
@AutoMapper(target = RunUserProfile.class, reverseConvertGenerate = false)
public class RunUserProfileBo {

    @Size(max = 64, message = "昵称长度不能超过64个字符")
    private String nickname;

    @Pattern(regexp = "^[012]$", message = "性别取值必须为0、1或2")
    private String gender;

    @PastOrPresent(message = "出生日期不能晚于今天")
    private LocalDate birthDate;

    @DecimalMin(value = "50.00", message = "身高不能小于50厘米")
    @DecimalMax(value = "300.00", message = "身高不能大于300厘米")
    @Digits(integer = 3, fraction = 2, message = "身高最多保留两位小数")
    private BigDecimal heightCm;

    @DecimalMin(value = "20.00", message = "体重不能小于20千克")
    @DecimalMax(value = "500.00", message = "体重不能大于500千克")
    @Digits(integer = 3, fraction = 2, message = "体重最多保留两位小数")
    private BigDecimal weightKg;

    @Size(max = 32, message = "省份编码长度不能超过32个字符")
    private String provinceCode;

    @Size(max = 64, message = "省份名称长度不能超过64个字符")
    private String provinceName;

    @Size(max = 32, message = "城市编码长度不能超过32个字符")
    private String cityCode;

    @Size(max = 64, message = "城市名称长度不能超过64个字符")
    private String cityName;
}
