package org.dromara.running.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 可供用户本人回放的有效轨迹点。 */
@Data
public class RunTrackPointVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer sequenceNo;
    private Date recordedAt;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
