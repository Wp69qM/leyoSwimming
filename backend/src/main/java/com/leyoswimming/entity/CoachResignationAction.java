package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("coach_resignation_action")
public class CoachResignationAction {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long ticketId;
  private Long packageId;
  private String action;
  private Long targetCoachId;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
