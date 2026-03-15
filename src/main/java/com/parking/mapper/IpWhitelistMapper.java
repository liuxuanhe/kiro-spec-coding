package com.parking.mapper;

import com.parking.model.IpWhitelist;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IP 白名单 Mapper 接口
 */
public interface IpWhitelistMapper {

    /**
     * 插入 IP 白名单记录
     */
    void insert(IpWhitelist ipWhitelist);

    /**
     * 根据 ID 查询
     */
    IpWhitelist selectById(@Param("id") Long id);

    /**
     * 按操作类型查询有效的 IP 白名单
     */
    List<IpWhitelist> selectByOperation(@Param("operationType") String operationType);

    /**
     * 查询全部有效的 IP 白名单
     */
    List<IpWhitelist> selectAll();

    /**
     * 逻辑删除
     */
    int logicalDelete(@Param("id") Long id);
}
