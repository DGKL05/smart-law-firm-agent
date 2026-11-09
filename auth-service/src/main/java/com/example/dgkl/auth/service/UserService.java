package com.example.dgkl.auth.service;

import com.example.dgkl.auth.dto.AdminUserRequest;
import com.example.dgkl.auth.dto.AuthRequest;
import com.example.dgkl.auth.dto.UserView;
import com.example.dgkl.common.response.PageResult;
import com.example.dgkl.common.security.SecurityUser;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public interface UserService {
    UserDetails loadForAuthentication(String username);

    Optional<SecurityUser> findActivePrincipal(Long id);

    void register(AuthRequest.Credentials request);

    UserView currentProfile();

    UserView updateProfile(AuthRequest.Profile request);

    void changePassword(AuthRequest.Password request);

    PageResult<UserView> listUsers(long pageNum, long pageSize, String keyword);

    UserView getUser(Long id);

    UserView createUser(AdminUserRequest request);

    UserView updateUser(Long id, AdminUserRequest request);

    void deleteUser(Long id);

    boolean activeUserExists(Long id);

    long countUsers();

    Long requireAdministrator();
}
