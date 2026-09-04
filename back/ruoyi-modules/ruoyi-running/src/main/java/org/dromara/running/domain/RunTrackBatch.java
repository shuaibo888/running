package org.dromara.running.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 客户端轨迹上传批次。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("run_track_batch")
public class RunTrackBatch extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;
    private Long workoutId;
    private Long userId;
    private String clientBatchId;
    private Integer startSequence;
    private Integer endSequence;
    private Integer pointCount;
    private Integer invalidPointCount;
    private BigDecimal distanceMeters;

    @TableLogic
    private Long delFlag;
}
