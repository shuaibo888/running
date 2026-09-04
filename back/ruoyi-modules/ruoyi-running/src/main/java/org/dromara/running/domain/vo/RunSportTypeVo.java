package org.dromara.running.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/** 小程序可选运动类型。 */
@Data
public class RunSportTypeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sportCode;
    private String sportName;
    private String trackingMode;
    private BigDecimal metValue;
    private String calorieAlgorithm;
    private Integer minDurationMinutes;
    private Integer maxDurationMinutes;
}
