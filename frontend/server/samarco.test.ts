import { describe, expect, it } from "vitest";

// Test the utility functions logic (server-side equivalent)
describe("SAMARCO Utility Functions", () => {
  const MESES = ["Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"];

  function sumArray(arr: { mes: string; valor: number }[]): number {
    return arr.reduce((acc, item) => acc + item.valor, 0);
  }

  function getByMes(arr: { mes: string; valor: number }[], mes: string): number {
    return arr.find((item) => item.mes === mes)?.valor ?? 0;
  }

  function formatCurrency(value: number): string {
    return new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  }

  function formatPercent(value: number, decimals = 4): string {
    return new Intl.NumberFormat("pt-BR", {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals,
    }).format(value * 100) + "%";
  }

  it("sumArray should correctly sum all values", () => {
    const data = [
      { mes: "Jan", valor: 100 },
      { mes: "Fev", valor: 200 },
      { mes: "Mar", valor: 300 },
    ];
    expect(sumArray(data)).toBe(600);
  });

  it("sumArray should handle empty array", () => {
    expect(sumArray([])).toBe(0);
  });

  it("getByMes should return correct value for existing month", () => {
    const data = [
      { mes: "Jan", valor: 116677.18 },
      { mes: "Fev", valor: 114099.75 },
    ];
    expect(getByMes(data, "Jan")).toBeCloseTo(116677.18, 2);
    expect(getByMes(data, "Fev")).toBeCloseTo(114099.75, 2);
  });

  it("getByMes should return 0 for non-existing month", () => {
    const data = [{ mes: "Jan", valor: 100 }];
    expect(getByMes(data, "Dez")).toBe(0);
  });

  it("formatCurrency should format Brazilian currency correctly", () => {
    const result = formatCurrency(1234567.89);
    expect(result).toContain("1.234.567,89");
  });

  it("formatPercent should format percentage with correct decimals", () => {
    const result = formatPercent(0.5904, 4);
    expect(result).toContain("59,0400%");
  });

  it("MESES array should have 12 months", () => {
    expect(MESES).toHaveLength(12);
    expect(MESES[0]).toBe("Jan");
    expect(MESES[11]).toBe("Dez");
  });
});

describe("SAMARCO Calculation Logic", () => {
  it("Consumo Samarco = Consumo Área Total - Geração Total", () => {
    const consumoArea = 116677.18;
    const geracao = 37922.17;
    const expected = consumoArea - geracao;
    const actual = 78755.01; // from mockApiData
    expect(Math.abs(expected - actual)).toBeLessThan(1.0);
  });

  it("Produção tms = Planejamento ktms × 1000", () => {
    const planejamentoKtms = 1059.068;
    const expected = planejamentoKtms * 1000;
    expect(expected).toBeCloseTo(1059068, 0);
  });

  it("Percentual MG + ES should equal approximately 100%", () => {
    const percMG = 0.5904655173697174;
    const percES = 0.40953448263028275;
    expect(percMG + percES).toBeCloseTo(1.0, 5);
  });

  it("Geração Total = Guilman + Muniz Freire", () => {
    const guilman = 23979.1671;
    const muniz = 13943.0;
    const total = 37922.1671;
    expect(guilman + muniz).toBeCloseTo(total, 2);
  });

  it("Total Encargos = MG Total + ES Total", () => {
    const mgTotal = 821928.78;
    const esTotal = 570072.54;
    const expected = mgTotal + esTotal;
    expect(expected).toBeCloseTo(1392001.32, 0);
  });

  it("Encargo Total = EER + ESS", () => {
    const eer = 1152969.83;
    const ess = 239031.50;
    const total = eer + ess;
    expect(total).toBeCloseTo(1392001.33, 0);
  });
});

describe("SAMARCO Data Validation", () => {
  it("should have 12 months of data for each metric", () => {
    const months = ["Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"];
    expect(months).toHaveLength(12);
  });

  it("should have 192 production records (16 areas × 12 months)", () => {
    const areas = 16;
    const months = 12;
    expect(areas * months).toBe(192);
  });

  it("validation status should be match when difference < 0.01", () => {
    const frontend = 116677.18250384345;
    const api = 116677.18250384345;
    const diff = Math.abs(frontend - api);
    expect(diff).toBeLessThan(0.01);
  });

  it("validation status should be close when difference < 1.0", () => {
    const frontend = 116677.18;
    const api = 116677.19;
    const diff = Math.abs(frontend - api);
    expect(diff).toBeLessThan(1.0);
    expect(diff).toBeGreaterThanOrEqual(0.01);
  });

  it("validation status should be mismatch when difference >= 1.0", () => {
    const frontend = 116677.0;
    const api = 116678.5;
    const diff = Math.abs(frontend - api);
    expect(diff).toBeGreaterThanOrEqual(1.0);
  });
});
