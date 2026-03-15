package com.parking.service.impl;

import com.parking.common.BusinessException;
import com.parking.common.ErrorCode;
import com.parking.mapper.IpWhitelistMapper;
import com.parking.model.IpWhitelist;
import com.parking.service.CacheService;
import com.parking.service.IpWhitelistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * IP 白名单管理服务实现
 * Validates: Requirements 20.4, 20.5, 20.6, 20.7
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpWhitelistServiceImpl implements IpWhitelistService {

    private final IpWhitelistMapper ipWhitelistMapper;
    private final CacheService cacheService;

    @Override
    public IpWhitelist addIpWhitelist(Long communityId, String ipAddress, String ipRange,
                                       String operationType, String description, Long adminId) {
        log.info("添加 IP 白名单: communityId={}, ip={}, operationType={}", communityId, ipAddress, operationType);

        IpWhitelist whitelist = new IpWhitelist();
        whitelist.setCommunityId(communityId);
        whitelist.setIpAddress(ipAddress);
        whitelist.setIpRange(ipRange);
        whitelist.setOperationType(operationType);
        whitelist.setDescription(description);
        whitelist.setStatus("active");
        whitelist.setCreateAdminId(adminId);
        whitelist.setCreateTime(LocalDateTime.now());

        ipWhitelistMapper.insert(whitelist);
        log.info("IP 白名单已添加: id={}", whitelist.getId());

        // 失效缓存
        invalidateCache();

        return whitelist;
    }

    @Override
    public void deleteIpWhitelist(Long id) {
        log.info("删除 IP 白名单: id={}", id);

        IpWhitelist existing = ipWhitelistMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        int rows = ipWhitelistMapper.logicalDelete(id);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        log.info("IP 白名单已删除: id={}", id);

        // 失效缓存
        invalidateCache();
    }

    @Override
    public List<IpWhitelist> listIpWhitelist() {
        return ipWhitelistMapper.selectAll();
    }

    /**
     * 失效 IP 白名单缓存
     */
    private void invalidateCache() {
        cacheService.deleteByPrefix("ip_whitelist:");
    }
}
