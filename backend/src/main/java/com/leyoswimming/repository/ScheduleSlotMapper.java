package com.leyoswimming.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyoswimming.entity.ScheduleSlot;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ScheduleSlotMapper extends BaseMapper<ScheduleSlot> {

  @Select(
      "SELECT * FROM schedule_slot WHERE coach_id = #{coachId} AND start_time > #{now} "
          + "AND status != 'hidden' ORDER BY start_time ASC LIMIT 1")
  ScheduleSlot findFirstVisibleFutureByCoachId(
      @Param("coachId") Long coachId, @Param("now") LocalDateTime now);

  @Update(
      "UPDATE schedule_slot SET status = 'hidden', updated_at = NOW() "
          + "WHERE coach_id = #{coachId} AND start_time > #{now} AND status != 'hidden'")
  int hideFutureByCoachId(@Param("coachId") Long coachId, @Param("now") LocalDateTime now);
}
