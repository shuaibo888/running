package org.dromara.running.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 单个运动轨迹点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("run_track_point")
public class RunTrackPoint extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;
    private Long workoutId;
    private Long batchId;
    private Integer sequenceNo;
    private Date recordedAt;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal accuracyMeters;
    private BigDecimal reportedSpeedMps;
    private BigDecimal altitudeMeters;
    private BigDecimal directionDegrees;
    private Boolean validFlag;
    private String invalidReason;
    private BigDecimal segmentDistanceMeters;

    @TableLogic
    private Long delFlag;
}
