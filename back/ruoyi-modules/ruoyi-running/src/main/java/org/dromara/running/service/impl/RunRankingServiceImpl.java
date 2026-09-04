package org.dromara.running.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.running.domain.RunUserProfile;
import org.dromara.running.domain.vo.RunRankingCacheVo;
import org.dromara.running.domain.vo.RunRankingEntryVo;
import org.dromara.running.domain.vo.RunRankingVo;
import org.dromara.running.mapper.RunRankingMapper;
import org.dromara.running.mapper.RunUserProfileMapper;
import org.dromara.running.service.IRunRankingService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

/** MySQL 可重建、Redis 短缓存的地区榜。 */
@RequiredArgsConstructor
@Service
public class RunRankingServiceImpl implements IRunRankingService {

    private static final Set<String> SCOPES = Set.of("CITY", "PROVINCE");
    private static final Set<String> PERIODS = Set.of("DAY", "WEEK", "MONTH", "ALL");

    private final RunRankingMapper rankingMapper;
    private final RunUserProfileMapper profileMapper;

    @Override
    public RunRankingVo getRanking(Long userId, String rawScope, String rawPeriod) {
        String scope = normalize(rawScope, "CITY", SCOPES, "排行榜地区范围不正确");
        String period = normalize(rawPeriod, "WEEK", PERIODS, "排行榜周期不正确");
        RunUserProfile profile = profileMapper.selectOne(Wrappers.<RunUserProfile>lambdaQuery()
            .eq(RunUserProfile::getUserId, userId));
        if (profile == null) {
            throw new ServiceException("请先完善运动档案和常住地区");
        }
        String regionCode = "CITY".equals(scope) ? profile.getCityCode() : profile.getProvinceCode();
        String regionName = "CITY".equals(scope) ? profile.getCityName() : profile.getProvinceName();
        if (StringUtils.isBlank(regionCode) || StringUtils.isBlank(regionName)) {
            throw new ServiceException("请先在运动档案中选择常住地区");
        }

        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) tenantId = TenantConstants.DEFAULT_TENANT_ID;
        Date startAt = periodStart(period);
        String cacheKey = GlobalConstants.GLOBAL_REDIS_KEY + "running:ranking:"
            + tenantId + ":" + scope + ":" + period + ":" + regionCode;
        RunRankingCacheVo cache = RedisUtils.getCacheObject(cacheKey);
        if (cache == null) {
            cache = new RunRankingCacheVo();
            cache.setGeneratedAt(new Date());
            cache.setEntries(rankingMapper.selectTop(tenantId, scope, regionCode, startAt));
            RedisUtils.setCacheObject(cacheKey, cache, java.time.Duration.ofSeconds(60));
        }
        RunRankingEntryVo currentUser = rankingMapper.selectCurrentUser(
            tenantId, scope, regionCode, startAt, userId);
        if (currentUser != null) currentUser.setCurrentUser(true);
        RunRankingEntryVo currentUserResult = currentUser;
        cache.getEntries().forEach(entry -> entry.setCurrentUser(
            currentUserResult != null && currentUserResult.getRankNo().equals(entry.getRankNo())));

        RunRankingVo result = new RunRankingVo();
        result.setScope(scope);
        result.setPeriod(period);
        result.setRegionCode(regionCode);
        result.setRegionName(regionName);
        result.setPeriodStartedAt(startAt);
        result.setGeneratedAt(cache.getGeneratedAt());
        result.setTieBreakRule("卡路里降序、距离降序、运动次数降序、用户稳定编号升序");
        result.setRegionRule("成绩按运动创建时的常住地区快照归属，修改档案不会搬动历史成绩");
        result.setEntries(cache.getEntries());
        result.setCurrentUserEntry(currentUser);
        return result;
    }

    private String normalize(String value, String defaultValue, Set<String> supported, String errorMessage) {
        String normalized = StringUtils.isBlank(value) ? defaultValue : value.toUpperCase(Locale.ROOT);
        if (!supported.contains(normalized)) throw new ServiceException(errorMessage);
        return normalized;
    }

    private Date periodStart(String period) {
        if ("ALL".equals(period)) return null;
        LocalDate today = LocalDate.now();
        LocalDate start = switch (period) {
            case "DAY" -> today;
            case "WEEK" -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "MONTH" -> today.withDayOfMonth(1);
            default -> throw new ServiceException("排行榜周期不正确");
        };
        return Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
