package org.dromara.running.domain.vo;

import java.io.Serializable;

/**
 * 腾讯位置服务逆地址解析结果。
 */
public record RunReverseGeocodeVo(
    double latitude,
    double longitude,
    String formattedAddress,
    String roughAddress,
    String nation,
    String province,
    String city,
    String district,
    String adcode
) implements Serializable {
}
