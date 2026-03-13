package com.samarco.calc.service;

import com.samarco.calc.input.CalendarioInput;
import com.samarco.calc.input.ConsumoEspecificoInput;
import com.samarco.calc.input.DemandaContratadaInput;
import com.samarco.calc.input.DespesasFixasInput;
import com.samarco.calc.input.PlanejamentoGeracaoInput;
import com.samarco.calc.input.PlanejamentoProducaoInput;
import com.samarco.calc.input.TarifasDistribuidorasInput;
import com.samarco.calc.input.TarifasEncargosInput;
import com.samarco.calc.model.AreaConsumoEspecifico;
import com.samarco.calc.model.AreaProducao;
import com.samarco.calc.model.CentroCustosResult;
import com.samarco.calc.model.ClasseCustoResult;
import com.samarco.calc.model.ConsumoAreaResult;
import com.samarco.calc.model.DistribuicaoCargaResult;
import com.samarco.calc.model.EncargosResult;
import com.samarco.calc.model.GeracaoPropriaResult;
import com.samarco.calc.model.Mes;
import com.samarco.calc.model.ResumoGeralResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * Orquestrador do Motor de Cálculo.
 * Executa todos os cálculos na ordem correta de dependência:
 *
 * Cálculo 01 (Produção) ──→ independente
 * Cálculo 02 (Consumo Específico) ──→ independente
 * Cálculo 03 (Consumo Área) ──→ depende de 01 + 02
 * Cálculo 04 (Geração Própria) ──→ depende de Calendário
 * Cálculo 05 (Encargos ESS/EER) ──→ depende de 03 + 04
 * Cálculo 13 (Distribuição Carga) ──→ depende de 03 + 04
 * Cálculo 14 (Classe de Custo) ──→ depende de 03 + 04 + 13 + Tarifas + Demanda
 * Cálculo 15 (Centro de Custos) ──→ depende de 03 + 14
 * Cálculo 16 (Resumo Geral) ──→ depende de 01 + 05 + 15 + Despesas
 */
@ApplicationScoped
public class MotorCalculoOrchestrator {

    @Inject
    ProducaoCalculator producaoCalculator;

    @Inject
    ConsumoEspecificoCalculator consumoEspecificoCalculator;

    @Inject
    ConsumoAreaCalculator consumoAreaCalculator;

    @Inject
    GeracaoPropriaCalculator geracaoPropriaCalculator;

    @Inject
    EncargosEssEerCalculator encargosCalculator;

    @Inject
    DistribuicaoCargaCalculator distribuicaoCargaCalculator;

    @Inject
    ClasseCustoCalculator classeCustoCalculator;

    @Inject
    CentroCustosCalculator centroCustosCalculator;

    @Inject
    ResumoGeralCalculator resumoGeralCalculator;

    /**
     * Executa todos os cálculos e retorna o resultado consolidado.
     */
    public ResultadoConsolidado executar(MotorCalculoInput input) {
        if (input == null) {
            throw new IllegalArgumentException("Input do motor de cálculo não pode ser nulo");
        }

        // Cálculo 01 — Produção (tms)
        Map<AreaProducao, Map<Mes, Double>> producaoTms =
                producaoCalculator.calcular(input.producao());

        // Cálculo 02 — Consumo Específico (kWh/tms)
        Map<AreaConsumoEspecifico, Map<Mes, Double>> consumoEspecifico =
                consumoEspecificoCalculator.calcular(input.consumoEspecifico());

        // Cálculo 03 — Consumo Área (kWh → MWh, totais, distribuição MG/ES, % por CC)
        ConsumoAreaResult consumoArea =
                consumoAreaCalculator.calcular(producaoTms, consumoEspecifico);

        // Cálculo 04 — Geração Própria (MWh, MWmédios)
        GeracaoPropriaResult geracaoPropria =
                geracaoPropriaCalculator.calcular(input.geracao(), input.calendario());

        // Cálculo 05 — Encargos ESS/EER (R$ por estado)
        EncargosResult encargos =
                encargosCalculator.calcular(input.tarifas(), consumoArea, geracaoPropria);

        // Cálculo 13 — Distribuição de Carga (MWh por estado)
        DistribuicaoCargaResult distribuicaoCarga =
                distribuicaoCargaCalculator.calcular(consumoArea, geracaoPropria);

        // Cálculo 14 — Classe de Custo (R$ por classe/estado)
        ClasseCustoResult classeCusto = null;
        if (input.tarifasDistribuidoras() != null && input.demandaContratada() != null) {
            classeCusto = classeCustoCalculator.calcular(
                    input.tarifasDistribuidoras(), input.demandaContratada(),
                    consumoArea, geracaoPropria, distribuicaoCarga);
        }

        // Cálculo 15 — Centro de Custos (R$ por CC)
        CentroCustosResult centroCustos = null;
        if (classeCusto != null) {
            centroCustos = centroCustosCalculator.calcular(classeCusto, consumoArea);
        }

        // Cálculo 16 — Resumo Geral
        ResumoGeralResult resumoGeral = null;
        if (centroCustos != null && input.despesasFixas() != null) {
            resumoGeral = resumoGeralCalculator.calcular(
                    centroCustos, encargos, input.despesasFixas(), producaoTms);
        }

        return new ResultadoConsolidado(
                producaoTms, consumoEspecifico,
                consumoArea, geracaoPropria, encargos,
                distribuicaoCarga, classeCusto, centroCustos, resumoGeral
        );
    }

    /**
     * Input consolidado para o motor de cálculo.
     */
    public record MotorCalculoInput(
            PlanejamentoProducaoInput producao,
            ConsumoEspecificoInput consumoEspecifico,
            PlanejamentoGeracaoInput geracao,
            CalendarioInput calendario,
            TarifasEncargosInput tarifas,
            TarifasDistribuidorasInput tarifasDistribuidoras,
            DemandaContratadaInput demandaContratada,
            DespesasFixasInput despesasFixas
    ) {
        // Construtor de compatibilidade para inputs sem os novos campos
        public MotorCalculoInput(
                PlanejamentoProducaoInput producao,
                ConsumoEspecificoInput consumoEspecifico,
                PlanejamentoGeracaoInput geracao,
                CalendarioInput calendario,
                TarifasEncargosInput tarifas) {
            this(producao, consumoEspecifico, geracao, calendario, tarifas, null, null, null);
        }
    }

    /**
     * Resultado consolidado de todos os cálculos.
     */
    public record ResultadoConsolidado(
            Map<AreaProducao, Map<Mes, Double>> producaoTms,
            Map<AreaConsumoEspecifico, Map<Mes, Double>> consumoEspecifico,
            ConsumoAreaResult consumoArea,
            GeracaoPropriaResult geracaoPropria,
            EncargosResult encargos,
            DistribuicaoCargaResult distribuicaoCarga,
            ClasseCustoResult classeCusto,
            CentroCustosResult centroCustos,
            ResumoGeralResult resumoGeral
    ) {
        // Construtor de compatibilidade
        public ResultadoConsolidado(
                Map<AreaProducao, Map<Mes, Double>> producaoTms,
                Map<AreaConsumoEspecifico, Map<Mes, Double>> consumoEspecifico,
                ConsumoAreaResult consumoArea,
                GeracaoPropriaResult geracaoPropria,
                EncargosResult encargos) {
            this(producaoTms, consumoEspecifico, consumoArea, geracaoPropria, encargos,
                    null, null, null, null);
        }
    }
}
