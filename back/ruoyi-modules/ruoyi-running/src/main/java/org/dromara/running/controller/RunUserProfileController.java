package org.dromara.running.controller;

import cn.hutool.core.io.FileUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.file.MimeTypeUtils;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.running.domain.bo.RunUserProfileBo;
import org.dromara.running.domain.vo.RunUserProfileVo;
import org.dromara.running.service.IRunUserProfileService;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

/**
 * 微信小程序当前用户运动档案接口。
 *
 * <p>接口使用框架全局 Sa-Token 登录校验，不接受客户端传入 userId。</p>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/user/profile")
public class RunUserProfileController {

    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;
    private static final int MAX_AVATAR_DIMENSION = 4096;

    private final IRunUserProfileService profileService;
    private final ISysOssService ossService;

    /** 获取当前登录用户档案。 */
    @GetMapping
    public R<RunUserProfileVo> getCurrent() {
        return R.ok(profileService.queryCurrent(LoginHelper.getUserId()));
    }

    /** 新增或更新当前登录用户档案。 */
    @RepeatSubmit
    @PutMapping
    public R<RunUserProfileVo> saveCurrent(@Validated @RequestBody RunUserProfileBo bo) {
        return R.ok("保存成功", profileService.saveCurrent(LoginHelper.getUserId(), bo));
    }

    /** 上传当前登录用户头像并立即更新运动档案。 */
    @RepeatSubmit(interval = 3000)
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<RunUserProfileVo> uploadAvatar(@RequestPart("file") MultipartFile file) {
        String validationMessage = validateAvatar(file);
        if (validationMessage != null) {
            return R.fail(validationMessage);
        }
        SysOssVo uploaded = ossService.upload(file);
        RunUserProfileVo profile = profileService.updateAvatar(
            LoginHelper.getUserId(), uploaded.getOssId(), uploaded.getUrl());
        return R.ok("头像上传成功", profile);
    }

    private String validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "请选择头像图片";
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            return "头像不能超过 5MB";
        }
        String extension = FileUtil.extName(file.getOriginalFilename());
        if (!StringUtils.equalsAnyIgnoreCase(extension, MimeTypeUtils.IMAGE_EXTENSION)) {
            return "头像格式不正确，请上传" + Arrays.toString(MimeTypeUtils.IMAGE_EXTENSION) + "格式";
        }
        String contentType = file.getContentType();
        if (StringUtils.isBlank(contentType)
            || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return "头像文件类型不正确";
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
            if (input == null) {
                return "无法读取头像图片";
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return "文件内容不是有效图片";
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1
                    || width > MAX_AVATAR_DIMENSION || height > MAX_AVATAR_DIMENSION) {
                    return "头像尺寸需在 1–4096 像素之间";
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            return "头像图片读取失败";
        }
        return null;
    }
}
