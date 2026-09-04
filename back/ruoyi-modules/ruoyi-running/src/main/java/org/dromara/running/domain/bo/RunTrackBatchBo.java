package org.dromara.running.domain.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 轨迹批量上传参数。
 */
@Data
public class RunTrackBatchBo {

    @NotBlank(message = "客户端批次ID不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{8,64}$", message = "客户端批次ID格式不正确")
    private String clientBatchId;

    @Valid
    @NotEmpty(message = "轨迹点不能为空")
    @Size(max = 100, message = "单批轨迹点不能超过100个")
    private List<RunTrackPointBo> points;
}
