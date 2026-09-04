package org.dromara.running.domain.enums;

/**
 * 服务端运动状态。
 */
public enum RunWorkoutStatus {
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED;

    public boolean isActive() {
        return this == RUNNING || this == PAUSED;
    }
}
