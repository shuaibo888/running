package org.dromara.running.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.running.domain.RunPointAccount;
import org.dromara.running.domain.RunPointTransaction;
import org.dromara.running.domain.RunVirtualRouteNode;
import org.dromara.running.domain.vo.RunPointOverviewVo;
import org.dromara.running.domain.vo.RunPointTransactionVo;
import org.dromara.running.mapper.RunPointAccountMapper;
import org.dromara.running.mapper.RunPointTransactionMapper;
import org.dromara.running.service.IRunPointService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** 积分账户、签到和业务奖励的统一记账实现。 */
@RequiredArgsConstructor
@Service
public class RunPointServiceImpl implements IRunPointService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String CHECKIN = "CHECKIN";
    private static final String ROUTE_NODE = "ROUTE_NODE";
    private static final String ACHIEVEMENT = "ACHIEVEMENT";
    private static final int NORMAL_CHECKIN_POINTS = 20;
    private static final int STREAK_CHECKIN_POINTS = 40;
    private static final int STREAK_REWARD_START_DAY = 7;

    private final RunPointAccountMapper accountMapper;
    private final RunPointTransactionMapper transactionMapper;

    @Override
    public RunPointOverviewVo getOverview(Long userId) {
        RunPointAccount account = findAccount(userId, false);
        return buildOverview(userId, account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RunPointOverviewVo checkIn(Long userId) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        String bizKey = today.toString();
        if (findTransaction(userId, CHECKIN, bizKey) != null) {
            return buildOverview(userId, findAccount(userId, false));
        }

        RunPointAccount account = getOrCreateAccountForUpdate(userId);
        if (today.equals(account.getLastCheckinDate())) {
            return buildOverview(userId, account);
        }
        int streak = today.minusDays(1).equals(account.getLastCheckinDate())
            ? valueOrZero(account.getCurrentCheckinStreak()) + 1
            : 1;
        int points = checkinPoints(streak);
        account.setCurrentCheckinStreak(streak);
        account.setLastCheckinDate(today);
        addPoints(account, points);
        saveTransaction(userId, CHECKIN, bizKey, points, account.getBalance(),
            "每日签到", "连续签到第" + streak + "天", null);
        return buildOverview(userId, account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void awardRouteNode(Long userId, Long workoutId, Long routeId, RunVirtualRouteNode node) {
        if (node == null || valueOrZero(node.getRewardPoints()) <= 0) {
            return;
        }
        awardOnce(userId, ROUTE_NODE, routeId + ":" + node.getId(), node.getRewardPoints(),
            "线路节点奖励", "抵达【" + node.getNodeName() + "】", workoutId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void awardAchievement(Long userId, Long workoutId, Long achievementId,
                                 String achievementName, Integer rewardPoints) {
        if (valueOrZero(rewardPoints) <= 0) {
            return;
        }
        awardOnce(userId, ACHIEVEMENT, String.valueOf(achievementId), rewardPoints,
            "成就奖励", "解锁【" + achievementName + "】", workoutId);
    }

    private void awardOnce(Long userId, String bizType, String bizKey, int points,
                           String title, String description, Long workoutId) {
        if (findTransaction(userId, bizType, bizKey) != null) {
            return;
        }
        RunPointAccount account = getOrCreateAccountForUpdate(userId);
        // 获取账户锁之后再次检查，避免并发请求重复发放。
        if (findTransaction(userId, bizType, bizKey) != null) {
            return;
        }
        addPoints(account, points);
        saveTransaction(userId, bizType, bizKey, points, account.getBalance(),
            title, description, workoutId);
    }

    private RunPointAccount getOrCreateAccountForUpdate(Long userId) {
        RunPointAccount account = findAccount(userId, true);
        if (account != null) {
            return account;
        }
        account = new RunPointAccount();
        account.setUserId(userId);
        account.setBalance(0);
        account.setTotalEarned(0);
        account.setCurrentCheckinStreak(0);
        account.setVersion(0L);
        if (accountMapper.insert(account) < 1) {
            throw new ServiceException("积分账户创建失败，请重试");
        }
        return account;
    }

    private RunPointAccount findAccount(Long userId, boolean forUpdate) {
        var query = Wrappers.<RunPointAccount>lambdaQuery()
            .eq(RunPointAccount::getUserId, userId);
        if (forUpdate) {
            query.last("FOR UPDATE");
        }
        return accountMapper.selectOne(query);
    }

    private RunPointTransaction findTransaction(Long userId, String bizType, String bizKey) {
        return transactionMapper.selectOne(Wrappers.<RunPointTransaction>lambdaQuery()
            .eq(RunPointTransaction::getUserId, userId)
            .eq(RunPointTransaction::getBizType, bizType)
            .eq(RunPointTransaction::getBizKey, bizKey));
    }

    private void addPoints(RunPointAccount account, int points) {
        account.setBalance(valueOrZero(account.getBalance()) + points);
        account.setTotalEarned(valueOrZero(account.getTotalEarned()) + points);
        if (accountMapper.updateById(account) < 1) {
            throw new ServiceException("积分账户更新冲突，请重试");
        }
    }

    private void saveTransaction(Long userId, String bizType, String bizKey, int points,
                                 int balanceAfter, String title, String description,
                                 Long workoutId) {
        RunPointTransaction transaction = new RunPointTransaction();
        transaction.setUserId(userId);
        transaction.setBizType(bizType);
        transaction.setBizKey(bizKey);
        transaction.setDeltaPoints(points);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setTitle(title);
        transaction.setDescription(description);
        transaction.setOccurredAt(new Date());
        transaction.setSourceWorkoutId(workoutId);
        if (transactionMapper.insert(transaction) < 1) {
            throw new ServiceException("积分流水保存失败，请重试");
        }
    }

    private RunPointOverviewVo buildOverview(Long userId, RunPointAccount account) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        boolean checkedInToday = account != null && today.equals(account.getLastCheckinDate());
        int streak = account == null ? 0 : valueOrZero(account.getCurrentCheckinStreak());
        int todayReward;
        if (checkedInToday) {
            RunPointTransaction todayTransaction = findTransaction(userId, CHECKIN, today.toString());
            todayReward = todayTransaction == null ? checkinPoints(streak) : todayTransaction.getDeltaPoints();
        } else {
            int nextStreak = account != null && today.minusDays(1).equals(account.getLastCheckinDate())
                ? streak + 1 : 1;
            todayReward = checkinPoints(nextStreak);
        }

        RunPointOverviewVo overview = new RunPointOverviewVo();
        overview.setBalance(account == null ? 0 : valueOrZero(account.getBalance()));
        overview.setTotalEarned(account == null ? 0 : valueOrZero(account.getTotalEarned()));
        overview.setCurrentCheckinStreak(streak);
        overview.setCheckedInToday(checkedInToday);
        overview.setTodayCheckinReward(todayReward);
        overview.setRecentTransactions(recentTransactions(userId));
        return overview;
    }

    private List<RunPointTransactionVo> recentTransactions(Long userId) {
        List<RunPointTransaction> transactions = transactionMapper.selectList(
            Wrappers.<RunPointTransaction>lambdaQuery()
                .eq(RunPointTransaction::getUserId, userId)
                .orderByDesc(RunPointTransaction::getOccurredAt)
                .orderByDesc(RunPointTransaction::getId)
                .last("LIMIT 50"));
        if (transactions.isEmpty()) {
            return Collections.emptyList();
        }
        return transactions.stream().map(this::toVo).toList();
    }

    private RunPointTransactionVo toVo(RunPointTransaction transaction) {
        RunPointTransactionVo vo = new RunPointTransactionVo();
        vo.setId(transaction.getId());
        vo.setBizType(transaction.getBizType());
        vo.setDeltaPoints(transaction.getDeltaPoints());
        vo.setBalanceAfter(transaction.getBalanceAfter());
        vo.setTitle(transaction.getTitle());
        vo.setDescription(transaction.getDescription());
        vo.setOccurredAt(transaction.getOccurredAt());
        return vo;
    }

    private int checkinPoints(int streak) {
        return streak >= STREAK_REWARD_START_DAY ? STREAK_CHECKIN_POINTS : NORMAL_CHECKIN_POINTS;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
