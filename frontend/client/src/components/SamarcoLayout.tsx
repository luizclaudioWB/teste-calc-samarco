import { useState, useEffect, createContext, useContext } from "react";
import { Link, useLocation } from "wouter";

// ─── Period Filter Context ────────────────────────────────────────────────────
export type PeriodFilter = {
  type: "all" | "month" | "quarter";
  value: number | null; // month: 0-11, quarter: 1-4
};

const PeriodContext = createContext<{
  period: PeriodFilter;
  setPeriod: (p: PeriodFilter) => void;
}>({ period: { type: "all", value: null }, setPeriod: () => {} });

export function usePeriod() {
  return useContext(PeriodContext);
}

const MESES = ["Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"];

export function getFilteredMonths(period: PeriodFilter): string[] {
  if (period.type === "all") return MESES;
  if (period.type === "month" && period.value !== null) return [MESES[period.value]];
  if (period.type === "quarter" && period.value !== null) {
    const q = period.value;
    return MESES.slice((q - 1) * 3, q * 3);
  }
  return MESES;
}

export function filterByPeriod<T extends { mes: string }>(arr: T[], period: PeriodFilter): T[] {
  const months = getFilteredMonths(period);
  return arr.filter((item) => months.includes(item.mes));
}

// ─── Nav Items ────────────────────────────────────────────────────────────────
interface NavItem {
  path: string;
  label: string;
  icon: string;
  badge?: string;
  group: string;
}

const navItems: NavItem[] = [
  { path: "/", label: "Dashboard", icon: "📊", group: "main" },
  { path: "/producao", label: "Produção", icon: "🏭", badge: "01", group: "calculos" },
  { path: "/consumo-especifico", label: "Consumo Específico", icon: "⚡", badge: "02", group: "calculos" },
  { path: "/consumo-area", label: "Consumo Área", icon: "📈", badge: "03", group: "calculos" },
  { path: "/geracao", label: "Geração Própria", icon: "🔋", badge: "04", group: "calculos" },
  { path: "/encargos", label: "Encargos ESS/EER", icon: "💰", badge: "05", group: "calculos" },
  { path: "/distribuicao-carga", label: "Distribuição de Carga", icon: "🔄", badge: "13", group: "calculos" },
  { path: "/classe-custo", label: "Classe de Custo", icon: "🏷️", badge: "14", group: "calculos" },
  { path: "/centro-custos", label: "Centro de Custos", icon: "🏢", badge: "15", group: "calculos" },
  { path: "/resumo-geral", label: "Resumo Geral", icon: "📋", badge: "16", group: "calculos" },
  { path: "/validacao", label: "Validação", icon: "✅", group: "validacao" },
];

const pageLabels: Record<string, string> = {
  "/": "Dashboard",
  "/producao": "Cálculo 01 — Produção",
  "/consumo-especifico": "Cálculo 02 — Consumo Específico",
  "/consumo-area": "Cálculo 03 — Consumo Área",
  "/geracao": "Cálculo 04 — Geração Própria",
  "/encargos": "Cálculo 05 — Encargos ESS/EER",
  "/distribuicao-carga": "Cálculo 13 — Distribuição de Carga",
  "/classe-custo": "Cálculo 14 — Classe de Custo",
  "/centro-custos": "Cálculo 15 — Centro de Custos",
  "/resumo-geral": "Cálculo 16 — Resumo Geral",
  "/validacao": "Painel de Validação",
};

// ─── Logo ─────────────────────────────────────────────────────────────────────
const LOGO_URL = "https://d2xsxph8kpxj0f.cloudfront.net/310519663434221501/Kg2woXgZyKb3u9tZyhTeWU/samarco-logo_ca74d1cc.png";

function SamarcoLogo({ collapsed }: { collapsed: boolean }) {
  return (
    <div className="flex items-center gap-2">
      <img
        src={LOGO_URL}
        alt="SAMARCO"
        style={{
          height: 36,
          width: "auto",
          objectFit: "contain",
          filter: "brightness(1.1)",
        }}
      />
      {!collapsed && (
        <div>
          <div style={{ color: "#e8edf2", fontWeight: 800, fontSize: "0.95rem", letterSpacing: "0.08em", lineHeight: 1 }}>SAMARCO</div>
          <div style={{ color: "#8a9bb5", fontSize: "0.6rem", fontWeight: 500, letterSpacing: "0.04em" }}>Motor de Cálculo Energético</div>
        </div>
      )}
    </div>
  );
}

// ─── API Status ───────────────────────────────────────────────────────────────
const GRAPHQL_ENDPOINT = import.meta.env.VITE_GRAPHQL_ENDPOINT || "http://localhost:8080/graphql";

function ApiStatusIndicator() {
  const [status, setStatus] = useState<"online" | "offline" | "checking">("checking");
  const [isLive, setIsLive] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const check = async () => {
      try {
        const res = await fetch(GRAPHQL_ENDPOINT, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ query: "{ __typename }" }),
          signal: AbortSignal.timeout(3000),
        });
        if (!cancelled) {
          if (res.ok) { setStatus("online"); setIsLive(true); }
          else { setStatus("offline"); setIsLive(false); }
        }
      } catch {
        if (!cancelled) { setStatus("offline"); setIsLive(false); }
      }
    };
    check();
    const interval = setInterval(check, 30000);
    return () => { cancelled = true; clearInterval(interval); };
  }, []);

  const color = status === "online" ? "#00c853" : status === "offline" ? "#ef5350" : "#ffa726";
  const label = status === "online" ? "API Online" : status === "offline" ? "API Offline" : "Verificando...";
  const source = isLive ? "Dados ao vivo" : "Dados de referência";

  return (
    <div className="flex items-center gap-2" title={`${label} — ${source}`}>
      <div style={{
        width: 8, height: 8, borderRadius: "50%",
        backgroundColor: color,
        boxShadow: `0 0 6px ${color}`,
        animation: status === "checking" ? "pulse 1.5s infinite" : "none",
      }} />
      <span style={{ color: "#8a9bb5", fontSize: "0.75rem", fontWeight: 500 }}>
        {label}
      </span>
      {status !== "checking" && (
        <span style={{
          fontSize: "0.65rem", fontWeight: 600, padding: "1px 6px", borderRadius: 10,
          background: isLive ? "rgba(0,200,83,0.1)" : "rgba(255,167,38,0.1)",
          color: isLive ? "#00c853" : "#ffa726",
          border: `1px solid ${isLive ? "rgba(0,200,83,0.3)" : "rgba(255,167,38,0.3)"}`,
        }}>
          {source}
        </span>
      )}
    </div>
  );
}

// ─── Period Filter Bar ────────────────────────────────────────────────────────
function PeriodFilterBar() {
  const { period, setPeriod } = usePeriod();
  const btnBase: React.CSSProperties = {
    padding: "4px 10px",
    borderRadius: 5,
    border: "1px solid #1e3a5f",
    background: "transparent",
    color: "#8a9bb5",
    fontSize: "0.72rem",
    fontWeight: 500,
    cursor: "pointer",
    fontFamily: "'Montserrat', sans-serif",
    transition: "all 0.15s",
  };
  const btnActive: React.CSSProperties = {
    ...btnBase,
    background: "rgba(0, 216, 255, 0.12)",
    borderColor: "#00D8FF",
    color: "#00D8FF",
    fontWeight: 700,
  };

  const isAll = period.type === "all";
  const isMonth = (m: number) => period.type === "month" && period.value === m;
  const isQ = (q: number) => period.type === "quarter" && period.value === q;

  return (
    <div className="flex items-center gap-2" style={{ flexWrap: "wrap" }}>
      <span style={{ color: "#4a5a72", fontSize: "0.7rem", fontWeight: 600, letterSpacing: "0.05em", textTransform: "uppercase" }}>Período:</span>
      <button style={isAll ? btnActive : btnBase} onClick={() => setPeriod({ type: "all", value: null })}>Anual</button>
      {[1, 2, 3, 4].map((q) => (
        <button key={q} style={isQ(q) ? btnActive : btnBase} onClick={() => setPeriod({ type: "quarter", value: q })}>
          T{q}
        </button>
      ))}
      <div style={{ width: 1, height: 16, background: "#1e3a5f", margin: "0 2px" }} />
      {["Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez"].map((m, i) => (
        <button key={m} style={isMonth(i) ? btnActive : btnBase} onClick={() => setPeriod({ type: "month", value: i })}>
          {m}
        </button>
      ))}
    </div>
  );
}

// ─── Layout ───────────────────────────────────────────────────────────────────
interface SamarcoLayoutProps {
  children: React.ReactNode;
}

export default function SamarcoLayout({ children }: SamarcoLayoutProps) {
  const [collapsed, setCollapsed] = useState(false);
  const [location] = useLocation();
  const [period, setPeriod] = useState<PeriodFilter>({ type: "all", value: null });

  const currentLabel = pageLabels[location] || "Dashboard";
  const pathParts = location.split("/").filter(Boolean);
  const chainNumbers = ["01", "02", "03", "04", "05", "13", "14", "15", "16"];

  return (
    <PeriodContext.Provider value={{ period, setPeriod }}>
      <div style={{ display: "flex", flexDirection: "column", height: "100vh", backgroundColor: "#0a0e14", fontFamily: "'Montserrat', sans-serif" }}>
        {/* Header */}
        <header className="glass-header" style={{
          height: "var(--header-height, 64px)",
          display: "flex", alignItems: "center", justifyContent: "space-between",
          padding: "0 1.5rem", position: "fixed", top: 0, left: 0, right: 0, zIndex: 100,
        }}>
          <div className="flex items-center gap-3">
            <button onClick={() => setCollapsed(!collapsed)} style={{
              background: "rgba(30, 58, 95, 0.4)", border: "1px solid #1e3a5f",
              borderRadius: 6, padding: "6px 8px", color: "#8a9bb5", cursor: "pointer",
              display: "flex", alignItems: "center", justifyContent: "center",
            }}>
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
                <rect x="2" y="4" width="14" height="1.5" rx="0.75" fill="currentColor"/>
                <rect x="2" y="8.25" width="14" height="1.5" rx="0.75" fill="currentColor"/>
                <rect x="2" y="12.5" width="14" height="1.5" rx="0.75" fill="currentColor"/>
              </svg>
            </button>
            <SamarcoLogo collapsed={false} />
          </div>
          <div className="flex items-center gap-4">
            <span className="badge-accent">Ano Base: 2026</span>
            <ApiStatusIndicator />
          </div>
        </header>

        {/* Body */}
        <div style={{ display: "flex", flex: 1, marginTop: "var(--header-height, 64px)", overflow: "hidden" }}>
          {/* Sidebar */}
          <aside style={{
            width: collapsed ? "var(--sidebar-collapsed, 64px)" : "var(--sidebar-width, 240px)",
            minWidth: collapsed ? "var(--sidebar-collapsed, 64px)" : "var(--sidebar-width, 240px)",
            backgroundColor: "#0d1520", borderRight: "1px solid #1e3a5f",
            display: "flex", flexDirection: "column",
            transition: "width 0.2s ease, min-width 0.2s ease",
            overflow: "hidden", position: "fixed",
            top: "var(--header-height, 64px)", bottom: 0, left: 0, zIndex: 50,
          }}>
            <nav style={{ flex: 1, padding: "12px 0", overflowY: "auto", overflowX: "hidden" }}>
              {/* Chain indicator */}
              {!collapsed && (
                <div style={{ padding: "8px 16px 12px", marginBottom: 4 }}>
                  <div style={{ fontSize: "0.65rem", color: "#4a5a72", fontWeight: 600, letterSpacing: "0.08em", textTransform: "uppercase", marginBottom: 6 }}>
                    Cálculos Encadeados
                  </div>
                  <div style={{ display: "flex", flexWrap: "wrap", gap: 3, fontSize: "0.6rem", color: "#457ba9", alignItems: "center" }}>
                    {chainNumbers.map((n, i) => (
                      <span key={n} style={{ display: "flex", alignItems: "center", gap: 3 }}>
                        <span style={{
                          background: "rgba(69, 123, 169, 0.2)", border: "1px solid #457ba9",
                          borderRadius: 3, padding: "1px 5px", fontWeight: 700, fontSize: "0.6rem"
                        }}>{n}</span>
                        {i < chainNumbers.length - 1 && <span style={{ color: "#4a5a72" }}>→</span>}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {navItems.map((item, idx) => {
                const isActive = location === item.path;
                const showSeparator = idx > 0 && item.group !== navItems[idx - 1].group;
                return (
                  <div key={item.path}>
                    {showSeparator && <div style={{ margin: "8px 16px", borderTop: "1px solid #1e3a5f" }} />}
                    <Link href={item.path}>
                      <div
                        className={`sidebar-item ${isActive ? "sidebar-item-active" : ""}`}
                        style={{
                          display: "flex", alignItems: "center",
                          gap: collapsed ? 0 : 10,
                          padding: collapsed ? "12px 0" : "10px 16px",
                          justifyContent: collapsed ? "center" : "flex-start",
                          cursor: "pointer",
                          color: isActive ? "#00D8FF" : "#8a9bb5",
                          textDecoration: "none",
                          fontSize: "0.82rem",
                          fontWeight: isActive ? 600 : 400,
                        }}
                        title={collapsed ? item.label : undefined}
                      >
                        <span style={{ fontSize: "1rem", flexShrink: 0 }}>{item.icon}</span>
                        {!collapsed && (
                          <span style={{ flex: 1, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                            {item.label}
                          </span>
                        )}
                        {!collapsed && item.badge && (
                          <span style={{
                            background: isActive ? "rgba(0, 216, 255, 0.15)" : "rgba(30, 58, 95, 0.5)",
                            color: isActive ? "#00D8FF" : "#4a5a72",
                            border: `1px solid ${isActive ? "rgba(0, 216, 255, 0.3)" : "#1e3a5f"}`,
                            borderRadius: 4, padding: "1px 6px", fontSize: "0.65rem", fontWeight: 700,
                          }}>{item.badge}</span>
                        )}
                      </div>
                    </Link>
                  </div>
                );
              })}
            </nav>
            {!collapsed && (
              <div style={{ padding: "12px 16px", borderTop: "1px solid #1e3a5f" }}>
                <div style={{ fontSize: "0.65rem", color: "#4a5a72", textAlign: "center" }}>
                  Motor de Cálculo Energético v2.0
                </div>
              </div>
            )}
          </aside>

          {/* Main content */}
          <main style={{
            flex: 1, overflow: "auto",
            marginLeft: collapsed ? "var(--sidebar-collapsed, 64px)" : "var(--sidebar-width, 240px)",
            transition: "margin-left 0.2s ease",
            backgroundColor: "#0a0e14",
          }}>
            {/* Breadcrumb */}
            <div style={{
              padding: "10px 24px", borderBottom: "1px solid rgba(30, 58, 95, 0.4)",
              backgroundColor: "rgba(13, 21, 32, 0.6)",
              display: "flex", alignItems: "center", gap: 6,
              fontSize: "0.78rem", color: "#8a9bb5",
            }}>
              <Link href="/"><span style={{ cursor: "pointer" }}>🏠</span></Link>
              <span style={{ color: "#4a5a72" }}>/</span>
              {pathParts.length === 0 ? (
                <span style={{ color: "#e8edf2", fontWeight: 500 }}>Dashboard</span>
              ) : (
                <>
                  <Link href="/"><span style={{ cursor: "pointer", color: "#8a9bb5" }}>Dashboard</span></Link>
                  <span style={{ color: "#4a5a72" }}>/</span>
                  <span style={{ color: "#e8edf2", fontWeight: 500 }}>{currentLabel}</span>
                </>
              )}
            </div>

            {/* Period Filter Bar */}
            <div style={{
              padding: "10px 24px",
              borderBottom: "1px solid rgba(30, 58, 95, 0.3)",
              backgroundColor: "rgba(13, 21, 32, 0.4)",
            }}>
              <PeriodFilterBar />
            </div>

            {/* Page content */}
            <div className="page-enter" style={{ padding: "24px" }}>
              {children}
            </div>
          </main>
        </div>
      </div>
    </PeriodContext.Provider>
  );
}
