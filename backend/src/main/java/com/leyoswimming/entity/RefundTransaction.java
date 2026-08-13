package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("refund_transaction")
public class RefundTransaction {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long orderId;
  private Long refundRecordId;
  private String channel;
  private String channelRefundNo;
  private BigDecimal amount;
  private String status;
  private String failureReason;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
