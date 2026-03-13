import { useMotorCalculo } from "../hooks/useMotorCalculo";
import { LoadingSkeleton, ErrorBanner, DataSourceTag } from "../components/ApiComponents";
import { useMemo } from "react";

import { MESES, formatKWh, getHeatmapColor, getHeatmapTextColor } from "../lib/utils";

const AREAS = [
  "Filtragem Germano (Arenoso)",
  "Beneficiamento Usina 1",
  "Beneficiamento Usina 2",
  "Beneficiamento Usina 3",
  "Mineroduto 1",
  "Mineroduto 2",
  "Mineroduto 3",
  "Preparação 1",
  "Preparação 2",
  "Usina 1",
  "Usina 2",
  "Usina 3",
  "Usina 4",
  "Estocagem",
  "Produção PSC + PSM",
  "Pellet Feed (embarque)",
];

export default function ConsumoEspecifico() {
  const { data, loading, hasError, errorMessage, isLiveData, refetch } = useMotorCalculo();

  type AreaRow = { area: string; avg: number } & Record<string, number>;

  const tableData = useMemo((): AreaRow[] => {
    return AREAS.map((area) => {
      const row: Record<string, number> = {};
      let sum = 0;
      let count = 0;
      MESES.forEach((mes) => {
        const found = (data as any).consumoEspecifico?.find(
          (d: any) => d.area === area && d.mes === mes
        );
        const val = found?.valor ?? 0;
        row[mes] = val;
        if (val > 0) { sum += val; count++; }
      });
      return { area, ...row, avg: count > 0 ? sum / count : 0 } as AreaRow;
    });
  }, []);

  const allValues = useMemo(() => {
    const vals: number[] = [];
    tableData.forEach((row) => {
      MESES.forEach((mes) => {
        const v = row[mes] as number;
        if (v > 0) vals.push(v);
      });
    });
    return vals;
  }, [tableData]);

  const minVal = useMemo(() => Math.min(...allValues), [allValues]);
  const maxVal = useMemo(() => Math.max(...allValues), [allValues]);

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      {/* Header */}
      <div className="flex items-center gap-3" style={{ flexWrap: "wrap" }}>
        <div>
          <h1 style={{ color: "#e8edf2", fontSize: "1.4rem", fontWeight: 800, margin: 0 }}>
            Cálculo 02 — Consumo Específico (kWh/tms)
          </h1>
          <p style={{ color: "#8a9bb5", fontSize: "0.8rem", margin: "4px 0 0" }}>
            Tabela de referência · Valores de entrada (pass-through)
          </p>
        </div>
        <span className="badge-accent" style={{ marginLeft: "auto" }}>Aba 6 do Excel</span>
      </div>

      {/* Info Card */}
      <div className="samarco-card" style={{ background: "rgba(0, 64, 113, 0.15)", borderColor: "#1e3a5f" }}>
        <div className="flex gap-6" style={{ flexWrap: "wrap" }}>
          <div>
            <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em" }}>Tipo</div>
            <div style={{ color: "#ffa726", fontSize: "0.85rem", marginTop: 4 }}>Pass-through com validação</div>
          </div>
          <div>
            <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em" }}>Descrição</div>
            <div style={{ color: "#e8edf2", fontSize: "0.85rem", marginTop: 4 }}>Cópia direta do input — quanto cada área consome (kWh) por tonelada produzida</div>
          </div>
          <div>
            <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em" }}>Legenda Heatmap</div>
            <div className="flex gap-3" style={{ marginTop: 4 }}>
              <span style={{ background: "rgba(0, 200, 83, 0.25)", color: "#00c853", padding: "2px 8px", borderRadius: 4, fontSize: "0.72rem" }}>Baixo</span>
              <span style={{ background: "rgba(255, 167, 38, 0.2)", color: "#ffa726", padding: "2px 8px", borderRadius: 4, fontSize: "0.72rem" }}>Médio</span>
              <span style={{ background: "rgba(239, 83, 80, 0.35)", color: "#ef5350", padding: "2px 8px", borderRadius: 4, fontSize: "0.72rem" }}>Alto</span>
            </div>
          </div>
        </div>
      </div>

      {/* Heatmap Table */}
      <div className="samarco-card">
        <div style={{ marginBottom: 12 }}>
          <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>
            Consumo Específico por Área — Heatmap (kWh/tms)
          </h3>
          <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>
            Cores indicam intensidade energética: verde = baixo, vermelho = alto
          </p>
        </div>
        <div style={{ overflowX: "auto" }}>
          <table className="samarco-table">
            <thead>
              <tr>
                <th>Área</th>
                {MESES.map((m) => <th key={m}>{m}</th>)}
                <th style={{ color: "#ffa726" }}>Média</th>
              </tr>
            </thead>
            <tbody>
              {tableData.map((row) => (
                <tr key={row.area}>
                  <td title={row.area}>{row.area}</td>
                  {MESES.map((mes) => {
                    const val = row[mes] as number;
                    if (val === 0) {
                      return <td key={mes} className="zero-value">—</td>;
                    }

  if (loading) return <LoadingSkeleton />;

                    return (
                      <td key={mes} style={{ padding: "4px 8px" }}>
                        <div
                          className="heatmap-cell"
                          style={{
                            background: getHeatmapColor(val, minVal, maxVal),
                            color: getHeatmapTextColor(val, minVal, maxVal),
                          }}
                        >
                          {formatKWh(val, 2)}
                        </div>
                      </td>
                    );
                  })}
                  <td style={{ color: "#ffa726", fontWeight: 600 }}>
                    {row.avg === 0 ? "—" : formatKWh(row.avg, 2)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Stats */}
      <div className="flex gap-4" style={{ flexWrap: "wrap" }}>
        {[
          { label: "Valor Mínimo", value: formatKWh(minVal, 4), color: "#00c853" },
          { label: "Valor Máximo", value: formatKWh(maxVal, 4), color: "#ef5350" },
          { label: "Média Geral", value: formatKWh(allValues.reduce((a, b) => a + b, 0) / allValues.length, 4), color: "#ffa726" },
          { label: "Áreas Ativas", value: `${tableData.filter(r => r.avg > 0).length} de ${AREAS.length}`, color: "#00D8FF" },
        ].map((stat) => (
          <div key={stat.label} className="samarco-card" style={{ flex: "1 1 150px", textAlign: "center" }}>
            <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em" }}>{stat.label}</div>
            <div style={{ color: stat.color, fontSize: "1.1rem", fontWeight: 800, marginTop: 6 }}>{stat.value}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
