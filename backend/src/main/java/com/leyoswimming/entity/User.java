package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user")
public class User {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String openid;
  private String unionId;
  private String phone;
  private String avatarUrl;
  private String name;
  private String identityStatus;
  private Boolean profileCompleted;
  private Integer status;
  private LocalDateTime deletedAt;
  private LocalDateTime anonymousAfter;
  private LocalDateTime lastLoginAt;
  private String loginIp;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
