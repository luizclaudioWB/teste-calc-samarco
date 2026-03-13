package com.samarco.calc.model;

/**
 * 16 áreas de produção conforme Aba "3Planejamento Produção - INPUT" e "4Produção - OCULTA".
 * Ref: Aba 4, linhas 3-18
 */
public enum AreaProducao {
    // Ref: Aba 4, row 3
    FILTRAGEM_GERMANO("Filtragem Germano (Arenoso)"),
    // Ref: Aba 4, row 4
    BENEFICIAMENTO_USINA_1("Beneficiamento Usina 1"),
    // Ref: Aba 4, row 5
    BENEFICIAMENTO_USINA_2("Beneficiamento Usina 2"),
    // Ref: Aba 4, row 6
    BENEFICIAMENTO_USINA_3("Beneficiamento Usina 3"),
    // Ref: Aba 4, row 7
    MINERODUTO_1("Mineroduto 1"),
    // Ref: Aba 4, row 8
    MINERODUTO_2("Mineroduto 2"),
    // Ref: Aba 4, row 9
    MINERODUTO_3("Mineroduto 3"),
    // Ref: Aba 4, row 10
    PREPARACAO_1("Preparação 1"),
    // Ref: Aba 4, row 11
    PREPARACAO_2("Preparação 2"),
    // Ref: Aba 4, row 12
    USINA_1("Usina 1"),
    // Ref: Aba 4, row 13
    USINA_2("Usina 2"),
    // Ref: Aba 4, row 14
    USINA_3("Usina 3"),
    // Ref: Aba 4, row 15
    USINA_4("Usina 4"),
    // Ref: Aba 4, row 16
    VENDAS("Vendas"),
    // Ref: Aba 4, row 17
    PRODUCAO_PSC_PSM("Produção PSC + PSM"),
    // Ref: Aba 4, row 18
    PELLET_FEED("Pellet Feed (embarque)");

    private final String label;

    AreaProducao(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static AreaProducao fromLabel(String label) {
        for (AreaProducao area : values()) {
            if (area.label.equalsIgnoreCase(label.trim())) {
                return area;
            }
        }
        throw new IllegalArgumentException("Área de produção inválida: " + label);
    }
}
