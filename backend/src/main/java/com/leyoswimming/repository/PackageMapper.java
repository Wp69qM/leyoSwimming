package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.CoursePackage;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PackageMapper extends BaseMapper<CoursePackage> {

  @Select(
      "SELECT * FROM `package` WHERE coach_id = #{coachId} AND status = 'active' ORDER BY id DESC")
  List<CoursePackage> findActiveByCoachId(@Param("coachId") Long coachId);

  @Select(
      "SELECT * FROM `package` WHERE user_id = #{userId} AND status = 'active' ORDER BY id DESC LIMIT 1")
  CoursePackage findFirstActiveByUserId(@Param("userId") Long userId);

  @Select(
      "SELECT COUNT(DISTINCT user_id) FROM `package` WHERE coach_id = #{coachId} AND status = 'active'")
  Long countActiveStudentsByCoachId(@Param("coachId") Long coachId);

  @Select(
      "SELECT * FROM `package` WHERE user_id = #{userId} AND package_mode = 'experience' "
          + "AND status IN ('active', 'exhausted') ORDER BY id DESC LIMIT 1")
  CoursePackage findExperiencePackageByUserId(@Param("userId") Long userId);
}
