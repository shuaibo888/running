package org.dromara.running.service;

import org.dromara.running.domain.vo.RunCityFootprintSummaryVo;

/** 城市足迹异步解析与查询服务。 */
public interface IRunCityFootprintService {

    RunCityFootprintSummaryVo querySummary(Long userId);

    int retryFailed(Long userId);

    void publishCompletedWorkout(Long workoutId);

    void process(String tenantId, Long workoutId);

    void processPending();
}
