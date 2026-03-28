import Navbar from './components/Navbar'
import HeroCarpooling from './components/HeroCarpooling'
import HowItWorks from './components/HowItWorks'
import Footer from './components/Footer'
// import './App.css' // Puedes borrar este import si usas Tailwind puro

function App() {
  return (
    <div className="min-h-screen flex flex-col bg-white font-sans overflow-x-hidden">
      
      {/* 1. Barra de Navegación (Fija arriba) */}
      <Navbar />
      
      {/* Contenido Scrolleable */}
      <main className="flex-grow">
        {/* 2. Sección Hero (La cabecera con la foto del coche) */}
        <HeroCarpooling />
        
        {/* 3. Sección Cómo Funciona (Los tres pasos) */}
        <HowItWorks />
      </main>

      <Footer />

    </div>
  )
}

export default App