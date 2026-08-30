package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("coach_application")
public class CoachApplication {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long coachId;
  private String status;
  private Integer previousCoachStatus;

  private String name;
  private String phone;
  private String phoneHash;
  private String gender;
  private Integer age;
  private String email;
  private String wechatQrUrl;
  private String idCardNo;
  private Integer teachingYears;
  private Integer totalStudents;
  private Integer totalHours;
  private String teachingStrokes;
  private String bio;
  private BigDecimal referencePrice;

  private LocalDateTime submittedAt;
  private LocalDateTime approvedAt;
  private Long approvedBy;
  private String rejectionReason;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
