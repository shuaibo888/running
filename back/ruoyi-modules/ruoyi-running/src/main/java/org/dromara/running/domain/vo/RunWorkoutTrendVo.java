package org.dromara.running.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** 连续自然日运动趋势，缺失日期由服务端补零。 */
@Data
public class RunWorkoutTrendVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer days;
    private String fromDate;
    private String toDate;
    private List<RunWorkoutTrendPointVo> points;
}
