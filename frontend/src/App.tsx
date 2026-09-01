import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import Navbar from './components/Navbar';
import HeroCarpooling from './components/HeroCarpooling';
import HowItWorks from './components/HowItWorks';
import Footer from './components/Footer';
import Registro from './components/autenticacion/Registro';
import InicioSesion from './components/autenticacion/InicioSesion';
import Perfil from './components/autenticacion/Perfil';
import CrearViaje from './components/viajes/CrearViaje';
import HomeLoggedIn from './components/HomeLoggedIn';
import NuevoVehiculo from './components/vehiculos/NuevoVehiculo';
import MisViajes from './components/viajes/MisViajes';
import DetalleViaje from './components/viajes/DetalleViaje';
import ResultadosBusquedaViajes from './components/viajes/ResultadosBusquedaViajes';
import TodosLosViajes from './components/viajes/TodosLosViajes';
import PerfilPublico from './components/autenticacion/PerfilPublico';
import Notificaciones from './components/notificacion/Notificaciones';
import Valoraciones from './components/autenticacion/Valoraciones';
import ViajesAsociadosScreen from './components/viajes/ViajesAsociadosScreen';

const hasValidToken = () => {
  const token = localStorage.getItem('token');
  return !!token && token !== 'undefined' && token !== 'null' && token.trim() !== '';
};

function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(hasValidToken());

  useEffect(() => {
    const handleAuthChange = () => setIsLoggedIn(hasValidToken());
    window.addEventListener('authChange', handleAuthChange);
    window.addEventListener('storage', handleAuthChange);

    return () => {
      window.removeEventListener('authChange', handleAuthChange);
      window.removeEventListener('storage', handleAuthChange);
    };
  }, []);

  return (
    <Router>
      <div className="min-h-screen flex flex-col bg-white font-sans overflow-x-hidden">
        <Navbar />

        <main className="flex-grow">
          <Routes>
            <Route
              path="/"
              element={
                isLoggedIn ? (
                  <HomeLoggedIn />
                ) : (
                  <>
                    <HeroCarpooling />
                    <HowItWorks />
                  </>
                )
              }
            />
            <Route path="/registro" element={<Registro />} />
            <Route path="/inicio-sesion" element={<InicioSesion />} />
            <Route path="/perfil" element={<Perfil />} />
            <Route path="/vehiculos/nuevo" element={<NuevoVehiculo />} />
            <Route path="/mis-viajes" element={<MisViajes />} />
            <Route path="/ofrecer-trayecto" element={<CrearViaje />} />
            <Route path="/viajes/:slug" element={<DetalleViaje />} />
            <Route path="/ofrecer-trayecto" element={isLoggedIn ? <CrearViaje /> : <Navigate to="/inicio-sesion" replace />} />
            <Route path="/buscar" element={<ResultadosBusquedaViajes />} />
            <Route path="/explorar" element={<TodosLosViajes />} />
            <Route path="/usuarios/:slug/perfil" element={<PerfilPublico />} />
            <Route path="/notificaciones" element={<Notificaciones />} />
            <Route path="/valoraciones" element={isLoggedIn ? <Valoraciones /> : <Navigate to="/inicio-sesion" replace />} />
            <Route path="/viajes/:slug/asociados" element={<ViajesAsociadosScreen />} />
          </Routes>
        </main>

        <Footer />
      </div>
    </Router>
  );
}

export default App;
