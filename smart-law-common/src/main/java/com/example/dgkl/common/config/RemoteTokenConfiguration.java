package com.example.dgkl.common.config;

import com.example.dgkl.common.security.AccessTokenVerifier;
import com.example.dgkl.common.security.IntrospectionUnavailableException;
import com.example.dgkl.common.security.OAuth2IntrospectionResponse;
import com.example.dgkl.common.security.OAuth2IntrospectionSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RemoteTokenConfiguration {
    @Bean
    public AccessTokenVerifier remoteAccessTokenVerifier(
            RestClient.Builder builder, OAuth2IntrospectionSettings settings) {
        HttpClient transport = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(transport);
        factory.setReadTimeout(Duration.ofSeconds(3));
        RestClient client = builder.requestFactory(factory).defaultHeaders(settings::authorize).build();
        return token -> {
            var form = new LinkedMultiValueMap<String, String>();
            form.add("token", token);
            form.add("token_type_hint", "access_token");
            OAuth2IntrospectionResponse response;
            try {
                response = client.post().uri(settings.uri()).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form).retrieve().body(OAuth2IntrospectionResponse.class);
            } catch (RestClientException exception) {
                log.warn("令牌内省调用失败，异常类型 {}", exception.getClass().getSimpleName());
                throw new IntrospectionUnavailableException();
            }
            if (response == null) {
                throw new IntrospectionUnavailableException();
            }
            return response.principal();
        };
    }
}
