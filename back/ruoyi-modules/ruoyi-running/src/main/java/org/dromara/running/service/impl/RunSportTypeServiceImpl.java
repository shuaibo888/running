package org.dromara.running.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.running.domain.RunSportType;
import org.dromara.running.domain.vo.RunSportTypeVo;
import org.dromara.running.mapper.RunSportTypeMapper;
import org.dromara.running.service.IRunSportTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

/** 从数据库读取启用中的按时长运动类型。 */
@RequiredArgsConstructor
@Service
public class RunSportTypeServiceImpl implements IRunSportTypeService {

    private static final String ENABLED = "0";
    private static final String TRACKING_DURATION = "DURATION";

    private final RunSportTypeMapper sportTypeMapper;

    @Override
    public List<RunSportTypeVo> listDurationTypes() {
        return sportTypeMapper.selectList(Wrappers.<RunSportType>lambdaQuery()
                .eq(RunSportType::getTrackingMode, TRACKING_DURATION)
                .eq(RunSportType::getStatus, ENABLED)
                .orderByAsc(RunSportType::getSortOrder)
                .orderByAsc(RunSportType::getId))
            .stream()
            .map(this::toVo)
            .toList();
    }

    private RunSportTypeVo toVo(RunSportType type) {
        RunSportTypeVo vo = new RunSportTypeVo();
        vo.setSportCode(type.getSportCode());
        vo.setSportName(type.getSportName());
        vo.setTrackingMode(type.getTrackingMode());
        vo.setMetValue(type.getMetValue());
        vo.setCalorieAlgorithm(type.getCalorieAlgorithm());
        vo.setMinDurationMinutes(type.getMinDurationMinutes());
        vo.setMaxDurationMinutes(type.getMaxDurationMinutes());
        return vo;
    }
}
