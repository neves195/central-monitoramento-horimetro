package br.com.manutencao.central_manutencao;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/**
 * Exercita os fluxos principais pela API, do JSON de entrada ao banco H2 real (migrations incluídas).
 * Os testes de service cobrem cada regra com o repository simulado; aqui o que se prova é a
 * integração: rota, validação, transação, consulta e formato da resposta.
 *
 * Roda dentro de uma transação desfeita ao fim de cada teste, então um teste não enxerga o outro.
 * O usuário vem de @WithMockUser: a autenticação de verdade é coberta pelo SecurityIntegrationTest.
 */
/**
 * Fluxos completos da API com MockMvc e banco real (H2): cadastro de equipamento, leituras, planos,
 * manutenções e alertas, incluindo os códigos de erro 400 e 409. Cada teste desfaz o que gravou
 * (@Transactional) e roda como ADMIN; as regras de papel ficam no SecurityIntegrationTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
class ApiFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    // ---- equipamento ----

    @Test
    void createsEquipmentAndListsIt() throws Exception {
        postJson("/api/equipments", equipmentJson("INT-001"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/equipments/")))
                .andExpect(jsonPath("$.code").value("INT-001"))
                .andExpect(jsonPath("$.currentHours").value(0))
                .andExpect(jsonPath("$.active").value(true));

        mvc.perform(get("/api/equipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("INT-001")));
    }

    @Test
    void rejectsDuplicateCodeWith409() throws Exception {
        postJson("/api/equipments", equipmentJson("ESC-001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messages[0]").value("Equipamento já cadastrado: ESC-001"));
    }

    @Test
    void rejectsBlankFieldsWith400ListingEachField() throws Exception {
        postJson("/api/equipments", "{\"code\": \"\", \"name\": \"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasSize(2)));
    }

    @Test
    void deactivatedEquipmentLeavesListAndComesBackAfterReactivate() throws Exception {
        long equipmentId = createEquipment("INT-002");

        mvc.perform(delete("/api/equipments/" + equipmentId))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/equipments"))
                .andExpect(jsonPath("$[*].code", not(hasItem("INT-002"))));

        // Desativado continua acessível por id: é a decisão do findById
        mvc.perform(get("/api/equipments/" + equipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(post("/api/equipments/" + equipmentId + "/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    // ---- leitura ----

    @Test
    void readingUpdatesEquipmentHours() throws Exception {
        long equipmentId = createEquipment("INT-003");

        registerReading(equipmentId, "150.5")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hours").value(150.5));

        mvc.perform(get("/api/equipments/" + equipmentId))
                .andExpect(jsonPath("$.currentHours").value(150.5));
        mvc.perform(get("/api/equipments/" + equipmentId + "/readings"))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void rejectsReadingBelowCurrentHours() throws Exception {
        long equipmentId = createEquipment("INT-004");
        registerReading(equipmentId, "100.0").andExpect(status().isCreated());

        registerReading(equipmentId, "99.9").andExpect(status().isConflict());

        mvc.perform(get("/api/equipments/" + equipmentId))
                .andExpect(jsonPath("$.currentHours").value(100.0));
    }

    // ---- plano ----

    @Test
    void createsPlanWithCalculatedFields() throws Exception {
        long equipmentId = createEquipment("INT-005");

        postJson("/api/equipments/" + equipmentId + "/plans", planJson())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/plans/")))
                .andExpect(jsonPath("$.lastMaintenanceHours").value(0))
                .andExpect(jsonPath("$.nextDueHours").value(100.0))
                .andExpect(jsonPath("$.remainingHours").value(100.0))
                .andExpect(jsonPath("$.status").value("ON_TIME"));
    }

    // Plano de 100h com aviso de 10h: 95h cai na faixa de aviso e 100h é o vencimento.
    // O status muda só com a leitura, sem ninguém atualizar o plano
    @Test
    void planStatusFollowsReadingsWithoutBeingSaved() throws Exception {
        long equipmentId = createEquipment("INT-006");
        long planId = createPlan(equipmentId);

        registerReading(equipmentId, "95.0");
        mvc.perform(get("/api/plans/" + planId)).andExpect(jsonPath("$.status").value("UPCOMING"));

        registerReading(equipmentId, "100.0");
        mvc.perform(get("/api/plans/" + planId)).andExpect(jsonPath("$.status").value("OVERDUE"));
    }

    // ---- manutenção + alerta ----

    // Fluxo principal do sistema: plano vencido gera alerta; concluir a manutenção reinicia a
    // contagem do plano e o alerta some; depois de concluída, não dá mais para cancelar (409)
    @Test
    void completingMaintenanceResetsPlanAndClearsAlert() throws Exception {
        long equipmentId = createEquipment("INT-007");
        long planId = createPlan(equipmentId);
        registerReading(equipmentId, "120.0");

        mvc.perform(get("/api/alerts"))
                .andExpect(jsonPath("$[*].equipmentCode", hasItem("INT-007")));

        long maintenanceId = idOf(postJson("/api/equipments/" + equipmentId + "/maintenances", """
                {"planId": %d, "scheduledDate": "2030-01-15", "description": "Troca de oleo"}
                """.formatted(planId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED")));

        mvc.perform(post("/api/maintenances/" + maintenanceId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.hoursAtMaintenance").value(120.0));

        mvc.perform(get("/api/plans/" + planId))
                .andExpect(jsonPath("$.lastMaintenanceHours").value(120.0))
                .andExpect(jsonPath("$.status").value("ON_TIME"));
        mvc.perform(get("/api/alerts"))
                .andExpect(jsonPath("$[*].equipmentCode", not(hasItem("INT-007"))));

        // COMPLETED é estado final
        mvc.perform(post("/api/maintenances/" + maintenanceId + "/cancel"))
                .andExpect(status().isConflict());
    }

    // Cancelar não é fazer: a contagem do plano continua de onde estava
    @Test
    void canceledMaintenanceDoesNotChangePlan() throws Exception {
        long equipmentId = createEquipment("INT-008");
        long planId = createPlan(equipmentId);

        long maintenanceId = idOf(postJson("/api/equipments/" + equipmentId + "/maintenances", """
                {"planId": %d, "scheduledDate": "2030-01-15", "description": "Troca de oleo"}
                """.formatted(planId)));

        mvc.perform(post("/api/maintenances/" + maintenanceId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        mvc.perform(get("/api/plans/" + planId))
                .andExpect(jsonPath("$.lastMaintenanceHours").value(0.0));
    }

    @Test
    void alertsComeMostUrgentFirst() throws Exception {
        // Dados da migration V2: 3 vencidos e 3 próximos
        mvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].equipmentCode").value("GER-001"))
                .andExpect(jsonPath("$[0].status").value("OVERDUE"))
                .andExpect(jsonPath("$[0].remainingHours").value(-350.5))
                .andExpect(jsonPath("$[5].status").value("UPCOMING"))
                .andExpect(jsonPath("$[5].remainingHours").value(19.5));
    }

    @Test
    void alertsCanBeFilteredByStatus() throws Exception {
        mvc.perform(get("/api/alerts").param("status", "UPCOMING"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].status", not(hasItem("OVERDUE"))));
    }

    // ---- ajudantes ----

    private ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String equipmentJson(String code) {
        return "{\"code\": \"" + code + "\", \"name\": \"Equipamento de teste\", \"category\": \"Teste\"}";
    }

    // Intervalo 100, aviso 10: fica ON_TIME até 89,9 h, UPCOMING de 90 a 99,9 h e OVERDUE a partir de 100 h
    private String planJson() {
        return "{\"name\": \"Troca de oleo\", \"intervalHours\": 100.0, \"warningHours\": 10.0}";
    }

    private long createEquipment(String code) throws Exception {
        return idOf(postJson("/api/equipments", equipmentJson(code)));
    }

    private long createPlan(long equipmentId) throws Exception {
        return idOf(postJson("/api/equipments/" + equipmentId + "/plans", planJson()));
    }

    private ResultActions registerReading(long equipmentId, String hours) throws Exception {
        return postJson("/api/equipments/" + equipmentId + "/readings",
                "{\"hours\": " + hours + ", \"registeredBy\": \"Teste\"}");
    }

    private long idOf(ResultActions result) throws Exception {
        Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
