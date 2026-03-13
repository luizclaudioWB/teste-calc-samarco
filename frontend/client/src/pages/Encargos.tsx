import { useMotorCalculo } from "../hooks/useMotorCalculo";
import { LoadingSkeleton, ErrorBanner, DataSourceTag } from "../components/ApiComponents";
import { useMemo } from "react";
import {
  BarChart, Bar,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from "recharts";

import { MESES, formatCurrency, formatNumber, formatMWh, sumArray, getByMes, CHART_COLORS, RECHARTS_TOOLTIP_STYLE } from "../lib/utils";

const CustomTooltip = ({ active, payload, label }: any) => {
  if (active && payload && payload.length) {
    return (
      <div style={{ ...RECHARTS_TOOLTIP_STYLE, padding: "10px 14px" }}>
        <p style={{ color: "#8a9bb5", marginBottom: 6, fontWeight: 600 }}>{label}</p>
        {payload.map((entry: any, i: number) => (
          <div key={i} style={{ color: entry.color, fontSize: "0.75rem", marginBottom: 2 }}>
            <span style={{ fontWeight: 600 }}>{entry.name}: </span>
            <span>{formatCurrency(entry.value)}</span>
          </div>
        ))}
      </div>
    );
  }
  return null;
};

export default function Encargos() {
  const { data, loading, hasError, errorMessage, isLiveData, refetch } = useMotorCalculo();

  const totalEER = useMemo(() => sumArray(data.valorTotalEER), []);
  const totalESS = useMemo(() => sumArray(data.valorTotalESS), []);
  const totalMG = useMemo(() => sumArray(data.mgTotal), []);
  const totalES = useMemo(() => sumArray(data.esTotal), []);
  const totalConsumoSamarco = useMemo(() => sumArray(data.consumoSamarcoMWh), []);

  const monthlyData = useMemo(() =>
    MESES.map((mes) => ({
      mes,
      consumoSamarco: getByMes(data.consumoSamarcoMWh, mes),
      eer: getByMes(data.valorTotalEER, mes),
      ess: getByMes(data.valorTotalESS, mes),
      mg: getByMes(data.mgTotal, mes),
      es: getByMes(data.esTotal, mes),
      mgEER: getByMes(data.mgEER, mes),
      mgESS: getByMes(data.mgESS, mes),
      esEER: getByMes(data.esEER, mes),
      esESS: getByMes(data.esESS, mes),
      total: getByMes(data.mgTotal, mes) + getByMes(data.esTotal, mes),
    })), []);


  if (loading) return <LoadingSkeleton />;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      {/* Header */}
      <div className="flex items-center gap-3" style={{ flexWrap: "wrap" }}>
        <div>
          <h1 style={{ color: "#e8edf2", fontSize: "1.4rem", fontWeight: 800, margin: 0 }}>
            Cálculo 05 — Encargos ESS/EER (R$)
          </h1>
          <p style={{ color: "#8a9bb5", fontSize: "0.8rem", margin: "4px 0 0" }}>
            Encargos por estado · MG e ES · Ano Base 2026
          </p>
        </div>
        <div className="flex gap-2" style={{ marginLeft: "auto" }}>
          <span className="badge-accent">Aba 12 do Excel</span>
          <span className="badge-warning">Depende: Cálc. 03 + 04</span>
        </div>
      </div>

      {/* Fórmulas */}
      <div className="samarco-card" style={{ background: "rgba(0, 64, 113, 0.15)", borderColor: "#1e3a5f" }}>
        <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: 10 }}>
          Fórmulas
        </div>
        <div className="flex gap-6" style={{ flexWrap: "wrap" }}>
          {[
            { label: "Consumo Samarco", formula: "Consumo Área Total − Geração Própria Total" },
            { label: "Valor EER", formula: "Consumo Samarco × (ERR + ERCAP)" },
            { label: "Valor ESS", formula: "ESS × Consumo Samarco" },
          ].map((f) => (
            <div key={f.label}>
              <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600 }}>{f.label}</div>
              <div style={{ color: "#00D8FF", fontFamily: "monospace", fontSize: "0.82rem", marginTop: 3 }}>{f.formula}</div>
            </div>
          ))}
        </div>
      </div>

      {/* KPI Cards */}
      <div className="flex gap-4" style={{ flexWrap: "wrap" }}>
        {[
          { label: "Total EER Anual", value: formatCurrency(totalEER), color: "#00D8FF", icon: "⚡" },
          { label: "Total ESS Anual", value: formatCurrency(totalESS), color: "#00c853", icon: "🔋" },
          { label: "Total MG", value: formatCurrency(totalMG), color: "#457ba9", icon: "🏔️" },
          { label: "Total ES", value: formatCurrency(totalES), color: "#00D8FF", icon: "🌊" },
          { label: "Total Geral", value: formatCurrency(totalMG + totalES), color: "#ef5350", icon: "💰" },
        ].map((kpi) => (
          <div key={kpi.label} className="samarco-card" style={{ flex: "1 1 150px", textAlign: "center" }}>
            <div style={{ fontSize: "1.4rem", marginBottom: 6 }}>{kpi.icon}</div>
            <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em" }}>{kpi.label}</div>
            <div style={{ color: kpi.color, fontSize: "1rem", fontWeight: 800, marginTop: 4 }}>{kpi.value}</div>
          </div>
        ))}
      </div>

      {/* Gráficos */}
      <div className="flex gap-4" style={{ flexWrap: "wrap" }}>
        {/* Grouped Bar MG vs ES */}
        <div className="samarco-card" style={{ flex: "1 1 300px" }}>
          <div style={{ marginBottom: 16 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>
              Encargos por Estado (R$/mês)
            </h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>MG vs ES</p>
          </div>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={monthlyData} margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5}/>
              <XAxis dataKey="mes" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={{ stroke: "#1e3a5f" }} tickLine={false}/>
              <YAxis tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false}
                tickFormatter={(v) => `${(v/1000000).toFixed(1)}M`}/>
              <Tooltip content={<CustomTooltip />} />
              <Legend wrapperStyle={{ color: "#8a9bb5", fontSize: "0.75rem" }}/>
              <Bar dataKey="mg" name="Minas Gerais" fill={CHART_COLORS.mg} radius={[3, 3, 0, 0]}/>
              <Bar dataKey="es" name="Espírito Santo" fill={CHART_COLORS.es} radius={[3, 3, 0, 0]}/>
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Stacked Bar EER vs ESS */}
        <div className="samarco-card" style={{ flex: "1 1 300px" }}>
          <div style={{ marginBottom: 16 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>
              Composição EER + ESS (R$/mês)
            </h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Nacional</p>
          </div>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={monthlyData} margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5}/>
              <XAxis dataKey="mes" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={{ stroke: "#1e3a5f" }} tickLine={false}/>
              <YAxis tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false}
                tickFormatter={(v) => `${(v/1000000).toFixed(1)}M`}/>
              <Tooltip content={<CustomTooltip />} />
              <Legend wrapperStyle={{ color: "#8a9bb5", fontSize: "0.75rem" }}/>
              <Bar dataKey="eer" name="EER" stackId="a" fill={CHART_COLORS.eer} radius={[0, 0, 0, 0]}/>
              <Bar dataKey="ess" name="ESS" stackId="a" fill={CHART_COLORS.ess} radius={[3, 3, 0, 0]}/>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Tabela Nacional */}
      <div className="samarco-card">
        <div style={{ marginBottom: 12 }}>
          <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>
            Nacional — Encargos Mensais (R$)
          </h3>
        </div>
        <div style={{ overflowX: "auto" }}>
          <table className="samarco-table">
            <thead>
              <tr>
                <th>Métrica</th>
                {MESES.map((m) => <th key={m}>{m}</th>)}
                <th style={{ color: "#00D8FF" }}>Total</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Consumo Samarco (MWh)</td>
                {monthlyData.map((d) => <td key={d.mes}>{formatNumber(d.consumoSamarco)}</td>)}
                <td style={{ color: "#ffa726", fontWeight: 700 }}>{formatNumber(totalConsumoSamarco)}</td>
              </tr>
              <tr>
                <td>Valor Total EER (R$)</td>
                {monthlyData.map((d) => <td key={d.mes}>{formatCurrency(d.eer)}</td>)}
                <td style={{ color: "#00D8FF", fontWeight: 700 }}>{formatCurrency(totalEER)}</td>
              </tr>
              <tr>
                <td>Valor Total ESS (R$)</td>
                {monthlyData.map((d) => <td key={d.mes}>{formatCurrency(d.ess)}</td>)}
                <td style={{ color: "#00c853", fontWeight: 700 }}>{formatCurrency(totalESS)}</td>
              </tr>
              <tr className="total-row">
                <td>Total Encargos (R$)</td>
                {monthlyData.map((d) => <td key={d.mes}>{formatCurrency(d.total)}</td>)}
                <td>{formatCurrency(totalMG + totalES)}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Tabelas por Estado */}
      <div className="flex gap-4" style={{ flexWrap: "wrap" }}>
        {/* Minas Gerais */}
        <div className="samarco-card" style={{ flex: "1 1 400px" }}>
          <div style={{ marginBottom: 12 }}>
            <div className="flex items-center gap-2">
              <span style={{ color: "#457ba9", fontSize: "1.1rem" }}>🏔️</span>
              <h3 style={{ color: "#457ba9", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Minas Gerais</h3>
              <span style={{ color: "#4a5a72", fontSize: "0.7rem", marginLeft: "auto" }}>
                EER: 50610007 | ESS: 50610008
              </span>
            </div>
          </div>
          <div style={{ overflowX: "auto" }}>
            <table className="samarco-table">
              <thead>
                <tr>
                  <th>Encargo</th>
                  {MESES.map((m) => <th key={m}>{m}</th>)}
                  <th>Total</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td style={{ color: "#00D8FF" }}>EER (R$)</td>
                  {monthlyData.map((d) => <td key={d.mes}>{formatCurrency(d.mgEER)}</td>)}
                  <td style={{ color: "#00D8FF", fontWeight: 700 }}>{formatCurrency(sumArray(data.mgEER))}</td>
                </tr>
                <tr>
                  <td style={{ color: "#00c853" }}>ESS (R$)</td>
                  {monthlyData.map((d) => <td key={d.mes}>{formatCurrency(d.mgESS)}</td>)}
                  <td style={{ color: "#00c853", fontWeight: 700 }}>{formatCurrency(sumArray(data.mgESS))}</td>
                </tr>
                <tr className="total-row">
                  <td>Total (R$)</td>
                  {monthlyData.map((d) => <td key={d.mes}>{formatCurrency(d.mg)}</td>)}
                  <td>{formatCurrency(totalMG)}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        {/* Espírito Santo */}
        <div className="samarco-card" style={{ flex: "1 1 400px" }}>
          <div style={{ marginBottom: 12 }}>
            <div className="flex items-center gap-2">
              <span style={{ color: "#00D8FF", fontSize: "1.1rem" }}>🌊</span>
              <h3 style={{ color: "#00D8FF", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Espírito Santo</h3>
            </div>
          </div>
          <div style={{ overflowX: "auto" }}>
            <table className="samarco-table">
              <thead>
                <tr>
                  <th>Encargo</th>
                  {MESES.map((m) => <th key={m}>{m}</th>)}
                  <th>Total</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td style={{ color: "#00D8FF" }}>EER (R$)</td>
                  {monthlyData.map((d) => <td key={d.mes}>{formatCurrency(d.esEER)}</td>)}
                  <td style={{ color: "#00D8FF", fontWeight: 700 }}>{formatCurrency(sumArray(data.esEER))}</td>
                </tr>
                <tr>
                  <td style={{ color: "#00c853" }}>ESS (R$)</td>
                  {monthlyData.map((d) => <td key={d.mes}>{formatCurrency(d.esESS)}</td>)}
                  <td style={{ color: "#00c853", fontWeight: 700 }}>{formatCurrency(sumArray(data.esESS))}</td>
                </tr>
                <tr className="total-row">
                  <td>Total (R$)</td>
                  {monthlyData.map((d) => <td key={d.mes}>{formatCurrency(d.es)}</td>)}
                  <td>{formatCurrency(totalES)}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
