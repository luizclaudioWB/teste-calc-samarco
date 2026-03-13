package com.samarco.calc.service;

import com.samarco.calc.input.CalendarioInput;
import com.samarco.calc.input.PlanejamentoGeracaoInput;
import com.samarco.calc.model.Calendario;
import com.samarco.calc.model.GeracaoPropriaResult;
import com.samarco.calc.model.Mes;
import com.samarco.calc.util.CsvLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste do Cálculo 04 — Geração Própria.
 * Compara resultado Java com valores do Excel (expected_calc04_geracao.csv).
 */
class GeracaoPropriaCalculatorTest {

    private static final double TOLERANCIA = 0.01;

    private static PlanejamentoGeracaoInput geracaoInput;
    private static CalendarioInput calendarioInput;

    @BeforeAll
    static void setUp() throws Exception {
        geracaoInput = CsvLoader.loadGeracao("fixtures/planejamento_geracao.csv");
        calendarioInput = CsvLoader.loadCalendario("fixtures/calendario_input.csv");
    }

    @Test
    void calendarioJaneiroDeve744Horas() {
        // Ref: Aba 9, row 11 Jan = 24 × 31 = 744
        GeracaoPropriaCalculator calculator = new GeracaoPropriaCalculator();
        GeracaoPropriaResult resultado = calculator.calcular(geracaoInput, calendarioInput);

        Calendario calJan = resultado.calendario().get(Mes.JAN);
        assertEquals(31, calJan.diasNoMes());
        assertEquals(10, calJan.diasNaoUteis());
        assertEquals(21, calJan.diasUteis());
        assertEquals(63.0, calJan.horasPonta(), TOLERANCIA);
        assertEquals(681.0, calJan.horasForaPonta(), TOLERANCIA);
        assertEquals(744.0, calJan.totalHoras(), TOLERANCIA);
    }

    @Test
    void guilmanJaneiroDeveSerValorDoInput() {
        // Ref: Aba 9, row 15 Jan = 23979.1671
        GeracaoPropriaCalculator calculator = new GeracaoPropriaCalculator();
        GeracaoPropriaResult resultado = calculator.calcular(geracaoInput, calendarioInput);

        assertEquals(23979.1671, resultado.guilmanMWh().get(Mes.JAN), TOLERANCIA);
    }

    @Test
    void guilmanMWmediosJaneiro() {
        // Ref: Aba 9, row 16 Jan = 23979.1671 / 744 = 32.2300633064516
        GeracaoPropriaCalculator calculator = new GeracaoPropriaCalculator();
        GeracaoPropriaResult resultado = calculator.calcular(geracaoInput, calendarioInput);

        assertEquals(32.2300633064516, resultado.guilmanMWmedios().get(Mes.JAN), TOLERANCIA);
    }

    @Test
    void totalMWhJaneiro() {
        // Ref: Aba 9, row 19 Jan = 23979.1671 + 13943 = 37922.1671
        GeracaoPropriaCalculator calculator = new GeracaoPropriaCalculator();
        GeracaoPropriaResult resultado = calculator.calcular(geracaoInput, calendarioInput);

        assertEquals(37922.1671, resultado.totalMWh().get(Mes.JAN), TOLERANCIA);
    }

    @Test
    void totalMWmediosJaneiro() {
        // Ref: Aba 9, row 20 Jan = 37922.1671 / 744 = 50.9706547043011
        GeracaoPropriaCalculator calculator = new GeracaoPropriaCalculator();
        GeracaoPropriaResult resultado = calculator.calcular(geracaoInput, calendarioInput);

        assertEquals(50.9706547043011, resultado.totalMWmedios().get(Mes.JAN), TOLERANCIA);
    }

    @Test
    void deveTer12MesesDeResultado() {
        GeracaoPropriaCalculator calculator = new GeracaoPropriaCalculator();
        GeracaoPropriaResult resultado = calculator.calcular(geracaoInput, calendarioInput);

        assertEquals(12, resultado.totalMWh().size());
        assertEquals(12, resultado.calendario().size());
    }

    @Test
    void todosOsMesesDevemBaterComExcel() {
        // Valores esperados do Excel: expected_calc04_geracao.csv
        GeracaoPropriaCalculator calculator = new GeracaoPropriaCalculator();
        GeracaoPropriaResult resultado = calculator.calcular(geracaoInput, calendarioInput);

        // Ref: Aba 9, row 19 — Total MWh por mês
        double[] expectedTotalMWh = {
                37922.1671, 33560.45235, 37627.72795, 34096.14825,
                34322.53325, 25436.6525, 22311.194, 21150.57205,
                20337.4585, 22328.92045, 28135.7122, 34939.3402
        };

        for (Mes mes : Mes.values()) {
            assertEquals(expectedTotalMWh[mes.getNumero() - 1], resultado.totalMWh().get(mes),
                    TOLERANCIA,
                    "Total MWh diverge em " + mes.getLabel());
        }
    }
}
