package org.dromara.running.domain.bo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 提交一笔按时长结算的非跑步运动。 */
@Data
public class RunManualWorkoutBo {

    @NotBlank(message = "客户端运动ID不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{8,64}$", message = "客户端运动ID格式不正确")
    private String clientWorkoutId;

    @NotBlank(message = "请选择运动类型")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "运动类型格式不正确")
    private String sportType;

    @NotNull(message = "运动时长不能为空")
    @Min(value = 5, message = "运动时长不能少于5分钟")
    @Max(value = 600, message = "运动时长不能超过600分钟")
    private Integer durationMinutes;
}
