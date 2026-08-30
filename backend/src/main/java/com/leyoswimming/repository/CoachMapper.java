package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.Coach;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CoachMapper extends BaseMapper<Coach> {

  @Select("SELECT * FROM coach WHERE union_id = #{unionId} AND status != 3 LIMIT 1")
  Coach findActiveByUnionId(@Param("unionId") String unionId);

  @Select("SELECT * FROM coach WHERE phone_hash = #{phoneHash} AND status != 3 LIMIT 1")
  Coach findActiveByPhone(@Param("phoneHash") String phoneHash);
}
