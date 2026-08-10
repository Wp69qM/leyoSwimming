package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

  @Select("SELECT * FROM `user` WHERE union_id = #{unionId} AND status = 0 LIMIT 1")
  User findActiveByUnionId(@Param("unionId") String unionId);

  @Select("SELECT * FROM `user` WHERE phone = #{phone} AND status = 0 LIMIT 1")
  User findActiveByPhone(@Param("phone") String phone);
}
