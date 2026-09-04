package org.dromara.running.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.running.domain.vo.RunSportTypeVo;
import org.dromara.running.service.IRunSportTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 小程序非跑步运动类型接口。 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/sport-types")
public class RunSportTypeController {

    private final IRunSportTypeService sportTypeService;

    @GetMapping
    public R<List<RunSportTypeVo>> list() {
        return R.ok(sportTypeService.listDurationTypes());
    }
}
