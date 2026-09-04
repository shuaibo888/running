package org.dromara.running.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/** 文化线路节点及当前用户解锁状态。 */
@Data
public class RunRouteNodeVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String nodeCode;
    private String nodeName;
    private BigDecimal thresholdDistanceMeters;
    private String storyTitle;
    private String storyContent;
    private String medalName;
    private Integer rewardPoints;
    private Integer sortOrder;
    private Boolean unlocked;
}
