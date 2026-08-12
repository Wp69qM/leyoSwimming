package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("admin_audit_log")
public class AdminAuditLog {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long adminUserId;
  private Long targetAdminUserId;
  private String action;
  private String beforeSnapshot;
  private String afterSnapshot;
  private String reason;
  private String ip;

  private LocalDateTime createdAt;
}
