package com.samarco.calc.service;

import com.samarco.calc.input.CalendarioInput;
import com.samarco.calc.input.ConsumoEspecificoInput;
import com.samarco.calc.input.DemandaContratadaInput;
import com.samarco.calc.input.PlanejamentoGeracaoInput;
import com.samarco.calc.input.PlanejamentoProducaoInput;
import com.samarco.calc.input.TarifasDistribuidorasInput;
import com.samarco.calc.model.AreaConsumoEspecifico;
import com.samarco.calc.model.AreaProducao;
import com.samarco.calc.model.CentroCustosResult;
import com.samarco.calc.model.ClasseCustoResult;
import com.samarco.calc.model.ConsumoAreaResult;
import com.samarco.calc.model.DistribuicaoCargaResult;
import com.samarco.calc.model.GeracaoPropriaResult;
import com.samarco.calc.model.Mes;
import com.samarco.calc.model.Unidade;
import com.samarco.calc.util.CsvLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste do Cálculo — Distribuição por Centro de Custos.
 * Ref: Aba "15Distrib Centro de Custos"
 *
 * Constrói a cadeia completa (Calc 01 → 02 → 03 → 04 → 13 → 14) antes de executar Calc 15.
 */
class CentroCustosCalculatorTest {

    // Tolerância R$ — aceita diferença de até R$ 1.00 por acúmulo de precisão BigDecimal×double
    private static final BigDecimal TOLERANCIA_RS = new BigDecimal("1.0");

    private static ClasseCustoResult classeCusto;
    private static ConsumoAreaResult consumoArea;

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
        GeracaoPropriaResult geracaoPropria = new GeracaoPropriaCalculator().calcular(geracaoInput, calendarioInput);

        // Cálculo 13 — Distribuição de Carga
        DistribuicaoCargaResult distribuicaoCarga = new DistribuicaoCargaCalculator()
                .calcular(consumoArea, geracaoPropria);

        // Cálculo 14 — Classe de Custo
        TarifasDistribuidorasInput tarifas = buildTarifasDistribuidoras();
        DemandaContratadaInput demanda = buildDemandaContratada();
        classeCusto = new ClasseCustoCalculator()
                .calcular(tarifas, demanda, consumoArea, geracaoPropria, distribuicaoCarga);
    }

    // -------------------------------------------------------------------------
    // Testes
    // -------------------------------------------------------------------------

    @Test
    void filtragemMGConsumoJaneiro() {
        // Ref: Aba 15 — Filtragem Germano consumo Jan = 381180.091323001 R$
        // = totalConsumoMG × percentualFiltragemGermano
        CentroCustosResult resultado = new CentroCustosCalculator()
                .calcular(classeCusto, consumoArea);

        assertBigDecimalClose(new BigDecimal("381180.091323001"),
                resultado.consumoMG().get(AreaConsumoEspecifico.FILTRAGEM_GERMANO).get(Mes.JAN),
                TOLERANCIA_RS,
                "Filtragem MG Consumo Jan");
    }

    @Test
    void preparacao2ESEncargoJaneiro() {
        // Ref: Aba 15 — Preparação 2 ES encargo Jan = 871630.235651477 R$
        // = totalTusdEncargoES × percentualPreparacao2
        CentroCustosResult resultado = new CentroCustosCalculator()
                .calcular(classeCusto, consumoArea);

        assertBigDecimalClose(new BigDecimal("871630.235651477"),
                resultado.encargoES().get(AreaConsumoEspecifico.PREPARACAO_2).get(Mes.JAN),
                TOLERANCIA_RS,
                "Preparação 2 ES Encargo Jan");
    }

    @Test
    void totalGeralSamarcoDeveSerSomaDas3Classes() {
        // Ref: Aba 15, row 67 = totalConsumo + totalUsoRede + totalEncargo
        CentroCustosResult resultado = new CentroCustosCalculator()
                .calcular(classeCusto, consumoArea);

        for (Mes mes : Mes.values()) {
            BigDecimal esperado = resultado.totalConsumoSamarco().get(mes)
                    .add(resultado.totalUsoRedeSamarco().get(mes))
                    .add(resultado.totalEncargoSamarco().get(mes));
            BigDecimal diff = esperado.subtract(resultado.totalGeralSamarco().get(mes)).abs();
            assertTrue(diff.compareTo(new BigDecimal("0.001")) <= 0,
                    "Total Geral Samarco = Consumo + Uso de Rede + Encargo em " + mes.getLabel()
                            + " (esperado=" + esperado + ", obtido=" + resultado.totalGeralSamarco().get(mes) + ")");
        }
    }

    @Test
    void todosCCsMGDevemTerConsumoNaoNulo() {
        // Verifica que todos os centros de custo MG foram calculados e têm valores não nulos
        CentroCustosResult resultado = new CentroCustosCalculator()
                .calcular(classeCusto, consumoArea);

        AreaConsumoEspecifico[] ccsMG = {
                AreaConsumoEspecifico.FILTRAGEM_GERMANO,
                AreaConsumoEspecifico.MINERACAO_1,
                AreaConsumoEspecifico.BENEFICIAMENTO_1,
                AreaConsumoEspecifico.BENEFICIAMENTO_2,
                AreaConsumoEspecifico.BENEFICIAMENTO_3,
                AreaConsumoEspecifico.MINERODUTO_1,
                AreaConsumoEspecifico.MINERODUTO_2,
                AreaConsumoEspecifico.MINERODUTO_3
        };

        for (AreaConsumoEspecifico cc : ccsMG) {
            assertNotNull(resultado.consumoMG().get(cc),
                    "consumoMG de " + cc.getLabel() + " não pode ser nulo");
            for (Mes mes : Mes.values()) {
                assertNotNull(resultado.consumoMG().get(cc).get(mes),
                        "consumoMG de " + cc.getLabel() + " em " + mes.getLabel() + " não pode ser nulo");
                assertTrue(resultado.consumoMG().get(cc).get(mes).compareTo(BigDecimal.ZERO) >= 0,
                        "consumoMG de " + cc.getLabel() + " deve ser >= 0 em " + mes.getLabel());
            }
        }
    }

    @Test
    void todosCCsESDevemTerConsumoNaoNulo() {
        // Verifica que todos os centros de custo ES foram calculados e têm valores não nulos
        CentroCustosResult resultado = new CentroCustosCalculator()
                .calcular(classeCusto, consumoArea);

        AreaConsumoEspecifico[] ccsES = {
                AreaConsumoEspecifico.PREPARACAO_1,
                AreaConsumoEspecifico.PREPARACAO_2,
                AreaConsumoEspecifico.USINA_1,
                AreaConsumoEspecifico.USINA_2,
                AreaConsumoEspecifico.USINA_3,
                AreaConsumoEspecifico.USINA_4,
                AreaConsumoEspecifico.ESTOCAGEM
        };

        for (AreaConsumoEspecifico cc : ccsES) {
            assertNotNull(resultado.consumoES().get(cc),
                    "consumoES de " + cc.getLabel() + " não pode ser nulo");
            for (Mes mes : Mes.values()) {
                assertNotNull(resultado.consumoES().get(cc).get(mes),
                        "consumoES de " + cc.getLabel() + " em " + mes.getLabel() + " não pode ser nulo");
                assertTrue(resultado.consumoES().get(cc).get(mes).compareTo(BigDecimal.ZERO) >= 0,
                        "consumoES de " + cc.getLabel() + " deve ser >= 0 em " + mes.getLabel());
            }
        }
    }

    @Test
    void resultadoDeveTer12MesesEmTodosOsMapas() {
        CentroCustosResult resultado = new CentroCustosCalculator()
                .calcular(classeCusto, consumoArea);

        assertEquals(12, resultado.totalGeralSamarco().size(), "totalGeralSamarco deve ter 12 meses");
        assertEquals(12, resultado.totalConsumoSamarco().size(), "totalConsumoSamarco deve ter 12 meses");
        assertEquals(12, resultado.totalUsoRedeSamarco().size(), "totalUsoRedeSamarco deve ter 12 meses");
        assertEquals(12, resultado.totalEncargoSamarco().size(), "totalEncargoSamarco deve ter 12 meses");
    }

    @Test
    void todosCCsMGDevemTer12Meses() {
        CentroCustosResult resultado = new CentroCustosCalculator()
                .calcular(classeCusto, consumoArea);

        AreaConsumoEspecifico[] ccsMG = {
                AreaConsumoEspecifico.FILTRAGEM_GERMANO,
                AreaConsumoEspecifico.MINERACAO_1,
                AreaConsumoEspecifico.BENEFICIAMENTO_1,
                AreaConsumoEspecifico.BENEFICIAMENTO_2,
                AreaConsumoEspecifico.BENEFICIAMENTO_3,
                AreaConsumoEspecifico.MINERODUTO_1,
                AreaConsumoEspecifico.MINERODUTO_2,
                AreaConsumoEspecifico.MINERODUTO_3
        };

        for (AreaConsumoEspecifico cc : ccsMG) {
            assertEquals(12, resultado.consumoMG().get(cc).size(),
                    "consumoMG de " + cc.getLabel() + " deve ter 12 meses");
            assertEquals(12, resultado.usoRedeMG().get(cc).size(),
                    "usoRedeMG de " + cc.getLabel() + " deve ter 12 meses");
            assertEquals(12, resultado.encargoMG().get(cc).size(),
                    "encargoMG de " + cc.getLabel() + " deve ter 12 meses");
        }
    }

    // -------------------------------------------------------------------------
    // Builders hardcoded (mesmos do ClasseCustoCalculatorTest)
    // -------------------------------------------------------------------------

    private static TarifasDistribuidorasInput buildTarifasDistribuidoras() {
        Map<Mes, BigDecimal> tusdFioPontaGermano = buildPeriodo(
                new BigDecimal("6.84586757688229"), 1, 6,
                new BigDecimal("7.50258483563096"), 7, 12);
        Map<Mes, BigDecimal> tusdFioPontaMatipo = buildPeriodo(
                new BigDecimal("15.5738709677419"), 1, 6,
                new BigDecimal("29.665248361175"), 7, 12);
        Map<Mes, BigDecimal> tusdFioPontaUbu = buildPeriodo(
                new BigDecimal("20.4845187200415"), 1, 7,
                new BigDecimal("20.117692172749"), 8, 12);
        Map<Unidade, Map<Mes, BigDecimal>> tusdFioPonta = new EnumMap<>(Unidade.class);
        tusdFioPonta.put(Unidade.GERMANO, tusdFioPontaGermano);
        tusdFioPonta.put(Unidade.MATIPO, tusdFioPontaMatipo);
        tusdFioPonta.put(Unidade.UBU, tusdFioPontaUbu);

        Map<Mes, BigDecimal> tusdFioFPGermano = buildPeriodo(
                new BigDecimal("6.83333112407211"), 1, 6,
                new BigDecimal("7.54115853658537"), 7, 12);
        Map<Mes, BigDecimal> tusdFioFPMatipo = buildPeriodo(
                new BigDecimal("8.37241935483871"), 1, 6,
                new BigDecimal("11.691941870904"), 7, 12);
        Map<Mes, BigDecimal> tusdFioFPUbu = new EnumMap<>(Mes.class);
        for (Mes mes : Mes.values()) {
            int n = mes.getNumero();
            if (n >= 1 && n <= 6) {
                tusdFioFPUbu.put(mes, new BigDecimal("8.37241935483871"));
            } else if (n == 7) {
                tusdFioFPUbu.put(mes, new BigDecimal("11.691941870904"));
            } else {
                tusdFioFPUbu.put(mes, new BigDecimal("8.54284214138739"));
            }
        }
        Map<Unidade, Map<Mes, BigDecimal>> tusdFioForaPonta = new EnumMap<>(Unidade.class);
        tusdFioForaPonta.put(Unidade.GERMANO, tusdFioFPGermano);
        tusdFioForaPonta.put(Unidade.MATIPO, tusdFioFPMatipo);
        tusdFioForaPonta.put(Unidade.UBU, tusdFioFPUbu);

        Map<Mes, BigDecimal> tusdEncGermano = buildPeriodo(
                new BigDecimal("64.1480646871686"), 1, 5,
                new BigDecimal("58.4970174973489"), 6, 12);
        Map<Mes, BigDecimal> tusdEncMatipo = buildPeriodo(
                new BigDecimal("81.1773387096774"), 1, 6,
                new BigDecimal("82.3855494897628"), 7, 12);
        Map<Mes, BigDecimal> tusdEncUbu = buildPeriodo(
                new BigDecimal("86.8299002461459"), 1, 7,
                new BigDecimal("83.2161020764466"), 8, 12);
        Map<Unidade, Map<Mes, BigDecimal>> tusdEncargo = new EnumMap<>(Unidade.class);
        tusdEncargo.put(Unidade.GERMANO, tusdEncGermano);
        tusdEncargo.put(Unidade.MATIPO, tusdEncMatipo);
        tusdEncargo.put(Unidade.UBU, tusdEncUbu);

        Map<Mes, BigDecimal> descontoGermano = buildPeriodo(
                new BigDecimal("66.52"), 1, 6,
                new BigDecimal("60.66"), 7, 12);
        Map<Mes, BigDecimal> descontoUbu = buildPeriodo(
                new BigDecimal("77.55"), 1, 7,
                new BigDecimal("78.620665438016"), 8, 12);
        Map<Unidade, Map<Mes, BigDecimal>> descontoAutoProducao = new EnumMap<>(Unidade.class);
        descontoAutoProducao.put(Unidade.GERMANO, descontoGermano);
        descontoAutoProducao.put(Unidade.MATIPO, buildConstante(BigDecimal.ZERO));
        descontoAutoProducao.put(Unidade.UBU, descontoUbu);

        Map<Mes, BigDecimal> pmixMG = buildConstante(new BigDecimal("160.998461751314"));
        Map<Mes, BigDecimal> pmixES = buildConstante(new BigDecimal("161.245095423521"));

        return new TarifasDistribuidorasInput(
                tusdFioPonta, tusdFioForaPonta, tusdEncargo, descontoAutoProducao, pmixMG, pmixES);
    }

    private static DemandaContratadaInput buildDemandaContratada() {
        Map<Unidade, Map<Mes, Double>> ponta = new EnumMap<>(Unidade.class);
        Map<Unidade, Map<Mes, Double>> foraPonta = new EnumMap<>(Unidade.class);
        ponta.put(Unidade.GERMANO, buildConstanteDouble(120000.0));
        ponta.put(Unidade.MATIPO, buildConstanteDouble(11000.0));
        ponta.put(Unidade.UBU, buildConstanteDouble(88000.0));
        foraPonta.put(Unidade.GERMANO, buildConstanteDouble(120000.0));
        foraPonta.put(Unidade.MATIPO, buildConstanteDouble(11000.0));
        foraPonta.put(Unidade.UBU, buildConstanteDouble(88000.0));
        return new DemandaContratadaInput(ponta, foraPonta);
    }

    private static Map<Mes, BigDecimal> buildPeriodo(BigDecimal valor1, int inicioA, int fimA,
                                                      BigDecimal valor2, int inicioB, int fimB) {
        Map<Mes, BigDecimal> mapa = new EnumMap<>(Mes.class);
        for (Mes mes : Mes.values()) {
            int n = mes.getNumero();
            if (n >= inicioA && n <= fimA) {
                mapa.put(mes, valor1);
            } else if (n >= inicioB && n <= fimB) {
                mapa.put(mes, valor2);
            }
        }
        return mapa;
    }

    private static Map<Mes, BigDecimal> buildConstante(BigDecimal valor) {
        Map<Mes, BigDecimal> mapa = new EnumMap<>(Mes.class);
        for (Mes mes : Mes.values()) {
            mapa.put(mes, valor);
        }
        return mapa;
    }

    private static Map<Mes, Double> buildConstanteDouble(double valor) {
        Map<Mes, Double> mapa = new EnumMap<>(Mes.class);
        for (Mes mes : Mes.values()) {
            mapa.put(mes, valor);
        }
        return mapa;
    }

    // -------------------------------------------------------------------------
    // Helper de asserção BigDecimal com tolerância
    // -------------------------------------------------------------------------

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
