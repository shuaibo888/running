package org.dromara.running.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 小程序统一登录结果。
 */
@Data
@AllArgsConstructor
public class RunLoginVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String accessToken;
    private Long expireIn;
    private String clientId;
    private Boolean profileCompleted;
}
