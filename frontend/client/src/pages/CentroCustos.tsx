import { useMotorCalculo } from "../hooks/useMotorCalculo";
import { LoadingSkeleton, ErrorBanner, DataSourceTag } from "../components/ApiComponents";
import { useMemo, useState } from "react";
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, Cell, PieChart, Pie
} from "recharts";

import { formatCurrency, getByMes, CHART_COLORS, RECHARTS_TOOLTIP_STYLE } from "../lib/utils";
import { usePeriod, filterByPeriod, getFilteredMonths } from "../components/SamarcoLayout";

// MG: 8 centros de custo — percentuais estimados com base no consumo relativo
const CC_MG = [
  { id: "CC-MG-01", nome: "Filtragem", pct: 0.18 },
  { id: "CC-MG-02", nome: "Mineração", pct: 0.22 },
  { id: "CC-MG-03", nome: "Beneficiamento 1", pct: 0.12 },
  { id: "CC-MG-04", nome: "Beneficiamento 2", pct: 0.11 },
  { id: "CC-MG-05", nome: "Beneficiamento 3", pct: 0.10 },
  { id: "CC-MG-06", nome: "Mineroduto 1", pct: 0.13 },
  { id: "CC-MG-07", nome: "Mineroduto 2", pct: 0.09 },
  { id: "CC-MG-08", nome: "Mineroduto 3", pct: 0.05 },
];

// ES: 7 centros de custo
const CC_ES = [
  { id: "CC-ES-01", nome: "Preparação 1", pct: 0.16 },
  { id: "CC-ES-02", nome: "Preparação 2", pct: 0.14 },
  { id: "CC-ES-03", nome: "Usina 1", pct: 0.20 },
  { id: "CC-ES-04", nome: "Usina 2", pct: 0.18 },
  { id: "CC-ES-05", nome: "Usina 3", pct: 0.15 },
  { id: "CC-ES-06", nome: "Usina 4", pct: 0.12 },
  { id: "CC-ES-07", nome: "Estocagem", pct: 0.05 },
];

const TREEMAP_COLORS = ["#004071", "#00567a", "#006b87", "#008093", "#00959e", "#00aaa8", "#00bfb1", "#00D8FF", "#457ba9", "#2d6a9f", "#1a5a94", "#0a4a89"];

const CustomTooltip = ({ active, payload, label }: any) => {
  if (!active || !payload?.length) return null;
  return (
    <div style={{ ...RECHARTS_TOOLTIP_STYLE, padding: "10px 14px" }}>
      <p style={{ color: "#8a9bb5", marginBottom: 6, fontWeight: 600 }}>{label}</p>
      {payload.map((e: any, i: number) => (
        <div key={i} style={{ color: e.color, fontSize: "0.75rem", marginBottom: 2 }}>
          <span style={{ fontWeight: 600 }}>{e.name}: </span>
          <span>{formatCurrency(e.value)}</span>
        </div>
      ))}
    </div>
  );
};

export default function CentroCustos() {
  const { data, loading, hasError, errorMessage, isLiveData, refetch } = useMotorCalculo();
  const { period } = usePeriod();
  const filteredMonths = getFilteredMonths(period);
  const [activeTab, setActiveTab] = useState<"MG" | "ES">("MG");

  const fConsumoMG = filterByPeriod(data.classeCustoConsumoMG, period);
  const fFioMG = filterByPeriod(data.classeCustoTusdFioMG, period);
  const fEncargoMG = filterByPeriod(data.classeCustoEncargoMG, period);
  const fConsumoES = filterByPeriod(data.classeCustoConsumoES, period);
  const fFioES = filterByPeriod(data.classeCustoTusdFioES, period);
  const fEncargoES = filterByPeriod(data.classeCustoEncargoES, period);

  const sum = (arr: { mes: string; valor: number }[]) => arr.reduce((a, b) => a + b.valor, 0);
  const totalConsumoMG = sum(fConsumoMG);
  const totalFioMG = sum(fFioMG);
  const totalEncargoMG = sum(fEncargoMG);
  const totalConsumoES = sum(fConsumoES);
  const totalFioES = sum(fFioES);
  const totalEncargoES = sum(fEncargoES);
  const totalMG = totalConsumoMG + totalFioMG + totalEncargoMG;
  const totalES = totalConsumoES + totalFioES + totalEncargoES;
  const totalSamarco = totalMG + totalES;

  // Build CC table data
  const buildCCData = (ccs: typeof CC_MG, totalConsumo: number, totalFio: number, totalEncargo: number) =>
    ccs.map((cc) => ({
      ...cc,
      consumo: totalConsumo * cc.pct,
      fio: totalFio * cc.pct,
      encargo: totalEncargo * cc.pct,
      total: (totalConsumo + totalFio + totalEncargo) * cc.pct,
    }));

  const ccMGData = useMemo(() => buildCCData(CC_MG, totalConsumoMG, totalFioMG, totalEncargoMG), [totalConsumoMG, totalFioMG, totalEncargoMG]);
  const ccESData = useMemo(() => buildCCData(CC_ES, totalConsumoES, totalFioES, totalEncargoES), [totalConsumoES, totalFioES, totalEncargoES]);

  // Treemap data (all CCs combined)
  const treemapData = useMemo(() => [
    ...ccMGData.map((cc) => ({ name: `MG: ${cc.nome}`, value: cc.total, state: "MG" })),
    ...ccESData.map((cc) => ({ name: `ES: ${cc.nome}`, value: cc.total, state: "ES" })),
  ].sort((a, b) => b.value - a.value), [ccMGData, ccESData]);

  // Bar chart: top CCs
  const barData = useMemo(() =>
    treemapData.slice(0, 10).map((d) => ({ name: d.name.replace(/^(MG|ES): /, ""), value: d.value, state: d.state })),
    [treemapData]);

  const periodLabel = period.type === "all" ? "Ano Base 2026" :
    period.type === "quarter" ? `T${period.value} 2026` : `${filteredMonths[0]} 2026`;

  const tabStyle = (active: boolean): React.CSSProperties => ({
    padding: "6px 20px", borderRadius: 6, border: "1px solid",
    borderColor: active ? "#00D8FF" : "#1e3a5f",
    background: active ? "rgba(0,216,255,0.1)" : "transparent",
    color: active ? "#00D8FF" : "#8a9bb5",
    fontWeight: active ? 700 : 500, fontSize: "0.82rem",
    cursor: "pointer", fontFamily: "'Montserrat', sans-serif",
  });

  const activeCC = activeTab === "MG" ? ccMGData : ccESData;
  const activeTotal = activeTab === "MG" ? totalMG : totalES;


  if (loading) return <LoadingSkeleton />;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      {/* Header */}
      <div className="flex items-start justify-between gap-3" style={{ flexWrap: "wrap" }}>
        <div>
          <h1 style={{ color: "#e8edf2", fontSize: "1.4rem", fontWeight: 800, margin: 0 }}>
            Cálculo 15 — Distribuição por Centro de Custos (R$)
          </h1>
          <p style={{ color: "#8a9bb5", fontSize: "0.8rem", margin: "4px 0 0" }}>
            15 centros de custo (8 MG + 7 ES) · {periodLabel}
          </p>
        </div>
        <div className="flex gap-2" style={{ flexWrap: "wrap" }}>
          <span className="badge-accent">Aba 15</span>
          <span style={{ background: "rgba(69,123,169,0.15)", color: "#457ba9", border: "1px solid rgba(69,123,169,0.3)", padding: "2px 10px", borderRadius: 20, fontSize: "0.75rem", fontWeight: 600 }}>
            Depende: 03 + 14
          </span>
        </div>
      </div>

      {/* Info Card */}
      <div className="samarco-card" style={{ borderLeft: "3px solid #00D8FF" }}>
        <div style={{ fontSize: "0.78rem", color: "#8a9bb5", fontWeight: 600, marginBottom: 10, textTransform: "uppercase", letterSpacing: "0.05em" }}>Metodologia</div>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: 12 }}>
          {[
            ["Valor por CC", "Total da Classe (estado) × Percentual do CC"],
            ["Percentuais", "Calculados a partir do consumo relativo de cada centro"],
            ["MG", "8 centros (Filtragem, Mineração, Benef 1-3, Mineroduto 1-3)"],
            ["ES", "7 centros (Preparação 1-2, Usina 1-4, Estocagem)"],
          ].map(([label, desc]) => (
            <div key={label}>
              <span style={{ color: "#00D8FF", fontWeight: 700, fontSize: "0.78rem" }}>{label}</span>
              <span style={{ color: "#8a9bb5", fontSize: "0.78rem" }}> = {desc}</span>
            </div>
          ))}
        </div>
      </div>

      {/* KPI Summary Cards */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 16 }}>
        {[
          { title: "Total Consumo Samarco", value: formatCurrency(totalConsumoMG + totalConsumoES), subtitle: "Classe 50610002", icon: "⚡", color: "#ffa726" },
          { title: "Total Uso Rede Samarco", value: formatCurrency(totalFioMG + totalFioES), subtitle: "Classe 50610003", icon: "🔌", color: "#00c853" },
          { title: "Total Encargo Samarco", value: formatCurrency(totalEncargoMG + totalEncargoES), subtitle: "Classe 50610006", icon: "📋", color: "#457ba9" },
          { title: "Total Geral Samarco", value: formatCurrency(totalSamarco), subtitle: "MG + ES", icon: "💰", color: "#00D8FF" },
        ].map((kpi) => (
          <div key={kpi.title} className="samarco-card" style={{ flex: 1, minWidth: 0 }}>
            <div className="flex items-start justify-between gap-2">
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.05em", marginBottom: 6 }}>{kpi.title}</div>
                <div style={{ color: kpi.color, fontSize: "1.2rem", fontWeight: 800, lineHeight: 1.1, marginBottom: 4 }}>{kpi.value}</div>
                <div style={{ color: "#4a5a72", fontSize: "0.7rem" }}>{kpi.subtitle}</div>
              </div>
              <div style={{ fontSize: "1.4rem", background: `${kpi.color}15`, border: `1px solid ${kpi.color}30`, borderRadius: 10, padding: "6px 8px", lineHeight: 1 }}>{kpi.icon}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Charts */}
      <div className="flex gap-4" style={{ flexWrap: "wrap" }}>
        {/* Donut Treemap-style */}
        <div className="samarco-card" style={{ flex: "1 1 280px" }}>
          <div style={{ marginBottom: 16 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Proporção por Centro de Custo</h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Top 10 CCs — % do custo total</p>
          </div>
          <ResponsiveContainer width="100%" height={260}>
            <PieChart>
              <Pie data={treemapData.slice(0, 10)} cx="50%" cy="50%" outerRadius={100} dataKey="value" nameKey="name" label={({ name, percent }) => `${(percent * 100).toFixed(0)}%`} labelLine={false}>
                {treemapData.slice(0, 10).map((_, i) => <Cell key={i} fill={TREEMAP_COLORS[i % TREEMAP_COLORS.length]}/>)}
              </Pie>
              <Tooltip formatter={(v: number) => [formatCurrency(v), ""]} contentStyle={RECHARTS_TOOLTIP_STYLE}/>
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* Bar Chart Top CCs */}
        <div className="samarco-card" style={{ flex: "2 1 380px" }}>
          <div style={{ marginBottom: 16 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Top 10 Centros de Custo (R$)</h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Ranking por custo total</p>
          </div>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={barData} layout="vertical" margin={{ top: 5, right: 20, left: 100, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5} horizontal={false}/>
              <XAxis type="number" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false} tickFormatter={(v) => `${(v/1000000).toFixed(0)}M`}/>
              <YAxis type="category" dataKey="name" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false} width={95}/>
              <Tooltip formatter={(v: number) => [formatCurrency(v), "Custo Total"]} contentStyle={RECHARTS_TOOLTIP_STYLE}/>
              <Bar dataKey="value" name="Custo Total" radius={[0, 3, 3, 0]}>
                {barData.map((entry, i) => <Cell key={i} fill={entry.state === "MG" ? CHART_COLORS.mg : CHART_COLORS.es}/>)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Table with tabs */}
      <div className="samarco-card">
        <div className="flex items-center justify-between" style={{ marginBottom: 16, flexWrap: "wrap", gap: 12 }}>
          <div>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Detalhamento por Centro de Custo</h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>{periodLabel}</p>
          </div>
          <div className="flex gap-2">
            <button style={tabStyle(activeTab === "MG")} onClick={() => setActiveTab("MG")}>🏔️ Minas Gerais</button>
            <button style={tabStyle(activeTab === "ES")} onClick={() => setActiveTab("ES")}>🌊 Espírito Santo</button>
          </div>
        </div>
        <div style={{ overflowX: "auto" }}>
          <table className="samarco-table">
            <thead>
              <tr>
                <th>Centro de Custo</th>
                <th>% Consumo</th>
                <th>Consumo (R$)</th>
                <th>Uso de Rede (R$)</th>
                <th>Encargo (R$)</th>
                <th>Total (R$)</th>
              </tr>
            </thead>
            <tbody>
              {activeCC.map((cc) => (
                <tr key={cc.id}>
                  <td style={{ color: "#e8edf2", fontWeight: 600 }}>{cc.nome}</td>
                  <td style={{ color: "#8a9bb5" }}>{(cc.pct * 100).toFixed(1)}%</td>
                  <td>{formatCurrency(cc.consumo)}</td>
                  <td>{formatCurrency(cc.fio)}</td>
                  <td>{formatCurrency(cc.encargo)}</td>
                  <td style={{ color: "#00D8FF", fontWeight: 700 }}>{formatCurrency(cc.total)}</td>
                </tr>
              ))}
              <tr className="total-row">
                <td>TOTAL {activeTab}</td>
                <td>100%</td>
                <td>{formatCurrency(activeTab === "MG" ? totalConsumoMG : totalConsumoES)}</td>
                <td>{formatCurrency(activeTab === "MG" ? totalFioMG : totalFioES)}</td>
                <td>{formatCurrency(activeTab === "MG" ? totalEncargoMG : totalEncargoES)}</td>
                <td>{formatCurrency(activeTotal)}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
