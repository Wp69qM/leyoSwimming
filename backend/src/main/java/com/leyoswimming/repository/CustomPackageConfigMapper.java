package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.CustomPackageConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CustomPackageConfigMapper extends BaseMapper<CustomPackageConfig> {

  @Select("SELECT * FROM `custom_package_config` ORDER BY id LIMIT 1")
  CustomPackageConfig findFirst();
}
