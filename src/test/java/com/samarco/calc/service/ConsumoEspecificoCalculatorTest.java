package com.samarco.calc.service;

import com.samarco.calc.input.ConsumoEspecificoInput;
import com.samarco.calc.model.AreaConsumoEspecifico;
import com.samarco.calc.model.Mes;
import com.samarco.calc.util.CsvLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste do Cálculo 02 — Consumo Específico.
 * Verifica que o pass-through mantém os valores intactos.
 */
class ConsumoEspecificoCalculatorTest {

    private static final double TOLERANCIA = 0.0001;

    private static ConsumoEspecificoInput input;

    @BeforeAll
    static void setUp() throws Exception {
        input = CsvLoader.loadConsumoEspecifico("fixtures/consumo_especifico.csv");
    }

    @Test
    void deveRetornarValoresIdenticos() {
        ConsumoEspecificoCalculator calculator = new ConsumoEspecificoCalculator();
        Map<AreaConsumoEspecifico, Map<Mes, Double>> resultado = calculator.calcular(input);

        assertNotNull(resultado);

        for (Map.Entry<AreaConsumoEspecifico, Map<Mes, Double>> entry : input.consumoEspecifico().entrySet()) {
            AreaConsumoEspecifico area = entry.getKey();
            assertTrue(resultado.containsKey(area),
                    "Área não encontrada: " + area.getLabel());

            for (Mes mes : Mes.values()) {
                Double expected = entry.getValue().get(mes);
                Double actual = resultado.get(area).get(mes);
                if (expected == null) continue;

                assertEquals(expected, actual, TOLERANCIA,
                        String.format("Divergência em %s %s", area.getLabel(), mes.getLabel()));
            }
        }
    }

    @Test
    void filtragemGermanoJaneiroDeve327820() {
        // Ref: Aba 6, C3 = 3.2782
        ConsumoEspecificoCalculator calculator = new ConsumoEspecificoCalculator();
        Map<AreaConsumoEspecifico, Map<Mes, Double>> resultado = calculator.calcular(input);

        assertEquals(3.2782,
                resultado.get(AreaConsumoEspecifico.FILTRAGEM_GERMANO).get(Mes.JAN),
                TOLERANCIA);
    }

    @Test
    void deveRejeitarInputNulo() {
        ConsumoEspecificoCalculator calculator = new ConsumoEspecificoCalculator();
        assertThrows(IllegalArgumentException.class, () -> calculator.calcular(null));
    }
}
