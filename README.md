# Central de Monitoramento de Manutenção Preventiva por Horímetro

Sistema para acompanhar a manutenção preventiva de equipamentos pelo horímetro, o medidor de horas trabalhadas. Cada equipamento tem planos do tipo "trocar o óleo a cada 250 horas". A partir das leituras do horímetro, o sistema calcula quais planos estão em dia, próximos do vencimento ou vencidos, e mostra um painel de alertas com o mais urgente no topo.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4.1.2, Spring Web MVC, Spring Data JPA, Bean Validation, Spring Security |
| Banco | H2 em memória, com esquema versionado pelo Flyway |
| Testes do backend | JUnit 5, Mockito, MockMvc |
| Frontend | Vue 3, Vue Router, Vite |

## Pré-requisitos

- Java 21. O Maven não precisa estar instalado, o projeto traz o Maven Wrapper.
- Node.js 20.19 ou mais novo (ou 22.12 ou mais novo), exigência do Vite 8.

## Como rodar

Use dois terminais, a partir da raiz do repositório.

**Backend** (porta 8080):

```bash
cd backend
./mvnw spring-boot:run        # no Windows: .\mvnw.cmd spring-boot:run
```

**Frontend** (porta 5173):

```bash
cd frontend
npm install
npm run dev
```

Abra <http://localhost:5173>. O Vite repassa as chamadas de `/api` para o backend. Se o backend estiver em outro endereço, defina `BACKEND_URL` antes do `npm run dev`.

O banco já sobe com dados de exemplo: 6 equipamentos e 10 planos, sendo 3 vencidos, 3 próximos e 4 em dia. Como é H2 em memória, tudo volta ao estado inicial quando o backend reinicia.

### Usuários de teste

| Usuário | Senha | Papel | Pode |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | ler e escrever |
| `viewer` | `viewer123` | VIEWER | apenas ler |

As senhas ficam em `application.properties` só porque este é um ambiente de desenvolvimento.

### Console do H2 (opcional)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Abra <http://localhost:8080/h2-console> com a URL JDBC `jdbc:h2:mem:manutencao`, usuário `sa` e senha vazia. Sem o perfil `dev`, o console não fica liberado.

## Estrutura do projeto

```text
.
├── README.md
├── backend/                           API em Spring Boot
│   ├── pom.xml
│   ├── mvnw, mvnw.cmd                 Maven Wrapper
│   └── src/
│       ├── main/
│       │   ├── java/br/com/manutencao/central_manutencao/
│       │   │   ├── CentralManutencaoApplication.java
│       │   │   ├── equipment/
│       │   │   ├── reading/
│       │   │   ├── plan/
│       │   │   ├── maintenance/
│       │   │   ├── alert/
│       │   │   ├── security/
│       │   │   └── common/
│       │   └── resources/
│       │       ├── application.properties
│       │       └── db/migration/      V1 (esquema) e V2 (dados de exemplo)
│       └── test/                      testes, com a mesma divisão de pacotes
└── frontend/                          Vue 3 + Vite
    ├── index.html
    ├── vite.config.js                 plugin do Vue e proxy de /api
    ├── public/                        arquivos estáticos (favicon)
    └── src/
        ├── main.js, App.vue, router.js
        ├── api/
        ├── components/
        ├── stores/
        ├── views/
        ├── format.js
        └── styles.css
```

Cada pacote do backend agrupa controller, service, repository, entidade e os records de request e response de um assunto. O controller só traduz HTTP, e as regras de negócio ficam no service.

| Pacote | Papel |
|---|---|
| `equipment` | Equipamentos monitorados: cadastro, código único, horímetro atual e exclusão lógica |
| `reading` | Leituras do horímetro: histórico imutável que atualiza o horímetro do equipamento |
| `plan` | Planos de manutenção preventiva (a cada N horas) e o cálculo de vencimento, restante e status |
| `maintenance` | Manutenções agendadas, concluídas ou canceladas; concluir zera a contagem do plano |
| `alert` | Painel de alertas: planos vencidos e próximos, calculados na hora, sem tabela própria |
| `security` | HTTP Basic, papéis ADMIN e VIEWER, rota `/api/me` e os erros 401 e 403 em JSON |
| `common` | Exceções de negócio (404 e 409) e o `GlobalExceptionHandler`, que define o formato único de erro |

| Pasta do frontend | Papel |
|---|---|
| `api/` | Cliente HTTP que injeta as credenciais e converte erros da API em `ApiError` |
| `components/` | Peças reutilizáveis: cabeçalho do equipamento e selo de status |
| `stores/` | Estado de autenticação, só em memória |
| `views/` | Uma tela por rota: login, alertas, equipamentos, leituras, planos e manutenções |

## Rotas da API

Todas as rotas exigem HTTP Basic. `GET` aceita ADMIN e VIEWER; `POST`, `PUT` e `DELETE` são só de ADMIN.

| Método | Rota | O que faz | Sucesso |
|---|---|---|---|
| GET | `/api/me` | Usuário logado e papéis | 200 |
| GET | `/api/alerts` | Planos vencidos e próximos, mais urgente primeiro (`?status=OVERDUE` ou `UPCOMING`) | 200 |
| GET | `/api/equipments` | Lista os equipamentos ativos | 200 |
| POST | `/api/equipments` | Cria um equipamento | 201 |
| GET | `/api/equipments/{id}` | Busca por id, inclusive desativado | 200 |
| PUT | `/api/equipments/{id}` | Atualiza código, nome e categoria | 200 |
| DELETE | `/api/equipments/{id}` | Desativa (exclusão lógica) | 204 |
| POST | `/api/equipments/{id}/reactivate` | Reativa | 200 |
| GET | `/api/equipments/{id}/readings` | Histórico de leituras, mais recente primeiro | 200 |
| POST | `/api/equipments/{id}/readings` | Lança uma leitura e atualiza o horímetro | 201 |
| GET | `/api/equipments/{id}/plans` | Lista os planos ativos do equipamento | 200 |
| POST | `/api/equipments/{id}/plans` | Cria um plano | 201 |
| GET | `/api/plans/{id}` | Busca um plano | 200 |
| PUT | `/api/plans/{id}` | Atualiza um plano | 200 |
| DELETE | `/api/plans/{id}` | Desativa um plano | 204 |
| POST | `/api/plans/{id}/reactivate` | Reativa um plano | 200 |
| GET | `/api/equipments/{id}/maintenances` | Lista as manutenções (`?status=SCHEDULED`, `COMPLETED` ou `CANCELED`) | 200 |
| POST | `/api/equipments/{id}/maintenances` | Agenda uma manutenção | 201 |
| GET | `/api/maintenances/{id}` | Busca uma manutenção | 200 |
| POST | `/api/maintenances/{id}/complete` | Conclui (corpo opcional) | 200 |
| POST | `/api/maintenances/{id}/cancel` | Cancela | 200 |

### Exemplos com curl

A sintaxe é de bash; no Windows, use o Git Bash. Nesse ambiente, evite acentos no argumento `-d`, porque o curl pode recebê-los fora de UTF-8 e a API responde 400.

```bash
# Alertas (equipamento 1 = ESC-001 nos dados de exemplo)
curl -u viewer:viewer123 http://localhost:8080/api/alerts
curl -u viewer:viewer123 "http://localhost:8080/api/alerts?status=OVERDUE"

# Criar equipamento
curl -u admin:admin123 -X POST http://localhost:8080/api/equipments \
  -H "Content-Type: application/json" \
  -d '{"code": "BOM-001", "name": "Bomba de lama", "category": "Bomba"}'

# Lançar leitura
curl -u admin:admin123 -X POST http://localhost:8080/api/equipments/1/readings \
  -H "Content-Type: application/json" \
  -d '{"hours": 4900.0, "registeredBy": "Carlos Lima"}'

# Criar plano (sem lastMaintenanceHours, a contagem começa no horímetro atual)
curl -u admin:admin123 -X POST http://localhost:8080/api/equipments/1/plans \
  -H "Content-Type: application/json" \
  -d '{"name": "Troca de correia", "intervalHours": 400.0, "warningHours": 40.0}'

# Agendar manutenção ligada ao plano 1
curl -u admin:admin123 -X POST http://localhost:8080/api/equipments/1/maintenances \
  -H "Content-Type: application/json" \
  -d '{"planId": 1, "scheduledDate": "2030-01-15", "description": "Troca de oleo", "responsible": "Oficina Central"}'

# Concluir (use o id devolvido no agendamento; sem corpo, vale hoje e o horímetro atual)
curl -u admin:admin123 -X POST http://localhost:8080/api/maintenances/8/complete

# Cancelar
curl -u admin:admin123 -X POST http://localhost:8080/api/maintenances/4/cancel

# Desativar e reativar
curl -u admin:admin123 -X DELETE http://localhost:8080/api/equipments/6
curl -u admin:admin123 -X POST http://localhost:8080/api/equipments/6/reactivate
```

### Formato dos erros

Os erros de regra de negócio, validação e segurança têm o mesmo formato:

```json
{
  "status": 409,
  "error": "Conflict",
  "messages": ["A leitura (100.0) nao pode ser menor que o horimetro atual (4820.0)"],
  "timestamp": "2026-11-04T10:30:00"
}
```

| Código | Quando acontece |
|---|---|
| 400 | Campo inválido, com uma mensagem por campo |
| 401 | Sem credenciais, ou credenciais erradas |
| 403 | Papel sem permissão, como VIEWER fazendo POST |
| 404 | Recurso não encontrado |
| 409 | Regra de negócio violada |

## Regras de negócio

**Status do plano.** É calculado a cada consulta:

- próximo vencimento = última manutenção + intervalo;
- restante = próximo vencimento − horímetro atual.

| Status | Condição |
|---|---|
| `OVERDUE` | restante ≤ 0 |
| `UPCOMING` | restante ≤ horas de aviso |
| `ON_TIME` | restante maior que as horas de aviso |

Ao criar um plano sem última manutenção, vale o horímetro atual. Informar uma última manutenção maior que o horímetro atual é recusado (409).

**Leitura.** O horímetro só anda para a frente: uma leitura menor que a atual é recusada (409) e uma igual é aceita. A leitura e a atualização do equipamento acontecem na mesma transação. Leituras nunca são alteradas nem apagadas, e equipamento desativado não aceita leitura.

**Manutenção.** Nasce `SCHEDULED` e só desse estado vai para `COMPLETED` ou `CANCELED`; qualquer outra transição é 409. Concluir sem dados assume a data de hoje e o horímetro atual, e horas acima do horímetro atual são recusadas. Concluir uma manutenção ligada a um plano zera a contagem dele na mesma transação, então um plano vencido volta a ficar em dia. O plano precisa ser do mesmo equipamento e estar ativo. Sem plano, a manutenção é avulsa e não altera nenhum plano.

**Alertas.** São os planos ativos, de equipamentos ativos, com status `OVERDUE` ou `UPCOMING`, do menor restante para o maior. Não existe tabela de alertas: eles aparecem e somem sozinhos conforme o horímetro e as manutenções mudam.

## Decisões de projeto

**H2 em memória.** Rodar o projeto não exige instalar um banco. O preço é perder os dados a cada reinício, o que é aceitável numa demonstração e seria inaceitável em produção.

**Flyway.** O esquema é definido só pelas migrations, e o Hibernate roda com `ddl-auto=validate`: ele confere as entidades contra o banco e nunca altera tabelas. Assim o esquema fica versionado junto com o código.

**Status calculado.** Status, restante e vencimento são métodos da entidade `MaintenancePlan`, nunca colunas. Um status gravado poderia ficar desatualizado e exigiria atualizar vários planos a cada leitura. Calculado, ele nunca diverge e é testável sem Spring nem banco.

**Exclusão lógica.** Equipamentos e planos são desativados em vez de apagados, porque leituras e manutenções são histórico. O `DELETE` só marca `active = false`, e a reativação desfaz isso. Manutenções não têm `DELETE`: o cancelamento é o próprio status.

**`GET /api/equipments/{id}` devolve equipamento desativado.** A listagem mostra só os ativos, mas a busca por id não filtra por `active`, e a resposta traz esse campo. A reativação precisa encontrar o equipamento, e o histórico continua consultável. A proteção está nas escritas: equipamento desativado não aceita leitura, plano nem manutenção (409).

**Reativar não é em cascata.** Reativar um equipamento não reativa os planos dele, porque um plano pode ter sido desativado antes por outro motivo. Plano de equipamento desativado não pode ser reativado (409).

**HTTP Basic com papéis.** É a forma mais simples de autenticar uma API stateless: não há sessão no servidor e cada requisição leva as credenciais, por isso o CSRF fica desligado. As senhas passam por BCrypt mesmo com usuários em memória. Os erros 401 e 403 nascem nos filtros, antes do controller, e por isso são escritos pelo `SecurityErrorHandler`, não pelo `@RestControllerAdvice`.

**Evolução para JWT.** O Basic manda a senha em toda requisição, e o frontend precisa mantê-la em memória. A troca seria: criar `POST /api/auth/login`, que valida usuário e senha e devolve um token assinado de curta duração com o papel; trocar `httpBasic` por `oauth2ResourceServer().jwt()` no `SecurityFilterChain`; e fazer o frontend enviar `Authorization: Bearer`. As regras por papel e método não mudam, e em seguida os usuários iriam para uma tabela.

**Credenciais só em memória no frontend.** No Basic, o header carrega a própria senha, e o `localStorage` ficaria exposto a qualquer XSS. Por isso recarregar a página pede login de novo.

**Proxy em vez de CORS.** O navegador fala só com o servidor do frontend, que repassa `/api` ao backend. O backend não libera nenhuma origem.

## Evolução do banco

Toda mudança de esquema é uma nova migration em `backend/src/main/resources/db/migration`, no padrão `V3__descricao.sql`. Uma migration já aplicada nunca é editada: o Flyway guarda o checksum de cada uma e recusaria subir, e os ambientes deixariam de ter o mesmo histórico. Para corrigir algo, cria-se outra migration.

Hoje existem duas: `V1__schema_inicial.sql` (tabelas) e `V2__dados_exemplo.sql` (dados de demonstração).

Para trocar para PostgreSQL:

1. Adicionar as dependências `org.postgresql:postgresql` e `org.flywaydb:flyway-database-postgresql`.
2. Apontar `spring.datasource.url`, `username` e `password` para o novo banco, com as credenciais em variáveis de ambiente.
3. Mover a V2 para outro diretório e incluí-lo em `spring.flyway.locations` só nos ambientes de demonstração, para os dados de exemplo não irem à produção.
4. Rodar a suíte de testes contra o PostgreSQL, por exemplo com Testcontainers.

As migrations usam SQL padrão (`GENERATED BY DEFAULT AS IDENTITY`, `NUMERIC`, `TIMESTAMP`) justamente para facilitar essa troca, mas elas ainda não foram executadas em PostgreSQL.

## Testes

```bash
cd backend
./mvnw test
```

São 82 testes:

| Suíte | Cobre |
|---|---|
| `MaintenancePlanTest` | Cálculo de status, incluindo os limites exatos |
| `*ServiceTest` | Regras de negócio com o repository simulado (Mockito) |
| `SecurityIntegrationTest` | 401, 403 e 2xx com a cadeia de filtros real e as senhas de verdade |
| `ApiFlowIntegrationTest` | Fluxos completos por HTTP até o H2 real, com ADMIN autenticado |
| `SampleDataTest` | Dados de exemplo conferidos contra as regras calculadas |

Para gerar o build do frontend:

```bash
cd frontend
npm run build
```

## Limitações conhecidas

- O banco é em memória, então os dados somem a cada reinício.
- Os usuários são fixos e ficam em memória, com senhas no `application.properties`. Em produção viriam de variáveis de ambiente e de uma tabela.
- O HTTP Basic só é aceitável com HTTPS, que não está configurado aqui.
- A migration V2 roda em qualquer ambiente; veja o item 3 da troca para PostgreSQL.
- O frontend cadastra equipamentos e planos, mas não os edita, desativa nem reativa. Essas operações existem só na API.
- A API não lista equipamentos nem planos desativados, então é preciso saber o id para reativá-los.
- JSON malformado, parâmetro de enum inválido e rota inexistente respondem com o status certo, mas no formato padrão do Spring, não no formato único de erro.
- O `PUT /api/plans/{id}` não valida `lastMaintenanceHours` contra o horímetro atual, ao contrário da criação.
- A data da leitura não é validada: a regra compara só as horas, então é possível lançar uma leitura maior com data antiga.
- Não há controle de concorrência: duas leituras simultâneas podem disputar a atualização do horímetro.
- As listas não têm paginação.
- O frontend não tem testes automatizados no repositório.
- O backend não serve o frontend: em produção, o `dist/` precisaria de um servidor que também repasse `/api` ao backend.
