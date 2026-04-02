import React from 'react';

interface PrivacyModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const PrivacyModal: React.FC<PrivacyModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center px-4 bg-slate-900/60 backdrop-blur-sm">
      <div className="bg-white w-full max-w-3xl max-h-[85vh] rounded-[2rem] shadow-2xl flex flex-col overflow-hidden border border-gray-100">
        
        {/* Cabecera */}
        <div className="px-8 py-6 border-b border-gray-100 flex justify-between items-center bg-white">
          <div>
            <h2 className="text-2xl font-extrabold text-slate-900">Política de Privacidad</h2>
            <p className="text-xs text-gray-400 mt-1 uppercase tracking-widest font-semibold">CompiCar · Protección de Datos</p>
          </div>
          <button 
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-full transition-colors text-gray-400 hover:text-slate-900 text-xl"
          >
            ✕
          </button>
        </div>

        {/* Contenido */}
        <div className="p-8 overflow-y-auto text-slate-700 leading-relaxed text-sm scrollbar-thin scrollbar-thumb-gray-200">
          <div className="space-y-8">
            
            <section>
              <h3 className="font-bold text-slate-900 text-base mb-2">1. Responsable del tratamiento</h3>
              <p>El responsable del tratamiento de los datos personales es el equipo desarrollador del proyecto <strong>TFG de Ingeniería del Software</strong>.</p>
            </section>

            <section>
              <h3 className="font-bold text-slate-900 text-base mb-2">2. Datos personales recogidos</h3>
              <ul className="list-disc pl-5 space-y-2">
                <li><strong>Obligatorios para registro:</strong> nombre, correo electrónico, contraseña.</li>
                <li><strong>Opcionales para el servicio:</strong> número de teléfono, foto de perfil, información del vehículo (modelo, matrícula, color).</li>
                <li><strong>Datos de pago:</strong> número de tarjeta bancaria, gestionado mediante <strong>Braintree con tokenización</strong>, evitando almacenamiento directo en la plataforma.</li>
                <li><strong>Prevención:</strong> en caso de expulsión, se almacenará de forma segura y hashada el correo y la matrícula.</li>
              </ul>
            </section>

            <section>
              <h3 className="font-bold text-slate-900 text-base mb-2">3. Finalidad del tratamiento</h3>
              <p>Los datos se usan exclusivamente para autenticar usuarios, gestionar reservas y pagos, facilitar la comunicación y cumplir obligaciones legales y fiscales.</p>
            </section>

            <section>
              <h3 className="font-bold text-slate-900 text-base mb-2">4. Base legal del tratamiento</h3>
              <p>Consentimiento explícito del usuario, necesidad contractual para el servicio y cumplimiento de obligaciones legales (RGPD).</p>
            </section>

            <section>
              <h3 className="font-bold text-slate-900 text-base mb-2">5. Principios de protección de datos</h3>
              <p className="mb-2">Garantizamos la minimización de datos y seguridad avanzada:</p>
              <ul className="list-disc pl-5 space-y-1 text-gray-600 italic">
                <li>Comunicación cifrada con <strong>HTTPS/TLS 1.2</strong> o superior.</li>
                <li>Contraseñas almacenadas con <strong>bcrypt</strong>.</li>
                <li>Control de acceso mediante <strong>JWT</strong> y roles.</li>
              </ul>
            </section>

            <section>
              <h3 className="font-bold text-slate-900 text-base mb-2">6. Derechos del usuario</h3>
              <p>El usuario puede acceder, rectificar, solicitar la supresión ("derecho al olvido"), limitar u oponerse al tratamiento enviando una solicitud a <strong>contacto@proyectotfg.example.com</strong>.</p>
            </section>

            <section>
              <h3 className="font-bold text-slate-900 text-base mb-2">9. Protección de datos de pago</h3>
              <p>Todos los pagos se gestionan mediante <strong>Braintree</strong>, cumpliendo <strong>PCI DSS Nivel 1</strong>. Los datos de tarjeta nunca se almacenan en nuestros servidores.</p>
            </section>

            <section>
              <h3 className="font-bold text-slate-900 text-base mb-2">10. Conservación y eliminación</h3>
              <p>Los datos se borran al eliminar la cuenta, salvo los hashes necesarios para prevención de reincidencias durante un máximo de <strong>3 años</strong>.</p>
            </section>

            <section>
              <h3 className="font-bold text-slate-900 text-base mb-2">11. Consentimiento y aceptación</h3>
              <p>Al registrarse, el usuario acepta explícitamente esta política de privacidad. Última actualización: Marzo 2026.</p>
            </section>

            <section>
              <h3 className="font-bold text-slate-900 text-base mb-2">12. Contacto</h3>
              <p>Para dudas sobre privacidad y protección de datos: <strong>contacto@proyectotfg.example.com</strong></p>
            </section>

          </div>
        </div>

        {/* Footer del Modal */}
        <div className="px-8 py-6 border-t border-gray-100 flex justify-end bg-gray-50/50">
          <button 
            onClick={onClose}
            className="bg-slate-950 text-white px-10 py-3 rounded-full font-bold hover:bg-slate-800 transition-all shadow-lg active:scale-95"
          >
            Cerrar y volver
          </button>
        </div>
      </div>
    </div>
  );
};

export default PrivacyModal;