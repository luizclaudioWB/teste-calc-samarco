import { useMotorCalculo } from "../hooks/useMotorCalculo";
import { LoadingSkeleton, ErrorBanner, DataSourceTag } from "../components/ApiComponents";
import { useMemo } from "react";
import {
  BarChart, Bar, LineChart, Line,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from "recharts";

import { formatMWh, formatNumber, sumArray, getByMes, CHART_COLORS, RECHARTS_TOOLTIP_STYLE } from "../lib/utils";
import { usePeriod, filterByPeriod, getFilteredMonths } from "../components/SamarcoLayout";

function KpiCard({ title, value, subtitle, icon, color }: { title: string; value: string; subtitle?: string; icon: string; color: string }) {
  return (
    <div className="samarco-card" style={{ flex: 1, minWidth: 0 }}>
      <div className="flex items-start justify-between gap-2">
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ color: "#8a9bb5", fontSize: "0.72rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: 6 }}>{title}</div>
          <div style={{ color, fontSize: "1.4rem", fontWeight: 800, lineHeight: 1.1, marginBottom: 4 }}>{value}</div>
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
          <span>{formatNumber(e.value, 2)} MWh</span>
        </div>
      ))}
    </div>
  );
};

export default function DistribuicaoCarga() {
  const { data, loading, hasError, errorMessage, isLiveData, refetch } = useMotorCalculo();
  const { period } = usePeriod();

  const filteredMonths = getFilteredMonths(period);

  const fMG = filterByPeriod(data.distribCargaNecCompraMG, period);
  const fES = filterByPeriod(data.distribCargaNecCompraES, period);
  const fSamarco = filterByPeriod(data.distribCargaNecCompraSamarco, period);
  const fPerdasMG = filterByPeriod(data.distribCargaPerdasMG, period);
  const fPerdasES = filterByPeriod(data.distribCargaPerdasES, period);
  const fConsumoMG = filterByPeriod(data.consumoMG_MWh, period);
  const fConsumoES = filterByPeriod(data.consumoES_MWh, period);
  const fGeracaoGuilman = filterByPeriod(data.geracaoGuilmanMWh, period);
  const fGeracaoMuniz = filterByPeriod(data.geracaoMunizFreireMWh, period);

  const totalSamarco = useMemo(() => fSamarco.reduce((a, b) => a + b.valor, 0), [fSamarco]);
  const totalMG = useMemo(() => fMG.reduce((a, b) => a + b.valor, 0), [fMG]);
  const totalES = useMemo(() => fES.reduce((a, b) => a + b.valor, 0), [fES]);
  const totalPerdas = useMemo(() =>
    fPerdasMG.reduce((a, b) => a + b.valor, 0) + fPerdasES.reduce((a, b) => a + b.valor, 0),
    [fPerdasMG, fPerdasES]);

  const chartData = useMemo(() =>
    filteredMonths.map((mes) => ({
      mes,
      compraMG: getByMes(data.distribCargaNecCompraMG, mes),
      compraES: getByMes(data.distribCargaNecCompraES, mes),
    })), [filteredMonths]);

  const lineData = useMemo(() =>
    filteredMonths.map((mes) => ({
      mes,
      consumoMG: getByMes(data.consumoMG_MWh, mes),
      geracaoGuilman: getByMes(data.geracaoGuilmanMWh, mes),
      consumoES: getByMes(data.consumoES_MWh, mes),
      geracaoMuniz: getByMes(data.geracaoMunizFreireMWh, mes),
    })), [filteredMonths]);

  const tableRows = useMemo(() =>
    filteredMonths.map((mes) => ({
      mes,
      consumoMG: getByMes(data.consumoMG_MWh, mes),
      perdasMG: getByMes(data.distribCargaPerdasMG, mes),
      necTotalMG: getByMes(data.consumoMG_MWh, mes) + getByMes(data.distribCargaPerdasMG, mes),
      geracaoGuilman: getByMes(data.geracaoGuilmanMWh, mes),
      compraMG: getByMes(data.distribCargaNecCompraMG, mes),
      consumoES: getByMes(data.consumoES_MWh, mes),
      perdasES: getByMes(data.distribCargaPerdasES, mes),
      necTotalES: getByMes(data.consumoES_MWh, mes) + getByMes(data.distribCargaPerdasES, mes),
      geracaoMuniz: getByMes(data.geracaoMunizFreireMWh, mes),
      compraES: getByMes(data.distribCargaNecCompraES, mes),
      compraSamarco: getByMes(data.distribCargaNecCompraSamarco, mes),
    })), [filteredMonths]);

  const periodLabel = period.type === "all" ? "Ano Base 2026" :
    period.type === "quarter" ? `T${period.value} 2026` :
    `${filteredMonths[0]} 2026`;


  if (loading) return <LoadingSkeleton />;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      {/* Header */}
      <div className="flex items-start justify-between gap-3" style={{ flexWrap: "wrap" }}>
        <div>
          <h1 style={{ color: "#e8edf2", fontSize: "1.4rem", fontWeight: 800, margin: 0 }}>
            Cálculo 13 — Distribuição de Carga (MWh)
          </h1>
          <p style={{ color: "#8a9bb5", fontSize: "0.8rem", margin: "4px 0 0" }}>
            Balanço energético por estado · {periodLabel}
          </p>
        </div>
        <div className="flex gap-2" style={{ flexWrap: "wrap" }}>
          <span className="badge-accent">Aba 13</span>
          <span style={{ background: "rgba(69,123,169,0.15)", color: "#457ba9", border: "1px solid rgba(69,123,169,0.3)", padding: "2px 10px", borderRadius: 20, fontSize: "0.75rem", fontWeight: 600 }}>
            Depende: Cálc. 03 + 04
          </span>
        </div>
      </div>

      {/* Info Card */}
      <div className="samarco-card" style={{ borderLeft: "3px solid #00D8FF" }}>
        <div style={{ fontSize: "0.78rem", color: "#8a9bb5", fontWeight: 600, marginBottom: 10, textTransform: "uppercase", letterSpacing: "0.05em" }}>Fórmulas</div>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: 12 }}>
          {[
            ["Perdas", "Consumo × 3% (fator de perda da rede)"],
            ["Necessidade Total", "Consumo + Perdas"],
            ["Compra MG", "MAX(0, Nec. Total MG − Geração Guilman)"],
            ["Compra ES", "Nec. Total ES − Geração Muniz Freire"],
            ["Compra Samarco", "Compra MG + Compra ES"],
          ].map(([label, formula]) => (
            <div key={label}>
              <span style={{ color: "#457ba9", fontWeight: 700, fontSize: "0.78rem" }}>{label}</span>
              <span style={{ color: "#8a9bb5", fontSize: "0.78rem" }}> = {formula}</span>
            </div>
          ))}
        </div>
      </div>

      {/* KPIs */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 16 }}>
        <KpiCard title="Compra Total Samarco" value={formatMWh(totalSamarco)} subtitle="MG + ES" icon="⚡" color="#00D8FF"/>
        <KpiCard title="Compra MG" value={formatMWh(totalMG)} subtitle="Necessidade Minas Gerais" icon="🏔️" color="#457ba9"/>
        <KpiCard title="Compra ES" value={formatMWh(totalES)} subtitle="Necessidade Espírito Santo" icon="🌊" color="#00D8FF"/>
        <KpiCard title="Perdas Totais" value={formatMWh(totalPerdas)} subtitle="MG + ES (3%)" icon="⚠️" color="#ef5350"/>
      </div>

      {/* Charts */}
      <div className="flex gap-4" style={{ flexWrap: "wrap" }}>
        <div className="samarco-card" style={{ flex: "1 1 300px" }}>
          <div style={{ marginBottom: 16 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Necessidade de Compra por Estado (MWh/mês)</h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Stacked — MG + ES</p>
          </div>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={chartData} margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5}/>
              <XAxis dataKey="mes" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={{ stroke: "#1e3a5f" }} tickLine={false}/>
              <YAxis tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false} tickFormatter={(v) => `${(v/1000).toFixed(0)}k`}/>
              <Tooltip content={<CustomTooltip />}/>
              <Legend wrapperStyle={{ color: "#8a9bb5", fontSize: "0.75rem" }}/>
              <Bar dataKey="compraMG" name="Compra MG" stackId="a" fill={CHART_COLORS.mg} radius={[0, 0, 0, 0]}/>
              <Bar dataKey="compraES" name="Compra ES" stackId="a" fill={CHART_COLORS.es} radius={[3, 3, 0, 0]}/>
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="samarco-card" style={{ flex: "1 1 300px" }}>
          <div style={{ marginBottom: 16 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Consumo vs Geração por Estado (MWh/mês)</h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Necessidade vs Disponibilidade</p>
          </div>
          <ResponsiveContainer width="100%" height={240}>
            <LineChart data={lineData} margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5}/>
              <XAxis dataKey="mes" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={{ stroke: "#1e3a5f" }} tickLine={false}/>
              <YAxis tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false} tickFormatter={(v) => `${(v/1000).toFixed(0)}k`}/>
              <Tooltip content={<CustomTooltip />}/>
              <Legend wrapperStyle={{ color: "#8a9bb5", fontSize: "0.75rem" }}/>
              <Line type="monotone" dataKey="consumoMG" name="Consumo MG" stroke={CHART_COLORS.mg} strokeWidth={2} dot={false}/>
              <Line type="monotone" dataKey="geracaoGuilman" name="Geração Guilman" stroke="#00c853" strokeWidth={2} dot={false} strokeDasharray="5 5"/>
              <Line type="monotone" dataKey="consumoES" name="Consumo ES" stroke={CHART_COLORS.es} strokeWidth={2} dot={false}/>
              <Line type="monotone" dataKey="geracaoMuniz" name="Geração Muniz" stroke="#ffa726" strokeWidth={2} dot={false} strokeDasharray="5 5"/>
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Table */}
      <div className="samarco-card">
        <div style={{ marginBottom: 16 }}>
          <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Balanço Energético por Estado (MWh/mês)</h3>
          <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>{periodLabel}</p>
        </div>
        <div style={{ overflowX: "auto" }}>
          <table className="samarco-table">
            <thead>
              <tr>
                <th>Métrica</th>
                {filteredMonths.map((m) => <th key={m}>{m}</th>)}
                <th>Total</th>
              </tr>
            </thead>
            <tbody>
              {/* MG Section */}
              <tr><td colSpan={filteredMonths.length + 2} style={{ background: "rgba(69,123,169,0.1)", color: "#457ba9", fontWeight: 700, fontSize: "0.78rem", textAlign: "left", padding: "6px 12px" }}>🏔️ Minas Gerais</td></tr>
              {[
                { label: "Consumo MG (MWh)", vals: tableRows.map((r) => r.consumoMG) },
                { label: "Perdas MG (MWh)", vals: tableRows.map((r) => r.perdasMG) },
                { label: "Necessidade Total MG (MWh)", vals: tableRows.map((r) => r.necTotalMG) },
                { label: "Geração Guilman (MWh)", vals: tableRows.map((r) => r.geracaoGuilman) },
                { label: "Necessidade Compra MG (MWh)", vals: tableRows.map((r) => r.compraMG) },
              ].map(({ label, vals }) => (
                <tr key={label}>
                  <td style={{ color: "#e8edf2" }}>{label}</td>
                  {vals.map((v, i) => <td key={i}>{formatNumber(v, 2)}</td>)}
                  <td style={{ color: "#00D8FF", fontWeight: 700 }}>{formatNumber(vals.reduce((a, b) => a + b, 0), 2)}</td>
                </tr>
              ))}
              {/* ES Section */}
              <tr><td colSpan={filteredMonths.length + 2} style={{ background: "rgba(0,216,255,0.08)", color: "#00D8FF", fontWeight: 700, fontSize: "0.78rem", textAlign: "left", padding: "6px 12px" }}>🌊 Espírito Santo</td></tr>
              {[
                { label: "Consumo ES (MWh)", vals: tableRows.map((r) => r.consumoES) },
                { label: "Perdas ES (MWh)", vals: tableRows.map((r) => r.perdasES) },
                { label: "Necessidade Total ES (MWh)", vals: tableRows.map((r) => r.necTotalES) },
                { label: "Geração Muniz Freire (MWh)", vals: tableRows.map((r) => r.geracaoMuniz) },
                { label: "Necessidade Compra ES (MWh)", vals: tableRows.map((r) => r.compraES) },
              ].map(({ label, vals }) => (
                <tr key={label}>
                  <td style={{ color: "#e8edf2" }}>{label}</td>
                  {vals.map((v, i) => <td key={i}>{formatNumber(v, 2)}</td>)}
                  <td style={{ color: "#00D8FF", fontWeight: 700 }}>{formatNumber(vals.reduce((a, b) => a + b, 0), 2)}</td>
                </tr>
              ))}
              {/* Samarco Total */}
              <tr className="total-row">
                <td>⚡ Compra Total Samarco (MWh)</td>
                {tableRows.map((r, i) => <td key={i}>{formatNumber(r.compraSamarco, 2)}</td>)}
                <td>{formatNumber(tableRows.reduce((a, r) => a + r.compraSamarco, 0), 2)}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
