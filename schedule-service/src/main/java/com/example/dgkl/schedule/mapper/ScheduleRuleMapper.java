package com.example.dgkl.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dgkl.schedule.entity.ScheduleRule;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ScheduleRuleMapper extends BaseMapper<ScheduleRule> {
    @Select("SELECT EXISTS(SELECT 1 FROM lawyer_schedule WHERE lawyer_id = #{lawyerId})")
    boolean hasConfiguredRules(@Param("lawyerId") Long lawyerId);
}
