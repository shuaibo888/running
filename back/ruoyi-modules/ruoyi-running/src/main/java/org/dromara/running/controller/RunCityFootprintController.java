package org.dromara.running.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.running.domain.vo.RunCityFootprintSummaryVo;
import org.dromara.running.service.IRunCityFootprintService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 小程序城市运动足迹接口。 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/city-footprints")
public class RunCityFootprintController {

    private final IRunCityFootprintService footprintService;

    @GetMapping
    public R<RunCityFootprintSummaryVo> summary() {
        return R.ok(footprintService.querySummary(LoginHelper.getUserId()));
    }

    @PostMapping("/retry")
    public R<Integer> retry() {
        int count = footprintService.retryFailed(LoginHelper.getUserId());
        return R.ok(count > 0 ? "已重新加入解析队列" : "没有需要重试的运动", count);
    }
}
