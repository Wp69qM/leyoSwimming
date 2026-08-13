package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("`order`")
public class Order {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String orderNo;
  private String type;
  private String status;
  private Long userId;
  private Long coachId;
  private Long packageId;
  private Long purchaseOrderId;
  private BigDecimal originalAmount;
  private BigDecimal discountAmount;
  private BigDecimal paidAmount;
  private String paymentMethod;
  private String channelTradeNo;
  private String reason;
  private String rejectedReason;
  private Long approvedBy;
  private LocalDateTime approvedAt;
  private LocalDateTime refundedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
