package org.dromara.running.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.running.domain.vo.RunRankingVo;
import org.dromara.running.service.IRunRankingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 小程序地区卡路里排行榜。 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/rankings")
public class RunRankingController {

    private final IRunRankingService rankingService;

    @GetMapping
    public R<RunRankingVo> ranking(
        @RequestParam(defaultValue = "CITY") String scope,
        @RequestParam(defaultValue = "WEEK") String period) {
        return R.ok(rankingService.getRanking(LoginHelper.getUserId(), scope, period));
    }
}
