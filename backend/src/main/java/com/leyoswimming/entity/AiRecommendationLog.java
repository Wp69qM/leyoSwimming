package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName(value = "ai_recommendation_log", autoResultMap = true)
public class AiRecommendationLog {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String sessionId;
  private String messageId;
  private Long userId;
  private String input;

  @TableField(typeHandler = JacksonTypeHandler.class)
  private Object toolCalls;

  private String llmResponse;

  @TableField(typeHandler = JacksonTypeHandler.class)
  private Object finalResponse;

  private Integer latencyMs;
  private Integer llmLatencyMs;
  private LocalDateTime createdAt;
}
