package com.window.global.config;

import com.window.domain.whitelist.repository.WhitelistRepository;
import com.window.global.security.handler.CustomExceptionHandler;
import com.window.global.security.jwt.JwtAuthorizationFilter;
import com.window.global.security.jwt.JwtTokenProvider;
import com.window.global.security.oauth.OAuth2FailHandler;
import com.window.global.security.oauth.OAuth2Service;
import com.window.global.security.oauth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final OAuth2Service oAuth2Service;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailHandler oAuth2FailHandler;
    private final CustomExceptionHandler customExceptionHandler;
    private final WhitelistRepository whitelistRepository;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtAuthorizationFilter jwtAuthorizationFilter(){
        return new JwtAuthorizationFilter(jwtTokenProvider, whitelistRepository);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf((csrf) -> csrf.disable());
        http.cors(Customizer.withDefaults());
        http.formLogin((formLogin) -> formLogin.disable());
        http.httpBasic((httpBasic) -> httpBasic.disable());
        http.authorizeHttpRequests((requests) -> requests
                .requestMatchers("/members/reissue").permitAll()
                .requestMatchers("/sensors/**").permitAll()
                .requestMatchers("/windows/open/auto/**", "/windows/close/auto/**", "/windows/state/**").hasAuthority("WHITELIST")
                .requestMatchers("/places/**", "/schedules/**", "/windows/**", "/reports/**").hasRole("USER")
                .requestMatchers("/actions/avg/**").hasRole("USER")
                .requestMatchers("/actions/**").hasAuthority("WHITELIST")
                .anyRequest().authenticated());


        http.oauth2Login((oauth) ->
                oauth.userInfoEndpoint(c -> c.userService(oAuth2Service))
                        .redirectionEndpoint(
                                (redirectionEndpointConfig) -> redirectionEndpointConfig.baseUri(("/login/oauth2/code/*")))
                        .authorizationEndpoint((authorizationEndpointConfig) ->
                                authorizationEndpointConfig.baseUri("/oauth2/authorization"))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailHandler)
        );

        http.exceptionHandling((handle) -> handle.authenticationEntryPoint(customExceptionHandler));
        http.addFilterBefore(jwtAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

}
