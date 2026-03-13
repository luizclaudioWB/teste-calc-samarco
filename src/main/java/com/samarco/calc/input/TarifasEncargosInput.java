package com.samarco.calc.input;

import com.samarco.calc.model.Mes;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Input de tarifas de encargos ESS/EER.
 * IMPORTANTE: As tarifas variam por mês (diferente do que o spec original sugeria).
 * Ref: Aba "12Encargos ESS EER - CALC", rows 2, 3 e 7
 */
public record TarifasEncargosInput(
        Map<Mes, BigDecimal> errTarifa,   // Ref: Aba 12, row 2 — R$/MWh
        Map<Mes, BigDecimal> ercapTarifa, // Ref: Aba 12, row 3 — R$/MWh
        Map<Mes, BigDecimal> essTarifa    // Ref: Aba 12, row 7 — R$/MWh
) {
    public TarifasEncargosInput {
        if (errTarifa == null || errTarifa.isEmpty()) {
            throw new IllegalArgumentException("Tarifa ERR não pode ser nula ou vazia");
        }
        if (ercapTarifa == null || ercapTarifa.isEmpty()) {
            throw new IllegalArgumentException("Tarifa ERCAP não pode ser nula ou vazia");
        }
        if (essTarifa == null || essTarifa.isEmpty()) {
            throw new IllegalArgumentException("Tarifa ESS não pode ser nula ou vazia");
        }
    }
}
