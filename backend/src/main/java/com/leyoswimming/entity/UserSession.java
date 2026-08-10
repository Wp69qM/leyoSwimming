package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_session")
public class UserSession {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private String sessionKeyEncrypted;
  private String refreshTokenHash;
  private LocalDateTime expiresAt;
  private String deviceName;
  private String deviceId;
  private LocalDateTime lastActiveAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
