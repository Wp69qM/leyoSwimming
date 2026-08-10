package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("booking")
public class Booking {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long packageId;
  private Long coachId;
  private Long userId;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String status;
  private Integer cancelReason;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
