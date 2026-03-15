package com.parking.mapper;

import com.parking.model.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作日志 Mapper 接口
 */
@Mapper
public interface OperationLogMapper {

    /**
     * 插入操作日志
     */
    void insert(@Param("log") OperationLog log);

    /**
     * 按条件查询操作日志
     */
    List<OperationLog> selectByCondition(@Param("communityId") Long communityId,
                                          @Param("operatorId") Long operatorId,
                                          @Param("operationType") String operationType,
                                          @Param("startTime") String startTime,
                                          @Param("endTime") String endTime);
}
