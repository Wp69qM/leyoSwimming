package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("coach_audit_log")
public class CoachAuditLog {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long coachId;
  private Long applicationId;
  private Long adminId;
  private String action;
  private Integer fromStatus;
  private Integer toStatus;
  private String reason;

  private LocalDateTime createdAt;
}
