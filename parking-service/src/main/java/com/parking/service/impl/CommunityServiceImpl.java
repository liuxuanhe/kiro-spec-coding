package com.parking.service.impl;

import com.parking.common.BusinessException;
import com.parking.common.ErrorCode;
import com.parking.common.OperationLogAnnotation;
import com.parking.dto.CommunityCreateRequest;
import com.parking.mapper.AdminMapper;
import com.parking.mapper.CommunityMapper;
import com.parking.model.Community;
import com.parking.service.CommunityService;
import com.parking.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 小区服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final CommunityMapper communityMapper;
    private final JwtTokenService jwtTokenService;
    private final AdminMapper adminMapper;

    @Override
    public List<Community> listCommunities(String role, Long communityId) {
        if ("super_admin".equals(role)) {
            // Super_Admin 返回所有小区
            return communityMapper.selectAll();
        }
        // Property_Admin 仅返回本小区
        Community community = communityMapper.selectById(communityId);
        if (community == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(community);
    }

    @Override
    @OperationLogAnnotation(operationType = "CREATE", targetType = "COMMUNITY")
    public Community createCommunity(CommunityCreateRequest request, Long operatorId) {
        // 校验小区编码唯一性
        Community existing = communityMapper.selectByCode(request.getCommunityCode());
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARKING_12002);
        }

        Community community = new Community();
        community.setCommunityName(request.getCommunityName());
        community.setCommunityCode(request.getCommunityCode());
        community.setProvince(request.getProvince());
        community.setCity(request.getCity());
        community.setDistrict(request.getDistrict());
        community.setAddress(request.getAddress());
        community.setContactPerson(request.getContactPerson());
        community.setContactPhone(request.getContactPhone());
        community.setStatus("active");

        communityMapper.insert(community);
        log.info("创建小区成功: id={}, communityName={}, communityCode={}, operatorId={}",
                community.getId(), community.getCommunityName(), community.getCommunityCode(), operatorId);

        return community;
    }

    @Override
    @OperationLogAnnotation(operationType = "UPDATE", targetType = "COMMUNITY")
    public Community updateCommunity(Long id, CommunityCreateRequest request, Long operatorId) {
        Community existing = communityMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARKING_12001);
        }

        // 如果修改了编码，校验新编码唯一性
        if (!existing.getCommunityCode().equals(request.getCommunityCode())) {
            Community codeCheck = communityMapper.selectByCode(request.getCommunityCode());
            if (codeCheck != null) {
                throw new BusinessException(ErrorCode.PARKING_12002);
            }
        }

        existing.setCommunityName(request.getCommunityName());
        existing.setProvince(request.getProvince());
        existing.setCity(request.getCity());
        existing.setDistrict(request.getDistrict());
        existing.setAddress(request.getAddress());
        existing.setContactPerson(request.getContactPerson());
        existing.setContactPhone(request.getContactPhone());
        existing.setStatus(request.getContactPhone() != null ? existing.getStatus() : existing.getStatus());

        communityMapper.update(existing);
        log.info("更新小区成功: id={}, communityName={}, operatorId={}",
                id, existing.getCommunityName(), operatorId);

        return communityMapper.selectById(id);
    }

    @Override
    @OperationLogAnnotation(operationType = "UPDATE", targetType = "COMMUNITY")
    public String switchCommunity(Long adminId, Long targetCommunityId) {
        // 验证目标小区存在
        Community target = communityMapper.selectById(targetCommunityId);
        if (target == null) {
            throw new BusinessException(ErrorCode.PARKING_12001);
        }

        log.info("Super_Admin 切换小区: adminId={}, targetCommunityId={}, communityName={}",
                adminId, targetCommunityId, target.getCommunityName());

        // 重新签发包含新 communityId 的 Access Token
        return jwtTokenService.generateAccessToken(adminId, "super_admin", targetCommunityId, null);
    }
}
