package com.samarco.calc.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Resultado do Cálculo 05 — Encargos ESS/EER.
 * Ref: Aba "12Encargos ESS EER - CALC"
 */
public record EncargosResult(
        // Seção 1 — Nacional
        Map<Mes, BigDecimal> totalErrErcapTarifa, // Ref: Aba 12, row 4 = ERR + ERCAP
        Map<Mes, BigDecimal> consumoSamarcoMWh,   // Ref: Aba 12, row 5 = ConsumoArea_Total - Geracao_Total
        Map<Mes, BigDecimal> valorTotalEer,        // Ref: Aba 12, row 6 = consumo × totalErrErcap
        Map<Mes, BigDecimal> valorTotalEss,        // Ref: Aba 12, row 8 = ESS × consumo

        // Seção 2 — Distribuição por estado
        Map<Mes, BigDecimal> mgEer,   // Ref: Aba 12, row 11 = valorEer × %MG
        Map<Mes, BigDecimal> mgEss,   // Ref: Aba 12, row 12 = valorEss × %MG
        Map<Mes, BigDecimal> mgTotal, // Ref: Aba 12, row 13 = mgEer + mgEss
        Map<Mes, BigDecimal> esEer,   // Ref: Aba 12, row 14 = valorEer × %ES
        Map<Mes, BigDecimal> esEss,   // Ref: Aba 12, row 15 = valorEss × %ES
        Map<Mes, BigDecimal> esTotal  // Ref: Aba 12, row 16 = esEer + esEss
) {
}
