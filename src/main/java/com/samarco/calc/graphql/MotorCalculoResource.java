package com.samarco.calc.graphql;

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
import com.samarco.calc.model.Mes;
import com.samarco.calc.model.Unidade;
import com.samarco.calc.service.MotorCalculoOrchestrator;
import com.samarco.calc.service.MotorCalculoOrchestrator.MotorCalculoInput;
import com.samarco.calc.service.MotorCalculoOrchestrator.ResultadoConsolidado;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * API GraphQL para o Motor de Cálculo Samarco.
 * Expõe os resultados de todos os cálculos energéticos (Abas 4-16).
 */
@GraphQLApi
public class MotorCalculoResource {

    @Inject
    MotorCalculoOrchestrator orchestrator;

    @Query("calcularMotorCompleto")
    @Description("Executa todos os cálculos do motor energético e retorna resultado consolidado")
    public MotorCalculoResponse calcularMotorCompleto() {
        MotorCalculoInput input = criarInputPadrao();
        ResultadoConsolidado resultado = orchestrator.executar(input);
        return toResponse(resultado);
    }

    private MotorCalculoResponse toResponse(ResultadoConsolidado resultado) {
        MotorCalculoResponse r = new MotorCalculoResponse();

        // Cálculo 01 — Produção
        r.producaoTms = toAreaMesValues(resultado.producaoTms());

        // Cálculo 02 — Consumo Específico
        r.consumoEspecificoKWhTms = toAreaConsumoMesValues(resultado.consumoEspecifico());

        // Produção por tipo (do Resumo Geral)
        if (resultado.resumoGeral() != null) {
            var rg = resultado.resumoGeral();
            r.producaoPelotas = toMesValues(rg.producaoPelotas());
            r.producaoPelletFeed = toMesValues(rg.producaoPelletFeed());
            r.producaoPSC = toMesValues(rg.producaoPscPsm());
            r.producaoPSM = toMesValues(zeroMesMap());
        }

        // Cálculo 03 — Consumo Área
        r.consumoAreaTotalMWh = toMesValues(resultado.consumoArea().totalMWh());
        r.consumoMG_MWh = toMesValues(resultado.consumoArea().consumoMG_MWh());
        r.consumoES_MWh = toMesValues(resultado.consumoArea().consumoES_MWh());
        r.percentualMG = toMesValues(resultado.consumoArea().percentualMG());
        r.percentualES = toMesValues(resultado.consumoArea().percentualES());

        // Cálculo 04 — Geração Própria
        r.geracaoGuilmanMWh = toMesValues(resultado.geracaoPropria().guilmanMWh());
        r.geracaoMunizFreireMWh = toMesValues(resultado.geracaoPropria().munizFreireMWh());
        r.geracaoTotalMWh = toMesValues(resultado.geracaoPropria().totalMWh());
        r.calendario = new ArrayList<>();
        for (Mes mes : Mes.values()) {
            var cal = resultado.geracaoPropria().calendario().get(mes);
            CalendarioMesResponse cm = new CalendarioMesResponse();
            cm.mes = mes.getLabel();
            cm.diasNoMes = cal.diasNoMes();
            cm.diasUteis = cal.diasUteis();
            cm.horasPonta = cal.horasPonta();
            cm.horasForaPonta = cal.horasForaPonta();
            cm.totalHoras = cal.totalHoras();
            r.calendario.add(cm);
        }

        // Cálculo 05 — Encargos ESS/EER
        r.consumoSamarcoMWh = toBigDecimalMesValues(resultado.encargos().consumoSamarcoMWh());
        r.valorTotalEER = toBigDecimalMesValues(resultado.encargos().valorTotalEer());
        r.valorTotalESS = toBigDecimalMesValues(resultado.encargos().valorTotalEss());
        r.mgEER = toBigDecimalMesValues(resultado.encargos().mgEer());
        r.mgESS = toBigDecimalMesValues(resultado.encargos().mgEss());
        r.mgTotal = toBigDecimalMesValues(resultado.encargos().mgTotal());
        r.esEER = toBigDecimalMesValues(resultado.encargos().esEer());
        r.esESS = toBigDecimalMesValues(resultado.encargos().esEss());
        r.esTotal = toBigDecimalMesValues(resultado.encargos().esTotal());

        // Cálculo 13 — Distribuição de Carga
        if (resultado.distribuicaoCarga() != null) {
            var dc = resultado.distribuicaoCarga();
            r.distribCargaNecCompraMG = toMesValues(dc.necessidadeCompraMG());
            r.distribCargaNecCompraES = toMesValues(dc.necessidadeCompraES());
            r.distribCargaNecCompraSamarco = toMesValues(dc.necessidadeCompraSamarco());
            r.distribCargaPerdasMG = toMesValues(dc.perdasMG_MWh());
            r.distribCargaPerdasES = toMesValues(dc.perdasES_MWh());
        }

        // Cálculo 14 — Classe de Custo
        if (resultado.classeCusto() != null) {
            var cc = resultado.classeCusto();
            r.classeCustoTusdFioMG = toBigDecimalMesValues(cc.totalTusdFioMG());
            r.classeCustoTusdFioES = toBigDecimalMesValues(cc.totalTusdFioES());
            r.classeCustoEncargoMG = toBigDecimalMesValues(cc.totalTusdEncargoMG());
            r.classeCustoEncargoES = toBigDecimalMesValues(cc.totalTusdEncargoES());
            r.classeCustoConsumoMG = toBigDecimalMesValues(cc.totalConsumoMG());
            r.classeCustoConsumoES = toBigDecimalMesValues(cc.totalConsumoES());
        }

        // Cálculo 16 — Resumo Geral
        if (resultado.resumoGeral() != null) {
            var rg = resultado.resumoGeral();
            r.resumoConsumo = toBigDecimalMesValues(rg.consumo50610002());
            r.resumoUsoRede = toBigDecimalMesValues(rg.usoRede50610003());
            r.resumoEncargo = toBigDecimalMesValues(rg.encargo50610006());
            r.resumoEER = toBigDecimalMesValues(rg.eer50610007());
            r.resumoESS = toBigDecimalMesValues(rg.ess50610008());
            r.resumoTotalGeral = toBigDecimalMesValues(rg.totalGeral());
            r.resumoProducaoTotal = toMesValues(rg.producaoTotal());
            r.resumoCustoEspecifico = toBigDecimalMesValues(rg.custoEspecifico());
            r.resumoCustoFixo = toBigDecimalMesValues(rg.custoFixo());
            r.resumoCustoVariavel = toBigDecimalMesValues(rg.custoVariavel());
        }

        return r;
    }

    // --- Tipos de resposta GraphQL ---

    public static class MotorCalculoResponse {
        // Cálculo 01 — Produção (tms)
        public List<AreaMesValue> producaoTms;
        public List<MesValue> producaoPelotas;
        public List<MesValue> producaoPelletFeed;
        public List<MesValue> producaoPSC;
        public List<MesValue> producaoPSM;

        // Cálculo 02 — Consumo Específico (kWh/tms)
        public List<AreaMesValue> consumoEspecificoKWhTms;

        // Cálculo 03 — Consumo Área
        public List<MesValue> consumoAreaTotalMWh;
        public List<MesValue> consumoMG_MWh;
        public List<MesValue> consumoES_MWh;
        public List<MesValue> percentualMG;
        public List<MesValue> percentualES;

        // Cálculo 04 — Geração Própria
        public List<MesValue> geracaoGuilmanMWh;
        public List<MesValue> geracaoMunizFreireMWh;
        public List<MesValue> geracaoTotalMWh;
        public List<CalendarioMesResponse> calendario;

        // Cálculo 05 — Encargos ESS/EER
        public List<MesValue> consumoSamarcoMWh;
        public List<MesValue> valorTotalEER;
        public List<MesValue> valorTotalESS;
        public List<MesValue> mgEER;
        public List<MesValue> mgESS;
        public List<MesValue> mgTotal;
        public List<MesValue> esEER;
        public List<MesValue> esESS;
        public List<MesValue> esTotal;

        // Cálculo 13 — Distribuição de Carga
        public List<MesValue> distribCargaNecCompraMG;
        public List<MesValue> distribCargaNecCompraES;
        public List<MesValue> distribCargaNecCompraSamarco;
        public List<MesValue> distribCargaPerdasMG;
        public List<MesValue> distribCargaPerdasES;

        // Cálculo 14 — Classe de Custo
        public List<MesValue> classeCustoTusdFioMG;
        public List<MesValue> classeCustoTusdFioES;
        public List<MesValue> classeCustoEncargoMG;
        public List<MesValue> classeCustoEncargoES;
        public List<MesValue> classeCustoConsumoMG;
        public List<MesValue> classeCustoConsumoES;

        // Cálculo 16 — Resumo Geral
        public List<MesValue> resumoConsumo;
        public List<MesValue> resumoUsoRede;
        public List<MesValue> resumoEncargo;
        public List<MesValue> resumoEER;
        public List<MesValue> resumoESS;
        public List<MesValue> resumoTotalGeral;
        public List<MesValue> resumoProducaoTotal;
        public List<MesValue> resumoCustoEspecifico;
        public List<MesValue> resumoCustoFixo;
        public List<MesValue> resumoCustoVariavel;
    }

    public static class AreaMesValue {
        public String area;
        public String mes;
        public double valor;
    }

    public static class MesValue {
        public String mes;
        public double valor;
    }

    public static class CalendarioMesResponse {
        public String mes;
        public int diasNoMes;
        public int diasUteis;
        public double horasPonta;
        public double horasForaPonta;
        public double totalHoras;
    }

    // --- Conversores ---

    private List<AreaMesValue> toAreaConsumoMesValues(Map<AreaConsumoEspecifico, Map<Mes, Double>> map) {
        List<AreaMesValue> result = new ArrayList<>();
        for (Map.Entry<AreaConsumoEspecifico, Map<Mes, Double>> entry : map.entrySet()) {
            for (Map.Entry<Mes, Double> mesEntry : entry.getValue().entrySet()) {
                AreaMesValue v = new AreaMesValue();
                v.area = entry.getKey().getLabel();
                v.mes = mesEntry.getKey().getLabel();
                v.valor = mesEntry.getValue();
                result.add(v);
            }
        }
        return result;
    }

    private Map<Mes, Double> zeroMesMap() {
        Map<Mes, Double> map = new EnumMap<>(Mes.class);
        for (Mes mes : Mes.values()) map.put(mes, 0.0);
        return map;
    }

    private List<AreaMesValue> toAreaMesValues(Map<AreaProducao, Map<Mes, Double>> map) {
        List<AreaMesValue> result = new ArrayList<>();
        for (Map.Entry<AreaProducao, Map<Mes, Double>> entry : map.entrySet()) {
            for (Map.Entry<Mes, Double> mesEntry : entry.getValue().entrySet()) {
                AreaMesValue v = new AreaMesValue();
                v.area = entry.getKey().getLabel();
                v.mes = mesEntry.getKey().getLabel();
                v.valor = mesEntry.getValue();
                result.add(v);
            }
        }
        return result;
    }

    private List<MesValue> toMesValues(Map<Mes, Double> map) {
        List<MesValue> result = new ArrayList<>();
        for (Mes mes : Mes.values()) {
            Double val = map.get(mes);
            if (val != null) {
                MesValue v = new MesValue();
                v.mes = mes.getLabel();
                v.valor = val;
                result.add(v);
            }
        }
        return result;
    }

    private List<MesValue> toBigDecimalMesValues(Map<Mes, BigDecimal> map) {
        List<MesValue> result = new ArrayList<>();
        for (Mes mes : Mes.values()) {
            BigDecimal val = map.get(mes);
            if (val != null) {
                MesValue v = new MesValue();
                v.mes = mes.getLabel();
                v.valor = val.doubleValue();
                result.add(v);
            }
        }
        return result;
    }

    // --- Input padrão (dados do Excel 2026) ---

    private MotorCalculoInput criarInputPadrao() {
        // Produção (ktms) — Ref: Aba 3
        Map<AreaProducao, Map<Mes, Double>> prodKtms = new EnumMap<>(AreaProducao.class);
        addProd(prodKtms, AreaProducao.FILTRAGEM_GERMANO, 1059.06847971261, 1021.03204582921, 1004.45470121026, 1119.81998267452, 1137.35168489642, 1148.22792867489, 1188.57177810075, 1138.51130682527, 1149.78297799541, 1159.53392468453, 1051.46339092688, 1120.4827388095);
        addProd(prodKtms, AreaProducao.BENEFICIAMENTO_USINA_1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        addProd(prodKtms, AreaProducao.BENEFICIAMENTO_USINA_2, 599.777013988936, 546.473046337862, 576.603593494139, 565.607409838606, 584.428484154006, 619.565263154035, 641.889633167329, 641.702192323429, 624.349080385919, 611.983339353751, 525.623556525034, 585.605841592393);
        addProd(prodKtms, AreaProducao.BENEFICIAMENTO_USINA_3, 660.813394924345, 683.621845312905, 680.473946502607, 806.838557088322, 828.463273567, 802.192534849812, 822.426587661967, 751.434925464488, 793.45275482829, 794.818491112382, 734.242518675521, 726.840262977234);
        addProd(prodKtms, AreaProducao.MINERODUTO_1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        addProd(prodKtms, AreaProducao.MINERODUTO_2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        addProd(prodKtms, AreaProducao.MINERODUTO_3, 1260.59040891328, 1230.09489165077, 1257.07753999674, 1372.44596692693, 1412.89175772101, 1421.75779800385, 1464.3162208293, 1393.13711778792, 1417.80183521421, 1406.80183046613, 1259.86607520055, 1312.44610456963);
        addProd(prodKtms, AreaProducao.PREPARACAO_1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        addProd(prodKtms, AreaProducao.PREPARACAO_2, 1260.59040891328, 1230.09489165077, 1257.07753999675, 1372.44596692693, 1412.89175772101, 1421.75779800385, 1464.3162208293, 1393.13711778792, 1417.80183521421, 1406.80183046613, 1259.86607520055, 1312.44610456963);
        addProd(prodKtms, AreaProducao.USINA_1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        addProd(prodKtms, AreaProducao.USINA_2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        addProd(prodKtms, AreaProducao.USINA_3, 474.39089440342, 457.319946441967, 385.051649299158, 522.009763930424, 527.210938538106, 532.46778232443, 560.315955250413, 527.285440406607, 523.743978637014, 536.104513516067, 471.719741525447, 488.46412951789);
        addProd(prodKtms, AreaProducao.USINA_4, 592.837543225209, 551.331095874749, 678.843054873898, 651.574151395314, 637.68127231241, 687.994772363659, 700.108892389257, 636.35485374873, 675.250698377472, 669.787796999254, 565.428397481056, 628.825573757082);
        addProd(prodKtms, AreaProducao.VENDAS, 1067.22843762863, 1008.65104231672, 1063.89470417306, 1173.58391532574, 1164.89221085052, 1220.46255468809, 1260.42484763967, 1163.64029415534, 1198.99467701449, 1205.89231051532, 1037.1481390065, 1117.28970327497);
        addProd(prodKtms, AreaProducao.PRODUCAO_PSC_PSM, 143.855055938658, 174.65285938085, 143.825052337558, 144.812255237932, 193.959029897655, 145.234162992193, 145.593823628757, 176.047762647398, 162.94095209313, 145.103030794638, 174.909333251059, 144.305607329475);
        addProd(prodKtms, AreaProducao.PELLET_FEED, 385.926883, 385.926883, 385.926883, 385.926883, 385.926883, 385.926883, 385.926883, 385.926883, 385.926883, 385.926883, 385.926883, 385.926883);

        // Consumo Específico (kWh/tms) — Ref: Aba 5
        Map<AreaConsumoEspecifico, Map<Mes, Double>> ceMap = new EnumMap<>(AreaConsumoEspecifico.class);
        addCE(ceMap, AreaConsumoEspecifico.FILTRAGEM_GERMANO, 3.27822183223059, 3.35949285542862, 3.28738273377368, 3.01104385861773, 2.92484896837796, 2.90660969526739, 2.82213291174198, 2.9663232335391, 2.91471974246362, 2.93751039450292, 3.28010657747266, 3.14869691457168);
        addCE(ceMap, AreaConsumoEspecifico.MINERACAO_1, 1.48408897482972, 1.37425160040094, 1.48881161277667, 1.3200198889732, 1.32444175319198, 1.27360568753251, 1.27830341342915, 1.3434574280404, 1.2771983693027, 1.33008969243801, 1.43735499021526, 1.42639954059472);
        addCE(ceMap, AreaConsumoEspecifico.BENEFICIAMENTO_1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        addCE(ceMap, AreaConsumoEspecifico.BENEFICIAMENTO_2, 42.3959899378023, 46.5313713464997, 44.0998643381824, 44.9572261743456, 43.5094129383661, 41.0419075474832, 39.6145052608621, 39.6260766352249, 40.7274408641437, 41.5503799120608, 48.3770940901294, 43.4219374944335);
        addCE(ceMap, AreaConsumoEspecifico.BENEFICIAMENTO_3, 42.2150234604106, 40.806505862642, 40.9953078527963, 34.5746867413553, 33.6722888155648, 34.7750115379116, 33.9193978843703, 37.1240086291001, 35.1579607651764, 35.0975576637741, 37.9932416766367, 38.3800576637442);
        addCE(ceMap, AreaConsumoEspecifico.MINERODUTO_1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        addCE(ceMap, AreaConsumoEspecifico.MINERODUTO_2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        addCE(ceMap, AreaConsumoEspecifico.MINERODUTO_3, 7.10512647714471, 7.19228825927993, 7.55856866424988, 7.10512647714471, 7.05788485082513, 7.1973399764735, 7.12678854398574, 7.18073747513508, 7.10512647714471, 7.18073747513508, 7.14285714285714, 7.18073747513508);
        addCE(ceMap, AreaConsumoEspecifico.PREPARACAO_1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        addCE(ceMap, AreaConsumoEspecifico.PREPARACAO_2, 10.77, 10.77, 10.77, 10.77, 10.77, 10.77, 10.77, 10.77, 10.77, 10.77, 10.77, 10.77);
        addCE(ceMap, AreaConsumoEspecifico.USINA_1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        addCE(ceMap, AreaConsumoEspecifico.USINA_2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        addCE(ceMap, AreaConsumoEspecifico.USINA_3, 29.95, 29.95, 29.95, 29.95, 29.95, 29.95, 29.95, 29.95, 29.95, 29.95, 29.95, 29.95);
        addCE(ceMap, AreaConsumoEspecifico.USINA_4, 32.17, 32.17, 32.17, 32.17, 32.17, 32.17, 32.17, 32.17, 32.17, 32.17, 32.17, 32.17);
        addCE(ceMap, AreaConsumoEspecifico.ESTOCAGEM, 0.868773527622028, 0.868773527622028, 0.868773527622028, 0.868773527622028, 0.868773527622028, 0.868773527622028, 0.868773527622028, 0.868773527622028, 0.868773527622028, 0.868773527622028, 0.868773527622028, 0.868773527622028);

        // Geração (MWh) — Ref: Aba 8
        Map<Mes, Double> guilman = mesMap(23979.1671, 21368.45235, 24144.72795, 24559.14825, 24972.53325, 18891.6525, 16420.194, 15320.57205, 15407.4585, 16528.92045, 17382.7122, 21189.3402);
        Map<Mes, Double> muniz = mesMap(13943, 12192, 13483, 9537, 9350, 6545, 5891, 5830, 4930, 5800, 10753, 13750);

        // Calendário — Ref: Aba 9
        Map<Mes, Integer> diasNoMes = Map.ofEntries(
                Map.entry(Mes.JAN, 31), Map.entry(Mes.FEV, 28), Map.entry(Mes.MAR, 31),
                Map.entry(Mes.ABR, 30), Map.entry(Mes.MAI, 31), Map.entry(Mes.JUN, 30),
                Map.entry(Mes.JUL, 31), Map.entry(Mes.AGO, 31), Map.entry(Mes.SET, 30),
                Map.entry(Mes.OUT, 31), Map.entry(Mes.NOV, 30), Map.entry(Mes.DEZ, 31));
        Map<Mes, Integer> diasNaoUteis = Map.ofEntries(
                Map.entry(Mes.JAN, 10), Map.entry(Mes.FEV, 9), Map.entry(Mes.MAR, 10),
                Map.entry(Mes.ABR, 8), Map.entry(Mes.MAI, 11), Map.entry(Mes.JUN, 8),
                Map.entry(Mes.JUL, 8), Map.entry(Mes.AGO, 10), Map.entry(Mes.SET, 8),
                Map.entry(Mes.OUT, 9), Map.entry(Mes.NOV, 10), Map.entry(Mes.DEZ, 9));

        // Tarifas Encargos (R$/MWh por mês) — Ref: Aba 12
        Map<Mes, BigDecimal> err = bigDecimalMesMap("11.3299542824591", "14.2925124745266", "16.6092370383086", "16.9263320010871", "17.0724409157406", "11.86274727182", "10.5420967811414", "8.82799936625398", "7.93809533347903", "8.77296482027835", "9.89288738945115", "12.0629068450646");
        Map<Mes, BigDecimal> ercap = bigDecimalMesMap("3.31", "3.42", "3.24", "3.56119970062708", "3.58509216196125", "3.74720614407091", "7.08880369703058", "7.06747029863739", "6.99112100529334", "6.75573600355482", "7.03611955711038", "6.9628329255811");
        Map<Mes, BigDecimal> ess = bigDecimalMesMap("3.03512734680074", "0.20743694854762", "1.53537956133978", "0.685698742697896", "0.35952278786897", "0.909677915939472", "4.25939588140306", "7.41165610954343", "7.52054803171039", "3.64158432341785", "5.21254152709107", "2.11727012918011");

        // Tarifas Distribuidoras — Ref: Aba 2
        TarifasDistribuidorasInput tarifasDistrib = criarTarifasDistribuidoras();

        // Demanda Contratada — Ref: Aba 10
        DemandaContratadaInput demanda = criarDemandaContratada();

        // Despesas Fixas — Ref: Aba 16, rows 8-15
        DespesasFixasInput despesas = criarDespesasFixas();

        return new MotorCalculoInput(
                new PlanejamentoProducaoInput(prodKtms),
                new ConsumoEspecificoInput(ceMap),
                new PlanejamentoGeracaoInput(guilman, muniz),
                new CalendarioInput(diasNoMes, diasNaoUteis),
                new TarifasEncargosInput(err, ercap, ess),
                tarifasDistrib, demanda, despesas
        );
    }

    private TarifasDistribuidorasInput criarTarifasDistribuidoras() {
        Map<Unidade, Map<Mes, BigDecimal>> tusdFioPonta = new EnumMap<>(Unidade.class);
        Map<Unidade, Map<Mes, BigDecimal>> tusdFioFP = new EnumMap<>(Unidade.class);
        Map<Unidade, Map<Mes, BigDecimal>> tusdEncargo = new EnumMap<>(Unidade.class);
        Map<Unidade, Map<Mes, BigDecimal>> descontoAP = new EnumMap<>(Unidade.class);

        // Germano — Ref: Aba 2, rows 3-6
        tusdFioPonta.put(Unidade.GERMANO, bigDecimalMesMapSplit("6.84586757688229", "7.50258483563096", 6));
        tusdFioFP.put(Unidade.GERMANO, bigDecimalMesMapSplit("6.83333112407211", "7.54115853658537", 6));
        tusdEncargo.put(Unidade.GERMANO, bigDecimalMesMapSplit("64.1480646871686", "58.4970174973489", 5));
        descontoAP.put(Unidade.GERMANO, bigDecimalMesMapSplit("66.52", "60.66", 6));

        // Matipó — Ref: Aba 2, rows 8-12
        tusdFioPonta.put(Unidade.MATIPO, bigDecimalMesMapSplit("15.5738709677419", "29.665248361175", 6));
        tusdFioFP.put(Unidade.MATIPO, bigDecimalMesMapSplit("8.37241935483871", "11.691941870904", 6));
        tusdEncargo.put(Unidade.MATIPO, bigDecimalMesMapSplit("81.1773387096774", "82.3855494897628", 6));
        descontoAP.put(Unidade.MATIPO, bigDecimalMesMap("0","0","0","0","0","0","0","0","0","0","0","0"));

        // UBU — Ref: Aba 2, rows 17-21
        tusdFioPonta.put(Unidade.UBU, bigDecimalMesMapCustom(
                "20.4845187200415","20.4845187200415","20.4845187200415","20.4845187200415",
                "20.4845187200415","20.4845187200415","20.4845187200415",
                "20.117692172749","20.117692172749","20.117692172749","20.117692172749","20.117692172749"));
        tusdFioFP.put(Unidade.UBU, bigDecimalMesMapCustom(
                "8.37241935483871","8.37241935483871","8.37241935483871","8.37241935483871",
                "8.37241935483871","8.37241935483871","11.691941870904",
                "8.54284214138739","8.54284214138739","8.54284214138739","8.54284214138739","8.54284214138739"));
        tusdEncargo.put(Unidade.UBU, bigDecimalMesMapCustom(
                "86.8299002461459","86.8299002461459","86.8299002461459","86.8299002461459",
                "86.8299002461459","86.8299002461459","86.8299002461459",
                "83.2161020764466","83.2161020764466","83.2161020764466","83.2161020764466","83.2161020764466"));
        descontoAP.put(Unidade.UBU, bigDecimalMesMapCustom(
                "77.55","77.55","77.55","77.55","77.55","77.55","77.55",
                "78.620665438016","78.620665438016","78.620665438016","78.620665438016","78.620665438016"));

        Map<Mes, BigDecimal> pmixMG = bigDecimalMesMap("160.998461751314","160.998461751314","160.998461751314","160.998461751314","160.998461751314","160.998461751314","160.998461751314","160.998461751314","160.998461751314","160.998461751314","160.998461751314","160.998461751314");
        Map<Mes, BigDecimal> pmixES = bigDecimalMesMap("161.245095423521","161.245095423521","161.245095423521","161.245095423521","161.245095423521","161.245095423521","161.245095423521","161.245095423521","161.245095423521","161.245095423521","161.245095423521","161.245095423521");

        return new TarifasDistribuidorasInput(tusdFioPonta, tusdFioFP, tusdEncargo, descontoAP, pmixMG, pmixES);
    }

    private DemandaContratadaInput criarDemandaContratada() {
        Map<Unidade, Map<Mes, Double>> ponta = new EnumMap<>(Unidade.class);
        Map<Unidade, Map<Mes, Double>> fp = new EnumMap<>(Unidade.class);
        ponta.put(Unidade.GERMANO, mesMapConstant(120000));
        fp.put(Unidade.GERMANO, mesMapConstant(120000));
        ponta.put(Unidade.MATIPO, mesMapConstant(11000));
        fp.put(Unidade.MATIPO, mesMapConstant(11000));
        ponta.put(Unidade.UBU, mesMapConstant(88000));
        fp.put(Unidade.UBU, mesMapConstant(88000));
        return new DemandaContratadaInput(ponta, fp);
    }

    private DespesasFixasInput criarDespesasFixas() {
        return new DespesasFixasInput(
                bigDecimalMesMap("241214.438110908","368746.254946268","391217.355595486","336157.108917813","347984.38699056","264024.711657817","238093.510726971","174321.067428341","151424.985296812","141787.801903991","138058.492899658","179222.385820984"),
                bigDecimalMesMapCustom("665774.26","665774.26","665774.26","665774.26","665774.26","665774.26","719482.69","719482.69","719482.69","719482.69","719482.69","719482.69"),
                bigDecimalMesMap("1178695.89","1226507.16","1371448","1491723.94","1540771.14","1674148.62","1521470.84","1806129.92","1755558.99","1710922.73","1330109.71","1234823.8"),
                bigDecimalMesMapConstant("203645.692887151"),
                bigDecimalMesMap("334059.96","334059.96","334059.96","454060","414060","414060","334059.96","334059.96","334059.96","334059.96","334059.96","334059.96"),
                bigDecimalMesMapConstant("25000"),
                bigDecimalMesMapConstant("108333"),
                bigDecimalMesMap("772738.24","235785.28","235785.28","235785.28","331560.28","331560.28","331560.28","331560.28","331560.28","711560.28","331560.28","331560.28")
        );
    }

    // --- Helpers ---

    private void addProd(Map<AreaProducao, Map<Mes, Double>> map, AreaProducao area, double... vals) {
        map.put(area, mesMap(vals));
    }

    private void addCE(Map<AreaConsumoEspecifico, Map<Mes, Double>> map, AreaConsumoEspecifico area, double... vals) {
        map.put(area, mesMap(vals));
    }

    private Map<Mes, Double> mesMap(double... vals) {
        Map<Mes, Double> map = new EnumMap<>(Mes.class);
        Mes[] meses = Mes.values();
        for (int i = 0; i < vals.length && i < 12; i++) {
            map.put(meses[i], vals[i]);
        }
        return map;
    }

    private Map<Mes, Double> mesMapConstant(double val) {
        Map<Mes, Double> map = new EnumMap<>(Mes.class);
        for (Mes mes : Mes.values()) map.put(mes, val);
        return map;
    }

    private Map<Mes, BigDecimal> bigDecimalMesMap(String... vals) {
        Map<Mes, BigDecimal> map = new EnumMap<>(Mes.class);
        Mes[] meses = Mes.values();
        for (int i = 0; i < vals.length && i < 12; i++) {
            map.put(meses[i], new BigDecimal(vals[i]));
        }
        return map;
    }

    private Map<Mes, BigDecimal> bigDecimalMesMapCustom(String... vals) {
        return bigDecimalMesMap(vals);
    }

    private Map<Mes, BigDecimal> bigDecimalMesMapConstant(String val) {
        Map<Mes, BigDecimal> map = new EnumMap<>(Mes.class);
        BigDecimal bd = new BigDecimal(val);
        for (Mes mes : Mes.values()) map.put(mes, bd);
        return map;
    }

    /** Cria map com valor1 para meses 1..splitAt e valor2 para meses (splitAt+1)..12 */
    private Map<Mes, BigDecimal> bigDecimalMesMapSplit(String valor1, String valor2, int splitAt) {
        Map<Mes, BigDecimal> map = new EnumMap<>(Mes.class);
        BigDecimal v1 = new BigDecimal(valor1);
        BigDecimal v2 = new BigDecimal(valor2);
        for (Mes mes : Mes.values()) {
            map.put(mes, mes.getNumero() <= splitAt ? v1 : v2);
        }
        return map;
    }
}
