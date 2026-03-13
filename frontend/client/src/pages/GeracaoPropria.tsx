import { useMotorCalculo } from "../hooks/useMotorCalculo";
import { LoadingSkeleton, ErrorBanner, DataSourceTag } from "../components/ApiComponents";
import { useMemo } from "react";
import {
  AreaChart, Area, BarChart, Bar,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from "recharts";

import { MESES, formatMWh, formatNumber, sumArray, getByMes, CHART_COLORS, RECHARTS_TOOLTIP_STYLE } from "../lib/utils";

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

export default function GeracaoPropria() {
  const { data, loading, hasError, errorMessage, isLiveData, refetch } = useMotorCalculo();

  const totalGuilman = useMemo(() => sumArray(data.geracaoGuilmanMWh), []);
  const totalMuniz = useMemo(() => sumArray(data.geracaoMunizFreireMWh), []);
  const totalGeral = useMemo(() => sumArray(data.geracaoTotalMWh), []);

  const chartData = useMemo(() =>
    MESES.map((mes) => ({
      mes,
      guilman: getByMes(data.geracaoGuilmanMWh, mes),
      muniz: getByMes(data.geracaoMunizFreireMWh, mes),
      total: getByMes(data.geracaoTotalMWh, mes),
    })), []);

  const calendarioData = useMemo(() =>
    data.calendario.map((c) => ({
      ...c,
      horasForaPonta: c.horasForaPonta,
      horasPonta: c.horasPonta,
    })), []);

  const tableData = useMemo(() =>
    MESES.map((mes) => {
      const guilman = getByMes(data.geracaoGuilmanMWh, mes);
      const muniz = getByMes(data.geracaoMunizFreireMWh, mes);
      const total = getByMes(data.geracaoTotalMWh, mes);
      const cal = data.calendario.find((c) => c.mes === mes);
      const horas = cal?.totalHoras ?? 744;
      return {
        mes,
        guilman,
        guilmanMW: guilman / horas,
        muniz,
        munizMW: muniz / horas,
        total,
        totalMW: total / horas,
      };
    }), []);


  if (loading) return <LoadingSkeleton />;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      {/* Header */}
      <div className="flex items-center gap-3" style={{ flexWrap: "wrap" }}>
        <div>
          <h1 style={{ color: "#e8edf2", fontSize: "1.4rem", fontWeight: 800, margin: 0 }}>
            Cálculo 04 — Geração Própria
          </h1>
          <p style={{ color: "#8a9bb5", fontSize: "0.8rem", margin: "4px 0 0" }}>
            UHE Guilman Amorim + PCH Muniz Freire · Ano Base 2026
          </p>
        </div>
        <span className="badge-accent" style={{ marginLeft: "auto" }}>Aba 9 do Excel</span>
      </div>

      {/* Plant Cards */}
      <div className="flex gap-4" style={{ flexWrap: "wrap" }}>
        {/* Guilman Amorim */}
        <div className="samarco-card kpi-glow-accent" style={{ flex: "1 1 250px" }}>
          <div className="flex items-center gap-3" style={{ marginBottom: 12 }}>
            <div style={{
              fontSize: "2rem",
              background: "rgba(69, 123, 169, 0.15)",
              border: "1px solid rgba(69, 123, 169, 0.3)",
              borderRadius: 10,
              padding: "8px 10px",
              lineHeight: 1,
            }}>💧</div>
            <div>
              <div style={{ color: "#e8edf2", fontWeight: 700, fontSize: "0.95rem" }}>UHE Guilman Amorim</div>
              <div style={{ color: "#8a9bb5", fontSize: "0.72rem" }}>Usina Hidrelétrica</div>
            </div>
          </div>
          <div style={{ color: "#457ba9", fontSize: "1.6rem", fontWeight: 800 }}>
            {formatMWh(totalGuilman)}
          </div>
          <div style={{ color: "#8a9bb5", fontSize: "0.72rem", marginTop: 4 }}>Total Anual 2026</div>
          <div style={{ marginTop: 12, height: 40 }}>
            <ResponsiveContainer width="100%" height={40}>
              <AreaChart data={chartData} margin={{ top: 0, right: 0, left: 0, bottom: 0 }}>
                <Area type="monotone" dataKey="guilman" stroke="#457ba9" fill="rgba(69,123,169,0.2)" strokeWidth={1.5}/>
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Muniz Freire */}
        <div className="samarco-card kpi-glow-success" style={{ flex: "1 1 250px" }}>
          <div className="flex items-center gap-3" style={{ marginBottom: 12 }}>
            <div style={{
              fontSize: "2rem",
              background: "rgba(0, 200, 83, 0.15)",
              border: "1px solid rgba(0, 200, 83, 0.3)",
              borderRadius: 10,
              padding: "8px 10px",
              lineHeight: 1,
            }}>⚡</div>
            <div>
              <div style={{ color: "#e8edf2", fontWeight: 700, fontSize: "0.95rem" }}>PCH Muniz Freire</div>
              <div style={{ color: "#8a9bb5", fontSize: "0.72rem" }}>Pequena Central Hidrelétrica</div>
            </div>
          </div>
          <div style={{ color: "#00c853", fontSize: "1.6rem", fontWeight: 800 }}>
            {formatMWh(totalMuniz)}
          </div>
          <div style={{ color: "#8a9bb5", fontSize: "0.72rem", marginTop: 4 }}>Total Anual 2026</div>
          <div style={{ marginTop: 12, height: 40 }}>
            <ResponsiveContainer width="100%" height={40}>
              <AreaChart data={chartData} margin={{ top: 0, right: 0, left: 0, bottom: 0 }}>
                <Area type="monotone" dataKey="muniz" stroke="#00c853" fill="rgba(0,200,83,0.2)" strokeWidth={1.5}/>
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Total */}
        <div className="samarco-card" style={{ flex: "1 1 200px", background: "rgba(0, 64, 113, 0.2)", borderColor: "#1e3a5f" }}>
          <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em" }}>Total Geração 2026</div>
          <div style={{ color: "#00D8FF", fontSize: "1.6rem", fontWeight: 800, marginTop: 8 }}>{formatMWh(totalGeral)}</div>
          <div style={{ marginTop: 12 }}>
            <div className="flex justify-between" style={{ marginBottom: 6 }}>
              <span style={{ color: "#457ba9", fontSize: "0.75rem" }}>Guilman</span>
              <span style={{ color: "#457ba9", fontSize: "0.75rem", fontWeight: 600 }}>
                {((totalGuilman / totalGeral) * 100).toFixed(1)}%
              </span>
            </div>
            <div style={{ background: "#1e3a5f", borderRadius: 4, height: 6, overflow: "hidden" }}>
              <div style={{ background: "#457ba9", height: "100%", width: `${(totalGuilman / totalGeral) * 100}%`, borderRadius: 4 }}/>
            </div>
            <div className="flex justify-between" style={{ marginTop: 8, marginBottom: 6 }}>
              <span style={{ color: "#00c853", fontSize: "0.75rem" }}>Muniz Freire</span>
              <span style={{ color: "#00c853", fontSize: "0.75rem", fontWeight: 600 }}>
                {((totalMuniz / totalGeral) * 100).toFixed(1)}%
              </span>
            </div>
            <div style={{ background: "#1e3a5f", borderRadius: 4, height: 6, overflow: "hidden" }}>
              <div style={{ background: "#00c853", height: "100%", width: `${(totalMuniz / totalGeral) * 100}%`, borderRadius: 4 }}/>
            </div>
          </div>
        </div>
      </div>

      {/* Stacked Area Chart */}
      <div className="samarco-card">
        <div style={{ marginBottom: 16 }}>
          <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>
            Geração Mensal por Usina (MWh)
          </h3>
          <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Guilman Amorim + Muniz Freire = Total</p>
        </div>
        <ResponsiveContainer width="100%" height={260}>
          <AreaChart data={chartData} margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
            <defs>
              <linearGradient id="gradGuilman" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#457ba9" stopOpacity={0.5}/>
                <stop offset="95%" stopColor="#457ba9" stopOpacity={0.05}/>
              </linearGradient>
              <linearGradient id="gradMuniz" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#00c853" stopOpacity={0.5}/>
                <stop offset="95%" stopColor="#00c853" stopOpacity={0.05}/>
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5}/>
            <XAxis dataKey="mes" tick={{ fill: "#8a9bb5", fontSize: 11 }} axisLine={{ stroke: "#1e3a5f" }} tickLine={false}/>
            <YAxis tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false}
              tickFormatter={(v) => `${(v/1000).toFixed(0)}k`}/>
            <Tooltip content={<CustomTooltip />} />
            <Legend wrapperStyle={{ color: "#8a9bb5", fontSize: "0.75rem" }}/>
            <Area type="monotone" dataKey="guilman" name="Guilman Amorim" stroke="#457ba9" fill="url(#gradGuilman)" strokeWidth={2}/>
            <Area type="monotone" dataKey="muniz" name="Muniz Freire" stroke="#00c853" fill="url(#gradMuniz)" strokeWidth={2}/>
          </AreaChart>
        </ResponsiveContainer>
      </div>

      {/* Calendário */}
      <div className="flex gap-4" style={{ flexWrap: "wrap" }}>
        <div className="samarco-card" style={{ flex: "3 1 400px" }}>
          <div style={{ marginBottom: 12 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>
              Calendário 2026
            </h3>
            <div className="flex gap-4" style={{ marginTop: 6 }}>
              <span style={{ color: "#8a9bb5", fontSize: "0.72rem" }}>
                Horas Ponta/Dia: <strong style={{ color: "#ffa726" }}>3h</strong>
              </span>
              <span style={{ color: "#8a9bb5", fontSize: "0.72rem" }}>
                Horas/Dia: <strong style={{ color: "#00D8FF" }}>24h</strong>
              </span>
            </div>
          </div>
          <div style={{ overflowX: "auto" }}>
            <table className="samarco-table">
              <thead>
                <tr>
                  <th>Mês</th>
                  <th>Dias</th>
                  <th>Dias Úteis</th>
                  <th>H. Ponta</th>
                  <th>H. Fora Ponta</th>
                  <th>Total Horas</th>
                </tr>
              </thead>
              <tbody>
                {calendarioData.map((c) => (
                  <tr key={c.mes}>
                    <td style={{ fontWeight: 600 }}>{c.mes}</td>
                    <td>{c.diasNoMes}</td>
                    <td>{c.diasUteis}</td>
                    <td style={{ color: "#ffa726" }}>{c.horasPonta.toFixed(0)}h</td>
                    <td style={{ color: "#8a9bb5" }}>{c.horasForaPonta.toFixed(0)}h</td>
                    <td style={{ color: "#00D8FF", fontWeight: 600 }}>{c.totalHoras.toFixed(0)}h</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Bar chart horas */}
        <div className="samarco-card" style={{ flex: "2 1 260px" }}>
          <div style={{ marginBottom: 12 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Horas Ponta vs Fora Ponta</h3>
          </div>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={calendarioData} margin={{ top: 5, right: 10, left: 0, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5}/>
              <XAxis dataKey="mes" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={{ stroke: "#1e3a5f" }} tickLine={false}/>
              <YAxis tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false}/>
              <Tooltip contentStyle={RECHARTS_TOOLTIP_STYLE}/>
              <Legend wrapperStyle={{ color: "#8a9bb5", fontSize: "0.75rem" }}/>
              <Bar dataKey="horasPonta" name="H. Ponta" stackId="a" fill="#ffa726" radius={[0, 0, 0, 0]}/>
              <Bar dataKey="horasForaPonta" name="H. Fora Ponta" stackId="a" fill="#1e3a5f" radius={[3, 3, 0, 0]}/>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Tabela Geração Mensal */}
      <div className="samarco-card">
        <div style={{ marginBottom: 12 }}>
          <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>
            Geração Mensal Detalhada
          </h3>
        </div>
        <div style={{ overflowX: "auto" }}>
          <table className="samarco-table">
            <thead>
              <tr>
                <th>Usina</th>
                <th>Métrica</th>
                {MESES.map((m) => <th key={m}>{m}</th>)}
                <th style={{ color: "#00D8FF" }}>Total</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td style={{ color: "#457ba9", fontWeight: 600 }}>Guilman Amorim</td>
                <td style={{ color: "#8a9bb5" }}>MWh</td>
                {tableData.map((d) => <td key={d.mes}>{formatNumber(d.guilman)}</td>)}
                <td style={{ color: "#457ba9", fontWeight: 700 }}>{formatNumber(totalGuilman)}</td>
              </tr>
              <tr>
                <td style={{ color: "#457ba9" }}>Guilman Amorim</td>
                <td style={{ color: "#8a9bb5" }}>MWmédios</td>
                {tableData.map((d) => <td key={d.mes}>{d.guilmanMW.toFixed(2)}</td>)}
                <td style={{ color: "#457ba9", fontWeight: 700 }}>{(totalGuilman / 8760).toFixed(2)}</td>
              </tr>
              <tr>
                <td style={{ color: "#00c853", fontWeight: 600 }}>Muniz Freire</td>
                <td style={{ color: "#8a9bb5" }}>MWh</td>
                {tableData.map((d) => <td key={d.mes}>{formatNumber(d.muniz)}</td>)}
                <td style={{ color: "#00c853", fontWeight: 700 }}>{formatNumber(totalMuniz)}</td>
              </tr>
              <tr>
                <td style={{ color: "#00c853" }}>Muniz Freire</td>
                <td style={{ color: "#8a9bb5" }}>MWmédios</td>
                {tableData.map((d) => <td key={d.mes}>{d.munizMW.toFixed(2)}</td>)}
                <td style={{ color: "#00c853", fontWeight: 700 }}>{(totalMuniz / 8760).toFixed(2)}</td>
              </tr>
              <tr className="total-row">
                <td>Total</td>
                <td>MWh</td>
                {tableData.map((d) => <td key={d.mes}>{formatNumber(d.total)}</td>)}
                <td>{formatNumber(totalGeral)}</td>
              </tr>
              <tr className="total-row">
                <td>Total</td>
                <td>MWmédios</td>
                {tableData.map((d) => <td key={d.mes}>{d.totalMW.toFixed(2)}</td>)}
                <td>{(totalGeral / 8760).toFixed(2)}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
