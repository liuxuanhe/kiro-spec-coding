package com.parking.mapper;

import com.parking.model.OwnerInfoModifyApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 敏感信息修改申请 Mapper 接口
 * Validates: Requirements 24.1, 24.2, 24.3, 24.6
 */
@Mapper
public interface OwnerInfoModifyMapper {

    /**
     * 插入修改申请记录
     *
     * @param application 申请实体
     */
    void insert(OwnerInfoModifyApplication application);

    /**
     * 根据ID查询申请记录
     *
     * @param id 申请ID
     * @return 申请实体
     */
    OwnerInfoModifyApplication selectById(@Param("id") Long id);

    /**
     * 根据ID查询申请记录（行级锁，SELECT FOR UPDATE）
     *
     * @param id 申请ID
     * @return 申请实体
     */
    OwnerInfoModifyApplication selectByIdForUpdate(@Param("id") Long id);

    /**
     * 更新审批状态
     *
     * @param id            申请ID
     * @param status        新状态
     * @param rejectReason  驳回原因（可为空）
     * @param auditAdminId  审批管理员ID
     * @return 更新行数
     */
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("rejectReason") String rejectReason,
                     @Param("auditAdminId") Long auditAdminId);
}
