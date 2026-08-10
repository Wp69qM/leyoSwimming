package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sms_code")
public class SmsCode {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String phoneHash;
  private String code;
  private String scene;
  private String appType;
  private LocalDateTime expiresAt;
  private Boolean used;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
