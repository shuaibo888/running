package org.dromara.running.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/** 用户累计城市运动足迹。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("run_city_footprint")
public class RunCityFootprint extends TenantEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;
    private Long userId;
    private String provinceCode;
    private String provinceName;
    private String cityCode;
    private String cityName;
    private Long firstWorkoutId;
    private Date firstReachedAt;
    private Long lastWorkoutId;
    private Date lastReachedAt;
    private Integer workoutCount;

    @Version
    private Long version;

    @TableLogic
    private Long delFlag;
}
