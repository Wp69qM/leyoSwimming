package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("audit_log")
public class AuditLog {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String actorType;
  private Long actorId;
  private String targetType;
  private Long targetId;
  private String action;
  private String beforeSnapshot;
  private String afterSnapshot;
  private String reason;
  private String ip;

  private LocalDateTime createdAt;
}
