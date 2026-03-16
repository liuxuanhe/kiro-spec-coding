package com.parking.service;

import com.parking.dto.CommunityCreateRequest;
import com.parking.model.Community;

import java.util.List;

/**
 * 小区服务接口
 * 提供小区列表查询、创建、更新与切换功能
 */
public interface CommunityService {

    /**
     * 查询小区列表
     * Super_Admin 返回所有小区，Property_Admin 仅返回本小区
     *
     * @param role        当前用户角色
     * @param communityId 当前用户所属小区ID
     * @return 小区列表
     */
    List<Community> listCommunities(String role, Long communityId);

    /**
     * 创建小区
     * 仅 Super_Admin 可执行
     *
     * @param request   创建请求
     * @param operatorId 操作人ID
     * @return 创建后的小区实体
     */
    Community createCommunity(CommunityCreateRequest request, Long operatorId);

    /**
     * 更新小区信息
     * 仅 Super_Admin 可执行
     *
     * @param id         小区ID
     * @param request    更新请求
     * @param operatorId 操作人ID
     * @return 更新后的小区实体
     */
    Community updateCommunity(Long id, CommunityCreateRequest request, Long operatorId);

    /**
     * Super_Admin 切换当前操作小区
     *
     * @param adminId           当前管理员ID
     * @param targetCommunityId 目标小区ID
     * @return 新的 Access Token
     */
    String switchCommunity(Long adminId, Long targetCommunityId);
}
