package com.samarco.calc.model;

/**
 * Dados de calendário para um mês específico.
 * Ref: Aba "9Geração Própria - CALC", linhas 6-11
 */
public record Calendario(
        int diasNoMes,          // Ref: Aba 9, row 6
        int diasNaoUteis,       // Ref: Aba 9, row 7
        int diasUteis,          // Ref: Aba 9, row 8 = diasNoMes - diasNaoUteis
        double horasPonta,      // Ref: Aba 9, row 9 = diasUteis * HORAS_PONTA_POR_DIA
        double horasForaPonta,  // Ref: Aba 9, row 10 = totalHoras - horasPonta
        double totalHoras       // Ref: Aba 9, row 11 = HORAS_POR_DIA * diasNoMes
) {
}
