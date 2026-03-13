import { useMotorCalculo } from "../hooks/useMotorCalculo";
import { LoadingSkeleton, ErrorBanner, DataSourceTag } from "../components/ApiComponents";
import { useMemo } from "react";
import {
  BarChart, Bar, AreaChart, Area, LineChart, Line,
  PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from "recharts";

import { MESES, formatCurrency, formatNumber, getByMes, CHART_COLORS, RECHARTS_TOOLTIP_STYLE } from "../lib/utils";
import { usePeriod, filterByPeriod, getFilteredMonths } from "../components/SamarcoLayout";

const CustomTooltip = ({ active, payload, label, currency = false }: any) => {
  if (!active || !payload?.length) return null;
  return (
    <div style={{ ...RECHARTS_TOOLTIP_STYLE, padding: "10px 14px" }}>
      <p style={{ color: "#8a9bb5", marginBottom: 6, fontWeight: 600 }}>{label}</p>
      {payload.map((e: any, i: number) => (
        <div key={i} style={{ color: e.color, fontSize: "0.75rem", marginBottom: 2 }}>
          <span style={{ fontWeight: 600 }}>{e.name}: </span>
          <span>{currency ? formatCurrency(e.value) : formatNumber(e.value, 2)}</span>
        </div>
      ))}
    </div>
  );
};

function BigKpiCard({ title, value, subtitle, icon, color }: { title: string; value: string; subtitle: string; icon: string; color: string }) {
  return (
    <div className="samarco-card" style={{ flex: 1, minWidth: 220, padding: "24px 20px" }}>
      <div style={{ fontSize: "2rem", marginBottom: 12 }}>{icon}</div>
      <div style={{ color: "#8a9bb5", fontSize: "0.72rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: 8 }}>{title}</div>
      <div style={{ color, fontSize: "1.8rem", fontWeight: 900, lineHeight: 1.1, marginBottom: 6 }}>{value}</div>
      <div style={{ color: "#4a5a72", fontSize: "0.75rem" }}>{subtitle}</div>
    </div>
  );
}

export default function ResumoGeral() {
  const { data, loading, hasError, errorMessage, isLiveData, refetch } = useMotorCalculo();
  const { period } = usePeriod();
  const filteredMonths = getFilteredMonths(period);

  const fConsumo = filterByPeriod(data.resumoConsumo, period);
  const fUsoRede = filterByPeriod(data.resumoUsoRede, period);
  const fEncargo = filterByPeriod(data.resumoEncargo, period);
  const fEER = filterByPeriod(data.resumoEER, period);
  const fESS = filterByPeriod(data.resumoESS, period);
  const fTotal = filterByPeriod(data.resumoTotalGeral, period);
  const fProducao = filterByPeriod(data.resumoProducaoTotal, period);
  const fCustoEsp = filterByPeriod(data.resumoCustoEspecifico, period);
  const fFixo = filterByPeriod(data.resumoCustoFixo, period);
  const fVariavel = filterByPeriod(data.resumoCustoVariavel, period);

  const sum = (arr: { mes: string; valor: number }[]) => arr.reduce((a, b) => a + b.valor, 0);
  const avg = (arr: { mes: string; valor: number }[]) => arr.length ? sum(arr) / arr.length : 0;

  const totalGeral = sum(fTotal);
  const totalProducao = sum(fProducao);
  const avgCustoEsp = avg(fCustoEsp);
  const totalConsumo = sum(fConsumo);
  const totalUsoRede = sum(fUsoRede);
  const totalEncargo = sum(fEncargo);
  const totalEER = sum(fEER);
  const totalESS = sum(fESS);
  const totalFixo = sum(fFixo);
  const totalVariavel = sum(fVariavel);

  const periodLabel = period.type === "all" ? "Ano Base 2026" :
    period.type === "quarter" ? `T${period.value} 2026` : `${filteredMonths[0]} 2026`;

  // Charts data
  const stackedData = useMemo(() =>
    filteredMonths.map((mes) => ({
      mes,
      consumo: getByMes(data.resumoConsumo, mes),
      usoRede: getByMes(data.resumoUsoRede, mes),
      encargo: getByMes(data.resumoEncargo, mes),
      eer: getByMes(data.resumoEER, mes),
      ess: getByMes(data.resumoESS, mes),
    })), [filteredMonths]);

  const fixoVariavelData = useMemo(() =>
    filteredMonths.map((mes) => ({
      mes,
      fixo: getByMes(data.resumoCustoFixo, mes),
      variavel: getByMes(data.resumoCustoVariavel, mes),
    })), [filteredMonths]);

  const custoEspData = useMemo(() =>
    filteredMonths.map((mes) => ({
      mes,
      custoEsp: getByMes(data.resumoCustoEspecifico, mes),
    })), [filteredMonths]);

  const dualAxisData = useMemo(() =>
    filteredMonths.map((mes) => ({
      mes,
      total: getByMes(data.resumoTotalGeral, mes),
      producao: getByMes(data.resumoProducaoTotal, mes),
    })), [filteredMonths]);

  const donutFixoVariavel = [
    { name: "Custo Fixo", value: totalFixo, color: "#457ba9" },
    { name: "Custo Variável", value: totalVariavel, color: "#00D8FF" },
  ];

  // Composition table rows
  const composicaoRows = [
    { label: "50610002 — Consumo de Energia", key: "resumoConsumo", total: totalConsumo, color: "#ffa726" },
    { label: "50610003 — Uso de Rede (TUSD FIO)", key: "resumoUsoRede", total: totalUsoRede, color: "#00c853" },
    { label: "50610006 — Encargo TUSD", key: "resumoEncargo", total: totalEncargo, color: "#457ba9" },
    { label: "50610007 — EER/ERCAP", key: "resumoEER", total: totalEER, color: "#ef5350" },
    { label: "50610008 — ESS", key: "resumoESS", total: totalESS, color: "#e91e63" },
  ];

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      {/* Header */}
      <div className="flex items-start justify-between gap-3" style={{ flexWrap: "wrap" }}>
        <div>
          <h1 style={{ color: "#e8edf2", fontSize: "1.4rem", fontWeight: 800, margin: 0 }}>
            Cálculo 16 — Resumo Geral
          </h1>
          <p style={{ color: "#8a9bb5", fontSize: "0.8rem", margin: "4px 0 0" }}>
            Consolidação final de todos os custos · {periodLabel}
          </p>
        </div>
        <div className="flex gap-2" style={{ flexWrap: "wrap" }}>
          <span className="badge-accent">Aba 16</span>
          <span style={{ background: "rgba(0,216,255,0.15)", color: "#00D8FF", border: "1px solid rgba(0,216,255,0.3)", padding: "2px 10px", borderRadius: 20, fontSize: "0.75rem", fontWeight: 700 }}>
            ⭐ Página Principal
          </span>
          <span style={{ background: "rgba(69,123,169,0.15)", color: "#457ba9", border: "1px solid rgba(69,123,169,0.3)", padding: "2px 10px", borderRadius: 20, fontSize: "0.75rem", fontWeight: 600 }}>
            Consolidação final
          </span>
        </div>
      </div>

      {/* Info Card */}
      <div className="samarco-card" style={{ borderLeft: "3px solid #00D8FF" }}>
        <div style={{ fontSize: "0.78rem", color: "#8a9bb5", fontWeight: 600, marginBottom: 10, textTransform: "uppercase", letterSpacing: "0.05em" }}>Fórmulas</div>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: 12 }}>
          {[
            ["Custo Total", "Consumo + Uso de Rede + Encargo + EER + ESS + Despesas Fixas"],
            ["Custo Específico", "Custo Total / Produção Total (R$/tms)"],
            ["Custo Fixo", "Uso de Rede + Despesas Fixas"],
            ["Custo Variável", "Consumo + Encargo + EER + ESS"],
          ].map(([label, formula]) => (
            <div key={label}>
              <span style={{ color: "#00D8FF", fontWeight: 700, fontSize: "0.78rem" }}>{label}</span>
              <span style={{ color: "#8a9bb5", fontSize: "0.78rem" }}> = {formula}</span>
            </div>
          ))}
        </div>
      </div>

      {/* 3 Big KPI Cards */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: 20 }}>
        <BigKpiCard
          title="Custo Total Anual"
          value={formatCurrency(totalGeral)}
          subtitle={`Consolidação ${filteredMonths.length} meses`}
          icon="💰"
          color="#00D8FF"
        />
        <BigKpiCard
          title="Produção Total"
          value={`${formatNumber(totalProducao, 0)} tms`}
          subtitle="Pelotas + Pellet Feed + PSC/PSM"
          icon="📦"
          color="#00c853"
        />
        <BigKpiCard
          title="Custo Específico Médio"
          value={`R$ ${formatNumber(avgCustoEsp, 2)}/tms`}
          subtitle="Custo por tonelada produzida"
          icon="📊"
          color="#ffa726"
        />
      </div>

      {/* Row: Stacked Bar Composição + Donut Fixo/Variável */}
      <div className="flex gap-4" style={{ flexWrap: "wrap" }}>
        <div className="samarco-card" style={{ flex: "2 1 380px" }}>
          <div style={{ marginBottom: 16 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Composição de Custos por Mês (R$)</h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>5 classes de custo empilhadas</p>
          </div>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={stackedData} margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5}/>
              <XAxis dataKey="mes" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={{ stroke: "#1e3a5f" }} tickLine={false}/>
              <YAxis tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false} tickFormatter={(v) => `${(v/1000000).toFixed(0)}M`}/>
              <Tooltip content={(props) => <CustomTooltip {...props} currency />}/>
              <Legend wrapperStyle={{ color: "#8a9bb5", fontSize: "0.72rem" }}/>
              <Bar dataKey="consumo" name="Consumo" stackId="a" fill="#ffa726"/>
              <Bar dataKey="usoRede" name="Uso de Rede" stackId="a" fill="#00c853"/>
              <Bar dataKey="encargo" name="Encargo TUSD" stackId="a" fill="#457ba9"/>
              <Bar dataKey="eer" name="EER/ERCAP" stackId="a" fill="#ef5350"/>
              <Bar dataKey="ess" name="ESS" stackId="a" fill="#e91e63" radius={[3, 3, 0, 0]}/>
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="samarco-card" style={{ flex: "1 1 240px", display: "flex", flexDirection: "column" }}>
          <div style={{ marginBottom: 16 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Fixo vs Variável</h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Estrutura de custos anual</p>
          </div>
          <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center" }}>
            <ResponsiveContainer width="100%" height={180}>
              <PieChart>
                <Pie data={donutFixoVariavel} cx="50%" cy="50%" innerRadius={55} outerRadius={80} paddingAngle={3} dataKey="value">
                  {donutFixoVariavel.map((entry, i) => <Cell key={i} fill={entry.color} stroke="transparent"/>)}
                </Pie>
                <Tooltip formatter={(v: number) => [formatCurrency(v), ""]} contentStyle={RECHARTS_TOOLTIP_STYLE}/>
              </PieChart>
            </ResponsiveContainer>
            <div className="flex gap-4" style={{ marginTop: 8 }}>
              {donutFixoVariavel.map((d) => (
                <div key={d.name} style={{ textAlign: "center" }}>
                  <div style={{ color: d.color, fontSize: "0.7rem", fontWeight: 600 }}>{d.name}</div>
                  <div style={{ color: "#e8edf2", fontSize: "0.85rem", fontWeight: 700 }}>
                    {((d.value / (totalFixo + totalVariavel)) * 100).toFixed(1)}%
                  </div>
                  <div style={{ color: "#4a5a72", fontSize: "0.68rem" }}>{formatCurrency(d.value)}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Row: Custo Total vs Produção (dual axis) + Custo Específico */}
      <div className="flex gap-4" style={{ flexWrap: "wrap" }}>
        <div className="samarco-card" style={{ flex: "1 1 300px" }}>
          <div style={{ marginBottom: 16 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Custo Total vs Produção</h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Eixo duplo — R$ e tms</p>
          </div>
          <ResponsiveContainer width="100%" height={240}>
            <LineChart data={dualAxisData} margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5}/>
              <XAxis dataKey="mes" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={{ stroke: "#1e3a5f" }} tickLine={false}/>
              <YAxis yAxisId="left" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false} tickFormatter={(v) => `${(v/1000000).toFixed(0)}M`}/>
              <YAxis yAxisId="right" orientation="right" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false} tickFormatter={(v) => `${(v/1000).toFixed(0)}k`}/>
              <Tooltip contentStyle={RECHARTS_TOOLTIP_STYLE}/>
              <Legend wrapperStyle={{ color: "#8a9bb5", fontSize: "0.75rem" }}/>
              <Line yAxisId="left" type="monotone" dataKey="total" name="Custo Total (R$)" stroke="#00D8FF" strokeWidth={2.5} dot={{ fill: "#00D8FF", r: 3 }}/>
              <Line yAxisId="right" type="monotone" dataKey="producao" name="Produção (tms)" stroke="#00c853" strokeWidth={2} dot={false} strokeDasharray="5 5"/>
            </LineChart>
          </ResponsiveContainer>
        </div>

        <div className="samarco-card" style={{ flex: "1 1 300px" }}>
          <div style={{ marginBottom: 16 }}>
            <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Evolução do Custo Específico (R$/tms)</h3>
            <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Custo por tonelada produzida</p>
          </div>
          <ResponsiveContainer width="100%" height={240}>
            <AreaChart data={custoEspData} margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
              <defs>
                <linearGradient id="gradCustoEsp" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#ffa726" stopOpacity={0.4}/>
                  <stop offset="95%" stopColor="#ffa726" stopOpacity={0.05}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5}/>
              <XAxis dataKey="mes" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={{ stroke: "#1e3a5f" }} tickLine={false}/>
              <YAxis tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false} tickFormatter={(v) => `R$ ${v.toFixed(0)}`}/>
              <Tooltip formatter={(v: number) => [`R$ ${formatNumber(v, 2)}/tms`, "Custo Específico"]} contentStyle={RECHARTS_TOOLTIP_STYLE}/>
              <Area type="monotone" dataKey="custoEsp" name="Custo Específico" stroke="#ffa726" fill="url(#gradCustoEsp)" strokeWidth={2.5}/>
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Fixo vs Variável Stacked Bar */}
      <div className="samarco-card">
        <div style={{ marginBottom: 16 }}>
          <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Análise Fixo vs Variável por Mês (R$)</h3>
          <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Estrutura de custos mensal</p>
        </div>
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={fixoVariavelData} margin={{ top: 5, right: 10, left: 10, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" strokeOpacity={0.5}/>
            <XAxis dataKey="mes" tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={{ stroke: "#1e3a5f" }} tickLine={false}/>
            <YAxis tick={{ fill: "#8a9bb5", fontSize: 10 }} axisLine={false} tickLine={false} tickFormatter={(v) => `${(v/1000000).toFixed(0)}M`}/>
            <Tooltip content={(props) => <CustomTooltip {...props} currency />}/>
            <Legend wrapperStyle={{ color: "#8a9bb5", fontSize: "0.75rem" }}/>
            <Bar dataKey="fixo" name="Custo Fixo" stackId="a" fill="#457ba9"/>
            <Bar dataKey="variavel" name="Custo Variável" stackId="a" fill="#00D8FF" radius={[3, 3, 0, 0]}/>
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Composição de Custos Table */}
      <div className="samarco-card">
        <div style={{ marginBottom: 16 }}>
          <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Composição de Custos (R$/mês)</h3>
          <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Por classe de custo · {periodLabel}</p>
        </div>
        <div style={{ overflowX: "auto" }}>
          <table className="samarco-table">
            <thead>
              <tr>
                <th>Classe de Custo</th>
                {filteredMonths.map((m) => <th key={m}>{m}</th>)}
                <th>Total Anual</th>
                <th>%</th>
              </tr>
            </thead>
            <tbody>
              {composicaoRows.map(({ label, key, total, color }) => {
                const vals = filteredMonths.map((mes) => getByMes((data as any)[key], mes));
                const pct = totalGeral > 0 ? (total / totalGeral) * 100 : 0;
                return (
                  <tr key={key}>
                    <td style={{ color: "#e8edf2", fontWeight: 600 }}>
                      <span style={{ display: "inline-block", width: 8, height: 8, borderRadius: "50%", background: color, marginRight: 8 }}/>
                      {label}
                    </td>
                    {vals.map((v, i) => <td key={i}>{formatCurrency(v)}</td>)}
                    <td style={{ color, fontWeight: 700 }}>{formatCurrency(total)}</td>
                    <td style={{ color: "#8a9bb5" }}>{pct.toFixed(1)}%</td>
                  </tr>
                );
              })}
              <tr className="total-row">
                <td>TOTAL GERAL</td>
                {filteredMonths.map((mes) => <td key={mes}>{formatCurrency(getByMes(data.resumoTotalGeral, mes))}</td>)}
                <td>{formatCurrency(totalGeral)}</td>
                <td>100%</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Produção e Custo Específico Table */}
      <div className="samarco-card">
        <div style={{ marginBottom: 16 }}>
          <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Produção e Custo Específico</h3>
          <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>Produção total e custo por tonelada · {periodLabel}</p>
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
              <tr>
                <td style={{ color: "#e8edf2", fontWeight: 600 }}>Produção Total (tms)</td>
                {filteredMonths.map((mes) => <td key={mes}>{formatNumber(getByMes(data.resumoProducaoTotal, mes), 0)}</td>)}
                <td style={{ color: "#00c853", fontWeight: 700 }}>{formatNumber(totalProducao, 0)}</td>
              </tr>
              <tr>
                <td style={{ color: "#e8edf2", fontWeight: 600 }}>Custo Total (R$)</td>
                {filteredMonths.map((mes) => <td key={mes}>{formatCurrency(getByMes(data.resumoTotalGeral, mes))}</td>)}
                <td style={{ color: "#00D8FF", fontWeight: 700 }}>{formatCurrency(totalGeral)}</td>
              </tr>
              <tr className="total-row">
                <td>Custo Específico (R$/tms)</td>
                {filteredMonths.map((mes) => <td key={mes}>R$ {formatNumber(getByMes(data.resumoCustoEspecifico, mes), 2)}</td>)}
                <td>R$ {formatNumber(avgCustoEsp, 2)}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Fixo vs Variável Table */}
      <div className="samarco-card">
        <div style={{ marginBottom: 16 }}>
          <h3 style={{ color: "#e8edf2", fontSize: "0.9rem", fontWeight: 700, margin: 0 }}>Análise Fixo vs Variável Detalhada</h3>
          <p style={{ color: "#8a9bb5", fontSize: "0.72rem", margin: "4px 0 0" }}>{periodLabel}</p>
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
                { label: "Custo Fixo (R$)", key: "resumoCustoFixo", total: totalFixo, color: "#457ba9", fmt: formatCurrency },
                { label: "Custo Fixo/tms (R$)", key: null, total: null, color: "#457ba9", fmt: null },
                { label: "Custo Variável (R$)", key: "resumoCustoVariavel", total: totalVariavel, color: "#00D8FF", fmt: formatCurrency },
                { label: "Custo Variável/tms (R$)", key: null, total: null, color: "#00D8FF", fmt: null },
              ].map(({ label, key, total, color, fmt }, rowIdx) => {
                if (!key || !fmt) {
                  // Derived row: custo/tms
                  const isFixo = label.includes("Fixo");
                  const custoArr = isFixo ? data.resumoCustoFixo : data.resumoCustoVariavel;
                  const vals = filteredMonths.map((mes) => {
                    const prod = getByMes(data.resumoProducaoTotal, mes);
                    const custo = getByMes(custoArr, mes);
                    return prod > 0 ? custo / prod : 0;
                  });
                  const avgVal = vals.length ? vals.reduce((a, b) => a + b, 0) / vals.length : 0;
                  return (
                    <tr key={label}>
                      <td style={{ color: "#e8edf2" }}>{label}</td>
                      {vals.map((v, i) => <td key={i}>R$ {formatNumber(v, 2)}</td>)}
                      <td style={{ color, fontWeight: 700 }}>R$ {formatNumber(avgVal, 2)}</td>
                    </tr>
                  );
                }
                const vals = filteredMonths.map((mes) => getByMes((data as any)[key], mes));
                return (
                  <tr key={label}>
                    <td style={{ color: "#e8edf2", fontWeight: 600 }}>{label}</td>
                    {vals.map((v, i) => <td key={i}>{fmt(v)}</td>)}
                    <td style={{ color, fontWeight: 700 }}>{fmt(total!)}</td>
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
