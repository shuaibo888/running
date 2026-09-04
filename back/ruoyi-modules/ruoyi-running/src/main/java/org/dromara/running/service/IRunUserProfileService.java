package org.dromara.running.service;

import org.dromara.running.domain.bo.RunUserProfileBo;
import org.dromara.running.domain.vo.RunUserProfileVo;

/**
 * 小程序用户运动档案服务。
 */
public interface IRunUserProfileService {

    /** 查询当前登录用户的运动档案。 */
    RunUserProfileVo queryCurrent(Long userId);

    /** 保存当前登录用户的运动档案。 */
    RunUserProfileVo saveCurrent(Long userId, RunUserProfileBo bo);

    /** 更新当前登录用户由对象存储托管的头像。 */
    RunUserProfileVo updateAvatar(Long userId, Long avatarOssId, String avatarUrl);
}
