package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("admin_user")
public class AdminUser {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String username;
  private String passwordHash;
  private String name;
  private String phone;
  private String role;
  private Integer status;
  private LocalDateTime lastLoginAt;
  private LocalDateTime deletedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @Version
  private Integer version;
}
