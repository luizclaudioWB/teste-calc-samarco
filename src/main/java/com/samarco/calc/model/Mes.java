package com.samarco.calc.model;

public enum Mes {
    JAN(1, "Jan"),
    FEV(2, "Fev"),
    MAR(3, "Mar"),
    ABR(4, "Abr"),
    MAI(5, "Mai"),
    JUN(6, "Jun"),
    JUL(7, "Jul"),
    AGO(8, "Ago"),
    SET(9, "Set"),
    OUT(10, "Out"),
    NOV(11, "Nov"),
    DEZ(12, "Dez");

    private final int numero;
    private final String label;

    Mes(int numero, String label) {
        this.numero = numero;
        this.label = label;
    }

    public int getNumero() {
        return numero;
    }

    public String getLabel() {
        return label;
    }

    public static Mes fromLabel(String label) {
        for (Mes mes : values()) {
            if (mes.label.equalsIgnoreCase(label)) {
                return mes;
            }
        }
        throw new IllegalArgumentException("Mês inválido: " + label);
    }

    public static Mes fromNumero(int numero) {
        for (Mes mes : values()) {
            if (mes.numero == numero) {
                return mes;
            }
        }
        throw new IllegalArgumentException("Número de mês inválido: " + numero);
    }
}
