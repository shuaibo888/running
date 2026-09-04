package org.dromara.running.service;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.running.config.RunAuthProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * 使用服务端稳定密钥生成身份 HMAC，避免手机号摘要可被离线枚举。
 */
@RequiredArgsConstructor
@Service
public class RunIdentityHashService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final RunAuthProperties properties;

    public String hash(String namespace, String value) {
        String secret = properties.getIdentityHashSecret();
        if (StringUtils.isBlank(secret) || secret.length() < 32) {
            throw new ServiceException("身份摘要密钥未配置或长度不足32位");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal((namespace + ":" + value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new ServiceException("身份摘要计算失败");
        }
    }
}
