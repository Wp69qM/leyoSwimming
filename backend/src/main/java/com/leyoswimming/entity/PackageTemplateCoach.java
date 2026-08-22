package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("package_template_coach")
public class PackageTemplateCoach {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long packageTemplateId;
  private Long coachId;
  private BigDecimal referencePriceSnapshot;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
