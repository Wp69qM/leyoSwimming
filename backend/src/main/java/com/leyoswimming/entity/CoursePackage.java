package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("package")
public class CoursePackage {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private Long coachId;
  private String packageMode;
  private Integer totalHours;
  private Integer consumedCount;
  private Integer reservedCount;
  private Integer availableCount;
  private BigDecimal pricePerHour;
  private BigDecimal paidAmount;
  private BigDecimal originalPrice;
  private Boolean refundEnabled;
  private BigDecimal refundRatio;
  private Integer refundValidDays;
  private String status;
  private String frozenReason;
  private LocalDateTime pendingHandoverAt;
  private LocalDateTime expireAt;
  private LocalDateTime exhaustedAt;
  private LocalDateTime refundedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
