import React, { useState } from 'react';

const HeroCarpooling: React.FC = () => {
  const [origen, setOrigen] = useState('');
  const [destino, setDestino] = useState('');

  return (
    // ESCALADO: Aumentamos py-20 a py-24 para dar más aire vertical
    <header className="bg-white px-6 md:px-12 py-12 md:py-24 overflow-hidden">
      
      {/* ESCALADO: Subimos max-w-6xl a max-w-7xl para más ancho de pantalla */}
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-12">
        
        {/* Lado Izquierdo: Texto y Buscador - Ahora ocupa el 55% */}
        <div className="w-full md:w-[55%] shrink-0">
          <div className="flex items-center gap-2 mb-6 bg-green-50 w-fit px-3.5 py-1.5 rounded-full border border-green-100">
            <span className="h-2.5 w-2.5 rounded-full bg-[#00BF63]"></span>
            <span className="text-[11px] font-bold text-[#00BF63] uppercase tracking-wider">
              Carpooling diario · Rápido, fácil y sostenible
            </span>
          </div>

          {/* ESCALADO: Titular a text-5xl (móvil) y text-6xl/7xl (escritorio) */}
          <h1 className="text-5xl md:text-6xl lg:text-7xl font-extrabold text-slate-950 leading-[1.1] mb-8 tracking-tight">
            Comparte el camino, <br />
            <span className="text-gradient">reduce el gasto.</span>
          </h1>

          {/* ESCALADO: Párrafo a text-lg/xl */}
          <p className="text-gray-500 text-lg md:text-xl mb-12 max-w-lg leading-relaxed">
            Conecta con vecinos y compañeros que hacen el mismo trayecto cada día. 
            Ahorra dinero, reduce emisiones y llega sin estrés.
          </p>

          {/* ESCALADO: Buscador más grande (p-3 y py-4 en inputs) */}
          <div className="bg-white rounded-2xl md:rounded-full p-3 shadow-2xl border border-gray-100 flex flex-col md:flex-row items-center w-full max-w-3xl">
            <div className="flex items-center flex-1 px-5 gap-3 w-full border-b md:border-b-0 md:border-r border-gray-100 border-gray-100">
              <span className="text-red-400 text-xl">📍</span>
              <input 
                type="text" 
                placeholder="¿Dónde empiezas?" 
                className="py-4 text-base focus:outline-none w-full bg-transparent text-slate-800 placeholder:text-gray-400"
                value={origen}
                onChange={(e) => setOrigen(e.target.value)}
              />
            </div>
            <div className="flex items-center flex-1 px-5 gap-3 w-full">
              <span className="text-red-400 text-xl">📍</span>
              <input 
                type="text" 
                placeholder="¿Adónde vas?" 
                className="py-4 text-base focus:outline-none w-full bg-transparent text-slate-800 placeholder:text-gray-400"
                value={destino}
                onChange={(e) => setDestino(e.target.value)}
              />
            </div>
            <button className="bg-gradient-compi hover:opacity-90 text-white px-9 py-4 rounded-full font-bold flex items-center gap-2 transition-all w-full md:w-auto justify-center text-sm shadow-md active:scale-95">
              <span>🔍</span> Buscar viaje
            </button>
          </div>
        </div>

        {/* Lado Derecho: Imagen - Ahora ocupa el 45% (más grande) */}
        <div className="w-full md:w-[45%] flex justify-end">
          <img 
            src="/images/principal.png" 
            alt="Amigos en coche" 
            // ESCALADO: max-w controlado para que no se estire demasiado en pantallas gigantes
            className="rounded-[3rem] shadow-2xl w-full h-auto object-cover max-w-[500px] lg:max-w-none"
          />
        </div>

      </div>
    </header>
  );
};

export default HeroCarpooling;