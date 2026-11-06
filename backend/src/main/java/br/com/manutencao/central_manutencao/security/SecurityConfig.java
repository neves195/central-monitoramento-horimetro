package br.com.manutencao.central_manutencao.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança da API: HTTP Basic, sem sessão, com permissões por papel e método.
 *
 * Papéis: ADMIN lê e escreve; VIEWER só lê. Os usuários ficam em memória porque o escopo
 * do projeto não inclui cadastro de usuários (veja "Evolução para JWT" no README).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Codificador de senhas BCrypt: as senhas nunca ficam em texto puro na memória. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Usuários em memória: {@code admin} (ADMIN) e {@code viewer} (VIEWER).
     * As senhas vêm de {@code application.properties} e passam pelo encoder antes de entrar.
     */
    @Bean
    public UserDetailsService userDetailsService(
            @Value("${app.security.admin-password}") String adminPassword,
            @Value("${app.security.viewer-password}") String viewerPassword) {

        var admin = User.withUsername("admin")
                .password(passwordEncoder().encode(adminPassword))
                .roles("ADMIN")
                .build();

        var viewer = User.withUsername("viewer")
                .password(passwordEncoder().encode(viewerPassword))
                .roles("VIEWER")
                .build();

        return new InMemoryUserDetailsManager(admin, viewer);
    }

    /**
     * Cadeia principal de filtros: stateless, sem CSRF, regras por papel e login HTTP Basic.
     *
     * Os erros 401 e 403 são escritos pelo {@link SecurityErrorHandler}, no mesmo formato JSON
     * do restante da API.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandler errorHandler) throws Exception {
        http
                // Stateless: sem sessão, cada requisição leva as credenciais
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CSRF desligado: ele protege sessões baseadas em cookie, e aqui não há sessão nem cookie
                .csrf(csrf -> csrf.disable())
                // Vale a PRIMEIRA regra que combina, por isso o anyRequest fica por último
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("ADMIN", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // A senha errada é tratada pelo próprio filtro do Basic, por isso ele também recebe o entry point
                .httpBasic(basic -> basic.authenticationEntryPoint(errorHandler.authenticationEntryPoint()))
                // Erros do filtro acontecem antes do controller: sem isto o 401 vem vazio e o 403 no formato padrão
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(errorHandler.authenticationEntryPoint())
                        .accessDeniedHandler(errorHandler.accessDeniedHandler()));

        return http.build();
    }

    /**
     * Libera o console do H2 SOMENTE no perfil "dev". Sem o perfil, esta cadeia não existe e o
     * /h2-console cai na regra geral (exige login). Para ligar:
     * {@code ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev}.
     *
     * O console abre dentro de um frame da própria página, por isso o frameOptions sameOrigin.
     */
    @Bean
    @Profile("dev")
    @Order(1)
    public SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/h2-console/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
