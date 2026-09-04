package org.dromara.running.service;

import org.dromara.running.domain.vo.RunRankingVo;

/** 城市/省份卡路里排行榜。 */
public interface IRunRankingService {
    RunRankingVo getRanking(Long userId, String scope, String period);
}
