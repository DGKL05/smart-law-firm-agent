package com.example.dgkl.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dgkl.auth.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
    @Select("SELECT id FROM sys_role WHERE role_code = 'ADMIN' AND deleted = 0 FOR UPDATE")
    Long lockAdministratorRole();
}
