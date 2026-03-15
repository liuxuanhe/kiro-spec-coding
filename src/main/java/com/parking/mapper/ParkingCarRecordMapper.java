package com.parking.mapper;

import com.parking.model.ParkingCarRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 入场记录 Mapper 接口
 * 支持按月分表路由，表名格式: parking_car_record_yyyymm
 * Validates: Requirements 15.2
 */
@Mapper
public interface ParkingCarRecordMapper {

    /**
     * 插入入场记录到指定月份分表
     *
     * @param tableName 分表名称（如 parking_car_record_202501）
     * @param record    入场记录实体
     */
    void insertToTable(@Param("tableName") String tableName,
                       @Param("record") ParkingCarRecord record);

    /**
     * 统计指定小区当前在场车辆数（跨分表查询当前月份）
     *
     * @param tableName   分表名称
     * @param communityId 小区ID
     * @return 在场车辆数
     */
    int countEnteredByTable(@Param("tableName") String tableName,
                            @Param("communityId") Long communityId);
}
