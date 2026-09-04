package org.dromara.running.event;

/** 运动事务提交后触发城市足迹解析。 */
public record RunWorkoutCompletedEvent(String tenantId, Long workoutId) {
}
