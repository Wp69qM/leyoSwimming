package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
@TableName("coach")
public class Coach {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String openid;
  private String unionId;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String phone;

  private String phoneHash;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String avatarUrl;

  private String name;
  private Integer status;
  private String rejectionReason;
  private LocalDateTime submittedAt;
  private LocalDateTime approvedAt;
  private LocalDateTime lastLoginAt;
  private String loginIp;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private Integer age;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String gender;

  private Boolean profileCompleted;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String personalDesc;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private Integer teachingYears;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String email;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String wechatQrUrl;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String idCardNo;

  private String idCardHash;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private Integer totalStudents;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private Integer totalHours;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String teachingStrokes;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String bio;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private java.math.BigDecimal referencePrice;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private LocalDateTime priceChangedAt;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private Integer priceChangeCountToday = 0;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private java.math.BigDecimal rating;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String realtimeStatus;

  @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.IGNORED)
  private List<CoachCertificate> certificates;

  @Version
  private Integer version;

  @Data
  public static class CoachCertificate {
    private String name;
    private String url;
  }
}
