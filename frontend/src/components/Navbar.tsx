import React from 'react';

const Navbar: React.FC = () => {
  return (
    // ESCALADO: Aumentamos el padding vertical (py-6) para un navbar más alto
    <nav className="flex items-center justify-between px-10 md:px-16 py-6 bg-white border-b border-gray-100 sticky top-0 z-50 shadow-sm">
      
      {/* LOGO: Aumentamos la altura de h-9 a h-12 para que se vea mucho mejor */}
      <div className="flex items-center">
        <img src="images/LogoCompleto.png" alt="CompiCar" className="h-12 w-auto object-contain cursor-pointer" />
      </div>

      {/* ENLACES: Texto un poco más grande (text-base) y más espaciado */}
      <ul className="hidden md:flex gap-10 text-base font-semibold text-slate-700">
        <li><a href="#como-funciona" className="hover:text-[#1E50D6] transition-colors">Cómo funciona</a></li>
        <li><a href="#publicar" className="hover:text-[#1E50D6] transition-colors">Publicar un viaje</a></li>
        {/* El botón de contacto ahora apunta al footer */}
        <li><a href="#contacto" className="hover:text-[#1E50D6] transition-colors">Contacto</a></li>
      </ul>

      {/* DERECHA: Botones más grandes y con más peso visual */}
      <div className="flex gap-6 items-center">
        <span className="text-gray-500 hover:text-gray-900 cursor-pointer text-xl">🔍</span>
        <button className="text-base font-bold text-gray-600 hover:text-gray-900 transition-colors">
          Iniciar sesión
        </button>
        {/* BOTÓN REGISTRO: Más grande (px-8 py-3) y texto base */}
        <button className="bg-gradient-compi hover:opacity-90 text-white px-8 py-3 rounded-full text-base font-bold transition-all shadow-lg active:scale-95">
          Registrarse gratis
        </button>
      </div>
    </nav>
  );
};

export default Navbar;