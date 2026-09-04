package org.dromara.running.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 切换当前虚拟文化线路。 */
@Data
public class RunCurrentRouteBo {
    @NotNull(message = "线路ID不能为空")
    private Long routeId;
}
