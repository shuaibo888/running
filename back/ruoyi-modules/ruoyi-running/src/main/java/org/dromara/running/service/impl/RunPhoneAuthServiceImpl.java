package org.dromara.running.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.running.config.RunAuthProperties;
import org.dromara.running.config.RunSmsProperties;
import org.dromara.running.domain.RunPhoneIdentity;
import org.dromara.running.domain.vo.RunLoginVo;
import org.dromara.running.mapper.RunPhoneIdentityMapper;
import org.dromara.running.service.IRunPhoneAuthService;
import org.dromara.running.service.RunAppTokenService;
import org.dromara.running.service.RunIdentityHashService;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 * 手机号验证码认证服务实现。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RunPhoneAuthServiceImpl implements IRunPhoneAuthService {

    private static final String SMS_CODE_KEY_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "running:sms-code:";

    private final RunAuthProperties authProperties;
    private final RunSmsProperties smsProperties;
    private final RunPhoneIdentityMapper identityMapper;
    private final SysUserMapper sysUserMapper;
    private final ISysUserService userService;
    private final RunAppTokenService tokenService;
    private final RunIdentityHashService identityHashService;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void sendCode(String phone) {
        validateSmsConfiguration();
        String normalizedPhone = normalizePhone(phone);
        String code = RandomUtil.randomNumbers(smsProperties.getCodeLength());
        LinkedHashMap<String, String> templateParams = new LinkedHashMap<>(1);
        templateParams.put(smsProperties.getCodeParamName(), code);

        SmsBlend smsBlend = SmsFactory.getSmsBlend(smsProperties.getBlendId());
        SmsResponse response = smsBlend.sendMessage(normalizedPhone, smsProperties.getTemplateId(), templateParams);
        if (response == null || !response.isSuccess()) {
            log.error("燃赛路跑登录验证码发送失败，短信供应商返回失败状态");
            throw new ServiceException("验证码发送失败，请稍后重试");
        }

        RedisUtils.setCacheObject(codeCacheKey(normalizedPhone), code,
            Duration.ofMinutes(smsProperties.getExpirationMinutes()));
    }

    @Override
    public RunLoginVo login(String phone, String code) {
        validateSmsConfiguration();
        String normalizedPhone = normalizePhone(phone);
        validateAndConsumeCode(normalizedPhone, code);
        SysUser user = TenantHelper.dynamic(authProperties.getTenantId(),
            () -> transactionTemplate.execute(status -> resolveOrCreateUser(normalizedPhone)));
        return tokenService.issue(user);
    }

    @Override
    public boolean isBound(Long userId) {
        return TenantHelper.dynamic(authProperties.getTenantId(), () ->
            identityMapper.selectCount(Wrappers.<RunPhoneIdentity>lambdaQuery()
                .eq(RunPhoneIdentity::getUserId, userId)) > 0);
    }

    private SysUser resolveOrCreateUser(String phone) {
        String phoneHash = hashPhone(phone);
        RunPhoneIdentity identity = identityMapper.selectOne(Wrappers.<RunPhoneIdentity>lambdaQuery()
            .eq(RunPhoneIdentity::getPhoneHash, phoneHash));
        if (identity != null) {
            identity.setLastLoginTime(new Date());
            identityMapper.updateById(identity);
            return sysUserMapper.selectById(identity.getUserId());
        }

        String username = "phone_" + phoneHash.substring(0, 22);
        SysUser user = selectUserByUsername(username);
        if (user == null) {
            SysUserBo newUser = new SysUserBo();
            newUser.setUserName(username);
            newUser.setNickName("手机用户");
            newUser.setUserType(UserType.APP_USER.getUserType());
            newUser.setPassword(BCrypt.hashpw(IdUtil.fastSimpleUUID()));
            newUser.setSex("2");
            newUser.setStatus(SystemConstants.NORMAL);
            if (!userService.registerUser(newUser, authProperties.getTenantId())) {
                throw new ServiceException("手机用户创建失败，请稍后重试");
            }
            user = selectUserByUsername(username);
        }
        if (user == null || !UserType.APP_USER.getUserType().equals(user.getUserType())) {
            throw new ServiceException("手机号身份映射异常，请联系管理员");
        }

        RunPhoneIdentity newIdentity = new RunPhoneIdentity();
        newIdentity.setPhoneHash(phoneHash);
        newIdentity.setUserId(user.getUserId());
        newIdentity.setLastLoginTime(new Date());
        if (identityMapper.insert(newIdentity) < 1) {
            throw new ServiceException("手机号身份保存失败，请稍后重试");
        }
        return user;
    }

    private SysUser selectUserByUsername(String username) {
        return sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
            .eq(SysUser::getUserName, username));
    }

    private void validateAndConsumeCode(String phone, String submittedCode) {
        String key = codeCacheKey(phone);
        String savedCode = RedisUtils.getCacheObject(key);
        RedisUtils.deleteObject(key);
        if (StringUtils.isBlank(savedCode)) {
            throw new ServiceException("验证码已过期，请重新获取");
        }
        if (!StringUtils.equals(savedCode, submittedCode)) {
            throw new ServiceException("验证码不正确，请重新获取");
        }
    }

    private void validateSmsConfiguration() {
        if (!smsProperties.isEnabled()) {
            throw new ServiceException("手机号验证码登录尚未启用");
        }
        if (StringUtils.isAnyBlank(smsProperties.getBlendId(), smsProperties.getTemplateId(),
            smsProperties.getCodeParamName(), authProperties.getIdentityHashSecret())) {
            throw new ServiceException("短信服务尚未完成配置");
        }
        if (smsProperties.getCodeLength() < 4 || smsProperties.getCodeLength() > 8
            || smsProperties.getExpirationMinutes() < 1) {
            throw new ServiceException("短信验证码参数配置不正确");
        }
    }

    private String normalizePhone(String phone) {
        return StringUtils.trim(phone);
    }

    private String hashPhone(String phone) {
        return identityHashService.hash("phone", phone);
    }

    private String codeCacheKey(String phone) {
        return SMS_CODE_KEY_PREFIX + hashPhone(phone);
    }
}
