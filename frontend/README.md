# SAMARCO — Motor de Cálculo Energético (Frontend)

Dashboard web completo para monitoramento e análise energética da SAMARCO, Ano Base 2026.

## Stack

- **React 19** + **TypeScript** + **Vite**
- **Tailwind CSS 4** (dark theme premium SAMARCO)
- **Recharts** para visualizações interativas
- **Apollo Client** para integração GraphQL
- **tRPC** + **Express** (backend BFF)
- **Drizzle ORM** + **MySQL**

## Páginas implementadas

| Rota | Cálculo | Descrição |
|------|---------|-----------|
| `/` | Dashboard | Visão consolidada com 6 KPIs e gráficos |
| `/producao` | 01 | Produção (tms) — 16 áreas × 12 meses |
| `/consumo-especifico` | 02 | Consumo Específico (kWh/tms) + heatmap |
| `/consumo-area` | 03 | Consumo Área (MWh) por estado MG/ES |
| `/geracao-propria` | 04 | Geração Própria — UHE Guilman + PCH Muniz |
| `/encargos` | 05 | Encargos ESS/EER (R$) por estado |
| `/distribuicao-carga` | 13 | Distribuição de Carga (MWh) |
| `/classe-custo` | 14 | Classe de Custo (R$) |
| `/centro-custos` | 15 | Centro de Custos (R$) |
| `/resumo-geral` | 16 | Resumo Geral — consolidação final |
| `/validacao` | — | Painel de Validação Frontend vs API |

## Integração GraphQL

O frontend conecta ao backend Quarkus via Apollo Client. Configure o endpoint:

```env
VITE_GRAPHQL_ENDPOINT=http://localhost:8080/graphql
```

Quando a API estiver indisponível, o dashboard exibe automaticamente os dados de referência (`jsonParaComparacao.json`) com um banner de aviso.

## Desenvolvimento

```bash
pnpm install
pnpm dev
```

## Testes

```bash
pnpm test
```

19 testes unitários cobrindo utilitários de formatação e cálculos energéticos.
