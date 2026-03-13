import { useMotorCalculo } from "../hooks/useMotorCalculo";
import { LoadingSkeleton, ErrorBanner, DataSourceTag } from "../components/ApiComponents";
import { useMemo } from "react";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from "recharts";

import { MESES, formatTms, formatNumber, RECHARTS_TOOLTIP_STYLE } from "../lib/utils";

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

export default function Producao() {
  const { data, loading, hasError, errorMessage, isLiveData, refetch } = useMotorCalculo();


  type AreaRow = { area: string; total: number } & Record<string, number>;

  const tableData = useMemo((): AreaRow[] => {
    return AREAS.map((area) => {
      const row: Record<string, number> = {};
      let total = 0;
      MESES.forEach((mes) => {
        const found = data.producaoTms.find((d) => d.area === area && d.mes === mes);
        const val = found?.valor ?? 0;
        row[mes] = val;
        total += val;
      });
      return { area, ...row, total } as AreaRow;
    });
  }, []);

  const totals = useMemo((): Record<string, number> => {
    const row: Record<string, number> = {};
    let grandTotal = 0;
    MESES.forEach((mes) => {
      const sum = tableData.reduce((acc, r) => acc + (r[mes] ?? 0), 0);
      row[mes] = sum;
      grandTotal += sum;
    });
    return { ...row, total: grandTotal };
  }, [tableData]);

  const top5 = useMemo(() => {
    return [...tableData]
      .sort((a, b) => b.total - a.total)
      .slice(0, 5)
      .map((r) => ({ area: r.area.length > 22 ? r.area.slice(0, 22) + "…" : r.area, total: r.total }));
  }, [tableData]);

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      return (
        <div style={{ ...RECHARTS_TOOLTIP_STYLE, padding: "10px 14px" }}>
          <p style={{ color: "#8a9bb5", marginBottom: 4, fontWeight: 600, fontSize: "0.75rem" }}>{label}</p>
          <p style={{ color: "#00D8FF", fontWeight: 700 }}>{formatTms(payload[0].value)} tms</p>
        </div>
      );
    }
    return null;
  };


  if (loading) return <LoadingSkeleton />;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      {/* Header */}
      <div className="flex items-center gap-3" style={{ flexWrap: "wrap" }}>
        <div>
          <h1 style={{ color: "#e8edf2", fontSize: "1.4rem", fontWeight: 800, margin: 0 }}>
            Cálculo 01 — Produção (tms)
          </h1>
          <p style={{ color: "#8a9bb5", fontSize: "0.8rem", margin: "4px 0 0" }}>
            16 áreas de produção × 12 meses · Ano Base 2026
          </p>
        </div>
        <div className="flex gap-2" style={{ marginLeft: "auto" }}>
          <DataSourceTag isLive={isLiveData} />
          <span className="badge-accent">Aba 4 do Excel</span>
        </div>
      </div>
      {hasError && <ErrorBanner message={errorMessage} onRetry={refetch} isFallback={!isLiveData} />}

      {/* Info Card */}
      <div className="samarco-card" style={{ background: "rgba(0, 64, 113, 0.15)", borderColor: "#1e3a5f" }}>
        <div className="flex gap-6" style={{ flexWrap: "wrap" }}>
          <div>
            <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em" }}>Fórmula</div>
            <div style={{ color: "#00D8FF", fontFamily: "monospace", fontSize: "0.85rem", marginTop: 4 }}>
              Produção_tms = Planejamento_ktms × 1.000
            </div>
          </div>
          <div>
            <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em" }}>Escopo</div>
            <div style={{ color: "#e8edf2", fontSize: "0.85rem", marginTop: 4 }}>16 áreas × 12 meses = 192 registros</div>
          </div>
          <div>
            <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em" }}>Multiplicador</div>
            <div style={{ color: "#e8edf2", fontSize: "0.85rem", marginTop: 4 }}>1.000 (ktms → tms)</div>
          </div>
          <div>
            <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em" }}>Total Anual</div>
            <div style={{ color: "#00c853", fontSize: "0.85rem", fontWeight: 700, marginTop: 4 }}>
              {formatTms(totals.total)} tms
            </div>
          </div>
        </div>
      </div>

      {/* Main Table */}
      <div className="samarco-card">
        <div style={{ marginBottom: 12 }}>
          <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>
            Produção por Área (tms/mês)
          </h3>
        </div>
        <div style={{ overflowX: "auto" }}>
          <table className="samarco-table">
            <thead>
              <tr>
                <th>Área</th>
                {MESES.map((m) => <th key={m}>{m}</th>)}
                <th style={{ color: "#00D8FF" }}>Total</th>
              </tr>
            </thead>
            <tbody>
              {tableData.map((row) => (
                <tr key={row.area}>
                  <td title={row.area} style={{ maxWidth: 200 }}>{row.area}</td>
                  {MESES.map((mes) => (
                    <td key={mes} className={row[mes] === 0 ? "zero-value" : ""}>
                      {row[mes] === 0 ? "—" : formatTms(row[mes] as number)}
                    </td>
                  ))}
                  <td style={{ fontWeight: 600, color: row.total === 0 ? "#4a5a72" : "#e8edf2" }}>
                    {row.total === 0 ? "—" : formatTms(row.total)}
                  </td>
                </tr>
              ))}
              <tr className="total-row">
                <td>TOTAL</td>
                {MESES.map((mes) => <td key={mes}>{formatTms(totals[mes] as number)}</td>)}
                <td>{formatTms(totals.total)}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Top 5 Chart */}
      <div className="samarco-card">
        <div style={{ marginBottom: 16 }}>
          <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>
            Top 5 Áreas por Produção Anual
          </h3>
          <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Total acumulado 2026 (tms)</p>
        </div>
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={top5} layout="vertical" margin={{ top: 5, right: 30, left: 10, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5} horizontal={false}/>
            <XAxis type="number" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false}
              tickFormatter={(v) => `${(v / 1000000).toFixed(1)}M`}/>
            <YAxis type="category" dataKey="area" tick={{ fill: "#e8edf2", fontSize: 11 }} axisLine={false} tickLine={false} width={160}/>
            <Tooltip content={<CustomTooltip />} />
            <Bar dataKey="total" radius={[0, 4, 4, 0]}>
              {top5.map((_, i) => (
                <Cell key={i} fill={i === 0 ? "#00D8FF" : i === 1 ? "#457ba9" : "#1e3a5f"}/>
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
