package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.CoachCertificate;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CoachCertificateMapper extends BaseMapper<CoachCertificate> {

  @Select("SELECT * FROM coach_certificate WHERE coach_id = #{coachId} ORDER BY sort_order")
  List<CoachCertificate> findByCoachId(@Param("coachId") Long coachId);

  @Delete("DELETE FROM coach_certificate WHERE coach_id = #{coachId}")
  void deleteByCoachId(@Param("coachId") Long coachId);
}
