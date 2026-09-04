package org.dromara.running.service;

import org.dromara.running.domain.vo.RunReverseGeocodeVo;

/**
 * 地图与行政区解析服务。
 */
public interface IRunMapService {

    RunReverseGeocodeVo reverseGeocode(double latitude, double longitude);
}
