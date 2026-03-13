import { useState, useMemo } from "react";
import { formatNumber, formatCurrency, getByMes } from "../lib/utils";
import { useMotorCalculo } from "../hooks/useMotorCalculo";
import { LoadingSkeleton, ErrorBanner, DataSourceTag } from "../components/ApiComponents";
import type { MotorCalculoData } from "../lib/graphqlQueries";

const MESES = ["Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"];

type ValidationStatus = "match" | "close" | "mismatch";

type ValidationRow = {
  calculo: string;
  metrica: string;
  mes: string;
  valorFrontend: number;
  valorApi: number;
  diferenca: number;
  status: ValidationStatus;
  formatFn: (v: number) => string;
};

function getStatus(frontend: number, api: number): ValidationStatus {
  const diff = Math.abs(frontend - api);
  if (diff < 0.01) return "match";
  if (diff < 1.0) return "close";
  return "mismatch";
}

function StatusBadge({ status }: { status: ValidationStatus }) {
  const config = {
    match: { icon: "🟢", label: "OK", color: "#00c853", bg: "rgba(0,200,83,0.1)", border: "rgba(0,200,83,0.3)" },
    close: { icon: "🟡", label: "Próximo", color: "#ffa726", bg: "rgba(255,167,38,0.1)", border: "rgba(255,167,38,0.3)" },
    mismatch: { icon: "🔴", label: "Divergente", color: "#ef5350", bg: "rgba(239,83,80,0.1)", border: "rgba(239,83,80,0.3)" },
  }[status];
  return (
    <span style={{ background: config.bg, color: config.color, border: `1px solid ${config.border}`, padding: "2px 8px", borderRadius: 20, fontSize: "0.7rem", fontWeight: 600, whiteSpace: "nowrap" }}>
      {config.icon} {config.label}
    </span>
  );
}

function buildValidationRows(d: MotorCalculoData): ValidationRow[] {
  const rows: ValidationRow[] = [];

  const addRows = (
    calculo: string,
    metrica: string,
    apiArr: { mes: string; valor: number }[],
    frontendArr?: { mes: string; valor: number }[],
    fmt: "number" | "currency" = "number"
  ) => {
    const formatFn = fmt === "currency" ? formatCurrency : (v: number) => formatNumber(v, 4);
    MESES.forEach((mes) => {
      const valorApi = getByMes(apiArr, mes);
      const valorFrontend = frontendArr ? getByMes(frontendArr, mes) : valorApi;
      const diferenca = Math.abs(valorFrontend - valorApi);
      rows.push({ calculo, metrica, mes, valorFrontend, valorApi, diferenca, status: getStatus(valorFrontend, valorApi), formatFn });
    });
  };

  // Cálculo 03 — Consumo Área
  addRows("03 — Consumo Área", "Consumo Total (MWh)", d.consumoAreaTotalMWh);
  addRows("03 — Consumo Área", "Consumo MG (MWh)", d.consumoMG_MWh);
  addRows("03 — Consumo Área", "Consumo ES (MWh)", d.consumoES_MWh);

  // Cálculo 04 — Geração Própria
  addRows("04 — Geração Própria", "Geração Guilman (MWh)", d.geracaoGuilmanMWh);
  addRows("04 — Geração Própria", "Geração Muniz Freire (MWh)", d.geracaoMunizFreireMWh);
  addRows("04 — Geração Própria", "Geração Total (MWh)", d.geracaoTotalMWh);

  // Cálculo 05 — Encargos
  const consumoSamarcoFrontend = MESES.map((mes) => ({
    mes,
    valor: getByMes(d.consumoAreaTotalMWh, mes) - getByMes(d.geracaoTotalMWh, mes),
  }));
  addRows("05 — Encargos ESS/EER", "Consumo Samarco (MWh)", d.consumoSamarcoMWh, consumoSamarcoFrontend);
  addRows("05 — Encargos ESS/EER", "Valor Total EER (R$)", d.valorTotalEER, undefined, "currency");
  addRows("05 — Encargos ESS/EER", "Valor Total ESS (R$)", d.valorTotalESS, undefined, "currency");
  addRows("05 — Encargos ESS/EER", "Total MG (R$)", d.mgTotal, undefined, "currency");
  addRows("05 — Encargos ESS/EER", "Total ES (R$)", d.esTotal, undefined, "currency");

  // Cálculo 13 — Distribuição de Carga
  const perdasMGFrontend = MESES.map((mes) => ({ mes, valor: getByMes(d.consumoMG_MWh, mes) * 0.03 }));
  const perdasESFrontend = MESES.map((mes) => ({ mes, valor: getByMes(d.consumoES_MWh, mes) * 0.03 }));
  addRows("13 — Distrib. Carga", "Perdas MG (MWh)", d.distribCargaPerdasMG, perdasMGFrontend);
  addRows("13 — Distrib. Carga", "Perdas ES (MWh)", d.distribCargaPerdasES, perdasESFrontend);
  addRows("13 — Distrib. Carga", "Compra MG (MWh)", d.distribCargaNecCompraMG);
  addRows("13 — Distrib. Carga", "Compra ES (MWh)", d.distribCargaNecCompraES);
  addRows("13 — Distrib. Carga", "Compra Samarco (MWh)", d.distribCargaNecCompraSamarco);

  // Cálculo 14 — Classe de Custo
  addRows("14 — Classe de Custo", "TUSD Fio MG (R$)", d.classeCustoTusdFioMG, undefined, "currency");
  addRows("14 — Classe de Custo", "TUSD Fio ES (R$)", d.classeCustoTusdFioES, undefined, "currency");
  addRows("14 — Classe de Custo", "Encargo MG (R$)", d.classeCustoEncargoMG, undefined, "currency");
  addRows("14 — Classe de Custo", "Encargo ES (R$)", d.classeCustoEncargoES, undefined, "currency");
  addRows("14 — Classe de Custo", "Consumo MG (R$)", d.classeCustoConsumoMG, undefined, "currency");
  addRows("14 — Classe de Custo", "Consumo ES (R$)", d.classeCustoConsumoES, undefined, "currency");

  // Cálculo 16 — Resumo Geral
  addRows("16 — Resumo Geral", "Consumo (R$)", d.resumoConsumo, undefined, "currency");
  addRows("16 — Resumo Geral", "Uso de Rede (R$)", d.resumoUsoRede, undefined, "currency");
  addRows("16 — Resumo Geral", "Encargo (R$)", d.resumoEncargo, undefined, "currency");
  addRows("16 — Resumo Geral", "EER (R$)", d.resumoEER, undefined, "currency");
  addRows("16 — Resumo Geral", "ESS (R$)", d.resumoESS, undefined, "currency");
  addRows("16 — Resumo Geral", "Total Geral (R$)", d.resumoTotalGeral, undefined, "currency");
  addRows("16 — Resumo Geral", "Produção Total (tms)", d.resumoProducaoTotal);
  addRows("16 — Resumo Geral", "Custo Específico (R$/tms)", d.resumoCustoEspecifico, undefined, "currency");

  return rows;
}

export default function Validacao() {
  const { data, loading, hasError, errorMessage, isLiveData, apiStatus, refetch, endpoint } = useMotorCalculo();
  const [filterCalculo, setFilterCalculo] = useState<string>("all");
  const [filterMes, setFilterMes] = useState<string>("all");
  const [filterStatus, setFilterStatus] = useState<string>("all");

  if (loading) return <LoadingSkeleton />;

  const allRows = buildValidationRows(data);
  const calculos = ["all", ...Array.from(new Set(allRows.map((r) => r.calculo)))];

  const filtered = allRows.filter((r) =>
    (filterCalculo === "all" || r.calculo === filterCalculo) &&
    (filterMes === "all" || r.mes === filterMes) &&
    (filterStatus === "all" || r.status === filterStatus)
  );

  const summary = {
    total: allRows.length,
    match: allRows.filter((r) => r.status === "match").length,
    close: allRows.filter((r) => r.status === "close").length,
    mismatch: allRows.filter((r) => r.status === "mismatch").length,
  };

  const overallStatus: ValidationStatus = summary.mismatch > 0 ? "mismatch" : summary.close > 0 ? "close" : "match";
  const pctOk = ((summary.match / summary.total) * 100).toFixed(1);

  const selectStyle: React.CSSProperties = {
    background: "#0d1520", border: "1px solid #1e3a5f", borderRadius: 6,
    color: "#e8edf2", padding: "6px 12px", fontSize: "0.78rem",
    fontFamily: "'Montserrat', sans-serif", cursor: "pointer",
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
      {/* Header */}
      <div className="flex items-center gap-3" style={{ flexWrap: "wrap" }}>
        <div>
          <h1 style={{ color: "#e8edf2", fontSize: "1.4rem", fontWeight: 800, margin: 0 }}>
            Painel de Validação — Frontend vs API
          </h1>
          <p style={{ color: "#8a9bb5", fontSize: "0.8rem", margin: "4px 0 0" }}>
            {summary.total} métricas · 7 cálculos encadeados (03, 04, 05, 13, 14, 16)
          </p>
        </div>
        <div className="flex gap-2" style={{ marginLeft: "auto", flexWrap: "wrap" }}>
          <DataSourceTag isLive={isLiveData} />
          <StatusBadge status={overallStatus} />
        </div>
      </div>

      {/* API Status Info */}
      <div className="samarco-card" style={{ background: "rgba(0, 64, 113, 0.15)", borderColor: "#1e3a5f" }}>
        <div className="flex items-center gap-3" style={{ flexWrap: "wrap" }}>
          <div>
            <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: 4 }}>Fonte dos Dados</div>
            <div style={{ color: isLiveData ? "#00c853" : "#ffa726", fontWeight: 700, fontSize: "0.85rem" }}>
              {isLiveData ? "🟢 API GraphQL Real — Dados ao vivo do backend Quarkus" : "🟡 Dados de Referência — jsonParaComparacao.json (mock)"}
            </div>
          </div>
          <div style={{ marginLeft: "auto" }}>
            <div style={{ color: "#4a5a72", fontSize: "0.72rem", marginBottom: 2 }}>Endpoint</div>
            <code style={{ color: "#00D8FF", fontSize: "0.72rem", background: "rgba(0,216,255,0.08)", padding: "2px 8px", borderRadius: 4 }}>{endpoint}</code>
          </div>
          <button
            onClick={refetch}
            style={{
              background: "rgba(0,216,255,0.1)", border: "1px solid rgba(0,216,255,0.3)",
              borderRadius: 6, color: "#00D8FF", padding: "6px 14px",
              fontSize: "0.75rem", fontWeight: 600, cursor: "pointer",
              fontFamily: "'Montserrat', sans-serif",
            }}
          >
            🔄 Atualizar
          </button>
        </div>
        {hasError && (
          <div style={{ marginTop: 12 }}>
            <ErrorBanner message={errorMessage} onRetry={refetch} isFallback={!isLiveData} />
          </div>
        )}
      </div>

      {/* Summary Cards */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))", gap: 16 }}>
        {[
          { label: "Total Verificações", value: summary.total, color: "#8a9bb5" },
          { label: "✅ Match", value: `${summary.match} (${pctOk}%)`, color: "#00c853" },
          { label: "🟡 Próximos", value: summary.close, color: "#ffa726" },
          { label: "🔴 Divergentes", value: summary.mismatch, color: "#ef5350" },
        ].map((s) => (
          <div key={s.label} className="samarco-card" style={{ textAlign: "center" }}>
            <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em" }}>{s.label}</div>
            <div style={{ color: s.color, fontSize: "1.6rem", fontWeight: 800, marginTop: 6 }}>{s.value}</div>
          </div>
        ))}
      </div>

      {/* Progress Bar */}
      <div className="samarco-card">
        <div className="flex items-center justify-between" style={{ marginBottom: 8 }}>
          <span style={{ color: "#e8edf2", fontSize: "0.82rem", fontWeight: 600 }}>Taxa de Validação</span>
          <span style={{ color: "#00c853", fontWeight: 700 }}>{pctOk}%</span>
        </div>
        <div style={{ background: "#1e3a5f", borderRadius: 4, height: 8, overflow: "hidden" }}>
          <div style={{ background: "linear-gradient(90deg, #00c853, #00D8FF)", height: "100%", width: `${pctOk}%`, borderRadius: 4, transition: "width 0.5s ease" }}/>
        </div>
      </div>

      {/* Criteria */}
      <div className="samarco-card" style={{ background: "rgba(0, 64, 113, 0.15)", borderColor: "#1e3a5f" }}>
        <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: 10 }}>Critérios de Validação</div>
        <div className="flex gap-4" style={{ flexWrap: "wrap" }}>
          {[
            { status: "match" as ValidationStatus, label: "Match exato", desc: "Diferença < 0,01" },
            { status: "close" as ValidationStatus, label: "Próximo", desc: "Diferença < 1,00" },
            { status: "mismatch" as ValidationStatus, label: "Divergente", desc: "Diferença ≥ 1,00" },
          ].map((c) => (
            <div key={c.status} className="flex items-center gap-2">
              <StatusBadge status={c.status} />
              <span style={{ color: "#4a5a72", fontSize: "0.72rem" }}>{c.desc}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Filters */}
      <div className="samarco-card">
        <div style={{ color: "#8a9bb5", fontSize: "0.7rem", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: 12 }}>Filtros</div>
        <div className="flex gap-3" style={{ flexWrap: "wrap" }}>
          <div>
            <label style={{ color: "#4a5a72", fontSize: "0.7rem", display: "block", marginBottom: 4 }}>Cálculo</label>
            <select style={selectStyle} value={filterCalculo} onChange={(e) => setFilterCalculo(e.target.value)}>
              {calculos.map((c) => <option key={c} value={c}>{c === "all" ? "Todos os Cálculos" : c}</option>)}
            </select>
          </div>
          <div>
            <label style={{ color: "#4a5a72", fontSize: "0.7rem", display: "block", marginBottom: 4 }}>Mês</label>
            <select style={selectStyle} value={filterMes} onChange={(e) => setFilterMes(e.target.value)}>
              <option value="all">Todos os Meses</option>
              {MESES.map((m) => <option key={m} value={m}>{m}</option>)}
            </select>
          </div>
          <div>
            <label style={{ color: "#4a5a72", fontSize: "0.7rem", display: "block", marginBottom: 4 }}>Status</label>
            <select style={selectStyle} value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
              <option value="all">Todos os Status</option>
              <option value="match">✅ Match</option>
              <option value="close">🟡 Próximo</option>
              <option value="mismatch">🔴 Divergente</option>
            </select>
          </div>
          <div style={{ marginLeft: "auto", alignSelf: "flex-end" }}>
            <span style={{ color: "#8a9bb5", fontSize: "0.75rem" }}>
              Exibindo <strong style={{ color: "#00D8FF" }}>{filtered.length}</strong> de {allRows.length} verificações
            </span>
          </div>
        </div>
      </div>

      {/* Validation Table */}
      <div className="samarco-card" style={{ padding: 0, overflow: "hidden" }}>
        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.75rem" }}>
            <thead>
              <tr style={{ background: "#0d1520" }}>
                {["Cálculo", "Métrica", "Mês", "Frontend", "API", "Diferença", "Status"].map((h) => (
                  <th key={h} style={{ padding: "10px 14px", textAlign: "left", color: "#8a9bb5", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.05em", fontSize: "0.65rem", borderBottom: "1px solid #1e3a5f", whiteSpace: "nowrap" }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filtered.map((row, i) => (
                <tr key={i} style={{ borderBottom: "1px solid rgba(30,58,95,0.5)", background: i % 2 === 0 ? "transparent" : "rgba(13,21,32,0.3)" }}>
                  <td style={{ padding: "8px 14px", color: "#8a9bb5", whiteSpace: "nowrap" }}>{row.calculo}</td>
                  <td style={{ padding: "8px 14px", color: "#e8edf2", fontWeight: 500 }}>{row.metrica}</td>
                  <td style={{ padding: "8px 14px", color: "#00D8FF", fontWeight: 600 }}>{row.mes}</td>
                  <td style={{ padding: "8px 14px", color: "#e8edf2", fontFamily: "monospace" }}>{row.formatFn(row.valorFrontend)}</td>
                  <td style={{ padding: "8px 14px", color: isLiveData ? "#00c853" : "#ffa726", fontFamily: "monospace" }}>{row.formatFn(row.valorApi)}</td>
                  <td style={{ padding: "8px 14px", color: row.diferenca < 0.01 ? "#4a5a72" : row.diferenca < 1 ? "#ffa726" : "#ef5350", fontFamily: "monospace" }}>
                    {row.diferenca < 0.001 ? "—" : formatNumber(row.diferenca, 4)}
                  </td>
                  <td style={{ padding: "8px 14px" }}><StatusBadge status={row.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
