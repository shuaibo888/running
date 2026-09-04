package org.dromara.running.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.running.domain.RunWorkout;
import org.dromara.running.domain.vo.RunWorkoutStatisticsVo;
import org.dromara.running.domain.vo.RunWorkoutTrendPointVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/** 运动记录 Mapper。 */
public interface RunWorkoutMapper extends BaseMapperPlus<RunWorkout, RunWorkout> {

    @Select("""
        SELECT COUNT(*) AS workout_count,
               COALESCE(SUM(elapsed_seconds), 0) AS total_elapsed_seconds,
               COALESCE(SUM(distance_meters), 0) AS total_distance_meters,
               COALESCE(SUM(calories_kcal), 0) AS total_calories_kcal,
               MIN(started_at) AS first_workout_at,
               MAX(finished_at) AS last_workout_at
        FROM run_workout
        WHERE user_id = #{userId}
          AND status = 'COMPLETED'
          AND del_flag = 0
        """)
    RunWorkoutStatisticsVo selectCompletedStatistics(@Param("userId") Long userId);

    @Select("""
        SELECT DATE_FORMAT(finished_at, '%Y-%m-%d') AS trendDate,
               COUNT(*) AS workoutCount,
               COALESCE(SUM(elapsed_seconds), 0) AS totalElapsedSeconds,
               COALESCE(SUM(distance_meters), 0) AS totalDistanceMeters,
               COALESCE(SUM(calories_kcal), 0) AS totalCaloriesKcal
        FROM run_workout
        WHERE user_id = #{userId}
          AND status = 'COMPLETED'
          AND finished_at >= #{from}
          AND finished_at < #{to}
          AND del_flag = 0
        GROUP BY DATE_FORMAT(finished_at, '%Y-%m-%d')
        ORDER BY DATE_FORMAT(finished_at, '%Y-%m-%d')
        """)
    List<RunWorkoutTrendPointVo> selectDailyTrends(@Param("userId") Long userId,
                                                    @Param("from") Date from,
                                                    @Param("to") Date to);

    @Select("""
        SELECT COALESCE(MAX(streak_days), 0)
        FROM (
            SELECT COUNT(*) AS streak_days
            FROM (
                SELECT workout_date,
                       TO_DAYS(workout_date) - ROW_NUMBER() OVER (ORDER BY workout_date) AS streak_group
                FROM (
                    SELECT DISTINCT DATE(finished_at) AS workout_date
                    FROM run_workout
                    WHERE user_id = #{userId}
                      AND status = 'COMPLETED'
                      AND del_flag = 0
                ) daily_workouts
            ) sequenced_days
            GROUP BY streak_group
        ) streaks
        """)
    Long selectLongestConsecutiveDays(@Param("userId") Long userId);

    @Select("""
        SELECT COUNT(*)
        FROM run_city_footprint
        WHERE user_id = #{userId}
          AND del_flag = 0
        """)
    Long selectCityCount(@Param("userId") Long userId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
        SELECT *
        FROM run_workout
        WHERE status = 'COMPLETED'
          AND del_flag = 0
          AND (
              (city_resolve_status IN ('PENDING', 'RETRY')
               AND (city_resolve_next_at IS NULL OR city_resolve_next_at <= #{now}))
              OR (city_resolve_status = 'PROCESSING' AND update_time < #{staleBefore})
          )
        ORDER BY COALESCE(city_resolve_next_at, finished_at), id
        LIMIT #{limit}
        """)
    List<RunWorkout> selectPendingCityResolutions(@Param("now") Date now,
                                                   @Param("staleBefore") Date staleBefore,
                                                   @Param("limit") int limit);

    @InterceptorIgnore(tenantLine = "true")
    @Update("""
        UPDATE run_workout
        SET city_resolve_status = 'PROCESSING',
            city_resolve_attempts = city_resolve_attempts + 1,
            city_resolve_next_at = NULL,
            city_resolve_error = NULL,
            update_time = #{now}
        WHERE tenant_id = #{tenantId}
          AND id = #{workoutId}
          AND status = 'COMPLETED'
          AND del_flag = 0
          AND (
              (city_resolve_status IN ('PENDING', 'RETRY')
               AND (city_resolve_next_at IS NULL OR city_resolve_next_at <= #{now}))
              OR (city_resolve_status = 'PROCESSING' AND update_time < #{staleBefore})
          )
        """)
    int claimCityResolution(@Param("tenantId") String tenantId,
                            @Param("workoutId") Long workoutId,
                            @Param("now") Date now,
                            @Param("staleBefore") Date staleBefore);

    @InterceptorIgnore(tenantLine = "true")
    @Update("""
        UPDATE run_workout
        SET city_resolve_status = 'COMPLETED', city_resolved_at = #{resolvedAt},
            city_resolve_next_at = NULL, city_resolve_error = NULL, update_time = #{resolvedAt}
        WHERE tenant_id = #{tenantId} AND id = #{workoutId}
          AND city_resolve_status = 'PROCESSING' AND del_flag = 0
        """)
    int completeCityResolution(@Param("tenantId") String tenantId,
                               @Param("workoutId") Long workoutId,
                               @Param("resolvedAt") Date resolvedAt);

    @InterceptorIgnore(tenantLine = "true")
    @Update("""
        UPDATE run_workout
        SET city_resolve_status = #{status}, city_resolve_next_at = #{nextAt},
            city_resolve_error = #{errorCode}, update_time = #{now}
        WHERE tenant_id = #{tenantId} AND id = #{workoutId}
          AND city_resolve_status = 'PROCESSING' AND del_flag = 0
        """)
    int failCityResolution(@Param("tenantId") String tenantId,
                           @Param("workoutId") Long workoutId,
                           @Param("status") String status,
                           @Param("nextAt") Date nextAt,
                           @Param("errorCode") String errorCode,
                           @Param("now") Date now);

    @Update("""
        UPDATE run_workout
        SET city_resolve_status = 'RETRY', city_resolve_attempts = 0,
            city_resolve_next_at = NULL, city_resolve_error = NULL, update_time = NOW()
        WHERE user_id = #{userId} AND status = 'COMPLETED'
          AND city_resolve_status = 'FAILED' AND del_flag = 0
        """)
    int retryFailedCityResolutions(@Param("userId") Long userId);
}
