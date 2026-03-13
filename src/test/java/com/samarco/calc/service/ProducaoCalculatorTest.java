package com.samarco.calc.service;

import com.samarco.calc.input.PlanejamentoProducaoInput;
import com.samarco.calc.model.AreaProducao;
import com.samarco.calc.model.Mes;
import com.samarco.calc.util.CsvLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste do Cálculo 01 — Produção (tms).
 * Compara resultado Java com valores do Excel (expected_calc01_producao.csv).
 */
class ProducaoCalculatorTest {

    private static final double TOLERANCIA = 0.01; // tolerância para double comparison

    private static Map<AreaProducao, Map<Mes, Double>> expectedOutput;
    private static PlanejamentoProducaoInput input;

    @BeforeAll
    static void setUp() throws Exception {
        input = CsvLoader.loadProducao("fixtures/planejamento_producao.csv");
        expectedOutput = loadExpectedProducao("fixtures/expected_calc01_producao.csv");
    }

    @Test
    void deveConverterKtmsParaTmsParaTodasAreasEMeses() {
        ProducaoCalculator calculator = new ProducaoCalculator();
        Map<AreaProducao, Map<Mes, Double>> resultado = calculator.calcular(input);

        assertNotNull(resultado);

        for (Map.Entry<AreaProducao, Map<Mes, Double>> entry : expectedOutput.entrySet()) {
            AreaProducao area = entry.getKey();
            Map<Mes, Double> expectedMes = entry.getValue();

            assertTrue(resultado.containsKey(area),
                    "Área não encontrada no resultado: " + area.getLabel());

            Map<Mes, Double> resultadoMes = resultado.get(area);

            for (Mes mes : Mes.values()) {
                Double expected = expectedMes.get(mes);
                Double actual = resultadoMes.get(mes);

                if (expected == null) continue;

                assertNotNull(actual,
                        "Valor nulo para " + area.getLabel() + " " + mes.getLabel());

                assertEquals(expected, actual, TOLERANCIA,
                        String.format("Divergência em %s %s: esperado=%.6f, obtido=%.6f",
                                area.getLabel(), mes.getLabel(), expected, actual));
            }
        }
    }

    @Test
    void deveRejeitarInputNulo() {
        ProducaoCalculator calculator = new ProducaoCalculator();
        assertThrows(IllegalArgumentException.class, () -> calculator.calcular(null));
    }

    @Test
    void filtragemGermanoJaneiroDeveSerInputVezes1000() {
        // Ref: Aba 4, B3 = 1059068.48 (= 1059.069 × 1000, arredondamento do Excel)
        ProducaoCalculator calculator = new ProducaoCalculator();
        Map<AreaProducao, Map<Mes, Double>> resultado = calculator.calcular(input);

        double inputKtms = input.producaoKtms().get(AreaProducao.FILTRAGEM_GERMANO).get(Mes.JAN);
        double resultadoTms = resultado.get(AreaProducao.FILTRAGEM_GERMANO).get(Mes.JAN);

        assertEquals(inputKtms * 1000.0, resultadoTms, TOLERANCIA);
    }

    /**
     * Carrega valores esperados do CSV extraído do Excel.
     */
    private static Map<AreaProducao, Map<Mes, Double>> loadExpectedProducao(String resourcePath) throws Exception {
        Map<AreaProducao, Map<Mes, Double>> expected = new EnumMap<>(AreaProducao.class);
        Mes[] meses = Mes.values();

        try (InputStream is = ProducaoCalculatorTest.class.getClassLoader().getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            reader.readLine(); // skip title row
            reader.readLine(); // skip header row

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",", -1);
                String areaLabel = parts[0].trim();
                if (areaLabel.isEmpty() || areaLabel.equals("Multiplicador")) continue;

                try {
                    AreaProducao area = AreaProducao.fromLabel(areaLabel);
                    Map<Mes, Double> valores = new EnumMap<>(Mes.class);
                    for (int i = 0; i < 12 && i + 1 < parts.length; i++) {
                        String val = parts[i + 1].trim();
                        if (!val.isEmpty()) {
                            valores.put(meses[i], Double.parseDouble(val));
                        }
                    }
                    expected.put(area, valores);
                } catch (IllegalArgumentException e) {
                    // skip
                }
            }
        }

        return expected;
    }
}
