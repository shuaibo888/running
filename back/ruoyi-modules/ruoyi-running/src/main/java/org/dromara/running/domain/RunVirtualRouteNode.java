package org.dromara.running.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;

/** 虚拟文化线路节点。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("run_virtual_route_node")
public class RunVirtualRouteNode extends TenantEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("id")
    private Long id;
    private Long routeId;
    private String nodeCode;
    private String nodeName;
    private BigDecimal thresholdDistanceMeters;
    private String storyTitle;
    private String storyContent;
    private String medalName;
    private Integer rewardPoints;
    private Integer sortOrder;
    @TableLogic
    private Long delFlag;
}
