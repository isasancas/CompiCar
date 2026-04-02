import React from 'react';

const steps = [
  {
    icon: '👤', // Icono Perfil
    title: 'Crea tu perfil',
    description: 'Regístrate en segundos, indica tu ruta habitual y horario de trabajo. Verificamos tu identidad para mayor confianza.',
    color: '#00BF63' // Verde
  },
  {
    icon: '🔍', // Icono Lupa
    title: 'Encuentra tu compañero',
    description: 'Buscamos personas con trayectos similares al tuyo. Filtra por horario, punto de encuentro y valoraciones.',
    color: '#00BF63' // Verde
  },
  {
    icon: '🚗', // Icono Coche
    title: '¡Comparte y ahorra!',
    description: 'Coordina el viaje, divide el coste del combustible y viaja más cómodo. Todo desde la app.',
    color: '#00BF63' // Verde
  }
];

const HowItWorks: React.FC = () => {
  return (
    <section id="como-funciona" className="bg-slate-50 px-8 md:px-20 py-24 border-t border-gray-100">
      <div className="max-w-6xl mx-auto text-center">
        
        {/* Cabecera de Sección */}
        <div className="flex justify-center mb-6">
          <div className="flex items-center gap-2 bg-green-50 w-fit px-4 py-1.5 rounded-full border border-green-100">
            <span className="text-[11px] font-bold text-[#00BF63] uppercase tracking-wider">
              Cómo funciona
            </span>
          </div>
        </div>

        <h2 className="text-4xl md:text-5xl font-extrabold text-slate-950 leading-tight mb-8">
          Tres pasos para empezar
        </h2>

        <p className="text-gray-600 text-lg mb-20 max-w-2xl mx-auto leading-relaxed">
          Empezar a compartir vehículo nunca fue tan sencillo. En menos de 5 minutos ya puedes encontrar tu compañero de trayecto.
        </p>

        {/* Pasos con Línea Conectora */}
        <div className="relative grid grid-cols-1 md:grid-cols-3 gap-12 md:gap-8">
          
          {/* La Línea Conectora (solo visible en escritorio) */}
          <div className="hidden md:block absolute top-12 left-0 right-0 h-0.5 bg-gray-200 z-0">
            {/* Parte coloreada de la línea */}
            <div className="absolute top-0 left-[16.66%] right-[16.66%] h-full bg-blue-100"></div>
          </div>

          {steps.map((step, index) => (
            <div key={index} className="relative z-10 flex flex-col items-center">
              
              {/* Icono Circular */}
              <div className="w-24 h-24 rounded-full border-2 border-[#00BF63] bg-white flex items-center justify-center text-5xl mb-8 shadow-inner">
                {step.icon}
              </div>

              {/* Texto */}
              <h3 className="text-2xl font-bold text-slate-950 mb-4">
                {step.title}
              </h3>
              <p className="text-gray-600 text-sm leading-relaxed px-4">
                {step.description}
              </p>
            </div>
          ))}
        </div>

      </div>
    </section>
  );
};

export default HowItWorks;