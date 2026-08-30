package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("coach_certificate_application")
public class CoachCertificateApplication {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long applicationId;
  private String certType;
  private String imageUrl;
  private Integer sortOrder;

  private LocalDateTime createdAt;
}
