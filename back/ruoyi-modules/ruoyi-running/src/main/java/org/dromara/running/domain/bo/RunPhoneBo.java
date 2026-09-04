package org.dromara.running.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.dromara.common.core.constant.RegexConstants;

/**
 * 手机验证码发送参数。
 */
@Data
public class RunPhoneBo {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = RegexConstants.MOBILE, message = "手机号格式不正确")
    private String phone;
}
