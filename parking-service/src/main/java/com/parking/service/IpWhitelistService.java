package com.parking.service;

import com.parking.model.IpWhitelist;

import java.util.List;

/**
 * IP 白名单管理服务接口
 * Validates: Requirements 20.4, 20.5, 20.6
 */
public interface IpWhitelistService {

    /**
     * 添加 IP 白名单
     */
    IpWhitelist addIpWhitelist(Long communityId, String ipAddress, String ipRange,
                                String operationType, String description, Long adminId);

    /**
     * 删除 IP 白名单（逻辑删除）
     */
    void deleteIpWhitelist(Long id);

    /**
     * 查询 IP 白名单列表
     */
    List<IpWhitelist> listIpWhitelist();
}
