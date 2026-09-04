package org.dromara.running.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.common.ratelimiter.enums.LimitType;
import org.dromara.running.domain.bo.RunWechatLoginBo;
import org.dromara.running.domain.vo.RunLoginVo;
import org.dromara.running.service.IRunWechatAuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 燃赛路跑微信小程序认证接口。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/auth")
public class RunWechatAuthController {

    private final IRunWechatAuthService authService;

    /** 使用 uni.login code 与 getPhoneNumber phoneCode 一次完成微信手机号授权登录。 */
    @SaIgnore
    @RateLimiter(time = 60, count = 20, limitType = LimitType.IP)
    @PostMapping("/wechat-login")
    public R<RunLoginVo> wechatLogin(@Validated @RequestBody RunWechatLoginBo bo) {
        return R.ok("登录成功", authService.login(bo.getCode(), bo.getPhoneCode()));
    }

    /** 注销当前小程序登录态。 */
    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok("退出成功");
    }
}
