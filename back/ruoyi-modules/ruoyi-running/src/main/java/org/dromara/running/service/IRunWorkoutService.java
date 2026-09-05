package org.dromara.running.service;

import org.dromara.running.domain.bo.RunTrackBatchBo;
import org.dromara.running.domain.bo.RunManualWorkoutBo;
import org.dromara.running.domain.bo.RunWorkoutFinishBo;
import org.dromara.running.domain.bo.RunWorkoutStartBo;
import org.dromara.running.domain.vo.RunWorkoutVo;
import org.dromara.running.domain.vo.RunTrackPointVo;
import org.dromara.running.domain.vo.RunWorkoutStatisticsVo;
import org.dromara.running.domain.vo.RunWorkoutTrendVo;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;

/**
 * 跑步记录核心服务。
 */
public interface IRunWorkoutService {

    RunWorkoutVo start(Long userId, RunWorkoutStartBo bo);

    RunWorkoutVo recordManual(Long userId, RunManualWorkoutBo bo);

    RunWorkoutVo queryActive(Long userId);

    RunWorkoutVo queryById(Long userId, Long workoutId);

    TableDataInfo<RunWorkoutVo> queryHistory(Long userId, int pageNum, int pageSize);

    List<RunTrackPointVo> queryTrackPoints(Long userId, Long workoutId, int afterSequence, int limit);

    RunWorkoutStatisticsVo queryStatistics(Long userId);

    RunWorkoutTrendVo queryDailyTrends(Long userId, int days);

    RunWorkoutVo appendTrackBatch(Long userId, Long workoutId, RunTrackBatchBo bo);

    RunWorkoutVo pause(Long userId, Long workoutId);

    RunWorkoutVo resume(Long userId, Long workoutId);

    RunWorkoutVo abandon(Long userId, Long workoutId);

    RunWorkoutVo finish(Long userId, Long workoutId, RunWorkoutFinishBo bo);
}
