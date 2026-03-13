package com.samarco.calc.service;

import com.samarco.calc.model.AreaConsumoEspecifico;
import com.samarco.calc.model.AreaProducao;
import com.samarco.calc.model.ConsumoAreaResult;
import com.samarco.calc.model.GrupoConsumo;
import com.samarco.calc.model.Mes;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cálculo 03 — Consumo Área.
 * Calcula consumo por centro de custo: ConsumoEspecifico × Produção.
 * Ref: Aba "7Consumo Área - CALC"
 *
 * ATENÇÃO: O mapeamento entre áreas de consumo e produção NÃO é 1:1.
 * Algumas áreas de consumo referenciam linhas de produção com offset,
 * e Mineração agrega 3 linhas de produção.
 * Beneficiamento 1 e Mineroduto 1 têm valores fixos (hardcoded no Excel).
 */
@ApplicationScoped
public class ConsumoAreaCalculator {

    // Ref: Aba 7, row 5 — valor fixo mensal para Beneficiamento 1
    private static final double BENEFICIAMENTO_1_FIXO = 1040115.68181818;

    // Ref: Aba 7, row 8 — valores fixos mensais para Mineroduto 1
    // Mapeados conforme Excel: variam por mês (230000 para meses de 31 dias, etc.)
    private static final Map<Mes, Double> MINERODUTO_1_FIXO = Map.ofEntries(
            Map.entry(Mes.JAN, 230000.0),
            Map.entry(Mes.FEV, 210000.0),
            Map.entry(Mes.MAR, 230000.0),
            Map.entry(Mes.ABR, 225000.0),
            Map.entry(Mes.MAI, 210000.0),
            Map.entry(Mes.JUN, 230000.0),
            Map.entry(Mes.JUL, 225000.0),
            Map.entry(Mes.AGO, 230000.0),
            Map.entry(Mes.SET, 230000.0),
            Map.entry(Mes.OUT, 230000.0),
            Map.entry(Mes.NOV, 225000.0),
            Map.entry(Mes.DEZ, 230000.0)
    );

    private static final double KWH_PARA_MWH = 1000.0;

    /**
     * Calcula consumo por área.
     *
     * @param producaoTms       resultado do Cálculo 01 (produção em tms)
     * @param consumoEspecifico resultado do Cálculo 02 (kWh/tms)
     * @return consumo por área com totais e distribuição por estado
     */
    public ConsumoAreaResult calcular(
            Map<AreaProducao, Map<Mes, Double>> producaoTms,
            Map<AreaConsumoEspecifico, Map<Mes, Double>> consumoEspecifico) {

        if (producaoTms == null || consumoEspecifico == null) {
            throw new IllegalArgumentException("Inputs não podem ser nulos");
        }

        Map<AreaConsumoEspecifico, Map<Mes, Double>> consumoKWh = new EnumMap<>(AreaConsumoEspecifico.class);

        for (Mes mes : Mes.values()) {
            // Ref: Aba 7, row 3 = Aba6!C3 * Aba4!B3
            putConsumo(consumoKWh, AreaConsumoEspecifico.FILTRAGEM_GERMANO, mes,
                    getConsumoEsp(consumoEspecifico, AreaConsumoEspecifico.FILTRAGEM_GERMANO, mes)
                            * getProducao(producaoTms, AreaProducao.FILTRAGEM_GERMANO, mes));

            // Ref: Aba 7, row 4 = (Aba4!B4 + Aba4!B5 + Aba4!B6) * Aba6!C4
            // Mineração agrega: Beneficiamento Usina 1 + 2 + 3
            double prodMineracao = getProducao(producaoTms, AreaProducao.BENEFICIAMENTO_USINA_1, mes)
                    + getProducao(producaoTms, AreaProducao.BENEFICIAMENTO_USINA_2, mes)
                    + getProducao(producaoTms, AreaProducao.BENEFICIAMENTO_USINA_3, mes);
            putConsumo(consumoKWh, AreaConsumoEspecifico.MINERACAO_1, mes,
                    prodMineracao * getConsumoEsp(consumoEspecifico, AreaConsumoEspecifico.MINERACAO_1, mes));

            // Ref: Aba 7, row 5 = valor fixo 1040115.68181818
            putConsumo(consumoKWh, AreaConsumoEspecifico.BENEFICIAMENTO_1, mes, BENEFICIAMENTO_1_FIXO);

            // Ref: Aba 7, row 6 = Aba6!C6 * Aba4!B5 (Consumo Benef 2 × Prod Usina 2)
            putConsumo(consumoKWh, AreaConsumoEspecifico.BENEFICIAMENTO_2, mes,
                    getConsumoEsp(consumoEspecifico, AreaConsumoEspecifico.BENEFICIAMENTO_2, mes)
                            * getProducao(producaoTms, AreaProducao.BENEFICIAMENTO_USINA_2, mes));

            // Ref: Aba 7, row 7 = Aba6!C7 * Aba4!B6 (Consumo Benef 3 × Prod Usina 3)
            putConsumo(consumoKWh, AreaConsumoEspecifico.BENEFICIAMENTO_3, mes,
                    getConsumoEsp(consumoEspecifico, AreaConsumoEspecifico.BENEFICIAMENTO_3, mes)
                            * getProducao(producaoTms, AreaProducao.BENEFICIAMENTO_USINA_3, mes));

            // Ref: Aba 7, row 8 = valor fixo por mês
            putConsumo(consumoKWh, AreaConsumoEspecifico.MINERODUTO_1, mes, MINERODUTO_1_FIXO.get(mes));

            // Ref: Aba 7, row 9 = Aba6!C9 * Aba4!B8
            putConsumo(consumoKWh, AreaConsumoEspecifico.MINERODUTO_2, mes,
                    getConsumoEsp(consumoEspecifico, AreaConsumoEspecifico.MINERODUTO_2, mes)
                            * getProducao(producaoTms, AreaProducao.MINERODUTO_2, mes));

            // Ref: Aba 7, row 10 = Aba6!C10 * Aba4!B9
            putConsumo(consumoKWh, AreaConsumoEspecifico.MINERODUTO_3, mes,
                    getConsumoEsp(consumoEspecifico, AreaConsumoEspecifico.MINERODUTO_3, mes)
                            * getProducao(producaoTms, AreaProducao.MINERODUTO_3, mes));

            // Ref: Aba 7, row 11 = Aba6!C11 * Aba4!B10
            putConsumo(consumoKWh, AreaConsumoEspecifico.PREPARACAO_1, mes,
                    getConsumoEsp(consumoEspecifico, AreaConsumoEspecifico.PREPARACAO_1, mes)
                            * getProducao(producaoTms, AreaProducao.PREPARACAO_1, mes));

            // Ref: Aba 7, row 12 = Aba6!C12 * Aba4!B11
            putConsumo(consumoKWh, AreaConsumoEspecifico.PREPARACAO_2, mes,
                    getConsumoEsp(consumoEspecifico, AreaConsumoEspecifico.PREPARACAO_2, mes)
                            * getProducao(producaoTms, AreaProducao.PREPARACAO_2, mes));

            // Ref: Aba 7, row 13 = Aba6!C13 * Aba4!B12
            putConsumo(consumoKWh, AreaConsumoEspecifico.USINA_1, mes,
                    getConsumoEsp(consumoEspecifico, AreaConsumoEspecifico.USINA_1, mes)
                            * getProducao(producaoTms, AreaProducao.USINA_1, mes));

            // Ref: Aba 7, row 14 = Aba6!C14 * Aba4!B13
            putConsumo(consumoKWh, AreaConsumoEspecifico.USINA_2, mes,
                    getConsumoEsp(consumoEspecifico, AreaConsumoEspecifico.USINA_2, mes)
                            * getProducao(producaoTms, AreaProducao.USINA_2, mes));

            // Ref: Aba 7, row 15 = Aba6!C15 * Aba4!B14
            putConsumo(consumoKWh, AreaConsumoEspecifico.USINA_3, mes,
                    getConsumoEsp(consumoEspecifico, AreaConsumoEspecifico.USINA_3, mes)
                            * getProducao(producaoTms, AreaProducao.USINA_3, mes));

            // Ref: Aba 7, row 16 = Aba6!C16 * Aba4!B15
            putConsumo(consumoKWh, AreaConsumoEspecifico.USINA_4, mes,
                    getConsumoEsp(consumoEspecifico, AreaConsumoEspecifico.USINA_4, mes)
                            * getProducao(producaoTms, AreaProducao.USINA_4, mes));

            // Ref: Aba 7, row 17 = Aba6!C17 * Aba4!B16
            putConsumo(consumoKWh, AreaConsumoEspecifico.ESTOCAGEM, mes,
                    getConsumoEsp(consumoEspecifico, AreaConsumoEspecifico.ESTOCAGEM, mes)
                            * getProducao(producaoTms, AreaProducao.VENDAS, mes));
        }

        // Agregar em grupos (MWh) — Ref: Aba 7, rows 21-27
        Map<GrupoConsumo, Map<Mes, Double>> consumoMWh = calcularGruposMWh(consumoKWh);

        // Total — Ref: Aba 7, row 28 = SUM(C21:C27)
        Map<Mes, Double> totalMWh = new EnumMap<>(Mes.class);
        for (Mes mes : Mes.values()) {
            double total = 0;
            for (GrupoConsumo grupo : GrupoConsumo.values()) {
                total += consumoMWh.get(grupo).get(mes);
            }
            totalMWh.put(mes, total);
        }

        // Distribuição por estado — Ref: Aba 7, rows 31-34
        // MG = Filtragem + Mineração + Concentração + Mineroduto (rows 21-24)
        // ES = Preparação + Pelotização + Estocagem (rows 25-27)
        Map<Mes, Double> consumoMG = new EnumMap<>(Mes.class);
        Map<Mes, Double> consumoES = new EnumMap<>(Mes.class);
        Map<Mes, Double> percentualMG = new EnumMap<>(Mes.class);
        Map<Mes, Double> percentualES = new EnumMap<>(Mes.class);

        for (Mes mes : Mes.values()) {
            // Ref: Aba 7, row 31 = SUM(C21:C24)
            double mg = consumoMWh.get(GrupoConsumo.FILTRAGEM_GERMANO).get(mes)
                    + consumoMWh.get(GrupoConsumo.MINERACAO).get(mes)
                    + consumoMWh.get(GrupoConsumo.CONCENTRACAO).get(mes)
                    + consumoMWh.get(GrupoConsumo.MINERODUTO).get(mes);
            consumoMG.put(mes, mg);

            // Ref: Aba 7, row 32 = SUM(C25:C27)
            double es = consumoMWh.get(GrupoConsumo.PREPARACAO).get(mes)
                    + consumoMWh.get(GrupoConsumo.PELOTIZACAO).get(mes)
                    + consumoMWh.get(GrupoConsumo.ESTOCAGEM).get(mes);
            consumoES.put(mes, es);

            double total = totalMWh.get(mes);
            // Ref: Aba 7, row 33 = C31 / C28
            percentualMG.put(mes, mg / total);
            // Ref: Aba 7, row 34 = C32 / C28
            percentualES.put(mes, es / total);
        }

        // % por centro de custo — Ref: Aba 7, rows 38-52
        // MG: Filtragem usa denominador dinâmico; demais usam $C$31 (Jan)
        // ES: todos usam $C$32 (Jan)
        Map<AreaConsumoEspecifico, Map<Mes, Double>> pctCCMG = calcularPercentualCentroCustoMG(consumoKWh, consumoMG);
        Map<AreaConsumoEspecifico, Map<Mes, Double>> pctCCES = calcularPercentualCentroCustoES(consumoKWh, consumoES);

        return new ConsumoAreaResult(
                consumoKWh, consumoMWh, totalMWh,
                consumoMG, consumoES, percentualMG, percentualES,
                pctCCMG, pctCCES
        );
    }

    /**
     * Agrega consumo kWh em 7 grupos e converte para MWh.
     * Ref: Aba 7, rows 21-27 (divide por 1000)
     */
    private Map<GrupoConsumo, Map<Mes, Double>> calcularGruposMWh(
            Map<AreaConsumoEspecifico, Map<Mes, Double>> consumoKWh) {

        Map<GrupoConsumo, Map<Mes, Double>> grupos = new EnumMap<>(GrupoConsumo.class);
        for (GrupoConsumo grupo : GrupoConsumo.values()) {
            grupos.put(grupo, new EnumMap<>(Mes.class));
        }

        for (Mes mes : Mes.values()) {
            // Row 21: Filtragem Germano = row 3
            grupos.get(GrupoConsumo.FILTRAGEM_GERMANO).put(mes,
                    getConsumoKWh(consumoKWh, AreaConsumoEspecifico.FILTRAGEM_GERMANO, mes) / KWH_PARA_MWH);

            // Row 22: Mineração = row 4
            grupos.get(GrupoConsumo.MINERACAO).put(mes,
                    getConsumoKWh(consumoKWh, AreaConsumoEspecifico.MINERACAO_1, mes) / KWH_PARA_MWH);

            // Row 23: Concentração = rows 5 + 6 + 7
            double concentracao = (getConsumoKWh(consumoKWh, AreaConsumoEspecifico.BENEFICIAMENTO_1, mes)
                    + getConsumoKWh(consumoKWh, AreaConsumoEspecifico.BENEFICIAMENTO_2, mes)
                    + getConsumoKWh(consumoKWh, AreaConsumoEspecifico.BENEFICIAMENTO_3, mes)) / KWH_PARA_MWH;
            grupos.get(GrupoConsumo.CONCENTRACAO).put(mes, concentracao);

            // Row 24: Mineroduto = rows 8 + 9 + 10
            double mineroduto = (getConsumoKWh(consumoKWh, AreaConsumoEspecifico.MINERODUTO_1, mes)
                    + getConsumoKWh(consumoKWh, AreaConsumoEspecifico.MINERODUTO_2, mes)
                    + getConsumoKWh(consumoKWh, AreaConsumoEspecifico.MINERODUTO_3, mes)) / KWH_PARA_MWH;
            grupos.get(GrupoConsumo.MINERODUTO).put(mes, mineroduto);

            // Row 25: Preparação = rows 11 + 12
            double preparacao = (getConsumoKWh(consumoKWh, AreaConsumoEspecifico.PREPARACAO_1, mes)
                    + getConsumoKWh(consumoKWh, AreaConsumoEspecifico.PREPARACAO_2, mes)) / KWH_PARA_MWH;
            grupos.get(GrupoConsumo.PREPARACAO).put(mes, preparacao);

            // Row 26: Pelotização = rows 13 + 14 + 15 + 16
            double pelotizacao = (getConsumoKWh(consumoKWh, AreaConsumoEspecifico.USINA_1, mes)
                    + getConsumoKWh(consumoKWh, AreaConsumoEspecifico.USINA_2, mes)
                    + getConsumoKWh(consumoKWh, AreaConsumoEspecifico.USINA_3, mes)
                    + getConsumoKWh(consumoKWh, AreaConsumoEspecifico.USINA_4, mes)) / KWH_PARA_MWH;
            grupos.get(GrupoConsumo.PELOTIZACAO).put(mes, pelotizacao);

            // Row 27: Estocagem = row 17
            grupos.get(GrupoConsumo.ESTOCAGEM).put(mes,
                    getConsumoKWh(consumoKWh, AreaConsumoEspecifico.ESTOCAGEM, mes) / KWH_PARA_MWH);
        }

        return grupos;
    }

    private void putConsumo(Map<AreaConsumoEspecifico, Map<Mes, Double>> map,
                            AreaConsumoEspecifico area, Mes mes, double valor) {
        map.computeIfAbsent(area, k -> new EnumMap<>(Mes.class)).put(mes, valor);
    }

    private double getConsumoEsp(Map<AreaConsumoEspecifico, Map<Mes, Double>> consumoEspecifico,
                                  AreaConsumoEspecifico area, Mes mes) {
        Map<Mes, Double> valoresMes = consumoEspecifico.get(area);
        if (valoresMes == null) {
            throw new IllegalArgumentException("Consumo específico faltando para área: " + area.getLabel());
        }
        Double valor = valoresMes.get(mes);
        if (valor == null) {
            throw new IllegalArgumentException(
                    "Consumo específico faltando para " + area.getLabel() + " " + mes.getLabel());
        }
        return valor;
    }

    private double getProducao(Map<AreaProducao, Map<Mes, Double>> producaoTms,
                                AreaProducao area, Mes mes) {
        Map<Mes, Double> valoresMes = producaoTms.get(area);
        if (valoresMes == null) {
            throw new IllegalArgumentException("Produção faltando para área: " + area.getLabel());
        }
        Double valor = valoresMes.get(mes);
        if (valor == null) {
            throw new IllegalArgumentException(
                    "Produção faltando para " + area.getLabel() + " " + mes.getLabel());
        }
        return valor;
    }

    // Áreas MG para cálculo de % por CC — Ref: Aba 7, rows 38-45
    private static final AreaConsumoEspecifico[] AREAS_MG = {
            AreaConsumoEspecifico.FILTRAGEM_GERMANO,
            AreaConsumoEspecifico.MINERACAO_1,
            AreaConsumoEspecifico.BENEFICIAMENTO_1,
            AreaConsumoEspecifico.BENEFICIAMENTO_2,
            AreaConsumoEspecifico.BENEFICIAMENTO_3,
            AreaConsumoEspecifico.MINERODUTO_1,
            AreaConsumoEspecifico.MINERODUTO_2,
            AreaConsumoEspecifico.MINERODUTO_3
    };

    // Áreas ES para cálculo de % por CC — Ref: Aba 7, rows 46-52
    private static final AreaConsumoEspecifico[] AREAS_ES = {
            AreaConsumoEspecifico.PREPARACAO_1,
            AreaConsumoEspecifico.PREPARACAO_2,
            AreaConsumoEspecifico.USINA_1,
            AreaConsumoEspecifico.USINA_2,
            AreaConsumoEspecifico.USINA_3,
            AreaConsumoEspecifico.USINA_4,
            AreaConsumoEspecifico.ESTOCAGEM
    };

    /**
     * Calcula % por centro de custo MG.
     * Ref: Aba 7, rows 38-45
     * NOTA: Filtragem usa denominador dinâmico (consumoMG do mês).
     * Demais áreas usam denominador fixo ($C$31 = consumoMG de Jan).
     */
    private Map<AreaConsumoEspecifico, Map<Mes, Double>> calcularPercentualCentroCustoMG(
            Map<AreaConsumoEspecifico, Map<Mes, Double>> consumoKWh,
            Map<Mes, Double> consumoMG_MWh) {

        Map<AreaConsumoEspecifico, Map<Mes, Double>> result = new EnumMap<>(AreaConsumoEspecifico.class);
        double denominadorFixo = consumoMG_MWh.get(Mes.JAN) * KWH_PARA_MWH; // $C$31 * 1000

        for (AreaConsumoEspecifico area : AREAS_MG) {
            Map<Mes, Double> pctMes = new EnumMap<>(Mes.class);
            for (Mes mes : Mes.values()) {
                double kWh = getConsumoKWh(consumoKWh, area, mes);
                double denominador;
                if (area == AreaConsumoEspecifico.FILTRAGEM_GERMANO) {
                    // Ref: Aba 7, row 38 — usa denominador dinâmico por mês
                    denominador = consumoMG_MWh.get(mes) * KWH_PARA_MWH;
                } else {
                    // Ref: Aba 7, rows 39-45 — usa $C$31 (Jan) fixo
                    denominador = denominadorFixo;
                }
                pctMes.put(mes, denominador != 0 ? kWh / denominador : 0.0);
            }
            result.put(area, pctMes);
        }
        return result;
    }

    /**
     * Calcula % por centro de custo ES.
     * Ref: Aba 7, rows 46-52
     * Todas as áreas usam denominador fixo ($C$32 = consumoES de Jan).
     */
    private Map<AreaConsumoEspecifico, Map<Mes, Double>> calcularPercentualCentroCustoES(
            Map<AreaConsumoEspecifico, Map<Mes, Double>> consumoKWh,
            Map<Mes, Double> consumoES_MWh) {

        Map<AreaConsumoEspecifico, Map<Mes, Double>> result = new EnumMap<>(AreaConsumoEspecifico.class);
        double denominadorFixo = consumoES_MWh.get(Mes.JAN) * KWH_PARA_MWH; // $C$32 * 1000

        for (AreaConsumoEspecifico area : AREAS_ES) {
            Map<Mes, Double> pctMes = new EnumMap<>(Mes.class);
            for (Mes mes : Mes.values()) {
                double kWh = getConsumoKWh(consumoKWh, area, mes);
                pctMes.put(mes, denominadorFixo != 0 ? kWh / denominadorFixo : 0.0);
            }
            result.put(area, pctMes);
        }
        return result;
    }

    private double getConsumoKWh(Map<AreaConsumoEspecifico, Map<Mes, Double>> consumoKWh,
                                  AreaConsumoEspecifico area, Mes mes) {
        Map<Mes, Double> valoresMes = consumoKWh.get(area);
        if (valoresMes == null) {
            return 0.0;
        }
        Double valor = valoresMes.get(mes);
        return valor != null ? valor : 0.0;
    }
}
