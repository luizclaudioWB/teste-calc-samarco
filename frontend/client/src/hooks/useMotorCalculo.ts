import { useQuery } from "@apollo/client/react";
import { useMemo } from "react";
import { mockApiData } from "../data/mockData";
import { MOTOR_CALCULO_QUERY, type MotorCalculoData, type MotorCalculoQueryResult } from "../lib/graphqlQueries";
import { getErrorMessage, isNetworkError, GRAPHQL_ENDPOINT } from "../lib/apolloClient";

export type ApiStatus = "loading" | "live" | "fallback" | "error";

export type UseMotorCalculoResult = {
  /** The resolved data — either from the API or from mock fallback */
  data: MotorCalculoData;
  /** True while the initial fetch is in progress */
  loading: boolean;
  /** True if the API returned an error (data will be mock fallback) */
  hasError: boolean;
  /** Human-readable error message when hasError is true */
  errorMessage: string;
  /** Whether the current data is live (from API) or mock (fallback) */
  isLiveData: boolean;
  /** Current API connection status */
  apiStatus: ApiStatus;
  /** Trigger a manual refetch from the API */
  refetch: () => void;
  /** The GraphQL endpoint being used */
  endpoint: string;
};

/**
 * Primary data hook for the SAMARCO Motor de Cálculo Energético.
 *
 * Behavior:
 * - Fetches data from the GraphQL API (Quarkus backend) using Apollo Client.
 * - While loading, returns mock data so the UI renders immediately.
 * - On network/GraphQL error, falls back to mock data and sets hasError=true.
 * - When the API responds successfully, replaces mock data with live data.
 *
 * Usage:
 * ```tsx
 * const { data, loading, hasError, errorMessage, isLiveData, apiStatus } = useMotorCalculo();
 * ```
 */
export function useMotorCalculo(): UseMotorCalculoResult {
  const { data: rawData, loading, error, refetch } = useQuery<MotorCalculoQueryResult>(
    MOTOR_CALCULO_QUERY,
    {
      errorPolicy: "all",
      notifyOnNetworkStatusChange: true,
    }
  );

  const result = useMemo((): UseMotorCalculoResult => {
    const hasApiData = !!rawData?.calcularMotorCompleto;
    const hasError = !!error;
    const isLiveData = hasApiData && !hasError;

    // Merge API data with mock as fallback for any missing fields
    const data: MotorCalculoData = hasApiData
      ? { ...mockApiData, ...rawData!.calcularMotorCompleto }
      : mockApiData;

    let apiStatus: ApiStatus;
    if (loading && !hasApiData) {
      apiStatus = "loading";
    } else if (isLiveData) {
      apiStatus = "live";
    } else if (hasError && !hasApiData) {
      apiStatus = "fallback";
    } else if (hasError) {
      apiStatus = "error";
    } else {
      apiStatus = "fallback";
    }

    const errorMessage = hasError ? getErrorMessage(error) : "";

    return {
      data,
      loading: loading && !hasApiData,
      hasError,
      errorMessage,
      isLiveData,
      apiStatus,
      refetch: () => { refetch().catch(console.error); },
      endpoint: GRAPHQL_ENDPOINT,
    };
  }, [rawData, loading, error, refetch]);

  return result;
}
