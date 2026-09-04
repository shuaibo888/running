package org.dromara.running.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 小程序积分流水。 */
@Data
public class RunPointTransactionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String bizType;
    private Integer deltaPoints;
    private Integer balanceAfter;
    private String title;
    private String description;
    private Date occurredAt;
}
