# SAMARCO Motor de Cálculo Energético — TODO

## Fase 1: Setup e Design System
- [x] Instalar dependências: recharts (já disponível), @apollo/client, graphql
- [x] Configurar design system dark theme SAMARCO (CSS variables, Montserrat)
- [x] Configurar Apollo Client para GraphQL
- [x] Criar dados JSON estáticos (mock data do jsonParaComparacao.json)

## Fase 2: Layout Principal
- [x] Header fixo (logo SAMARCO, badge Ano Base 2026, status API)
- [x] Sidebar colapsável com navegação (7 itens + separador)
- [x] Breadcrumb dinâmico
- [x] Layout wrapper com roteamento

## Fase 3: Dashboard Consolidado
- [x] KPI Cards (4 cards: Consumo Total, Geração Própria, Consumo Líquido, Custo Encargos)
- [x] Stacked Area Chart (Consumo vs Geração)
- [x] Donut Chart (Distribuição MG/ES)
- [x] Grouped Bar Chart (Encargos por Estado)
- [x] Stacked Bar Chart (Composição Encargos)
- [x] Tabela Resumo Anual por Mês

## Fase 4: Páginas de Cálculos 01-03
- [x] Página Produção (/producao) — tabela 16 áreas × 12 meses + Bar Chart Top 5
- [x] Página Consumo Específico (/consumo-especifico) — tabela + heatmap
- [x] Página Consumo Área (/consumo-area) — grupos, distribuição MG/ES, gráficos

## Fase 5: Páginas de Cálculos 04-05 e Validação
- [x] Página Geração Própria (/geracao) — calendário, cards usinas, Stacked Area
- [x] Página Encargos ESS/EER (/encargos) — KPIs, tabelas por estado, gráficos
- [x] Painel de Validação (/validacao) — comparação frontend vs API

## Fase 6: Testes e Ajustes
- [x] Testes vitest (19 testes passando)
- [x] Responsividade mobile
- [x] Checkpoint final

## Atualização v2 — Novas Funcionalidades
- [x] Upload logo real SAMARCO e integrar no Header
- [x] Filtros interativos de período (mês e trimestre) no Dashboard
- [x] Atualizar mockData com campos novos do JSON (Cálculos 13-16)
- [x] Atualizar Dashboard com 2 novos KPI cards (Custo Total Geral, Custo Específico)
- [x] Atualizar Sidebar com 4 novos itens de menu (Cálculos 13-16)
- [x] Atualizar cadeia de dependência 01→02→03→04→05→13→14→15→16
- [x] Página /distribuicao-carga — Cálculo 13 (Distribuição de Carga MWh por estado)
- [x] Página /classe-custo — Cálculo 14 (Classe de Custo R$ por classe/estado)
- [x] Página /centro-custos — Cálculo 15 (Centro de Custos R$ por CC)
- [x] Página /resumo-geral — Cálculo 16 (Resumo Geral — página mais importante)
- [x] Atualizar Painel de Validação com novos campos
- [x] Atualizar rotas no App.tsx
- [x] Testes vitest atualizados (19 passando)

## Atualização v3 — Integração GraphQL Real

- [x] Instalar @apollo/client e graphql
- [x] Configurar Apollo Client com endpoint configurável via variável de ambiente
- [x] Criar query GraphQL completa (10 blocos de campos)
- [x] Criar hook useMotorCalculo com loading/error/fallback
- [x] Criar componentes LoadingSkeleton e ErrorBanner reutilizáveis
- [x] Atualizar SamarcoLayout com status real da API (polling 30s)
- [x] Atualizar Dashboard para usar hook real
- [x] Atualizar Producao para usar hook real
- [x] Atualizar ConsumoEspecifico para usar hook real
- [x] Atualizar ConsumoArea para usar hook real
- [x] Atualizar GeracaoPropria para usar hook real
- [x] Atualizar Encargos para usar hook real
- [x] Atualizar DistribuicaoCarga para usar hook real
- [x] Atualizar ClasseCusto para usar hook real
- [x] Atualizar CentroCustos para usar hook real
- [x] Atualizar ResumoGeral para usar hook real
- [x] Atualizar Validacao para comparar frontend vs API real
- [x] Testes vitest (19 passando)
- [x] Checkpoint final v3

## Correção v3.1 — Rules of Hooks

- [x] Corrigir Producao.tsx — hooks após loading return
- [x] Corrigir ConsumoEspecifico.tsx — hooks após loading return
- [x] Corrigir ConsumoArea.tsx — hooks após loading return
- [x] Corrigir GeracaoPropria.tsx — hooks após loading return
- [x] Corrigir Encargos.tsx — hooks após loading return
- [x] Corrigir DistribuicaoCarga.tsx — hooks após loading return
- [x] Corrigir ClasseCusto.tsx — hooks após loading return
- [x] Corrigir CentroCustos.tsx — hooks após loading return
- [x] Corrigir ResumoGeral.tsx — hooks após loading return
