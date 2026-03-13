import { useMotorCalculo } from "../hooks/useMotorCalculo";
import { LoadingSkeleton, ErrorBanner, DataSourceTag } from "../components/ApiComponents";
import { useMemo, useState } from "react";
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from "recharts";

import { formatCurrency, formatNumber, getByMes, CHART_COLORS, RECHARTS_TOOLTIP_STYLE } from "../lib/utils";
import { usePeriod, filterByPeriod, getFilteredMonths } from "../components/SamarcoLayout";

function KpiCard({ title, value, subtitle, icon, color }: { title: string; value: string; subtitle?: string; icon: string; color: string }) {
  return (
    <div className="samarco-card" style={{ flex: 1, minWidth: 0 }}>
      <div className="flex items-start justify-between gap-2">
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ color: "#8a9bb5", fontSize: "0.72rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: 6 }}>{title}</div>
          <div style={{ color, fontSize: "1.3rem", fontWeight: 800, lineHeight: 1.1, marginBottom: 4 }}>{value}</div>
          {subtitle && <div style={{ color: "#4a5a72", fontSize: "0.72rem" }}>{subtitle}</div>}
        </div>
        <div style={{ fontSize: "1.6rem", background: `${color}15`, border: `1px solid ${color}30`, borderRadius: 10, padding: "8px 10px", lineHeight: 1 }}>{icon}</div>
      </div>
    </div>
  );
}

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

export default function ClasseCusto() {
  const { data, loading, hasError, errorMessage, isLiveData, refetch } = useMotorCalculo();
  const { period } = usePeriod();
  const filteredMonths = getFilteredMonths(period);
  const [activeTab, setActiveTab] = useState<"MG" | "ES">("MG");

  const fTusdFioMG = filterByPeriod(data.classeCustoTusdFioMG, period);
  const fTusdFioES = filterByPeriod(data.classeCustoTusdFioES, period);
  const fEncargoMG = filterByPeriod(data.classeCustoEncargoMG, period);
  const fEncargoES = filterByPeriod(data.classeCustoEncargoES, period);
  const fConsumoMG = filterByPeriod(data.classeCustoConsumoMG, period);
  const fConsumoES = filterByPeriod(data.classeCustoConsumoES, period);

  const sum = (arr: { mes: string; valor: number }[]) => arr.reduce((a, b) => a + b.valor, 0);
  const totalTusdFioMG = useMemo(() => sum(fTusdFioMG), [fTusdFioMG]);
  const totalTusdFioES = useMemo(() => sum(fTusdFioES), [fTusdFioES]);
  const totalEncargoMG = useMemo(() => sum(fEncargoMG), [fEncargoMG]);
  const totalEncargoES = useMemo(() => sum(fEncargoES), [fEncargoES]);
  const totalConsumoMG = useMemo(() => sum(fConsumoMG), [fConsumoMG]);
  const totalConsumoES = useMemo(() => sum(fConsumoES), [fConsumoES]);

  const totalMG = totalTusdFioMG + totalEncargoMG + totalConsumoMG;
  const totalES = totalTusdFioES + totalEncargoES + totalConsumoES;
  const totalConsumo = totalConsumoMG + totalConsumoES;
  const totalFio = totalTusdFioMG + totalTusdFioES;

  const stackedData = useMemo(() =>
    filteredMonths.map((mes) => ({
      mes,
      consumoMG: getByMes(data.classeCustoConsumoMG, mes),
      fioMG: getByMes(data.classeCustoTusdFioMG, mes),
      encargoMG: getByMes(data.classeCustoEncargoMG, mes),
      consumoES: getByMes(data.classeCustoConsumoES, mes),
      fioES: getByMes(data.classeCustoTusdFioES, mes),
      encargoES: getByMes(data.classeCustoEncargoES, mes),
    })), [filteredMonths]);

  const groupedData = useMemo(() =>
    filteredMonths.map((mes) => ({
      mes,
      MG: getByMes(data.classeCustoConsumoMG, mes) + getByMes(data.classeCustoTusdFioMG, mes) + getByMes(data.classeCustoEncargoMG, mes),
      ES: getByMes(data.classeCustoConsumoES, mes) + getByMes(data.classeCustoTusdFioES, mes) + getByMes(data.classeCustoEncargoES, mes),
    })), [filteredMonths]);

  const tableDataMG = useMemo(() =>
    filteredMonths.map((mes) => ({
      mes,
      fio: getByMes(data.classeCustoTusdFioMG, mes),
      encargo: getByMes(data.classeCustoEncargoMG, mes),
      consumo: getByMes(data.classeCustoConsumoMG, mes),
      total: getByMes(data.classeCustoTusdFioMG, mes) + getByMes(data.classeCustoEncargoMG, mes) + getByMes(data.classeCustoConsumoMG, mes),
    })), [filteredMonths]);

  const tableDataES = useMemo(() =>
    filteredMonths.map((mes) => ({
      mes,
      fio: getByMes(data.classeCustoTusdFioES, mes),
      encargo: getByMes(data.classeCustoEncargoES, mes),
      consumo: getByMes(data.classeCustoConsumoES, mes),
      total: getByMes(data.classeCustoTusdFioES, mes) + getByMes(data.classeCustoEncargoES, mes) + getByMes(data.classeCustoConsumoES, mes),
    })), [filteredMonths]);

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

  const activeTable = activeTab === "MG" ? tableDataMG : tableDataES;
  const activeTotals = activeTab === "MG"
    ? { fio: totalTusdFioMG, encargo: totalEncargoMG, consumo: totalConsumoMG, total: totalMG }
    : { fio: totalTusdFioES, encargo: totalEncargoES, consumo: totalConsumoES, total: totalES };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      {/* Header */}
      <div className="flex items-start justify-between gap-3" style={{ flexWrap: "wrap" }}>
        <div>
          <h1 style={{ color: "#e8edf2", fontSize: "1.4rem", fontWeight: 800, margin: 0 }}>
            Cálculo 14 — Classe de Custo (R$)
          </h1>
          <p style={{ color: "#8a9bb5", fontSize: "0.8rem", margin: "4px 0 0" }}>
            Custo por classe e estado · {periodLabel}
          </p>
        </div>
        <div className="flex gap-2" style={{ flexWrap: "wrap" }}>
          <span className="badge-accent">Aba 14</span>
          <span style={{ background: "rgba(69,123,169,0.15)", color: "#457ba9", border: "1px solid rgba(69,123,169,0.3)", padding: "2px 10px", borderRadius: 20, fontSize: "0.75rem", fontWeight: 600 }}>
            Depende: 03 + 04 + 13 + Tarifas
          </span>
        </div>
      </div>

      {/* Info Card */}
      <div className="samarco-card" style={{ borderLeft: "3px solid #00D8FF" }}>
        <div style={{ fontSize: "0.78rem", color: "#8a9bb5", fontWeight: 600, marginBottom: 10, textTransform: "uppercase", letterSpacing: "0.05em" }}>Classes de Custo</div>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: 12 }}>
          {[
            ["50610002 — Consumo de Energia", "Quantidade comprada × PMIX"],
            ["50610003 — Uso de Rede (TUSD FIO)", "Demanda × tarifa (ponta + fora ponta)"],
            ["50610006 — Encargo (TUSD)", "Consumo MWh × tarifa − crédito PROINFA"],
          ].map(([label, desc]) => (
            <div key={label}>
              <span style={{ color: "#00D8FF", fontWeight: 700, fontSize: "0.78rem" }}>{label}</span>
              <span style={{ color: "#8a9bb5", fontSize: "0.78rem" }}> = {desc}</span>
            </div>
          ))}
        </div>
      </div>

      {/* KPIs */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 16 }}>
        <KpiCard title="Total MG" value={formatCurrency(totalMG)} subtitle="Custo total Minas Gerais" icon="🏔️" color="#457ba9"/>
        <KpiCard title="Total ES" value={formatCurrency(totalES)} subtitle="Custo total Espírito Santo" icon="🌊" color="#00D8FF"/>
        <KpiCard title="Total Consumo Energia" value={formatCurrency(totalConsumo)} subtitle="Classe 50610002" icon="⚡" color="#ffa726"/>
        <KpiCard title="Total Uso de Rede" value={formatCurrency(totalFio)} subtitle="Classe 50610003" icon="🔌" color="#00c853"/>
      </div>

      {/* Charts */}
      <div className="flex gap-4" style={{ flexWrap: "wrap" }}>
        <div className="samarco-card" style={{ flex: "1 1 300px" }}>
          <div style={{ marginBottom: 16 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Composição por Classe (R$/mês)</h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Stacked — 3 classes · MG</p>
          </div>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={stackedData} margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5}/>
              <XAxis dataKey="mes" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={{ stroke: "#1e3a5f" }} tickLine={false}/>
              <YAxis tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false} tickFormatter={(v) => `${(v/1000000).toFixed(0)}M`}/>
              <Tooltip content={<CustomTooltip />}/>
              <Legend wrapperStyle={{ color: "#8a9bb5", fontSize: "0.75rem" }}/>
              <Bar dataKey="consumoMG" name="Consumo MG" stackId="a" fill="#ffa726"/>
              <Bar dataKey="fioMG" name="TUSD Fio MG" stackId="a" fill={CHART_COLORS.mg}/>
              <Bar dataKey="encargoMG" name="Encargo MG" stackId="a" fill="#00c853" radius={[3, 3, 0, 0]}/>
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="samarco-card" style={{ flex: "1 1 300px" }}>
          <div style={{ marginBottom: 16 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>MG vs ES por Mês (R$)</h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Grouped Bar — Total por estado</p>
          </div>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={groupedData} margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5}/>
              <XAxis dataKey="mes" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={{ stroke: "#1e3a5f" }} tickLine={false}/>
              <YAxis tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false} tickFormatter={(v) => `${(v/1000000).toFixed(0)}M`}/>
              <Tooltip content={<CustomTooltip />}/>
              <Legend wrapperStyle={{ color: "#8a9bb5", fontSize: "0.75rem" }}/>
              <Bar dataKey="MG" name="Minas Gerais" fill={CHART_COLORS.mg} radius={[3, 3, 0, 0]}/>
              <Bar dataKey="ES" name="Espírito Santo" fill={CHART_COLORS.es} radius={[3, 3, 0, 0]}/>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Table with tabs */}
      <div className="samarco-card">
        <div className="flex items-center justify-between" style={{ marginBottom: 16, flexWrap: "wrap", gap: 12 }}>
          <div>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Detalhamento por Estado (R$/mês)</h3>
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
                <th>Classe de Custo</th>
                {filteredMonths.map((m) => <th key={m}>{m}</th>)}
                <th>Total</th>
              </tr>
            </thead>
            <tbody>
              {[
                { label: `50610003 — TUSD FIO ${activeTab}`, key: "fio" as const },
                { label: `50610006 — Encargo TUSD ${activeTab}`, key: "encargo" as const },
                { label: `50610002 — Consumo Energia ${activeTab}`, key: "consumo" as const },
              ].map(({ label, key }) => {
                const vals = activeTable.map((r) => r[key]);

  if (loading) return <LoadingSkeleton />;

                return (
                  <tr key={key}>
                    <td style={{ color: "#e8edf2" }}>{label}</td>
                    {vals.map((v, i) => <td key={i}>{formatCurrency(v)}</td>)}
                    <td style={{ color: "#00D8FF", fontWeight: 700 }}>{formatCurrency(activeTotals[key])}</td>
                  </tr>
                );
              })}
              <tr className="total-row">
                <td>TOTAL {activeTab}</td>
                {activeTable.map((r, i) => <td key={i}>{formatCurrency(r.total)}</td>)}
                <td>{formatCurrency(activeTotals.total)}</td>
              </tr>
            </tbody>
          </table>
        </div>
        {activeTab === "MG" && (
          <div style={{ marginTop: 12, padding: "8px 12px", background: "rgba(69,123,169,0.08)", borderRadius: 6, fontSize: "0.75rem", color: "#8a9bb5" }}>
            ℹ️ Crédito PROINFA MG = Geração Guilman Amorim × Desconto Germano (50610007 EER/ERCAP | 50610008 ESS)
          </div>
        )}
        {activeTab === "ES" && (
          <div style={{ marginTop: 12, padding: "8px 12px", background: "rgba(0,216,255,0.06)", borderRadius: 6, fontSize: "0.75rem", color: "#8a9bb5" }}>
            ℹ️ Crédito PROINFA ES = Geração Muniz Freire × Desconto UBU (Ponto de Entrega: UBU/ES)
          </div>
        )}
      </div>
    </div>
  );
}
