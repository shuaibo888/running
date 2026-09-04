package org.dromara.running.domain.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 开始跑步参数。
 */
@Data
public class RunWorkoutStartBo {

    @NotBlank(message = "客户端运动ID不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{8,64}$", message = "客户端运动ID格式不正确")
    private String clientWorkoutId;

    @Valid
    @NotNull(message = "起点不能为空")
    private RunTrackPointBo initialPoint;
}
