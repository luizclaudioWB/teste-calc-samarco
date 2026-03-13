package com.samarco.calc.service;

import com.samarco.calc.input.DemandaContratadaInput;
import com.samarco.calc.input.TarifasDistribuidorasInput;
import com.samarco.calc.model.AreaConsumoEspecifico;
import com.samarco.calc.model.ClasseCustoResult;
import com.samarco.calc.model.ConsumoAreaResult;
import com.samarco.calc.model.DistribuicaoCargaResult;
import com.samarco.calc.model.GeracaoPropriaResult;
import com.samarco.calc.model.Mes;
import com.samarco.calc.model.Unidade;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

/**
 * Cálculo — Distribuição de Classe de Custo por Estado.
 * Ref: Aba "14Distrib de Classe de Custo"
 *
 * Distribui custos em 3 classes por estado:
 * - 50610002 (Consumo de Energia) = quantidade comprada × PMIX
 * - 50610003 (Uso de Rede) = demanda × tarifa TUSD FIO
 * - 50610006 (Encargo) = consumo MWh × tarifa TUSD ENCARGO (líquido de PROINFA)
 */
@ApplicationScoped
public class ClasseCustoCalculator {

    private static final int SCALE = 15;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal KWH_PARA_MWH = new BigDecimal("1000");

    /**
     * Calcula distribuição de classe de custo por estado.
     */
    public ClasseCustoResult calcular(
            TarifasDistribuidorasInput tarifas,
            DemandaContratadaInput demanda,
            ConsumoAreaResult consumoArea,
            GeracaoPropriaResult geracaoPropria,
            DistribuicaoCargaResult distribuicaoCarga) {

        if (tarifas == null || demanda == null || consumoArea == null
                || geracaoPropria == null || distribuicaoCarga == null) {
            throw new IllegalArgumentException("Inputs não podem ser nulos");
        }

        Map<Mes, BigDecimal> devolProinfaMG = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> tusdFioGermano = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> tusdFioMatipo = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> totalTusdFioMG = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> tusdEncGermano = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> tusdEncMatipo = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> totalTusdEncMG = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> consumoEneMG = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> totalConsMG = new EnumMap<>(Mes.class);

        Map<Mes, BigDecimal> devolProinfaES = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> tusdFioUbu = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> totalTusdFioES = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> tusdEncUbu = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> totalTusdEncES = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> consumoEneES = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> totalConsES = new EnumMap<>(Mes.class);

        // Áreas conectadas a cada subestação — para cálculo de encargo
        // Germano: rows 3-7 (Filtragem, Mineração, Benef1, Benef2, Benef3)
        AreaConsumoEspecifico[] areasGermano = {
                AreaConsumoEspecifico.FILTRAGEM_GERMANO,
                AreaConsumoEspecifico.MINERACAO_1,
                AreaConsumoEspecifico.BENEFICIAMENTO_1,
                AreaConsumoEspecifico.BENEFICIAMENTO_2,
                AreaConsumoEspecifico.BENEFICIAMENTO_3
        };
        // Matipó: rows 8-10 (Mineroduto 1, 2, 3)
        AreaConsumoEspecifico[] areasMatipo = {
                AreaConsumoEspecifico.MINERODUTO_1,
                AreaConsumoEspecifico.MINERODUTO_2,
                AreaConsumoEspecifico.MINERODUTO_3
        };
        // UBU: rows 11-17 (Preparação 1, 2, Usina 1, 2, 3, 4, Estocagem)
        AreaConsumoEspecifico[] areasUbu = {
                AreaConsumoEspecifico.PREPARACAO_1,
                AreaConsumoEspecifico.PREPARACAO_2,
                AreaConsumoEspecifico.USINA_1,
                AreaConsumoEspecifico.USINA_2,
                AreaConsumoEspecifico.USINA_3,
                AreaConsumoEspecifico.USINA_4,
                AreaConsumoEspecifico.ESTOCAGEM
        };

        for (Mes mes : Mes.values()) {
            // === MG ===

            // Ref: Aba 14, row 4 — Devoluç PROINFA = Guilman × Desconto Germano
            BigDecimal guilmanMWh = BigDecimal.valueOf(geracaoPropria.guilmanMWh().get(mes));
            BigDecimal descontoGermano = tarifas.descontoAutoProducao().get(Unidade.GERMANO).get(mes);
            BigDecimal devolMG = guilmanMWh.multiply(descontoGermano);
            devolProinfaMG.put(mes, devolMG);

            // Ref: Aba 14, row 5 — TUSD FIO Germano = demandaPonta × tarifaPonta + demandaFP × tarifaFP
            BigDecimal fioGermano = calcularTusdFio(demanda, tarifas, Unidade.GERMANO, mes);
            tusdFioGermano.put(mes, fioGermano);

            // Ref: Aba 14, row 6 — TUSD FIO Matipó
            BigDecimal fioMatipo = calcularTusdFio(demanda, tarifas, Unidade.MATIPO, mes);
            tusdFioMatipo.put(mes, fioMatipo);

            // Ref: Aba 14, row 7 — Total = Germano + Matipó
            totalTusdFioMG.put(mes, fioGermano.add(fioMatipo));

            // Ref: Aba 14, row 8 — TUSD Encargo Germano = SUM(kWh rows 3-7)/1000 × tarifa
            BigDecimal consumoMWhGermano = somaConsumoMWh(consumoArea, areasGermano, mes);
            BigDecimal tarifaEncGermano = tarifas.tusdEncargo().get(Unidade.GERMANO).get(mes);
            BigDecimal encGermano = consumoMWhGermano.multiply(tarifaEncGermano);
            tusdEncGermano.put(mes, encGermano);

            // Ref: Aba 14, row 9 — TUSD Encargo Matipó = SUM(kWh rows 8-10)/1000 × tarifa
            BigDecimal consumoMWhMatipo = somaConsumoMWh(consumoArea, areasMatipo, mes);
            BigDecimal tarifaEncMatipo = tarifas.tusdEncargo().get(Unidade.MATIPO).get(mes);
            BigDecimal encMatipo = consumoMWhMatipo.multiply(tarifaEncMatipo);
            tusdEncMatipo.put(mes, encMatipo);

            // Ref: Aba 14, row 10 — Total Encargo MG = MAX(0, Germano - PROINFA) + Matipó
            BigDecimal encGermanoLiquido = encGermano.subtract(devolMG).max(BigDecimal.ZERO);
            totalTusdEncMG.put(mes, encGermanoLiquido.add(encMatipo));

            // Ref: Aba 14, row 11 — Consumo = qtdeCompradaMG × pmixMG
            BigDecimal qtdeCompraMG = BigDecimal.valueOf(distribuicaoCarga.qtdeCompradaMG().get(mes));
            BigDecimal pmixMG = tarifas.pmixMG().get(mes);
            BigDecimal consMG = qtdeCompraMG.multiply(pmixMG);
            consumoEneMG.put(mes, consMG);
            totalConsMG.put(mes, consMG);

            // === ES ===

            // Ref: Aba 14, row 15 — Devoluç PROINFA = MunizFreire × Desconto UBU
            BigDecimal munizMWh = BigDecimal.valueOf(geracaoPropria.munizFreireMWh().get(mes));
            BigDecimal descontoUbu = tarifas.descontoAutoProducao().get(Unidade.UBU).get(mes);
            BigDecimal devolES = munizMWh.multiply(descontoUbu);
            devolProinfaES.put(mes, devolES);

            // Ref: Aba 14, row 16 — TUSD FIO UBU
            BigDecimal fioUbu = calcularTusdFio(demanda, tarifas, Unidade.UBU, mes);
            tusdFioUbu.put(mes, fioUbu);
            totalTusdFioES.put(mes, fioUbu);

            // Ref: Aba 14, row 18 — TUSD Encargo UBU = SUM(kWh rows 11-17)/1000 × tarifa
            BigDecimal consumoMWhUbu = somaConsumoMWh(consumoArea, areasUbu, mes);
            BigDecimal tarifaEncUbu = tarifas.tusdEncargo().get(Unidade.UBU).get(mes);
            BigDecimal encUbu = consumoMWhUbu.multiply(tarifaEncUbu);
            tusdEncUbu.put(mes, encUbu);

            // Ref: Aba 14, row 19 — Total Encargo ES = MAX(0, UBU - PROINFA)
            BigDecimal encUbuLiquido = encUbu.subtract(devolES).max(BigDecimal.ZERO);
            totalTusdEncES.put(mes, encUbuLiquido);

            // Ref: Aba 14, row 20 — Consumo = qtdeCompradaES × pmixES
            BigDecimal qtdeCompraES = BigDecimal.valueOf(distribuicaoCarga.qtdeCompradaES().get(mes));
            BigDecimal pmixES = tarifas.pmixES().get(mes);
            BigDecimal consES = qtdeCompraES.multiply(pmixES);
            consumoEneES.put(mes, consES);
            totalConsES.put(mes, consES);
        }

        return new ClasseCustoResult(
                devolProinfaMG, tusdFioGermano, tusdFioMatipo, totalTusdFioMG,
                tusdEncGermano, tusdEncMatipo, totalTusdEncMG, consumoEneMG, totalConsMG,
                devolProinfaES, tusdFioUbu, totalTusdFioES,
                tusdEncUbu, totalTusdEncES, consumoEneES, totalConsES
        );
    }

    /**
     * Calcula TUSD FIO/DEM = demandaPonta × tarifaPonta + demandaFP × tarifaFP
     */
    private BigDecimal calcularTusdFio(DemandaContratadaInput demanda,
                                        TarifasDistribuidorasInput tarifas,
                                        Unidade unidade, Mes mes) {
        BigDecimal demPonta = BigDecimal.valueOf(demanda.demandaPonta().get(unidade).get(mes));
        BigDecimal demFP = BigDecimal.valueOf(demanda.demandaForaPonta().get(unidade).get(mes));
        BigDecimal tarPonta = tarifas.tusdFioPonta().get(unidade).get(mes);
        BigDecimal tarFP = tarifas.tusdFioForaPonta().get(unidade).get(mes);
        return demPonta.multiply(tarPonta).add(demFP.multiply(tarFP));
    }

    /**
     * Soma consumo kWh das áreas e converte para MWh.
     * Ref: Aba 14, ex: SUM('7Consumo Área - CALC'!C3:C7)/1000
     */
    private BigDecimal somaConsumoMWh(ConsumoAreaResult consumoArea,
                                       AreaConsumoEspecifico[] areas, Mes mes) {
        double somaKWh = 0;
        for (AreaConsumoEspecifico area : areas) {
            Map<Mes, Double> valores = consumoArea.consumoKWh().get(area);
            if (valores != null && valores.get(mes) != null) {
                somaKWh += valores.get(mes);
            }
        }
        return BigDecimal.valueOf(somaKWh).divide(KWH_PARA_MWH, SCALE, ROUNDING);
    }
}
