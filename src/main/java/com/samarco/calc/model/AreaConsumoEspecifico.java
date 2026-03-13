package com.samarco.calc.model;

/**
 * 16 áreas de consumo específico conforme Aba "5Plan Consumo Especifico -INPUT" e "6Consumo Especifico - OCULTA".
 * Os nomes diferem das áreas de produção.
 * Ref: Aba 6, linhas 3-18
 */
public enum AreaConsumoEspecifico {
    // Ref: Aba 6, row 3
    FILTRAGEM_GERMANO("Filtragem Germano"),
    // Ref: Aba 6, row 4 (CSV: "Mineração", Excel: "Mineração 1")
    MINERACAO_1("Mineração"),
    // Ref: Aba 6, row 5
    BENEFICIAMENTO_1("Beneficiamento 1"),
    // Ref: Aba 6, row 6
    BENEFICIAMENTO_2("Beneficiamento 2"),
    // Ref: Aba 6, row 7
    BENEFICIAMENTO_3("Beneficiamento 3"),
    // Ref: Aba 6, row 8
    MINERODUTO_1("Mineroduto 1"),
    // Ref: Aba 6, row 9
    MINERODUTO_2("Mineroduto 2"),
    // Ref: Aba 6, row 10
    MINERODUTO_3("Mineroduto 3"),
    // Ref: Aba 6, row 11
    PREPARACAO_1("Preparação 1"),
    // Ref: Aba 6, row 12
    PREPARACAO_2("Preparação 2"),
    // Ref: Aba 6, row 13
    USINA_1("Usina 1"),
    // Ref: Aba 6, row 14
    USINA_2("Usina 2"),
    // Ref: Aba 6, row 15
    USINA_3("Usina 3"),
    // Ref: Aba 6, row 16
    USINA_4("Usina 4"),
    // Ref: Aba 6, row 17 (nota: no CSV aparece como "Estocagem")
    ESTOCAGEM("Estocagem"),
    // Ref: Aba 6, row 18 — Pellet Feed aparece zerado no consumo específico
    PELLET_FEED("Pellet Feed");

    private final String label;

    AreaConsumoEspecifico(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static AreaConsumoEspecifico fromLabel(String label) {
        String trimmed = label.trim();
        for (AreaConsumoEspecifico area : values()) {
            if (area.label.equalsIgnoreCase(trimmed)) {
                return area;
            }
        }
        throw new IllegalArgumentException("Área de consumo específico inválida: " + label);
    }
}
