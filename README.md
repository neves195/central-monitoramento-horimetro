# Central de Monitoramento de Manutenção Preventiva por Horímetro

## Decisões de projeto

### `GET /api/equipments/{id}` devolve equipamento desativado

A listagem (`GET /api/equipments`) mostra só os ativos, mas a busca por id não filtra por `active`. A resposta traz o campo `active`, então o cliente sabe a situação do equipamento. Motivos:

- a reativação precisa encontrar o equipamento desativado;
- leituras e manutenções continuam consultáveis depois da desativação, pois são histórico.

As escritas continuam protegidas: equipamento desativado não aceita leitura, plano nem manutenção (409).
