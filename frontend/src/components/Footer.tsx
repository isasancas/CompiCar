import React, { useState } from 'react';
import PrivacyModal from './PrivacyModal';
import TermsModal from './TermsModal'; // Importamos el nuevo modal

const Footer: React.FC = () => {
  const [isPrivacyOpen, setIsPrivacyOpen] = useState(false);
  const [isTermsOpen, setIsTermsOpen] = useState(false);

  return (
    <footer id="contacto" className="bg-slate-950 text-white py-16 px-6 md:px-12 border-t border-slate-800">
      <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-4 gap-12">
        
        {/* Columna 1: Logo */}
        <div className="space-y-6">
          <div className="flex items-center gap-3">
            <img src="/images/LogoCompleto.png" alt="Icono CompiCar" className="h-10 w-auto" />
          </div>
          <p className="text-slate-400 text-sm leading-relaxed">
            La plataforma líder para compartir coche en tus trayectos diarios. 
            Ahorra dinero, conecta con vecinos y cuida el planeta.
          </p>
        </div>

        {/* Columna 2: Plataforma */}
        <div>
          <h4 className="font-bold mb-6 text-base text-white">Plataforma</h4>
          <ul className="space-y-4 text-slate-400 text-sm">
            <li className="hover:text-[#00BF63] cursor-pointer transition-colors">Buscar viaje</li>
            <li className="hover:text-[#00BF63] cursor-pointer transition-colors">Publicar viaje</li>
          </ul>
        </div>

        {/* Columna 3: Legal Modificada */}
        <div>
          <h4 className="font-bold mb-6 text-base text-white">Legal</h4>
          <ul className="space-y-4 text-slate-400 text-sm">
            <li>
              <button onClick={() => setIsPrivacyOpen(true)} className="hover:text-[#00BF63] transition-colors">
                Política de Privacidad
              </button>
            </li>
            <li>
              <button onClick={() => setIsTermsOpen(true)} className="hover:text-[#00BF63] transition-colors">
                Términos y Condiciones
              </button>
            </li>
          </ul>
        </div>

        {/* Columna 4: Contacto */}
        <div>
          <h4 className="font-bold mb-6 text-base text-white">Contacto</h4>
          <p className="text-slate-400 text-sm">📧 contacto@proyectotfg.example.com</p>
        </div>
      </div>

      <div className="max-w-7xl mx-auto border-t border-slate-800 mt-16 pt-8 text-center text-slate-500 text-xs">
        <p>© 2026 CompiCar. Proyecto TFG Ingeniería del Software.</p>
      </div>

      {/* Modales */}
      <PrivacyModal isOpen={isPrivacyOpen} onClose={() => setIsPrivacyOpen(false)} />
      <TermsModal isOpen={isTermsOpen} onClose={() => setIsTermsOpen(false)} />
    </footer>
  );
};

export default Footer;