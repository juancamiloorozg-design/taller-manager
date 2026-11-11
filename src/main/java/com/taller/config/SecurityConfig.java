package com.taller.config;

import com.taller.security.LoginExitosoHandler;
import com.taller.security.LoginFallidoHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // Todo se sirve desde el propio sitio: sin scripts, estilos ni fuentes de terceros.
    private static final String POLITICA_CSP = "default-src 'self'; "
            + "script-src 'self'; "
            + "style-src 'self'; "
            + "img-src 'self' data:; "
            + "font-src 'self'; "
            + "object-src 'none'; "
            + "base-uri 'self'; "
            + "form-action 'self'; "
            + "frame-ancestors 'none'";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           LoginExitosoHandler loginExitoso,
                                           LoginFallidoHandler loginFallido) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/legal/**", "/css/**", "/js/**", "/vendor/**",
                                "/favicon.ico", "/error", "/actuator/health").permitAll()
                        .requestMatchers("/usuarios/**", "/admin/**", "/informes/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(loginExitoso)
                        .failureHandler(loginFallido)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(POLITICA_CSP))
                        .referrerPolicy(ref -> ref.policy(ReferrerPolicy.SAME_ORIGIN)));
        return http.build();
    }
}
