package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.CoachCertificateApplication;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CoachCertificateApplicationMapper extends BaseMapper<CoachCertificateApplication> {

  @Select(
      "SELECT * FROM coach_certificate_application WHERE application_id = #{applicationId} ORDER BY sort_order")
  List<CoachCertificateApplication> findByApplicationId(@Param("applicationId") Long applicationId);
}
