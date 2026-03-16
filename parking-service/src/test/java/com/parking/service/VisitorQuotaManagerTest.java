package com.parking.service;

import com.parking.mapper.VisitorSessionMapper;
import com.parking.model.VisitorSession;
import com.parking.service.impl.VisitorQuotaManagerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * VisitorQuotaManager 单元测试
 * Validates: Requirements 7.2, 8.6, 8.7, 8.8, 8.9, 8.10, 10.2
 */
@ExtendWith(MockitoExtension.class)
class VisitorQuotaManagerTest {

    @Mock
    private VisitorSessionMapper visitorSessionMapper;

    @InjectMocks
    private VisitorQuotaManagerImpl visitorQuotaManager;

    private static final Long COMMUNITY_ID = 1001L;
    private static final String HOUSE_NO = "1-101";

    @Test
    @DisplayName("计算月度配额使用量 - 正常返回累计值")
    void calculateMonthlyUsage_shouldReturnSum() {
        YearMonth month = YearMonth.of(2026, 3);
        when(visitorSessionMapper.sumMonthlyDuration(COMMUNITY_ID, HOUSE_NO, 2026, 3))
                .thenReturn(1200L);

        long usage = visitorQuotaManager.calculateMonthlyUsage(COMMUNITY_ID, HOUSE_NO, month);

        assertEquals(1200L, usage);
        verify(visitorSessionMapper).sumMonthlyDuration(COMMUNITY_ID, HOUSE_NO, 2026, 3);
    }

    @Test
    @DisplayName("计算月度配额使用量 - 无记录返回0")
    void calculateMonthlyUsage_shouldReturnZeroWhenNull() {
        YearMonth month = YearMonth.of(2026, 3);
        when(visitorSessionMapper.sumMonthlyDuration(COMMUNITY_ID, HOUSE_NO, 2026, 3))
                .thenReturn(null);

        long usage = visitorQuotaManager.calculateMonthlyUsage(COMMUNITY_ID, HOUSE_NO, month);

        assertEquals(0L, usage);
    }

    @Test
    @DisplayName("检查配额充足 - 使用量低于上限")
    void checkQuotaSufficient_shouldReturnTrueWhenUnderLimit() {
        YearMonth now = YearMonth.now();
        when(visitorSessionMapper.sumMonthlyDuration(eq(COMMUNITY_ID), eq(HOUSE_NO),
                eq(now.getYear()), eq(now.getMonthValue())))
                .thenReturn(2000L); // 2000分钟 < 4320分钟

        assertTrue(visitorQuotaManager.checkQuotaSufficient(COMMUNITY_ID, HOUSE_NO));
    }

    @Test
    @DisplayName("检查配额不足 - 使用量达到上限")
    void checkQuotaSufficient_shouldReturnFalseWhenAtLimit() {
        YearMonth now = YearMonth.now();
        when(visitorSessionMapper.sumMonthlyDuration(eq(COMMUNITY_ID), eq(HOUSE_NO),
                eq(now.getYear()), eq(now.getMonthValue())))
                .thenReturn(4320L); // 恰好等于72小时

        assertFalse(visitorQuotaManager.checkQuotaSufficient(COMMUNITY_ID, HOUSE_NO));
    }

    @Test
    @DisplayName("检查配额不足 - 使用量超过上限")
    void checkQuotaSufficient_shouldReturnFalseWhenOverLimit() {
        YearMonth now = YearMonth.now();
        when(visitorSessionMapper.sumMonthlyDuration(eq(COMMUNITY_ID), eq(HOUSE_NO),
                eq(now.getYear()), eq(now.getMonthValue())))
                .thenReturn(5000L);

        assertFalse(visitorQuotaManager.checkQuotaSufficient(COMMUNITY_ID, HOUSE_NO));
    }

    @Test
    @DisplayName("累计停放时长 - 正常累计")
    void accumulateDuration_shouldAccumulateCorrectly() {
        Long sessionId = 100L;
        LocalDateTime entryTime = LocalDateTime.of(2026, 3, 15, 10, 0);
        LocalDateTime exitTime = LocalDateTime.of(2026, 3, 15, 12, 30);

        VisitorSession session = new VisitorSession();
        session.setId(sessionId);
        session.setLastEntryTime(entryTime);
        session.setAccumulatedDuration(60); // 之前已累计60分钟

        when(visitorSessionMapper.selectById(sessionId)).thenReturn(session);

        long duration = visitorQuotaManager.accumulateDuration(sessionId, exitTime);

        // 本次停放 2.5小时 = 150分钟
        assertEquals(150L, duration);
        // 累计 60 + 150 = 210分钟
        verify(visitorSessionMapper).updateDurationAndStatus(sessionId, 210, "out_of_park");
    }

    @Test
    @DisplayName("累计停放时长 - 首次出场使用 sessionStart")
    void accumulateDuration_shouldUseSessionStartWhenNoLastEntry() {
        Long sessionId = 100L;
        LocalDateTime sessionStart = LocalDateTime.of(2026, 3, 15, 9, 0);
        LocalDateTime exitTime = LocalDateTime.of(2026, 3, 15, 11, 0);

        VisitorSession session = new VisitorSession();
        session.setId(sessionId);
        session.setSessionStart(sessionStart);
        session.setLastEntryTime(null); // 无 lastEntryTime
        session.setAccumulatedDuration(0);

        when(visitorSessionMapper.selectById(sessionId)).thenReturn(session);

        long duration = visitorQuotaManager.accumulateDuration(sessionId, exitTime);

        // 2小时 = 120分钟
        assertEquals(120L, duration);
        verify(visitorSessionMapper).updateDurationAndStatus(sessionId, 120, "out_of_park");
    }

    @Test
    @DisplayName("累计停放时长 - 会话不存在返回0")
    void accumulateDuration_shouldReturnZeroWhenSessionNotFound() {
        when(visitorSessionMapper.selectById(999L)).thenReturn(null);

        long duration = visitorQuotaManager.accumulateDuration(999L, LocalDateTime.now());

        assertEquals(0L, duration);
        verify(visitorSessionMapper, never()).updateDurationAndStatus(anyLong(), anyInt(), anyString());
    }

    @Test
    @DisplayName("累计停放时长 - 出场时间早于入场时间按0处理")
    void accumulateDuration_shouldReturnZeroWhenExitBeforeEntry() {
        Long sessionId = 100L;
        LocalDateTime entryTime = LocalDateTime.of(2026, 3, 15, 12, 0);
        LocalDateTime exitTime = LocalDateTime.of(2026, 3, 15, 10, 0); // 早于入场

        VisitorSession session = new VisitorSession();
        session.setId(sessionId);
        session.setLastEntryTime(entryTime);
        session.setAccumulatedDuration(60);

        when(visitorSessionMapper.selectById(sessionId)).thenReturn(session);

        long duration = visitorQuotaManager.accumulateDuration(sessionId, exitTime);

        assertEquals(0L, duration);
        // 累计不变：60 + 0 = 60
        verify(visitorSessionMapper).updateDurationAndStatus(sessionId, 60, "out_of_park");
    }

    @Test
    @DisplayName("检查超时会话 - 返回超时列表")
    void checkTimeout_shouldReturnTimeoutSessions() {
        VisitorSession s1 = new VisitorSession();
        s1.setId(1L);
        s1.setAccumulatedDuration(1500);
        VisitorSession s2 = new VisitorSession();
        s2.setId(2L);
        s2.setAccumulatedDuration(1440);

        when(visitorSessionMapper.selectTimeoutSessions()).thenReturn(Arrays.asList(s1, s2));

        List<VisitorSession> result = visitorQuotaManager.checkTimeout();

        assertEquals(2, result.size());
        verify(visitorSessionMapper).selectTimeoutSessions();
    }

    @Test
    @DisplayName("检查超时会话 - 无超时返回空列表")
    void checkTimeout_shouldReturnEmptyWhenNoTimeout() {
        when(visitorSessionMapper.selectTimeoutSessions()).thenReturn(Collections.emptyList());

        List<VisitorSession> result = visitorQuotaManager.checkTimeout();

        assertTrue(result.isEmpty());
    }
}
