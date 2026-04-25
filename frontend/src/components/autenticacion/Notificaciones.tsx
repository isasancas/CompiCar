import React, { useEffect, useState } from 'react';
import { buildApiUrl } from '../../apiConfig';

const Notificaciones: React.FC = () => {
  const [reservas, setReservas] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const token = localStorage.getItem('token');

  const fetchPendientes = async () => {
    try {
      // Necesitarás crear este endpoint que filtre reservas de TUS viajes
      const response = await fetch(buildApiUrl('/api/reservas/pendientes-conductor'), {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (response.ok) {
        const data = await response.json();
        setReservas(data);
      }
    } catch (error) {
      console.error("Error cargando notificaciones", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchPendientes(); }, []);

  const gestionarReserva = async (id: number, accion: 'confirmar' | 'rechazar') => {
    try {
      const endpoint = accion === 'confirmar' 
        ? `/api/reservas/confirmar?reservaId=${id}` 
        : `/api/reservas/cancelar?reservaId=${id}`; // Asegúrate de que en el back se llame así

      const response = await fetch(buildApiUrl(endpoint), {
        method: 'PUT',
        headers: { Authorization: `Bearer ${token}` }
      });
      
      if (response.ok) {
        // 1. Quitamos la reserva de la lista visual
        setReservas(prev => prev.filter(r => r.id !== id));
        
        // 2. LANZAMOS EL EVENTO AQUÍ (Dentro de la lógica, no en el HTML)
        window.dispatchEvent(new Event('authChange'));
      } else {
        const errorData = await response.json().catch(() => ({}));
        alert(`Error: ${errorData.message || 'No se pudo procesar'}`);
      }
    } catch (error) {
      alert("Error de conexión al procesar la solicitud");
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="max-w-2xl mx-auto">
        <h1 className="text-2xl font-bold text-slate-800 mb-6">Bandeja de Entrada</h1>
        
        {loading ? (
          <p>Cargando solicitudes...</p>
        ) : reservas.length === 0 ? (
          <div className="bg-white p-10 rounded-2xl text-center shadow-sm border border-slate-200">
            <p className="text-slate-500">No tienes solicitudes pendientes por ahora.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {reservas.map((reserva) => (
              <div key={reserva.id} className="bg-white p-5 rounded-2xl shadow-sm border border-slate-200 flex flex-col gap-4">
                <div className="flex justify-between items-start">
                  <div>
                    <p className="font-bold text-lg text-slate-900">
                      {reserva.persona.nombre} quiere unirse a tu viaje
                    </p>
                    <p className="text-sm text-slate-600">
                      Trayecto: {reserva.viaje.paradas[0].localizacion} → {reserva.viaje.paradas[reserva.viaje.paradas.length - 1].localizacion}
                    </p>
                    <p className="text-sm font-semibold text-blue-600">
                      Plazas solicitadas: {reserva.cantidadPlazas}
                    </p>
                  </div>
                  <span className="text-xs bg-amber-100 text-amber-700 px-2 py-1 rounded-lg font-bold">PENDIENTE</span>
                </div>

                <div className="flex gap-3">
                  <button 
                    onClick={() => gestionarReserva(reserva.id, 'confirmar')}
                    className="flex-1 bg-green-600 text-white py-2 rounded-xl font-bold hover:bg-green-700 transition-colors"
                  >
                    Aceptar
                  </button>
                  <button 
                    onClick={() => gestionarReserva(reserva.id, 'rechazar')}
                    className="flex-1 bg-white border border-red-200 text-red-600 py-2 rounded-xl font-bold hover:bg-red-50 transition-colors"
                  >
                    Rechazar
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Notificaciones;