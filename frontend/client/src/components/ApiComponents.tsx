import type { ApiStatus } from "../hooks/useMotorCalculo";

// ─── Loading Skeleton ──────────────────────────────────────────────────────────

function SkeletonBlock({ width = "100%", height = 16, radius = 4 }: { width?: string | number; height?: number; radius?: number }) {
  return (
    <div style={{
      width, height, borderRadius: radius,
      background: "linear-gradient(90deg, #0d1520 25%, #1a2a3f 50%, #0d1520 75%)",
      backgroundSize: "200% 100%",
      animation: "shimmer 1.5s infinite",
    }}/>
  );
}

export function LoadingSkeleton() {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 24, padding: "8px 0" }}>
      {/* KPI Cards skeleton */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 16 }}>
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="samarco-card" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            <SkeletonBlock width="60%" height={12}/>
            <SkeletonBlock width="80%" height={28} radius={6}/>
            <SkeletonBlock width="45%" height={10}/>
          </div>
        ))}
      </div>
      {/* Chart skeleton */}
      <div className="samarco-card">
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          <SkeletonBlock width="40%" height={14}/>
          <SkeletonBlock width="25%" height={10}/>
          <SkeletonBlock width="100%" height={200} radius={8}/>
        </div>
      </div>
      {/* Table skeleton */}
      <div className="samarco-card">
        <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
          <SkeletonBlock width="35%" height={14}/>
          {[1, 2, 3, 4, 5].map((i) => (
            <SkeletonBlock key={i} width="100%" height={36} radius={4}/>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── Error Banner ──────────────────────────────────────────────────────────────

export function ErrorBanner({
  message,
  onRetry,
  isFallback = false,
}: {
  message: string;
  onRetry?: () => void;
  isFallback?: boolean;
}) {
  return (
    <div style={{
      background: isFallback ? "rgba(255,167,38,0.08)" : "rgba(239,83,80,0.08)",
      border: `1px solid ${isFallback ? "rgba(255,167,38,0.3)" : "rgba(239,83,80,0.3)"}`,
      borderRadius: 10,
      padding: "14px 18px",
      display: "flex",
      alignItems: "flex-start",
      gap: 12,
      marginBottom: 8,
    }}>
      <span style={{ fontSize: "1.2rem", lineHeight: 1.3 }}>{isFallback ? "⚠️" : "🔴"}</span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          color: isFallback ? "#ffa726" : "#ef5350",
          fontWeight: 700,
          fontSize: "0.82rem",
          marginBottom: 4,
        }}>
          {isFallback ? "API indisponível — exibindo dados de referência" : "Erro na API GraphQL"}
        </div>
        <div style={{ color: "#8a9bb5", fontSize: "0.75rem", lineHeight: 1.5 }}>{message}</div>
        {isFallback && (
          <div style={{ color: "#4a5a72", fontSize: "0.72rem", marginTop: 6 }}>
            Os dados exibidos são provenientes do arquivo <code style={{ color: "#00D8FF", background: "rgba(0,216,255,0.08)", padding: "1px 5px", borderRadius: 3 }}>jsonParaComparacao.json</code> (referência de validação).
          </div>
        )}
      </div>
      {onRetry && (
        <button
          onClick={onRetry}
          style={{
            background: "rgba(0,216,255,0.1)",
            border: "1px solid rgba(0,216,255,0.3)",
            borderRadius: 6,
            color: "#00D8FF",
            padding: "6px 14px",
            fontSize: "0.75rem",
            fontWeight: 600,
            cursor: "pointer",
            fontFamily: "'Montserrat', sans-serif",
            whiteSpace: "nowrap",
            flexShrink: 0,
          }}
        >
          🔄 Tentar novamente
        </button>
      )}
    </div>
  );
}

// ─── API Status Badge ──────────────────────────────────────────────────────────

export function ApiStatusBadge({ status, endpoint }: { status: ApiStatus; endpoint: string }) {
  const config: Record<ApiStatus, { icon: string; label: string; color: string; bg: string; border: string }> = {
    loading: { icon: "⏳", label: "Conectando...", color: "#8a9bb5", bg: "rgba(138,155,181,0.1)", border: "rgba(138,155,181,0.3)" },
    live: { icon: "🟢", label: "API Online", color: "#00c853", bg: "rgba(0,200,83,0.1)", border: "rgba(0,200,83,0.3)" },
    fallback: { icon: "🟡", label: "Dados de Referência", color: "#ffa726", bg: "rgba(255,167,38,0.1)", border: "rgba(255,167,38,0.3)" },
    error: { icon: "🔴", label: "API com Erro", color: "#ef5350", bg: "rgba(239,83,80,0.1)", border: "rgba(239,83,80,0.3)" },
  };

  const c = config[status];

  return (
    <div
      title={`Endpoint: ${endpoint}`}
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 6,
        background: c.bg,
        border: `1px solid ${c.border}`,
        borderRadius: 20,
        padding: "4px 12px",
        fontSize: "0.75rem",
        fontWeight: 600,
        color: c.color,
        fontFamily: "'Montserrat', sans-serif",
        cursor: "default",
        userSelect: "none",
      }}
    >
      <span style={{ fontSize: "0.7rem" }}>{c.icon}</span>
      {c.label}
    </div>
  );
}

// ─── Data Source Tag ───────────────────────────────────────────────────────────

export function DataSourceTag({ isLive }: { isLive: boolean }) {
  return (
    <span style={{
      background: isLive ? "rgba(0,200,83,0.1)" : "rgba(255,167,38,0.1)",
      border: `1px solid ${isLive ? "rgba(0,200,83,0.3)" : "rgba(255,167,38,0.3)"}`,
      color: isLive ? "#00c853" : "#ffa726",
      borderRadius: 12,
      padding: "2px 10px",
      fontSize: "0.7rem",
      fontWeight: 600,
    }}>
      {isLive ? "🟢 Dados da API" : "🟡 Dados de Referência"}
    </span>
  );
}
