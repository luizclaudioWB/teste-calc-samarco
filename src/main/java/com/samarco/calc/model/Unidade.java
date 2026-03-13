package com.samarco.calc.model;

/**
 * Unidades consumidoras/geradoras da Samarco.
 * Ref: Aba "10Demanda - INPUT" e "2Tabela Tarifas Distribuidoras"
 */
public enum Unidade {
    GERMANO("Germano", "MG"),
    MATIPO("Matipó", "MG"),
    UBU("Ubu", "ES");

    private final String label;
    private final String estado;

    Unidade(String label, String estado) {
        this.label = label;
        this.estado = estado;
    }

    public String getLabel() {
        return label;
    }

    public String getEstado() {
        return estado;
    }

    public static Unidade fromLabel(String label) {
        for (Unidade u : values()) {
            if (u.label.equalsIgnoreCase(label.trim())) {
                return u;
            }
        }
        throw new IllegalArgumentException("Unidade inválida: " + label);
    }
}
