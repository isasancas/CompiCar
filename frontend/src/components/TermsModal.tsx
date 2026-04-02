import React from 'react';

interface TermsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const TermsModal: React.FC<TermsModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center px-4 bg-slate-900/60 backdrop-blur-sm">
      <div className="bg-white w-full max-w-3xl max-h-[85vh] rounded-[2rem] shadow-2xl flex flex-col overflow-hidden border border-gray-100">
        
        {/* Cabecera */}
        <div className="px-8 py-6 border-b border-gray-100 flex justify-between items-center bg-white">
          <div>
            <h2 className="text-2xl font-extrabold text-slate-900">Términos y Condiciones</h2>
            <p className="text-xs text-gray-400 mt-1 uppercase tracking-widest font-semibold">CompiCar · Acuerdo de Uso</p>
          </div>
          <button 
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-full transition-colors text-gray-400 hover:text-slate-900 text-xl"
          >
            ✕
          </button>
        </div>

        {/* Contenido */}
        <div className="p-8 overflow-y-auto text-slate-700 leading-relaxed text-sm">
          <div className="space-y-8">
            
            <section>
              <h3 className="font-bold text-slate-900 text-base mb-2">1. Naturaleza del Servicio</h3>
              <p>CompiCar es una plataforma de <strong>intermediación</strong> que conecta a conductores y pasajeros particulares para compartir los gastos de un trayecto. No es un servicio de transporte público ni de taxi.</p>
            </section>

            <section>
              <h3 className="font-bold text-slate-900 text-base mb-2">2. Requisitos del Conductor</h3>
              <ul className="list-disc pl-5 space-y-2">
                <li>Poseer un permiso de conducir válido y en vigor.</li>
                <li>Tener el vehículo con la <strong>ITV</strong> y el <strong>seguro obligatorio</strong> al día.</li>
                <li>No obtener beneficio económico; el importe recibido debe limitarse a compartir gastos de combustible y mantenimiento.</li>
              </ul>
            </section>

            <section>
              <h3 className="font-bold text-slate-900 text-base mb-2">3. Reservas y Pagos</h3>
              <p>Los pagos se realizan a través de la plataforma mediante <strong>Braintree</strong>. El importe se retiene en el momento de la reserva y se transfiere al conductor una vez finalizado el viaje sin incidencias.</p>
            </section>

            <section>
              <h3 className="font-bold text-slate-900 text-base mb-2">4. Política de Cancelación</h3>
              <ul className="list-disc pl-5 space-y-2">
                <li><strong>Pasajero:</strong> Devolución del 100% si se cancela con más de 24h de antelación. 50% si es entre 24h y 2h. Sin devolución si es menos de 2h.</li>
                <li><strong>Conductor:</strong> La cancelación injustificada puede conllevar penalizaciones en la valoración y suspensión de la cuenta.</li>
              </ul>
            </section>

            <section>
              <h3 className="font-bold text-slate-900 text-base mb-2">5. Normas de Comportamiento</h3>
              <p>Se exige puntualidad, respeto y cumplimiento de las normas de tráfico. Queda prohibido el transporte de sustancias ilegales o el comportamiento inadecuado que ponga en riesgo la seguridad.</p>
            </section>

            <section>
              <h3 className="font-bold text-slate-900 text-base mb-2">6. Responsabilidad</h3>
              <p>CompiCar no se hace responsable de los incidentes ocurridos durante el trayecto, siendo responsabilidad del seguro del vehículo y de los usuarios implicados.</p>
            </section>

          </div>
        </div>

        {/* Footer del Modal */}
        <div className="px-8 py-6 border-t border-gray-100 flex justify-end bg-gray-50/50">
          <button 
            onClick={onClose}
            className="bg-slate-950 text-white px-10 py-3 rounded-full font-bold hover:bg-slate-800 transition-all shadow-lg active:scale-95"
          >
            Aceptar
          </button>
        </div>
      </div>
    </div>
  );
};

export default TermsModal;