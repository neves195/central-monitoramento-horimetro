package br.com.manutencao.central_manutencao.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import br.com.manutencao.central_manutencao.common.GlobalExceptionHandler.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Gera os erros 401 e 403 no mesmo formato JSON do resto da API.
 *
 * Esses erros acontecem nos filtros de segurança, ANTES de a requisição chegar a um
 * controller. Por isso o GlobalExceptionHandler não os enxerga e a resposta é escrita aqui.
 */
@Component
public class SecurityErrorHandler {

    private final ObjectMapper objectMapper;

    /** Recebe o ObjectMapper do Spring para serializar o corpo do erro. */
    public SecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 401: não sabemos quem é (sem credenciais ou credenciais erradas). */
    AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            // Este header faz o navegador abrir a janela nativa de login. O frontend se identifica
            // com X-Requested-With para recebê-lo sem o header e mostrar o erro na própria tela.
            if (!"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                response.setHeader("WWW-Authenticate", "Basic realm=\"central-manutencao\"");
            }
            write(response, HttpStatus.UNAUTHORIZED, "Credenciais ausentes ou inválidas");
        };
    }

    /** 403: sabemos quem é, mas o papel dele não permite a operação. */
    AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                write(response, HttpStatus.FORBIDDEN, "Seu perfil não tem permissão para esta operação");
    }

    // Reaproveita o record ApiError: o cliente trata todos os erros da API do mesmo jeito.
    // A mensagem é fixa de propósito, para não expor detalhes internos da autenticação.
    private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(), List.of(message), LocalDateTime.now());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
