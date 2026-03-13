package com.samarco.calc.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Resultado do Cálculo — Distribuição por Centro de Custos.
 * Ref: Aba "15Distrib Centro de Custos"
 *
 * Para cada centro de custo (= AreaConsumoEspecifico), 3 classes de custo:
 * - 50610002 (Consumo de Energia)
 * - 50610003 (Uso de Rede / TUSD FIO)
 * - 50610006 (Encargo / TUSD ENCARGO)
 */
public record CentroCustosResult(
        // Por centro de custo MG — Ref: Aba 15, rows 3-28
        Map<AreaConsumoEspecifico, Map<Mes, BigDecimal>> consumoMG,    // 50610002
        Map<AreaConsumoEspecifico, Map<Mes, BigDecimal>> usoRedeMG,    // 50610003
        Map<AreaConsumoEspecifico, Map<Mes, BigDecimal>> encargoMG,    // 50610006

        // Por centro de custo ES — Ref: Aba 15, rows 29-49
        Map<AreaConsumoEspecifico, Map<Mes, BigDecimal>> consumoES,    // 50610002
        Map<AreaConsumoEspecifico, Map<Mes, BigDecimal>> usoRedeES,    // 50610003
        Map<AreaConsumoEspecifico, Map<Mes, BigDecimal>> encargoES,    // 50610006

        // Totais por classe — Ref: Aba 15, rows 64-67
        Map<Mes, BigDecimal> totalConsumoSamarco,   // 50610002 MG+ES
        Map<Mes, BigDecimal> totalUsoRedeSamarco,    // 50610003 MG+ES
        Map<Mes, BigDecimal> totalEncargoSamarco,    // 50610006 MG+ES
        Map<Mes, BigDecimal> totalGeralSamarco       // soma dos 3
) {
}
