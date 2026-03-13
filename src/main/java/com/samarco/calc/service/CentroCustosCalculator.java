package com.samarco.calc.service;

import com.samarco.calc.model.AreaConsumoEspecifico;
import com.samarco.calc.model.CentroCustosResult;
import com.samarco.calc.model.ClasseCustoResult;
import com.samarco.calc.model.ConsumoAreaResult;
import com.samarco.calc.model.Mes;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Cálculo — Distribuição por Centro de Custos.
 * Ref: Aba "15Distrib Centro de Custos"
 *
 * Distribui os totais de Aba 14 por centro de custo usando os % da Aba 7 (rows 38-52).
 * Para cada CC: valor = total_classe_estado × percentual_CC
 */
@ApplicationScoped
public class CentroCustosCalculator {

    // Centros de custo MG — Ref: Aba 7, rows 38-45
    private static final AreaConsumoEspecifico[] CCS_MG = {
            AreaConsumoEspecifico.FILTRAGEM_GERMANO,
            AreaConsumoEspecifico.MINERACAO_1,
            AreaConsumoEspecifico.BENEFICIAMENTO_1,
            AreaConsumoEspecifico.BENEFICIAMENTO_2,
            AreaConsumoEspecifico.BENEFICIAMENTO_3,
            AreaConsumoEspecifico.MINERODUTO_1,
            AreaConsumoEspecifico.MINERODUTO_2,
            AreaConsumoEspecifico.MINERODUTO_3
    };

    // Centros de custo ES — Ref: Aba 7, rows 46-52
    private static final AreaConsumoEspecifico[] CCS_ES = {
            AreaConsumoEspecifico.PREPARACAO_1,
            AreaConsumoEspecifico.PREPARACAO_2,
            AreaConsumoEspecifico.USINA_1,
            AreaConsumoEspecifico.USINA_2,
            AreaConsumoEspecifico.USINA_3,
            AreaConsumoEspecifico.USINA_4,
            AreaConsumoEspecifico.ESTOCAGEM
    };

    /**
     * Calcula distribuição por centro de custos.
     *
     * @param classeCusto resultado da Aba 14 (totais por classe/estado)
     * @param consumoArea resultado da Aba 7 (percentuais por CC)
     * @return custos distribuídos por centro de custo
     */
    public CentroCustosResult calcular(ClasseCustoResult classeCusto,
                                        ConsumoAreaResult consumoArea) {
        if (classeCusto == null || consumoArea == null) {
            throw new IllegalArgumentException("Inputs não podem ser nulos");
        }

        // MG
        Map<AreaConsumoEspecifico, Map<Mes, BigDecimal>> consumoMG = new EnumMap<>(AreaConsumoEspecifico.class);
        Map<AreaConsumoEspecifico, Map<Mes, BigDecimal>> usoRedeMG = new EnumMap<>(AreaConsumoEspecifico.class);
        Map<AreaConsumoEspecifico, Map<Mes, BigDecimal>> encargoMG = new EnumMap<>(AreaConsumoEspecifico.class);

        for (AreaConsumoEspecifico cc : CCS_MG) {
            Map<Mes, BigDecimal> consumoCC = new EnumMap<>(Mes.class);
            Map<Mes, BigDecimal> usoRedeCC = new EnumMap<>(Mes.class);
            Map<Mes, BigDecimal> encargoCC = new EnumMap<>(Mes.class);

            for (Mes mes : Mes.values()) {
                BigDecimal pct = BigDecimal.valueOf(
                        consumoArea.percentualPorCentroCustoMG().get(cc).get(mes));

                // Ref: Aba 15 — Consumo = Aba14!C12 × Aba7!C{row}
                consumoCC.put(mes, classeCusto.totalConsumoMG().get(mes).multiply(pct));
                // Ref: Aba 15 — Uso de Rede = Aba14!C7 × Aba7!C{row}
                usoRedeCC.put(mes, classeCusto.totalTusdFioMG().get(mes).multiply(pct));
                // Ref: Aba 15 — Encargo = Aba14!C10 × Aba7!C{row}
                encargoCC.put(mes, classeCusto.totalTusdEncargoMG().get(mes).multiply(pct));
            }

            consumoMG.put(cc, consumoCC);
            usoRedeMG.put(cc, usoRedeCC);
            encargoMG.put(cc, encargoCC);
        }

        // ES
        Map<AreaConsumoEspecifico, Map<Mes, BigDecimal>> consumoES = new EnumMap<>(AreaConsumoEspecifico.class);
        Map<AreaConsumoEspecifico, Map<Mes, BigDecimal>> usoRedeES = new EnumMap<>(AreaConsumoEspecifico.class);
        Map<AreaConsumoEspecifico, Map<Mes, BigDecimal>> encargoES = new EnumMap<>(AreaConsumoEspecifico.class);

        for (AreaConsumoEspecifico cc : CCS_ES) {
            Map<Mes, BigDecimal> consumoCC = new EnumMap<>(Mes.class);
            Map<Mes, BigDecimal> usoRedeCC = new EnumMap<>(Mes.class);
            Map<Mes, BigDecimal> encargoCC = new EnumMap<>(Mes.class);

            for (Mes mes : Mes.values()) {
                BigDecimal pct = BigDecimal.valueOf(
                        consumoArea.percentualPorCentroCustoES().get(cc).get(mes));

                // Ref: Aba 15 — Consumo = Aba14!C21 × Aba7!C{row}
                consumoCC.put(mes, classeCusto.totalConsumoES().get(mes).multiply(pct));
                // Ref: Aba 15 — Uso de Rede = Aba14!C17 × Aba7!C{row}
                usoRedeCC.put(mes, classeCusto.totalTusdFioES().get(mes).multiply(pct));
                // Ref: Aba 15 — Encargo = Aba14!C19 × Aba7!C{row}
                encargoCC.put(mes, classeCusto.totalTusdEncargoES().get(mes).multiply(pct));
            }

            consumoES.put(cc, consumoCC);
            usoRedeES.put(cc, usoRedeCC);
            encargoES.put(cc, encargoCC);
        }

        // Totais Samarco (MG + ES)
        Map<Mes, BigDecimal> totalConsumo = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> totalUsoRede = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> totalEncargo = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> totalGeral = new EnumMap<>(Mes.class);

        for (Mes mes : Mes.values()) {
            BigDecimal sumConsumoMG = sumCC(consumoMG, CCS_MG, mes);
            BigDecimal sumConsumoES = sumCC(consumoES, CCS_ES, mes);
            BigDecimal tConsumo = sumConsumoMG.add(sumConsumoES);
            totalConsumo.put(mes, tConsumo);

            BigDecimal sumUsoRedeMG = sumCC(usoRedeMG, CCS_MG, mes);
            BigDecimal sumUsoRedeES = sumCC(usoRedeES, CCS_ES, mes);
            BigDecimal tUsoRede = sumUsoRedeMG.add(sumUsoRedeES);
            totalUsoRede.put(mes, tUsoRede);

            BigDecimal sumEncargoMG = sumCC(encargoMG, CCS_MG, mes);
            BigDecimal sumEncargoES = sumCC(encargoES, CCS_ES, mes);
            BigDecimal tEncargo = sumEncargoMG.add(sumEncargoES);
            totalEncargo.put(mes, tEncargo);

            totalGeral.put(mes, tConsumo.add(tUsoRede).add(tEncargo));
        }

        return new CentroCustosResult(
                consumoMG, usoRedeMG, encargoMG,
                consumoES, usoRedeES, encargoES,
                totalConsumo, totalUsoRede, totalEncargo, totalGeral
        );
    }

    private BigDecimal sumCC(Map<AreaConsumoEspecifico, Map<Mes, BigDecimal>> map,
                              AreaConsumoEspecifico[] ccs, Mes mes) {
        BigDecimal sum = BigDecimal.ZERO;
        for (AreaConsumoEspecifico cc : ccs) {
            sum = sum.add(map.get(cc).get(mes));
        }
        return sum;
    }
}
