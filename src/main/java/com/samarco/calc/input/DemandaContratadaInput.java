package com.samarco.calc.input;

import com.samarco.calc.model.Mes;
import com.samarco.calc.model.Unidade;

import java.util.Map;

/**
 * Input de demanda contratada por unidade e horário.
 * Ref: Aba "10Demanda - INPUT"
 *
 * Valores em kW (constantes por mês no planejamento 2026).
 */
public record DemandaContratadaInput(
        // Demanda Ponta (kW) por unidade e mês
        Map<Unidade, Map<Mes, Double>> demandaPonta,
        // Demanda Fora Ponta (kW) por unidade e mês
        Map<Unidade, Map<Mes, Double>> demandaForaPonta
) {
    public DemandaContratadaInput {
        if (demandaPonta == null || demandaForaPonta == null) {
            throw new IllegalArgumentException("Demanda contratada: nenhum campo pode ser nulo");
        }
    }
}
