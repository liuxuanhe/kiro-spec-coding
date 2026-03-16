package com.parking.service;

import com.parking.common.BusinessException;
import com.parking.service.impl.VerificationCodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * VerificationCodeService 单元测试
 * Validates: Requirements 1.2, 1.3
 */
@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private VerificationCodeServiceImpl verificationCodeService;

    private static final String TEST_PHONE = "13812345678";
    private static final String CODE_KEY = "verification_code:" + TEST_PHONE;
    private static final String FAIL_COUNT_KEY = "verification_fail_count:" + TEST_PHONE;

    @BeforeEach
    void setUp() {
        verificationCodeService = new VerificationCodeServiceImpl(redisTemplate);
    }

    @Nested
    @DisplayName("send - 发送验证码")
    class SendTests {

        @Test
        @DisplayName("发送验证码应存入 Redis 并设置5分钟过期")
        void send_shouldStoreCodeInRedisWithExpiry() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            verificationCodeService.send(TEST_PHONE);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
            verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(),
                    eq(5L), eq(TimeUnit.MINUTES));

            assertEquals(CODE_KEY, keyCaptor.getValue());
            // 验证码应为6位数字
            String code = valueCaptor.getValue().toString();
            assertEquals(6, code.length());
            assertTrue(code.matches("\\d{6}"), "验证码应为6位数字");
        }
    }


    @Nested
    @DisplayName("verify - 验证码校验")
    class VerifyTests {

        @Test
        @DisplayName("验证码正确时应返回 true 并删除验证码和失败计数")
        void verify_correctCode_shouldReturnTrueAndCleanup() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            // 失败次数为0
            when(valueOperations.get(FAIL_COUNT_KEY)).thenReturn(null);
            // 存储的验证码
            when(valueOperations.get(CODE_KEY)).thenReturn("123456");
            when(redisTemplate.delete(CODE_KEY)).thenReturn(true);
            when(redisTemplate.delete(FAIL_COUNT_KEY)).thenReturn(true);

            boolean result = verificationCodeService.verify(TEST_PHONE, "123456");

            assertTrue(result);
            verify(redisTemplate).delete(CODE_KEY);
            verify(redisTemplate).delete(FAIL_COUNT_KEY);
        }

        @Test
        @DisplayName("验证码错误时应返回 false 并增加失败次数")
        void verify_wrongCode_shouldReturnFalseAndIncrementFailCount() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(FAIL_COUNT_KEY)).thenReturn(null);
            when(valueOperations.get(CODE_KEY)).thenReturn("123456");
            when(valueOperations.increment(FAIL_COUNT_KEY)).thenReturn(1L);
            when(redisTemplate.expire(FAIL_COUNT_KEY, 10L, TimeUnit.MINUTES)).thenReturn(true);

            boolean result = verificationCodeService.verify(TEST_PHONE, "654321");

            assertFalse(result);
            verify(valueOperations).increment(FAIL_COUNT_KEY);
            verify(redisTemplate).expire(FAIL_COUNT_KEY, 10L, TimeUnit.MINUTES);
        }

        @Test
        @DisplayName("验证码过期时应抛出 PARKING_1002 异常")
        void verify_expiredCode_shouldThrowParking1002() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(FAIL_COUNT_KEY)).thenReturn(null);
            // 验证码不存在（已过期）
            when(valueOperations.get(CODE_KEY)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> verificationCodeService.verify(TEST_PHONE, "123456"));

            assertEquals(1002, exception.getCode());
        }

        @Test
        @DisplayName("失败次数达到3次时应抛出 PARKING_1001 锁定异常")
        void verify_thirdFailure_shouldThrowParking1001() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(FAIL_COUNT_KEY)).thenReturn(null);
            when(valueOperations.get(CODE_KEY)).thenReturn("123456");
            // 第3次失败
            when(valueOperations.increment(FAIL_COUNT_KEY)).thenReturn(3L);
            when(redisTemplate.expire(FAIL_COUNT_KEY, 10L, TimeUnit.MINUTES)).thenReturn(true);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> verificationCodeService.verify(TEST_PHONE, "wrong_code"));

            assertEquals(1001, exception.getCode());
        }

        @Test
        @DisplayName("已锁定状态下调用 verify 应直接抛出 PARKING_1001")
        void verify_alreadyLocked_shouldThrowParking1001() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            // 失败次数已达3次
            when(valueOperations.get(FAIL_COUNT_KEY)).thenReturn(3);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> verificationCodeService.verify(TEST_PHONE, "123456"));

            assertEquals(1001, exception.getCode());
            // 不应查询验证码
            verify(valueOperations, never()).get(CODE_KEY);
        }

        @Test
        @DisplayName("失败次数超过3次时仍应保持锁定")
        void verify_failCountExceedsMax_shouldStayLocked() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(FAIL_COUNT_KEY)).thenReturn(5);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> verificationCodeService.verify(TEST_PHONE, "123456"));

            assertEquals(1001, exception.getCode());
        }

        @Test
        @DisplayName("第2次失败不应锁定，应返回 false")
        void verify_secondFailure_shouldNotLock() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(FAIL_COUNT_KEY)).thenReturn(null);
            when(valueOperations.get(CODE_KEY)).thenReturn("123456");
            when(valueOperations.increment(FAIL_COUNT_KEY)).thenReturn(2L);
            when(redisTemplate.expire(FAIL_COUNT_KEY, 10L, TimeUnit.MINUTES)).thenReturn(true);

            boolean result = verificationCodeService.verify(TEST_PHONE, "wrong_code");

            assertFalse(result);
        }

        @Test
        @DisplayName("验证成功后失败计数应被清除")
        void verify_success_shouldClearFailCount() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(FAIL_COUNT_KEY)).thenReturn(2);
            when(valueOperations.get(CODE_KEY)).thenReturn("999999");
            when(redisTemplate.delete(CODE_KEY)).thenReturn(true);
            when(redisTemplate.delete(FAIL_COUNT_KEY)).thenReturn(true);

            boolean result = verificationCodeService.verify(TEST_PHONE, "999999");

            assertTrue(result);
            verify(redisTemplate).delete(FAIL_COUNT_KEY);
        }
    }
}
