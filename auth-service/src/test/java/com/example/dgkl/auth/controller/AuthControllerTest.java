package com.example.dgkl.auth.controller;

import com.example.dgkl.auth.config.OAuth2SecurityConfig;
import com.example.dgkl.auth.entity.SysRole;
import com.example.dgkl.auth.entity.SysUser;
import com.example.dgkl.auth.entity.SysUserRole;
import com.example.dgkl.auth.mapper.SysRoleMapper;
import com.example.dgkl.auth.mapper.SysUserMapper;
import com.example.dgkl.auth.mapper.SysUserRoleMapper;
import com.example.dgkl.auth.service.UserService;
import com.example.dgkl.common.exception.GlobalExceptionHandler;
import com.example.dgkl.common.security.SecurityUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AuthControllerTest.TestConfig.class)
class AuthControllerTest {
    @MockBean
    private SysUserMapper userMapper;
    @MockBean
    private SysRoleMapper roleMapper;
    @MockBean
    private SysUserRoleMapper userRoleMapper;
    @MockBean
    private OAuth2AuthorizationService authorizations;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthController controller;
    @Autowired
    private AdminUserController adminUserController;
    @Autowired
    private UserService users;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller, adminUserController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void profileLoadsCurrentUserWithoutExposingPassword() throws Exception {
        authenticateAs("USER");
        SysUser user = activeUser("alice");
        user.setId(17L);
        user.setNickname("Alice");
        when(userMapper.selectById(17L)).thenReturn(user);

        mvc.perform(get("/api/auth/me"))
                .andExpect(jsonPath("$.data.id").value(17))
                .andExpect(jsonPath("$.data.nickname").value("Alice"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void registrationPersistsOrdinaryUserRole() throws Exception {
        SysRole userRole = new SysRole();
        userRole.setId(2L);
        userRole.setRoleCode("USER");
        when(roleMapper.selectOne(any())).thenReturn(userRole);
        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(17L);
            return 1;
        }).when(userMapper).insert(any(SysUser.class));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"secret123"}
                                """))
                .andExpect(jsonPath("$.code").value(200));

        verify(userRoleMapper).insert(argThat((SysUserRole role) ->
                Long.valueOf(17L).equals(role.getUserId()) && Long.valueOf(2L).equals(role.getRoleId())));
    }

    @Test
    void storedPasswordHashCannotBeUsedAsThePassword() {
        SysUser user = activeUser("alice");
        when(userMapper.selectOne(any())).thenReturn(user);

        var provider = new OAuth2SecurityConfig().formProvider(users, passwordEncoder);
        var credentials = UsernamePasswordAuthenticationToken.unauthenticated("alice", user.getPassword());
        assertThatThrownBy(() -> provider.authenticate(credentials)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void administratorCannotDeleteTheirOwnAccount() throws Exception {
        authenticateAs("ADMIN");
        when(roleMapper.lockAdministratorRole()).thenReturn(1L);
        when(userMapper.isActiveAdministrator(17L)).thenReturn(true);

        mvc.perform(delete("/api/admin/users/17"))
                .andExpect(jsonPath("$.code").value(409));

        verify(userMapper, never()).deleteById(17L);
    }

    @Test
    void disablingTheLastAdministratorIsRejected() throws Exception {
        authenticateAs("ADMIN");
        when(roleMapper.lockAdministratorRole()).thenReturn(1L);
        when(userMapper.isActiveAdministrator(17L)).thenReturn(true);
        when(userMapper.countActiveAdministrators()).thenReturn(1L);
        when(userMapper.selectById(17L)).thenReturn(activeUser("alice"));
        SysUserRole assignment = new SysUserRole();
        assignment.setUserId(17L);
        assignment.setRoleId(1L);
        SysRole administratorRole = new SysRole();
        administratorRole.setId(1L);
        administratorRole.setRoleCode("ADMIN");
        when(userRoleMapper.selectList(any())).thenReturn(List.of(assignment));
        when(roleMapper.selectByIds(List.of(1L))).thenReturn(List.of(administratorRole));

        mvc.perform(put("/api/admin/users/17").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","status":0}
                                """))
                .andExpect(jsonPath("$.code").value(409));

        verify(userMapper, never()).update(any(), any());
    }

    private void authenticateAs(String role) {
        SecurityUser principal = new SecurityUser(17L, "alice", "", List.of(role), true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private SysUser activeUser(String username) {
        SysUser user = new SysUser();
        user.setId(17L);
        user.setUsername(username);
        user.setStatus(1);
        user.setPassword(passwordEncoder.encode("secret123"));
        return user;
    }

    @Configuration(proxyBeanMethods = false)
    @Import({AuthController.class, AdminUserController.class})
    @ComponentScan("com.example.dgkl.auth.service")
    static class TestConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(4);
        }
    }
}
