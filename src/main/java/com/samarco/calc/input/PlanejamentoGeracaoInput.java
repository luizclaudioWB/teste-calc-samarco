package com.samarco.calc.input;

import com.samarco.calc.model.Mes;

import java.util.Map;

/**
 * Input de geração planejada em MWh para as 2 usinas.
 * Ref: Aba "8Planejamento Geração - INPUT"
 */
public record PlanejamentoGeracaoInput(
        Map<Mes, Double> guilmanAmorimMWh,  // Ref: Aba 8, row 2
        Map<Mes, Double> munizFreireMWh     // Ref: Aba 8, row 3
) {
    public PlanejamentoGeracaoInput {
        if (guilmanAmorimMWh == null || guilmanAmorimMWh.isEmpty()) {
            throw new IllegalArgumentException("Geração Guilman Amorim não pode ser nula ou vazia");
        }
        if (munizFreireMWh == null || munizFreireMWh.isEmpty()) {
            throw new IllegalArgumentException("Geração Muniz Freire não pode ser nula ou vazia");
        }
    }
}
