package org.dromara.running.service;

import org.dromara.running.domain.RunVirtualRouteNode;
import org.dromara.running.domain.vo.RunPointOverviewVo;

/** 用户积分账户与奖励服务。 */
public interface IRunPointService {

    RunPointOverviewVo getOverview(Long userId);

    RunPointOverviewVo checkIn(Long userId);

    void awardRouteNode(Long userId, Long workoutId, Long routeId, RunVirtualRouteNode node);

    void awardAchievement(Long userId, Long workoutId, Long achievementId,
                          String achievementName, Integer rewardPoints);
}
