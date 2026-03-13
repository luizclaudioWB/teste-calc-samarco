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
import com.samarco.calc.model.Unidade;
import com.samarco.calc.util.CsvLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste do Cálculo — Resumo Geral.
 * Ref: Aba "16Resumo GERAL"
 *
 * Constrói a cadeia completa (Calc 01 → 02 → 03 → 04 → 05 → 13 → 14 → 15) antes de executar Calc 16.
 * DespesasFixasInput é hardcoded com os valores das rows 8-15 da Aba 16.
 */
class ResumoGeralCalculatorTest {

    // Tolerância R$ — aceita até R$ 10 por conta do acúmulo de precisão em toda a cadeia
    private static final BigDecimal TOLERANCIA_RS = new BigDecimal("10.0");
    // Tolerância tms — aceita 0.01 tms
    private static final double TOLERANCIA_TMS = 0.01;

    private static CentroCustosResult centroCustos;
    private static EncargosResult encargos;
    private static DespesasFixasInput despesasFixas;
    private static Map<AreaProducao, Map<Mes, Double>> producaoTms;

    @BeforeAll
    static void setUp() throws Exception {
        // Cálculo 01 — Produção
        PlanejamentoProducaoInput prodInput = CsvLoader.loadProducao("fixtures/planejamento_producao.csv");
        producaoTms = new ProducaoCalculator().calcular(prodInput);

        // Cálculo 02 — Consumo Específico
        ConsumoEspecificoInput ceInput = CsvLoader.loadConsumoEspecifico("fixtures/consumo_especifico.csv");
        Map<AreaConsumoEspecifico, Map<Mes, Double>> consumoEsp = new ConsumoEspecificoCalculator().calcular(ceInput);

        // Cálculo 03 — Consumo Área
        ConsumoAreaResult consumoArea = new ConsumoAreaCalculator().calcular(producaoTms, consumoEsp);

        // Cálculo 04 — Geração Própria
        PlanejamentoGeracaoInput geracaoInput = CsvLoader.loadGeracao("fixtures/planejamento_geracao.csv");
        CalendarioInput calendarioInput = CsvLoader.loadCalendario("fixtures/calendario_input.csv");
        GeracaoPropriaResult geracaoPropria = new GeracaoPropriaCalculator().calcular(geracaoInput, calendarioInput);

        // Cálculo 05 — Encargos ESS/EER
        TarifasEncargosInput tarifasEncargos = CsvLoader.loadTarifas("fixtures/tarifas_encargos.csv");
        encargos = new EncargosEssEerCalculator().calcular(tarifasEncargos, consumoArea, geracaoPropria);

        // Cálculo 13 — Distribuição de Carga
        DistribuicaoCargaResult distribuicaoCarga = new DistribuicaoCargaCalculator()
                .calcular(consumoArea, geracaoPropria);

        // Cálculo 14 — Classe de Custo
        TarifasDistribuidorasInput tarifasDist = buildTarifasDistribuidoras();
        DemandaContratadaInput demanda = buildDemandaContratada();
        ClasseCustoResult classeCusto = new ClasseCustoCalculator()
                .calcular(tarifasDist, demanda, consumoArea, geracaoPropria, distribuicaoCarga);

        // Cálculo 15 — Centro de Custos
        centroCustos = new CentroCustosCalculator().calcular(classeCusto, consumoArea);

        // Despesas fixas hardcoded — Ref: Aba 16, rows 8-15
        despesasFixas = buildDespesasFixas();
    }

    // -------------------------------------------------------------------------
    // Testes
    // -------------------------------------------------------------------------

    @Test
    void totalGeralJaneiro() {
        // Ref: Aba 16, row 16 Jan = 28665952.0288249 R$
        ResumoGeralResult resultado = new ResumoGeralCalculator()
                .calcular(centroCustos, encargos, despesasFixas, producaoTms);

        assertBigDecimalClose(new BigDecimal("28665952.0288249"),
                resultado.totalGeral().get(Mes.JAN),
                TOLERANCIA_RS,
                "Total Geral Jan");
    }

    @Test
    void producaoTotalJaneiro() {
        // Ref: Aba 16, row 23 Jan = 1597010.37656729 tms
        ResumoGeralResult resultado = new ResumoGeralCalculator()
                .calcular(centroCustos, encargos, despesasFixas, producaoTms);

        assertEquals(1597010.37656729, resultado.producaoTotal().get(Mes.JAN), TOLERANCIA_TMS,
                "Produção Total Jan");
    }

    @Test
    void custoEspecificoJaneiro() {
        // Ref: Aba 16, row 24 Jan = totalGeral / producaoTotal
        ResumoGeralResult resultado = new ResumoGeralCalculator()
                .calcular(centroCustos, encargos, despesasFixas, producaoTms);

        BigDecimal totalGeral = resultado.totalGeral().get(Mes.JAN);
        double producaoTotal = resultado.producaoTotal().get(Mes.JAN);
        BigDecimal custoEsperado = totalGeral.divide(
                BigDecimal.valueOf(producaoTotal), 15, java.math.RoundingMode.HALF_UP);

        BigDecimal diff = custoEsperado.subtract(resultado.custoEspecifico().get(Mes.JAN)).abs();
        assertTrue(diff.compareTo(new BigDecimal("0.01")) <= 0,
                "Custo Específico Jan deve ser totalGeral / producaoTotal");
    }

    @Test
    void custoFixoJaneiro() {
        // Ref: Aba 16, row 27 Jan = usoRede + despesas fixas
        ResumoGeralResult resultado = new ResumoGeralCalculator()
                .calcular(centroCustos, encargos, despesasFixas, producaoTms);

        BigDecimal custoFixo = resultado.custoFixo().get(Mes.JAN);
        assertNotNull(custoFixo, "Custo Fixo Jan não pode ser nulo");
        assertTrue(custoFixo.compareTo(BigDecimal.ZERO) > 0,
                "Custo Fixo Jan deve ser positivo");
    }

    @Test
    void custoVariavelJaneiro() {
        // Ref: Aba 16, row 29 Jan = consumo + encargo + EER + ESS
        ResumoGeralResult resultado = new ResumoGeralCalculator()
                .calcular(centroCustos, encargos, despesasFixas, producaoTms);

        // Custo variável = totalGeral - custoFixo
        BigDecimal totalGeral = resultado.totalGeral().get(Mes.JAN);
        BigDecimal custoFixo = resultado.custoFixo().get(Mes.JAN);
        BigDecimal custoVariavelEsperado = totalGeral.subtract(custoFixo);

        assertBigDecimalClose(custoVariavelEsperado,
                resultado.custoVariavel().get(Mes.JAN),
                TOLERANCIA_RS,
                "Custo Variável Jan");
    }

    @Test
    void custoFixoMaisVariavelDeveIgualTotalGeral() {
        // Invariante: custoFixo + custoVariavel = totalGeral (para todos os meses)
        ResumoGeralResult resultado = new ResumoGeralCalculator()
                .calcular(centroCustos, encargos, despesasFixas, producaoTms);

        for (Mes mes : Mes.values()) {
            BigDecimal soma = resultado.custoFixo().get(mes).add(resultado.custoVariavel().get(mes));
            BigDecimal diff = resultado.totalGeral().get(mes).subtract(soma).abs();
            assertTrue(diff.compareTo(TOLERANCIA_RS) <= 0,
                    "custoFixo + custoVariavel deve = totalGeral em " + mes.getLabel());
        }
    }

    @Test
    void producaoPelotasDeveSerSomaDas4Usinas() {
        // Ref: Aba 16, row 20 = SUM(Usina1 + Usina2 + Usina3 + Usina4)
        ResumoGeralResult resultado = new ResumoGeralCalculator()
                .calcular(centroCustos, encargos, despesasFixas, producaoTms);

        for (Mes mes : Mes.values()) {
            double u1 = getProducao(AreaProducao.USINA_1, mes);
            double u2 = getProducao(AreaProducao.USINA_2, mes);
            double u3 = getProducao(AreaProducao.USINA_3, mes);
            double u4 = getProducao(AreaProducao.USINA_4, mes);
            double esperado = u1 + u2 + u3 + u4;

            assertEquals(esperado, resultado.producaoPelotas().get(mes), TOLERANCIA_TMS,
                    "Produção Pelotas = Soma Usinas 1-4 em " + mes.getLabel());
        }
    }

    @Test
    void todosOsMesesTotalGeralDevemTerValorPositivo() {
        ResumoGeralResult resultado = new ResumoGeralCalculator()
                .calcular(centroCustos, encargos, despesasFixas, producaoTms);

        for (Mes mes : Mes.values()) {
            assertTrue(resultado.totalGeral().get(mes).compareTo(BigDecimal.ZERO) > 0,
                    "Total Geral deve ser positivo em " + mes.getLabel());
        }
    }

    @Test
    void resultadoDeveTer12MesesEmTodosOsMapas() {
        ResumoGeralResult resultado = new ResumoGeralCalculator()
                .calcular(centroCustos, encargos, despesasFixas, producaoTms);

        assertEquals(12, resultado.totalGeral().size(), "totalGeral deve ter 12 meses");
        assertEquals(12, resultado.producaoTotal().size(), "producaoTotal deve ter 12 meses");
        assertEquals(12, resultado.custoEspecifico().size(), "custoEspecifico deve ter 12 meses");
        assertEquals(12, resultado.custoFixo().size(), "custoFixo deve ter 12 meses");
        assertEquals(12, resultado.custoVariavel().size(), "custoVariavel deve ter 12 meses");
    }

    // -------------------------------------------------------------------------
    // Builder de DespesasFixasInput hardcoded — Ref: Aba 16, rows 8-15
    // -------------------------------------------------------------------------

    /**
     * Despesas fixas hardcoded conforme Aba 16, rows 8-15 (valores em R$ por mês).
     */
    private static DespesasFixasInput buildDespesasFixas() {
        // row 8 — Royalties ANEEL (50620002)
        double[] royalties = {
                241214.438110908, 368746.254946268, 391217.355595486,
                336157.108917813, 347984.38699056,  264024.711657817,
                238093.510726971, 174321.067428341, 151424.985296812,
                141787.801903991, 138058.492899658, 179222.385820984
        };

        // row 9 — Serviço de Transmissão (50620003)
        // Jan-Jun=665774.26, Jul-Dez=719482.69
        double[] transmissao = {
                665774.26, 665774.26, 665774.26,
                665774.26, 665774.26, 665774.26,
                719482.69, 719482.69, 719482.69,
                719482.69, 719482.69, 719482.69
        };

        // row 10 — Operação Hidrelétricas (50620004)
        double[] hidreletricas = {
                1178695.89, 1226507.16, 1371448.00,
                1491723.94, 1540771.14, 1674148.62,
                1521470.84, 1806129.92, 1755558.99,
                1710922.73, 1330109.71, 1234823.80
        };

        // row 11 — Outras Despesas Hidrelétricas (50620099)
        // Constante: 203645.692887151 todos os meses
        double[] outrasHidro = {
                203645.692887151, 203645.692887151, 203645.692887151,
                203645.692887151, 203645.692887151, 203645.692887151,
                203645.692887151, 203645.692887151, 203645.692887151,
                203645.692887151, 203645.692887151, 203645.692887151
        };

        // row 12 — O&M Rede Básica (50630001)
        double[] omRB = {
                334059.96, 334059.96, 334059.96,
                454060.00, 414060.00, 414060.00,
                334059.96, 334059.96, 334059.96,
                334059.96, 334059.96, 334059.96
        };

        // row 13 — Outras Despesas RB (50630099)
        // Constante: 25000 todos os meses
        double[] outrasRB = {
                25000.0, 25000.0, 25000.0,
                25000.0, 25000.0, 25000.0,
                25000.0, 25000.0, 25000.0,
                25000.0, 25000.0, 25000.0
        };

        // row 14 — Energia Administrativa (50610005)
        // Constante: 108333 todos os meses
        double[] energiaAdmin = {
                108333.0, 108333.0, 108333.0,
                108333.0, 108333.0, 108333.0,
                108333.0, 108333.0, 108333.0,
                108333.0, 108333.0, 108333.0
        };

        // row 15 — Outros Custos EE (50610099)
        double[] outrosCustos = {
                772738.24, 235785.28, 235785.28,
                235785.28, 331560.28, 331560.28,
                331560.28, 331560.28, 331560.28,
                711560.28, 331560.28, 331560.28
        };

        return new DespesasFixasInput(
                buildBigDecimalMap(royalties),
                buildBigDecimalMap(transmissao),
                buildBigDecimalMap(hidreletricas),
                buildBigDecimalMap(outrasHidro),
                buildBigDecimalMap(omRB),
                buildBigDecimalMap(outrasRB),
                buildBigDecimalMap(energiaAdmin),
                buildBigDecimalMap(outrosCustos)
        );
    }

    /** Converte array double[12] (Jan=0, Dez=11) para Map<Mes, BigDecimal>. */
    private static Map<Mes, BigDecimal> buildBigDecimalMap(double[] valores) {
        Map<Mes, BigDecimal> mapa = new EnumMap<>(Mes.class);
        for (Mes mes : Mes.values()) {
            mapa.put(mes, BigDecimal.valueOf(valores[mes.getNumero() - 1]));
        }
        return mapa;
    }

    // -------------------------------------------------------------------------
    // Builders hardcoded para Tarifas e Demanda (replicados dos testes anteriores)
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

    /** Acessa producaoTms com null-safety. */
    private double getProducao(AreaProducao area, Mes mes) {
        Map<Mes, Double> valores = producaoTms.get(area);
        if (valores == null || valores.get(mes) == null) return 0.0;
        return valores.get(mes);
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
