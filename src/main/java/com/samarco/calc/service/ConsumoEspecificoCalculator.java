package com.samarco.calc.service;

import com.samarco.calc.input.ConsumoEspecificoInput;
import com.samarco.calc.model.AreaConsumoEspecifico;
import com.samarco.calc.model.Mes;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cálculo 02 — Consumo Específico.
 * Pass-through com validação dos valores de input.
 * Ref: Aba "6Consumo Especifico - OCULTA"
 * Fórmula: ConsumoEspecifico[área][mês] = PlanConsumoEspecificoInput[área][mês]
 */
@ApplicationScoped
public class ConsumoEspecificoCalculator {

    /**
     * Valida e retorna o consumo específico.
     *
     * @param input consumo específico em kWh/tms
     * @return consumo específico validado por área e mês
     */
    public Map<AreaConsumoEspecifico, Map<Mes, Double>> calcular(ConsumoEspecificoInput input) {
        if (input == null) {
            throw new IllegalArgumentException("Input de consumo específico não pode ser nulo");
        }

        Map<AreaConsumoEspecifico, Map<Mes, Double>> resultado = new EnumMap<>(AreaConsumoEspecifico.class);

        for (Map.Entry<AreaConsumoEspecifico, Map<Mes, Double>> entry : input.consumoEspecifico().entrySet()) {
            AreaConsumoEspecifico area = entry.getKey();
            Map<Mes, Double> valoresMes = entry.getValue();

            Map<Mes, Double> consumoValidado = new EnumMap<>(Mes.class);
            for (Map.Entry<Mes, Double> mesEntry : valoresMes.entrySet()) {
                Mes mes = mesEntry.getKey();
                Double valor = mesEntry.getValue();

                if (valor == null) {
                    throw new IllegalArgumentException(
                            "Valor nulo para área " + area.getLabel() + " mês " + mes.getLabel());
                }
                if (valor < 0) {
                    throw new IllegalArgumentException(
                            "Valor negativo para área " + area.getLabel() + " mês " + mes.getLabel() + ": " + valor);
                }

                // Ref: Aba 6, C3 = '5Plan Consumo Especifico -INPUT'!B3
                consumoValidado.put(mes, valor);
            }

            resultado.put(area, consumoValidado);
        }

        return resultado;
    }
}
