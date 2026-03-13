package com.samarco.calc.model;

import java.util.Map;

/**
 * Resultado do Cálculo 04 — Geração Própria.
 * Ref: Aba "9Geração Própria - CALC", linhas 15-20
 */
public record GeracaoPropriaResult(
        Map<Mes, Calendario> calendario,
        Map<Mes, Double> guilmanMWh,       // Ref: Aba 9, row 15
        Map<Mes, Double> guilmanMWmedios,  // Ref: Aba 9, row 16 = guilmanMWh / totalHoras
        Map<Mes, Double> munizFreireMWh,   // Ref: Aba 9, row 17
        Map<Mes, Double> munizFreireMWmedios, // Ref: Aba 9, row 18 = munizFreireMWh / totalHoras
        Map<Mes, Double> totalMWh,         // Ref: Aba 9, row 19 = guilman + munizFreire
        Map<Mes, Double> totalMWmedios     // Ref: Aba 9, row 20 = totalMWh / totalHoras
) {
}
