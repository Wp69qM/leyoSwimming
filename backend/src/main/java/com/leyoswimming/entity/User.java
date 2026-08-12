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
@TableName("user")
public class User {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String openid;
  private String unionId;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String phone;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String phoneHash;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String avatarUrl;

  private String name;
  private String identityStatus;
  private Integer identity;
  private String source;
  private Boolean profileCompleted;
  private Integer status;
  private Integer age;
  private String gender;

  @Version
  private Integer version;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String guardianName;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String guardianPhone;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String guardianPhoneHash;

  private Boolean hasSwimBasis;

  @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.IGNORED)
  private List<String> swimStrokes;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private Integer swimYears;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String personalDesc;
  private LocalDateTime deletedAt;
  private LocalDateTime anonymousAfter;
  private LocalDateTime lastLoginAt;
  private String loginIp;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
