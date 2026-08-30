package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.PackageTemplateCoach;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PackageTemplateCoachMapper extends BaseMapper<PackageTemplateCoach> {

  @Select("SELECT * FROM `package_template_coach` WHERE package_template_id = #{templateId} ORDER BY coach_id")
  List<PackageTemplateCoach> findByTemplateId(@Param("templateId") Long templateId);

  @Delete("DELETE FROM `package_template_coach` WHERE package_template_id = #{templateId}")
  void deleteByTemplateId(@Param("templateId") Long templateId);

  @Select("SELECT COUNT(*) FROM `package_template_coach` WHERE package_template_id = #{templateId}")
  Long selectCountByTemplateId(@Param("templateId") Long templateId);
}
