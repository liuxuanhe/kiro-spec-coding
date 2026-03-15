package com.parking.service;

import com.parking.common.BusinessException;
import com.parking.mapper.IpWhitelistMapper;
import com.parking.model.IpWhitelist;
import com.parking.service.impl.IpWhitelistServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * IpWhitelistServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class IpWhitelistServiceTest {

    @Mock
    private IpWhitelistMapper ipWhitelistMapper;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private IpWhitelistServiceImpl ipWhitelistService;

    @Test
    @DisplayName("添加 IP 白名单 - 成功")
    void addIpWhitelist_success() {
        doAnswer(invocation -> {
            IpWhitelist w = invocation.getArgument(0);
            w.setId(1L);
            return null;
        }).when(ipWhitelistMapper).insert(any(IpWhitelist.class));

        IpWhitelist result = ipWhitelistService.addIpWhitelist(
                1001L, "192.168.1.100", null, "export_raw_data", "测试白名单", 100L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("192.168.1.100", result.getIpAddress());
        assertEquals("active", result.getStatus());
        assertEquals("export_raw_data", result.getOperationType());
        verify(cacheService).deleteByPrefix("ip_whitelist:");
    }

    @Test
    @DisplayName("添加 IP 白名单 - 含 CIDR 范围")
    void addIpWhitelist_withCidr() {
        doAnswer(invocation -> {
            IpWhitelist w = invocation.getArgument(0);
            w.setId(2L);
            return null;
        }).when(ipWhitelistMapper).insert(any(IpWhitelist.class));

        IpWhitelist result = ipWhitelistService.addIpWhitelist(
                1001L, null, "192.168.1.0/24", "modify_parking_config", "办公网段", 100L);

        assertNotNull(result);
        assertEquals("192.168.1.0/24", result.getIpRange());
    }

    @Test
    @DisplayName("删除 IP 白名单 - 成功")
    void deleteIpWhitelist_success() {
        IpWhitelist existing = new IpWhitelist();
        existing.setId(1L);
        when(ipWhitelistMapper.selectById(1L)).thenReturn(existing);
        when(ipWhitelistMapper.logicalDelete(1L)).thenReturn(1);

        assertDoesNotThrow(() -> ipWhitelistService.deleteIpWhitelist(1L));

        verify(ipWhitelistMapper).logicalDelete(1L);
        verify(cacheService).deleteByPrefix("ip_whitelist:");
    }

    @Test
    @DisplayName("删除 IP 白名单 - 不存在抛异常")
    void deleteIpWhitelist_notFound() {
        when(ipWhitelistMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> ipWhitelistService.deleteIpWhitelist(999L));
    }

    @Test
    @DisplayName("查询 IP 白名单列表")
    void listIpWhitelist_success() {
        IpWhitelist w1 = new IpWhitelist();
        w1.setId(1L);
        w1.setIpAddress("192.168.1.1");
        IpWhitelist w2 = new IpWhitelist();
        w2.setId(2L);
        w2.setIpAddress("10.0.0.1");
        when(ipWhitelistMapper.selectAll()).thenReturn(List.of(w1, w2));

        List<IpWhitelist> result = ipWhitelistService.listIpWhitelist();

        assertEquals(2, result.size());
    }
}
