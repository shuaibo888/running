package org.dromara.running.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.common.ratelimiter.enums.LimitType;
import org.dromara.running.domain.bo.RunReverseGeocodeBo;
import org.dromara.running.domain.vo.RunReverseGeocodeVo;
import org.dromara.running.service.IRunMapService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序地图能力接口。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/app/map")
public class RunMapController {

    private final IRunMapService mapService;

    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP)
    @PostMapping("/reverse-geocode")
    public R<RunReverseGeocodeVo> reverseGeocode(@Validated @RequestBody RunReverseGeocodeBo bo) {
        return R.ok(mapService.reverseGeocode(bo.getLatitude(), bo.getLongitude()));
    }
}
