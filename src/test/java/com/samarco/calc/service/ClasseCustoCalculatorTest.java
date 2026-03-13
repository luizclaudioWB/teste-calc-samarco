package com.samarco.calc.service;

import com.samarco.calc.input.CalendarioInput;
import com.samarco.calc.input.ConsumoEspecificoInput;
import com.samarco.calc.input.DemandaContratadaInput;
import com.samarco.calc.input.PlanejamentoGeracaoInput;
import com.samarco.calc.input.PlanejamentoProducaoInput;
import com.samarco.calc.input.TarifasDistribuidorasInput;
import com.samarco.calc.model.AreaConsumoEspecifico;
import com.samarco.calc.model.AreaProducao;
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
 * Teste do Cálculo — Distribuição de Classe de Custo por Estado.
 * Ref: Aba "14Distrib de Classe de Custo"
 *
 * Tarifas e demandas são hardcoded conforme Aba 2 (tarifas_distribuidoras.csv) e Aba 10.
 * Constrói a cadeia completa (Calc 01 → 02 → 03 → 04 → 13) antes de executar Calc 14.
 */
class ClasseCustoCalculatorTest {

    // Tolerância R$ — aceita diferença de até R$ 1.00 por acúmulo de precisão
    private static final BigDecimal TOLERANCIA_RS = new BigDecimal("1.0");

    private static ConsumoAreaResult consumoArea;
    private static GeracaoPropriaResult geracaoPropria;
    private static DistribuicaoCargaResult distribuicaoCarga;
    private static TarifasDistribuidorasInput tarifas;
    private static DemandaContratadaInput demanda;

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

        // Cálculo 13 — Distribuição de Carga
        distribuicaoCarga = new DistribuicaoCargaCalculator().calcular(consumoArea, geracaoPropria);

        // Inputs hardcoded: Tarifas Distribuidoras (Aba 2) e Demanda Contratada (Aba 10)
        tarifas = buildTarifasDistribuidoras();
        demanda = buildDemandaContratada();
    }

    // -------------------------------------------------------------------------
    // Builders hardcoded — valores extraídos de tarifas_distribuidoras.csv (Aba 2)
    // -------------------------------------------------------------------------

    /**
     * Monta TarifasDistribuidorasInput com valores hardcoded de Aba 2.
     * Ref: tarifas_distribuidoras.csv
     */
    private static TarifasDistribuidorasInput buildTarifasDistribuidoras() {
        // TUSD FIO Ponta (R$/kW)
        // Germano: Jan-Jun=6.84586757688229, Jul-Dez=7.50258483563096
        Map<Mes, BigDecimal> tusdFioPontaGermano = buildPeriodo(
                new BigDecimal("6.84586757688229"), 1, 6,
                new BigDecimal("7.50258483563096"), 7, 12);

        // Matipó: Jan-Jun=15.5738709677419, Jul-Dez=29.665248361175
        Map<Mes, BigDecimal> tusdFioPontaMatipo = buildPeriodo(
                new BigDecimal("15.5738709677419"), 1, 6,
                new BigDecimal("29.665248361175"), 7, 12);

        // UBU: Jan-Jul=20.4845187200415, Ago-Dez=20.117692172749
        Map<Mes, BigDecimal> tusdFioPontaUbu = buildPeriodo(
                new BigDecimal("20.4845187200415"), 1, 7,
                new BigDecimal("20.117692172749"), 8, 12);

        Map<Unidade, Map<Mes, BigDecimal>> tusdFioPonta = new EnumMap<>(Unidade.class);
        tusdFioPonta.put(Unidade.GERMANO, tusdFioPontaGermano);
        tusdFioPonta.put(Unidade.MATIPO, tusdFioPontaMatipo);
        tusdFioPonta.put(Unidade.UBU, tusdFioPontaUbu);

        // TUSD FIO Fora Ponta (R$/kW)
        // Germano: Jan-Jun=6.83333112407211, Jul-Dez=7.54115853658537
        Map<Mes, BigDecimal> tusdFioFPGermano = buildPeriodo(
                new BigDecimal("6.83333112407211"), 1, 6,
                new BigDecimal("7.54115853658537"), 7, 12);

        // Matipó: Jan-Jun=8.37241935483871, Jul-Dez=11.691941870904
        Map<Mes, BigDecimal> tusdFioFPMatipo = buildPeriodo(
                new BigDecimal("8.37241935483871"), 1, 6,
                new BigDecimal("11.691941870904"), 7, 12);

        // UBU: Jan-Jun=8.37241935483871, Jul=11.691941870904, Ago-Dez=8.54284214138739
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

        // TUSD Encargo (R$/MWh)
        // Germano: Jan-May=64.1480646871686, Jun-Dez=58.4970174973489
        Map<Mes, BigDecimal> tusdEncGermano = buildPeriodo(
                new BigDecimal("64.1480646871686"), 1, 5,
                new BigDecimal("58.4970174973489"), 6, 12);

        // Matipó: Jan-Jun=81.1773387096774, Jul-Dez=82.3855494897628
        Map<Mes, BigDecimal> tusdEncMatipo = buildPeriodo(
                new BigDecimal("81.1773387096774"), 1, 6,
                new BigDecimal("82.3855494897628"), 7, 12);

        // UBU: Jan-Jul=86.8299002461459, Ago-Dez=83.2161020764466
        Map<Mes, BigDecimal> tusdEncUbu = buildPeriodo(
                new BigDecimal("86.8299002461459"), 1, 7,
                new BigDecimal("83.2161020764466"), 8, 12);

        Map<Unidade, Map<Mes, BigDecimal>> tusdEncargo = new EnumMap<>(Unidade.class);
        tusdEncargo.put(Unidade.GERMANO, tusdEncGermano);
        tusdEncargo.put(Unidade.MATIPO, tusdEncMatipo);
        tusdEncargo.put(Unidade.UBU, tusdEncUbu);

        // Desconto Auto-Produção (R$/MWh)
        // Germano: Jan-Jun=66.52, Jul-Dez=60.66
        Map<Mes, BigDecimal> descontoGermano = buildPeriodo(
                new BigDecimal("66.52"), 1, 6,
                new BigDecimal("60.66"), 7, 12);

        // UBU: Jan-Jul=77.55, Ago-Dez=78.620665438016
        Map<Mes, BigDecimal> descontoUbu = buildPeriodo(
                new BigDecimal("77.55"), 1, 7,
                new BigDecimal("78.620665438016"), 8, 12);

        // Matipó não tem desconto auto-produção relevante — usa zero
        Map<Mes, BigDecimal> descontoMatipo = buildConstante(BigDecimal.ZERO);

        Map<Unidade, Map<Mes, BigDecimal>> descontoAutoProducao = new EnumMap<>(Unidade.class);
        descontoAutoProducao.put(Unidade.GERMANO, descontoGermano);
        descontoAutoProducao.put(Unidade.MATIPO, descontoMatipo);
        descontoAutoProducao.put(Unidade.UBU, descontoUbu);

        // PMIX (R$/MWh)
        // MG: 160.998461751314 todos os meses
        // ES: 161.245095423521 todos os meses
        Map<Mes, BigDecimal> pmixMG = buildConstante(new BigDecimal("160.998461751314"));
        Map<Mes, BigDecimal> pmixES = buildConstante(new BigDecimal("161.245095423521"));

        return new TarifasDistribuidorasInput(
                tusdFioPonta, tusdFioForaPonta, tusdEncargo, descontoAutoProducao, pmixMG, pmixES);
    }

    /**
     * Monta DemandaContratadaInput com valores hardcoded de Aba 10.
     * Germano: Ponta/FP = 120000 kW
     * Matipó:  Ponta/FP = 11000 kW
     * UBU:     Ponta/FP = 88000 kW
     */
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

    // -------------------------------------------------------------------------
    // Helpers para construção de mapas de tarifas
    // -------------------------------------------------------------------------

    /** Preenche mapa com valor1 nos meses [inicioA..fimA] e valor2 nos meses [inicioB..fimB]. */
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

    /** Preenche mapa com mesmo valor em todos os meses. */
    private static Map<Mes, BigDecimal> buildConstante(BigDecimal valor) {
        Map<Mes, BigDecimal> mapa = new EnumMap<>(Mes.class);
        for (Mes mes : Mes.values()) {
            mapa.put(mes, valor);
        }
        return mapa;
    }

    /** Preenche mapa de Double com mesmo valor em todos os meses. */
    private static Map<Mes, Double> buildConstanteDouble(double valor) {
        Map<Mes, Double> mapa = new EnumMap<>(Mes.class);
        for (Mes mes : Mes.values()) {
            mapa.put(mes, valor);
        }
        return mapa;
    }

    // -------------------------------------------------------------------------
    // Testes
    // -------------------------------------------------------------------------

    @Test
    void tusdFioGermanoJaneiro() {
        // Ref: Aba 14, row 5 Jan = 1641503.84411453 R$
        // = demandaPonta × tarifaPonta + demandaFP × tarifaFP
        // = 120000 × 6.84586757688229 + 120000 × 6.83333112407211
        ClasseCustoResult resultado = new ClasseCustoCalculator()
                .calcular(tarifas, demanda, consumoArea, geracaoPropria, distribuicaoCarga);

        assertBigDecimalClose(new BigDecimal("1641503.84411453"),
                resultado.tusdFioGermano().get(Mes.JAN),
                TOLERANCIA_RS,
                "TUSD FIO Germano Jan");
    }

    @Test
    void consumoEnergiaMGJaneiro() {
        // Ref: Aba 14, row 11 Jan = 7563933.18085522 R$
        // = qtdeCompradaMG × pmixMG
        ClasseCustoResult resultado = new ClasseCustoCalculator()
                .calcular(tarifas, demanda, consumoArea, geracaoPropria, distribuicaoCarga);

        assertBigDecimalClose(new BigDecimal("7563933.18085522"),
                resultado.consumoEnergiaMG().get(Mes.JAN),
                TOLERANCIA_RS,
                "Consumo Energia MG Jan");
    }

    @Test
    void totalTusdEncargoMGJaneiro() {
        // Ref: Aba 14, row 10 Jan = 2980747.55925553 R$
        ClasseCustoResult resultado = new ClasseCustoCalculator()
                .calcular(tarifas, demanda, consumoArea, geracaoPropria, distribuicaoCarga);

        assertBigDecimalClose(new BigDecimal("2980747.55925553"),
                resultado.totalTusdEncargoMG().get(Mes.JAN),
                TOLERANCIA_RS,
                "Total TUSD Encargo MG Jan");
    }

    @Test
    void totalTusdEncargoESJaneiro() {
        // Ref: Aba 14, row 19 Jan — encargo UBU líquido de PROINFA
        ClasseCustoResult resultado = new ClasseCustoCalculator()
                .calcular(tarifas, demanda, consumoArea, geracaoPropria, distribuicaoCarga);

        // Encargo UBU = consumoMWhUBU × tarifaEncUBU - devolPROINFA
        // O valor deve ser positivo (UBU cobre mais que Muniz Freire)
        BigDecimal totalEncES = resultado.totalTusdEncargoES().get(Mes.JAN);
        assertNotNull(totalEncES, "Total Encargo ES Jan não pode ser nulo");
        assertTrue(totalEncES.compareTo(BigDecimal.ZERO) >= 0,
                "Total Encargo ES deve ser >= 0 em Jan (líquido de PROINFA)");
    }

    @Test
    void totalTusdFioMGDeveSerSomaGermanoMatipo() {
        // Ref: Aba 14, row 7 = row 5 + row 6
        ClasseCustoResult resultado = new ClasseCustoCalculator()
                .calcular(tarifas, demanda, consumoArea, geracaoPropria, distribuicaoCarga);

        for (Mes mes : Mes.values()) {
            BigDecimal esperado = resultado.tusdFioGermano().get(mes)
                    .add(resultado.tusdFioMatipo().get(mes));
            assertEquals(0, esperado.compareTo(resultado.totalTusdFioMG().get(mes)),
                    "Total TUSD FIO MG = Germano + Matipó em " + mes.getLabel());
        }
    }

    @Test
    void totalTusdFioESDeveSerUbu() {
        // Ref: Aba 14, row 17 = row 16 (só UBU em ES)
        ClasseCustoResult resultado = new ClasseCustoCalculator()
                .calcular(tarifas, demanda, consumoArea, geracaoPropria, distribuicaoCarga);

        for (Mes mes : Mes.values()) {
            assertEquals(0, resultado.tusdFioUbu().get(mes).compareTo(resultado.totalTusdFioES().get(mes)),
                    "Total TUSD FIO ES deve igual UBU em " + mes.getLabel());
        }
    }

    @Test
    void totalConsumoMGDeveSerIgualConsumoEnergiaMG() {
        // Ref: Aba 14, row 12 = row 11 (somente consumo de energia em MG)
        ClasseCustoResult resultado = new ClasseCustoCalculator()
                .calcular(tarifas, demanda, consumoArea, geracaoPropria, distribuicaoCarga);

        for (Mes mes : Mes.values()) {
            assertEquals(0, resultado.consumoEnergiaMG().get(mes).compareTo(resultado.totalConsumoMG().get(mes)),
                    "Total Consumo MG = Consumo Energia MG em " + mes.getLabel());
        }
    }

    @Test
    void totalConsumoESDeveSerIgualConsumoEnergiaES() {
        // Ref: Aba 14, row 21 = row 20
        ClasseCustoResult resultado = new ClasseCustoCalculator()
                .calcular(tarifas, demanda, consumoArea, geracaoPropria, distribuicaoCarga);

        for (Mes mes : Mes.values()) {
            assertEquals(0, resultado.consumoEnergiaES().get(mes).compareTo(resultado.totalConsumoES().get(mes)),
                    "Total Consumo ES = Consumo Energia ES em " + mes.getLabel());
        }
    }

    @Test
    void resultadoDeveTer12MesesParaTodosOsCampos() {
        ClasseCustoResult resultado = new ClasseCustoCalculator()
                .calcular(tarifas, demanda, consumoArea, geracaoPropria, distribuicaoCarga);

        assertEquals(12, resultado.tusdFioGermano().size(), "tusdFioGermano deve ter 12 meses");
        assertEquals(12, resultado.consumoEnergiaMG().size(), "consumoEnergiaMG deve ter 12 meses");
        assertEquals(12, resultado.totalTusdEncargoMG().size(), "totalTusdEncargoMG deve ter 12 meses");
        assertEquals(12, resultado.tusdFioUbu().size(), "tusdFioUbu deve ter 12 meses");
        assertEquals(12, resultado.consumoEnergiaES().size(), "consumoEnergiaES deve ter 12 meses");
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
