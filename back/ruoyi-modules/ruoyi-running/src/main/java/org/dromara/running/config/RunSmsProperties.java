package org.dromara.running.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 手机验证码登录配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "running.sms")
public class RunSmsProperties {

    private boolean enabled = false;
    private String blendId = "config1";
    private String templateId;
    private String codeParamName = "code";
    private int codeLength = 6;
    private int expirationMinutes = 5;
}
