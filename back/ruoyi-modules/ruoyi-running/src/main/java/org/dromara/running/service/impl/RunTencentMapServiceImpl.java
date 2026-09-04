package org.dromara.running.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.running.config.RunTencentMapProperties;
import org.dromara.running.domain.vo.RunReverseGeocodeVo;
import org.dromara.running.service.IRunMapService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 腾讯位置服务 WebService API 实现。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RunTencentMapServiceImpl implements IRunMapService {

    private static final String REVERSE_GEOCODE_URL = "https://apis.map.qq.com/ws/geocoder/v1/";
    private static final String CACHE_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "running:map:reverse:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    private final RunTencentMapProperties properties;

    @Override
    public RunReverseGeocodeVo reverseGeocode(double latitude, double longitude) {
        validateConfiguration();
        String cacheKey = buildCacheKey(latitude, longitude);
        RunReverseGeocodeVo cached = RedisUtils.getCacheObject(cacheKey);
        if (cached != null) {
            return withCoordinates(cached, latitude, longitude);
        }

        RunReverseGeocodeVo result = requestTencent(latitude, longitude);
        RedisUtils.setCacheObject(cacheKey, result, CACHE_TTL);
        return result;
    }

    private void validateConfiguration() {
        if (StringUtils.isBlank(properties.getWebserviceKey())) {
            throw new ServiceException("腾讯位置服务尚未完成服务端配置");
        }
    }

    private RunReverseGeocodeVo requestTencent(double latitude, double longitude) {
        String location = latitude + "," + longitude;
        String query = "location=" + encode(location)
            + "&key=" + encode(properties.getWebserviceKey())
            + "&get_poi=0";
        HttpRequest request = HttpRequest.newBuilder(URI.create(REVERSE_GEOCODE_URL + "?" + query))
            .timeout(Duration.ofSeconds(5))
            .header("Accept", "application/json")
            .GET()
            .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("腾讯逆地址解析 HTTP 状态异常: {}", response.statusCode());
                throw new ServiceException("位置解析暂时不可用，请稍后重试");
            }
            return parseResponse(response.body(), latitude, longitude);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("位置解析请求已中断，请重试");
        } catch (IOException e) {
            log.warn("腾讯逆地址解析网络请求失败: {}", e.getClass().getSimpleName());
            throw new ServiceException("位置解析暂时不可用，请检查网络后重试");
        }
    }

    private RunReverseGeocodeVo parseResponse(String body, double latitude, double longitude) {
        try {
            JsonNode root = JsonUtils.getObjectMapper().readTree(body);
            int status = root.path("status").asInt(-1);
            if (status != 0 || !root.path("result").isObject()) {
                log.warn("腾讯逆地址解析业务状态异常: status={}, message={}",
                    status, root.path("message").asText(""));
                throw new ServiceException("位置解析失败，请稍后重试");
            }

            JsonNode result = root.path("result");
            JsonNode location = result.path("location");
            JsonNode component = result.path("address_component");
            JsonNode adInfo = result.path("ad_info");
            JsonNode formatted = result.path("formatted_addresses");
            return new RunReverseGeocodeVo(
                location.path("lat").asDouble(latitude),
                location.path("lng").asDouble(longitude),
                textOrNull(result, "address"),
                textOrNull(formatted, "rough"),
                firstText(component, adInfo, "nation"),
                firstText(component, adInfo, "province"),
                firstText(component, adInfo, "city"),
                firstText(component, adInfo, "district"),
                textOrNull(adInfo, "adcode")
            );
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("腾讯逆地址解析响应无法解析: {}", e.getClass().getSimpleName());
            throw new ServiceException("位置解析结果异常，请稍后重试");
        }
    }

    private String firstText(JsonNode primary, JsonNode fallback, String field) {
        String value = textOrNull(primary, field);
        return StringUtils.isNotBlank(value) ? value : textOrNull(fallback, field);
    }

    private RunReverseGeocodeVo withCoordinates(RunReverseGeocodeVo source,
                                                 double latitude,
                                                 double longitude) {
        return new RunReverseGeocodeVo(
            latitude,
            longitude,
            source.formattedAddress(),
            source.roughAddress(),
            source.nation(),
            source.province(),
            source.city(),
            source.district(),
            source.adcode()
        );
    }

    private String textOrNull(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return StringUtils.isBlank(value) ? null : value;
    }

    private String buildCacheKey(double latitude, double longitude) {
        return CACHE_PREFIX + roundCoordinate(latitude) + ":" + roundCoordinate(longitude);
    }

    private String roundCoordinate(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).toPlainString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
