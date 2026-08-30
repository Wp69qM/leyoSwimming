package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("coach_login_log")
public class CoachLoginLog {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long coachId;
  private String phoneHash;
  private String ip;
  private String userAgent;
  private Integer status;
  private String reason;
  private LocalDateTime createdAt;
}
