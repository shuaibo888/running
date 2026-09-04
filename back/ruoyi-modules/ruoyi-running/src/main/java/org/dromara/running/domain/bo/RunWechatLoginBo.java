package org.dromara.running.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 微信小程序登录参数。
 */
@Data
public class RunWechatLoginBo {

    /** uni.login 返回的一次性临时 code，用于换取 openid。 */
    @NotBlank(message = "微信登录凭证不能为空")
    @Size(max = 128, message = "微信登录凭证格式不正确")
    private String code;

    /** getPhoneNumber 回调返回的一次性动态令牌，用于换取微信验证手机号。 */
    @NotBlank(message = "微信手机号授权凭证不能为空")
    @Size(max = 256, message = "微信手机号授权凭证格式不正确")
    private String phoneCode;
}
