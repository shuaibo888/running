package org.dromara.running.domain.bo;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 客户端采集的 GCJ-02 轨迹点。
 */
@Data
public class RunTrackPointBo {

    @NotNull(message = "轨迹序号不能为空")
    @Min(value = 0, message = "轨迹序号不能小于0")
    private Integer sequenceNo;

    @NotNull(message = "定位时间不能为空")
    private Date recordedAt;

    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度不能小于-90")
    @DecimalMax(value = "90.0", message = "纬度不能大于90")
    private BigDecimal latitude;

    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度不能小于-180")
    @DecimalMax(value = "180.0", message = "经度不能大于180")
    private BigDecimal longitude;

    @DecimalMin(value = "0.0", message = "定位精度不能小于0")
    @DecimalMax(value = "10000.0", message = "定位精度不能大于10000米")
    private BigDecimal accuracyMeters;

    @DecimalMin(value = "0.0", message = "设备速度不能小于0")
    @DecimalMax(value = "200.0", message = "设备速度不能大于200米每秒")
    private BigDecimal reportedSpeedMps;

    @DecimalMin(value = "-1000.0", message = "海拔不能小于-1000米")
    @DecimalMax(value = "10000.0", message = "海拔不能大于10000米")
    private BigDecimal altitudeMeters;

    @DecimalMin(value = "0.0", message = "方向不能小于0度")
    @DecimalMax(value = "360.0", message = "方向不能大于360度")
    private BigDecimal directionDegrees;
}
