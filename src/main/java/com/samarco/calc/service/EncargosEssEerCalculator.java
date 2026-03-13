package com.samarco.calc.service;

import com.samarco.calc.input.TarifasEncargosInput;
import com.samarco.calc.model.ConsumoAreaResult;
import com.samarco.calc.model.EncargosResult;
import com.samarco.calc.model.GeracaoPropriaResult;
import com.samarco.calc.model.Mes;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

/**
 * Cálculo 05 — Encargos ESS/EER.
 * Calcula encargos nacionais e distribui por estado (MG/ES).
 * Ref: Aba "12Encargos ESS EER - CALC"
 *
 * IMPORTANTE: Todas as tarifas variam por mês.
 * Todos os valores monetários em BigDecimal com HALF_UP, máxima precisão intermediária.
 */
@ApplicationScoped
public class EncargosEssEerCalculator {

    private static final int SCALE_INTERMEDIARIO = 15;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Calcula encargos ESS/EER.
     *
     * @param tarifas        tarifas mensais de ERR, ERCAP e ESS
     * @param consumoArea    resultado do Cálculo 03 (consumo área com totais e percentuais)
     * @param geracaoPropria resultado do Cálculo 04 (geração própria com total MWh)
     * @return encargos nacionais e por estado
     */
    public EncargosResult calcular(TarifasEncargosInput tarifas,
                                    ConsumoAreaResult consumoArea,
                                    GeracaoPropriaResult geracaoPropria) {
        if (tarifas == null || consumoArea == null || geracaoPropria == null) {
            throw new IllegalArgumentException("Inputs não podem ser nulos");
        }

        Map<Mes, BigDecimal> totalErrErcapTarifa = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> consumoSamarcoMWh = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> valorTotalEer = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> valorTotalEss = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> mgEer = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> mgEss = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> mgTotal = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> esEer = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> esEss = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> esTotal = new EnumMap<>(Mes.class);

        for (Mes mes : Mes.values()) {
            BigDecimal err = tarifas.errTarifa().get(mes);
            BigDecimal ercap = tarifas.ercapTarifa().get(mes);
            BigDecimal ess = tarifas.essTarifa().get(mes);

            if (err == null || ercap == null || ess == null) {
                throw new IllegalArgumentException("Tarifa faltando para mês " + mes.getLabel());
            }

            // Ref: Aba 12, row 4 = SUM(C2:C3)
            BigDecimal totalTarifa = err.add(ercap);
            totalErrErcapTarifa.put(mes, totalTarifa);

            // Ref: Aba 12, row 5 = '7Consumo Área - CALC'!C28 - '9Geração Própria - CALC'!C19
            Double consumoAreaTotal = consumoArea.totalMWh().get(mes);
            Double geracaoTotal = geracaoPropria.totalMWh().get(mes);

            if (consumoAreaTotal == null || geracaoTotal == null) {
                throw new IllegalArgumentException("Consumo ou geração faltando para mês " + mes.getLabel());
            }

            BigDecimal consumo = BigDecimal.valueOf(consumoAreaTotal)
                    .subtract(BigDecimal.valueOf(geracaoTotal));
            consumoSamarcoMWh.put(mes, consumo);

            // Ref: Aba 12, row 6 = C5 * C4
            BigDecimal vEer = consumo.multiply(totalTarifa);
            valorTotalEer.put(mes, vEer);

            // Ref: Aba 12, row 8 = C7 * C5
            BigDecimal vEss = ess.multiply(consumo);
            valorTotalEss.put(mes, vEss);

            // Distribuição por estado
            Double pctMG = consumoArea.percentualMG().get(mes);
            Double pctES = consumoArea.percentualES().get(mes);

            if (pctMG == null || pctES == null) {
                throw new IllegalArgumentException("Percentual estado faltando para mês " + mes.getLabel());
            }

            BigDecimal percentMG = BigDecimal.valueOf(pctMG);
            BigDecimal percentES = BigDecimal.valueOf(pctES);

            // Ref: Aba 12, row 11 = C6 * '7Consumo Área - CALC'!C33
            BigDecimal mgEerVal = vEer.multiply(percentMG);
            mgEer.put(mes, mgEerVal);

            // Ref: Aba 12, row 12 = C8 * '7Consumo Área - CALC'!C33
            BigDecimal mgEssVal = vEss.multiply(percentMG);
            mgEss.put(mes, mgEssVal);

            // Ref: Aba 12, row 13 = D11 + D12
            mgTotal.put(mes, mgEerVal.add(mgEssVal));

            // Ref: Aba 12, row 14 = C6 * '7Consumo Área - CALC'!C34
            BigDecimal esEerVal = vEer.multiply(percentES);
            esEer.put(mes, esEerVal);

            // Ref: Aba 12, row 15 = C8 * '7Consumo Área - CALC'!C34
            BigDecimal esEssVal = vEss.multiply(percentES);
            esEss.put(mes, esEssVal);

            // Ref: Aba 12, row 16 = D14 + D15
            esTotal.put(mes, esEerVal.add(esEssVal));
        }

        return new EncargosResult(
                totalErrErcapTarifa, consumoSamarcoMWh,
                valorTotalEer, valorTotalEss,
                mgEer, mgEss, mgTotal,
                esEer, esEss, esTotal
        );
    }
}
