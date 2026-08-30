package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("terms_policy")
public class TermsPolicy {

  @TableId(type = IdType.INPUT)
  private String version;

  private String content;
  private LocalDateTime effectiveAt;

  @TableField("is_current")
  private Boolean current;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
