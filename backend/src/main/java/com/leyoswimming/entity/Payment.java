package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("payment")
public class Payment {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long orderId;
  private Long userId;
  private String channel;
  private String channelTradeNo;
  private BigDecimal amount;
  private String status;
  private LocalDateTime paidAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
