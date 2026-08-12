package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.CoachApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CoachApplicationMapper extends BaseMapper<CoachApplication> {

  @Select(
      "SELECT * FROM coach_application WHERE coach_id = #{coachId} AND status = 'pending' LIMIT 1")
  CoachApplication findPendingByCoachId(@Param("coachId") Long coachId);

  @Select(
      "SELECT * FROM coach_application WHERE coach_id = #{coachId} ORDER BY created_at DESC LIMIT 1")
  CoachApplication findLatestByCoachId(@Param("coachId") Long coachId);
}
