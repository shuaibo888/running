package org.dromara.running.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.running.domain.RunTrackPoint;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

import java.util.List;

/** 轨迹点 Mapper。 */
public interface RunTrackPointMapper extends BaseMapperPlus<RunTrackPoint, RunTrackPoint> {

    /**
     * 从有效轨迹中等距抽取有限数量候选点，避免为每个定位点调用逆地址解析。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
        WITH ranked AS (
            SELECT id, tenant_id, workout_id, sequence_no, latitude, longitude,
                   ROW_NUMBER() OVER (ORDER BY sequence_no) AS row_no,
                   COUNT(*) OVER () AS total_count
            FROM run_track_point
            WHERE tenant_id = #{tenantId}
              AND workout_id = #{workoutId}
              AND valid_flag = 1
              AND del_flag = 0
        )
        SELECT id, tenant_id, workout_id, sequence_no, latitude, longitude
        FROM ranked
        WHERE row_no = 1
           OR row_no = total_count
           OR MOD(row_no - 1, GREATEST(1, CEIL(total_count / #{maxCandidates}))) = 0
        ORDER BY sequence_no
        """)
    List<RunTrackPoint> selectCityCandidatePoints(@Param("tenantId") String tenantId,
                                                   @Param("workoutId") Long workoutId,
                                                   @Param("maxCandidates") int maxCandidates);
}
