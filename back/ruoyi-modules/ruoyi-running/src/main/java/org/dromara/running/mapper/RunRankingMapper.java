package org.dromara.running.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.running.domain.vo.RunRankingEntryVo;

import java.util.Date;
import java.util.List;

/** 地区排行榜数据库聚合查询。 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface RunRankingMapper {

    @Select("""
        <script>
        WITH totals AS (
            SELECT p.user_id,
                   COALESCE(NULLIF(p.nickname, ''), CONCAT('燃赛跑者', RIGHT(CAST(p.user_id AS CHAR), 4))) AS nickname,
                   p.avatar_url,
                   SUM(w.calories_kcal) AS calories_kcal,
                   SUM(w.distance_meters) AS distance_meters,
                   COUNT(*) AS workout_count
            FROM run_user_profile p
            INNER JOIN run_workout w
              ON w.tenant_id = p.tenant_id
             AND w.user_id = p.user_id
             AND w.status = 'COMPLETED'
             AND w.ranking_eligible = 1
             AND w.del_flag = 0
            WHERE p.tenant_id = #{tenantId}
              AND p.del_flag = 0
            <choose>
              <when test="scope == 'CITY'">AND w.ranking_city_code = #{regionCode}</when>
              <otherwise>AND w.ranking_province_code = #{regionCode}</otherwise>
            </choose>
            <if test="startAt != null">AND w.finished_at &gt;= #{startAt}</if>
            GROUP BY p.user_id, p.nickname, p.avatar_url
        ), ranked AS (
            SELECT ROW_NUMBER() OVER (
                       ORDER BY calories_kcal DESC, distance_meters DESC, workout_count DESC, user_id ASC
                   ) AS rank_no,
                   user_id, nickname, avatar_url, calories_kcal, distance_meters, workout_count
            FROM totals
        )
        SELECT rank_no, user_id, nickname, avatar_url, calories_kcal, distance_meters, workout_count
        FROM ranked
        ORDER BY rank_no
        LIMIT 100
        </script>
        """)
    List<RunRankingEntryVo> selectTop(
        @Param("tenantId") String tenantId,
        @Param("scope") String scope,
        @Param("regionCode") String regionCode,
        @Param("startAt") Date startAt);

    @Select("""
        <script>
        WITH totals AS (
            SELECT p.user_id,
                   COALESCE(NULLIF(p.nickname, ''), CONCAT('燃赛跑者', RIGHT(CAST(p.user_id AS CHAR), 4))) AS nickname,
                   p.avatar_url,
                   SUM(w.calories_kcal) AS calories_kcal,
                   SUM(w.distance_meters) AS distance_meters,
                   COUNT(*) AS workout_count
            FROM run_user_profile p
            INNER JOIN run_workout w
              ON w.tenant_id = p.tenant_id
             AND w.user_id = p.user_id
             AND w.status = 'COMPLETED'
             AND w.ranking_eligible = 1
             AND w.del_flag = 0
            WHERE p.tenant_id = #{tenantId}
              AND p.del_flag = 0
            <choose>
              <when test="scope == 'CITY'">AND w.ranking_city_code = #{regionCode}</when>
              <otherwise>AND w.ranking_province_code = #{regionCode}</otherwise>
            </choose>
            <if test="startAt != null">AND w.finished_at &gt;= #{startAt}</if>
            GROUP BY p.user_id, p.nickname, p.avatar_url
        ), ranked AS (
            SELECT ROW_NUMBER() OVER (
                       ORDER BY calories_kcal DESC, distance_meters DESC, workout_count DESC, user_id ASC
                   ) AS rank_no,
                   user_id, nickname, avatar_url, calories_kcal, distance_meters, workout_count
            FROM totals
        )
        SELECT rank_no, user_id, nickname, avatar_url, calories_kcal, distance_meters, workout_count
        FROM ranked
        WHERE user_id = #{userId}
        </script>
        """)
    RunRankingEntryVo selectCurrentUser(
        @Param("tenantId") String tenantId,
        @Param("scope") String scope,
        @Param("regionCode") String regionCode,
        @Param("startAt") Date startAt,
        @Param("userId") Long userId);
}
