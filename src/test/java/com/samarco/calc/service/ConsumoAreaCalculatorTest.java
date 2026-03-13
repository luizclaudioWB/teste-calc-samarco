package com.samarco.calc.service;

import com.samarco.calc.input.ConsumoEspecificoInput;
import com.samarco.calc.input.PlanejamentoProducaoInput;
import com.samarco.calc.model.AreaConsumoEspecifico;
import com.samarco.calc.model.AreaProducao;
import com.samarco.calc.model.ConsumoAreaResult;
import com.samarco.calc.model.GrupoConsumo;
import com.samarco.calc.model.Mes;
import com.samarco.calc.util.CsvLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste do Cálculo 03 — Consumo Área.
 * Compara resultado Java com valores do Excel (expected_calc03_consumo_area.csv).
 */
class ConsumoAreaCalculatorTest {

    // Tolerância maior por conta da cadeia de multiplicações
    private static final double TOLERANCIA = 0.1;
    private static final double TOLERANCIA_PCT = 0.000001;

    private static Map<AreaProducao, Map<Mes, Double>> producaoTms;
    private static Map<AreaConsumoEspecifico, Map<Mes, Double>> consumoEspecifico;

    @BeforeAll
    static void setUp() throws Exception {
        PlanejamentoProducaoInput prodInput = CsvLoader.loadProducao("fixtures/planejamento_producao.csv");
        ProducaoCalculator prodCalc = new ProducaoCalculator();
        producaoTms = prodCalc.calcular(prodInput);

        ConsumoEspecificoInput ceInput = CsvLoader.loadConsumoEspecifico("fixtures/consumo_especifico.csv");
        ConsumoEspecificoCalculator ceCalc = new ConsumoEspecificoCalculator();
        consumoEspecifico = ceCalc.calcular(ceInput);
    }

    @Test
    void filtragemGermanoJaneiro() {
        // Ref: Aba 7, row 3 Jan = 3471861.41 kWh
        ConsumoAreaCalculator calculator = new ConsumoAreaCalculator();
        ConsumoAreaResult resultado = calculator.calcular(producaoTms, consumoEspecifico);

        assertEquals(3471861.41, resultado.consumoKWh()
                        .get(AreaConsumoEspecifico.FILTRAGEM_GERMANO).get(Mes.JAN),
                TOLERANCIA);
    }

    @Test
    void totalMWhJaneiro() {
        // Ref: Aba 7, row 28 Jan = 116677.13 MWh
        ConsumoAreaCalculator calculator = new ConsumoAreaCalculator();
        ConsumoAreaResult resultado = calculator.calcular(producaoTms, consumoEspecifico);

        assertEquals(116677.13, resultado.totalMWh().get(Mes.JAN), TOLERANCIA);
    }

    @Test
    void distribuicaoEstadoJaneiro() {
        // Ref: Aba 7, rows 31-34 Jan
        ConsumoAreaCalculator calculator = new ConsumoAreaCalculator();
        ConsumoAreaResult resultado = calculator.calcular(producaoTms, consumoEspecifico);

        // MG = 68893.75 MWh
        assertEquals(68893.75, resultado.consumoMG_MWh().get(Mes.JAN), TOLERANCIA);
        // ES = 47783.38 MWh
        assertEquals(47783.38, resultado.consumoES_MWh().get(Mes.JAN), TOLERANCIA);
        // %MG = 0.5905
        assertEquals(0.590465, resultado.percentualMG().get(Mes.JAN), TOLERANCIA_PCT);
        // %ES = 0.4095
        assertEquals(0.409535, resultado.percentualES().get(Mes.JAN), TOLERANCIA_PCT);
    }

    @Test
    void todosOsMesesTotalMWhDevemBaterComExcel() {
        ConsumoAreaCalculator calculator = new ConsumoAreaCalculator();
        ConsumoAreaResult resultado = calculator.calcular(producaoTms, consumoEspecifico);

        // Ref: Aba 7, row 28 — Total MWh por mês
        double[] expectedTotal = {
                116677.130687531, 114099.828732371, 117103.520579065,
                121920.494557967, 122277.579043237, 124428.348336437,
                126421.229283849, 122125.743992683, 123550.551448111,
                123775.915380402, 115636.006676375, 119383.42258832
        };

        for (Mes mes : Mes.values()) {
            assertEquals(expectedTotal[mes.getNumero() - 1], resultado.totalMWh().get(mes),
                    TOLERANCIA,
                    "Total MWh diverge em " + mes.getLabel());
        }
    }

    @Test
    void grupoConcentracaoDeveAgregarBeneficiamento123() {
        // Ref: Aba 7, row 23 Jan = 54364.49 MWh (constante porque Benef 1 é fixo)
        ConsumoAreaCalculator calculator = new ConsumoAreaCalculator();
        ConsumoAreaResult resultado = calculator.calcular(producaoTms, consumoEspecifico);

        assertEquals(54364.49, resultado.consumoMWh()
                        .get(GrupoConsumo.CONCENTRACAO).get(Mes.JAN),
                TOLERANCIA);
    }

    @Test
    void percentuaisMGESDevemSomarUm() {
        ConsumoAreaCalculator calculator = new ConsumoAreaCalculator();
        ConsumoAreaResult resultado = calculator.calcular(producaoTms, consumoEspecifico);

        for (Mes mes : Mes.values()) {
            double soma = resultado.percentualMG().get(mes) + resultado.percentualES().get(mes);
            assertEquals(1.0, soma, 0.0001,
                    "Percentuais MG + ES devem somar 1.0 em " + mes.getLabel());
        }
    }
}
