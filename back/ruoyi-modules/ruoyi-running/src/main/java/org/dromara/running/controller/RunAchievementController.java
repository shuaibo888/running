package org.dromara.running.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.running.domain.vo.RunAchievementVo;
import org.dromara.running.service.IRunAchievementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 小程序里程碑成就。 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/achievements")
public class RunAchievementController {

    private final IRunAchievementService achievementService;

    @GetMapping
    public R<List<RunAchievementVo>> list() {
        return R.ok(achievementService.listAchievements(LoginHelper.getUserId()));
    }
}
