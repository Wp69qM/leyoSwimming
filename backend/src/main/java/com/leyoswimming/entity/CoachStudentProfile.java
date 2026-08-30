package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("coach_student_profile")
public class CoachStudentProfile {

  @TableId(type = IdType.AUTO)
  private Long profileId;

  private Long coachId;
  private Long studentUserId;
  private String learningStrokes;
  private Integer swimLevel;
  private String basics;
  private String notes;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
