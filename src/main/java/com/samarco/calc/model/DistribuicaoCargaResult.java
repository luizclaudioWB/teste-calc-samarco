package com.samarco.calc.model;

import java.util.Map;

/**
 * Resultado do Cálculo — Distribuição de Carga por Estado.
 * Ref: Aba "13Distribuição de Carga"
 */
public record DistribuicaoCargaResult(
        // MG — Ref: Aba 13, rows 3-11
        Map<Mes, Double> consumoMG_MWh,          // row 5 = Aba7!C31
        Map<Mes, Double> geracaoGuilmanMWh,      // row 6 = Aba9!C15
        Map<Mes, Double> perdasMG_MWh,           // row 7 = consumoMG × 0.03
        Map<Mes, Double> necessidadeTotalMG,     // row 9 = consumo + perdas
        Map<Mes, Double> necessidadeCompraMG,    // row 10 = necessidadeTotal - geração
        Map<Mes, Double> qtdeCompradaMG,         // row 11 = copy of row 10

        // ES — Ref: Aba 13, rows 14-22
        Map<Mes, Double> consumoES_MWh,          // row 16 = Aba7!C32
        Map<Mes, Double> geracaoMunizFreireMWh,  // row 17 = Aba9!C17
        Map<Mes, Double> perdasES_MWh,           // row 18 = consumoES × 0.03
        Map<Mes, Double> necessidadeTotalES,     // row 20 = consumo + perdas
        Map<Mes, Double> necessidadeCompraES,    // row 21 = necessidadeTotal - geração
        Map<Mes, Double> qtdeCompradaES,         // row 22 = copy of row 21

        // Samarco Total — Ref: Aba 13, rows 25-32
        Map<Mes, Double> consumoTotalMWh,        // row 26 = MG + ES
        Map<Mes, Double> geracaoTotalMWh,        // row 27 = Guilman + MunizFreire
        Map<Mes, Double> perdasTotalMWh,         // row 28 = perdasMG + perdasES
        Map<Mes, Double> necessidadeTotalSamarco,// row 30
        Map<Mes, Double> necessidadeCompraSamarco,// row 31
        Map<Mes, Double> qtdeCompradaSamarco     // row 32
) {
}
