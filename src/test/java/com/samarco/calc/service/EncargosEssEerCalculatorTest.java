package com.samarco.calc.service;

import com.samarco.calc.input.CalendarioInput;
import com.samarco.calc.input.ConsumoEspecificoInput;
import com.samarco.calc.input.PlanejamentoGeracaoInput;
import com.samarco.calc.input.PlanejamentoProducaoInput;
import com.samarco.calc.input.TarifasEncargosInput;
import com.samarco.calc.model.AreaConsumoEspecifico;
import com.samarco.calc.model.AreaProducao;
import com.samarco.calc.model.ConsumoAreaResult;
import com.samarco.calc.model.EncargosResult;
import com.samarco.calc.model.GeracaoPropriaResult;
import com.samarco.calc.model.Mes;
import com.samarco.calc.util.CsvLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste do Cálculo 05 — Encargos ESS/EER.
 * Compara resultado Java com valores do Excel (expected_calc05_encargos.csv).
 */
class EncargosEssEerCalculatorTest {

    // Tolerância para BigDecimal — aceita diferença de até R$ 1.00 por conta de
    // acúmulo de precisão em cadeia de cálculos double → BigDecimal
    private static final BigDecimal TOLERANCIA = new BigDecimal("1.0");

    private static ConsumoAreaResult consumoArea;
    private static GeracaoPropriaResult geracaoPropria;
    private static TarifasEncargosInput tarifas;

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

        // Tarifas
        tarifas = CsvLoader.loadTarifas("fixtures/tarifas_encargos.csv");
    }

    @Test
    void consumoSamarcoJaneiro() {
        // Ref: Aba 12, row 5 Jan = 78754.96 MWh
        EncargosResult resultado = new EncargosEssEerCalculator()
                .calcular(tarifas, consumoArea, geracaoPropria);

        assertBigDecimalClose(new BigDecimal("78754.96"),
                resultado.consumoSamarcoMWh().get(Mes.JAN),
                new BigDecimal("1.0"),
                "Consumo Samarco Jan");
    }

    @Test
    void valorTotalEerJaneiro() {
        // Ref: Aba 12, row 6 Jan = 1152969.07
        EncargosResult resultado = new EncargosEssEerCalculator()
                .calcular(tarifas, consumoArea, geracaoPropria);

        assertBigDecimalClose(new BigDecimal("1152969.07"),
                resultado.valorTotalEer().get(Mes.JAN),
                TOLERANCIA,
                "Valor Total EER Jan");
    }

    @Test
    void valorTotalEssJaneiro() {
        // Ref: Aba 12, row 8 Jan = 239031.34
        EncargosResult resultado = new EncargosEssEerCalculator()
                .calcular(tarifas, consumoArea, geracaoPropria);

        assertBigDecimalClose(new BigDecimal("239031.34"),
                resultado.valorTotalEss().get(Mes.JAN),
                TOLERANCIA,
                "Valor Total ESS Jan");
    }

    @Test
    void mgEerJaneiro() {
        // Ref: Aba 12, row 11 Jan = 680787.81
        EncargosResult resultado = new EncargosEssEerCalculator()
                .calcular(tarifas, consumoArea, geracaoPropria);

        assertBigDecimalClose(new BigDecimal("680787.81"),
                resultado.mgEer().get(Mes.JAN),
                TOLERANCIA,
                "MG EER Jan");
    }

    @Test
    void esEerJaneiro() {
        // Ref: Aba 12, row 14 Jan = 472181.26
        EncargosResult resultado = new EncargosEssEerCalculator()
                .calcular(tarifas, consumoArea, geracaoPropria);

        assertBigDecimalClose(new BigDecimal("472181.26"),
                resultado.esEer().get(Mes.JAN),
                TOLERANCIA,
                "ES EER Jan");
    }

    @Test
    void mgTotalDeveSerSomaMgEerMgEss() {
        EncargosResult resultado = new EncargosEssEerCalculator()
                .calcular(tarifas, consumoArea, geracaoPropria);

        for (Mes mes : Mes.values()) {
            BigDecimal expected = resultado.mgEer().get(mes).add(resultado.mgEss().get(mes));
            assertEquals(0, expected.compareTo(resultado.mgTotal().get(mes)),
                    "MG Total deve ser MG_EER + MG_ESS em " + mes.getLabel());
        }
    }

    @Test
    void esTotalDeveSerSomaEsEerEsEss() {
        EncargosResult resultado = new EncargosEssEerCalculator()
                .calcular(tarifas, consumoArea, geracaoPropria);

        for (Mes mes : Mes.values()) {
            BigDecimal expected = resultado.esEer().get(mes).add(resultado.esEss().get(mes));
            assertEquals(0, expected.compareTo(resultado.esTotal().get(mes)),
                    "ES Total deve ser ES_EER + ES_ESS em " + mes.getLabel());
        }
    }

    @Test
    void todosOsMesesConsumoSamarcoDevemBater() {
        EncargosResult resultado = new EncargosEssEerCalculator()
                .calcular(tarifas, consumoArea, geracaoPropria);

        // Ref: Aba 12, row 5 — Consumo Samarco MWh por mês
        double[] expectedConsumo = {
                78754.96, 80539.38, 79475.79, 87824.35,
                87955.05, 98991.70, 104110.04, 100975.17,
                103213.09, 101447.00, 87500.29, 84444.08
        };

        for (Mes mes : Mes.values()) {
            assertBigDecimalClose(
                    BigDecimal.valueOf(expectedConsumo[mes.getNumero() - 1]),
                    resultado.consumoSamarcoMWh().get(mes),
                    new BigDecimal("1.0"),
                    "Consumo Samarco " + mes.getLabel());
        }
    }

    private static void assertBigDecimalClose(BigDecimal expected, BigDecimal actual,
                                               BigDecimal tolerancia, String message) {
        assertNotNull(actual, message + " é nulo");
        BigDecimal diff = expected.subtract(actual).abs();
        assertTrue(diff.compareTo(tolerancia) <= 0,
                String.format("%s: esperado=%s, obtido=%s, diff=%s (tolerância=%s)",
                        message, expected.toPlainString(), actual.toPlainString(),
                        diff.toPlainString(), tolerancia.toPlainString()));
    }
}
