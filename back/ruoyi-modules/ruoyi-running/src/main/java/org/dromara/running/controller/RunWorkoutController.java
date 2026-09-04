package org.dromara.running.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.common.ratelimiter.enums.LimitType;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.running.domain.bo.RunTrackBatchBo;
import org.dromara.running.domain.bo.RunManualWorkoutBo;
import org.dromara.running.domain.bo.RunWorkoutFinishBo;
import org.dromara.running.domain.bo.RunWorkoutStartBo;
import org.dromara.running.domain.vo.RunWorkoutVo;
import org.dromara.running.domain.vo.RunTrackPointVo;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.running.service.IRunWorkoutService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序跑步记录核心接口。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/workouts")
public class RunWorkoutController {

    private final IRunWorkoutService workoutService;

    @PostMapping
    public R<RunWorkoutVo> start(@Valid @RequestBody RunWorkoutStartBo bo) {
        return R.ok("运动已开始", workoutService.start(LoginHelper.getUserId(), bo));
    }

    @PostMapping("/manual")
    public R<RunWorkoutVo> recordManual(@Valid @RequestBody RunManualWorkoutBo bo) {
        return R.ok("运动已记录", workoutService.recordManual(LoginHelper.getUserId(), bo));
    }

    @GetMapping("/active")
    public R<RunWorkoutVo> active() {
        return R.ok(workoutService.queryActive(LoginHelper.getUserId()));
    }

    @GetMapping("/{workoutId}")
    public R<RunWorkoutVo> detail(@PathVariable Long workoutId) {
        return R.ok(workoutService.queryById(LoginHelper.getUserId(), workoutId));
    }

    @GetMapping
    public TableDataInfo<RunWorkoutVo> history(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return workoutService.queryHistory(LoginHelper.getUserId(), pageNum, pageSize);
    }

    @GetMapping("/{workoutId}/track-points")
    public R<List<RunTrackPointVo>> trackPoints(
        @PathVariable Long workoutId,
        @RequestParam(defaultValue = "-1") int afterSequence,
        @RequestParam(defaultValue = "500") int limit) {
        return R.ok(workoutService.queryTrackPoints(
            LoginHelper.getUserId(), workoutId, afterSequence, limit));
    }

    @RateLimiter(time = 60, count = 120, limitType = LimitType.IP)
    @PostMapping("/{workoutId}/track-batches")
    public R<RunWorkoutVo> appendTrackBatch(@PathVariable Long workoutId,
                                             @Valid @RequestBody RunTrackBatchBo bo) {
        return R.ok(workoutService.appendTrackBatch(LoginHelper.getUserId(), workoutId, bo));
    }

    @PostMapping("/{workoutId}/pause")
    public R<RunWorkoutVo> pause(@PathVariable Long workoutId) {
        return R.ok("运动已暂停", workoutService.pause(LoginHelper.getUserId(), workoutId));
    }

    @PostMapping("/{workoutId}/resume")
    public R<RunWorkoutVo> resume(@PathVariable Long workoutId) {
        return R.ok("运动已继续", workoutService.resume(LoginHelper.getUserId(), workoutId));
    }

    @PostMapping("/{workoutId}/finish")
    public R<RunWorkoutVo> finish(@PathVariable Long workoutId,
                                   @Valid @RequestBody RunWorkoutFinishBo bo) {
        return R.ok("运动已完成", workoutService.finish(LoginHelper.getUserId(), workoutId, bo));
    }
}
