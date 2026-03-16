package com.parking.service;

import com.parking.dto.EntryRequest;
import com.parking.dto.EntryResponse;

/**
 * 车辆入场服务接口
 * 处理 Primary 车辆自动入场逻辑
 * Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9, 5.10, 15.2
 */
public interface EntryService {

    /**
     * 车辆入场
     * 幂等检查 → 查询车牌状态 → 分布式锁 → 车位校验 → 创建入场记录 → 设置幂等键 → 失效缓存 → 记录日志
     *
     * @param request 入场请求
     * @return 入场响应
     */
    EntryResponse vehicleEntry(EntryRequest request);
}
