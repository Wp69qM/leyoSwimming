package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("refund_record")
public class RefundRecord {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long packageId;
  private Long ticketId;
  private BigDecimal refundAmount;
  private Integer status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
