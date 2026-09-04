package org.dromara.running.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 腾讯位置服务服务端配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "running.tencent-map")
public class RunTencentMapProperties {

    /** 仅允许配置具备 WebService API 权限的服务端 Key。 */
    private String webserviceKey;
}
