# Roteiro do vídeo (5 minutos)

## Antes de gravar

1. Reinicie o backend, para o banco voltar aos dados de exemplo: `cd backend` e `./mvnw spring-boot:run`.
2. Em outro terminal: `cd frontend` e `npm run dev`.
3. Abra <http://localhost:5173> na tela de login, sem usuário preenchido.
4. Deixe um terminal pronto na raiz do projeto, com letra grande, para os comandos curl.
5. Deixe abertos no editor: `MaintenancePlan.java`, `SecurityConfig.java`, `V2__dados_exemplo.sql` e `ApiFlowIntegrationTest.java`.
6. Faça um ensaio inteiro e reinicie o backend de novo: o roteiro altera os dados.

## Roteiro

| Tempo | Mostrar | Falar |
|---|---|---|
| 0:00 a 0:25 | Tela de login | "Este sistema acompanha a manutenção preventiva de equipamentos pelo horímetro. O problema é saber, a partir das horas trabalhadas, o que está vencido e o que vence em breve." |
| 0:25 a 0:55 | Login como `admin` e painel de alertas | "O backend é Spring Boot com Java 21, e o frontend é Vue 3. O painel traz 3 vencidos e 3 próximos, com o mais urgente no topo. O gerador está 350,5 horas atrasado." |
| 0:55 a 1:50 | Equipamentos → `EMP-001` → Leituras. Lançar `100`: aparece o erro. Lançar `1530`: o horímetro muda. | "Primeiro a regra: o horímetro só anda para a frente, então 100 é recusado com 409. Agora uma leitura válida, de 1.530 horas. A leitura e o horímetro do equipamento são gravados na mesma transação." |
| 1:50 a 2:15 | Aba Planos do `EMP-001` e, depois, o Painel | "O plano vencia em 1.520 horas, então passou para Vencido. Ninguém atualizou o plano: o status é calculado a cada consulta, nunca gravado. O alerta apareceu sozinho." |
| 2:15 a 3:00 | No painel, clicar em Agendar no `EMP-001`. Preencher a descrição e agendar. Concluir sem informar horas. Voltar ao Painel. | "Agendo a partir do alerta, com o plano já selecionado. A manutenção nasce agendada e só pode ser concluída ou cancelada. Ao concluir, a contagem do plano recomeça, e o alerta some." |
| 3:00 a 3:40 | Sair, entrar como `viewer` e abrir Manutenções do `GER-001`. Depois, no terminal, os comandos abaixo. | "O VIEWER só consulta: não vê formulários nem botões. Mas esconder o botão não é segurança. A proteção está no backend: sem login é 401, e VIEWER tentando escrever é 403, ambos no mesmo formato JSON da API." |
| 3:40 a 4:30 | Editor: `status()` em `MaintenancePlan.java`, depois as regras em `SecurityConfig.java`, depois o início da `V2__dados_exemplo.sql` | "A regra central fica na entidade e é testável sem banco. A segurança é por papel e método HTTP. O banco evolui só por migrations do Flyway, e o Hibernate apenas valida. Os dados de exemplo não usam ids fixos, porque isso quebraria o próximo cadastro." |
| 4:30 a 4:45 | Terminal com o resultado de `./mvnw test`, já rodado antes | "São 82 testes: regras com Mockito e fluxos completos com MockMvc no banco real." |
| 4:45 a 5:00 | README, seção de limitações | "Os próximos passos seriam PostgreSQL e JWT. Os dois já estão descritos no README, junto com as limitações conhecidas." |

## Comandos do trecho de 3:00

```bash
curl -i http://localhost:8080/api/alerts
curl -i -u viewer:viewer123 -X POST http://localhost:8080/api/equipments \
  -H "Content-Type: application/json" -d '{"code": "X-1", "name": "Teste"}'
```

## Se algo der errado

- **Login não funciona:** confira se o backend está na porta 8080.
- **Os números não batem com o roteiro:** o banco já foi alterado. Reinicie o backend.
- **Passou de 5 minutos:** corte o trecho do editor para só o método `status()`.
