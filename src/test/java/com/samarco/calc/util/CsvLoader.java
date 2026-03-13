package com.samarco.calc.util;

import com.samarco.calc.input.CalendarioInput;
import com.samarco.calc.input.ConsumoEspecificoInput;
import com.samarco.calc.input.PlanejamentoGeracaoInput;
import com.samarco.calc.input.PlanejamentoProducaoInput;
import com.samarco.calc.input.TarifasEncargosInput;
import com.samarco.calc.model.AreaConsumoEspecifico;
import com.samarco.calc.model.AreaProducao;
import com.samarco.calc.model.Mes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Utilitário para carregar CSVs como objetos de input para testes.
 */
public final class CsvLoader {

    private static final Mes[] MESES = Mes.values();

    private CsvLoader() {
    }

    /**
     * Carrega planejamento de produção do CSV.
     * CSV format: Área,Jan,Fev,...,Dez (primeira linha = header, segunda = "Dias no mês")
     */
    public static PlanejamentoProducaoInput loadProducao(String resourcePath) throws IOException {
        Map<AreaProducao, Map<Mes, Double>> producao = new EnumMap<>(AreaProducao.class);

        try (BufferedReader reader = openResource(resourcePath)) {
            String header = reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                String areaLabel = parts[0].trim();

                if (areaLabel.equals("Dias no mês") || areaLabel.isEmpty()) {
                    continue;
                }

                try {
                    AreaProducao area = AreaProducao.fromLabel(areaLabel);
                    Map<Mes, Double> valores = parseDoubleValues(parts);
                    producao.put(area, valores);
                } catch (IllegalArgumentException e) {
                    // Área não mapeada no enum, skip
                }
            }
        }

        return new PlanejamentoProducaoInput(producao);
    }

    /**
     * Carrega consumo específico do CSV.
     */
    public static ConsumoEspecificoInput loadConsumoEspecifico(String resourcePath) throws IOException {
        Map<AreaConsumoEspecifico, Map<Mes, Double>> consumo = new EnumMap<>(AreaConsumoEspecifico.class);

        try (BufferedReader reader = openResource(resourcePath)) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                String areaLabel = parts[0].trim();

                if (areaLabel.isEmpty()) continue;

                try {
                    AreaConsumoEspecifico area = AreaConsumoEspecifico.fromLabel(areaLabel);
                    Map<Mes, Double> valores = parseDoubleValues(parts);
                    consumo.put(area, valores);
                } catch (IllegalArgumentException e) {
                    // Área não mapeada, skip
                }
            }
        }

        return new ConsumoEspecificoInput(consumo);
    }

    /**
     * Carrega planejamento de geração do CSV.
     * CSV format: Usina,Jan,...,Dez
     */
    public static PlanejamentoGeracaoInput loadGeracao(String resourcePath) throws IOException {
        Map<Mes, Double> guilman = new EnumMap<>(Mes.class);
        Map<Mes, Double> munizFreire = new EnumMap<>(Mes.class);

        try (BufferedReader reader = openResource(resourcePath)) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                String usina = parts[0].trim();
                Map<Mes, Double> valores = parseDoubleValues(parts);

                if (usina.contains("Guilman")) {
                    guilman.putAll(valores);
                } else if (usina.contains("Muniz")) {
                    munizFreire.putAll(valores);
                }
            }
        }

        return new PlanejamentoGeracaoInput(guilman, munizFreire);
    }

    /**
     * Carrega calendário input do CSV gerado do Excel.
     * CSV format: Label,Jan,...,Dez (2 linhas: dias no mês + dias não úteis)
     */
    public static CalendarioInput loadCalendario(String resourcePath) throws IOException {
        Map<Mes, Integer> diasNoMes = new EnumMap<>(Mes.class);
        Map<Mes, Integer> diasNaoUteis = new EnumMap<>(Mes.class);

        try (BufferedReader reader = openResource(resourcePath)) {
            // Linha 1: Número de dias
            String line1 = reader.readLine();
            String[] parts1 = line1.split(",", -1);
            for (int i = 0; i < 12 && i + 1 < parts1.length; i++) {
                String val = parts1[i + 1].trim();
                if (!val.isEmpty()) {
                    diasNoMes.put(MESES[i], (int) Double.parseDouble(val));
                }
            }

            // Linha 2: Número de dias não úteis
            String line2 = reader.readLine();
            String[] parts2 = line2.split(",", -1);
            for (int i = 0; i < 12 && i + 1 < parts2.length; i++) {
                String val = parts2[i + 1].trim();
                if (!val.isEmpty()) {
                    diasNaoUteis.put(MESES[i], (int) Double.parseDouble(val));
                }
            }
        }

        return new CalendarioInput(diasNoMes, diasNaoUteis);
    }

    /**
     * Carrega tarifas de encargos do CSV gerado do Excel.
     * CSV format: 3 linhas (ERR, ERCAP, ESS) com Label,Unidade,Jan,...,Dez
     */
    public static TarifasEncargosInput loadTarifas(String resourcePath) throws IOException {
        Map<Mes, BigDecimal> err = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> ercap = new EnumMap<>(Mes.class);
        Map<Mes, BigDecimal> ess = new EnumMap<>(Mes.class);

        try (BufferedReader reader = openResource(resourcePath)) {
            // ERR
            parseTarifaLine(reader.readLine(), err);
            // ERCAP
            parseTarifaLine(reader.readLine(), ercap);
            // ESS
            parseTarifaLine(reader.readLine(), ess);
        }

        return new TarifasEncargosInput(err, ercap, ess);
    }

    private static void parseTarifaLine(String line, Map<Mes, BigDecimal> target) {
        String[] parts = line.split(",", -1);
        // parts[0] = label, parts[1] = unidade, parts[2..13] = Jan..Dez
        for (int i = 0; i < 12 && i + 2 < parts.length; i++) {
            String val = parts[i + 2].trim();
            if (!val.isEmpty()) {
                target.put(MESES[i], new BigDecimal(val));
            }
        }
    }

    private static Map<Mes, Double> parseDoubleValues(String[] parts) {
        Map<Mes, Double> valores = new EnumMap<>(Mes.class);
        for (int i = 0; i < 12 && i + 1 < parts.length; i++) {
            String val = parts[i + 1].trim();
            if (!val.isEmpty()) {
                try {
                    valores.put(MESES[i], Double.parseDouble(val));
                } catch (NumberFormatException e) {
                    // Ignorar valores não numéricos (texto do Excel)
                }
            }
        }
        return valores;
    }

    private static BufferedReader openResource(String resourcePath) {
        InputStream is = CsvLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalArgumentException("Recurso não encontrado: " + resourcePath);
        }
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }
}
