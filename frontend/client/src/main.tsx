import { ApolloProvider } from "@apollo/client/react";
import { apolloClient } from "./lib/apolloClient";
import { createRoot } from "react-dom/client";
import App from "./App";
import "./index.css";

createRoot(document.getElementById("root")!).render(
  <ApolloProvider client={apolloClient}>
    <App />
  </ApolloProvider>
);
