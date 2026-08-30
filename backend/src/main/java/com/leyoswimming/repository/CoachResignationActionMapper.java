package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.CoachResignationAction;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CoachResignationActionMapper extends BaseMapper<CoachResignationAction> {

  @Select(
      "SELECT * FROM coach_resignation_action WHERE ticket_id = #{ticketId} ORDER BY id DESC")
  List<CoachResignationAction> findByTicketId(@Param("ticketId") Long ticketId);

  @Select(
      "SELECT * FROM coach_resignation_action WHERE ticket_id = #{ticketId} AND package_id = #{packageId} LIMIT 1")
  CoachResignationAction findByTicketIdAndPackageId(
      @Param("ticketId") Long ticketId, @Param("packageId") Long packageId);
}
