package org.dromara.running.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/** 地区卡路里排行榜响应。 */
@Data
public class RunRankingVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String scope;
    private String period;
    private String regionCode;
    private String regionName;
    private Date periodStartedAt;
    private Date generatedAt;
    private String tieBreakRule;
    private String regionRule;
    private List<RunRankingEntryVo> entries;
    private RunRankingEntryVo currentUserEntry;
}
