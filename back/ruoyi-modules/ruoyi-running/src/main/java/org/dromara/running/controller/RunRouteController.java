package org.dromara.running.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.running.domain.bo.RunCurrentRouteBo;
import org.dromara.running.domain.vo.RunRouteVo;
import org.dromara.running.service.IRunRouteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 小程序虚拟文化线路。 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/routes")
public class RunRouteController {

    private final IRunRouteService routeService;

    @GetMapping
    public R<List<RunRouteVo>> list() {
        return R.ok(routeService.listRoutes(LoginHelper.getUserId()));
    }

    @GetMapping("/{routeId}")
    public R<RunRouteVo> detail(@PathVariable Long routeId) {
        return R.ok(routeService.getRoute(LoginHelper.getUserId(), routeId));
    }

    @PutMapping("/current")
    public R<RunRouteVo> selectCurrent(@Valid @RequestBody RunCurrentRouteBo bo) {
        return R.ok("当前线路已切换",
            routeService.selectCurrentRoute(LoginHelper.getUserId(), bo.getRouteId()));
    }
}
