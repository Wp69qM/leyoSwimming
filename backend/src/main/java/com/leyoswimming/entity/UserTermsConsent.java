package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_terms_consent")
public class UserTermsConsent {

  @TableId(type = IdType.AUTO)
  private Long consentId;

  private String actorType;
  private Long userId;
  private String version;
  private String status;
  private LocalDateTime agreedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
