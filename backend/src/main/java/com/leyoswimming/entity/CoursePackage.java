package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
@TableName(value = "package", autoResultMap = true)
public class CoursePackage {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String packageNo;
  private Long userId;
  private Long coachId;
  private Long orderId;
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

  // 购买时快照
  private String packageName;
  private String coachName;
  private String teachingType;

  @TableField(typeHandler = JacksonTypeHandler.class)
  private List<Integer> strokeIds;

  private Integer durationMinutes;
  private Integer validDays;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String frozenReason;

  private LocalDateTime pendingHandoverAt;
  private LocalDateTime expireAt;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String extendReason;

  private LocalDateTime exhaustedAt;
  private LocalDateTime refundedAt;

  @Version
  private Integer version;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
