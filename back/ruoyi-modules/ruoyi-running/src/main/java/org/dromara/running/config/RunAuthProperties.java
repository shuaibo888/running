package org.dromara.running.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 两种小程序登录方式共用的会话配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "running.auth")
public class RunAuthProperties {

    private String tenantId = "000000";
    private String clientId = "running-miniprogram";
    private String identityHashSecret;
    private long tokenTimeout = 604800L;
    private long tokenActiveTimeout = 86400L;
}
