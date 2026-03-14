package com.parking.service;

import com.parking.common.BusinessException;
import com.parking.service.impl.AntiReplayServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AntiReplayService 单元测试
 * Validates: Requirements 19.5, 19.6, 19.7, 19.8
 */
@ExtendWith(MockitoExtension.class)
class AntiReplayServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AntiReplayServiceImpl antiReplayService;

    @BeforeEach
    void setUp() {
        antiReplayService = new AntiReplayServiceImpl(stringRedisTemplate);
    }

    @Nested
    @DisplayName("validateTimestamp - 时间戳验证")
    class ValidateTimestampTests {

        @Test
        @DisplayName("当前时间戳应验证通过")
        void validateTimestamp_currentTime_shouldPass() {
            String timestamp = String.valueOf(System.currentTimeMillis());
            assertDoesNotThrow(() -> antiReplayService.validateTimestamp(timestamp));
        }

        @Test
        @DisplayName("4分钟前的时间戳应验证通过")
        void validateTimestamp_withinWindow_shouldPass() {
            long fourMinutesAgo = System.currentTimeMillis() - 4 * 60 * 1000L;
            String timestamp = String.valueOf(fourMinutesAgo);
            assertDoesNotThrow(() -> antiReplayService.validateTimestamp(timestamp));
        }

        @Test
        @DisplayName("6分钟前的时间戳应抛出 PARKING_19001")
        void validateTimestamp_outsideWindow_shouldThrow() {
            long sixMinutesAgo = System.currentTimeMillis() - 6 * 60 * 1000L;
            String timestamp = String.valueOf(sixMinutesAgo);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> antiReplayService.validateTimestamp(timestamp));
            assertEquals(19001, ex.getCode());
        }

        @Test
        @DisplayName("未来6分钟的时间戳应抛出 PARKING_19001")
        void validateTimestamp_futureOutsideWindow_shouldThrow() {
            long sixMinutesLater = System.currentTimeMillis() + 6 * 60 * 1000L;
            String timestamp = String.valueOf(sixMinutesLater);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> antiReplayService.validateTimestamp(timestamp));
            assertEquals(19001, ex.getCode());
        }

        @Test
        @DisplayName("null 时间戳应抛出 PARKING_19001")
        void validateTimestamp_null_shouldThrow() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> antiReplayService.validateTimestamp(null));
            assertEquals(19001, ex.getCode());
        }

        @Test
        @DisplayName("空字符串时间戳应抛出 PARKING_19001")
        void validateTimestamp_empty_shouldThrow() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> antiReplayService.validateTimestamp(""));
            assertEquals(19001, ex.getCode());
        }

        @Test
        @DisplayName("非数字时间戳应抛出 PARKING_19001")
        void validateTimestamp_nonNumeric_shouldThrow() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> antiReplayService.validateTimestamp("not_a_number"));
            assertEquals(19001, ex.getCode());
        }
    }

    @Nested
    @DisplayName("validateNonce - nonce 唯一性验证")
    class ValidateNonceTests {

        @Test
        @DisplayName("新 nonce 应验证通过并存入 Redis")
        void validateNonce_newNonce_shouldPass() {
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(
                    eq("anti_replay:nonce:abc123"),
                    eq("1"),
                    eq(Duration.ofMinutes(5))
            )).thenReturn(Boolean.TRUE);

            assertDoesNotThrow(() -> antiReplayService.validateNonce("abc123"));

            verify(valueOperations).setIfAbsent(
                    eq("anti_replay:nonce:abc123"),
                    eq("1"),
                    eq(Duration.ofMinutes(5))
            );
        }

        @Test
        @DisplayName("重复 nonce 应抛出 PARKING_19002")
        void validateNonce_duplicateNonce_shouldThrow() {
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(
                    eq("anti_replay:nonce:abc123"),
                    eq("1"),
                    eq(Duration.ofMinutes(5))
            )).thenReturn(Boolean.FALSE);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> antiReplayService.validateNonce("abc123"));
            assertEquals(19002, ex.getCode());
        }

        @Test
        @DisplayName("null nonce 应抛出 PARKING_19002")
        void validateNonce_null_shouldThrow() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> antiReplayService.validateNonce(null));
            assertEquals(19002, ex.getCode());
        }

        @Test
        @DisplayName("空字符串 nonce 应抛出 PARKING_19002")
        void validateNonce_empty_shouldThrow() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> antiReplayService.validateNonce(""));
            assertEquals(19002, ex.getCode());
        }
    }

    @Nested
    @DisplayName("validate - 完整防重放验证")
    class ValidateTests {

        @Test
        @DisplayName("有效 timestamp 和新 nonce 应验证通过")
        void validate_validTimestampAndNewNonce_shouldPass() {
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(Duration.ofMinutes(5))))
                    .thenReturn(Boolean.TRUE);

            String timestamp = String.valueOf(System.currentTimeMillis());
            assertDoesNotThrow(() -> antiReplayService.validate(timestamp, "unique_nonce_001"));
        }

        @Test
        @DisplayName("过期 timestamp 应直接抛出 PARKING_19001，不检查 nonce")
        void validate_expiredTimestamp_shouldThrowWithoutCheckingNonce() {
            long tenMinutesAgo = System.currentTimeMillis() - 10 * 60 * 1000L;
            String timestamp = String.valueOf(tenMinutesAgo);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> antiReplayService.validate(timestamp, "some_nonce"));
            assertEquals(19001, ex.getCode());

            // 不应调用 Redis（timestamp 验证失败后直接返回）
            verifyNoInteractions(stringRedisTemplate);
        }

        @Test
        @DisplayName("有效 timestamp 但重复 nonce 应抛出 PARKING_19002")
        void validate_validTimestampButDuplicateNonce_shouldThrow() {
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(Duration.ofMinutes(5))))
                    .thenReturn(Boolean.FALSE);

            String timestamp = String.valueOf(System.currentTimeMillis());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> antiReplayService.validate(timestamp, "duplicate_nonce"));
            assertEquals(19002, ex.getCode());
        }
    }
}
