package com.parking.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 停车费用实体（预留）
 */
@Data
public class ParkingFee {
    private Long id;
    private Long communityId;
    private String houseNo;
    private String carNumber;
    private Long entryRecordId;
    private BigDecimal feeAmount;
    private String feeType;
    private String paymentStatus;
    private String paymentMethod;
    private LocalDateTime paymentTime;
    private String transactionId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
