import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { buildApiUrl } from '../../apiConfig';

const Notificaciones: React.FC = () => {
  const navigate = useNavigate();
  const [reservas, setReservas] = useState<any[]>([]);
  const [avisos, setAvisos] = useState<any[]>([]); // <-- Nuevo estado para cancelaciones
  const [loading, setLoading] = useState(true);
  const token = localStorage.getItem('token');

  const fetchTodo = async () => {
    try {
      setLoading(true);
      // Lanzamos ambas peticiones en paralelo
      const [resReservas, resAvisos] = await Promise.all([
        fetch(buildApiUrl('/api/reservas/pendientes-conductor'), {
          headers: { Authorization: `Bearer ${token}` }
        }),
        fetch(buildApiUrl('/api/notificaciones/mis-notificaciones'), {
          headers: { Authorization: `Bearer ${token}` }
        })
      ]);

      if (resReservas.ok) setReservas(await resReservas.json());
      if (resAvisos.ok) setAvisos(await resAvisos.json());

    } catch (error) {
      console.error("Error cargando notificaciones", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchTodo(); }, []);

  const gestionarReserva = async (id: number, accion: 'confirmar' | 'rechazar') => {
    try {
      const endpoint = accion === 'confirmar' 
        ? `/api/reservas/confirmar?reservaId=${id}` 
        : `/api/reservas/cancelar?reservaId=${id}`;

      const response = await fetch(buildApiUrl(endpoint), {
        method: 'PUT',
        headers: { Authorization: `Bearer ${token}` }
      });
      
      if (response.ok) {
        setReservas(prev => prev.filter(r => r.id !== id));
        window.dispatchEvent(new Event('authChange'));
      }
    } catch (error) {
      alert("Error de conexión");
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="max-w-2xl mx-auto">
        <h1 className="text-3xl font-extrabold text-slate-900 mb-8">Bandeja de Entrada</h1>
        
        {loading ? (
          <p className="text-slate-500 animate-pulse">Cargando bandeja...</p>
        ) : (
          <div className="space-y-10">
            
            {/* SECCIÓN 1: SOLICITUDES DE RESERVA */}
            <section>
              <h2 className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-4">Solicitudes de reserva</h2>
              {reservas.length === 0 ? (
                <p className="text-sm text-slate-400 italic">No hay solicitudes pendientes.</p>
              ) : (
                <div className="space-y-4">
                  {reservas.map((reserva) => (
                    <div key={reserva.id} className="bg-white p-5 rounded-2xl shadow-sm border border-slate-200 flex flex-col gap-4">
                      <div className="flex justify-between items-start">
                        <div className="flex flex-col">
                          <div className="flex items-center gap-2">
                            <p className="font-bold text-lg text-slate-900">{reserva.persona.nombre} quiere viajar contigo</p>
                            <button onClick={() => navigate(`/usuarios/${reserva.persona.slug}/perfil`)} className="text-xs font-bold text-blue-600 hover:underline bg-blue-50 px-2 py-1 rounded">Ver perfil</button>
                          </div>
                          <p className="text-sm text-slate-600 italic">Reserva para {reserva.cantidadPlazas} plazas</p>
                        </div>
                        <span className="text-[10px] bg-amber-100 text-amber-700 px-2 py-1 rounded-full font-black">SOLICITUD</span>
                      </div>

                      <div className="flex gap-3">
                        <button 
                          onClick={() => gestionarReserva(reserva.id, 'confirmar')} 
                          className="flex-1 bg-emerald-600 text-white py-2.5 rounded-xl font-bold hover:bg-emerald-700 active:scale-95 transition-all shadow-sm shadow-emerald-200"
                        >
                          Aceptar
                        </button>
                        <button 
                          onClick={() => gestionarReserva(reserva.id, 'rechazar')} 
                          className="flex-1 bg-white border-2 border-red-100 text-red-600 py-2.5 rounded-xl font-bold hover:bg-red-50 hover:border-red-200 active:scale-95 transition-all"
                        >
                          Rechazar
                        </button>                      
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>

            {/* SECCIÓN 2: AVISOS (CANCELACIONES Y OTROS) */}
            <section>
              <h2 className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-4">Avisos y actividad</h2>
              {avisos.length === 0 ? (
                <p className="text-sm text-slate-400 italic">No hay actividad reciente.</p>
              ) : (
                <div className="space-y-3">
                  {avisos.map((aviso) => (
                    <div 
                      key={aviso.id} 
                      className={`p-4 rounded-2xl border flex items-center gap-4 ${
                        aviso.tipo === 'VIAJE_CANCELADO' || aviso.tipo === 'RESERVA_CANCELADA' 
                        ? 'bg-red-50 border-red-100' 
                        : 'bg-white border-slate-200'
                      }`}
                    >
                      <div className={`w-2 h-2 rounded-full ${aviso.leida ? 'bg-transparent' : 'bg-blue-500'}`} />
                      <div className="flex-1">
                        <p className={`text-sm ${aviso.tipo.includes('CANCELADA') ? 'text-red-900' : 'text-slate-700'}`}>
                          <span className="font-bold">
                            {aviso.tipo === 'VIAJE_CANCELADO' ? '⚠️ Viaje cancelado: ' : 'ℹ️ '}
                          </span>
                          {aviso.mensaje}
                        </p>
                        <p className="text-[10px] text-slate-400 mt-1 uppercase font-semibold">
                          {new Date(aviso.fechaCreacion).toLocaleDateString()}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>

          </div>
        )}
      </div>
    </div>
  );
};

export default Notificaciones;