package com.samarco.calc.util;

import com.samarco.calc.model.Calendario;

/**
 * Utilitário para cálculo de calendário (horas ponta/fora ponta).
 * Ref: Aba "9Geração Própria - CALC", linhas 6-11
 */
public final class CalendarioUtil {

    // Ref: Aba 9, célula $B$2
    private static final int HORAS_PONTA_POR_DIA = 3;

    // Ref: Aba 9, célula $B$3
    private static final int HORAS_POR_DIA = 24;

    private CalendarioUtil() {
    }

    /**
     * Calcula o calendário de um mês.
     *
     * @param diasNoMes    Ref: Aba 9, row 6
     * @param diasNaoUteis Ref: Aba 9, row 7 (feriados + finais de semana)
     */
    public static Calendario calcular(int diasNoMes, int diasNaoUteis) {
        if (diasNoMes <= 0) {
            throw new IllegalArgumentException("Dias no mês deve ser positivo: " + diasNoMes);
        }
        if (diasNaoUteis < 0 || diasNaoUteis >= diasNoMes) {
            throw new IllegalArgumentException("Dias não úteis inválido: " + diasNaoUteis);
        }

        // Ref: Aba 9, row 8 = C6 - C7
        int diasUteis = diasNoMes - diasNaoUteis;

        // Ref: Aba 9, row 9 = C8 * $B$2
        double horasPonta = (double) diasUteis * HORAS_PONTA_POR_DIA;

        // Ref: Aba 9, row 11 = $B$3 * C6
        double totalHoras = (double) HORAS_POR_DIA * diasNoMes;

        // Ref: Aba 9, row 10 = C11 - C9
        double horasForaPonta = totalHoras - horasPonta;

        return new Calendario(diasNoMes, diasNaoUteis, diasUteis, horasPonta, horasForaPonta, totalHoras);
    }
}
