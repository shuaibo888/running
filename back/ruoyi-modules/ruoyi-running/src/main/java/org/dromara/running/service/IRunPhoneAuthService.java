package org.dromara.running.service;

import org.dromara.running.domain.vo.RunLoginVo;

/**
 * 手机号验证码认证服务。
 */
public interface IRunPhoneAuthService {

    void sendCode(String phone);

    RunLoginVo login(String phone, String code);

    boolean isBound(Long userId);
}
