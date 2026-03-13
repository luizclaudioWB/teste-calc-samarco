package com.samarco.calc.input;

import com.samarco.calc.model.AreaProducao;
import com.samarco.calc.model.Mes;

import java.util.Map;

/**
 * Input de produção planejada em ktms (kilotoneladas métricas secas).
 * Ref: Aba "3Planejamento Produção - INPUT"
 */
public record PlanejamentoProducaoInput(
        Map<AreaProducao, Map<Mes, Double>> producaoKtms
) {
    public PlanejamentoProducaoInput {
        if (producaoKtms == null || producaoKtms.isEmpty()) {
            throw new IllegalArgumentException("Produção em ktms não pode ser nula ou vazia");
        }
    }
}
