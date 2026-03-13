package com.samarco.calc.input;

import com.samarco.calc.model.Mes;
import com.samarco.calc.model.Unidade;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Input de tarifas das distribuidoras por unidade e mês.
 * Ref: Aba "2Tabela Tarifas Distribuidoras"
 *
 * Tarifas mudam na troca de período tarifário (tipicamente Jul).
 */
public record TarifasDistribuidorasInput(
        // TUSD FIO / DEMANDA — Ponta (R$/kW) por unidade e mês
        Map<Unidade, Map<Mes, BigDecimal>> tusdFioPonta,
        // TUSD FIO / DEMANDA — Fora Ponta (R$/kW) por unidade e mês
        Map<Unidade, Map<Mes, BigDecimal>> tusdFioForaPonta,
        // TUSD ENCARGO — Encargo Distribuição (R$/MWh) por unidade e mês
        Map<Unidade, Map<Mes, BigDecimal>> tusdEncargo,
        // Desconto Auto-Produção (R$/MWh) por unidade e mês — usado no PROINFA
        Map<Unidade, Map<Mes, BigDecimal>> descontoAutoProducao,
        // PMIX — Preço médio de energia por estado (R$/MWh)
        Map<Mes, BigDecimal> pmixMG,
        Map<Mes, BigDecimal> pmixES
) {
    public TarifasDistribuidorasInput {
        if (tusdFioPonta == null || tusdFioForaPonta == null || tusdEncargo == null
                || descontoAutoProducao == null || pmixMG == null || pmixES == null) {
            throw new IllegalArgumentException("Tarifas distribuidoras: nenhum campo pode ser nulo");
        }
    }
}
