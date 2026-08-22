package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("ai_chat_session")
public class AiChatSession {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String sessionId;
  private Long userId;
  private String title;
  private Integer status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
