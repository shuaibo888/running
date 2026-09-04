package org.dromara.running.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 燃赛路跑微信小程序配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "running.wechat")
public class RunWechatProperties {

    /** 微信小程序 AppID。 */
    private String appId;

    /** 微信小程序 AppSecret；开发期写在服务端配置，部署前迁移至安全注入。 */
    private String appSecret;

}
