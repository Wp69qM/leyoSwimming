package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.CoachResignationTicket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CoachResignationTicketMapper extends BaseMapper<CoachResignationTicket> {

  @Select(
      "SELECT * FROM coach_resignation_ticket WHERE coach_id = #{coachId} "
          + "AND status IN ('processing', 'pending_audit') ORDER BY id DESC LIMIT 1")
  CoachResignationTicket findActiveByCoachId(@Param("coachId") Long coachId);
}
