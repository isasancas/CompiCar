import React, { useState } from 'react';

interface FaqModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const faqData = [
  {
    category: '🚗 Generales y Cuenta',
    items: [
      {
        q: '¿Qué es CompiCar?',
        a: 'CompiCar es una plataforma web para la gestión de viajes compartidos orientada a trayectos diarios, permitiendo a los usuarios ahorrar dinero, conectar con otras personas y cuidar el medio ambiente.'
      },
      {
        q: '¿Es necesario registrarse para usar la plataforma?',
        a: 'No es estrictamente necesario para explorar los viajes disponibles. Sin embargo, para ofrecer o solicitar un viaje sí es obligatorio estar registrado e iniciar sesión.'
      },
      {
        q: '¿Cómo puedo darme de baja o eliminar mis datos?',
        a: 'Puedes solicitar la supresión de tus datos enviando un correo a compicarsa@gmail.com. Se eliminará la cuenta respetando los plazos legales de prevención.'
      }
    ]
  },
  {
    category: '💳 Pagos y Seguridad',
    items: [
      {
        q: '¿Cómo funcionan los pagos al reservar un viaje?',
        a: 'Al realizar la reserva, el sistema retiene la transacción de forma segura mediante Stripe. El dinero solo se libera al conductor una vez que el viaje se realiza con éxito.'
      },
      {
        q: '¿Están seguros los datos de mi tarjeta?',
        a: 'Totalmente. Todos los pagos son procesados externamente por Stripe (PCI DSS Nivel 1) con tokenización, por lo que nunca almacenamos tus datos bancarios.'
      },
      {
        q: '¿Cómo se calcula el precio de los trayectos?',
        a: 'Contamos con un sistema inteligente que calcula el coste estimado en base a la distancia, las características del vehículo y el precio actual del combustible.'
      }
    ]
  },
  {
    category: '📍 Pasajeros',
    items: [
      {
        q: '¿Qué es el código de check-in y para qué sirve?',
        a: 'Es un código de 6 dígitos que recibes tras reservar. Debes dárselo al conductor al subir al coche para confirmar tu asistencia y validar tu plaza.'
      },
      {
        q: '¿Qué ocurre si el conductor no se presenta?',
        a: 'Si pasa la hora de salida y el conductor no acude, podrás reportarlo desde la app. El viaje se cancelará y se te reembolsará el 100% del dinero.'
      }
    ]
  },
  {
    category: '🚘 Conductores',
    items: [
      {
        q: '¿Puedo ofrecer viajes recurrentes para ir a trabajar?',
        a: 'Sí, puedes configurar trayectos recurrentes definiendo un rango de fechas y los días de la semana que se repiten.'
      },
      {
        q: '¿Cómo y cuándo cobro mis viajes?',
        a: 'Cuando los pasajeros introducen el código de check-in y finalizas el trayecto, el saldo se libera en tu cuenta de Stripe para que lo transfieras a tu banco.'
      }
    ]
  }
];

const FaqModal: React.FC<FaqModalProps> = ({ isOpen, onClose }) => {
  const [searchTerm, setSearchTerm] = useState('');

  if (!isOpen) return null;

  // Opción 1: Filtrado de preguntas y respuestas en tiempo real
  const filteredFaqs = faqData.map(cat => ({
    ...cat,
    items: cat.items.filter(
      item =>
        item.q.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.a.toLowerCase().includes(searchTerm.toLowerCase())
    )
  })).filter(cat => cat.items.length > 0);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
      <div className="relative w-full max-w-2xl max-h-[85vh] bg-slate-900 border border-slate-800 text-white rounded-xl shadow-2xl flex flex-col overflow-hidden">
        
        {/* Cabecera del Modal + Buscador (Opción 1) */}
        <div className="p-6 border-b border-slate-800 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-xl font-bold text-white">Preguntas Frecuentes (FAQ)</h3>
            <button
              onClick={onClose}
              className="text-slate-400 hover:text-white transition-colors text-2xl font-bold w-8 h-8 flex items-center justify-center rounded-lg"
            >
              &times;
            </button>
          </div>

          {/* Campo de búsqueda */}
          <div className="relative">
            <input
              type="text"
              placeholder="Buscar por palabra clave (ej: pago, cancelar, check-in)..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full px-4 py-2.5 bg-slate-950 border border-slate-800 rounded-lg text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-[#00BF63] transition-colors"
            />
            {searchTerm && (
              <button
                onClick={() => setSearchTerm('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-500 hover:text-slate-300"
              >
                Limpiar
              </button>
            )}
          </div>
        </div>

        {/* Contenido desplegable con scroll */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1">
          {filteredFaqs.length > 0 ? (
            filteredFaqs.map((cat, idx) => (
              <div key={idx} className="space-y-3">
                <h4 className="text-base font-semibold text-[#00BF63] border-b border-slate-800 pb-1">
                  {cat.category}
                </h4>
                <div className="space-y-2">
                  {cat.items.map((item, itemIdx) => (
                    <details
                      key={itemIdx}
                      className="group border border-slate-800 rounded-lg bg-slate-950/60 transition-all duration-200"
                    >
                      <summary className="flex items-center justify-between p-4 cursor-pointer font-medium text-slate-200 hover:text-white select-none">
                        <span>{item.q}</span>
                        <span className="text-slate-500 transition-transform duration-300 group-open:rotate-180 group-open:text-[#00BF63]">
                          ▼
                        </span>
                      </summary>
                      <div className="px-4 pb-4 pt-1 text-slate-400 text-sm border-t border-slate-800/50 leading-relaxed">
                        {item.a}
                      </div>
                    </details>
                  ))}
                </div>
              </div>
            ))
          ) : (
            <p className="text-center text-slate-500 py-8 text-sm">
              No se encontraron preguntas relacionadas con "{searchTerm}".
            </p>
          )}
        </div>

        {/* Pie del Modal con soporte directo (Opción 3) */}
        <div className="p-4 border-t border-slate-800 bg-slate-950/50 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-400">
          <span>
            ¿No encuentras respuesta a tu duda? Escríbenos a{' '}
            <a href="mailto:compicarsa@gmail.com" className="text-[#00BF63] hover:underline font-medium">
              compicarsa@gmail.com
            </a>
          </span>
          <button
            onClick={onClose}
            className="px-5 py-2 bg-slate-800 hover:bg-slate-700 text-white text-sm font-medium rounded-lg transition-colors w-full sm:w-auto"
          >
            Cerrar
          </button>
        </div>

      </div>
    </div>
  );
};

export default FaqModal;