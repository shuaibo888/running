package org.dromara.running.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 城市足迹展示项。 */
@Data
public class RunCityFootprintVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String provinceCode;
    private String provinceName;
    private String cityCode;
    private String cityName;
    private Date firstReachedAt;
    private Date lastReachedAt;
    private Integer workoutCount;
}
