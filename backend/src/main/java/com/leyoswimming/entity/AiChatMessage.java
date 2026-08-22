package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName(value = "ai_chat_message", autoResultMap = true)
public class AiChatMessage {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String messageId;
  private String sessionId;
  private String role;
  private String content;

  @TableField(typeHandler = JacksonTypeHandler.class)
  private Object recommendations;

  @TableField(typeHandler = JacksonTypeHandler.class)
  private Object toolCalls;

  private LocalDateTime createdAt;
}
