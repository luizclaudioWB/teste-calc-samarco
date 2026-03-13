import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const MESES = ["Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"];

export function formatMWh(value: number, decimals = 2): string {
  return new Intl.NumberFormat("pt-BR", {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value) + " MWh";
}

export function formatTms(value: number): string {
  return new Intl.NumberFormat("pt-BR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

export function formatNumber(value: number, decimals = 2): string {
  return new Intl.NumberFormat("pt-BR", {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value);
}

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

export function formatPercent(value: number, decimals = 4): string {
  return new Intl.NumberFormat("pt-BR", {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value * 100) + "%";
}

export function formatKWh(value: number, decimals = 4): string {
  return new Intl.NumberFormat("pt-BR", {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value);
}

export function sumArray(arr: { mes: string; valor: number }[]): number {
  return arr.reduce((acc, item) => acc + item.valor, 0);
}

export function getByMes(arr: { mes: string; valor: number }[], mes: string): number {
  return arr.find((item) => item.mes === mes)?.valor ?? 0;
}

export function getHeatmapColor(value: number, min: number, max: number): string {
  if (max === min) return "rgba(69, 123, 169, 0.3)";
  const ratio = (value - min) / (max - min);
  if (ratio < 0.2) return "rgba(0, 200, 83, 0.25)";
  if (ratio < 0.4) return "rgba(0, 200, 83, 0.15)";
  if (ratio < 0.6) return "rgba(255, 167, 38, 0.2)";
  if (ratio < 0.8) return "rgba(239, 83, 80, 0.2)";
  return "rgba(239, 83, 80, 0.35)";
}

export function getHeatmapTextColor(value: number, min: number, max: number): string {
  if (max === min) return "#8a9bb5";
  const ratio = (value - min) / (max - min);
  if (ratio < 0.4) return "#00c853";
  if (ratio < 0.6) return "#ffa726";
  return "#ef5350";
}

export const CHART_COLORS = {
  accent: "#00D8FF",
  primary: "#457ba9",
  success: "#00c853",
  warning: "#ffa726",
  danger: "#ef5350",
  mg: "#457ba9",
  es: "#00D8FF",
  guilman: "#457ba9",
  muniz: "#00c853",
  eer: "#00D8FF",
  ess: "#00c853",
};

export const RECHARTS_TOOLTIP_STYLE = {
  backgroundColor: "#111922",
  border: "1px solid #1e3a5f",
  borderRadius: 8,
  color: "#e8edf2",
  fontFamily: "'Montserrat', sans-serif",
  fontSize: "0.78rem",
};
