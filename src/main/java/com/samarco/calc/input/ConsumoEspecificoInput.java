package com.samarco.calc.input;

import com.samarco.calc.model.AreaConsumoEspecifico;
import com.samarco.calc.model.Mes;

import java.util.Map;

/**
 * Input de consumo específico em kWh/tms.
 * Ref: Aba "5Plan Consumo Especifico -INPUT"
 */
public record ConsumoEspecificoInput(
        Map<AreaConsumoEspecifico, Map<Mes, Double>> consumoEspecifico
) {
    public ConsumoEspecificoInput {
        if (consumoEspecifico == null || consumoEspecifico.isEmpty()) {
            throw new IllegalArgumentException("Consumo específico não pode ser nulo ou vazio");
        }
    }
}
