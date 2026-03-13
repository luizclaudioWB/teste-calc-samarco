package com.samarco.calc.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Resultado do Cálculo — Resumo Geral.
 * Ref: Aba "16Resumo GERAL"
 */
public record ResumoGeralResult(
        // Custos por classe — Ref: Aba 16, rows 3-7
        Map<Mes, BigDecimal> consumo50610002,       // row 3 = Aba15!D64
        Map<Mes, BigDecimal> usoRede50610003,       // row 4 = Aba15!D65
        Map<Mes, BigDecimal> encargo50610006,        // row 5 = Aba15!D66
        Map<Mes, BigDecimal> eer50610007,            // row 6 = Aba12!C6
        Map<Mes, BigDecimal> ess50610008,            // row 7 = Aba12!C8

        // Despesas fixas — Ref: Aba 16, rows 8-15
        Map<Mes, BigDecimal> royaltiesAneel,         // row 8
        Map<Mes, BigDecimal> servicoTransmissao,     // row 9
        Map<Mes, BigDecimal> operacaoHidreletricas,  // row 10
        Map<Mes, BigDecimal> outrasDespHidreletricas,// row 11
        Map<Mes, BigDecimal> omRedeBasica,           // row 12
        Map<Mes, BigDecimal> outrasDespRB,           // row 13
        Map<Mes, BigDecimal> energiaAdministrativa,  // row 14
        Map<Mes, BigDecimal> outrosCustosEE,         // row 15

        // Total — Ref: Aba 16, row 16
        Map<Mes, BigDecimal> totalGeral,

        // Produção — Ref: Aba 16, rows 20-24
        Map<Mes, Double> producaoPelotas,            // row 20 = SUM(Aba4!B12:B15)
        Map<Mes, Double> producaoPelletFeed,         // row 21 = Aba4!B18
        Map<Mes, Double> producaoPscPsm,             // row 22 = Aba4!B17 (Vendas)
        Map<Mes, Double> producaoTotal,              // row 23 = SUM
        Map<Mes, BigDecimal> custoEspecifico,        // row 24 = totalGeral / producaoTotal

        // Fixo vs Variável — Ref: Aba 16, rows 27-30
        Map<Mes, BigDecimal> custoFixo,              // row 27
        Map<Mes, BigDecimal> custoFixoPorTms,        // row 28
        Map<Mes, BigDecimal> custoVariavel,          // row 29
        Map<Mes, BigDecimal> custoVariavelPorTms     // row 30
) {
}
