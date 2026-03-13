import { gql } from "@apollo/client";

/**
 * Fragment for the standard { mes, valor } pair used across all calculation results.
 */
const MES_VALOR_FIELDS = `{ mes valor }`;

/**
 * Fragment for the calendar data used in Geração Própria (Cálculo 04).
 */
const CALENDARIO_FIELDS = `{
  mes
  diasNoMes
  diasUteis
  horasPonta
  horasForaPonta
  totalHoras
}`;

/**
 * Complete query for the SAMARCO Motor de Cálculo Energético.
 * Covers all 10 calculation blocks (Cálculos 01-05, 13-16).
 *
 * Backend: Quarkus + SmallRye GraphQL
 * Endpoint: http://localhost:8080/graphql
 */
export const MOTOR_CALCULO_QUERY = gql`
  query CalcularMotorCompleto {
    calcularMotorCompleto {

      # ─── Cálculo 01 — Produção (tms) ───────────────────────────────────────
      producaoTms ${MES_VALOR_FIELDS}
      producaoPelotas ${MES_VALOR_FIELDS}
      producaoPelletFeed ${MES_VALOR_FIELDS}
      producaoPSC ${MES_VALOR_FIELDS}
      producaoPSM ${MES_VALOR_FIELDS}

      # ─── Cálculo 02 — Consumo Específico (kWh/tms) ─────────────────────────
      consumoEspecificoKWhTms ${MES_VALOR_FIELDS}

      # ─── Cálculo 03 — Consumo Área (MWh) ───────────────────────────────────
      consumoAreaTotalMWh ${MES_VALOR_FIELDS}
      consumoMG_MWh ${MES_VALOR_FIELDS}
      consumoES_MWh ${MES_VALOR_FIELDS}
      percentualMG ${MES_VALOR_FIELDS}
      percentualES ${MES_VALOR_FIELDS}

      # ─── Cálculo 04 — Geração Própria (MWh) ────────────────────────────────
      geracaoGuilmanMWh ${MES_VALOR_FIELDS}
      geracaoMunizFreireMWh ${MES_VALOR_FIELDS}
      geracaoTotalMWh ${MES_VALOR_FIELDS}
      calendario ${CALENDARIO_FIELDS}

      # ─── Cálculo 05 — Encargos ESS/EER (R$) ────────────────────────────────
      consumoSamarcoMWh ${MES_VALOR_FIELDS}
      valorTotalEER ${MES_VALOR_FIELDS}
      valorTotalESS ${MES_VALOR_FIELDS}
      mgEER ${MES_VALOR_FIELDS}
      mgESS ${MES_VALOR_FIELDS}
      mgTotal ${MES_VALOR_FIELDS}
      esEER ${MES_VALOR_FIELDS}
      esESS ${MES_VALOR_FIELDS}
      esTotal ${MES_VALOR_FIELDS}

      # ─── Cálculo 13 — Distribuição de Carga (MWh) ──────────────────────────
      distribCargaNecCompraMG ${MES_VALOR_FIELDS}
      distribCargaNecCompraES ${MES_VALOR_FIELDS}
      distribCargaNecCompraSamarco ${MES_VALOR_FIELDS}
      distribCargaPerdasMG ${MES_VALOR_FIELDS}
      distribCargaPerdasES ${MES_VALOR_FIELDS}

      # ─── Cálculo 14 — Classe de Custo (R$) ─────────────────────────────────
      classeCustoTusdFioMG ${MES_VALOR_FIELDS}
      classeCustoTusdFioES ${MES_VALOR_FIELDS}
      classeCustoEncargoMG ${MES_VALOR_FIELDS}
      classeCustoEncargoES ${MES_VALOR_FIELDS}
      classeCustoConsumoMG ${MES_VALOR_FIELDS}
      classeCustoConsumoES ${MES_VALOR_FIELDS}

      # ─── Cálculo 16 — Resumo Geral (R$) ────────────────────────────────────
      resumoConsumo ${MES_VALOR_FIELDS}
      resumoUsoRede ${MES_VALOR_FIELDS}
      resumoEncargo ${MES_VALOR_FIELDS}
      resumoEER ${MES_VALOR_FIELDS}
      resumoESS ${MES_VALOR_FIELDS}
      resumoTotalGeral ${MES_VALOR_FIELDS}
      resumoProducaoTotal ${MES_VALOR_FIELDS}
      resumoCustoEspecifico ${MES_VALOR_FIELDS}
      resumoCustoFixo ${MES_VALOR_FIELDS}
      resumoCustoVariavel ${MES_VALOR_FIELDS}
    }
  }
`;

/**
 * Type definitions matching the GraphQL response shape.
 * These mirror the mockApiData structure exactly for drop-in replacement.
 */
export type MesValor = { mes: string; valor: number; area?: string };

export type CalendarioItem = {
  mes: string;
  diasNoMes: number;
  diasUteis: number;
  horasPonta: number;
  horasForaPonta: number;
  totalHoras: number;
};

export type MotorCalculoData = {
  // Cálculo 01
  producaoTms: MesValor[];
  producaoPelotas?: MesValor[];
  producaoPelletFeed?: MesValor[];
  producaoPSC?: MesValor[];
  producaoPSM?: MesValor[];
  // Cálculo 02
  consumoEspecificoKWhTms?: MesValor[];
  // Cálculo 03
  consumoAreaTotalMWh: MesValor[];
  consumoMG_MWh: MesValor[];
  consumoES_MWh: MesValor[];
  percentualMG: MesValor[];
  percentualES: MesValor[];
  // Cálculo 04
  geracaoGuilmanMWh: MesValor[];
  geracaoMunizFreireMWh: MesValor[];
  geracaoTotalMWh: MesValor[];
  calendario: CalendarioItem[];
  // Cálculo 05
  consumoSamarcoMWh: MesValor[];
  valorTotalEER: MesValor[];
  valorTotalESS: MesValor[];
  mgEER: MesValor[];
  mgESS: MesValor[];
  mgTotal: MesValor[];
  esEER: MesValor[];
  esESS: MesValor[];
  esTotal: MesValor[];
  // Cálculo 13
  distribCargaNecCompraMG: MesValor[];
  distribCargaNecCompraES: MesValor[];
  distribCargaNecCompraSamarco: MesValor[];
  distribCargaPerdasMG: MesValor[];
  distribCargaPerdasES: MesValor[];
  // Cálculo 14
  classeCustoTusdFioMG: MesValor[];
  classeCustoTusdFioES: MesValor[];
  classeCustoEncargoMG: MesValor[];
  classeCustoEncargoES: MesValor[];
  classeCustoConsumoMG: MesValor[];
  classeCustoConsumoES: MesValor[];
  // Cálculo 16
  resumoConsumo: MesValor[];
  resumoUsoRede: MesValor[];
  resumoEncargo: MesValor[];
  resumoEER: MesValor[];
  resumoESS: MesValor[];
  resumoTotalGeral: MesValor[];
  resumoProducaoTotal: MesValor[];
  resumoCustoEspecifico: MesValor[];
  resumoCustoFixo: MesValor[];
  resumoCustoVariavel: MesValor[];
};

export type MotorCalculoQueryResult = {
  calcularMotorCompleto: MotorCalculoData;
};
