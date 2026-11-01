# Progresso do projeto: Central de Monitoramento de Horímetro

Atualizado em 31/10/2026. Prazo final: 08/11/2026. Checkpoint: API com os 6 requisitos até 03/11/2026.

## Contexto e forma de trabalho

- Projeto de faculdade, backend em `backend/` (Spring Boot, Java 21, Maven, JPA, H2 em memória, Flyway, Validation).
- Pacote base: `br.com.manutencao.central_manutencao`.
- João está treinando lógica de programação. O modo combinado é **guiar, não entregar pronto**: dar pistas, deixar João escrever o código, depois verificar (ler o arquivo e rodar `./mvnw -q -o compile`) e explicar os erros. Só entregar código pronto quando João pedir.
- Explicações em português, curtas, com exemplos de outro assunto (`User`, `Product`) em vez da resposta direta.
- Commits em português, no formato `feat: ...`.

## O que está pronto (módulo de equipamento)

| Arquivo | O que faz |
|---|---|
| `equipment/Equipment.java` | Entidade. Campos: id, code, name, category, currentHours, active, createdAt. Exclusão lógica pelo `active`. |
| `equipment/EquipmentRepository.java` | `existsByCodeIgnoreCase`, `findByActiveTrue`. |
| `equipment/EquipmentService.java` | `create`, `list` (só ativos), `findById`, `update`, `deactivate` (`setActive(false)`). |
| `equipment/EquipmentRequest.java` | Record com `code`, `name`, `category`. `@NotBlank` em code e name, `@Size` igual ao `length` da entidade (30, 120, 60). |
| `equipment/EquipmentResponse.java` | Record com id, code, name, category, currentHours, active, e `from(Equipment)`. |
| `equipment/EquipmentController.java` | Rotas em `/api/equipments` (veja abaixo). |
| `common/GlobalExceptionHandler.java` | `@RestControllerAdvice` que converte exceções em respostas HTTP. |
| `common/BusinessRuleException.java`, `ResourceNotFoundException.java` | Exceções lançadas pelo service. |

### Rotas

| Ação | Método | Caminho | Resposta |
|---|---|---|---|
| Criar | POST | `/api/equipments` | 201 com `Location` |
| Listar ativos | GET | `/api/equipments` | 200 |
| Buscar um | GET | `/api/equipments/{id}` | 200 |
| Atualizar | PUT | `/api/equipments/{id}` | 200 |
| Desativar | DELETE | `/api/equipments/{id}` | 204 |

### Tratamento de erros

Formato único: `{status, error, messages[], timestamp}`.

- `ResourceNotFoundException` -> 404
- `BusinessRuleException` (código duplicado) -> 409
- `MethodArgumentNotValidException` (`@Valid`) -> 400, uma mensagem por campo

Todos foram testados com `curl` em uma instância temporária.

## Decisões e observações

- O DTO separa o contrato da API do modelo do banco: o cliente não controla id, active nem currentHours.
- `findById` do service **não** filtra por `active`, então um equipamento desativado ainda pode ser buscado por id. Decisão em aberto.
- O `deactivate` não chama `save`, porque o método é `@Transactional` e o JPA grava a mudança sozinho. O `update` chama `save` por padrão do código.
- O VS Code já trocou `java.version` no `pom.xml` para 25 por conta própria. O JDK instalado é o 21, então isso quebra a compilação (`release version 25 not supported`). Se acontecer de novo, voltar para 21.
- `ddl-auto=validate` com H2 em memória: as tabelas vêm das migrations do Flyway. O banco zera a cada reinício.

## O que falta

1. Testes do `EquipmentService` (o plano prevê logo depois da API, num bloco só).
2. Os outros requisitos da API (são 6 no total, só equipamento está pronto).
3. `reactivate(Long id)`, o inverso do `deactivate`, sugerido como exercício e ainda não feito.
4. Decidir se `findById` deve esconder equipamentos desativados.

## Erros que João já treinou (para não repetir a explicação do zero)

- Records: campos ficam só dentro dos parênteses, o corpo `{}` fica vazio ou guarda métodos como `from`.
- Imports de anotações pelo nome completo (`jakarta.validation.constraints.NotBlank`), não pela pasta.
- Getter de `boolean` é `isX()`, e `camelCase` importa (`isActive`, não `IsActive`).
- Passar o valor direto (`false`) em vez de chamar métodos inexistentes.
- Ler a mensagem `cannot find symbol` do compilador.

## Etapas concluídas (resumo para estudar)

### Etapa 1: leituras de horímetro (`reading/`)
- Rotas `POST` e `GET /api/equipments/{equipmentId}/readings`: o equipamento vem da URL, porque a leitura só existe dentro de um equipamento.
- Regra central: a leitura não pode ser menor que o horímetro atual (igual é aceito). Violação -> `BusinessRuleException` (409).
- Ao gravar, o `equipments.current_hours` é atualizado na MESMA transação (`@Transactional`): ou grava os dois ou nenhum. Isso evita leitura salva com equipamento desatualizado.
- Equipamento desativado não aceita leitura (409). Leituras nunca são alteradas nem apagadas, pois são o histórico (a tabela nem tem `active`).
- Testes do service com Mockito (`HourMeterReadingServiceTest`) cobrem: atualiza o horímetro, aceita igual, rejeita menor, rejeita equipamento inativo.

### Etapa 2: planos de manutenção (`plan/`)
- Rotas: `POST/GET /api/equipments/{equipmentId}/plans` (criar e listar ativos) e `GET/PUT/DELETE /api/plans/{id}` (DELETE desativa, 204).
- Vencimento, restante e status ficam em métodos da entidade (`nextDueHours()`, `remainingHours()`, `status()`) e NUNCA no banco. Assim o status acompanha o horímetro sem precisar atualizar nada; a regra fica num só lugar e é testável sem Spring.
- Regra do status: restante <= 0 -> `OVERDUE`; restante <= `warningHours` -> `UPCOMING`; senão `ON_TIME`. Os limites exatos (0 e igual ao aviso) têm teste.
- Plano sem `lastMaintenanceHours` assume o horímetro atual do equipamento. Informar uma última manutenção maior que o horímetro atual é recusado (409), pois não existe manutenção "no futuro" do horímetro.
- No `update`, `lastMaintenanceHours` vazio mantém o valor atual; o equipamento de um plano nunca muda (vem da URL só na criação).

### Etapa 3: manutenções (`maintenance/`)
- Rotas: `POST/GET /api/equipments/{equipmentId}/maintenances` (agendar; listar com `?status=` opcional), `GET /api/maintenances/{id}`, `POST /api/maintenances/{id}/complete` e `POST /api/maintenances/{id}/cancel`.
- É uma máquina de estados simples: nasce `SCHEDULED` e só dela pode ir para `COMPLETED` ou `CANCELED`. Qualquer outra transição é 409. Não há DELETE: o cancelamento é o próprio status, e o histórico fica preservado (exclusão lógica).
- Concluir uma manutenção ligada a um plano zera a contagem do plano (`lastMaintenanceHours` = horas da manutenção) na MESMA transação. Por isso o plano `OVERDUE` volta a `ON_TIME` sem ninguém recalcular nada.
- Valores padrão ao concluir: data = hoje, horas = horímetro atual. Horas maiores que o horímetro atual são recusadas (409). Manutenção sem `planId` é avulsa e não mexe em nenhum plano.
- Ao agendar com plano, o plano precisa ser do mesmo equipamento e estar ativo (409 se não). 14 testes de service cobrem as regras, incluindo "concluir zera o plano".
