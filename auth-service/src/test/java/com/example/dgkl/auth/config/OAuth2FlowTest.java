package com.example.dgkl.auth.config;

import com.example.dgkl.auth.controller.AuthController;
import com.example.dgkl.auth.controller.OAuth2CsrfController;
import com.example.dgkl.auth.dto.UserView;
import com.example.dgkl.auth.service.OAuth2SessionService;
import com.example.dgkl.auth.service.UserService;
import com.example.dgkl.common.config.ServiceSecurityConfig;
import com.example.dgkl.common.security.InternalServiceToken;
import com.example.dgkl.common.security.SecurityUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(OAuth2FlowTest.TestConfig.class)
@TestPropertySource(properties = "OAUTH2_INTROSPECTION_CLIENT_SECRET=" + OAuth2FlowTest.CLIENT_SECRET)
class OAuth2FlowTest {
    static final String CLIENT_SECRET = "test-introspection-secret-32-bytes-minimum";
    private static final String REDIRECT_URI = "http://localhost:5173/oauth/callback";
    private static final String VERIFIER = "pSkD_W9X9kHgK7MRqYRrPoAjltRogpa3It6TSc_97Wo";
    private static final String TEST_PASSWORD = "classroom-test-password";
    private static final String SAS_SCHEMA_ROOT = "classpath:org/springframework/security/oauth2/server/authorization/";

    @Autowired
    private WebApplicationContext webContext;
    @Autowired
    private FilterChainProxy filters;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private JdbcTemplate jdbc;
    @MockBean
    private UserService users;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM oauth2_authorization");
        mvc = MockMvcBuilders.webAppContextSetup(webContext).addFilters(filters).build();
        when(users.loadForAuthentication("alice")).thenReturn(User.withUsername("17")
                .password(encoder.encode(TEST_PASSWORD)).roles("USER").build());
    }

    @Test
    void loginWithoutCsrfUsesStructuredForbidden() throws Exception {
        MvcResult csrf = mvc.perform(get("/api/oauth2/csrf")).andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) csrf.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mvc.perform(post("/api/oauth2/login").session(session)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "alice").param("password", TEST_PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("登录请求已过期，请刷新后重试"));

        verify(users, never()).loadForAuthentication("alice");
    }

    @ParameterizedTest
    @ValueSource(strings = {REDIRECT_URI, "http://localhost:5174/oauth/callback",
            "http://127.0.0.1:5173/oauth/callback", "http://127.0.0.1:5174/oauth/callback"})
    void authorizesWithPkcePersistsTokenUsesCurrentRoleAndRevokesOnLogout(String redirectUri) throws Exception {
        AtomicReference<SecurityUser> account = new AtomicReference<>(principal("ADMIN"));
        when(users.findActivePrincipal(17L)).thenAnswer(invocation -> Optional.ofNullable(account.get()));
        when(users.currentProfile())
                .thenReturn(new UserView(17L, "alice", "Alice", null, null, List.of("ADMIN"), 1));

        AuthorizedCode authorization = authorize(redirectUri);
        MvcResult exchange = mvc.perform(post("/api/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code").param("client_id", "smart-law-spa")
                        .param("code", authorization.code()).param("code_verifier", VERIFIER)
                        .param("redirect_uri", redirectUri))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.refresh_token").doesNotExist())
                .andReturn();
        String token = body(exchange).get("access_token").asText();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM oauth2_authorization WHERE access_token_value IS NOT NULL", Integer.class))
                .isEqualTo(1);

        mvc.perform(introspect(token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.user_id").value(17)).andExpect(jsonPath("$.roles[0]").value("ADMIN"));

        account.set(principal("USER"));
        mvc.perform(introspect(token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("USER"));
        account.set(null);
        mvc.perform(introspect(token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        account.set(principal("USER"));
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(17));

        mvc.perform(post("/api/auth/logout").session(authorization.session())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(authorization.session().isInvalid()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oauth2_authorization", Integer.class)).isZero();
        mvc.perform(introspect(token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authorizationCodeCannotBeExchangedWithWrongVerifier() throws Exception {
        AuthorizedCode authorization = authorize(REDIRECT_URI);

        mvc.perform(post("/api/oauth2/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code").param("client_id", "smart-law-spa")
                        .param("code", authorization.code())
                        .param("code_verifier", "invalid_pkce_verifier_that_has_sufficient_length_for_validation")
                        .param("redirect_uri", REDIRECT_URI))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.access_token").doesNotExist());
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM oauth2_authorization WHERE access_token_value IS NOT NULL", Integer.class))
                .isZero();
    }

    private AuthorizedCode authorize(String redirectUri) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(VERIFIER.getBytes(StandardCharsets.US_ASCII));
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        MvcResult loginRedirect = mvc.perform(get("/api/oauth2/authorize").accept(MediaType.TEXT_HTML)
                        .queryParam("response_type", "code").queryParam("client_id", "smart-law-spa")
                        .queryParam("scope", "smart-law-api").queryParam("redirect_uri", redirectUri)
                        .queryParam("state", "state-verification")
                        .queryParam("code_challenge", challenge).queryParam("code_challenge_method", "S256"))
                .andExpect(status().isFound()).andExpect(header().string(HttpHeaders.LOCATION, "/login?oauth=1"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginRedirect.getRequest().getSession(false);
        assertThat(session).isNotNull();
        MvcResult csrf = mvc.perform(get("/api/oauth2/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        JsonNode csrfData = body(csrf).get("data");
        MvcResult loggedIn = mvc.perform(post("/api/oauth2/login").session(session)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "alice").param("password", TEST_PASSWORD)
                        .param(csrfData.get("parameterName").asText(), csrfData.get("token").asText()))
                .andExpect(status().isFound()).andReturn();
        MockHttpSession authenticated = (MockHttpSession) loggedIn.getRequest().getSession(false);
        URI savedRequest = URI.create(loggedIn.getResponse().getHeader(HttpHeaders.LOCATION));
        assertThat(savedRequest.getPath()).isEqualTo("/api/oauth2/authorize");
        MvcResult callback = mvc.perform(get(savedRequest).session(authenticated))
                .andExpect(status().isFound()).andReturn();
        String location = callback.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location).startsWith(redirectUri + "?");
        var query = UriComponentsBuilder.fromUriString(location).build().getQueryParams();
        assertThat(query.getFirst("state")).isEqualTo("state-verification");
        String code = query.getFirst("code");
        assertThat(code).isNotBlank();
        return new AuthorizedCode(code, authenticated);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder introspect(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("smart-law-resource-server", CLIENT_SECRET);
        return post("/internal/oauth2/introspect").headers(headers).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", token).param("token_type_hint", "access_token");
    }

    private JsonNode body(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private SecurityUser principal(String role) {
        return new SecurityUser(17L, "alice", "", List.of(role), true);
    }

    private record AuthorizedCode(String code, MockHttpSession session) {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @Import({OAuth2PersistenceConfig.class, OAuth2SecurityConfig.class, OAuth2SessionService.class,
            OAuth2CsrfController.class, AuthController.class, ServiceSecurityConfig.class})
    static class TestConfig {
        @Bean(destroyMethod = "shutdown")
        EmbeddedDatabase dataSource() {
            return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2)
                    .addScripts(SAS_SCHEMA_ROOT + "client/oauth2-registered-client-schema.sql",
                            SAS_SCHEMA_ROOT + "oauth2-authorization-schema.sql",
                            SAS_SCHEMA_ROOT + "oauth2-authorization-consent-schema.sql")
                    .build();
        }

        @Bean
        JdbcTemplate jdbcTemplate(EmbeddedDatabase database) {
            return new JdbcTemplate(database);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(4);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean
        InternalServiceToken internalServiceToken() {
            return new InternalServiceToken("integration-test-internal-token");
        }
    }
}
