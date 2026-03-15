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

    /**
     * 统计指定日期之前的操作日志数量
     */
    int countBeforeDate(@Param("beforeDate") String beforeDate);

    /**
     * 批量插入到归档表
     */
    void insertToArchive(@Param("beforeDate") String beforeDate, @Param("batchSize") int batchSize);

    /**
     * 删除指定日期之前的操作日志
     */
    int deleteBeforeDate(@Param("beforeDate") String beforeDate, @Param("batchSize") int batchSize);

    /**
     * 统计归档表中指定日期之前的记录数
     */
    int countArchiveBeforeDate(@Param("beforeDate") String beforeDate);
}
