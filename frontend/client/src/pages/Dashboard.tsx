import { useMemo } from "react";
import {
  AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell,
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from "recharts";
import { MESES, formatMWh, formatCurrency, formatPercent, formatNumber, sumArray, getByMes, CHART_COLORS, RECHARTS_TOOLTIP_STYLE } from "../lib/utils";
import { usePeriod, filterByPeriod, getFilteredMonths } from "../components/SamarcoLayout";
import { useMotorCalculo } from "../hooks/useMotorCalculo";
import { LoadingSkeleton, ErrorBanner, DataSourceTag } from "../components/ApiComponents";

function KpiCard({
  title, value, subtitle, icon, color, glowClass
}: {
  title: string; value: string; subtitle?: string;
  icon: string; color: string; glowClass: string;
}) {
  return (
    <div className={`samarco-card ${glowClass}`} style={{ flex: 1, minWidth: 0, transition: "all 0.2s" }}>
      <div className="flex items-start justify-between gap-2">
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ color: "#8a9bb5", fontSize: "0.72rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: 6 }}>
            {title}
          </div>
          <div style={{ color, fontSize: "1.5rem", fontWeight: 800, lineHeight: 1.1, marginBottom: 4 }}>
            {value}
          </div>
          {subtitle && <div style={{ color: "#4a5a72", fontSize: "0.72rem", fontWeight: 500 }}>{subtitle}</div>}
        </div>
        <div style={{
          fontSize: "1.6rem", background: `${color}15`, border: `1px solid ${color}30`,
          borderRadius: 10, padding: "8px 10px", lineHeight: 1,
        }}>{icon}</div>
      </div>
    </div>
  );
}

const CustomTooltip = ({ active, payload, label }: any) => {
  if (!active || !payload?.length) return null;
  return (
    <div style={{ ...RECHARTS_TOOLTIP_STYLE, padding: "10px 14px" }}>
      <p style={{ color: "#8a9bb5", marginBottom: 6, fontWeight: 600 }}>{label}</p>
      {payload.map((entry: any, i: number) => (
        <div key={i} style={{ color: entry.color, fontSize: "0.75rem", marginBottom: 2 }}>
          <span style={{ fontWeight: 600 }}>{entry.name}: </span>
          <span>{typeof entry.value === "number" ? formatNumber(entry.value) : entry.value}</span>
        </div>
      ))}
    </div>
  );
};

export default function Dashboard() {
  const { data, loading, hasError, errorMessage, isLiveData, apiStatus, refetch } = useMotorCalculo();
  const { period } = usePeriod();

  const filteredMonths = getFilteredMonths(period);

  const filteredConsumo = filterByPeriod(data.consumoAreaTotalMWh, period);
  const filteredGeracao = filterByPeriod(data.geracaoTotalMWh, period);
  const filteredSamarco = filterByPeriod(data.consumoSamarcoMWh, period);
  const filteredMgTotal = filterByPeriod(data.mgTotal, period);
  const filteredEsTotal = filterByPeriod(data.esTotal, period);
  const filteredResumoTotal = filterByPeriod(data.resumoTotalGeral, period);
  const filteredCustoEsp = filterByPeriod(data.resumoCustoEspecifico, period);
  const filteredMG = filterByPeriod(data.consumoMG_MWh, period);
  const filteredES = filterByPeriod(data.consumoES_MWh, period);

  const totalConsumo = useMemo(() => filteredConsumo.reduce((a, b) => a + b.valor, 0), [filteredConsumo]);
  const totalGeracao = useMemo(() => filteredGeracao.reduce((a, b) => a + b.valor, 0), [filteredGeracao]);
  const totalConsumoSamarco = useMemo(() => filteredSamarco.reduce((a, b) => a + b.valor, 0), [filteredSamarco]);
  const totalEncargos = useMemo(() =>
    filteredMgTotal.reduce((a, b) => a + b.valor, 0) + filteredEsTotal.reduce((a, b) => a + b.valor, 0),
    [filteredMgTotal, filteredEsTotal]);
  const totalGeral = useMemo(() => filteredResumoTotal.reduce((a, b) => a + b.valor, 0), [filteredResumoTotal]);
  const avgCustoEsp = useMemo(() => {
    if (!filteredCustoEsp.length) return 0;
    return filteredCustoEsp.reduce((a, b) => a + b.valor, 0) / filteredCustoEsp.length;
  }, [filteredCustoEsp]);

  const totalMG = useMemo(() => filteredMG.reduce((a, b) => a + b.valor, 0), [filteredMG]);
  const totalES = useMemo(() => filteredES.reduce((a, b) => a + b.valor, 0), [filteredES]);

  const chartData = useMemo(() =>
    filteredMonths.map((mes) => ({
      mes,
      consumoTotal: getByMes(data.consumoAreaTotalMWh, mes),
      geracao: getByMes(data.geracaoTotalMWh, mes),
      consumoSamarco: getByMes(data.consumoSamarcoMWh, mes),
    })), [filteredMonths, data]);

  const encargosData = useMemo(() =>
    filteredMonths.map((mes) => ({
      mes,
      MG: getByMes(data.mgTotal, mes),
      ES: getByMes(data.esTotal, mes),
    })), [filteredMonths, data]);

  const custoGeralData = useMemo(() =>
    filteredMonths.map((mes) => ({
      mes,
      total: getByMes(data.resumoTotalGeral, mes),
    })), [filteredMonths, data]);

  const donutData = useMemo(() => [
    { name: "Minas Gerais", value: totalMG, color: CHART_COLORS.mg },
    { name: "Espírito Santo", value: totalES, color: CHART_COLORS.es },
  ], [totalMG, totalES]);

  const tableData = useMemo(() =>
    filteredMonths.map((mes) => ({
      mes,
      consumoTotal: getByMes(data.consumoAreaTotalMWh, mes),
      geracao: getByMes(data.geracaoTotalMWh, mes),
      consumoSamarco: getByMes(data.consumoSamarcoMWh, mes),
      percMG: getByMes(data.percentualMG, mes),
      percES: getByMes(data.percentualES, mes),
      eer: getByMes(data.valorTotalEER, mes),
      ess: getByMes(data.valorTotalESS, mes),
      totalEncargos: getByMes(data.mgTotal, mes) + getByMes(data.esTotal, mes),
      totalGeral: getByMes(data.resumoTotalGeral, mes),
      custoEsp: getByMes(data.resumoCustoEspecifico, mes),
    })), [filteredMonths, data]);

  const periodLabel = period.type === "all" ? "Ano Base 2026" :
    period.type === "quarter" ? `T${period.value} 2026` :
    `${MESES[period.value!]} 2026`;

  if (loading) return <LoadingSkeleton />;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      {/* Page Header */}
      <div className="flex items-center gap-3">
        <div>
          <h1 style={{ color: "#e8edf2", fontSize: "1.4rem", fontWeight: 800, margin: 0 }}>
            Dashboard — Visão Consolidada
          </h1>
          <p style={{ color: "#8a9bb5", fontSize: "0.8rem", margin: "4px 0 0" }}>
            10 cálculos encadeados · {periodLabel}
          </p>
        </div>
        <DataSourceTag isLive={isLiveData} />
      </div>

      {/* Error / Fallback Banner */}
      {hasError && (
        <ErrorBanner
          message={errorMessage}
          onRetry={refetch}
          isFallback={!isLiveData}
        />
      )}

      {/* Row 1: 6 KPI Cards */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 16 }}>
        <KpiCard title="Consumo Total" value={formatMWh(totalConsumo)} subtitle="Consumo Área Total" icon="⚡" color="#00D8FF" glowClass="kpi-glow-accent"/>
        <KpiCard title="Geração Própria" value={formatMWh(totalGeracao)} subtitle="Guilman + Muniz Freire" icon="🔋" color="#00c853" glowClass="kpi-glow-success"/>
        <KpiCard title="Consumo Líquido" value={formatMWh(totalConsumoSamarco)} subtitle="Consumo − Geração" icon="📊" color="#ffa726" glowClass="kpi-glow-warning"/>
        <KpiCard title="Custo Total Encargos" value={formatCurrency(totalEncargos)} subtitle="EER + ESS (MG + ES)" icon="💰" color="#ef5350" glowClass="kpi-glow-danger"/>
        <KpiCard title="Custo Total Geral" value={formatCurrency(totalGeral)} subtitle="Consumo + Uso Rede + Encargos" icon="📋" color="#457ba9" glowClass=""/>
        <KpiCard title="Custo Específico Médio" value={`R$ ${formatNumber(avgCustoEsp, 2)}/tms`} subtitle="Custo por tonelada produzida" icon="🏷️" color="#8a9bb5" glowClass=""/>
      </div>

      {/* Row 2: Area Chart + Donut */}
      <div className="flex gap-4" style={{ flexWrap: "wrap" }}>
        <div className="samarco-card" style={{ flex: "3 1 400px" }}>
          <div style={{ marginBottom: 16 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Consumo vs Geração (MWh/mês)</h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Área empilhada · {periodLabel}</p>
          </div>
          <ResponsiveContainer width="100%" height={260}>
            <AreaChart data={chartData} margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
              <defs>
                <linearGradient id="gradConsumo" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#457ba9" stopOpacity={0.4}/><stop offset="95%" stopColor="#457ba9" stopOpacity={0.05}/>
                </linearGradient>
                <linearGradient id="gradGeracao" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#00c853" stopOpacity={0.4}/><stop offset="95%" stopColor="#00c853" stopOpacity={0.05}/>
                </linearGradient>
                <linearGradient id="gradSamarco" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#ffa726" stopOpacity={0.3}/><stop offset="95%" stopColor="#ffa726" stopOpacity={0.05}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5}/>
              <XAxis dataKey="mes" tick={{ fill: "#8a9bb5", fontSize: 11 }} axisLine={{ stroke: "#1e3a5f" }} tickLine={false}/>
              <YAxis tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false} tickFormatter={(v) => `${(v/1000).toFixed(0)}k`}/>
              <Tooltip content={<CustomTooltip />}/>
              <Legend wrapperStyle={{ color: "#8a9bb5", fontSize: "0.75rem" }}/>
              <Area type="monotone" dataKey="consumoTotal" name="Consumo Total" stroke="#457ba9" fill="url(#gradConsumo)" strokeWidth={2}/>
              <Area type="monotone" dataKey="geracao" name="Geração Própria" stroke="#00c853" fill="url(#gradGeracao)" strokeWidth={2}/>
              <Area type="monotone" dataKey="consumoSamarco" name="Consumo Samarco" stroke="#ffa726" fill="url(#gradSamarco)" strokeWidth={2}/>
            </AreaChart>
          </ResponsiveContainer>
        </div>

        <div className="samarco-card" style={{ flex: "2 1 260px", display: "flex", flexDirection: "column" }}>
          <div style={{ marginBottom: 16 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Distribuição por Estado</h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Consumo Área Total (MWh)</p>
          </div>
          <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center" }}>
            <ResponsiveContainer width="100%" height={200}>
              <PieChart>
                <Pie data={donutData} cx="50%" cy="50%" innerRadius={60} outerRadius={90} paddingAngle={3} dataKey="value">
                  {donutData.map((entry, index) => <Cell key={index} fill={entry.color} stroke="transparent"/>)}
                </Pie>
                <Tooltip formatter={(value: number) => [formatMWh(value), ""]} contentStyle={RECHARTS_TOOLTIP_STYLE}/>
              </PieChart>
            </ResponsiveContainer>
            <div style={{ textAlign: "center", marginTop: -10 }}>
              <div style={{ color: "#8a9bb5", fontSize: "0.7rem" }}>Total</div>
              <div style={{ color: "#e8edf2", fontWeight: 800, fontSize: "1rem" }}>{formatNumber(totalMG + totalES)} MWh</div>
            </div>
            <div className="flex gap-4" style={{ marginTop: 12 }}>
              {donutData.map((d) => (
                <div key={d.name} style={{ textAlign: "center" }}>
                  <div style={{ color: d.color, fontSize: "0.7rem", fontWeight: 600 }}>{d.name}</div>
                  <div style={{ color: "#e8edf2", fontSize: "0.85rem", fontWeight: 700 }}>
                    {formatPercent(d.value / (totalMG + totalES), 1)}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Row 3: Encargos Bar + Custo Geral Line */}
      <div className="flex gap-4" style={{ flexWrap: "wrap" }}>
        <div className="samarco-card" style={{ flex: "1 1 300px" }}>
          <div style={{ marginBottom: 16 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Encargos por Estado (R$/mês)</h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>MG vs ES</p>
          </div>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={encargosData} margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5}/>
              <XAxis dataKey="mes" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={{ stroke: "#1e3a5f" }} tickLine={false}/>
              <YAxis tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false} tickFormatter={(v) => `${(v/1000000).toFixed(1)}M`}/>
              <Tooltip content={<CustomTooltip />}/>
              <Legend wrapperStyle={{ color: "#8a9bb5", fontSize: "0.75rem" }}/>
              <Bar dataKey="MG" name="Minas Gerais" fill={CHART_COLORS.mg} radius={[3, 3, 0, 0]}/>
              <Bar dataKey="ES" name="Espírito Santo" fill={CHART_COLORS.es} radius={[3, 3, 0, 0]}/>
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="samarco-card" style={{ flex: "1 1 300px" }}>
          <div style={{ marginBottom: 16 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Custo Total Mensal (R$)</h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Resumo Geral — Cálculo 16</p>
          </div>
          <ResponsiveContainer width="100%" height={240}>
            <AreaChart data={custoGeralData} margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
              <defs>
                <linearGradient id="gradGeral" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#457ba9" stopOpacity={0.4}/><stop offset="95%" stopColor="#457ba9" stopOpacity={0.05}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5}/>
              <XAxis dataKey="mes" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={{ stroke: "#1e3a5f" }} tickLine={false}/>
              <YAxis tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false} tickFormatter={(v) => `${(v/1000000).toFixed(0)}M`}/>
              <Tooltip formatter={(v: number) => [formatCurrency(v), "Custo Total"]} contentStyle={RECHARTS_TOOLTIP_STYLE}/>
              <Area type="monotone" dataKey="total" name="Custo Total" stroke="#457ba9" fill="url(#gradGeral)" strokeWidth={2.5}/>
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Row 4: Summary Table */}
      <div className="samarco-card">
        <div style={{ marginBottom: 16 }}>
          <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Visão Anual por Mês</h3>
          <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Todos os indicadores consolidados · {periodLabel}</p>
        </div>
        <div style={{ overflowX: "auto" }}>
          <table className="samarco-table">
            <thead>
              <tr>
                <th>Métrica</th>
                {filteredMonths.map((m) => <th key={m}>{m}</th>)}
                <th>Total / Média</th>
              </tr>
            </thead>
            <tbody>
              {[
                { label: "Consumo Total (MWh)", key: "consumoTotal", fmt: (v: number) => formatNumber(v, 2) },
                { label: "Geração Própria (MWh)", key: "geracao", fmt: (v: number) => formatNumber(v, 2) },
                { label: "Consumo Samarco (MWh)", key: "consumoSamarco", fmt: (v: number) => formatNumber(v, 2) },
                { label: "% MG", key: "percMG", fmt: (v: number) => formatPercent(v, 4) },
                { label: "% ES", key: "percES", fmt: (v: number) => formatPercent(v, 4) },
                { label: "Encargos EER (R$)", key: "eer", fmt: formatCurrency },
                { label: "Encargos ESS (R$)", key: "ess", fmt: formatCurrency },
                { label: "Total Encargos (R$)", key: "totalEncargos", fmt: formatCurrency },
                { label: "Custo Total Geral (R$)", key: "totalGeral", fmt: formatCurrency },
                { label: "Custo Específico (R$/tms)", key: "custoEsp", fmt: (v: number) => `R$ ${formatNumber(v, 2)}` },
              ].map(({ label, key, fmt }, rowIdx) => {
                const vals = tableData.map((r) => r[key as keyof typeof r] as number);
                const isPercent = key.startsWith("perc");
                const total = isPercent
                  ? vals.reduce((a, b) => a + b, 0) / vals.length
                  : vals.reduce((a, b) => a + b, 0);
                return (
                  <tr key={key} className={rowIdx === tableData.length - 1 ? "total-row" : ""}>
                    <td style={{ fontWeight: 600, color: "#e8edf2" }}>{label}</td>
                    {vals.map((v, i) => <td key={i}>{fmt(v)}</td>)}
                    <td style={{ color: "#00D8FF", fontWeight: 700 }}>{fmt(total)}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
