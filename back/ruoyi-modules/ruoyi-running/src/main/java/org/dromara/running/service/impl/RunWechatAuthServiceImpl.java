package org.dromara.running.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.request.AuthWechatMiniProgramRequest;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.running.config.RunAuthProperties;
import org.dromara.running.config.RunWechatProperties;
import org.dromara.running.domain.RunPhoneIdentity;
import org.dromara.running.domain.RunWechatIdentity;
import org.dromara.running.domain.vo.RunLoginVo;
import org.dromara.running.mapper.RunPhoneIdentityMapper;
import org.dromara.running.mapper.RunWechatIdentityMapper;
import org.dromara.running.service.RunAppTokenService;
import org.dromara.running.service.RunIdentityHashService;
import org.dromara.running.service.IRunWechatAuthService;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

/**
 * 微信小程序认证服务实现。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RunWechatAuthServiceImpl implements IRunWechatAuthService {

    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String PHONE_NUMBER_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";
    private static final String ACCESS_TOKEN_CACHE_PREFIX =
        GlobalConstants.GLOBAL_REDIS_KEY + "running:wechat:access-token:";
    private static final Object ACCESS_TOKEN_LOCK = new Object();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    private final RunWechatProperties properties;
    private final RunAuthProperties authProperties;
    private final RunWechatIdentityMapper identityMapper;
    private final RunPhoneIdentityMapper phoneIdentityMapper;
    private final SysUserMapper sysUserMapper;
    private final ISysUserService userService;
    private final RunAppTokenService tokenService;
    private final RunIdentityHashService identityHashService;
    private final TransactionTemplate transactionTemplate;

    @Override
    public RunLoginVo login(String code, String phoneCode) {
        validateConfiguration();
        WechatIdentity wechatIdentity = exchangeCode(code);
        String phone = exchangePhoneCode(phoneCode);
        SysUser user = TenantHelper.dynamic(authProperties.getTenantId(),
            () -> transactionTemplate.execute(status -> resolveOrCreateUser(wechatIdentity, phone)));

        return tokenService.issue(user);
    }

    @Override
    public boolean isBound(Long userId) {
        return TenantHelper.dynamic(authProperties.getTenantId(), () ->
            identityMapper.selectCount(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<RunWechatIdentity>lambdaQuery()
                    .eq(RunWechatIdentity::getAppId, properties.getAppId())
                    .eq(RunWechatIdentity::getUserId, userId)) > 0);
    }

    private void validateConfiguration() {
        if (StringUtils.isAnyBlank(properties.getAppId(), properties.getAppSecret(),
            authProperties.getTenantId(), authProperties.getClientId(),
            authProperties.getIdentityHashSecret())) {
            throw new ServiceException("微信小程序登录尚未完成服务端配置");
        }
    }

    private WechatIdentity exchangeCode(String code) {
        AuthRequest authRequest = new AuthWechatMiniProgramRequest(AuthConfig.builder()
            .clientId(properties.getAppId())
            .clientSecret(properties.getAppSecret())
            .ignoreCheckRedirectUri(true)
            .ignoreCheckState(true)
            .build());
        AuthCallback callback = new AuthCallback();
        callback.setCode(code);
        AuthResponse<AuthUser> response = authRequest.login(callback);
        if (!response.ok() || response.getData() == null || response.getData().getToken() == null) {
            log.warn("微信登录凭证校验失败: {}", response.getMsg());
            throw new ServiceException("微信登录失败，请重新进入小程序后再试");
        }

        AuthToken token = response.getData().getToken();
        if (StringUtils.isBlank(token.getOpenId())) {
            throw new ServiceException("微信登录未返回用户标识，请稍后重试");
        }
        return new WechatIdentity(token.getOpenId(), token.getUnionId());
    }

    private String exchangePhoneCode(String phoneCode) {
        PhoneNumberResponse response = requestPhoneNumber(phoneCode, getAccessToken(false));
        if (isAccessTokenError(response.errorCode())) {
            RedisUtils.deleteObject(accessTokenCacheKey());
            response = requestPhoneNumber(phoneCode, getAccessToken(true));
        }
        if (response.errorCode() != 0 || StringUtils.isBlank(response.phoneNumber())) {
            log.warn("微信手机号授权失败: errcode={}, errmsg={}",
                response.errorCode(), response.errorMessage());
            throw new ServiceException("微信手机号授权失败，请重新点击微信一键登录");
        }
        return normalizeMainlandPhone(response.phoneNumber());
    }

    private PhoneNumberResponse requestPhoneNumber(String phoneCode, String accessToken) {
        String url = PHONE_NUMBER_URL + "?access_token=" + encode(accessToken);
        String requestBody = JsonUtils.toJsonString(Map.of("code", phoneCode));
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(5))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .build();
        JsonNode root = requestWechat(request, "手机号授权");
        int errorCode = root.path("errcode").asInt(-1);
        JsonNode phoneInfo = root.path("phone_info");
        String phone = textOrNull(phoneInfo, "purePhoneNumber");
        if (StringUtils.isBlank(phone)) {
            phone = textOrNull(phoneInfo, "phoneNumber");
        }
        return new PhoneNumberResponse(errorCode, root.path("errmsg").asText(""), phone);
    }

    private String getAccessToken(boolean forceRefresh) {
        String cacheKey = accessTokenCacheKey();
        if (!forceRefresh) {
            String cached = RedisUtils.getCacheObject(cacheKey);
            if (StringUtils.isNotBlank(cached)) {
                return cached;
            }
        }

        synchronized (ACCESS_TOKEN_LOCK) {
            if (!forceRefresh) {
                String cached = RedisUtils.getCacheObject(cacheKey);
                if (StringUtils.isNotBlank(cached)) {
                    return cached;
                }
            }
            String query = "grant_type=client_credential"
                + "&appid=" + encode(properties.getAppId())
                + "&secret=" + encode(properties.getAppSecret());
            HttpRequest request = HttpRequest.newBuilder(URI.create(ACCESS_TOKEN_URL + "?" + query))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .GET()
                .build();
            JsonNode root = requestWechat(request, "接口调用凭证");
            String accessToken = textOrNull(root, "access_token");
            int expiresIn = root.path("expires_in").asInt(7200);
            if (StringUtils.isBlank(accessToken)) {
                log.warn("微信接口调用凭证获取失败: errcode={}, errmsg={}",
                    root.path("errcode").asInt(-1), root.path("errmsg").asText(""));
                throw new ServiceException("微信服务暂时不可用，请稍后重试");
            }
            RedisUtils.setCacheObject(cacheKey, accessToken,
                Duration.ofSeconds(Math.max(60, expiresIn - 300L)));
            return accessToken;
        }
    }

    private JsonNode requestWechat(HttpRequest request, String operation) {
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("微信{}接口 HTTP 状态异常: {}", operation, response.statusCode());
                throw new ServiceException("微信服务暂时不可用，请稍后重试");
            }
            return JsonUtils.getObjectMapper().readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("微信服务请求已中断，请重试");
        } catch (IOException e) {
            log.warn("微信{}接口请求失败: {}", operation, e.getClass().getSimpleName());
            throw new ServiceException("微信服务暂时不可用，请检查网络后重试");
        }
    }

    private String normalizeMainlandPhone(String phone) {
        String normalized = StringUtils.trim(phone).replace(" ", "").replace("-", "");
        if (normalized.startsWith("+86")) {
            normalized = normalized.substring(3);
        } else if (normalized.startsWith("86") && normalized.length() == 13) {
            normalized = normalized.substring(2);
        }
        if (!normalized.matches("^1[3-9]\\d{9}$")) {
            throw new ServiceException("当前仅支持中国大陆手机号登录");
        }
        return normalized;
    }

    private boolean isAccessTokenError(int errorCode) {
        return errorCode == 40001 || errorCode == 40014 || errorCode == 42001;
    }

    private String accessTokenCacheKey() {
        return ACCESS_TOKEN_CACHE_PREFIX + properties.getAppId();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String textOrNull(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return StringUtils.isBlank(value) ? null : value;
    }

    private SysUser resolveOrCreateUser(WechatIdentity wechatIdentity, String phone) {
        String openidHash = hashIdentity("openid", wechatIdentity.openid());
        String phoneHash = identityHashService.hash("phone", phone);
        RunWechatIdentity identity = identityMapper.selectOne(
            Wrappers.<RunWechatIdentity>lambdaQuery()
                .eq(RunWechatIdentity::getAppId, properties.getAppId())
                .eq(RunWechatIdentity::getOpenidHash, openidHash));
        RunPhoneIdentity phoneIdentity = phoneIdentityMapper.selectOne(
            Wrappers.<RunPhoneIdentity>lambdaQuery()
                .eq(RunPhoneIdentity::getPhoneHash, phoneHash));

        // 平台账号以微信官方验证手机号为唯一归并依据；openid 只是登录凭证。
        Long userId = phoneIdentity != null ? phoneIdentity.getUserId() : null;
        SysUser user;
        if (userId == null) {
            user = createPhoneUser(phoneHash);
            userId = user.getUserId();
        } else {
            user = sysUserMapper.selectById(userId);
        }
        if (user == null || !UserType.APP_USER.getUserType().equals(user.getUserType())) {
            throw new ServiceException("微信用户身份映射异常，请联系管理员");
        }

        if (identity == null) {
            identity = new RunWechatIdentity();
            identity.setAppId(properties.getAppId());
            identity.setOpenidHash(openidHash);
            identity.setUnionidHash(hashNullableIdentity("unionid", wechatIdentity.unionid()));
            identity.setUserId(userId);
            identity.setLastLoginTime(new Date());
            if (identityMapper.insert(identity) < 1) {
                throw new ServiceException("微信用户身份保存失败，请稍后重试");
            }
        } else if (!userId.equals(identity.getUserId())) {
            identity.setUserId(userId);
            identity.setLastLoginTime(new Date());
            identity.setUnionidHash(hashNullableIdentity("unionid", wechatIdentity.unionid()));
            if (identityMapper.updateById(identity) < 1) {
                throw new ServiceException("微信身份归并失败，请稍后重试");
            }
        } else {
            refreshIdentity(identity, wechatIdentity.unionid());
        }

        bindVerifiedPhone(userId, phoneHash, phoneIdentity);
        return user;
    }

    private SysUser createPhoneUser(String phoneHash) {
        String username = "phone_" + phoneHash.substring(0, 22);
        SysUser user = selectUserByUsername(username);
        if (user == null) {
            SysUserBo newUser = new SysUserBo();
            newUser.setUserName(username);
            newUser.setNickName("微信用户");
            newUser.setUserType(UserType.APP_USER.getUserType());
            newUser.setPassword(BCrypt.hashpw(IdUtil.fastSimpleUUID()));
            newUser.setSex("2");
            newUser.setStatus(SystemConstants.NORMAL);
            if (!userService.registerUser(newUser, authProperties.getTenantId())) {
                throw new ServiceException("微信用户创建失败，请稍后重试");
            }
            user = selectUserByUsername(username);
        }
        return user;
    }

    private SysUser selectUserByUsername(String username) {
        return sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
            .eq(SysUser::getUserName, username));
    }

    private void bindVerifiedPhone(Long userId, String phoneHash, RunPhoneIdentity phoneIdentity) {
        Date now = new Date();
        if (phoneIdentity != null) {
            phoneIdentity.setLastLoginTime(now);
            if (phoneIdentityMapper.updateById(phoneIdentity) < 1) {
                throw new ServiceException("微信手机号登录状态更新失败，请稍后重试");
            }
            return;
        }

        RunPhoneIdentity currentPhoneIdentity = phoneIdentityMapper.selectOne(
            Wrappers.<RunPhoneIdentity>lambdaQuery().eq(RunPhoneIdentity::getUserId, userId));
        if (currentPhoneIdentity == null) {
            currentPhoneIdentity = new RunPhoneIdentity();
            currentPhoneIdentity.setPhoneHash(phoneHash);
            currentPhoneIdentity.setUserId(userId);
            currentPhoneIdentity.setLastLoginTime(now);
            if (phoneIdentityMapper.insert(currentPhoneIdentity) < 1) {
                throw new ServiceException("微信手机号身份保存失败，请稍后重试");
            }
        } else {
            currentPhoneIdentity.setPhoneHash(phoneHash);
            currentPhoneIdentity.setLastLoginTime(now);
            if (phoneIdentityMapper.updateById(currentPhoneIdentity) < 1) {
                throw new ServiceException("微信手机号身份更新失败，请稍后重试");
            }
        }
    }

    private void refreshIdentity(RunWechatIdentity identity, String unionid) {
        identity.setLastLoginTime(new Date());
        String unionidHash = hashNullableIdentity("unionid", unionid);
        if (StringUtils.isBlank(identity.getUnionidHash()) && StringUtils.isNotBlank(unionidHash)) {
            identity.setUnionidHash(unionidHash);
        }
        if (identityMapper.updateById(identity) < 1) {
            throw new ServiceException("微信身份更新失败，请稍后重试");
        }
    }

    private String hashIdentity(String type, String value) {
        return identityHashService.hash("wechat:" + properties.getAppId() + ":" + type, value);
    }

    private String hashNullableIdentity(String type, String value) {
        return StringUtils.isBlank(value) ? null : hashIdentity(type, value);
    }

    private record WechatIdentity(String openid, String unionid) {
    }

    private record PhoneNumberResponse(int errorCode, String errorMessage, String phoneNumber) {
    }
}
