package com.parking.mapper;

import com.parking.model.AccessLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 访问日志 Mapper 接口
 */
@Mapper
public interface AccessLogMapper {

    /**
     * 插入访问日志
     */
    void insert(@Param("log") AccessLog log);

    /**
     * 按条件查询访问日志
     */
    List<AccessLog> selectByCondition(@Param("communityId") Long communityId,
                                       @Param("userId") Long userId,
                                       @Param("apiPath") String apiPath,
                                       @Param("startTime") String startTime,
                                       @Param("endTime") String endTime);

    /**
     * 统计指定日期之前的访问日志数量
     */
    int countBeforeDate(@Param("beforeDate") String beforeDate);

    /**
     * 批量插入到归档表
     */
    void insertToArchive(@Param("beforeDate") String beforeDate, @Param("batchSize") int batchSize);

    /**
     * 删除指定日期之前的访问日志
     */
    int deleteBeforeDate(@Param("beforeDate") String beforeDate, @Param("batchSize") int batchSize);

    /**
     * 统计归档表中指定日期之前的记录数
     */
    int countArchiveBeforeDate(@Param("beforeDate") String beforeDate);
}
