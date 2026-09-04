package org.dromara.running.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.running.domain.RunUserProfile;
import org.dromara.running.domain.bo.RunUserProfileBo;
import org.dromara.running.domain.vo.RunUserProfileVo;
import org.dromara.running.mapper.RunUserProfileMapper;
import org.dromara.running.service.IRunUserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 小程序用户运动档案服务实现。
 */
@RequiredArgsConstructor
@Service
public class RunUserProfileServiceImpl implements IRunUserProfileService {

    private final RunUserProfileMapper baseMapper;

    @Override
    public RunUserProfileVo queryCurrent(Long userId) {
        RunUserProfileVo profile = baseMapper.selectVoOne(Wrappers.<RunUserProfile>lambdaQuery()
            .eq(RunUserProfile::getUserId, userId));
        if (profile == null) {
            profile = new RunUserProfileVo();
            profile.setUserId(userId);
            profile.setProfileCompleted(false);
        }
        return profile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RunUserProfileVo saveCurrent(Long userId, RunUserProfileBo bo) {
        RunUserProfile profile = baseMapper.selectOne(Wrappers.<RunUserProfile>lambdaQuery()
            .eq(RunUserProfile::getUserId, userId));
        boolean isNew = profile == null;
        if (isNew) {
            profile = new RunUserProfile();
            profile.setUserId(userId);
        }

        applyEditableFields(profile, bo);
        profile.setProfileCompleted(isProfileCompleted(profile));

        int affectedRows = isNew ? baseMapper.insert(profile) : baseMapper.updateById(profile);
        if (affectedRows < 1) {
            throw new ServiceException("运动档案保存失败，请稍后重试");
        }
        return queryCurrent(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RunUserProfileVo updateAvatar(Long userId, Long avatarOssId, String avatarUrl) {
        if (avatarOssId == null || StringUtils.isBlank(avatarUrl)) {
            throw new ServiceException("头像上传结果无效，请稍后重试");
        }
        RunUserProfile profile = baseMapper.selectOne(Wrappers.<RunUserProfile>lambdaQuery()
            .eq(RunUserProfile::getUserId, userId));
        boolean isNew = profile == null;
        if (isNew) {
            profile = new RunUserProfile();
            profile.setUserId(userId);
        }
        profile.setAvatarOssId(avatarOssId);
        profile.setAvatarUrl(avatarUrl);
        profile.setProfileCompleted(isProfileCompleted(profile));

        int affectedRows = isNew ? baseMapper.insert(profile) : baseMapper.updateById(profile);
        if (affectedRows < 1) {
            throw new ServiceException("头像保存失败，请稍后重试");
        }
        return queryCurrent(userId);
    }

    private void applyEditableFields(RunUserProfile profile, RunUserProfileBo bo) {
        profile.setNickname(StringUtils.trim(bo.getNickname()));
        profile.setGender(bo.getGender());
        profile.setBirthDate(bo.getBirthDate());
        profile.setHeightCm(bo.getHeightCm());
        profile.setWeightKg(bo.getWeightKg());
        profile.setProvinceCode(StringUtils.trim(bo.getProvinceCode()));
        profile.setProvinceName(StringUtils.trim(bo.getProvinceName()));
        profile.setCityCode(StringUtils.trim(bo.getCityCode()));
        profile.setCityName(StringUtils.trim(bo.getCityName()));
    }

    private boolean isProfileCompleted(RunUserProfile profile) {
        return StringUtils.isNotBlank(profile.getNickname())
            && StringUtils.equalsAny(profile.getGender(), "1", "2")
            && profile.getBirthDate() != null
            && profile.getHeightCm() != null
            && profile.getWeightKg() != null;
    }
}
