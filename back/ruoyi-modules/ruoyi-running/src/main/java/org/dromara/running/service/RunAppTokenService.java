package org.dromara.running.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.running.config.RunAuthProperties;
import org.dromara.running.domain.vo.RunLoginVo;
import org.dromara.running.domain.vo.RunUserProfileVo;
import org.dromara.system.domain.SysUser;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 微信和手机号登录共用的小程序令牌签发服务。
 */
@RequiredArgsConstructor
@Service
public class RunAppTokenService {

    private final RunAuthProperties properties;
    private final IRunUserProfileService profileService;

    public RunLoginVo issue(SysUser user) {
        if (user == null) {
            throw new ServiceException("登录用户不存在");
        }
        if (SystemConstants.DISABLE.equals(user.getStatus())) {
            throw new ServiceException("账号已停用，请联系管理员");
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setTenantId(user.getTenantId());
        loginUser.setUserId(user.getUserId());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setUsername(user.getUserName());
        loginUser.setNickname(user.getNickName());
        loginUser.setUserType(UserType.APP_USER.getUserType());
        loginUser.setMenuPermission(Set.of());
        loginUser.setRolePermission(Set.of());
        loginUser.setClientKey("running");
        loginUser.setDeviceType("xcx");

        SaLoginParameter model = new SaLoginParameter();
        model.setDeviceType("xcx");
        model.setTimeout(properties.getTokenTimeout());
        model.setActiveTimeout(properties.getTokenActiveTimeout());
        model.setExtra(LoginHelper.CLIENT_KEY, properties.getClientId());
        LoginHelper.login(loginUser, model);

        RunUserProfileVo profile = profileService.queryCurrent(user.getUserId());
        return new RunLoginVo(
            StpUtil.getTokenValue(),
            StpUtil.getTokenTimeout(),
            properties.getClientId(),
            profile.getProfileCompleted()
        );
    }
}
