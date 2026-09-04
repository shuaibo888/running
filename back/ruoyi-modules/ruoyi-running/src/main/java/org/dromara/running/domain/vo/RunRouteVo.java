package org.dromara.running.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/** 虚拟文化线路与用户真实进度。 */
@Data
public class RunRouteVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

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
    private BigDecimal accumulatedDistanceMeters;
    private BigDecimal progressPercent;
    private Integer reachedNodeOrder;
    private Boolean selected;
    private List<RunRouteNodeVo> nodes;
}
