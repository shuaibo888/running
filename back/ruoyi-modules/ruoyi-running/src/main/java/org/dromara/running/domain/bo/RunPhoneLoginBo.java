package org.dromara.running.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.dromara.common.core.constant.RegexConstants;

/**
 * 手机号验证码登录或绑定参数。
 */
@Data
public class RunPhoneLoginBo {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = RegexConstants.MOBILE, message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "短信验证码不能为空")
    @Pattern(regexp = "^\\d{4,8}$", message = "短信验证码格式不正确")
    private String code;
}
