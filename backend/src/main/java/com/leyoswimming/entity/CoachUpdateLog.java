package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("coach_update_log")
public class CoachUpdateLog {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long coachId;
  private String fieldName;
  private String oldValue;
  private String newValue;
  private LocalDateTime createdAt;
}
