package com.example.dgkl.schedule.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ScheduleGuardMapper {
    // The version write keeps this lawyer locked globally after an AT branch commits locally.
    default void lock(Long lawyerId) {
        selectForUpdate(lawyerId);
        advanceVersion(lawyerId);
    }

    @Select("SELECT lawyer_id FROM schedule_lawyer_guard WHERE lawyer_id = #{lawyerId} FOR UPDATE")
    Long selectForUpdate(@Param("lawyerId") Long lawyerId);

    @Insert("INSERT INTO schedule_lawyer_guard (lawyer_id, guard_version) VALUES (#{lawyerId}, 1) "
            + "ON DUPLICATE KEY UPDATE guard_version = guard_version + 1")
    void advanceVersion(@Param("lawyerId") Long lawyerId);
}
