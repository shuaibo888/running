package org.dromara.running.service;

import org.dromara.running.domain.vo.RunSportTypeVo;

import java.util.List;

/** 小程序运动类型服务。 */
public interface IRunSportTypeService {

    List<RunSportTypeVo> listDurationTypes();
}
