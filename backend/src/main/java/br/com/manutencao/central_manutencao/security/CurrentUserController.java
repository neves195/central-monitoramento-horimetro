package br.com.manutencao.central_manutencao.security;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Diz ao frontend quem está logado. No HTTP Basic não existe rota de login: as credenciais
 * valem a cada requisição, então esta rota serve para testá-las e descobrir o papel.
 */
@RestController
public class CurrentUserController {

    /** Usuário logado: nome e papéis sem o prefixo ROLE_ (ex: ADMIN). */
    public record CurrentUserResponse(String username, List<String> roles) {
    }

    /**
     * GET /api/me. Papel necessário: ADMIN ou VIEWER (a regra de GET em /api/** do SecurityConfig já exige login).
     */
    @GetMapping("/api/me")
    public CurrentUserResponse me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .toList();
        return new CurrentUserResponse(authentication.getName(), roles);
    }
}
