package org.dromara.running.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/** 排行榜单项；平台用户ID仅用于服务端识别本人，不向前端暴露。 */
@Data
public class RunRankingEntryVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long rankNo;
    @JsonIgnore
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private BigDecimal caloriesKcal;
    private BigDecimal distanceMeters;
    private Long workoutCount;
    private Boolean currentUser;
}
