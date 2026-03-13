# Samarco Motor de Cálculo — Spec para Geração de Classes Java

## Objetivo

Gerar classes Java que reproduzam **exatamente** os cálculos que hoje vivem em abas de Excel do planejamento energético Samarco.
Cada aba de cálculo do Excel deve virar uma classe Java independente, recebendo os inputs necessários e retornando os resultados calculados.

**Resultado final esperado:** O motor Java deve reproduzir o resultado consolidado de **~R$ 392M/ano** com custo específico de **~R$ 19,40/tms**, batendo com o Excel.

## Contexto Técnico

- Stack: **Java + Quarkus**
- Arquitetura: use-case based (cada cálculo é um use-case ou service)
- Os dados de INPUT vêm de abas separadas no Excel (ex: `3Planejamento Produção - INPUT`, `5Plan Consumo Especifico - INPUT`, `8Planejamento Geração - INPUT`)
- No Java, esses inputs serão objetos/records recebidos como parâmetro
- As 16 áreas de produção e 12 meses são dimensões recorrentes (área × mês)
- Use `BigDecimal` para valores monetários/financeiros. `double` é aceitável para MWh/tms se a precisão não for crítica.

---

## Cadeia de Dependências (ordem de execução)

```
CAMADA 0 — INDEPENDENTES (sem dependências externas)
  ├── Cálculo 00: Gross-Up Tributário + PMIX (Aba 1 — 121 fórmulas)
  ├── Cálculo 01: Produção (tms)
  └── Cálculo 02: Consumo Específico

CAMADA 0.5 — DEPENDE DO CÁLCULO 00
  └── Cálculo 06: Tabela Tarifas Distribuidoras (consome LIQUIDO SAP da Aba 1)

CAMADA 1 — DEPENDE DA CAMADA 0
  ├── Cálculo 03: Consumo Área (depende de 01 + 02)     ← Aba 7, ~600 fórmulas
  └── Cálculo 04: Geração Própria (depende de Calendário)

CAMADA 2 — DEPENDE DA CAMADA 1
  ├── Cálculo 05: Encargos ESS/EER (depende de 03 + 04)
  └── Cálculo 07: Distribuição de Carga (depende de 03 + 04)

CAMADA 3 — DEPENDE DA CAMADA 2
  └── Cálculo 08: Classe de Custo (depende de 04 + 06 + 07 + 03)

CAMADA 4 — DEPENDE DA CAMADA 3
  └── Cálculo 09: Centro de Custos (depende de 08 + 03)

CAMADA 5 — CONSOLIDAÇÃO FINAL
  └── Cálculo 10: Resumo Geral (depende de 01 + 05 + 09 + inputs manuais)
```

**IMPORTANTE:** Respeitar esta ordem. Cada Calculator recebe como parâmetro o resultado dos Calculators anteriores — nunca acessa diretamente os inputs de outro Calculator.

---

## CÁLCULO 00 — Gross-Up Tributário + PMIX

**Origem Excel:** Aba `1Tarifas Distribuidoras - INPUT` (121 fórmulas)

**Lógica:** A Aba 1 NÃO é input puro. Ela recebe tarifas líquidas brutas e calcula os valores LIQUIDO SAP (descontando ICMS, PIS, COFINS) que alimentam a Aba 2 (Cálculo 06). Também calcula o PMIX (preço médio de energia) por estado.

### Parte 1 — Gross-Up de Tarifas por Distribuidora (Linhas 3-8)

Cada distribuidora tem 2 contratos (vigências diferentes). Para cada contrato, calcula-se:

**Inputs por contrato (colunas D-L):**
- `D` = USO DA REDE PONTA (R$/kW líquido)
- `E` = USO DA REDE FORA PONTA (R$/kW líquido)
- `F` = ENCARGO (R$/MWh líquido)
- `G` = Encargo Auto Produtor (R$/MWh)
- `I` = ICMS (%)
- `J` = PIS (%)
- `K` = COFINS (%)
- `L` = PIS/COFINS Faturamento Distribuidora (%)

**Fórmulas de gross-up (padrão para PONTA, FORA PONTA e ENCARGO):**
```
Desconto_AutoProducao (H) = Encargo - Encargo_AutoProdutor     // H = F - G
Valor_Bruto (M/R/W)       = (Tarifa_Liquida / (1 - ICMS)) / (1 - PIS_COFINS_Fat)
Valor_ICMS (N/S/X)        = Bruto × ICMS
Valor_PIS (O/T/Y)         = Bruto × PIS
Valor_COFINS (P/U/Z)      = Bruto × COFINS
LIQUIDO_SAP (Q/V/AA)      = Bruto - ICMS - PIS - COFINS
```

**Distribuidoras e contratos:**

| Linha | Distribuidora | Contrato | ICMS | Vigência |
|-------|--------------|----------|------|----------|
| 3 | ONS | Contrato 1 | 18% | Jan-Jun |
| 4 | ONS | Contrato 2 | 18% | Jul-Dez |
| 5 | EDP | Contrato 1 | 17% | Jan-Jul |
| 6 | EDP | Contrato 2 | 17% | Ago-Dez |
| 7 | ENERGISA | Contrato 1 | 0% | Jan-Jun |
| 8 | ENERGISA | Contrato 2 | 0% | Jul-Dez |

**⚠️ ATENÇÃO:** ENERGISA tem ICMS = 0%. Isso muda significativamente o gross-up.

### Parte 2 — Cálculo PMIX (Linhas 12-16)

PMIX = Preço Médio de energia comprada no mercado livre, calculado como média ponderada de 4 contratos de fornecimento:

```
Para cada fornecedor (linhas 12-15):
  IPCA_Total (F)   = IPCA_Realizado + IPCA_Projetado
  Preco_Ajustado (G) = Preco_Base × (1 + IPCA_Total)
  Valor_Montante (I) = Preco_Ajustado × Montante_MWmed

PMIX_Base (I16) = SUM(Valor_Montante) / SUM(Montante)
               = Média ponderada pelo montante
```

**Fornecedores (valores de referência):**

| Fornecedor | Base | Preço Base | IPCA Total | Preço Ajustado | Montante (MWmed) |
|-----------|------|-----------|------------|----------------|-----------------|
| Cemig Convencional | Mai/2021 | 142,59 | 29,95% | 185,30 | 80 |
| Cemig Incentivada | Nov/2023 | 152,88 | 10,73% | 169,28 | 10 |
| Elera | Fev/2024 | 159,20 | 8,75% | 173,13 | 20 |
| Matrix | Abr/2024 | 143,40 | 8,17% | 155,12 | 20 |

**PMIX Base resultante:** R$ 177,55/MWh (média ponderada)

### Parte 3 — PMIX por Estado (Linhas 22-23)

Aplica gross-up tributário ao PMIX base, diferenciado por estado:

```
Para cada estado (MG linha 22, ES linha 23):
  Valor_Bruto   = (PMIX_Base / (1 - PIS_COFINS_Fat)) / (1 - ICMS_Projetado)
  LIQUIDO_SAP   = Bruto - ICMS_val - PIS_val - COFINS_val
```

| Estado | ICMS | PIS | COFINS | PMIX LIQUIDO SAP |
|--------|------|-----|--------|-----------------|
| MG | 18% | 1,65% | 7,6% | **R$ 161,00/MWh** |
| ES | 17% | 1,65% | 7,6% | **R$ 161,25/MWh** |

**Saídas consumidas pelo Cálculo 06 (Aba 2):**
- `LIQUIDO_SAP_Ponta[distribuidora][contrato]` → colunas Q3:Q8
- `LIQUIDO_SAP_ForaPonta[distribuidora][contrato]` → colunas V3:V8
- `LIQUIDO_SAP_Encargo[distribuidora][contrato]` → colunas AA3:AA8
- `Desconto_AutoProducao[distribuidora][contrato]` → coluna H3:H8
- `PMIX_MG` → K22
- `PMIX_ES` → K23

**Nota para o sistema futuro:** A Aba 1 linha 18 tem um comentário: *"Considerar que haverá um campo para cadastrar o valor do IPCA do mês de faturamento até o final do ano corrente, para projetar o custo do ano seguinte"* — ou seja, o IPCA Projetado deve ser editável pelo usuário.

---

## CÁLCULO 01 — Produção (tms)

**Origem Excel:** Aba `4Produção - OCULTA` (~192 fórmulas)

**Lógica:** Converte produção de ktms → tms multiplicando por 1.000

**Regra geral:**
```
Producao_tms[área][mês] = PlanejamentoProducaoInput[área][mês] × Multiplicador
```

**Parâmetros:**
- `Multiplicador` = 1000 (célula fixa B20 no Excel)
- 16 áreas (linhas): Filtragem, Espessamento, Homogeneização, Moagem, Deslamagem, Flotação, Benef.1, Benef.2, Benef.3, Remoagem, Pelot.1, Pelot.2, Pelot.3, Usina Itabiritos, Vendas, Porto, Estocagem, Pellet Feed
- 12 meses (colunas): Jan–Dez

**Fórmula Excel exemplo:**
```
B3 (Filtragem Jan)    = '3Planejamento Produção - INPUT'!B4 * $B$20
B16 (Vendas Jan)      = '3Planejamento Produção - INPUT'!B17 * $B$20
B18 (Pellet Feed Jan) = '3Planejamento Produção - INPUT'!B19 * $B$20
```

**Classe Java esperada:**
- Input: `Map<String, Map<String, Double>>` ou record com produção em ktms por área e mês
- Output: mesma estrutura com valores em tms (× 1000)
- O multiplicador deve ser configurável (não hardcoded como magic number)

---

## CÁLCULO 02 — Consumo Específico

**Origem Excel:** Aba `6Consumo Específico - OCULTA` (~192 fórmulas)

**Lógica:** Espelha diretamente os valores do INPUT (cópia 1:1)

**Regra geral:**
```
ConsumoEspecifico[área][mês] = PlanConsumoEspecificoInput[área][mês]
```

**Fórmula Excel exemplo:**
```
C3  (Filtragem Jan) = '5Plan Consumo Especifico -INPUT'!B3
C6  (Benef.2 Jan)   = '5Plan Consumo Especifico -INPUT'!B6
C17 (Estocagem Jan)  = '5Plan Consumo Especifico -INPUT'!B18
```

**Nota:** Mesmo sendo cópia direta, manter como classe/método separado para:
1. Desacoplar a fonte de dados da lógica de cálculo
2. Permitir transformações futuras (ex: ajuste sazonal)
3. Validação dos inputs (nulls, negativos)

---

## CÁLCULO 03 — Consumo Área (CALC) ⚠️ ABA CENTRAL

**Origem Excel:** Aba `7Consumo Área - CALC` (~600+ fórmulas)

**Lógica:** Calcula consumo de energia elétrica (kWh) por centro de custo, agrupa em macro-áreas (MWh), divide por estado, e gera os percentuais de rateio que alimentam toda a cadeia posterior (Abas 12, 13, 14, 15, 16).

**Dependências:**
- Cálculo 01 (Produção tms)
- Cálculo 02 (Consumo Específico)

**NOTA CRÍTICA:** Esta é a aba mais complexa e central. Quase todos os cálculos posteriores dependem dela. Errar aqui propaga erro pra tudo.

### Seção 1 — Consumo Planejado por Centro de Custo (kWh/mês) — Linhas 2-17

Regra geral: `ConsumoKWh[CC][mês] = ConsumoEspecifico[área] × Producao_tms[área]`

**Mas há exceções importantes:**

| Linha | CC SAP | Área | Fórmula | Observação |
|-------|--------|------|---------|------------|
| 3 | 11140030 | Filtragem Germano | `ConsEsp.C3 × Producao.B3` | Padrão |
| 4 | 11110060 | Mineração | `(Producao.B4 + Producao.B5 + Producao.B6) × ConsEsp.C4` | ⚠️ Usa soma Benef1+2+3 como volume, NÃO produção própria |
| 5 | 11130180 | Beneficiamento 1 | **VALOR FIXO MANUAL = 1.040.115,68 kWh/mês** | ⚠️ Sem fórmula — input manual cadastrado por ano |
| 6 | 11131180 | Beneficiamento 2 | `ConsEsp.C6 × Producao.B5` | Padrão |
| 7 | 11132180 | Beneficiamento 3 | `ConsEsp.C7 × Producao.B6` | Padrão |
| 8 | 11162040 | Mineroduto 1 | **VALOR FIXO MANUAL** (varia mês a mês: 230k, 210k, 230k, 225k...) | ⚠️ Sem fórmula — input manual |
| 9 | 11162090 | Mineroduto 2 | `ConsEsp.C9 × Producao.B8` | = 0 (produção zero) |
| 10 | 11162120 | Mineroduto 3 | `ConsEsp.C10 × Producao.B9` | Padrão |
| 11 | 12110080 | Preparação 1 | `ConsEsp.C11 × Producao.B10` | = 0 (produção zero) |
| 12 | 12111080 | Preparação 2 | `ConsEsp.C12 × Producao.B11` | Padrão |
| 13 | 12133040 | Usina 1 | `ConsEsp.C13 × Producao.B12` | = 0 (produção zero) |
| 14 | 12135040 | Usina 2 | `ConsEsp.C14 × Producao.B13` | = 0 (produção zero) |
| 15 | 12136040 | Usina 3 | `ConsEsp.C15 × Producao.B14` | Padrão |
| 16 | 12138040 | Usina 4 | `ConsEsp.C16 × Producao.B15` | Padrão |
| 17 | 12420080 | Estocagem | `ConsEsp.C17 × Producao.Vendas` | Padrão |

**Coluna O (Planejado Anual):** `=SUM(Jan:Dez)` para cada linha.

**Decisão de design para inputs manuais:**
- Beneficiamento 1 (CC 11130180) e Mineroduto 1 (CC 11162040) precisam de um mecanismo de "override manual" — o sistema deve permitir cadastrar valores fixos para áreas que não possuem fórmula calculada.

### Seção 2 — Consumo por Macro-Área (MWh/mês) — Linhas 19-28

Converte kWh → MWh (÷ 1000) e agrupa centros de custo em 7 macro-áreas:

```
Linha 21: Filtragem Germano (MWh) = CC_11140030_kWh / 1000
Linha 22: Mineração (MWh)         = CC_11110060_kWh / 1000
Linha 23: Concentração (MWh)      = SUM(CC_Benef1 + CC_Benef2 + CC_Benef3) / 1000
Linha 24: Mineroduto (MWh)        = SUM(CC_Min1 + CC_Min2 + CC_Min3) / 1000
Linha 25: Preparação (MWh)        = SUM(CC_Prep1 + CC_Prep2) / 1000
Linha 26: Pelotização (MWh)       = SUM(CC_Usina1 + CC_Usina2 + CC_Usina3 + CC_Usina4) / 1000
Linha 27: Estocagem (MWh)         = CC_12420080_kWh / 1000
Linha 28: TOTAL SAMARCO (MWh)     = SUM(linhas 21:27)
```

### Seção 3 — Totais por Estado (MWh e %) — Linhas 30-34

**Regra de atribuição de estado:**
- **Minas Gerais** = Filtragem + Mineração + Concentração + Mineroduto (áreas de Germano)
- **Espírito Santo** = Preparação + Pelotização + Estocagem (áreas de Ubu)

```
Linha 31: MG (MWh)  = SUM(Filtragem + Mineração + Concentração + Mineroduto)  // SUM(C21:C24)
Linha 32: ES (MWh)  = SUM(Preparação + Pelotização + Estocagem)               // SUM(C25:C27)
Linha 33: MG (%)    = MG_MWh / Total_MWh                                       // C31/C28
Linha 34: ES (%)    = ES_MWh / Total_MWh                                       // C32/C28
```

**Valores de referência Jan:** MG = 68.894 MWh (59,0%) / ES = 47.783 MWh (41,0%)

### Seção 4 — % de Rateio por Centro de Custo — Linhas 36-52

**⚠️ SEÇÃO MAIS CRÍTICA — leia com atenção**

Gera os percentuais usados pela Aba 15 para ratear custos por centro de custo.

**Regra:** Cada CC recebe um % proporcional ao seu consumo kWh sobre o total do respectivo estado.

#### MG — Centros de Custo (linhas 38-45):

```
Linha 38: 11140030 Filtragem  = CC_kWh / (MG_MWh_DO_MES × 1000)    ⚠️ DENOMINADOR VARIÁVEL!
Linha 39: 11110060 Mineração  = CC_kWh / ($C$31 × 1000)             ← DENOMINADOR FIXO (JAN)
Linha 40: 11130180 Benef.1    = CC_kWh / ($C$31 × 1000)             ← FIXO (JAN)
Linha 41: 11131180 Benef.2    = CC_kWh / ($C$31 × 1000)             ← FIXO (JAN)
Linha 42: 11132180 Benef.3    = CC_kWh / ($C$31 × 1000)             ← FIXO (JAN)
Linha 43: 11162040 Minerod.1  = CC_kWh / ($C$31 × 1000)             ← FIXO (JAN)
Linha 44: 11162090 Minerod.2  = CC_kWh / ($C$31 × 1000)             ← FIXO (JAN)
Linha 45: 11162120 Minerod.3  = CC_kWh / ($C$31 × 1000)             ← FIXO (JAN)
```

#### ES — Centros de Custo (linhas 46-52):

```
Linha 46: 12110080 Prep.1     = CC_kWh / ($C$32 × 1000)             ← FIXO (JAN)
Linha 47: 12111080 Prep.2     = CC_kWh / ($C$32 × 1000)             ← FIXO (JAN)
Linha 48: 12133040 Usina 1    = CC_kWh / ($C$32 × 1000)             ← FIXO (JAN)
Linha 49: 12135040 Usina 2    = CC_kWh / ($C$32 × 1000)             ← FIXO (JAN)
Linha 50: 12136040 Usina 3    = CC_kWh / ($C$32 × 1000)             ← FIXO (JAN)
Linha 51: 12138040 Usina 4    = CC_kWh / ($C$32 × 1000)             ← FIXO (JAN)
Linha 52: 12420080 Estocagem  = CC_kWh / ($C$32 × 1000)             ← FIXO (JAN)
```

**⚠️ ARMADILHA CRÍTICA no rateio:**
1. **Filtragem Germano (linha 38)** usa denominador **variável por mês** (sem `$`) — referência relativa
2. **Todos os demais CCs (linhas 39-52)** usam denominador **fixo de Janeiro** (com `$C$`) — referência absoluta
3. **Consequência:** Os percentuais de rateio NÃO somam 100% em cada mês (só somam em Janeiro). Nos outros meses o numerador varia mas o denominador fica fixo no valor de Janeiro.
4. **Para o Java:** Reproduzir exatamente esse comportamento (denominador fixo Jan), mesmo que pareça um bug. Adicionar um comentário explicando a inconsistência e um TODO para validar com o time se é intencional.

**Coluna O (Planejado):** `=AVERAGE(Jan:Dez)` — média dos 12 meses (não soma!)

### Valores de Referência para Testes — % Rateio Jan

```
CC          | Área               | Estado | Jan_%
11140030    | Filtragem Germano  | MG     | 5.039%
11110060    | Mineração          | MG     | 2.716%
11130180    | Beneficiamento 1   | MG     | 1.510%
11131180    | Beneficiamento 2   | MG     | 36.909%
11132180    | Beneficiamento 3   | MG     | 40.492%
11162040    | Mineroduto 1       | MG     | 0.334%
11162090    | Mineroduto 2       | MG     | 0.000%
11162120    | Mineroduto 3       | MG     | 13.001%
12110080    | Preparação 1       | ES     | 0.000%
12111080    | Preparação 2       | ES     | 28.413%
12133040    | Usina 1            | ES     | 0.000%
12135040    | Usina 2            | ES     | 0.000%
12136040    | Usina 3            | ES     | 29.734%
12138040    | Usina 4            | ES     | 39.913%
12420080    | Estocagem          | ES     | 1.940%
```

### Quem Consome a Aba 7?

```
Aba 7 (Consumo Área - CALC) é consumida por:
├── Linha 28 (Total MWh)          → Aba 12 (Cálculo 05 — Encargos ESS/EER) → consumo líquido
├── Linhas 31-32 (MWh por estado) → Aba 13 (Cálculo 07 — Distrib Carga) → necessidade de compra
├── Linhas 33-34 (% por estado)   → Aba 12 (Cálculo 05 — Encargos) → rateio ESS/EER por estado
├── Linhas 3-17 (kWh por CC)      → Aba 14 (Cálculo 08 — Classe Custo) → cálculo TUSD Encargo
└── Linhas 38-52 (% rateio por CC)→ Aba 15 (Cálculo 09 — Centro Custos) → distribuição final R$
```

---

## CÁLCULO 04 — Geração Própria

**Origem Excel:** Aba `9Geração Própria - CALC` (~84 fórmulas)

### Seção 1 — Calendário (Linhas 5-11)

| Métrica | Fórmula | Descrição |
|---------|---------|-----------|
| Dias úteis | `dias_no_mês - feriados` | Dias úteis do mês |
| Horas Ponta | `dias_úteis × HORAS_PONTA_POR_DIA` | HORAS_PONTA_POR_DIA = 3 (célula $B$2) |
| Horas Fora Ponta | `total_horas - horas_ponta` | Complemento |
| Total Horas | `HORAS_POR_DIA × dias_no_mês` | HORAS_POR_DIA = 24 (célula $B$3) |

**Inputs necessários:**
- Dias no mês (ou calcular a partir do mês/ano)
- Feriados por mês
- Constantes: `HORAS_PONTA_POR_DIA = 3`, `HORAS_POR_DIA = 24`

### Seção 2 — Geração (Linhas 15-20)

| Métrica | Fórmula | Descrição |
|---------|---------|-----------|
| Gullman MWh | Input direto da Aba 8 | `'8Planejamento Geração - INPUT'!B2` |
| Gullman MWmédios | `Gullman_MWh / total_horas_mês` | MWh ÷ horas |
| Muniz Freire MWh | Input direto da Aba 8 | `'8Planejamento Geração - INPUT'!B3` |
| Muniz Freire MWmédios | `MunizFreire_MWh / total_horas_mês` | MWh ÷ horas |
| Total MWh | `Gullman_MWh + MunizFreire_MWh` | Soma das usinas |
| Total MWmédios | `Total_MWh / total_horas_mês` | Total ÷ horas |
| Acumulado MWh | `SUM(Total_MWh[Jan:mês_atual])` | Soma acumulada |
| Acumulado MWmédios | `AVERAGE(Total_MWmédios[Jan:mês_atual])` | Média acumulada |

**Saídas importantes:**
- `GullmanMWh[mês]` (linha 15) — usado no Cálculo 07 e 08
- `MunizFreireMWh[mês]` (linha 17) — usado no Cálculo 07 e 08
- `TotalMWh[mês]` (linha 19) — usado no Cálculo 05

---

## CÁLCULO 05 — Encargos ESS / EER

**Origem Excel:** Aba `12Encargos ESS EER - CALC` (~120 fórmulas)

### Seção 1 — Cálculo Nacional (Linhas 2-8)

| Métrica | Fórmula | Descrição |
|---------|---------|-----------|
| ERR (R$/MWh) | INPUT: `11.33` | Tarifa ERR |
| ERCAP (R$/MWh) | INPUT: `3.31` | Tarifa ERCAP |
| Total ERR/ERCAP | `ERR + ERCAP` | = 14.64 |
| Consumo Samarco (MWh) | `ConsumoArea_Total - GeracaoPropria_Total` | Aba7.C28 - Aba9.C19 |
| Valor Total EER (R$) | `Consumo_Samarco × Total_ERR_ERCAP` | Consumo × tarifa |
| ESS (R$/MWh) | INPUT: `3.04` | Tarifa ESS |
| Valor Total ESS (R$) | `ESS × Consumo_Samarco` | ESS × consumo |

**Inputs configuráveis:** `ERR_TARIFA`, `ERCAP_TARIFA`, `ESS_TARIFA`

### Seção 2 — Distribuição por Estado (Linhas 11-16)

```
MG ERR/ERCAP (50610007) = Valor_Total_EER × PercentualMG    // Aba7.C33
MG ESS (50610008)       = Valor_Total_ESS × PercentualMG
MG TOTAL                = MG_ERR + MG_ESS
ES ERR/ERCAP (50610007) = Valor_Total_EER × PercentualES    // Aba7.C34
ES ESS (50610008)       = Valor_Total_ESS × PercentualES
ES TOTAL                = ES_ERR + ES_ESS
```

**Dependências:** Cálculo 03 (Consumo Área — totais e percentuais), Cálculo 04 (Geração Própria — total MWh)

---

## CÁLCULO 06 — Tabela de Tarifas Distribuidoras

**Origem Excel:** Aba `2Tabela Tarifas Distribuidoras` (~120 fórmulas)

**Lógica:** Monta tabela mensal de tarifas por unidade consumidora, com **troca de contrato por vigência** (mês de corte muda conforme distribuidora).

### Minas Gerais

**Germano (ONS):**
```
Ponta (Jan-Jun):       = TarifasInput.ONS_contrato1.ponta         // $Q$3
Ponta (Jul-Dez):       = TarifasInput.ONS_contrato2.ponta         // $Q$4
Fora Ponta (Jan-Jun):  = TarifasInput.ONS_contrato1.foraPonta     // $V$3
Fora Ponta (Jul-Dez):  = TarifasInput.ONS_contrato2.foraPonta     // $V$4
Encargo (Jan-Mai):     = TarifasInput.ONS_contrato1.encargo       // $AA$3
Encargo (Jun-Dez):     = TarifasInput.ONS_contrato2.encargo       // $AA$4
Desconto GA:           = TarifasInput.ONS.descontoGA              // $H$3 / $H$4
```

**Matipó (ENERGISA):**
```
Ponta (Jan-Jun):       = TarifasInput.ENERGISA_contrato1.ponta    // $Q$7
Ponta (Jul-Dez):       = TarifasInput.ENERGISA_contrato2.ponta    // $D$8
Encargo Distrib:       = TarifasInput.ENERGISA.encargo            // $AA$7 / $AA$8
```

### Espírito Santo

**UBU (EDP):**
```
Ponta (Jan-Jul):       = TarifasInput.EDP_contrato1.ponta         // $Q$5
Ponta (Ago-Dez):       = TarifasInput.EDP_contrato2.ponta         // $Q$6
Fora Ponta (Jan-Jun):  = TarifasInput.ENERGISA_contrato1.foraPonta // $V$7 ⚠️ USA ENERGISA!
Fora Ponta (Ago-Dez):  = TarifasInput.EDP_contrato2.foraPonta     // $V$6
Desconto GA:           = TarifasInput.EDP.descontoGA               // $H$5 / $H$6
Encargo Distrib:       = TarifasInput.EDP.encargo                  // $AA$5 / $AA$6
```

**PMIX (fixo o ano todo):**
```
PMIX_MG = TarifasInput.PMIX_MG    // $K$22
PMIX_ES = TarifasInput.PMIX_ES    // $K$23
```

**⚠️ ATENÇÃO — Armadilhas no Excel:**
1. UBU Fora Ponta Jan-Jun referencia ENERGISA ($V$7), não EDP — confirmar se é intencional
2. Cada distribuidora tem mês de corte diferente para troca de contrato (Jun/Jul vs Jul/Ago)
3. Desconto GA vem de coluna $H$ (diferente das demais tarifas)

**Design sugerido:** Criar um `TarifaResolver` que recebe (unidade, tipo_tarifa, mês) e retorna a tarifa correta, encapsulando toda a lógica de vigência.

---

## CÁLCULO 07 — Distribuição de Carga

**Origem Excel:** Aba `13Distribuição de Carga` (~120 fórmulas)

**Lógica:** Calcula a necessidade de compra de energia por estado.

### Minas Gerais (Linhas 3-11)

```
Perda_Percentual       = 0.03                                    // INPUT MANUAL (3%)
Consumo_MG_MWh         = ConsumoArea.consumoMG[mês]              // Aba7.C31
Geracao_Guilman_MWh    = GeracaoPropria.gullmanMWh[mês]          // Aba9.C15
Perdas_MWh             = Consumo_MG × Perda_Percentual
Proinfa_MWh            = 0                                        // INPUT MANUAL
Necessidade_Total      = Consumo_MG + Perdas - Proinfa
Necessidade_Compra     = MAX(0, Necessidade_Total - Geracao_Guilman)
Qtde_Comprada          = INPUT (pode ser ajustado manualmente)
```

### Espírito Santo (Linhas 14-22)

```
Consumo_ES_MWh         = ConsumoArea.consumoES[mês]              // Aba7.C32
Geracao_MunizFreire    = GeracaoPropria.munizFreireMWh[mês]      // Aba9.C17
(demais fórmulas idênticas ao MG)
```

### Samarco Total (Linhas 25-32)

```
Consumo_Total     = Consumo_MG + Consumo_ES
Geracao_Total     = Guilman + MunizFreire
Perdas_Total      = Perdas_MG + Perdas_ES
Necessidade_Total = Nec_MG + Nec_ES
Necessidade_Compra = Compra_MG + Compra_ES
```

**Inputs configuráveis:** `PERCENTUAL_PERDA_MG`, `PERCENTUAL_PERDA_ES`, `PROINFA_MG`, `PROINFA_ES`

**Dependências:** Cálculo 03 (Consumo Área — MG/ES), Cálculo 04 (Geração Própria — por usina)

---

## CÁLCULO 08 — Distribuição por Classe de Custo

**Origem Excel:** Aba `14Distrib de Classe de Custo` (~200+ fórmulas)

**Lógica:** Calcula custo em R$ por classe SAP × estado × mês.

### Minas Gerais

**PROINFA — Devolução:**
```
Proinfa_MG = Geracao_Guilman × Desconto_GA_Germano
           = GeracaoPropria.gullmanMWh[mês] × TabelaTarifas.germano.descontoGA[mês]
           // Ref: Aba9.C15 * Aba2.E6
```

**TUSD FIO/DEMANDA (classe 50610003):**
```
Germano = (Demanda_Ponta × Tarifa_Ponta) + (Demanda_FP × Tarifa_FP)
        = (DemandaInput.germano.ponta[mês] × TabelaTarifas.germano.ponta[mês])
        + (DemandaInput.germano.foraPonta[mês] × TabelaTarifas.germano.foraPonta[mês])
        // Ref: ('10Demanda-INPUT'!C2 × Aba2.E3) + ('10Demanda-INPUT'!C3 × Aba2.E4)

Matipó  = mesma lógica com tarifas ENERGISA
Total_MG_TUSD = Germano + Matipó
```

**TUSD ENCARGO (classe 50610006):**
```
Germano = MAX(0, (SUM(ConsumoArea[Filtragem..Flotação]) / 1000 × Tarifa_Encargo_Germano) - Proinfa_MG)
        // Ref: IF(((SUM(Aba7.C3:C7)/1000) × Aba2.E5) - PROINFA < 0, 0, cálculo)
        // ⚠️ Consumo em kWh dividido por 1000 para converter a MWh

Matipó  = SUM(ConsumoArea[Mineroduto1..3]) / 1000 × Tarifa_Encargo_Matipó
        // Ref: (SUM(Aba7.C8:C10)/1000) × Aba2.E12

Total_MG_Encargo = Germano + Matipó
```

**CONSUMO ENERGIA (classe 50610002):**
```
Consumo_MG_R$ = DistribCarga.qtdeComprada_MG[mês] × TabelaTarifas.PMIX_MG
              // Ref: Aba13.C11 × Aba2.E13
```

### Espírito Santo

```
TUSD FIO/DEMANDA:  (Demanda_UBU_Ponta × Tarifa) + (Demanda_UBU_FP × Tarifa)
TUSD ENCARGO:      MAX(0, ConsumoES_kWh/1000 × Tarifa_Encargo_ES - Desconto_MunizFreire)
CONSUMO ENERGIA:   DistribCarga.qtdeComprada_ES[mês] × TabelaTarifas.PMIX_ES
```

**Saídas importantes (referenciadas pelo Cálculo 09):**
- `Total_MG_Consumo[mês]` (linha 12)
- `Total_MG_TUSD[mês]` (linha 7)
- `Total_MG_Encargo[mês]` (linha 10)
- `Total_ES_Consumo[mês]` (linha 21)
- `Total_ES_TUSD[mês]` (linha 17)
- `Total_ES_Encargo[mês]` (linha 19)

**Dependências:** Cálculo 04, Cálculo 06, Cálculo 07, Cálculo 03 + DemandaInput (Aba 10)

---

## CÁLCULO 09 — Distribuição por Centro de Custos

**Origem Excel:** Aba `15Distrib Centro de Custos` (~500+ fórmulas)

**Lógica:** Rateia cada classe de custo para cada centro de custo usando o percentual da Aba 7.

**Regra geral:**
```
Custo_CC[CC][classe][mês] = Total_ClasseCusto[estado][mês] × Percentual_Rateio[CC][mês]
```

### Minas Gerais (CCs: 11140030 a 11162120)

```
Consumo_CC (50610002) = ClasseCusto.totalMG_Consumo[mês] × ConsumoArea.percentualCC_MG[CC][mês]
                      // Ref: Aba14.{mês}12 × Aba7.{mês}{%CC_MG}

UsoRede_CC (50610003) = ClasseCusto.totalMG_TUSD[mês] × ConsumoArea.percentualCC_MG[CC][mês]
                      // Ref: Aba14.{mês}7 × Aba7.{mês}{%CC_MG}

Encargo_CC (50610006) = ClasseCusto.totalMG_Encargo[mês] × ConsumoArea.percentualCC_MG[CC][mês]
                      // Ref: Aba14.{mês}10 × Aba7.{mês}{%CC_MG}
```

### Espírito Santo (CCs: 12110080 a 12420080)

```
Consumo_CC (50610002) = ClasseCusto.totalES_Consumo[mês] × ConsumoArea.percentualCC_ES[CC][mês]
UsoRede_CC (50610003) = ClasseCusto.totalES_TUSD[mês] × ConsumoArea.percentualCC_ES[CC][mês]
Encargo_CC (50610006) = ClasseCusto.totalES_Encargo[mês] × ConsumoArea.percentualCC_ES[CC][mês]
```

### Totais (linhas 64-67)

```
Total_Samarco_Consumo (50610002)  = SUM(Consumo_MG_todos_CCs) + SUM(Consumo_ES_todos_CCs)
Total_Samarco_UsoRede (50610003)  = SUM(UsoRede_MG) + SUM(UsoRede_ES)
Total_Samarco_Encargo (50610006)  = SUM(Encargo_MG) + SUM(Encargo_ES)
TOTAL_GERAL                        = 50610002 + 50610003 + 50610006
```

**Dependências:** Cálculo 08 (Classe de Custo), Cálculo 03 (percentuais de rateio por CC)

---

## CÁLCULO 10 — Resumo Geral (Consolidação Final)

**Origem Excel:** Aba `16Resumo GERAL` (~100 fórmulas)

### Seção 1 — Custos por Classe (Linhas 3-16)

**Classes calculadas (vêm dos Calculators anteriores):**

| Classe SAP | Descrição | Fonte |
|-----------|-----------|-------|
| 50610002 | Consumo Energia | Cálculo 09, linha 64 |
| 50610003 | Uso Rede | Cálculo 09, linha 65 |
| 50610006 | Encargo | Cálculo 09, linha 66 |
| 50610007 | EER | Cálculo 05, linha 6 |
| 50610008 | ESS | Cálculo 05, linha 8 |

**Classes com input manual (parâmetros configuráveis):**

| Classe SAP | Descrição | Valor Exemplo |
|-----------|-----------|---------------|
| 50620002 | Royalties Hidrelétrica | Varia mês a mês |
| 50620003 | Transmissão EE | 665.774 (Jan-Jun) / 719.482 (Jul-Dez) |
| 50620004 | Operação Hidrelétrica | Varia mês a mês |
| 50620099 | Outras Despesas Hidro | Fixo 203.645/mês |
| 50630001 | O&M Rede Básica | Varia |
| 50630099 | Outras Rede Básica | Fixo 25.000/mês |
| 50610005 | Energia Administrativa | Fixo 108.333/mês |
| 50610099 | Outros Energia Elétrica | Varia |

```
TOTAL_GERAL_R$ = SUM(todas_as_classes)
```

### Seção 2 — Produção (Linhas 20-24)

```
Pelotas_tms    = SUM(Producao[Pelot1, Pelot2, Pelot3, UsinaItabiritos])
                // Ref: SUM(Aba4.{mês}12:{mês}15)
PelletFeed_tms = Producao.pelletFeed[mês]          // Ref: Aba4.{mês}18
PSC_PSM_tms    = Producao.porto[mês]               // Ref: Aba4.{mês}17
TOTAL_PRODUCAO = Pelotas + PelletFeed + PSC_PSM
```

### Seção 3 — Custo Específico

```
CUSTO_ESPECIFICO_EE = TOTAL_GERAL_R$ / TOTAL_PRODUCAO_tms
  Exemplo Jan: 28.665.952 / 1.597.010 = R$ 17,95/tms
  Exemplo Anual: 392.035.533 / 20.208.464 = R$ 19,40/tms
```

### Seção 4 — Custo Fixo vs Variável (Linhas 27-30)

```
CUSTO_FIXO = UsoRede + Royalties + Transmissão + OperaçãoHidro + OutrasHidro
           + O&M_RB + OutrasRB + EnergAdmin + OutrosEE
         // = 50610003 + 50620002 + 50620003 + 50620004 + 50620099
         //   + 50630001 + 50630099 + 50610005 + 50610099
         // Anual: ~R$ 100M (R$ 4,95/tms)

CUSTO_VARIAVEL = Consumo + Encargo + EER + ESS
               // = 50610002 + 50610006 + 50610007 + 50610008
               // Anual: ~R$ 292M (R$ 14,45/tms)
```

**Dependências:** Cálculo 01, Cálculo 05, Cálculo 09 + inputs manuais

---

## Estrutura de Projeto Sugerida

```
src/main/java/com/samarco/calc/
├── model/
│   ├── Mes.java                         // enum Jan-Dez
│   ├── Area.java                        // enum das 16 áreas de produção
│   ├── Estado.java                      // enum MG, ES
│   ├── CentroCusto.java                 // enum dos CCs SAP
│   ├── ClasseCusto.java                 // enum 50610002, 50610003, etc.
│   ├── UnidadeConsumidora.java          // enum GERMANO, MATIPO, UBU
│   ├── Calendario.java                  // record: dias, horasPonta, horasForaPonta, totalHoras
│   └── ResultadoMensal.java             // record genérico para Map<Mes, BigDecimal>
├── input/
│   ├── TarifasBrutasInput.java          // Aba 1 — tarifas líquidas brutas + alíquotas
│   ├── FornecedoresEnergiaInput.java    // Aba 1 — contratos PMIX (Cemig, Elera, Matrix)
│   ├── PlanejamentoProducaoInput.java
│   ├── ConsumoEspecificoInput.java
│   ├── PlanejamentoGeracaoInput.java
│   ├── DemandaInput.java                // Aba 10 — demanda contratada por unidade
│   ├── EncargosInput.java               // ERR, ERCAP, ESS tarifas
│   ├── CargaInput.java                  // % perda, Proinfa, ajuste manual qtde comprada
│   └── ResumoInputsManuais.java         // Royalties, Transmissão, O&M, etc.
├── service/
│   ├── GrossUpTributarioCalculator.java // Cálculo 00 — Aba 1 (gross-up + PMIX)
│   ├── ProducaoCalculator.java          // Cálculo 01 — Aba 4
│   ├── ConsumoEspecificoCalculator.java // Cálculo 02 — Aba 6
│   ├── ConsumoAreaCalculator.java       // Cálculo 03 — Aba 7 ⚠️ mais complexo
│   ├── GeracaoPropriaCalculator.java    // Cálculo 04 — Aba 9
│   ├── EncargosEssEerCalculator.java    // Cálculo 05 — Aba 12
│   ├── TabelaTarifasCalculator.java     // Cálculo 06 — Aba 2
│   ├── DistribuicaoCargaCalculator.java // Cálculo 07 — Aba 13
│   ├── ClasseCustoCalculator.java       // Cálculo 08 — Aba 14
│   ├── CentroCustosCalculator.java      // Cálculo 09 — Aba 15
│   ├── ResumoGeralCalculator.java       // Cálculo 10 — Aba 16
│   └── MotorCalculoOrchestrator.java    // Orquestra todos na ordem correta
└── util/
    └── CalendarioUtil.java              // dias úteis, feriados, horas
```

---

## Orquestrador — MotorCalculoOrchestrator

Classe que executa todos os Calculators na ordem correta da cadeia de dependências:

```java
public class MotorCalculoOrchestrator {

    public ResumoGeral executar(AllInputs inputs) {
        // CAMADA 0
        var grossUp = grossUpCalc.calcular(inputs.tarifasBrutas(), inputs.fornecedoresEnergia());
        var producao = producaoCalc.calcular(inputs.planejamentoProducao());
        var consumoEspecifico = consumoEspCalc.calcular(inputs.consumoEspecifico());

        // CAMADA 0.5
        var tabelaTarifas = tarifasCalc.calcular(grossUp);

        // CAMADA 1
        var consumoArea = consumoAreaCalc.calcular(producao, consumoEspecifico);
        var geracaoPropria = geracaoCalc.calcular(inputs.planejamentoGeracao(), inputs.calendario());

        // CAMADA 2
        var encargos = encargosCalc.calcular(consumoArea, geracaoPropria, inputs.encargos());
        var distribCarga = distribCargaCalc.calcular(consumoArea, geracaoPropria, inputs.carga());

        // CAMADA 3
        var classeCusto = classeCustoCalc.calcular(
            geracaoPropria, tabelaTarifas, distribCarga, consumoArea, inputs.demanda());

        // CAMADA 4
        var centroCustos = centroCustosCalc.calcular(classeCusto, consumoArea);

        // CAMADA 5
        return resumoGeralCalc.calcular(producao, encargos, centroCustos, inputs.manuais());
    }
}
```

---

## Regras para Implementação

1. **Cada Calculator deve ser testável isoladamente** — injetar dependências via construtor
2. **Valores monetários (R$) em `BigDecimal`** — usar `RoundingMode.HALF_UP` com 2 casas decimais
3. **Constantes nomeadas** — nada de magic numbers (ex: `private static final int MULTIPLICADOR_KTMS_PARA_TMS = 1000`)
4. **Validação de inputs** — null checks, valores negativos, meses inválidos
5. **Manter rastreabilidade** — comentários referenciando a célula/aba Excel de origem (ex: `// Ref: Aba 9, célula C15`)
6. **Respeitar cadeia de dependências** — NUNCA acessar input de outro Calculator diretamente; receber o resultado como parâmetro
7. **Testes unitários** — para cada Calculator, testar com valores conhecidos do Excel
8. **Teste de integração** — rodar o Orchestrator inteiro e comparar o resultado final (R$ 392M, R$ 19,40/tms) com o Excel

---

## CSVs de Referência Disponíveis

Na pasta `samarco-csv/` do projeto:
- `calc_01_producao_oculta.csv`
- `consumo_especifico.csv`
- `demanda_contratada.csv`
- `planejamento_geracao.csv`
- `planejamento_producao.csv`
- `tarifas_distribuidoras.csv`

Use estes CSVs como **fixtures de teste** — os valores neles são a "resposta certa" que o Java deve reproduzir.

---

## Resumo de Volume

| Cálculo | Aba Excel | Fórmulas | Complexidade |
|---------|-----------|----------|-------------|
| 00 Gross-Up + PMIX | 1 | 121 | Média (tributário) |
| 01 Produção | 4 | 192 | Baixa |
| 02 Consumo Específico | 6 | 180 | Baixa |
| 03 Consumo Área | 7 | 536 | **Alta** |
| 04 Geração Própria | 9 | 126 | Média |
| 05 Encargos ESS/EER | 12 | 125 | Média |
| 06 Tabela Tarifas | 2 | 156 | Média (vigências) |
| 07 Distrib Carga | 13 | 204 | Média |
| 08 Classe de Custo | 14 | 181 | **Alta** |
| 09 Centro de Custos | 15 | 684 | **Alta** (rateio) |
| 10 Resumo Geral | 16 | 215 | Média |
| **TOTAL** | | **2.720** | |