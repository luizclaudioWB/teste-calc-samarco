package com.samarco.calc.service;

import com.samarco.calc.input.CalendarioInput;
import com.samarco.calc.input.PlanejamentoGeracaoInput;
import com.samarco.calc.model.Calendario;
import com.samarco.calc.model.GeracaoPropriaResult;
import com.samarco.calc.model.Mes;
import com.samarco.calc.util.CalendarioUtil;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cálculo 04 — Geração Própria.
 * Calcula geração das usinas UHE Guilman Amorim e PCH Muniz Freire.
 * Ref: Aba "9Geração Própria - CALC"
 */
@ApplicationScoped
public class GeracaoPropriaCalculator {

    /**
     * Calcula geração própria para todos os meses.
     *
     * @param geracaoInput    MWh planejados por usina — Ref: Aba 8
     * @param calendarioInput dias no mês e dias não úteis — Ref: Aba 9, rows 6-7
     * @return resultado com MWh, MWmédios e acumulados
     */
    public GeracaoPropriaResult calcular(PlanejamentoGeracaoInput geracaoInput,
                                          CalendarioInput calendarioInput) {
        if (geracaoInput == null) {
            throw new IllegalArgumentException("Input de geração não pode ser nulo");
        }
        if (calendarioInput == null) {
            throw new IllegalArgumentException("Input de calendário não pode ser nulo");
        }

        Map<Mes, Calendario> calendario = new EnumMap<>(Mes.class);
        Map<Mes, Double> guilmanMWh = new EnumMap<>(Mes.class);
        Map<Mes, Double> guilmanMWmedios = new EnumMap<>(Mes.class);
        Map<Mes, Double> munizFreireMWh = new EnumMap<>(Mes.class);
        Map<Mes, Double> munizFreireMWmedios = new EnumMap<>(Mes.class);
        Map<Mes, Double> totalMWh = new EnumMap<>(Mes.class);
        Map<Mes, Double> totalMWmedios = new EnumMap<>(Mes.class);

        for (Mes mes : Mes.values()) {
            Integer diasNoMes = calendarioInput.diasNoMes().get(mes);
            Integer diasNaoUteis = calendarioInput.diasNaoUteis().get(mes);

            if (diasNoMes == null || diasNaoUteis == null) {
                throw new IllegalArgumentException("Dados de calendário faltando para mês " + mes.getLabel());
            }

            // Ref: Aba 9, rows 6-11
            Calendario cal = CalendarioUtil.calcular(diasNoMes, diasNaoUteis);
            calendario.put(mes, cal);

            // Ref: Aba 9, row 15 = '8Planejamento Geração - INPUT'!B2
            Double guilmanMWhValor = geracaoInput.guilmanAmorimMWh().get(mes);
            if (guilmanMWhValor == null) {
                throw new IllegalArgumentException("Guilman MWh faltando para mês " + mes.getLabel());
            }
            guilmanMWh.put(mes, guilmanMWhValor);

            // Ref: Aba 9, row 16 = C15 / B11
            guilmanMWmedios.put(mes, guilmanMWhValor / cal.totalHoras());

            // Ref: Aba 9, row 17 = '8Planejamento Geração - INPUT'!B3
            Double munizMWhValor = geracaoInput.munizFreireMWh().get(mes);
            if (munizMWhValor == null) {
                throw new IllegalArgumentException("Muniz Freire MWh faltando para mês " + mes.getLabel());
            }
            munizFreireMWh.put(mes, munizMWhValor);

            // Ref: Aba 9, row 18 = C17 / B11
            munizFreireMWmedios.put(mes, munizMWhValor / cal.totalHoras());

            // Ref: Aba 9, row 19 = C15 + C17
            double total = guilmanMWhValor + munizMWhValor;
            totalMWh.put(mes, total);

            // Ref: Aba 9, row 20 = C19 / B11
            totalMWmedios.put(mes, total / cal.totalHoras());
        }

        return new GeracaoPropriaResult(
                calendario, guilmanMWh, guilmanMWmedios,
                munizFreireMWh, munizFreireMWmedios,
                totalMWh, totalMWmedios
        );
    }
}
