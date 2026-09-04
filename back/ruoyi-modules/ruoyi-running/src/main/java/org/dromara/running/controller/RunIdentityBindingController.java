package org.dromara.running.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.running.domain.vo.RunLoginMethodsVo;
import org.dromara.running.service.IRunPhoneAuthService;
import org.dromara.running.service.IRunWechatAuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录用户的登录方式绑定接口。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/user")
public class RunIdentityBindingController {

    private final IRunPhoneAuthService phoneAuthService;
    private final IRunWechatAuthService wechatAuthService;

    @GetMapping("/login-methods")
    public R<RunLoginMethodsVo> getLoginMethods() {
        Long userId = LoginHelper.getUserId();
        return R.ok(new RunLoginMethodsVo(
            wechatAuthService.isBound(userId),
            phoneAuthService.isBound(userId)
        ));
    }
}
