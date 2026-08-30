package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_login_log")
public class UserLoginLog {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private String phoneHash;
  private String ip;
  private String userAgent;
  private Integer status;
  private String reason;
  private LocalDateTime createdAt;
}
