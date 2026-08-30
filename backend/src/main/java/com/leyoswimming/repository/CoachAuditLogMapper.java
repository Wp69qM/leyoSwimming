package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.CoachAuditLog;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CoachAuditLogMapper extends BaseMapper<CoachAuditLog> {

  @Select(
      "SELECT * FROM coach_audit_log WHERE coach_id = #{coachId} ORDER BY created_at DESC")
  List<CoachAuditLog> findByCoachId(@Param("coachId") Long coachId);
}
