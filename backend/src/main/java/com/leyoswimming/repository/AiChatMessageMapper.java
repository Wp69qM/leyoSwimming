package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {
}
