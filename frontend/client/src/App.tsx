import { Toaster } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { Route, Switch } from "wouter";
import ErrorBoundary from "./components/ErrorBoundary";
import { ThemeProvider } from "./contexts/ThemeContext";
import SamarcoLayout from "./components/SamarcoLayout";
import Dashboard from "./pages/Dashboard";
import Producao from "./pages/Producao";
import ConsumoEspecifico from "./pages/ConsumoEspecifico";
import ConsumoArea from "./pages/ConsumoArea";
import GeracaoPropria from "./pages/GeracaoPropria";
import Encargos from "./pages/Encargos";
import DistribuicaoCarga from "./pages/DistribuicaoCarga";
import ClasseCusto from "./pages/ClasseCusto";
import CentroCustos from "./pages/CentroCustos";
import ResumoGeral from "./pages/ResumoGeral";
import Validacao from "./pages/Validacao";
import NotFound from "./pages/NotFound";

function Router() {
  return (
    <SamarcoLayout>
      <Switch>
        <Route path="/" component={Dashboard} />
        <Route path="/producao" component={Producao} />
        <Route path="/consumo-especifico" component={ConsumoEspecifico} />
        <Route path="/consumo-area" component={ConsumoArea} />
        <Route path="/geracao" component={GeracaoPropria} />
        <Route path="/encargos" component={Encargos} />
        <Route path="/distribuicao-carga" component={DistribuicaoCarga} />
        <Route path="/classe-custo" component={ClasseCusto} />
        <Route path="/centro-custos" component={CentroCustos} />
        <Route path="/resumo-geral" component={ResumoGeral} />
        <Route path="/validacao" component={Validacao} />
        <Route component={NotFound} />
      </Switch>
    </SamarcoLayout>
  );
}

function App() {
  return (
    <ErrorBoundary>
      <ThemeProvider defaultTheme="dark">
        <TooltipProvider>
          <Toaster />
          <Router />
        </TooltipProvider>
      </ThemeProvider>
    </ErrorBoundary>
  );
}

export default App;
