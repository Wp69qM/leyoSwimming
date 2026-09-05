package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.AiKnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiKnowledgeDocumentMapper extends BaseMapper<AiKnowledgeDocument> {

  default long selectCountByTitle(String title) {
    return selectCount(
        new LambdaQueryWrapper<AiKnowledgeDocument>().eq(AiKnowledgeDocument::getTitle, title));
  }
}
