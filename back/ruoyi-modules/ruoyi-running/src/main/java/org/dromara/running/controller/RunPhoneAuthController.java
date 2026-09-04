package org.dromara.running.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.common.ratelimiter.enums.LimitType;
import org.dromara.running.domain.bo.RunPhoneBo;
import org.dromara.running.domain.bo.RunPhoneLoginBo;
import org.dromara.running.domain.vo.RunLoginVo;
import org.dromara.running.service.IRunPhoneAuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 手机号验证码登录接口。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/auth")
public class RunPhoneAuthController {

    private final IRunPhoneAuthService phoneAuthService;

    @SaIgnore
    @RateLimiter(key = "#bo.phone", time = 60, count = 1)
    @PostMapping("/sms-code")
    public R<Void> sendCode(@Validated @RequestBody RunPhoneBo bo) {
        phoneAuthService.sendCode(bo.getPhone());
        return R.ok("验证码已发送");
    }

    @SaIgnore
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/phone-login")
    public R<RunLoginVo> phoneLogin(@Validated @RequestBody RunPhoneLoginBo bo) {
        return R.ok("登录成功", phoneAuthService.login(bo.getPhone(), bo.getCode()));
    }
}
