package org.dromara.running.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** 用户积分首页数据。 */
@Data
public class RunPointOverviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer balance;
    private Integer totalEarned;
    private Integer currentCheckinStreak;
    private Boolean checkedInToday;
    private Integer todayCheckinReward;
    private List<RunPointTransactionVo> recentTransactions;
}
