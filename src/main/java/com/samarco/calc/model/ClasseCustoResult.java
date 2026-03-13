package com.samarco.calc.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Resultado do Cálculo — Distribuição de Classe de Custo por Estado.
 * Ref: Aba "14Distrib de Classe de Custo"
 */
public record ClasseCustoResult(
        // MG — Ref: Aba 14, rows 3-12
        Map<Mes, BigDecimal> devolucaoProinfaMG,    // row 4 = Guilman × Desconto_Germano
        Map<Mes, BigDecimal> tusdFioGermano,        // row 5 = demandaPonta×tarifaPonta + demandaFP×tarifaFP
        Map<Mes, BigDecimal> tusdFioMatipo,         // row 6 = idem Matipó
        Map<Mes, BigDecimal> totalTusdFioMG,        // row 7 = Germano + Matipó (50610003)
        Map<Mes, BigDecimal> tusdEncargoGermano,    // row 8 = SUM(kWh rows 3-7)/1000 × tarifa
        Map<Mes, BigDecimal> tusdEncargoMatipo,     // row 9 = SUM(kWh rows 8-10)/1000 × tarifa
        Map<Mes, BigDecimal> totalTusdEncargoMG,    // row 10 = MAX(0, Germano-Proinfa) + Matipó (50610006)
        Map<Mes, BigDecimal> consumoEnergiaMG,      // row 11 = qtdeCompradaMG × pmixMG (50610002)
        Map<Mes, BigDecimal> totalConsumoMG,        // row 12

        // ES — Ref: Aba 14, rows 14-21
        Map<Mes, BigDecimal> devolucaoProinfaES,    // row 15 = MunizFreire × Desconto_UBU
        Map<Mes, BigDecimal> tusdFioUbu,            // row 16 (50610003)
        Map<Mes, BigDecimal> totalTusdFioES,        // row 17
        Map<Mes, BigDecimal> tusdEncargoUbu,        // row 18 = SUM(kWh rows 11-17)/1000 × tarifa
        Map<Mes, BigDecimal> totalTusdEncargoES,    // row 19 = MAX(0, UBU-Proinfa) (50610006)
        Map<Mes, BigDecimal> consumoEnergiaES,      // row 20 = qtdeCompradaES × pmixES (50610002)
        Map<Mes, BigDecimal> totalConsumoES         // row 21
) {
}
