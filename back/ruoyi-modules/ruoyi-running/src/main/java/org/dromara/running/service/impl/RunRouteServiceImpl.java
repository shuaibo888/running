package org.dromara.running.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.running.domain.RunUserRouteProgress;
import org.dromara.running.domain.RunVirtualRoute;
import org.dromara.running.domain.RunVirtualRouteNode;
import org.dromara.running.domain.RunWorkout;
import org.dromara.running.domain.vo.RunRouteNodeVo;
import org.dromara.running.domain.vo.RunRouteVo;
import org.dromara.running.mapper.RunUserRouteProgressMapper;
import org.dromara.running.mapper.RunVirtualRouteMapper;
import org.dromara.running.mapper.RunVirtualRouteNodeMapper;
import org.dromara.running.service.IRunPointService;
import org.dromara.running.service.IRunRouteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 文化线路选择、展示和真实里程累计。 */
@RequiredArgsConstructor
@Service
public class RunRouteServiceImpl implements IRunRouteService {

    private static final String ENABLED = "0";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final RunVirtualRouteMapper routeMapper;
    private final RunVirtualRouteNodeMapper nodeMapper;
    private final RunUserRouteProgressMapper progressMapper;
    private final IRunPointService pointService;

    @Override
    public List<RunRouteVo> listRoutes(Long userId) {
        List<RunVirtualRoute> routes = routeMapper.selectList(Wrappers.<RunVirtualRoute>lambdaQuery()
            .eq(RunVirtualRoute::getStatus, ENABLED)
            .orderByAsc(RunVirtualRoute::getSortOrder)
            .orderByAsc(RunVirtualRoute::getId));
        if (routes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, RunUserRouteProgress> progressByRoute = progressMapper.selectList(
                Wrappers.<RunUserRouteProgress>lambdaQuery().eq(RunUserRouteProgress::getUserId, userId))
            .stream()
            .collect(Collectors.toMap(RunUserRouteProgress::getRouteId, Function.identity()));
        return routes.stream().map(route -> toVo(route, progressByRoute.get(route.getId()), false)).toList();
    }

    @Override
    public RunRouteVo getRoute(Long userId, Long routeId) {
        RunVirtualRoute route = requireEnabledRoute(routeId);
        RunUserRouteProgress progress = progressMapper.selectOne(Wrappers.<RunUserRouteProgress>lambdaQuery()
            .eq(RunUserRouteProgress::getUserId, userId)
            .eq(RunUserRouteProgress::getRouteId, routeId));
        return toVo(route, progress, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RunRouteVo selectCurrentRoute(Long userId, Long routeId) {
        RunVirtualRoute route = requireEnabledRoute(routeId);
        progressMapper.update(null, Wrappers.<RunUserRouteProgress>lambdaUpdate()
            .eq(RunUserRouteProgress::getUserId, userId)
            .eq(RunUserRouteProgress::getSelectedFlag, 1)
            .set(RunUserRouteProgress::getSelectedFlag, null));

        RunUserRouteProgress progress = progressMapper.selectOne(Wrappers.<RunUserRouteProgress>lambdaQuery()
            .eq(RunUserRouteProgress::getUserId, userId)
            .eq(RunUserRouteProgress::getRouteId, routeId)
            .last("FOR UPDATE"));
        if (progress == null) {
            progress = new RunUserRouteProgress();
            progress.setUserId(userId);
            progress.setRouteId(routeId);
            progress.setAccumulatedDistanceMeters(ZERO);
            progress.setReachedNodeOrder(0);
            progress.setSelectedFlag(1);
            refreshReachedNode(progress);
            if (progressMapper.insert(progress) < 1) {
                throw new ServiceException("线路选择失败，请重试");
            }
        } else {
            progress.setSelectedFlag(1);
            if (progressMapper.updateById(progress) < 1) {
                throw new ServiceException("线路选择冲突，请刷新后重试");
            }
        }
        return toVo(route, progress, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long resolveSelectedRouteId(Long userId) {
        RunUserRouteProgress selected = progressMapper.selectOne(Wrappers.<RunUserRouteProgress>lambdaQuery()
            .eq(RunUserRouteProgress::getUserId, userId)
            .eq(RunUserRouteProgress::getSelectedFlag, 1));
        if (selected != null) {
            RunVirtualRoute route = routeMapper.selectOne(Wrappers.<RunVirtualRoute>lambdaQuery()
                .eq(RunVirtualRoute::getId, selected.getRouteId())
                .eq(RunVirtualRoute::getStatus, ENABLED));
            if (route != null) {
                return route.getId();
            }
        }
        RunVirtualRoute first = routeMapper.selectOne(Wrappers.<RunVirtualRoute>lambdaQuery()
            .eq(RunVirtualRoute::getStatus, ENABLED)
            .orderByAsc(RunVirtualRoute::getSortOrder)
            .orderByAsc(RunVirtualRoute::getId)
            .last("LIMIT 1"));
        return first == null ? null : selectCurrentRoute(userId, first.getId()).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyCompletedWorkout(Long userId, RunWorkout workout) {
        if (workout.getRouteId() == null || workout.getDistanceMeters() == null
            || workout.getDistanceMeters().signum() <= 0) {
            return;
        }
        RunUserRouteProgress progress = progressMapper.selectOne(Wrappers.<RunUserRouteProgress>lambdaQuery()
            .eq(RunUserRouteProgress::getUserId, userId)
            .eq(RunUserRouteProgress::getRouteId, workout.getRouteId())
            .last("FOR UPDATE"));
        int previousReachedNodeOrder = progress == null ? 0 : progress.getReachedNodeOrder();
        if (progress == null) {
            progress = new RunUserRouteProgress();
            progress.setUserId(userId);
            progress.setRouteId(workout.getRouteId());
            progress.setAccumulatedDistanceMeters(workout.getDistanceMeters());
            progress.setReachedNodeOrder(0);
            progress.setSelectedFlag(null);
            progress.setLastWorkoutId(workout.getId());
            refreshReachedNode(progress);
            if (progressMapper.insert(progress) < 1) {
                throw new ServiceException("文化线路进度保存失败");
            }
        } else {
            if (workout.getId().equals(progress.getLastWorkoutId())) {
                return;
            }
            progress.setAccumulatedDistanceMeters(progress.getAccumulatedDistanceMeters().add(workout.getDistanceMeters()));
            progress.setLastWorkoutId(workout.getId());
            refreshReachedNode(progress);
            if (progressMapper.updateById(progress) < 1) {
                throw new ServiceException("文化线路进度更新冲突，请重试结束运动");
            }
        }
        awardNewlyReachedNodes(userId, workout, previousReachedNodeOrder, progress.getReachedNodeOrder());
    }

    private void awardNewlyReachedNodes(Long userId, RunWorkout workout,
                                        int previousReachedNodeOrder, Integer reachedNodeOrder) {
        int currentOrder = reachedNodeOrder == null ? 0 : reachedNodeOrder;
        if (currentOrder <= previousReachedNodeOrder) {
            return;
        }
        nodeMapper.selectList(Wrappers.<RunVirtualRouteNode>lambdaQuery()
                .eq(RunVirtualRouteNode::getRouteId, workout.getRouteId())
                .gt(RunVirtualRouteNode::getSortOrder, previousReachedNodeOrder)
                .le(RunVirtualRouteNode::getSortOrder, currentOrder)
                .gt(RunVirtualRouteNode::getRewardPoints, 0)
                .orderByAsc(RunVirtualRouteNode::getSortOrder))
            .forEach(node -> pointService.awardRouteNode(
                userId, workout.getId(), workout.getRouteId(), node));
    }

    private void refreshReachedNode(RunUserRouteProgress progress) {
        RunVirtualRouteNode reached = nodeMapper.selectOne(Wrappers.<RunVirtualRouteNode>lambdaQuery()
            .eq(RunVirtualRouteNode::getRouteId, progress.getRouteId())
            .le(RunVirtualRouteNode::getThresholdDistanceMeters, progress.getAccumulatedDistanceMeters())
            .orderByDesc(RunVirtualRouteNode::getSortOrder)
            .last("LIMIT 1"));
        progress.setReachedNodeOrder(reached == null ? 0 : reached.getSortOrder());
    }

    private RunVirtualRoute requireEnabledRoute(Long routeId) {
        RunVirtualRoute route = routeMapper.selectOne(Wrappers.<RunVirtualRoute>lambdaQuery()
            .eq(RunVirtualRoute::getId, routeId)
            .eq(RunVirtualRoute::getStatus, ENABLED));
        if (route == null) {
            throw new ServiceException("文化线路不存在或已下线");
        }
        return route;
    }

    private RunRouteVo toVo(RunVirtualRoute route, RunUserRouteProgress progress, boolean includeNodes) {
        RunRouteVo vo = new RunRouteVo();
        vo.setId(route.getId());
        vo.setRouteCode(route.getRouteCode());
        vo.setRouteName(route.getRouteName());
        vo.setSubtitle(route.getSubtitle());
        vo.setDescription(route.getDescription());
        vo.setStartCity(route.getStartCity());
        vo.setEndCity(route.getEndCity());
        vo.setTotalDistanceMeters(route.getTotalDistanceMeters());
        vo.setThemeColor(route.getThemeColor());
        vo.setCoverUrl(route.getCoverUrl());
        BigDecimal accumulated = progress == null ? ZERO : progress.getAccumulatedDistanceMeters();
        vo.setAccumulatedDistanceMeters(accumulated);
        vo.setProgressPercent(calculatePercent(accumulated, route.getTotalDistanceMeters()));
        vo.setReachedNodeOrder(progress == null ? 0 : progress.getReachedNodeOrder());
        vo.setSelected(progress != null && Integer.valueOf(1).equals(progress.getSelectedFlag()));
        if (includeNodes) {
            vo.setNodes(nodeMapper.selectList(Wrappers.<RunVirtualRouteNode>lambdaQuery()
                    .eq(RunVirtualRouteNode::getRouteId, route.getId())
                    .orderByAsc(RunVirtualRouteNode::getSortOrder))
                .stream().map(node -> toNodeVo(node, accumulated)).toList());
        }
        return vo;
    }

    private RunRouteNodeVo toNodeVo(RunVirtualRouteNode node, BigDecimal accumulated) {
        RunRouteNodeVo vo = new RunRouteNodeVo();
        vo.setId(node.getId());
        vo.setNodeCode(node.getNodeCode());
        vo.setNodeName(node.getNodeName());
        vo.setThresholdDistanceMeters(node.getThresholdDistanceMeters());
        vo.setStoryTitle(node.getStoryTitle());
        vo.setStoryContent(node.getStoryContent());
        vo.setMedalName(node.getMedalName());
        vo.setRewardPoints(node.getRewardPoints());
        vo.setSortOrder(node.getSortOrder());
        vo.setUnlocked(accumulated.compareTo(node.getThresholdDistanceMeters()) >= 0);
        return vo;
    }

    private BigDecimal calculatePercent(BigDecimal accumulated, BigDecimal total) {
        if (total == null || total.signum() <= 0) {
            return ZERO;
        }
        BigDecimal percent = accumulated.multiply(BigDecimal.valueOf(100L))
            .divide(total, 1, RoundingMode.HALF_UP);
        return percent.min(BigDecimal.valueOf(100L));
    }
}
