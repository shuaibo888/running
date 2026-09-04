package org.dromara.running.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** 用户城市足迹总览。 */
@Data
@AllArgsConstructor
public class RunCityFootprintSummaryVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int cityCount;
    private long pendingWorkoutCount;
    private long failedWorkoutCount;
    private List<RunCityFootprintVo> cities;
}
