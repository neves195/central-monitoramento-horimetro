package br.com.manutencao.central_manutencao.security;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testa a cadeia de segurança real, com as senhas de verdade: por isso usa httpBasic(...)
 * em vez de @WithMockUser. O @Transactional desfaz o que cada teste grava no banco.
 */
/**
 * Segurança da API com os filtros reais: 401 sem login ou com senha errada, 403 para VIEWER em
 * escrita, acesso liberado para o papel certo e o desvio do desafio Basic para o frontend.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

    private static final String EQUIPMENT_JSON = """
            {"code": "SEC-001", "name": "Equipamento do teste de seguranca"}
            """;

    @Autowired
    private MockMvc mvc;

    // ---- 401: não sabemos quem é ----

    @Test
    void requestWithoutLoginReturns401InApiErrorFormat() throws Exception {
        assertApiError(mvc.perform(get("/api/equipments")), 401, "Unauthorized");
    }

    @Test
    void wrongPasswordReturns401InApiErrorFormat() throws Exception {
        assertApiError(mvc.perform(get("/api/equipments").with(httpBasic("admin", "senha-errada"))), 401, "Unauthorized");
    }

    @Test
    void unknownUserReturns401() throws Exception {
        mvc.perform(get("/api/equipments").with(httpBasic("ninguem", "admin123")))
                .andExpect(status().isUnauthorized());
    }

    // ---- 403: sabemos quem é, mas o papel não permite ----

    @Test
    void viewerPostReturns403InApiErrorFormat() throws Exception {
        assertApiError(mvc.perform(post("/api/equipments").with(httpBasic("viewer", "viewer123"))
                .contentType(MediaType.APPLICATION_JSON).content(EQUIPMENT_JSON)), 403, "Forbidden");
    }

    @Test
    void viewerPutReturns403() throws Exception {
        mvc.perform(put("/api/equipments/1").with(httpBasic("viewer", "viewer123"))
                .contentType(MediaType.APPLICATION_JSON).content(EQUIPMENT_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerDeleteReturns403() throws Exception {
        mvc.perform(delete("/api/equipments/1").with(httpBasic("viewer", "viewer123")))
                .andExpect(status().isForbidden());
    }

    // ---- 2xx: papel certo ----

    @Test
    void viewerCanRead() throws Exception {
        mvc.perform(get("/api/equipments").with(httpBasic("viewer", "viewer123")))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanRead() throws Exception {
        mvc.perform(get("/api/equipments").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());
    }

    @Test
    void adminPostReturns201() throws Exception {
        mvc.perform(post("/api/equipments").with(httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON).content(EQUIPMENT_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SEC-001"));
    }

    // ---- rota usada pela tela de login ----

    @Test
    void meReturnsUsernameAndRole() throws Exception {
        mvc.perform(get("/api/me").with(httpBasic("viewer", "viewer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("viewer"))
                .andExpect(jsonPath("$.roles[0]").value("VIEWER"));
    }

    // Cliente comum (curl, navegador direto) recebe o desafio Basic e pode pedir as credenciais
    @Test
    void curlStyleClientReceivesBasicChallenge() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));
    }

    // Sem este desvio, o navegador abriria a janela nativa de login por cima da tela do Vue
    @Test
    void browserAppDoesNotReceiveBasicChallenge() throws Exception {
        mvc.perform(get("/api/me").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    // Mesmo formato do GlobalExceptionHandler: {status, error, messages[], timestamp}
    private void assertApiError(ResultActions result, int code, String error) throws Exception {
        result.andExpect(status().is(code))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(code))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.messages[0]").isNotEmpty())
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }
}
