package org.dromara.running.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.running.domain.RunCityFootprint;
import org.dromara.running.domain.RunTrackPoint;
import org.dromara.running.domain.RunWorkout;
import org.dromara.running.domain.RunWorkoutCity;
import org.dromara.running.domain.vo.RunCityFootprintSummaryVo;
import org.dromara.running.domain.vo.RunCityFootprintVo;
import org.dromara.running.domain.vo.RunReverseGeocodeVo;
import org.dromara.running.event.RunWorkoutCompletedEvent;
import org.dromara.running.mapper.RunCityFootprintMapper;
import org.dromara.running.mapper.RunTrackPointMapper;
import org.dromara.running.mapper.RunWorkoutCityMapper;
import org.dromara.running.mapper.RunWorkoutMapper;
import org.dromara.running.service.IRunCityFootprintService;
import org.dromara.running.service.IRunAchievementService;
import org.dromara.running.service.IRunMapService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 有效轨迹抽样、腾讯逆地址解析、足迹幂等落库与失败重试。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RunCityFootprintServiceImpl implements IRunCityFootprintService {

    private static final int MAX_CANDIDATE_POINTS = 24;
    private static final int MAX_ATTEMPTS = 8;
    private static final int PENDING_BATCH_SIZE = 20;
    private static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(10);
    private static final long[] RETRY_MINUTES = {1, 5, 15, 30, 60, 180, 360};

    private final RunWorkoutMapper workoutMapper;
    private final RunTrackPointMapper trackPointMapper;
    private final RunWorkoutCityMapper workoutCityMapper;
    private final RunCityFootprintMapper footprintMapper;
    private final IRunMapService mapService;
    private final IRunAchievementService achievementService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    @Override
    public RunCityFootprintSummaryVo querySummary(Long userId) {
        List<RunCityFootprintVo> cities = footprintMapper.selectList(
                Wrappers.<RunCityFootprint>lambdaQuery()
                    .eq(RunCityFootprint::getUserId, userId)
                    .orderByDesc(RunCityFootprint::getLastReachedAt)
                    .orderByAsc(RunCityFootprint::getCityCode))
            .stream().map(this::toVo).toList();
        long pending = workoutMapper.selectCount(Wrappers.<RunWorkout>lambdaQuery()
            .eq(RunWorkout::getUserId, userId)
            .in(RunWorkout::getCityResolveStatus, "PENDING", "PROCESSING", "RETRY"));
        long failed = workoutMapper.selectCount(Wrappers.<RunWorkout>lambdaQuery()
            .eq(RunWorkout::getUserId, userId)
            .eq(RunWorkout::getCityResolveStatus, "FAILED"));
        return new RunCityFootprintSummaryVo(cities.size(), pending, failed, cities);
    }

    @Override
    public int retryFailed(Long userId) {
        return workoutMapper.retryFailedCityResolutions(userId);
    }

    @Override
    public void publishCompletedWorkout(Long workoutId) {
        eventPublisher.publishEvent(new RunWorkoutCompletedEvent(TenantHelper.getTenantId(), workoutId));
    }

    @Override
    public void process(String tenantId, Long workoutId) {
        if (StringUtils.isBlank(tenantId) || workoutId == null) {
            return;
        }
        Date now = new Date();
        Date staleBefore = new Date(now.getTime() - PROCESSING_TIMEOUT.toMillis());
        if (workoutMapper.claimCityResolution(tenantId, workoutId, now, staleBefore) < 1) {
            return;
        }

        RunWorkout workout = TenantHelper.dynamic(tenantId, () -> workoutMapper.selectById(workoutId));
        try {
            if (workout == null) {
                throw new ServiceException("运动记录不存在");
            }
            List<RunTrackPoint> candidates = trackPointMapper.selectCityCandidatePoints(
                tenantId, workoutId, MAX_CANDIDATE_POINTS);
            Map<String, ResolvedCity> cities = resolveCities(candidates);
            if (cities.isEmpty()) {
                throw new ServiceException("有效轨迹未解析出城市");
            }
            Date resolvedAt = new Date();
            TenantHelper.dynamic(tenantId, () -> transactionTemplate.executeWithoutResult(status ->
                persistResolvedCities(tenantId, workout, cities.values(), resolvedAt)));
        } catch (Exception e) {
            markFailure(tenantId, workoutId, workout, e);
        }
    }

    @Override
    public void processPending() {
        Date now = new Date();
        Date staleBefore = new Date(now.getTime() - PROCESSING_TIMEOUT.toMillis());
        List<RunWorkout> pending = workoutMapper.selectPendingCityResolutions(
            now, staleBefore, PENDING_BATCH_SIZE);
        for (RunWorkout workout : pending) {
            process(workout.getTenantId(), workout.getId());
        }
    }

    private Map<String, ResolvedCity> resolveCities(List<RunTrackPoint> candidates) {
        Map<String, ResolvedCity> cities = new LinkedHashMap<>();
        for (RunTrackPoint point : candidates) {
            RunReverseGeocodeVo location = mapService.reverseGeocode(
                point.getLatitude().doubleValue(), point.getLongitude().doubleValue());
            String cityName = StringUtils.isNotBlank(location.city())
                ? location.city() : location.province();
            String cityCode = toCityCode(location.adcode());
            if (StringUtils.isAnyBlank(cityName, cityCode)) {
                continue;
            }
            cities.putIfAbsent(cityCode, new ResolvedCity(
                toProvinceCode(location.adcode()), location.province(), cityCode, cityName,
                point.getLatitude(), point.getLongitude()));
        }
        return cities;
    }

    private void persistResolvedCities(String tenantId,
                                       RunWorkout workout,
                                       Iterable<ResolvedCity> cities,
                                       Date resolvedAt) {
        Date reachedAt = workout.getFinishedAt() == null ? resolvedAt : workout.getFinishedAt();
        for (ResolvedCity city : cities) {
            RunWorkoutCity workoutCity = workoutCityMapper.selectOne(
                Wrappers.<RunWorkoutCity>lambdaQuery()
                    .eq(RunWorkoutCity::getWorkoutId, workout.getId())
                    .eq(RunWorkoutCity::getCityCode, city.cityCode()));
            if (workoutCity == null) {
                workoutCity = new RunWorkoutCity();
                workoutCity.setWorkoutId(workout.getId());
                workoutCity.setUserId(workout.getUserId());
                workoutCity.setProvinceCode(city.provinceCode());
                workoutCity.setProvinceName(city.provinceName());
                workoutCity.setCityCode(city.cityCode());
                workoutCity.setCityName(city.cityName());
                workoutCity.setSampleLatitude(city.latitude());
                workoutCity.setSampleLongitude(city.longitude());
                workoutCity.setResolvedAt(resolvedAt);
                if (workoutCityMapper.insert(workoutCity) < 1) {
                    throw new ServiceException("单次运动城市保存失败");
                }
            }

            RunCityFootprint footprint = footprintMapper.selectOne(
                Wrappers.<RunCityFootprint>lambdaQuery()
                    .eq(RunCityFootprint::getUserId, workout.getUserId())
                    .eq(RunCityFootprint::getCityCode, city.cityCode()));
            if (footprint == null) {
                footprint = new RunCityFootprint();
                footprint.setUserId(workout.getUserId());
                footprint.setProvinceCode(city.provinceCode());
                footprint.setProvinceName(city.provinceName());
                footprint.setCityCode(city.cityCode());
                footprint.setCityName(city.cityName());
                footprint.setFirstWorkoutId(workout.getId());
                footprint.setFirstReachedAt(reachedAt);
                footprint.setLastWorkoutId(workout.getId());
                footprint.setLastReachedAt(reachedAt);
                footprint.setWorkoutCount(1);
                if (footprintMapper.insert(footprint) < 1) {
                    throw new ServiceException("城市足迹保存失败");
                }
            } else if (!workout.getId().equals(footprint.getLastWorkoutId())) {
                footprint.setProvinceCode(city.provinceCode());
                footprint.setProvinceName(city.provinceName());
                footprint.setCityName(city.cityName());
                footprint.setLastWorkoutId(workout.getId());
                footprint.setLastReachedAt(reachedAt);
                footprint.setWorkoutCount(Math.addExact(footprint.getWorkoutCount(), 1));
                if (footprintMapper.updateById(footprint) < 1) {
                    throw new ServiceException("城市足迹更新冲突");
                }
            }
        }
        if (workoutMapper.completeCityResolution(tenantId, workout.getId(), resolvedAt) < 1) {
            throw new ServiceException("城市足迹解析状态更新失败");
        }
        achievementService.evaluateAfterCityFootprint(workout.getUserId(), workout.getId());
    }

    private void markFailure(String tenantId, Long workoutId, RunWorkout workout, Exception error) {
        int attempts = workout == null || workout.getCityResolveAttempts() == null
            ? 1 : workout.getCityResolveAttempts();
        boolean terminal = attempts >= MAX_ATTEMPTS;
        Date now = new Date();
        Date nextAt = terminal ? null : new Date(now.getTime()
            + Duration.ofMinutes(RETRY_MINUTES[Math.min(attempts - 1, RETRY_MINUTES.length - 1)]).toMillis());
        workoutMapper.failCityResolution(tenantId, workoutId,
            terminal ? "FAILED" : "RETRY", nextAt, classifyError(error), now);
        log.warn("运动城市足迹解析失败: tenantId={}, workoutId={}, attempt={}, terminal={}, errorType={}",
            tenantId, workoutId, attempts, terminal, error.getClass().getSimpleName());
    }

    private String classifyError(Exception error) {
        return error instanceof ServiceException ? "MAP_OR_DATA_ERROR" : "INTERNAL_ERROR";
    }

    private String toCityCode(String adcode) {
        if (StringUtils.isBlank(adcode)) {
            return null;
        }
        String digits = adcode.replaceAll("\\D", "");
        return digits.length() >= 4 ? digits.substring(0, 4) + "00" : null;
    }

    private String toProvinceCode(String adcode) {
        if (StringUtils.isBlank(adcode)) {
            return null;
        }
        String digits = adcode.replaceAll("\\D", "");
        return digits.length() >= 2 ? digits.substring(0, 2) + "0000" : null;
    }

    private RunCityFootprintVo toVo(RunCityFootprint footprint) {
        RunCityFootprintVo vo = new RunCityFootprintVo();
        vo.setProvinceCode(footprint.getProvinceCode());
        vo.setProvinceName(footprint.getProvinceName());
        vo.setCityCode(footprint.getCityCode());
        vo.setCityName(footprint.getCityName());
        vo.setFirstReachedAt(footprint.getFirstReachedAt());
        vo.setLastReachedAt(footprint.getLastReachedAt());
        vo.setWorkoutCount(footprint.getWorkoutCount());
        return vo;
    }

    private record ResolvedCity(String provinceCode,
                                String provinceName,
                                String cityCode,
                                String cityName,
                                BigDecimal latitude,
                                BigDecimal longitude) {
    }
}
