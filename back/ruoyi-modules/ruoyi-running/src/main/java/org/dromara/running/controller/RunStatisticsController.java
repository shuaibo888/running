package org.dromara.running.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.running.domain.vo.RunWorkoutStatisticsVo;
import org.dromara.running.domain.vo.RunWorkoutTrendVo;
import org.dromara.running.service.IRunWorkoutService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

/** 小程序个人累计运动数据。 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/statistics")
public class RunStatisticsController {

    private final IRunWorkoutService workoutService;

    @GetMapping("/overview")
    public R<RunWorkoutStatisticsVo> overview() {
        return R.ok(workoutService.queryStatistics(LoginHelper.getUserId()));
    }

    @GetMapping("/trends")
    public R<RunWorkoutTrendVo> trends(@RequestParam(defaultValue = "7") int days) {
        return R.ok(workoutService.queryDailyTrends(LoginHelper.getUserId(), days));
    }
}
