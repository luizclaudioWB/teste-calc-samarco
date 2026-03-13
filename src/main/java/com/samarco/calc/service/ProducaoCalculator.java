package com.samarco.calc.service;

import com.samarco.calc.input.PlanejamentoProducaoInput;
import com.samarco.calc.model.AreaProducao;
import com.samarco.calc.model.Mes;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cálculo 01 — Produção (tms).
 * Converte produção de ktms → tms multiplicando pelo multiplicador.
 * Ref: Aba "4Produção - OCULTA"
 * Fórmula: Producao_tms[área][mês] = PlanejamentoProducaoInput[área][mês] × MULTIPLICADOR
 */
@ApplicationScoped
public class ProducaoCalculator {

    // Ref: Aba 4, célula B20
    private static final double MULTIPLICADOR_KTMS_PARA_TMS = 1000.0;

    /**
     * Calcula produção em tms a partir de ktms.
     *
     * @param input produção planejada em ktms
     * @return produção em tms por área e mês
     */
    public Map<AreaProducao, Map<Mes, Double>> calcular(PlanejamentoProducaoInput input) {
        if (input == null) {
            throw new IllegalArgumentException("Input de produção não pode ser nulo");
        }

        Map<AreaProducao, Map<Mes, Double>> resultado = new EnumMap<>(AreaProducao.class);

        for (Map.Entry<AreaProducao, Map<Mes, Double>> entry : input.producaoKtms().entrySet()) {
            AreaProducao area = entry.getKey();
            Map<Mes, Double> valoresMes = entry.getValue();

            Map<Mes, Double> producaoTms = new EnumMap<>(Mes.class);
            for (Map.Entry<Mes, Double> mesEntry : valoresMes.entrySet()) {
                Mes mes = mesEntry.getKey();
                Double valorKtms = mesEntry.getValue();

                if (valorKtms == null) {
                    throw new IllegalArgumentException(
                            "Valor nulo para área " + area.getLabel() + " mês " + mes.getLabel());
                }
                if (valorKtms < 0) {
                    throw new IllegalArgumentException(
                            "Valor negativo para área " + area.getLabel() + " mês " + mes.getLabel() + ": " + valorKtms);
                }

                // Ref: Aba 4, B3 = '3Planejamento Produção - INPUT'!B4 * $B$20
                producaoTms.put(mes, valorKtms * MULTIPLICADOR_KTMS_PARA_TMS);
            }

            resultado.put(area, producaoTms);
        }

        return resultado;
    }
}
