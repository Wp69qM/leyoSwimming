package com.leyoswimming.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("ai_knowledge_document")
public class AiKnowledgeDocument {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String title;
  private String category;
  private String contentType;
  private String sourceType;
  private String content;
  private Integer status;
  private Long createdBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
