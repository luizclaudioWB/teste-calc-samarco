package com.samarco.calc.service;

import com.samarco.calc.input.CalendarioInput;
import com.samarco.calc.input.ConsumoEspecificoInput;
import com.samarco.calc.input.PlanejamentoGeracaoInput;
import com.samarco.calc.input.PlanejamentoProducaoInput;
import com.samarco.calc.model.AreaConsumoEspecifico;
import com.samarco.calc.model.AreaProducao;
import com.samarco.calc.model.ConsumoAreaResult;
import com.samarco.calc.model.DistribuicaoCargaResult;
import com.samarco.calc.model.GeracaoPropriaResult;
import com.samarco.calc.model.Mes;
import com.samarco.calc.util.CsvLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste do Cálculo — Distribuição de Carga por Estado.
 * Ref: Aba "13Distribuição de Carga"
 *
 * Constrói a cadeia completa (Calc 01 → 02 → 03 → 04) e compara com valores do Excel.
 */
class DistribuicaoCargaCalculatorTest {

    // Tolerância MWh — aceita 0.01 MWh por conta de acúmulo de precisão double
    private static final double TOLERANCIA_MWH = 1e-2;

    private static ConsumoAreaResult consumoArea;
    private static GeracaoPropriaResult geracaoPropria;

    @BeforeAll
    static void setUp() throws Exception {
        // Cálculo 01 — Produção
        PlanejamentoProducaoInput prodInput = CsvLoader.loadProducao("fixtures/planejamento_producao.csv");
        Map<AreaProducao, Map<Mes, Double>> producaoTms = new ProducaoCalculator().calcular(prodInput);

        // Cálculo 02 — Consumo Específico
        ConsumoEspecificoInput ceInput = CsvLoader.loadConsumoEspecifico("fixtures/consumo_especifico.csv");
        Map<AreaConsumoEspecifico, Map<Mes, Double>> consumoEsp = new ConsumoEspecificoCalculator().calcular(ceInput);

        // Cálculo 03 — Consumo Área
        consumoArea = new ConsumoAreaCalculator().calcular(producaoTms, consumoEsp);

        // Cálculo 04 — Geração Própria
        PlanejamentoGeracaoInput geracaoInput = CsvLoader.loadGeracao("fixtures/planejamento_geracao.csv");
        CalendarioInput calendarioInput = CsvLoader.loadCalendario("fixtures/calendario_input.csv");
        geracaoPropria = new GeracaoPropriaCalculator().calcular(geracaoInput, calendarioInput);
    }

    @Test
    void necessidadeCompraMGJaneiro() {
        // Ref: Aba 13, row 10 Jan = 46981.4003101397 MWh
        DistribuicaoCargaResult resultado = new DistribuicaoCargaCalculator()
                .calcular(consumoArea, geracaoPropria);

        assertEquals(46981.4003101397, resultado.necessidadeCompraMG().get(Mes.JAN), TOLERANCIA_MWH,
                "Necessidade Compra MG Jan");
    }

    @Test
    void necessidadeCompraESJaneiro() {
        // Ref: Aba 13, row 21 Jan = 35273.8771980175 MWh
        DistribuicaoCargaResult resultado = new DistribuicaoCargaCalculator()
                .calcular(consumoArea, geracaoPropria);

        assertEquals(35273.8771980175, resultado.necessidadeCompraES().get(Mes.JAN), TOLERANCIA_MWH,
                "Necessidade Compra ES Jan");
    }

    @Test
    void consumoMGJaneiro() {
        // Ref: Aba 13, row 5 Jan = consumo MG da Aba 7 = 68893.75 MWh
        DistribuicaoCargaResult resultado = new DistribuicaoCargaCalculator()
                .calcular(consumoArea, geracaoPropria);

        assertEquals(68893.75, resultado.consumoMG_MWh().get(Mes.JAN), TOLERANCIA_MWH,
                "Consumo MG Jan");
    }

    @Test
    void consumoESJaneiro() {
        // Ref: Aba 13, row 16 Jan = consumo ES da Aba 7 = 47783.38 MWh
        DistribuicaoCargaResult resultado = new DistribuicaoCargaCalculator()
                .calcular(consumoArea, geracaoPropria);

        assertEquals(47783.38, resultado.consumoES_MWh().get(Mes.JAN), TOLERANCIA_MWH,
                "Consumo ES Jan");
    }

    @Test
    void perdasMGDevemSer3PorCentoDoConsumo() {
        // Ref: Aba 13, row 7 = consumoMG × 0.03
        DistribuicaoCargaResult resultado = new DistribuicaoCargaCalculator()
                .calcular(consumoArea, geracaoPropria);

        for (Mes mes : Mes.values()) {
            double consumoMG = resultado.consumoMG_MWh().get(mes);
            double perdasEsperadas = consumoMG * 0.03;
            assertEquals(perdasEsperadas, resultado.perdasMG_MWh().get(mes), TOLERANCIA_MWH,
                    "Perdas MG 3%% em " + mes.getLabel());
        }
    }

    @Test
    void perdasESDevemSer3PorCentoDoConsumo() {
        // Ref: Aba 13, row 18 = consumoES × 0.03
        DistribuicaoCargaResult resultado = new DistribuicaoCargaCalculator()
                .calcular(consumoArea, geracaoPropria);

        for (Mes mes : Mes.values()) {
            double consumoES = resultado.consumoES_MWh().get(mes);
            double perdasEsperadas = consumoES * 0.03;
            assertEquals(perdasEsperadas, resultado.perdasES_MWh().get(mes), TOLERANCIA_MWH,
                    "Perdas ES 3%% em " + mes.getLabel());
        }
    }

    @Test
    void totalNecessidadeCompraSamarcoJaneiro() {
        // Ref: Aba 13, row 31 Jan = necCompraMG + necCompraES
        DistribuicaoCargaResult resultado = new DistribuicaoCargaCalculator()
                .calcular(consumoArea, geracaoPropria);

        double expected = resultado.necessidadeCompraMG().get(Mes.JAN)
                + resultado.necessidadeCompraES().get(Mes.JAN);
        assertEquals(expected, resultado.necessidadeCompraSamarco().get(Mes.JAN), TOLERANCIA_MWH,
                "Necessidade Compra Total Jan");
    }

    @Test
    void todosOsMesesNecessidadeCompraMGDevemBater() {
        // Ref: Aba 13, row 10 — necessidade compra MG todos os meses
        DistribuicaoCargaResult resultado = new DistribuicaoCargaCalculator()
                .calcular(consumoArea, geracaoPropria);

        // Valores calculados: consumoMG × 1.03 - geração Guilman
        for (Mes mes : Mes.values()) {
            double consumoMG = consumoArea.consumoMG_MWh().get(mes);
            double guilman = geracaoPropria.guilmanMWh().get(mes);
            double necessidadeTotal = consumoMG * 1.03;
            double expected = Math.max(0.0, necessidadeTotal - guilman);

            assertEquals(expected, resultado.necessidadeCompraMG().get(mes), TOLERANCIA_MWH,
                    "Necessidade Compra MG em " + mes.getLabel());
        }
    }

    @Test
    void todosOsMesesNecessidadeCompraESDevemBater() {
        // Ref: Aba 13, row 21 — necessidade compra ES todos os meses
        DistribuicaoCargaResult resultado = new DistribuicaoCargaCalculator()
                .calcular(consumoArea, geracaoPropria);

        // Valores calculados: consumoES × 1.03 - geração MunizFreire
        for (Mes mes : Mes.values()) {
            double consumoES = consumoArea.consumoES_MWh().get(mes);
            double muniz = geracaoPropria.munizFreireMWh().get(mes);
            double necessidadeTotal = consumoES * 1.03;
            double expected = necessidadeTotal - muniz;

            assertEquals(expected, resultado.necessidadeCompraES().get(mes), TOLERANCIA_MWH,
                    "Necessidade Compra ES em " + mes.getLabel());
        }
    }

    @Test
    void consumoTotalDeveSerSomaMGES() {
        // Ref: Aba 13, row 26 = consumoMG + consumoES
        DistribuicaoCargaResult resultado = new DistribuicaoCargaCalculator()
                .calcular(consumoArea, geracaoPropria);

        for (Mes mes : Mes.values()) {
            double expected = resultado.consumoMG_MWh().get(mes) + resultado.consumoES_MWh().get(mes);
            assertEquals(expected, resultado.consumoTotalMWh().get(mes), TOLERANCIA_MWH,
                    "Consumo Total = MG + ES em " + mes.getLabel());
        }
    }

    @Test
    void qtdeCompradaIgualNecessidadeCompra() {
        // Ref: Aba 13, rows 11/22/32 são cópia de rows 10/21/31
        DistribuicaoCargaResult resultado = new DistribuicaoCargaCalculator()
                .calcular(consumoArea, geracaoPropria);

        for (Mes mes : Mes.values()) {
            assertEquals(resultado.necessidadeCompraMG().get(mes),
                    resultado.qtdeCompradaMG().get(mes), TOLERANCIA_MWH,
                    "qtdeCompradaMG deve igualar necessidadeCompraMG em " + mes.getLabel());

            assertEquals(resultado.necessidadeCompraES().get(mes),
                    resultado.qtdeCompradaES().get(mes), TOLERANCIA_MWH,
                    "qtdeCompradaES deve igualar necessidadeCompraES em " + mes.getLabel());
        }
    }
}
