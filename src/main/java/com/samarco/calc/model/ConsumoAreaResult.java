package com.samarco.calc.model;

import java.util.Map;

/**
 * Resultado do Cálculo 03 — Consumo Área.
 * Ref: Aba "7Consumo Área - CALC"
 */
public record ConsumoAreaResult(
        // Consumo por centro de custo em kWh — Ref: Aba 7, rows 3-17
        Map<AreaConsumoEspecifico, Map<Mes, Double>> consumoKWh,
        // Consumo agregado por grupo em MWh — Ref: Aba 7, rows 21-27
        Map<GrupoConsumo, Map<Mes, Double>> consumoMWh,
        // Total geral em MWh — Ref: Aba 7, row 28
        Map<Mes, Double> totalMWh,
        // Consumo MG em MWh — Ref: Aba 7, row 31
        Map<Mes, Double> consumoMG_MWh,
        // Consumo ES em MWh — Ref: Aba 7, row 32
        Map<Mes, Double> consumoES_MWh,
        // Percentual MG — Ref: Aba 7, row 33
        Map<Mes, Double> percentualMG,
        // Percentual ES — Ref: Aba 7, row 34
        Map<Mes, Double> percentualES,
        // % por centro de custo MG — Ref: Aba 7, rows 38-45
        // Filtragem usa denominador dinâmico (MG MWh do mês), demais usam Jan
        Map<AreaConsumoEspecifico, Map<Mes, Double>> percentualPorCentroCustoMG,
        // % por centro de custo ES — Ref: Aba 7, rows 46-52
        // Todos usam denominador fixo (ES MWh de Jan)
        Map<AreaConsumoEspecifico, Map<Mes, Double>> percentualPorCentroCustoES
) {
}
