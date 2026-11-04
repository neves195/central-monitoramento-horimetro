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
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Passo 2a: codificador de senhas
    // - Criar um bean PasswordEncoder que devolve um BCryptPasswordEncoder.
    // - Pistas: anotação @Bean em cima de um método público; o tipo de retorno é PasswordEncoder
    //   (da interface), mas o objeto criado é o BCrypt.
    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    // Passo 2b: usuários em memória (fazemos depois do 2a)
    // - Dois usuários: admin (papel ADMIN) e viewer (papel VIEWER).
    // - As senhas NUNCA entram em texto puro: sempre passam por encoder.encode(...).
    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService(
            @Value("${app.security.admin-password}") String adminPassword,
            @Value("${app.security.viewer-password}") String viewerPassword) {

        var admin = org.springframework.security.core.userdetails.User.withUsername("admin")
                .password(passwordEncoder().encode(adminPassword))
                .roles("ADMIN")
                .build();

        var viewer = org.springframework.security.core.userdetails.User.withUsername("viewer")
                .password(passwordEncoder().encode(viewerPassword))
                .roles("VIEWER")
                .build();

        return new org.springframework.security.provisioning.InMemoryUserDetailsManager(admin, viewer);
    }

    // Passo 3a: regras de acesso (SecurityFilterChain)
    // - Um bean SecurityFilterChain, que recebe um HttpSecurity e devolve http.build().
    //   Pista: o método declara "throws Exception".
    // 1. Stateless: a API não guarda sessão (cada requisição manda as credenciais).
    //    Pista: sessionManagement + SessionCreationPolicy.STATELESS
    // 2. CSRF desligado: ele protege formulários com cookie de sessão, e aqui não há sessão nem cookie.
    //    Pista: csrf + disable
    // 3. Regras por papel e método HTTP, em authorizeHttpRequests + requestMatchers:
    //    - GET em /api/**                 -> ADMIN e VIEWER
    //    - POST, PUT e DELETE em /api/**  -> só ADMIN
    //    - qualquer outra rota            -> precisa estar logado
    // 4. Login por HTTP Basic (o mesmo que o curl -u usa).
    //    Pista: httpBasic + Customizer.withDefaults()

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandler errorHandler) throws Exception {
        http
                // Stateless: sem sessão, cada requisição leva as credenciais
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CSRF desligado: sem sessão e sem cookie, não há o que explorar
                .csrf(csrf -> csrf.disable())
                // Vale a PRIMEIRA regra que combina, por isso o anyRequest fica por último
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("ADMIN", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // Login por HTTP Basic (o que o curl -u usa)
                // A senha errada é tratada pelo próprio filtro do Basic, por isso ele também recebe o entry point
                .httpBasic(basic -> basic.authenticationEntryPoint(errorHandler.authenticationEntryPoint()))
                // Erros do filtro (antes do controller): sem isto o 401 vem vazio e o 403 no formato padrão
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(errorHandler.authenticationEntryPoint())
                        .accessDeniedHandler(errorHandler.accessDeniedHandler()));

        return http.build();
    }

    // Passo 3b: console do H2 liberado SOMENTE no perfil "dev"
    // - Sem o perfil dev esta cadeia nem existe, e o /h2-console cai na regra acima (exige login).
    // - Para ligar: ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
    // - O console é aberto dentro de um frame da própria página, por isso o frameOptions sameOrigin.
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
