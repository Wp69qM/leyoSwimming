package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("coach_resignation_ticket")
public class CoachResignationTicket {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long coachId;
  private String ticketNo;
  private String reason;
  private String status;
  private Integer totalPackages;
  private Integer handledPackages;
  private Boolean scheduleCleared;
  private Integer settlementStatus;
  private LocalDateTime createdAt;
  private LocalDateTime submittedAt;
  private LocalDateTime updatedAt;
}
