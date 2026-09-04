package org.dromara.running.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;

/** 虚拟文化线路。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("run_virtual_route")
public class RunVirtualRoute extends TenantEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;
    private String routeCode;
    private String routeName;
    private String subtitle;
    private String description;
    private String startCity;
    private String endCity;
    private BigDecimal totalDistanceMeters;
    private String themeColor;
    private String coverUrl;
    private String status;
    private Integer sortOrder;
    @TableLogic
    private Long delFlag;
}
