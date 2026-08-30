package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
@TableName(value = "package_template", autoResultMap = true)
public class PackageTemplate {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String name;
  private String packageMode;
  private String teachingType;

  @TableField(typeHandler = JacksonTypeHandler.class)
  private List<Integer> strokeIds;

  private Integer totalHours;
  private Integer durationMinutes;
  private Integer validDays;
  private BigDecimal originalPrice;
  private BigDecimal price;
  private Boolean refundEnabled;
  private BigDecimal refundRatio;
  private Integer refundValidDays;

  @TableField(typeHandler = JacksonTypeHandler.class)
  private List<String> tags;

  private String description;

  @TableField(typeHandler = JacksonTypeHandler.class)
  private List<String> images;

  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @Version
  private Integer version;
}
