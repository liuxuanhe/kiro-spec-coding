package com.parking.mapper;

import com.parking.model.Community;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 小区 Mapper 接口
 */
@Mapper
public interface CommunityMapper {

    /**
     * 查询所有小区列表
     *
     * @return 小区列表
     */
    List<Community> selectAll();

    /**
     * 根据ID查询小区
     *
     * @param id 小区ID
     * @return 小区实体
     */
    Community selectById(@Param("id") Long id);

    /**
     * 根据小区编码查询小区
     *
     * @param communityCode 小区编码
     * @return 小区实体
     */
    Community selectByCode(@Param("communityCode") String communityCode);

    /**
     * 新增小区
     *
     * @param community 小区实体
     * @return 影响行数
     */
    int insert(Community community);

    /**
     * 更新小区信息
     *
     * @param community 小区实体
     * @return 影响行数
     */
    int update(Community community);
}
