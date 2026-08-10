package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("coach")
public class Coach {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String openid;
  private String unionId;
  private String phone;
  private String avatarUrl;
  private String name;
  private Integer status;
  private String rejectionReason;
  private LocalDateTime submittedAt;
  private LocalDateTime lastLoginAt;
  private String loginIp;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
