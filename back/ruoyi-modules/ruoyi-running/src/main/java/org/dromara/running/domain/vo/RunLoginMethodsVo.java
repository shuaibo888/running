package org.dromara.running.domain.vo;

import java.io.Serializable;

/**
 * 当前用户已经绑定的登录方式，不返回原始微信标识或手机号。
 */
public record RunLoginMethodsVo(boolean wechatBound, boolean phoneBound) implements Serializable {
}
