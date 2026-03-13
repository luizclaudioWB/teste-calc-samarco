package com.samarco.calc.input;

import com.samarco.calc.model.Mes;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Input de despesas fixas para o Resumo Geral.
 * Ref: Aba "16Resumo GERAL", rows 8-15
 *
 * Valores em R$ por mês, inseridos manualmente no planejamento.
 */
public record DespesasFixasInput(
        Map<Mes, BigDecimal> royaltiesAneel,          // row 8 — 50620002
        Map<Mes, BigDecimal> servicoTransmissao,      // row 9 — 50620003
        Map<Mes, BigDecimal> operacaoHidreletricas,   // row 10 — 50620004
        Map<Mes, BigDecimal> outrasDespHidreletricas, // row 11 — 50620099
        Map<Mes, BigDecimal> omRedeBasica,            // row 12 — 50630001
        Map<Mes, BigDecimal> outrasDespRB,            // row 13 — 50630099
        Map<Mes, BigDecimal> energiaAdministrativa,   // row 14 — 50610005
        Map<Mes, BigDecimal> outrosCustosEE           // row 15 — 50610099
) {
    public DespesasFixasInput {
        if (royaltiesAneel == null || servicoTransmissao == null || operacaoHidreletricas == null
                || outrasDespHidreletricas == null || omRedeBasica == null || outrasDespRB == null
                || energiaAdministrativa == null || outrosCustosEE == null) {
            throw new IllegalArgumentException("Despesas fixas: nenhum campo pode ser nulo");
        }
    }
}
