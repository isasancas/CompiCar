import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const HeroCarpooling: React.FC = () => {
  const navigate = useNavigate();
  const [origen, setOrigen] = useState('');
  const [destino, setDestino] = useState('');
  const [fecha, setFecha] = useState('');

  const handleBuscar = (e: React.FormEvent) => {
    e.preventDefault();

    const params = new URLSearchParams();
    if (origen.trim()) params.set('origen', origen.trim());
    if (destino.trim()) params.set('destino', destino.trim());
    if (fecha) params.set('fecha', fecha);

    navigate('/buscar?' + params.toString());
  };

  return (
    <header className="bg-white px-6 md:px-12 py-12 md:py-24 overflow-hidden">
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-12">
        <div className="w-full md:w-[55%] shrink-0">
          <div className="flex items-center gap-2 mb-6 bg-green-50 w-fit px-3.5 py-1.5 rounded-full border border-green-100">
            <span className="h-2.5 w-2.5 rounded-full bg-[#00BF63]"></span>
            <span className="text-[11px] font-bold text-[#00BF63] uppercase tracking-wider">
              Carpooling diario · Rapido, facil y sostenible
            </span>
          </div>

          <h1 className="text-5xl md:text-6xl lg:text-7xl font-extrabold text-slate-950 leading-[1.1] mb-8 tracking-tight">
            Comparte el camino, <br />
            <span className="text-gradient">reduce el gasto.</span>
          </h1>

          <p className="text-gray-500 text-lg md:text-xl mb-12 max-w-lg leading-relaxed">
            Conecta con vecinos y companeros que hacen el mismo trayecto cada dia.
            Ahorra dinero, reduce emisiones y llega sin estres.
          </p>

          <div className="flex gap-4 mb-12">
            <button
              onClick={() => navigate('/explorar')}
              className="border-2 border-green-600 text-green-600 hover:bg-green-50 px-8 py-3 rounded-full font-bold transition-all"
            >
              Explorar todos los viajes
            </button>
          </div>

          <form
            onSubmit={handleBuscar}
            className="bg-white rounded-2xl md:rounded-full p-3 shadow-2xl border border-gray-100 flex flex-col md:flex-row items-center w-full max-w-4xl"
          >
            <div className="flex items-center flex-1 px-5 gap-3 w-full border-b md:border-b-0 md:border-r border-gray-100">
              <span className="text-red-400 text-xl">📍</span>
              <input
                type="text"
                placeholder="Donde empiezas?"
                className="py-4 text-base focus:outline-none w-full bg-transparent text-slate-800 placeholder:text-gray-400"
                value={origen}
                onChange={(e) => setOrigen(e.target.value)}
              />
            </div>

            <div className="flex items-center flex-1 px-5 gap-3 w-full border-b md:border-b-0 md:border-r border-gray-100">
              <span className="text-red-400 text-xl">📍</span>
              <input
                type="text"
                placeholder="Adonde vas?"
                className="py-4 text-base focus:outline-none w-full bg-transparent text-slate-800 placeholder:text-gray-400"
                value={destino}
                onChange={(e) => setDestino(e.target.value)}
              />
            </div>

            <div className="flex items-center flex-1 px-5 gap-3 w-full">
              <span className="text-slate-500 text-lg">📅</span>
              <input
                type="date"
                className="py-4 text-base focus:outline-none w-full bg-transparent text-slate-700"
                value={fecha}
                onChange={(e) => setFecha(e.target.value)}
              />
            </div>

            <button
              type="submit"
              className="bg-gradient-compi hover:opacity-90 text-white px-9 py-4 rounded-full font-bold flex items-center gap-2 transition-all w-full md:w-auto justify-center text-sm shadow-md active:scale-95"
            >
              <span>🔍</span> Buscar viaje
            </button>
          </form>
        </div>

        <div className="w-full md:w-[45%] flex justify-end">
          <img
            src="/images/principal.png"
            alt="Amigos en coche"
            className="rounded-[3rem] shadow-2xl w-full h-auto object-cover max-w-[500px] lg:max-w-none"
          />
        </div>
      </div>
    </header>
  );
};

export default HeroCarpooling;