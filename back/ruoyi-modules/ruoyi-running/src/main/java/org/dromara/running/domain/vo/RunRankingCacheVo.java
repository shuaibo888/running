package org.dromara.running.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/** Redis 中短期缓存的地区榜 Top 100。 */
@Data
public class RunRankingCacheVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Date generatedAt;
    private List<RunRankingEntryVo> entries;
}
