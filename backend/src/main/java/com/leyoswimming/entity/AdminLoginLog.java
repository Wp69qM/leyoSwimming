package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("admin_login_log")
public class AdminLoginLog {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long adminUserId;
  private String username;
  private String ip;
  private String userAgent;
  private Integer status;
  private String reason;
  private LocalDateTime createdAt;
}
