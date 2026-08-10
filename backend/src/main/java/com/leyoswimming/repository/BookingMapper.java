package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.Booking;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BookingMapper extends BaseMapper<Booking> {

  @Select(
      "SELECT * FROM booking WHERE coach_id = #{coachId} AND start_time > #{now} "
          + "AND status IN ('booked', 'confirmed') ORDER BY start_time ASC")
  List<Booking> findFutureActiveByCoachId(
      @Param("coachId") Long coachId, @Param("now") LocalDateTime now);

  @Update(
      "UPDATE booking SET status = 'cancelled', cancel_reason = 2, updated_at = NOW() "
          + "WHERE coach_id = #{coachId} AND start_time > #{now} "
          + "AND status IN ('booked', 'confirmed')")
  int cancelFutureByCoachId(@Param("coachId") Long coachId, @Param("now") LocalDateTime now);
}
