package com.example.dgkl.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dgkl.auth.entity.SysUser;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @Delete("""
            DELETE FROM oauth2_authorization
            WHERE registered_client_id = #{clientRegistrationId} AND principal_name = #{principalName}
            """)
    void revokeAuthorizations(@Param("clientRegistrationId") String clientRegistrationId,
                              @Param("principalName") String principalName);

    @Select("""
            SELECT COUNT(*)
            FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE u.status = 1 AND u.deleted = 0 AND r.role_code = 'ADMIN' AND r.deleted = 0
            """)
    long countActiveAdministrators();

    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM sys_user u
                JOIN sys_user_role ur ON ur.user_id = u.id
                JOIN sys_role r ON r.id = ur.role_id
                WHERE u.id = #{userId} AND u.status = 1 AND u.deleted = 0
                  AND r.role_code = 'ADMIN' AND r.deleted = 0
            )
            """)
    boolean isActiveAdministrator(@Param("userId") Long userId);
}
