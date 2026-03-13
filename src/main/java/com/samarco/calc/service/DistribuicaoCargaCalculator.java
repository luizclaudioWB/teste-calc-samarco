package com.samarco.calc.service;

import com.samarco.calc.model.ConsumoAreaResult;
import com.samarco.calc.model.DistribuicaoCargaResult;
import com.samarco.calc.model.GeracaoPropriaResult;
import com.samarco.calc.model.Mes;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cálculo — Distribuição de Carga por Estado.
 * Ref: Aba "13Distribuição de Carga"
 *
 * Calcula necessidade de energia e compra por estado (MG/ES),
 * considerando perdas de 3% e geração própria.
 */
@ApplicationScoped
public class DistribuicaoCargaCalculator {

    // Ref: Aba 13, row 4 / row 15 — percentual de perda fixo
    private static final double PERCENTUAL_PERDA = 0.03;

    /**
     * Calcula distribuição de carga.
     *
     * @param consumoArea    resultado do Cálculo 03 (consumo MG/ES em MWh)
     * @param geracaoPropria resultado do Cálculo 04 (geração por usina)
     * @return distribuição de carga por estado
     */
    public DistribuicaoCargaResult calcular(ConsumoAreaResult consumoArea,
                                             GeracaoPropriaResult geracaoPropria) {
        if (consumoArea == null || geracaoPropria == null) {
            throw new IllegalArgumentException("Inputs não podem ser nulos");
        }

        Map<Mes, Double> consumoMG = new EnumMap<>(Mes.class);
        Map<Mes, Double> geracaoGuilman = new EnumMap<>(Mes.class);
        Map<Mes, Double> perdasMG = new EnumMap<>(Mes.class);
        Map<Mes, Double> necTotalMG = new EnumMap<>(Mes.class);
        Map<Mes, Double> necCompraMG = new EnumMap<>(Mes.class);
        Map<Mes, Double> qtdeCompraMG = new EnumMap<>(Mes.class);

        Map<Mes, Double> consumoES = new EnumMap<>(Mes.class);
        Map<Mes, Double> geracaoMuniz = new EnumMap<>(Mes.class);
        Map<Mes, Double> perdasES = new EnumMap<>(Mes.class);
        Map<Mes, Double> necTotalES = new EnumMap<>(Mes.class);
        Map<Mes, Double> necCompraES = new EnumMap<>(Mes.class);
        Map<Mes, Double> qtdeCompraES = new EnumMap<>(Mes.class);

        Map<Mes, Double> consumoTotal = new EnumMap<>(Mes.class);
        Map<Mes, Double> geracaoTotal = new EnumMap<>(Mes.class);
        Map<Mes, Double> perdasTotal = new EnumMap<>(Mes.class);
        Map<Mes, Double> necTotalSamarco = new EnumMap<>(Mes.class);
        Map<Mes, Double> necCompraSamarco = new EnumMap<>(Mes.class);
        Map<Mes, Double> qtdeCompraSamarco = new EnumMap<>(Mes.class);

        for (Mes mes : Mes.values()) {
            // --- MG ---
            // Ref: Aba 13, row 5 = '7Consumo Área - CALC'!C31
            double cMG = consumoArea.consumoMG_MWh().get(mes);
            consumoMG.put(mes, cMG);

            // Ref: Aba 13, row 6 = '9Geração Própria - CALC'!C15
            double gGuilman = geracaoPropria.guilmanMWh().get(mes);
            geracaoGuilman.put(mes, gGuilman);

            // Ref: Aba 13, row 7 = C5 * C4
            double pMG = cMG * PERCENTUAL_PERDA;
            perdasMG.put(mes, pMG);

            // Ref: Aba 13, row 9 = C5 + C7 - C8 (Proinfa = 0)
            double ntMG = cMG + pMG;
            necTotalMG.put(mes, ntMG);

            // Ref: Aba 13, row 10 = IF(C9 - C6 < 0, , C9 - C6)
            double ncMG = Math.max(0, ntMG - gGuilman);
            necCompraMG.put(mes, ncMG);

            // Ref: Aba 13, row 11 = row 10
            qtdeCompraMG.put(mes, ncMG);

            // --- ES ---
            // Ref: Aba 13, row 16 = '7Consumo Área - CALC'!C32
            double cES = consumoArea.consumoES_MWh().get(mes);
            consumoES.put(mes, cES);

            // Ref: Aba 13, row 17 = '9Geração Própria - CALC'!C17
            double gMuniz = geracaoPropria.munizFreireMWh().get(mes);
            geracaoMuniz.put(mes, gMuniz);

            // Ref: Aba 13, row 18 = C16 * C15
            double pES = cES * PERCENTUAL_PERDA;
            perdasES.put(mes, pES);

            // Ref: Aba 13, row 20 = C16 + C18 (sem Proinfa)
            double ntES = cES + pES;
            necTotalES.put(mes, ntES);

            // Ref: Aba 13, row 21 = C20 - C17
            double ncES = ntES - gMuniz;
            necCompraES.put(mes, ncES);

            // Ref: Aba 13, row 22 = row 21
            qtdeCompraES.put(mes, ncES);

            // --- Samarco Total ---
            // Ref: Aba 13, row 26 = C16 + C5
            consumoTotal.put(mes, cMG + cES);
            // Ref: Aba 13, row 27 = C17 + C6
            geracaoTotal.put(mes, gGuilman + gMuniz);
            // Ref: Aba 13, row 28 = C18 + C7
            perdasTotal.put(mes, pMG + pES);
            // Ref: Aba 13, row 30 = C20 + C9
            necTotalSamarco.put(mes, ntMG + ntES);
            // Ref: Aba 13, row 31 = C21 + C10
            necCompraSamarco.put(mes, ncMG + ncES);
            // Ref: Aba 13, row 32 = C22 + C11
            qtdeCompraSamarco.put(mes, ncMG + ncES);
        }

        return new DistribuicaoCargaResult(
                consumoMG, geracaoGuilman, perdasMG, necTotalMG, necCompraMG, qtdeCompraMG,
                consumoES, geracaoMuniz, perdasES, necTotalES, necCompraES, qtdeCompraES,
                consumoTotal, geracaoTotal, perdasTotal, necTotalSamarco, necCompraSamarco, qtdeCompraSamarco
        );
    }
}
