import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import HeroCarpooling from './components/HeroCarpooling';
import HowItWorks from './components/HowItWorks';
import Footer from './components/Footer';
import Registro from './components/autenticacion/Registro';
import InicioSesion from './components/autenticacion/InicioSesion';
import Perfil from './components/autenticacion/Perfil';

// import './App.css' // Puedes borrar este import si usas Tailwind puro

function App() {
  return (
    <Router>
      <div className="min-h-screen flex flex-col bg-white font-sans overflow-x-hidden">
        {/* 1. Barra de Navegación (Fija arriba) */}
        <Navbar />

        {/* Contenido Scrolleable */}
        <main className="flex-grow">
          <Routes>
            <Route path="/" element={
              <>
                {/* 2. Sección Hero (La cabecera con la foto del coche) */}
                <HeroCarpooling />

                {/* 3. Sección Cómo Funciona (Los tres pasos) */}
                <HowItWorks />
              </>
            } />
            <Route path="/registro" element={<Registro />} />
            <Route path="/inicio-sesion" element={<InicioSesion />} />
            <Route path="/perfil" element={<Perfil />} />
          </Routes>
        </main>

        <Footer />
      </div>
    </Router>
  );
}

export default App;
