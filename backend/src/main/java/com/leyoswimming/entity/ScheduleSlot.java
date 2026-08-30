package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("schedule_slot")
public class ScheduleSlot {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long coachId;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
