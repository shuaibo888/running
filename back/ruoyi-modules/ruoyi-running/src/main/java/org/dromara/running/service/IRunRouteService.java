package org.dromara.running.service;

import org.dromara.running.domain.RunWorkout;
import org.dromara.running.domain.vo.RunRouteVo;

import java.util.List;

/** 虚拟文化线路服务。 */
public interface IRunRouteService {
    List<RunRouteVo> listRoutes(Long userId);
    RunRouteVo getRoute(Long userId, Long routeId);
    RunRouteVo selectCurrentRoute(Long userId, Long routeId);
    Long resolveSelectedRouteId(Long userId);
    void applyCompletedWorkout(Long userId, RunWorkout workout);
}
