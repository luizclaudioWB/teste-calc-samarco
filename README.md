# Motor de Cálculo de Energia Samarco

Motor de cálculo de alta precisão em **Java 21 + Quarkus 3.32.3** que reproduz exatamente os cálculos da planilha Excel de planejamento energético da Samarco (~R$ 392M/ano).

**Regra de ouro:** Se o resultado Java bate com o Excel, tá certo. Sem arredondamento, sem opinião.

---

## Status de Implementação

| Camada | Cálculo | Status | Aba Excel |
|--------|---------|--------|-----------|
| 0 | Produção (`ProducaoCalculator`) | Implementado | Aba 4 |
| 0 | Consumo Específico (`ConsumoEspecificoCalculator`) | Implementado | Aba 6 |
| 0 | Gross-Up Tributário (`GrossUpTributarioCalculator`) | Pendente | Aba 1 |
| 1 | Consumo Área (`ConsumoAreaCalculator`) | Implementado | Aba 7 |
| 1 | Geração Própria (`GeracaoPropriaCalculator`) | Implementado | Aba 9 |
| 2 | Encargos ESS/EER (`EncargosEssEerCalculator`) | Implementado | Aba 12 |
| 2 | Distribuição de Carga (`DistribuicaoCargaCalculator`) | Pendente | Aba 13 |
| 3 | Classe de Custo (`ClasseCustoCalculator`) | Pendente | Aba 14 |
| 4 | Centro de Custos (`CentroCustosCalculator`) | Pendente | Aba 15 |
| 5 | Resumo Geral (`ResumoGeralCalculator`) | Pendente | Aba 16 |

**27 testes de paridade passando** (Java vs Excel) para os 5 cálculos implementados.

---

## Arquitetura e Cadeia de Dependências

```
Cálculo 01 (Produção) ──────────────┐
                                     ├──→ Cálculo 03 (Consumo Área) ──┐
Cálculo 02 (Consumo Específico) ────┘                                 │
                                                                       ├──→ Cálculo 05 (Encargos)
Cálculo 04 (Geração Própria) ← Calendário ───────────────────────────┘
```

Cada `Calculator` é um `@ApplicationScoped` CDI bean, testável isoladamente. O `MotorCalculoOrchestrator` executa tudo na ordem correta.

### Estrutura de Pacotes

```
src/main/java/com/samarco/calc/
├── model/          Mes, AreaProducao, AreaConsumoEspecifico, GrupoConsumo, Calendario, *Result
├── input/          PlanejamentoProducaoInput, ConsumoEspecificoInput, CalendarioInput, TarifasEncargosInput
├── service/        ProducaoCalculator, ConsumoEspecificoCalculator, ConsumoAreaCalculator,
│                   GeracaoPropriaCalculator, EncargosEssEerCalculator, MotorCalculoOrchestrator
├── graphql/        MotorCalculoResource (endpoint GraphQL)
└── util/           CalendarioUtil
```

---

## API GraphQL

### Subir o servidor
```bash
./mvnw quarkus:dev
```
Acesse o GraphQL UI em: `http://localhost:8080/q/graphql-ui`

### Query completa via curl

```bash
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ calcularMotorCompleto { consumoAreaTotalMWh { mes valor } consumoMG_MWh { mes valor } consumoES_MWh { mes valor } percentualMG { mes valor } percentualES { mes valor } geracaoGuilmanMWh { mes valor } geracaoMunizFreireMWh { mes valor } geracaoTotalMWh { mes valor } calendario { mes diasNoMes diasUteis horasPonta horasForaPonta totalHoras } consumoSamarcoMWh { mes valor } valorTotalEER { mes valor } valorTotalESS { mes valor } mgEER { mes valor } mgESS { mes valor } mgTotal { mes valor } esEER { mes valor } esESS { mes valor } esTotal { mes valor } } }"
  }' | python3 -m json.tool
```

### Exemplo de resposta (JSON resumido)

```json
{
  "data": {
    "calcularMotorCompleto": {
      "consumoAreaTotalMWh": [
        { "mes": "Jan", "valor": 116677.13 },
        { "mes": "Fev", "valor": 114099.83 },
        { "mes": "Mar", "valor": 117103.52 }
      ],
      "geracaoTotalMWh": [
        { "mes": "Jan", "valor": 37922.17 },
        { "mes": "Fev", "valor": 33560.45 },
        { "mes": "Mar", "valor": 37627.73 }
      ],
      "consumoSamarcoMWh": [
        { "mes": "Jan", "valor": 78754.96 },
        { "mes": "Fev", "valor": 80539.38 },
        { "mes": "Mar", "valor": 79475.79 }
      ],
      "valorTotalEER": [
        { "mes": "Jan", "valor": 1152969.07 },
        { "mes": "Fev", "valor": 1426554.71 },
        { "mes": "Mar", "valor": 1577533.85 }
      ],
      "mgTotal": [
        { "mes": "Jan", "valor": 821927.44 },
        { "mes": "Fev", "valor": 866999.50 },
        { "mes": "Mar", "valor": 1005333.70 }
      ],
      "esTotal": [
        { "mes": "Jan", "valor": 570072.97 },
        { "mes": "Fev", "valor": 576262.05 },
        { "mes": "Mar", "valor": 694225.65 }
      ],
      "calendario": [
        { "mes": "Jan", "diasNoMes": 31, "diasUteis": 21, "horasPonta": 63.0, "horasForaPonta": 681.0, "totalHoras": 744.0 }
      ],
      "percentualMG": [
        { "mes": "Jan", "valor": 0.5905 }
      ],
      "percentualES": [
        { "mes": "Jan", "valor": 0.4095 }
      ]
    }
  }
}
```

---

## Metodologia de Validação

1. **Extração (`extract_excel.py`):** Script Python que lê a planilha `SAMARCO-PLANILHA-REPLY.xlsx` e exporta valores calculados para CSVs.
2. **Fixtures (`src/test/resources/fixtures/`):** CSVs com precisão total (15+ casas decimais) servem como verdade absoluta.
3. **Testes de Paridade:** Cada `CalculatorTest` carrega o CSV via `CsvLoader` e compara Java vs Excel.

### Executando Testes
```bash
./mvnw test                                    # Todos os testes
./mvnw test -Dtest=ConsumoAreaCalculatorTest   # Teste específico
./mvnw test -Dtest="com.samarco.calc.**"       # Todos os testes Samarco
```

### Fixtures disponíveis

| Arquivo | Conteúdo |
|---------|----------|
| `planejamento_producao.csv` | Input: 16 áreas × 12 meses (ktms) |
| `consumo_especifico.csv` | Input: 16 áreas × 12 meses (kWh/tms) |
| `planejamento_geracao.csv` | Input: 2 usinas × 12 meses (MWh) |
| `calendario_input.csv` | Input: dias/mês e dias não úteis |
| `tarifas_encargos.csv` | Input: ERR, ERCAP, ESS por mês (R$/MWh) |
| `expected_calc01_producao.csv` | Output esperado: Cálculo 01 |
| `expected_calc02_consumo_especifico.csv` | Output esperado: Cálculo 02 |
| `expected_calc03_consumo_area.csv` | Output esperado: Cálculo 03 |
| `expected_calc04_geracao.csv` | Output esperado: Cálculo 04 |
| `expected_calc05_encargos.csv` | Output esperado: Cálculo 05 |

---

## Convenções

* **BigDecimal** para valores em R$ (`RoundingMode.HALF_UP`)
* **double** para MWh/tms (precisão suficiente)
* **Records** imutáveis para models e inputs
* **Comentários `// Ref: Aba X, célula Y`** rastreando a origem no Excel
* **Constantes nomeadas** — zero magic numbers
* Novos cálculos devem ser registrados no `MotorCalculoOrchestrator`

### Comandos
```bash
./mvnw quarkus:dev        # Modo Dev (Live Coding)
./mvnw test               # Rodar testes
python3 extract_excel.py  # Re-extrair fixtures do Excel
./mvnw package -Dnative   # Build nativo
```

---

## Prompt para Manus AI (Frontend)

Use este prompt para gerar um frontend que replica o cálculo do Excel e valida contra a API:

---

**PROMPT PARA MANUS AI:**

```
Preciso de um frontend web (React ou Vue) para visualizar e validar cálculos de planejamento energético da Samarco.

## Contexto
Tenho uma API GraphQL (Quarkus) rodando em http://localhost:8080/graphql que executa 5 cálculos encadeados de energia. Os cálculos reproduzem exatamente uma planilha Excel de planejamento energético (~R$ 392M/ano).

## O que a API retorna
O endpoint GraphQL `calcularMotorCompleto` retorna:
- Consumo por área (MWh) — 16 áreas × 12 meses, totais, distribuição MG/ES
- Geração própria (MWh) — 2 usinas (Guilman Amorim + Muniz Freire) × 12 meses
- Calendário — dias úteis, horas ponta/fora ponta por mês
- Encargos ESS/EER (R$) — valores nacionais + distribuição por estado (MG/ES)
- Percentuais de rateio MG/ES por mês

## curl de exemplo
curl -s http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ calcularMotorCompleto { consumoAreaTotalMWh { mes valor } geracaoTotalMWh { mes valor } consumoSamarcoMWh { mes valor } valorTotalEER { mes valor } valorTotalESS { mes valor } mgTotal { mes valor } esTotal { mes valor } calendario { mes diasNoMes diasUteis totalHoras } percentualMG { mes valor } percentualES { mes valor } } }"}'

## Funcionalidades do frontend
1. **Dashboard** com resumo anual: consumo total, geração total, consumo líquido (Samarco), custo total encargos
2. **Tabela 12 meses** estilo planilha com todas as métricas (consumo, geração, encargos)
3. **Gráficos** (barras empilhadas MG/ES, linha temporal de consumo vs geração)
4. **Painel de validação** que compara cada valor do frontend com o retorno da API — status verde/vermelho
5. **Calendário** mostrando dias úteis, horas ponta/fora ponta por mês
6. Tudo em português brasileiro, valores monetários em R$ formatados

## Dados da planilha Excel (para validação)
Os valores abaixo são a "verdade" do Excel. O frontend deve mostrar se o cálculo da API bate:

Consumo Total (MWh/mês): Jan=116677, Fev=114100, Mar=117104, Abr=121920, Mai=122278, Jun=124428, Jul=126421, Ago=122126, Set=123551, Out=123776, Nov=115636, Dez=119383
Geração Total (MWh/mês): Jan=37922, Fev=33560, Mar=37628, Abr=34096, Mai=34323, Jun=25437, Jul=22311, Ago=21151, Set=20337, Out=22329, Nov=28136, Dez=34939
Consumo Samarco (MWh): Jan=78755, Fev=80539, Mar=79476, Abr=87824, Mai=87955, Jun=98992, Jul=104110, Ago=100975, Set=103213, Out=101447, Nov=87500, Dez=84444
MG Total Encargos (R$): Jan=821927, Fev=867000, Mar=1005334, Abr=1060381, Mai=1054376, Jun=919656, Jul=1266426, Ago=1345995, Set=1309619, Out=1099425, Nov=1153500
ES Total Encargos (R$): Jan=570073, Fev=576262, Mar=694226, Abr=799144, Mai=794180, Jun=715650, Jul=1012574, Ago=1007446, Set=1007491, Out=845343, Nov=783892

## Stack sugerida
React + Vite + TailwindCSS + Recharts (ou Chart.js) + Apollo Client para GraphQL
Ou Vue + Nuxt se preferir.

Gere o projeto completo com README, package.json, e instruções de como rodar.
```

---

### Como usar o prompt

1. Suba a API: `./mvnw quarkus:dev`
2. Copie o prompt acima para o Manus AI (ou outro assistente de IA)
3. Anexe também:
   - O arquivo `SAMARCO-PLANILHA-REPLY.xlsx` (spec visual)
   - O JSON de retorno da API (rode o curl acima e salve)
4. Itere pedindo: histórico de faturas, cadastro de distribuidoras, tabela de tarifas, etc.

---

## Fluxo de Migração

```
Excel (dados)    → CSV (input)           [feito]
Excel (fórmulas) → Classes Java          [feito - 5 cálculos implementados]
CSV + Java       → Testes de paridade    [feito - 27 testes passando]
API GraphQL      → Endpoint exposto      [feito]
Frontend Manus   → Visualização + validação [próximo passo]
Classes validadas → Copiar pro Samarco   [integrar no sistema real]
```
