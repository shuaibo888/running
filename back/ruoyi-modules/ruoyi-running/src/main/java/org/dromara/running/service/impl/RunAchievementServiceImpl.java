package org.dromara.running.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.running.domain.RunAchievement;
import org.dromara.running.domain.RunUserAchievement;
import org.dromara.running.domain.vo.RunAchievementVo;
import org.dromara.running.domain.vo.RunWorkoutStatisticsVo;
import org.dromara.running.mapper.RunAchievementMapper;
import org.dromara.running.mapper.RunUserAchievementMapper;
import org.dromara.running.mapper.RunWorkoutMapper;
import org.dromara.running.service.IRunAchievementService;
import org.dromara.running.service.IRunPointService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 基于可重算累计值解锁里程碑。 */
@RequiredArgsConstructor
@Service
public class RunAchievementServiceImpl implements IRunAchievementService {

    private static final String ENABLED = "0";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final RunAchievementMapper achievementMapper;
    private final RunUserAchievementMapper userAchievementMapper;
    private final RunWorkoutMapper workoutMapper;
    private final IRunPointService pointService;

    @Override
    public List<RunAchievementVo> listAchievements(Long userId) {
        RunWorkoutStatisticsVo statistics = normalizedStatistics(userId);
        Map<Long, RunUserAchievement> unlocked = userAchievementMapper.selectList(
                Wrappers.<RunUserAchievement>lambdaQuery().eq(RunUserAchievement::getUserId, userId))
            .stream()
            .collect(Collectors.toMap(RunUserAchievement::getAchievementId, Function.identity()));
        return enabledDefinitions().stream()
            .map(definition -> toVo(definition, unlocked.get(definition.getId()), statistics))
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void evaluateAfterWorkout(Long userId, Long workoutId) {
        evaluate(userId, workoutId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void evaluateAfterCityFootprint(Long userId, Long workoutId) {
        evaluate(userId, workoutId);
    }

    private void evaluate(Long userId, Long workoutId) {
        RunWorkoutStatisticsVo statistics = normalizedStatistics(userId);
        Set<Long> unlockedIds = userAchievementMapper.selectList(
                Wrappers.<RunUserAchievement>lambdaQuery().eq(RunUserAchievement::getUserId, userId))
            .stream().map(RunUserAchievement::getAchievementId).collect(Collectors.toSet());
        for (RunAchievement definition : enabledDefinitions()) {
            if (unlockedIds.contains(definition.getId())) {
                continue;
            }
            BigDecimal currentValue = metricValue(definition.getMetricType(), statistics);
            if (currentValue.compareTo(definition.getThresholdValue()) < 0) {
                continue;
            }
            RunUserAchievement unlocked = new RunUserAchievement();
            unlocked.setUserId(userId);
            unlocked.setAchievementId(definition.getId());
            unlocked.setUnlockedAt(new Date());
            unlocked.setAchievedValue(currentValue);
            unlocked.setSourceWorkoutId(workoutId);
            if (userAchievementMapper.insert(unlocked) < 1) {
                throw new ServiceException("成就解锁保存失败，请重试结束运动");
            }
            pointService.awardAchievement(userId, workoutId, definition.getId(),
                definition.getAchievementName(), definition.getRewardPoints());
        }
    }

    private List<RunAchievement> enabledDefinitions() {
        return achievementMapper.selectList(Wrappers.<RunAchievement>lambdaQuery()
            .eq(RunAchievement::getStatus, ENABLED)
            .orderByAsc(RunAchievement::getSortOrder)
            .orderByAsc(RunAchievement::getId));
    }

    private RunWorkoutStatisticsVo normalizedStatistics(Long userId) {
        RunWorkoutStatisticsVo statistics = workoutMapper.selectCompletedStatistics(userId);
        if (statistics == null) {
            statistics = new RunWorkoutStatisticsVo();
        }
        if (statistics.getWorkoutCount() == null) statistics.setWorkoutCount(0L);
        if (statistics.getTotalDistanceMeters() == null) statistics.setTotalDistanceMeters(ZERO);
        if (statistics.getTotalCaloriesKcal() == null) statistics.setTotalCaloriesKcal(ZERO);
        Long cityCount = workoutMapper.selectCityCount(userId);
        statistics.setCityCount(cityCount == null ? 0L : cityCount);
        Long longestConsecutiveDays = workoutMapper.selectLongestConsecutiveDays(userId);
        statistics.setLongestConsecutiveDays(longestConsecutiveDays == null ? 0L : longestConsecutiveDays);
        return statistics;
    }

    private BigDecimal metricValue(String metricType, RunWorkoutStatisticsVo statistics) {
        return switch (metricType) {
            case "WORKOUT_COUNT" -> BigDecimal.valueOf(statistics.getWorkoutCount());
            case "TOTAL_DISTANCE" -> statistics.getTotalDistanceMeters();
            case "TOTAL_CALORIES" -> statistics.getTotalCaloriesKcal();
            case "CITY_COUNT" -> BigDecimal.valueOf(statistics.getCityCount());
            case "CONSECUTIVE_DAYS" -> BigDecimal.valueOf(statistics.getLongestConsecutiveDays());
            default -> ZERO;
        };
    }

    private RunAchievementVo toVo(RunAchievement definition,
                                   RunUserAchievement unlocked,
                                   RunWorkoutStatisticsVo statistics) {
        RunAchievementVo vo = new RunAchievementVo();
        vo.setId(definition.getId());
        vo.setAchievementCode(definition.getAchievementCode());
        vo.setAchievementName(definition.getAchievementName());
        vo.setDescription(definition.getDescription());
        vo.setMetricType(definition.getMetricType());
        vo.setThresholdValue(definition.getThresholdValue());
        vo.setMedalLevel(definition.getMedalLevel());
        vo.setIconUrl(definition.getIconUrl());
        vo.setRewardPoints(definition.getRewardPoints());
        BigDecimal current = metricValue(definition.getMetricType(), statistics);
        vo.setCurrentValue(current);
        vo.setProgressPercent(calculatePercent(current, definition.getThresholdValue()));
        vo.setUnlocked(unlocked != null);
        if (unlocked != null) {
            vo.setUnlockedAt(unlocked.getUnlockedAt());
            vo.setAchievedValue(unlocked.getAchievedValue());
        }
        return vo;
    }

    private BigDecimal calculatePercent(BigDecimal current, BigDecimal threshold) {
        if (threshold == null || threshold.signum() <= 0) return ZERO;
        return current.multiply(BigDecimal.valueOf(100L))
            .divide(threshold, 1, RoundingMode.HALF_UP)
            .min(BigDecimal.valueOf(100L));
    }
}
