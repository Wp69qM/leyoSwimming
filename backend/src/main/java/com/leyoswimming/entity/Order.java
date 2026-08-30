package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
@TableName(value = "order", autoResultMap = true)
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
  private String adjustReason;
  private Long approvedBy;
  private LocalDateTime approvedAt;
  private LocalDateTime refundedAt;
  private LocalDateTime paidAt;
  private LocalDateTime expireAt;

  // 购买时套餐快照
  private String packageMode;
  private Long packageTemplateId;
  private String packageName;
  private String coachName;
  private String teachingType;

  @TableField(typeHandler = JacksonTypeHandler.class)
  private List<Integer> strokeIds;

  private Integer totalHours;
  private Integer durationMinutes;
  private Integer validDays;
  private Boolean refundEnabled;
  private BigDecimal refundRatio;
  private Integer refundValidDays;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
