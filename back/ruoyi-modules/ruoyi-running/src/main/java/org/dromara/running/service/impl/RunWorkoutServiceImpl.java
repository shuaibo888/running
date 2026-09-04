package org.dromara.running.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.running.domain.RunTrackBatch;
import org.dromara.running.domain.RunTrackPoint;
import org.dromara.running.domain.RunUserProfile;
import org.dromara.running.domain.RunSportType;
import org.dromara.running.domain.RunWorkout;
import org.dromara.running.domain.bo.RunManualWorkoutBo;
import org.dromara.running.domain.bo.RunTrackBatchBo;
import org.dromara.running.domain.bo.RunTrackPointBo;
import org.dromara.running.domain.bo.RunWorkoutFinishBo;
import org.dromara.running.domain.bo.RunWorkoutStartBo;
import org.dromara.running.domain.enums.RunWorkoutStatus;
import org.dromara.running.domain.vo.RunWorkoutVo;
import org.dromara.running.domain.vo.RunTrackPointVo;
import org.dromara.running.domain.vo.RunWorkoutStatisticsVo;
import org.dromara.running.domain.vo.RunWorkoutTrendPointVo;
import org.dromara.running.domain.vo.RunWorkoutTrendVo;
import org.dromara.running.mapper.RunTrackBatchMapper;
import org.dromara.running.mapper.RunTrackPointMapper;
import org.dromara.running.mapper.RunUserProfileMapper;
import org.dromara.running.mapper.RunSportTypeMapper;
import org.dromara.running.mapper.RunWorkoutMapper;
import org.dromara.running.service.IRunWorkoutService;
import org.dromara.running.service.IRunAchievementService;
import org.dromara.running.service.IRunCityFootprintService;
import org.dromara.running.service.IRunRouteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 跑步状态机、轨迹校验与首版卡路里结算。
 */
@RequiredArgsConstructor
@Service
public class RunWorkoutServiceImpl implements IRunWorkoutService {

    private static final String SPORT_RUNNING = "RUNNING";
    private static final String SPORT_RUNNING_NAME = "户外跑步";
    private static final String SOURCE_GPS = "GPS";
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String TRACKING_DURATION = "DURATION";
    private static final String ENABLED = "0";
    private static final String RUNNING_CALORIE_ALGORITHM = "RUNNING_WEIGHT_DISTANCE_V1";
    private static final BigDecimal CALORIE_FACTOR = new BigDecimal("1.036");
    private static final BigDecimal ZERO_DISTANCE = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final double EARTH_RADIUS_METERS = 6_371_008.8D;
    private static final double MAX_ACCURACY_METERS = 60D;
    private static final double MAX_RUNNING_SPEED_MPS = 12D;
    private static final double MIN_MOVEMENT_METERS = 2D;
    private static final long MAX_TRACK_GAP_SECONDS = 120L;
    private static final long MAX_CLOCK_SKEW_MILLIS = Duration.ofMinutes(5).toMillis();
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final RunWorkoutMapper workoutMapper;
    private final RunTrackBatchMapper batchMapper;
    private final RunTrackPointMapper pointMapper;
    private final RunUserProfileMapper profileMapper;
    private final RunSportTypeMapper sportTypeMapper;
    private final IRunRouteService routeService;
    private final IRunAchievementService achievementService;
    private final IRunCityFootprintService cityFootprintService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RunWorkoutVo start(Long userId, RunWorkoutStartBo bo) {
        RunWorkout existing = workoutMapper.selectOne(Wrappers.<RunWorkout>lambdaQuery()
            .eq(RunWorkout::getUserId, userId)
            .eq(RunWorkout::getClientWorkoutId, bo.getClientWorkoutId()));
        if (existing != null) {
            return toVo(existing);
        }

        RunWorkout active = workoutMapper.selectOne(Wrappers.<RunWorkout>lambdaQuery()
            .eq(RunWorkout::getUserId, userId)
            .eq(RunWorkout::getActiveFlag, 1));
        if (active != null) {
            throw new ServiceException("你已有未结束的运动，请先恢复或结束原记录");
        }

        RunUserProfile profile = profileMapper.selectOne(Wrappers.<RunUserProfile>lambdaQuery()
            .eq(RunUserProfile::getUserId, userId));
        if (profile == null || !Boolean.TRUE.equals(profile.getProfileCompleted()) || profile.getWeightKg() == null) {
            throw new ServiceException("开始运动前请先完善昵称、性别、生日、身高和体重");
        }

        RunTrackPointBo initial = bo.getInitialPoint();
        validateInitialPoint(initial);
        RunWorkout workout = new RunWorkout();
        workout.setUserId(userId);
        workout.setClientWorkoutId(bo.getClientWorkoutId());
        workout.setSportType(SPORT_RUNNING);
        workout.setSportName(SPORT_RUNNING_NAME);
        workout.setRecordSource(SOURCE_GPS);
        workout.setStatus(RunWorkoutStatus.RUNNING.name());
        workout.setActiveFlag(1);
        // 运动时长以设备采集到的首个有效定位点为起点；上方已校验设备与服务器时钟偏差。
        workout.setStartedAt(initial.getRecordedAt());
        workout.setElapsedSeconds(0);
        workout.setPausedSeconds(0);
        workout.setDistanceMeters(ZERO_DISTANCE);
        workout.setCaloriesKcal(ZERO_DISTANCE);
        workout.setWeightKg(profile.getWeightKg());
        workout.setCalorieAlgorithm(RUNNING_CALORIE_ALGORITHM);
        workout.setMetValue(null);
        workout.setRankingEligible(true);
        applyRankingRegionSnapshot(workout, profile);
        workout.setRouteId(routeService.resolveSelectedRouteId(userId));
        workout.setStartLatitude(initial.getLatitude());
        workout.setStartLongitude(initial.getLongitude());
        workout.setEndLatitude(initial.getLatitude());
        workout.setEndLongitude(initial.getLongitude());
        workout.setPointCount(1);
        workout.setInvalidPointCount(0);
        workout.setLastTrackSeq(0);
        workout.setLastPointTime(initial.getRecordedAt());
        if (workoutMapper.insert(workout) < 1) {
            throw new ServiceException("运动创建失败，请稍后重试");
        }

        RunTrackPoint initialPoint = buildTrackPoint(workout.getId(), null, initial);
        initialPoint.setSequenceNo(0);
        initialPoint.setValidFlag(true);
        initialPoint.setSegmentDistanceMeters(ZERO_DISTANCE);
        if (pointMapper.insert(initialPoint) < 1) {
            throw new ServiceException("运动起点保存失败，请稍后重试");
        }
        return toVo(workout);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RunWorkoutVo recordManual(Long userId, RunManualWorkoutBo bo) {
        RunWorkout existing = workoutMapper.selectOne(Wrappers.<RunWorkout>lambdaQuery()
            .eq(RunWorkout::getUserId, userId)
            .eq(RunWorkout::getClientWorkoutId, bo.getClientWorkoutId()));
        if (existing != null) {
            return toVo(existing);
        }

        RunWorkout active = workoutMapper.selectOne(Wrappers.<RunWorkout>lambdaQuery()
            .eq(RunWorkout::getUserId, userId)
            .eq(RunWorkout::getActiveFlag, 1));
        if (active != null) {
            throw new ServiceException("你已有未结束的运动，请先恢复或结束原记录");
        }

        RunUserProfile profile = requireCompletedProfile(userId);
        RunSportType sportType = sportTypeMapper.selectOne(Wrappers.<RunSportType>lambdaQuery()
            .eq(RunSportType::getSportCode, bo.getSportType())
            .eq(RunSportType::getTrackingMode, TRACKING_DURATION)
            .eq(RunSportType::getStatus, ENABLED));
        if (sportType == null || sportType.getMetValue() == null || sportType.getMetValue().signum() <= 0) {
            throw new ServiceException("运动类型不存在、已停用或不支持按时长记录");
        }
        if (bo.getDurationMinutes() < sportType.getMinDurationMinutes()
            || bo.getDurationMinutes() > sportType.getMaxDurationMinutes()) {
            throw new ServiceException("该运动时长需在" + sportType.getMinDurationMinutes()
                + "至" + sportType.getMaxDurationMinutes() + "分钟之间");
        }

        int elapsedSeconds = Math.multiplyExact(bo.getDurationMinutes(), 60);
        Date finishedAt = new Date();
        Date startedAt = new Date(finishedAt.getTime() - elapsedSeconds * 1000L);
        RunWorkout workout = new RunWorkout();
        workout.setUserId(userId);
        workout.setClientWorkoutId(bo.getClientWorkoutId());
        workout.setSportType(sportType.getSportCode());
        workout.setSportName(sportType.getSportName());
        workout.setRecordSource(SOURCE_MANUAL);
        workout.setStatus(RunWorkoutStatus.COMPLETED.name());
        workout.setActiveFlag(null);
        workout.setStartedAt(startedAt);
        workout.setFinishedAt(finishedAt);
        workout.setElapsedSeconds(elapsedSeconds);
        workout.setPausedSeconds(0);
        workout.setDistanceMeters(ZERO_DISTANCE);
        workout.setCaloriesKcal(calculateMetCalories(
            profile.getWeightKg(), sportType.getMetValue(), bo.getDurationMinutes()));
        workout.setAvgPaceSeconds(null);
        workout.setWeightKg(profile.getWeightKg());
        workout.setCalorieAlgorithm(sportType.getCalorieAlgorithm());
        workout.setMetValue(sportType.getMetValue());
        // 手工时长无法验证真实性：计入个人累计和成就，但不进入地区排行榜。
        workout.setRankingEligible(false);
        applyRankingRegionSnapshot(workout, profile);
        workout.setRouteId(null);
        workout.setPointCount(0);
        workout.setInvalidPointCount(0);
        workout.setLastTrackSeq(-1);
        workout.setFinishRequestId(bo.getClientWorkoutId());
        if (workoutMapper.insert(workout) < 1) {
            throw new ServiceException("运动记录保存失败，请稍后重试");
        }
        achievementService.evaluateAfterWorkout(userId, workout.getId());
        return toVo(workout);
    }

    @Override
    public RunWorkoutVo queryActive(Long userId) {
        RunWorkout workout = workoutMapper.selectOne(Wrappers.<RunWorkout>lambdaQuery()
            .eq(RunWorkout::getUserId, userId)
            .eq(RunWorkout::getActiveFlag, 1));
        return workout == null ? null : toVo(workout);
    }

    @Override
    public RunWorkoutVo queryById(Long userId, Long workoutId) {
        return toVo(requireWorkout(userId, workoutId, false));
    }

    @Override
    public TableDataInfo<RunWorkoutVo> queryHistory(Long userId, int pageNum, int pageSize) {
        int safePageNum = Math.max(1, pageNum);
        int safePageSize = Math.max(1, Math.min(50, pageSize));
        Page<RunWorkout> page = workoutMapper.selectPage(
            new Page<>(safePageNum, safePageSize),
            Wrappers.<RunWorkout>lambdaQuery()
                .eq(RunWorkout::getUserId, userId)
                .eq(RunWorkout::getStatus, RunWorkoutStatus.COMPLETED.name())
                .orderByDesc(RunWorkout::getStartedAt)
                .orderByDesc(RunWorkout::getId));
        List<RunWorkoutVo> rows = page.getRecords().stream().map(this::toVo).toList();
        return new TableDataInfo<>(rows, page.getTotal());
    }

    @Override
    public List<RunTrackPointVo> queryTrackPoints(Long userId,
                                                  Long workoutId,
                                                  int afterSequence,
                                                  int limit) {
        requireWorkout(userId, workoutId, false);
        int safeLimit = Math.max(1, Math.min(500, limit));
        return pointMapper.selectList(Wrappers.<RunTrackPoint>lambdaQuery()
                .eq(RunTrackPoint::getWorkoutId, workoutId)
                .eq(RunTrackPoint::getValidFlag, true)
                .gt(RunTrackPoint::getSequenceNo, Math.max(-1, afterSequence))
                .orderByAsc(RunTrackPoint::getSequenceNo)
                .last("LIMIT " + safeLimit))
            .stream()
            .map(this::toTrackPointVo)
            .toList();
    }

    @Override
    public RunWorkoutStatisticsVo queryStatistics(Long userId) {
        RunWorkoutStatisticsVo statistics = workoutMapper.selectCompletedStatistics(userId);
        if (statistics == null) {
            statistics = new RunWorkoutStatisticsVo();
        }
        if (statistics.getWorkoutCount() == null) {
            statistics.setWorkoutCount(0L);
        }
        if (statistics.getTotalElapsedSeconds() == null) {
            statistics.setTotalElapsedSeconds(0L);
        }
        if (statistics.getTotalDistanceMeters() == null) {
            statistics.setTotalDistanceMeters(ZERO_DISTANCE);
        }
        if (statistics.getTotalCaloriesKcal() == null) {
            statistics.setTotalCaloriesKcal(ZERO_DISTANCE);
        }
        Long cityCount = workoutMapper.selectCityCount(userId);
        statistics.setCityCount(cityCount == null ? 0L : cityCount);
        Long longestConsecutiveDays = workoutMapper.selectLongestConsecutiveDays(userId);
        statistics.setLongestConsecutiveDays(longestConsecutiveDays == null ? 0L : longestConsecutiveDays);
        return statistics;
    }

    @Override
    public RunWorkoutTrendVo queryDailyTrends(Long userId, int days) {
        if (days != 7 && days != 30 && days != 90) {
            throw new ServiceException("趋势周期只支持7天、30天或90天");
        }
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate fromDate = today.minusDays(days - 1L);
        LocalDate exclusiveEndDate = today.plusDays(1L);
        Date from = Date.from(fromDate.atStartOfDay(BUSINESS_ZONE).toInstant());
        Date to = Date.from(exclusiveEndDate.atStartOfDay(BUSINESS_ZONE).toInstant());
        Map<String, RunWorkoutTrendPointVo> aggregated = new HashMap<>();
        for (RunWorkoutTrendPointVo point : workoutMapper.selectDailyTrends(userId, from, to)) {
            aggregated.put(point.getTrendDate(), point);
        }

        List<RunWorkoutTrendPointVo> points = new ArrayList<>(days);
        for (int index = 0; index < days; index++) {
            String date = fromDate.plusDays(index).toString();
            RunWorkoutTrendPointVo point = aggregated.getOrDefault(date, emptyTrendPoint(date));
            normalizeTrendPoint(point);
            points.add(point);
        }
        RunWorkoutTrendVo result = new RunWorkoutTrendVo();
        result.setDays(days);
        result.setFromDate(fromDate.toString());
        result.setToDate(today.toString());
        result.setPoints(points);
        return result;
    }

    private RunWorkoutTrendPointVo emptyTrendPoint(String date) {
        RunWorkoutTrendPointVo point = new RunWorkoutTrendPointVo();
        point.setTrendDate(date);
        point.setWorkoutCount(0L);
        point.setTotalElapsedSeconds(0L);
        point.setTotalDistanceMeters(ZERO_DISTANCE);
        point.setTotalCaloriesKcal(ZERO_DISTANCE);
        return point;
    }

    private void normalizeTrendPoint(RunWorkoutTrendPointVo point) {
        if (point.getWorkoutCount() == null) point.setWorkoutCount(0L);
        if (point.getTotalElapsedSeconds() == null) point.setTotalElapsedSeconds(0L);
        if (point.getTotalDistanceMeters() == null) point.setTotalDistanceMeters(ZERO_DISTANCE);
        if (point.getTotalCaloriesKcal() == null) point.setTotalCaloriesKcal(ZERO_DISTANCE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RunWorkoutVo appendTrackBatch(Long userId, Long workoutId, RunTrackBatchBo bo) {
        RunWorkout workout = requireWorkout(userId, workoutId, true);
        RunTrackBatch existingBatch = batchMapper.selectOne(Wrappers.<RunTrackBatch>lambdaQuery()
            .eq(RunTrackBatch::getWorkoutId, workoutId)
            .eq(RunTrackBatch::getClientBatchId, bo.getClientBatchId()));
        if (existingBatch != null) {
            return toVo(workout);
        }
        requireStatus(workout, RunWorkoutStatus.RUNNING, "运动暂停或已结束，不能继续上传轨迹");

        List<RunTrackPointBo> points = bo.getPoints();
        validateSequences(workout, points);
        RunTrackBatch batch = new RunTrackBatch();
        batch.setWorkoutId(workoutId);
        batch.setUserId(userId);
        batch.setClientBatchId(bo.getClientBatchId());
        batch.setStartSequence(points.get(0).getSequenceNo());
        batch.setEndSequence(points.get(points.size() - 1).getSequenceNo());
        batch.setPointCount(points.size());
        batch.setInvalidPointCount(0);
        batch.setDistanceMeters(ZERO_DISTANCE);
        if (batchMapper.insert(batch) < 1) {
            throw new ServiceException("轨迹批次保存失败，请重试");
        }

        RunTrackPoint previousValid = pointMapper.selectOne(Wrappers.<RunTrackPoint>lambdaQuery()
            .eq(RunTrackPoint::getWorkoutId, workoutId)
            .eq(RunTrackPoint::getValidFlag, true)
            .orderByDesc(RunTrackPoint::getSequenceNo)
            .last("LIMIT 1"));
        Date lastReceivedTime = workout.getLastPointTime();
        Date newestReceivedTime = lastReceivedTime;
        BigDecimal batchDistance = ZERO_DISTANCE;
        int invalidCount = 0;

        for (RunTrackPointBo pointBo : points) {
            RunTrackPoint point = buildTrackPoint(workoutId, batch.getId(), pointBo);
            TrackValidation validation = validateTrackPoint(workout, pointBo, lastReceivedTime, previousValid);
            point.setValidFlag(validation.valid());
            point.setInvalidReason(validation.invalidReason());
            point.setSegmentDistanceMeters(validation.segmentDistance());
            if (pointMapper.insert(point) < 1) {
                throw new ServiceException("轨迹点保存失败，请重试");
            }

            if (validation.valid()) {
                previousValid = point;
                batchDistance = batchDistance.add(validation.segmentDistance());
                workout.setEndLatitude(point.getLatitude());
                workout.setEndLongitude(point.getLongitude());
            } else {
                invalidCount++;
            }
            if (lastReceivedTime == null || pointBo.getRecordedAt().after(lastReceivedTime)) {
                lastReceivedTime = pointBo.getRecordedAt();
            }
            if (newestReceivedTime == null || pointBo.getRecordedAt().after(newestReceivedTime)) {
                newestReceivedTime = pointBo.getRecordedAt();
            }
        }

        batch.setInvalidPointCount(invalidCount);
        batch.setDistanceMeters(batchDistance.setScale(2, RoundingMode.HALF_UP));
        if (batchMapper.updateById(batch) < 1) {
            throw new ServiceException("轨迹批次汇总失败，请重试");
        }

        workout.setPointCount(workout.getPointCount() + points.size());
        workout.setInvalidPointCount(workout.getInvalidPointCount() + invalidCount);
        workout.setLastTrackSeq(points.get(points.size() - 1).getSequenceNo());
        workout.setLastPointTime(newestReceivedTime);
        workout.setDistanceMeters(workout.getDistanceMeters().add(batchDistance).setScale(2, RoundingMode.HALF_UP));
        workout.setCaloriesKcal(calculateCalories(workout.getWeightKg(), workout.getDistanceMeters()));
        if (workoutMapper.updateById(workout) < 1) {
            throw new ServiceException("运动轨迹汇总冲突，请重新获取运动状态后重试");
        }
        return toVo(workout);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RunWorkoutVo pause(Long userId, Long workoutId) {
        RunWorkout workout = requireWorkout(userId, workoutId, true);
        RunWorkoutStatus status = statusOf(workout);
        if (status == RunWorkoutStatus.PAUSED) {
            return toVo(workout);
        }
        requireStatus(workout, RunWorkoutStatus.RUNNING, "当前运动不能暂停");
        workout.setStatus(RunWorkoutStatus.PAUSED.name());
        workout.setPausedStartedAt(new Date());
        updateWorkout(workout, "暂停状态保存失败，请重试");
        return toVo(workout);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RunWorkoutVo resume(Long userId, Long workoutId) {
        RunWorkout workout = requireWorkout(userId, workoutId, true);
        RunWorkoutStatus status = statusOf(workout);
        if (status == RunWorkoutStatus.RUNNING) {
            return toVo(workout);
        }
        requireStatus(workout, RunWorkoutStatus.PAUSED, "当前运动不能继续");
        workout.setPausedSeconds(safeAddSeconds(
            workout.getPausedSeconds(), secondsBetween(workout.getPausedStartedAt(), new Date())));
        workout.setPausedStartedAt(null);
        workout.setStatus(RunWorkoutStatus.RUNNING.name());
        updateWorkout(workout, "继续状态保存失败，请重试");
        return toVo(workout);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RunWorkoutVo finish(Long userId, Long workoutId, RunWorkoutFinishBo bo) {
        RunWorkout workout = requireWorkout(userId, workoutId, true);
        if (statusOf(workout) == RunWorkoutStatus.COMPLETED) {
            return toVo(workout);
        }
        if (!statusOf(workout).isActive()) {
            throw new ServiceException("当前运动不能结束");
        }

        Date finishedAt = new Date();
        if (statusOf(workout) == RunWorkoutStatus.PAUSED) {
            workout.setPausedSeconds(safeAddSeconds(
                workout.getPausedSeconds(), secondsBetween(workout.getPausedStartedAt(), finishedAt)));
            workout.setPausedStartedAt(null);
        }
        workout.setFinishedAt(finishedAt);
        workout.setElapsedSeconds(calculateElapsedSeconds(workout, finishedAt));
        workout.setCaloriesKcal(calculateCalories(workout.getWeightKg(), workout.getDistanceMeters()));
        workout.setAvgPaceSeconds(calculateAveragePace(workout.getElapsedSeconds(), workout.getDistanceMeters()));
        workout.setFinishRequestId(bo.getClientFinishId());
        workout.setStatus(RunWorkoutStatus.COMPLETED.name());
        workout.setActiveFlag(null);
        workout.setCityResolveStatus("PENDING");
        workout.setCityResolveAttempts(0);
        workout.setCityResolveNextAt(null);
        workout.setCityResolvedAt(null);
        workout.setCityResolveError(null);
        updateWorkout(workout, "运动结束保存失败，请重试");
        routeService.applyCompletedWorkout(userId, workout);
        achievementService.evaluateAfterWorkout(userId, workout.getId());
        cityFootprintService.publishCompletedWorkout(workout.getId());
        return toVo(workout);
    }

    private RunWorkout requireWorkout(Long userId, Long workoutId, boolean forUpdate) {
        var wrapper = Wrappers.<RunWorkout>lambdaQuery()
            .eq(RunWorkout::getId, workoutId)
            .eq(RunWorkout::getUserId, userId);
        if (forUpdate) {
            wrapper.last("FOR UPDATE");
        }
        RunWorkout workout = workoutMapper.selectOne(wrapper);
        if (workout == null) {
            throw new ServiceException("运动记录不存在");
        }
        return workout;
    }

    private RunUserProfile requireCompletedProfile(Long userId) {
        RunUserProfile profile = profileMapper.selectOne(Wrappers.<RunUserProfile>lambdaQuery()
            .eq(RunUserProfile::getUserId, userId));
        if (profile == null || !Boolean.TRUE.equals(profile.getProfileCompleted()) || profile.getWeightKg() == null) {
            throw new ServiceException("记录运动前请先完善昵称、性别、生日、身高和体重");
        }
        return profile;
    }

    private void applyRankingRegionSnapshot(RunWorkout workout, RunUserProfile profile) {
        workout.setRankingProvinceCode(profile.getProvinceCode());
        workout.setRankingProvinceName(profile.getProvinceName());
        workout.setRankingCityCode(profile.getCityCode());
        workout.setRankingCityName(profile.getCityName());
    }

    private void validateInitialPoint(RunTrackPointBo point) {
        if (point.getSequenceNo() == null || point.getSequenceNo() != 0) {
            throw new ServiceException("起点轨迹序号必须为0");
        }
        if (point.getAccuracyMeters() == null
            || point.getAccuracyMeters().doubleValue() > MAX_ACCURACY_METERS) {
            throw new ServiceException("当前定位精度不足，请到开阔区域后重试");
        }
        long clockSkew = Math.abs(System.currentTimeMillis() - point.getRecordedAt().getTime());
        if (clockSkew > MAX_CLOCK_SKEW_MILLIS) {
            throw new ServiceException("设备时间与服务器差异过大，请校准系统时间后重试");
        }
    }

    private void validateSequences(RunWorkout workout, List<RunTrackPointBo> points) {
        int expected = workout.getLastTrackSeq() + 1;
        for (RunTrackPointBo point : points) {
            if (point.getSequenceNo() == null || point.getSequenceNo() != expected) {
                throw new ServiceException("轨迹序号不连续，请从序号" + expected + "重新上传");
            }
            expected++;
        }
    }

    private TrackValidation validateTrackPoint(RunWorkout workout,
                                               RunTrackPointBo point,
                                               Date lastReceivedTime,
                                               RunTrackPoint previousValid) {
        if (point.getRecordedAt().getTime() > System.currentTimeMillis() + 30_000L) {
            return TrackValidation.invalid("FUTURE_TIME");
        }
        if (point.getRecordedAt().getTime() < workout.getStartedAt().getTime() - 30_000L) {
            return TrackValidation.invalid("BEFORE_START");
        }
        if (lastReceivedTime != null && !point.getRecordedAt().after(lastReceivedTime)) {
            return TrackValidation.invalid("TIME_ORDER");
        }
        if (point.getAccuracyMeters() == null) {
            return TrackValidation.invalid("NO_ACCURACY");
        }
        if (point.getAccuracyMeters().doubleValue() > MAX_ACCURACY_METERS) {
            return TrackValidation.invalid("LOW_ACCURACY");
        }
        if (previousValid == null) {
            return TrackValidation.valid(ZERO_DISTANCE);
        }

        long seconds = secondsBetween(previousValid.getRecordedAt(), point.getRecordedAt());
        if (seconds <= 0) {
            return TrackValidation.invalid("TIME_ORDER");
        }
        if (seconds > MAX_TRACK_GAP_SECONDS) {
            return TrackValidation.valid(ZERO_DISTANCE);
        }

        double distance = haversineMeters(
            previousValid.getLatitude().doubleValue(),
            previousValid.getLongitude().doubleValue(),
            point.getLatitude().doubleValue(),
            point.getLongitude().doubleValue()
        );
        if (distance / seconds > MAX_RUNNING_SPEED_MPS) {
            return TrackValidation.invalid("EXCESSIVE_SPEED");
        }
        double previousAccuracy = previousValid.getAccuracyMeters() == null
            ? MAX_ACCURACY_METERS : previousValid.getAccuracyMeters().doubleValue();
        double accuracyNoiseFloor = Math.min(15D,
            (previousAccuracy + point.getAccuracyMeters().doubleValue()) * 0.25D);
        if (distance < Math.max(MIN_MOVEMENT_METERS, accuracyNoiseFloor)) {
            return TrackValidation.valid(ZERO_DISTANCE);
        }
        return TrackValidation.valid(BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP));
    }

    private RunTrackPoint buildTrackPoint(Long workoutId, Long batchId, RunTrackPointBo bo) {
        RunTrackPoint point = new RunTrackPoint();
        point.setWorkoutId(workoutId);
        point.setBatchId(batchId);
        point.setSequenceNo(bo.getSequenceNo());
        point.setRecordedAt(bo.getRecordedAt());
        point.setLatitude(bo.getLatitude());
        point.setLongitude(bo.getLongitude());
        point.setAccuracyMeters(bo.getAccuracyMeters());
        point.setReportedSpeedMps(bo.getReportedSpeedMps());
        point.setAltitudeMeters(bo.getAltitudeMeters());
        point.setDirectionDegrees(bo.getDirectionDegrees());
        return point;
    }

    private RunWorkoutStatus statusOf(RunWorkout workout) {
        try {
            return RunWorkoutStatus.valueOf(workout.getStatus());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ServiceException("运动状态异常，请联系管理员");
        }
    }

    private void requireStatus(RunWorkout workout, RunWorkoutStatus expected, String message) {
        if (statusOf(workout) != expected) {
            throw new ServiceException(message);
        }
    }

    private void updateWorkout(RunWorkout workout, String errorMessage) {
        if (workoutMapper.updateById(workout) < 1) {
            throw new ServiceException(errorMessage);
        }
    }

    private int calculateElapsedSeconds(RunWorkout workout, Date referenceTime) {
        Date activeUntil = statusOf(workout) == RunWorkoutStatus.PAUSED && workout.getPausedStartedAt() != null
            ? workout.getPausedStartedAt() : referenceTime;
        long elapsed = secondsBetween(workout.getStartedAt(), activeUntil) - workout.getPausedSeconds();
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, elapsed));
    }

    private BigDecimal calculateCalories(BigDecimal weightKg, BigDecimal distanceMeters) {
        if (weightKg == null || distanceMeters == null || distanceMeters.signum() <= 0) {
            return ZERO_DISTANCE;
        }
        return weightKg.multiply(distanceMeters)
            .multiply(CALORIE_FACTOR)
            .divide(BigDecimal.valueOf(1000L), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMetCalories(BigDecimal weightKg,
                                            BigDecimal metValue,
                                            int durationMinutes) {
        if (weightKg == null || metValue == null || durationMinutes <= 0) {
            return ZERO_DISTANCE;
        }
        return metValue.multiply(weightKg)
            .multiply(BigDecimal.valueOf(durationMinutes))
            .divide(BigDecimal.valueOf(60L), 2, RoundingMode.HALF_UP);
    }

    private Integer calculateAveragePace(int elapsedSeconds, BigDecimal distanceMeters) {
        if (distanceMeters == null || distanceMeters.compareTo(BigDecimal.TEN) < 0) {
            return null;
        }
        return BigDecimal.valueOf(elapsedSeconds)
            .multiply(BigDecimal.valueOf(1000L))
            .divide(distanceMeters, 0, RoundingMode.HALF_UP)
            .intValue();
    }

    private long secondsBetween(Date start, Date end) {
        if (start == null || end == null) {
            return 0L;
        }
        return Math.max(0L, (end.getTime() - start.getTime()) / 1000L);
    }

    private int safeAddSeconds(Integer current, long delta) {
        long total = (current == null ? 0L : current.longValue()) + Math.max(0L, delta);
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double latDelta = Math.toRadians(lat2 - lat1);
        double lngDelta = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDelta / 2D) * Math.sin(latDelta / 2D)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lngDelta / 2D) * Math.sin(lngDelta / 2D);
        return EARTH_RADIUS_METERS * 2D * Math.atan2(Math.sqrt(a), Math.sqrt(1D - a));
    }

    private RunWorkoutVo toVo(RunWorkout workout) {
        RunWorkoutVo vo = new RunWorkoutVo();
        vo.setId(workout.getId());
        vo.setClientWorkoutId(workout.getClientWorkoutId());
        vo.setSportType(workout.getSportType());
        vo.setSportName(workout.getSportName());
        vo.setRecordSource(workout.getRecordSource());
        vo.setStatus(workout.getStatus());
        vo.setStartedAt(workout.getStartedAt());
        vo.setPausedStartedAt(workout.getPausedStartedAt());
        vo.setFinishedAt(workout.getFinishedAt());
        int elapsed = statusOf(workout).isActive()
            ? calculateElapsedSeconds(workout, new Date()) : workout.getElapsedSeconds();
        vo.setElapsedSeconds(elapsed);
        vo.setPausedSeconds(workout.getPausedSeconds());
        vo.setDistanceMeters(workout.getDistanceMeters());
        vo.setCaloriesKcal(workout.getCaloriesKcal());
        vo.setAvgPaceSeconds(statusOf(workout).isActive()
            ? calculateAveragePace(elapsed, workout.getDistanceMeters()) : workout.getAvgPaceSeconds());
        vo.setWeightKg(workout.getWeightKg());
        vo.setCalorieAlgorithm(workout.getCalorieAlgorithm());
        vo.setMetValue(workout.getMetValue());
        vo.setRankingEligible(workout.getRankingEligible());
        vo.setRouteId(workout.getRouteId());
        vo.setStartLatitude(workout.getStartLatitude());
        vo.setStartLongitude(workout.getStartLongitude());
        vo.setEndLatitude(workout.getEndLatitude());
        vo.setEndLongitude(workout.getEndLongitude());
        vo.setPointCount(workout.getPointCount());
        vo.setInvalidPointCount(workout.getInvalidPointCount());
        vo.setLastTrackSeq(workout.getLastTrackSeq());
        return vo;
    }

    private RunTrackPointVo toTrackPointVo(RunTrackPoint point) {
        RunTrackPointVo vo = new RunTrackPointVo();
        vo.setSequenceNo(point.getSequenceNo());
        vo.setRecordedAt(point.getRecordedAt());
        vo.setLatitude(point.getLatitude());
        vo.setLongitude(point.getLongitude());
        return vo;
    }

    private record TrackValidation(boolean valid, String invalidReason, BigDecimal segmentDistance) {
        static TrackValidation valid(BigDecimal distance) {
            return new TrackValidation(true, null, distance);
        }

        static TrackValidation invalid(String reason) {
            return new TrackValidation(false, reason, ZERO_DISTANCE);
        }
    }
}
