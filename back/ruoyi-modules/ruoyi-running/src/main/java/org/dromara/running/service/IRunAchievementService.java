package org.dromara.running.service;

import org.dromara.running.domain.vo.RunAchievementVo;

import java.util.List;

/** 用户里程碑成就服务。 */
public interface IRunAchievementService {
    List<RunAchievementVo> listAchievements(Long userId);
    void evaluateAfterWorkout(Long userId, Long workoutId);
    void evaluateAfterCityFootprint(Long userId, Long workoutId);
}
