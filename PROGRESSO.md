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
