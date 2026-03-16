package com.parking.service.impl;

import com.parking.mapper.VisitorSessionMapper;
import com.parking.model.VisitorSession;
import com.parking.service.VisitorQuotaManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Visitor 配额管理器实现
 * 负责月度配额计算、配额校验、时长累计、超时检测
 * Validates: Requirements 7.2, 8.6, 8.7, 8.8, 8.9, 8.10, 10.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitorQuotaManagerImpl implements VisitorQuotaManager {

    /**
     * 月度配额上限：72小时 = 4320分钟
     */
    private static final long MONTHLY_QUOTA_MINUTES = 72 * 60;

    /**
     * 单次授权累计时长上限：24小时 = 1440分钟
     */
    private static final long SINGLE_SESSION_TIMEOUT_MINUTES = 1440;

    private final VisitorSessionMapper visitorSessionMapper;

    @Override
    public long calculateMonthlyUsage(Long communityId, String houseNo, YearMonth month) {
        Long usage = visitorSessionMapper.sumMonthlyDuration(
                communityId, houseNo, month.getYear(), month.getMonthValue());
        return usage != null ? usage : 0L;
    }

    @Override
    public boolean checkQuotaSufficient(Long communityId, String houseNo) {
        YearMonth currentMonth = YearMonth.now();
        long usage = calculateMonthlyUsage(communityId, houseNo, currentMonth);
        boolean sufficient = usage < MONTHLY_QUOTA_MINUTES;
        if (!sufficient) {
            log.info("Visitor 月度配额不足, communityId={}, houseNo={}, 已使用={}分钟, 上限={}分钟",
                    communityId, houseNo, usage, MONTHLY_QUOTA_MINUTES);
        }
        return sufficient;
    }

    @Override
    public long accumulateDuration(Long visitorSessionId, LocalDateTime exitTime) {
        VisitorSession session = visitorSessionMapper.selectById(visitorSessionId);
        if (session == null) {
            log.warn("Visitor 会话不存在, sessionId={}", visitorSessionId);
            return 0;
        }

        // 计算本次停放时长 = exitTime - lastEntryTime
        LocalDateTime entryTime = session.getLastEntryTime();
        if (entryTime == null) {
            entryTime = session.getSessionStart();
        }

        long durationMinutes = Duration.between(entryTime, exitTime).toMinutes();
        if (durationMinutes < 0) {
            log.warn("出场时间早于入场时间, sessionId={}, entryTime={}, exitTime={}",
                    visitorSessionId, entryTime, exitTime);
            durationMinutes = 0;
        }

        // 累加到 accumulated_duration
        int newTotal = (session.getAccumulatedDuration() != null ? session.getAccumulatedDuration() : 0)
                + (int) durationMinutes;

        // 更新会话：累计时长 + 状态改为 out_of_park
        visitorSessionMapper.updateDurationAndStatus(visitorSessionId, newTotal, "out_of_park");

        log.info("Visitor 时长累计, sessionId={}, 本次={}分钟, 累计={}分钟",
                visitorSessionId, durationMinutes, newTotal);

        return durationMinutes;
    }

    @Override
    public List<VisitorSession> checkTimeout() {
        List<VisitorSession> timeoutSessions = visitorSessionMapper.selectTimeoutSessions();
        log.info("检测到 {} 个超时 Visitor 会话", timeoutSessions.size());
        return timeoutSessions;
    }
}
