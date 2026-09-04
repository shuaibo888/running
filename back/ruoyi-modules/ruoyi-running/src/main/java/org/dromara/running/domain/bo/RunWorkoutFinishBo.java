package org.dromara.running.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 结束运动参数。
 */
@Data
public class RunWorkoutFinishBo {

    @NotBlank(message = "结束请求ID不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{8,64}$", message = "结束请求ID格式不正确")
    private String clientFinishId;
}
