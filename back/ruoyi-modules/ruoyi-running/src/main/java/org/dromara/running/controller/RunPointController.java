package org.dromara.running.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.running.domain.vo.RunPointOverviewVo;
import org.dromara.running.service.IRunPointService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 小程序积分首页与签到接口。 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/points")
public class RunPointController {

    private final IRunPointService pointService;

    @GetMapping
    public R<RunPointOverviewVo> overview() {
        return R.ok(pointService.getOverview(LoginHelper.getUserId()));
    }

    @RepeatSubmit(interval = 1000, message = "请勿重复签到")
    @PostMapping("/check-in")
    public R<RunPointOverviewVo> checkIn() {
        return R.ok("签到成功", pointService.checkIn(LoginHelper.getUserId()));
    }
}
