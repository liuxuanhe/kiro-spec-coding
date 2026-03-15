package com.parking.service;

import com.parking.model.VisitorSession;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Visitor 配额管理器接口
 * 负责月度配额计算、配额校验、时长累计、超时检测
 * Validates: Requirements 7.2, 8.6, 8.7, 8.8, 8.9, 8.10, 10.2
 */
public interface VisitorQuotaManager {

    /**
     * 计算月度配额使用量（分钟）
     * 公式: SUM(accumulated_duration) WHERE community_id=? AND house_no=? AND MONTH(create_time)=?
     *
     * @param communityId 小区ID
     * @param houseNo     房屋号
     * @param month       月份
     * @return 已使用分钟数
     */
    long calculateMonthlyUsage(Long communityId, String houseNo, YearMonth month);

    /**
     * 检查配额是否充足（当月累计 < 72小时 = 4320分钟）
     *
     * @param communityId 小区ID
     * @param houseNo     房屋号
     * @return true 表示配额充足
     */
    boolean checkQuotaSufficient(Long communityId, String houseNo);

    /**
     * 累计 Visitor 停放时长
     * 计算本次停放时长 = exitTime - lastEntryTime，累加到 accumulated_duration
     *
     * @param visitorSessionId 会话ID
     * @param exitTime         出场时间
     * @return 本次累计的分钟数
     */
    long accumulateDuration(Long visitorSessionId, LocalDateTime exitTime);

    /**
     * 检查超时会话（累计时长 ≥ 24小时 = 1440分钟）
     *
     * @return 超时的 Visitor 会话列表
     */
    List<VisitorSession> checkTimeout();
}
