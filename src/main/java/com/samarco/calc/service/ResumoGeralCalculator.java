package com.samarco.calc.service;

import com.samarco.calc.input.DespesasFixasInput;
import com.samarco.calc.model.AreaProducao;
import com.samarco.calc.model.CentroCustosResult;
import com.samarco.calc.model.EncargosResult;
import com.samarco.calc.model.Mes;
import com.samarco.calc.model.ResumoGeralResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

/**
 * Cálculo — Resumo Geral.
 * Ref: Aba "16Resumo GERAL"
 *
 * Consolida todos os custos (classes 50610002-50610008 + despesas fixas),
 * calcula produção total e custo específico (R$/tms).
 */
@ApplicationScoped
public class ResumoGeralCalculator {

    private static final int SCALE = 15;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Calcula resumo geral.
     *
     * @param centroCustos  resultado da Aba 15
     * @param encargos      resultado da Aba 12 (EER/ESS)
     * @param despesasFixas despesas fixas input (Aba 16, rows 8-15)
     * @param producaoTms   resultado do Cálculo 01 (produção em tms)
     * @return resumo geral consolidado
     */
    public ResumoGeralResult calcular(
            CentroCustosResult centroCustos,
            EncargosResult encargos,
            DespesasFixasInput despesasFixas,
            Map<AreaProducao, Map<Mes, Double>> producaoTms) {

        if (centroCustos == null || encargos == null || despesasFixas == null || producaoTms == null) {
            throw new IllegalArgumentException("Inputs não podem ser nulos");
        }

        // Classes de custo — Ref: Aba 16, rows 3-7
        Map<Mes, BigDecimal> consumo = centroCustos.totalConsumoSamarco();  // row 3
        Map<Mes, BigDecimal> usoRede = centroCustos.totalUsoRedeSamarco();  // row 4
        Map<Mes, BigDecimal> enc = centroCustos.totalEncargoSamarco();      // row 5
        Map<Mes, BigDecimal> eer = encargos.valorTotalEer();                // row 6
        Map<Mes, BigDecimal> ess = encargos.valorTotalEss();                // row 7

        // Total geral, produção, custo específico, fixo/variável
        Map<Mes, BigDecimal> totalGeral = new EnumMap<>(Mes.class);
        Map<Mes, Double> prodPelotas = new EnumMap<>(Mes.class);
        Map<Mes, Double> prodPelletFeed = new EnumMap<>(Mes.class);
        Map<Mes, Double> prodPscPsm = new EnumMap<>(Mes.class);
        Map<Mes, Double> prodTotal = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> custoEspecifico = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> custoFixo = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> custoFixoPorTms = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> custoVar = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> custoVarPorTms = new EnumMap<>(Mes.class);

        for (Mes mes : Mes.values()) {
            // Ref: Aba 16, row 16 = SUM(C3:C15)
            BigDecimal total = consumo.get(mes)
                    .add(usoRede.get(mes))
                    .add(enc.get(mes))
                    .add(eer.get(mes))
                    .add(ess.get(mes))
                    .add(despesasFixas.royaltiesAneel().get(mes))
                    .add(despesasFixas.servicoTransmissao().get(mes))
                    .add(despesasFixas.operacaoHidreletricas().get(mes))
                    .add(despesasFixas.outrasDespHidreletricas().get(mes))
                    .add(despesasFixas.omRedeBasica().get(mes))
                    .add(despesasFixas.outrasDespRB().get(mes))
                    .add(despesasFixas.energiaAdministrativa().get(mes))
                    .add(despesasFixas.outrosCustosEE().get(mes));
            totalGeral.put(mes, total);

            // Ref: Aba 16, row 20 = SUM('4Produção - OCULTA'!B12:B15)
            double pelotas = getProducao(producaoTms, AreaProducao.USINA_1, mes)
                    + getProducao(producaoTms, AreaProducao.USINA_2, mes)
                    + getProducao(producaoTms, AreaProducao.USINA_3, mes)
                    + getProducao(producaoTms, AreaProducao.USINA_4, mes);
            prodPelotas.put(mes, pelotas);

            // Ref: Aba 16, row 21 = '4Produção - OCULTA'!B18
            double pelletFeed = getProducao(producaoTms, AreaProducao.PELLET_FEED, mes);
            prodPelletFeed.put(mes, pelletFeed);

            // Ref: Aba 16, row 22 = '4Produção - OCULTA'!B17 (Produção PSC+PSM = Vendas no contexto)
            double pscPsm = getProducao(producaoTms, AreaProducao.PRODUCAO_PSC_PSM, mes);
            prodPscPsm.put(mes, pscPsm);

            // Ref: Aba 16, row 23 = SUM(C20:C22)
            double totalProd = pelotas + pelletFeed + pscPsm;
            prodTotal.put(mes, totalProd);

            // Ref: Aba 16, row 24 = C16 / C23
            if (totalProd != 0) {
                custoEspecifico.put(mes, total.divide(BigDecimal.valueOf(totalProd), SCALE, ROUNDING));
            } else {
                custoEspecifico.put(mes, BigDecimal.ZERO);
            }

            // Ref: Aba 16, row 27 — Custo Fixo = Uso de Rede + despesas fixas (rows 8-15)
            BigDecimal fixo = usoRede.get(mes)
                    .add(despesasFixas.royaltiesAneel().get(mes))
                    .add(despesasFixas.servicoTransmissao().get(mes))
                    .add(despesasFixas.operacaoHidreletricas().get(mes))
                    .add(despesasFixas.outrasDespHidreletricas().get(mes))
                    .add(despesasFixas.omRedeBasica().get(mes))
                    .add(despesasFixas.outrasDespRB().get(mes))
                    .add(despesasFixas.energiaAdministrativa().get(mes))
                    .add(despesasFixas.outrosCustosEE().get(mes));
            custoFixo.put(mes, fixo);

            // Ref: Aba 16, row 28 = C27 / C23
            if (totalProd != 0) {
                custoFixoPorTms.put(mes, fixo.divide(BigDecimal.valueOf(totalProd), SCALE, ROUNDING));
            } else {
                custoFixoPorTms.put(mes, BigDecimal.ZERO);
            }

            // Ref: Aba 16, row 29 — Custo Variável = Consumo + Encargo + EER + ESS
            BigDecimal variavel = consumo.get(mes)
                    .add(enc.get(mes))
                    .add(eer.get(mes))
                    .add(ess.get(mes));
            custoVar.put(mes, variavel);

            // Ref: Aba 16, row 30 = C29 / C23
            if (totalProd != 0) {
                custoVarPorTms.put(mes, variavel.divide(BigDecimal.valueOf(totalProd), SCALE, ROUNDING));
            } else {
                custoVarPorTms.put(mes, BigDecimal.ZERO);
            }
        }

        return new ResumoGeralResult(
                consumo, usoRede, enc, eer, ess,
                despesasFixas.royaltiesAneel(), despesasFixas.servicoTransmissao(),
                despesasFixas.operacaoHidreletricas(), despesasFixas.outrasDespHidreletricas(),
                despesasFixas.omRedeBasica(), despesasFixas.outrasDespRB(),
                despesasFixas.energiaAdministrativa(), despesasFixas.outrosCustosEE(),
                totalGeral,
                prodPelotas, prodPelletFeed, prodPscPsm, prodTotal, custoEspecifico,
                custoFixo, custoFixoPorTms, custoVar, custoVarPorTms
        );
    }

    private double getProducao(Map<AreaProducao, Map<Mes, Double>> producaoTms,
                                AreaProducao area, Mes mes) {
        Map<Mes, Double> valores = producaoTms.get(area);
        if (valores == null || valores.get(mes) == null) {
            return 0.0;
        }
        return valores.get(mes);
    }
}
