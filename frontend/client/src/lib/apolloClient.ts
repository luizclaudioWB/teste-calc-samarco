import { ApolloClient, InMemoryCache, HttpLink } from "@apollo/client";

/**
 * GraphQL endpoint for the SAMARCO Quarkus backend.
 * Can be overridden via VITE_GRAPHQL_ENDPOINT environment variable.
 * Falls back to localhost:8080 for local development.
 */
export const GRAPHQL_ENDPOINT: string =
  (import.meta.env?.VITE_GRAPHQL_ENDPOINT as string | undefined) ?? "http://localhost:8080/graphql";

const httpLink = new HttpLink({
  uri: GRAPHQL_ENDPOINT,
  fetchOptions: { method: "POST" },
  headers: { "Content-Type": "application/json" },
});

export const apolloClient = new ApolloClient({
  link: httpLink,
  cache: new InMemoryCache(),
  defaultOptions: {
    query: {
      fetchPolicy: "cache-first",
      errorPolicy: "all",
    },
    watchQuery: {
      fetchPolicy: "cache-and-network",
      errorPolicy: "all",
    },
  },
});

/** Helper to extract a human-readable error message from any error type */
export function getErrorMessage(error: unknown): string {
  if (!error) return "";
  if (error instanceof Error) {
    const msg = error.message;
    if (
      msg.includes("NetworkError") ||
      msg.includes("Failed to fetch") ||
      msg.includes("ECONNREFUSED") ||
      msg.includes("Load failed")
    ) {
      return `Não foi possível conectar à API GraphQL (${GRAPHQL_ENDPOINT}). Verifique se o backend Quarkus está em execução.`;
    }
    return msg;
  }
  return String(error);
}

/** Helper to check if an error is a network connectivity issue */
export function isNetworkError(error: unknown): boolean {
  if (!error) return false;
  const msg = error instanceof Error ? error.message : String(error);
  return (
    msg.includes("NetworkError") ||
    msg.includes("Failed to fetch") ||
    msg.includes("ECONNREFUSED") ||
    msg.includes("Load failed") ||
    msg.toLowerCase().includes("network")
  );
}
