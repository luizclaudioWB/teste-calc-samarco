package com.samarco.calc.model;

/**
 * Grupos de consumo agregados para conversão MWh.
 * Ref: Aba "7Consumo Área - CALC", rows 21-27
 */
public enum GrupoConsumo {
    // Ref: Aba 7, row 21 — agrega row 3 (Filtragem Germano)
    FILTRAGEM_GERMANO("Filtragem Germano"),
    // Ref: Aba 7, row 22 — agrega row 4 (Mineração)
    MINERACAO("Mineração"),
    // Ref: Aba 7, row 23 — agrega rows 5-7 (Beneficiamento 1+2+3)
    CONCENTRACAO("Concentração"),
    // Ref: Aba 7, row 24 — agrega rows 8-10 (Mineroduto 1+2+3)
    MINERODUTO("Mineroduto"),
    // Ref: Aba 7, row 25 — agrega rows 11-12 (Preparação 1+2)
    PREPARACAO("Preparação"),
    // Ref: Aba 7, row 26 — agrega rows 13-16 (Usina 1+2+3+4)
    PELOTIZACAO("Pelotização"),
    // Ref: Aba 7, row 27 — agrega row 17 (Estocagem)
    ESTOCAGEM("Estocagem");

    private final String label;

    GrupoConsumo(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
