package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("custom_package_config")
public class CustomPackageConfig {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String configKey;
  private Integer minHours;
  private Integer maxHours;
  private Integer defaultValidDays;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
