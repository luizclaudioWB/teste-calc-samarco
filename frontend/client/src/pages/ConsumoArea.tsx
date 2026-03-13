import { useMotorCalculo } from "../hooks/useMotorCalculo";
import { LoadingSkeleton, ErrorBanner, DataSourceTag } from "../components/ApiComponents";
import { useMemo } from "react";
import {
  BarChart, Bar, PieChart, Pie, Cell, LineChart, Line,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from "recharts";

import { MESES, formatMWh, formatNumber, formatPercent, sumArray, getByMes, CHART_COLORS, RECHARTS_TOOLTIP_STYLE } from "../lib/utils";

const CustomTooltip = ({ active, payload, label }: any) => {
  if (active && payload && payload.length) {
    return (
      <div style={{ ...RECHARTS_TOOLTIP_STYLE, padding: "10px 14px" }}>
        <p style={{ color: "#8a9bb5", marginBottom: 6, fontWeight: 600 }}>{label}</p>
        {payload.map((entry: any, i: number) => (
          <div key={i} style={{ color: entry.color, fontSize: "0.75rem", marginBottom: 2 }}>
            <span style={{ fontWeight: 600 }}>{entry.name}: </span>
            <span>{formatNumber(entry.value)} MWh</span>
          </div>
        ))}
      </div>
    );
  }
  return null;
};

export default function ConsumoArea() {
  const { data, loading, hasError, errorMessage, isLiveData, refetch } = useMotorCalculo();

  const totalMG = useMemo(() => sumArray(data.consumoMG_MWh), []);
  const totalES = useMemo(() => sumArray(data.consumoES_MWh), []);
  const totalGeral = useMemo(() => sumArray(data.consumoAreaTotalMWh), []);

  const monthlyData = useMemo(() =>
    MESES.map((mes) => ({
      mes,
      total: getByMes(data.consumoAreaTotalMWh, mes),
      mg: getByMes(data.consumoMG_MWh, mes),
      es: getByMes(data.consumoES_MWh, mes),
      percMG: getByMes(data.percentualMG, mes) * 100,
      percES: getByMes(data.percentualES, mes) * 100,
    })), []);

  const donutData = [
    { name: "Minas Gerais", value: totalMG, color: CHART_COLORS.mg },
    { name: "Espírito Santo", value: totalES, color: CHART_COLORS.es },
  ];

  const avgPercMG = useMemo(() => sumArray(data.percentualMG) / 12, []);
  const avgPercES = useMemo(() => sumArray(data.percentualES) / 12, []);


  if (loading) return <LoadingSkeleton />;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      {/* Header */}
      <div className="flex items-center gap-3" style={{ flexWrap: "wrap" }}>
        <div>
          <h1 style={{ color: "#e8edf2", fontSize: "1.4rem", fontWeight: 800, margin: 0 }}>
            Cálculo 03 — Consumo Área (MWh)
          </h1>
          <p style={{ color: "#8a9bb5", fontSize: "0.8rem", margin: "4px 0 0" }}>
            Distribuição por estado e variação sazonal · Ano Base 2026
          </p>
        </div>
        <div className="flex gap-2" style={{ marginLeft: "auto" }}>
          <span className="badge-accent">Aba 7 do Excel</span>
          <span className="badge-warning">Depende: Cálc. 01 + 02</span>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="flex gap-4" style={{ flexWrap: "wrap" }}>
        {[
          { label: "Consumo Total Anual", value: formatMWh(totalGeral), color: "#00D8FF" },
          { label: "Consumo MG (Anual)", value: formatMWh(totalMG), color: "#457ba9" },
          { label: "Consumo ES (Anual)", value: formatMWh(totalES), color: "#00D8FF" },
          { label: "% Médio MG", value: formatPercent(avgPercMG, 2), color: "#457ba9" },
          { label: "% Médio ES", value: formatPercent(avgPercES, 2), color: "#00D8FF" },
        ].map((kpi) => (
          <div key={kpi.label} className="samarco-card" style={{ flex: "1 1 140px", textAlign: "center" }}>
            <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em" }}>{kpi.label}</div>
            <div style={{ color: kpi.color, fontSize: "1.1rem", fontWeight: 800, marginTop: 6 }}>{kpi.value}</div>
          </div>
        ))}
      </div>

      {/* Seção 1: Consumo Mensal */}
      <div className="flex gap-4" style={{ flexWrap: "wrap" }}>
        {/* Bar Chart MG/ES */}
        <div className="samarco-card" style={{ flex: "3 1 400px" }}>
          <div style={{ marginBottom: 16 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>
              Consumo por Estado (MWh/mês)
            </h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Minas Gerais vs Espírito Santo</p>
          </div>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={monthlyData} margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5}/>
              <XAxis dataKey="mes" tick={{ fill: "#8a9bb5", fontSize: 11 }} axisLine={{ stroke: "#1e3a5f" }} tickLine={false}/>
              <YAxis tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false}
                tickFormatter={(v) => `${(v/1000).toFixed(0)}k`}/>
              <Tooltip content={<CustomTooltip />} />
              <Legend wrapperStyle={{ color: "#8a9bb5", fontSize: "0.75rem" }}/>
              <Bar dataKey="mg" name="Minas Gerais" stackId="a" fill={CHART_COLORS.mg} radius={[0, 0, 0, 0]}/>
              <Bar dataKey="es" name="Espírito Santo" stackId="a" fill={CHART_COLORS.es} radius={[3, 3, 0, 0]}/>
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Donut */}
        <div className="samarco-card" style={{ flex: "2 1 220px", display: "flex", flexDirection: "column" }}>
          <div style={{ marginBottom: 12 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Distribuição por Estado</h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Consumo Total Anual</p>
          </div>
          <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center" }}>
            <ResponsiveContainer width="100%" height={180}>
              <PieChart>
                <Pie data={donutData} cx="50%" cy="50%" innerRadius={55} outerRadius={80} paddingAngle={3} dataKey="value">
                  {donutData.map((entry, i) => <Cell key={i} fill={entry.color} stroke="transparent"/>)}
                </Pie>
                <Tooltip formatter={(v: number) => [formatMWh(v), ""]} contentStyle={RECHARTS_TOOLTIP_STYLE}/>
              </PieChart>
            </ResponsiveContainer>
            <div style={{ textAlign: "center", marginTop: -8 }}>
              <div style={{ color: "#8a9bb5", fontSize: "0.7rem" }}>Total</div>
              <div style={{ color: "#e8edf2", fontWeight: 800 }}>{formatNumber(totalGeral)} MWh</div>
            </div>
            <div className="flex gap-4" style={{ marginTop: 10 }}>
              {donutData.map((d) => (
                <div key={d.name} style={{ textAlign: "center" }}>
                  <div style={{ color: d.color, fontSize: "0.7rem", fontWeight: 600 }}>{d.name.split(" ")[0]}</div>
                  <div style={{ color: "#e8edf2", fontWeight: 700 }}>
                    {formatPercent(d.value / totalGeral, 1)}
                  </div>
                  <div style={{ color: "#8a9bb5", fontSize: "0.68rem" }}>{formatNumber(d.value)} MWh</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Seção 2: Variação Sazonal */}
      <div className="samarco-card">
        <div style={{ marginBottom: 16 }}>
          <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>
            Variação Sazonal — % por Estado (Jan–Dez)
          </h3>
          <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>
            Percentual mensal MG e ES ao longo do ano
          </p>
        </div>
        <ResponsiveContainer width="100%" height={220}>
          <LineChart data={monthlyData} margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5}/>
            <XAxis dataKey="mes" tick={{ fill: "#8a9bb5", fontSize: 11 }} axisLine={{ stroke: "#1e3a5f" }} tickLine={false}/>
            <YAxis tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false}
              tickFormatter={(v) => `${v.toFixed(1)}%`} domain={[35, 65]}/>
            <Tooltip
              formatter={(v: number) => [`${v.toFixed(2)}%`, ""]}
              contentStyle={RECHARTS_TOOLTIP_STYLE}
            />
            <Legend wrapperStyle={{ color: "#8a9bb5", fontSize: "0.75rem" }}/>
            <Line type="monotone" dataKey="percMG" name="% MG" stroke={CHART_COLORS.mg} strokeWidth={2.5} dot={{ fill: CHART_COLORS.mg, r: 4 }}/>
            <Line type="monotone" dataKey="percES" name="% ES" stroke={CHART_COLORS.es} strokeWidth={2.5} dot={{ fill: CHART_COLORS.es, r: 4 }}/>
          </LineChart>
        </ResponsiveContainer>
      </div>

      {/* Seção 3: Tabela Percentuais Mensais */}
      <div className="samarco-card">
        <div style={{ marginBottom: 12 }}>
          <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>
            Percentuais Mensais por Estado
          </h3>
        </div>
        <div style={{ overflowX: "auto" }}>
          <table className="samarco-table">
            <thead>
              <tr>
                <th>Estado / Métrica</th>
                {MESES.map((m) => <th key={m}>{m}</th>)}
                <th style={{ color: "#00D8FF" }}>Total/Média</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td style={{ color: "#457ba9" }}>MG (MWh)</td>
                {monthlyData.map((d) => <td key={d.mes}>{formatNumber(d.mg)}</td>)}
                <td style={{ color: "#457ba9", fontWeight: 700 }}>{formatNumber(totalMG)}</td>
              </tr>
              <tr>
                <td style={{ color: "#00D8FF" }}>ES (MWh)</td>
                {monthlyData.map((d) => <td key={d.mes}>{formatNumber(d.es)}</td>)}
                <td style={{ color: "#00D8FF", fontWeight: 700 }}>{formatNumber(totalES)}</td>
              </tr>
              <tr>
                <td style={{ color: "#457ba9" }}>MG (%)</td>
                {monthlyData.map((d) => <td key={d.mes}>{d.percMG.toFixed(4)}%</td>)}
                <td style={{ color: "#457ba9", fontWeight: 700 }}>{formatPercent(avgPercMG, 4)}</td>
              </tr>
              <tr>
                <td style={{ color: "#00D8FF" }}>ES (%)</td>
                {monthlyData.map((d) => <td key={d.mes}>{d.percES.toFixed(4)}%</td>)}
                <td style={{ color: "#00D8FF", fontWeight: 700 }}>{formatPercent(avgPercES, 4)}</td>
              </tr>
              <tr className="total-row">
                <td>Total (MWh)</td>
                {monthlyData.map((d) => <td key={d.mes}>{formatNumber(d.total)}</td>)}
                <td>{formatNumber(totalGeral)}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
