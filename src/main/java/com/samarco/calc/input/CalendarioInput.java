package com.samarco.calc.input;

import com.samarco.calc.model.Mes;

import java.util.Map;

/**
 * Input de calendário: dias no mês e dias não úteis (feriados + finais de semana).
 * Ref: Aba "9Geração Própria - CALC", rows 6-7
 */
public record CalendarioInput(
        Map<Mes, Integer> diasNoMes,     // Ref: Aba 9, row 6
        Map<Mes, Integer> diasNaoUteis   // Ref: Aba 9, row 7
) {
    public CalendarioInput {
        if (diasNoMes == null || diasNoMes.isEmpty()) {
            throw new IllegalArgumentException("Dias no mês não pode ser nulo ou vazio");
        }
        if (diasNaoUteis == null || diasNaoUteis.isEmpty()) {
            throw new IllegalArgumentException("Dias não úteis não pode ser nulo ou vazio");
        }
    }
}
