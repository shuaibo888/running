package org.dromara.running.service;

import org.dromara.running.domain.vo.RunLoginVo;

/**
 * 微信小程序认证服务。
 */
public interface IRunWechatAuthService {

    /** 使用微信身份 code 和手机号授权 code 一次建立平台登录态。 */
    RunLoginVo login(String code, String phoneCode);

    /** 当前平台用户是否已经绑定微信身份。 */
    boolean isBound(Long userId);
}
