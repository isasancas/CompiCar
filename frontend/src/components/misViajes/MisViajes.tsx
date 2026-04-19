import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { buildApiUrl } from '../../apiConfig';

interface Viaje {
  id: number;
  slug: string;
  fechaHoraSalida: string;
  estado: string;
  plazasDisponibles: number;
  precio: number;
  vehiculo: {
    marca: string;
    modelo: string;
    matricula: string;
  };
  paradas: Array<{
    localizacion: string;
    tipo: string;
    orden: number;
  }>;
}

const MisViajes: React.FC = () => {
  const navigate = useNavigate();
  const [viajesOfrecidos, setViajesOfrecidos] = useState<Viaje[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const getValidToken = () => {
    const token = localStorage.getItem('token');
    if (!token || token === 'undefined' || token === 'null' || token.trim() === '') {
      return null;
    }
    return token;
  };

  const clearLocalSession = useCallback((redirectTo: string) => {
    localStorage.removeItem('token');
    window.dispatchEvent(new Event('authChange'));
    navigate(redirectTo, { replace: true });
  }, [navigate]);

  const fetchViajes = useCallback(async () => {
    const token = getValidToken();
    if (!token) {
      clearLocalSession('/inicio-sesion');
      return;
    }

    try {
      const response = await fetch(buildApiUrl('/api/viajes/mis-viajes'), {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
      });

      if (response.ok) {
        const ofrecidos = await response.json();
        setViajesOfrecidos(ofrecidos);
      } else if (response.status === 401 || response.status === 403) {
        clearLocalSession('/inicio-sesion');
        return;
      } else {
        setError('Error al cargar los viajes');
      }
    } catch {
      setError('Error de conexión');
    } finally {
      setLoading(false);
    }
  }, [clearLocalSession]);

  useEffect(() => {
    fetchViajes();
  }, [fetchViajes]);

  const formatFecha = (fecha: string) => {
    return new Date(fecha).toLocaleString('es-ES', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getOrigenDestino = (paradas: Viaje['paradas']) => {
    const origen = paradas.find(p => p.tipo === 'ORIGEN')?.localizacion || 'Desconocido';
    const destino = paradas.find(p => p.tipo === 'DESTINO')?.localizacion || 'Desconocido';
    return { origen, destino };
  };

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center">Cargando...</div>;
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
        <div className="bg-white p-6 rounded-lg shadow-md max-w-md w-full text-center">
          <p className="text-red-500 mb-4">{error}</p>
          <button
            type="button"
            className="bg-gradient-compi hover:opacity-90 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline"
            onClick={() => clearLocalSession('/inicio-sesion')}
          >
            Ir a iniciar sesión
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-200 pb-10 pt-6">
      <div className="mx-auto max-w-6xl px-4">
        <button
          type="button"
          onClick={() => navigate('/perfil')}
          className="rounded-full border border-green-600 px-4 py-1 text-sm text-green-700 transition hover:bg-green-50"
        >
          Volver al perfil
        </button>

        <div className="mt-6 space-y-8">
          <div className="rounded-3xl border border-slate-300 bg-white p-6 shadow-sm">
            <h1 className="text-3xl font-bold text-slate-900 mb-6">Mis viajes</h1>
            {viajesOfrecidos.length === 0 ? (
              <p className="text-slate-600">No hay viajes.</p>
            ) : (
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                {viajesOfrecidos.map((viaje) => {
                  const { origen, destino } = getOrigenDestino(viaje.paradas);
                  return (
                    <div key={viaje.id} className="rounded-2xl border border-slate-300 bg-gray-50 p-4 shadow-sm">
                      <div className="flex justify-between items-start mb-2">
                        <h3 className="font-semibold text-slate-900">
                          {viaje.vehiculo.marca} {viaje.vehiculo.modelo}
                        </h3>
                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                          viaje.estado === 'ACTIVO' ? 'bg-green-100 text-green-800' :
                          viaje.estado === 'COMPLETADO' ? 'bg-blue-100 text-blue-800' :
                          'bg-gray-100 text-gray-800'
                        }`}>
                          {viaje.estado}
                        </span>
                      </div>
                      <p className="text-sm text-slate-600 mb-1">
                        {origen} → {destino}
                      </p>
                      <p className="text-sm text-slate-600 mb-1">
                        {formatFecha(viaje.fechaHoraSalida)}
                      </p>
                      <p className="text-sm text-slate-600 mb-1">
                        Plazas disponibles: {viaje.plazasDisponibles}
                      </p>
                      <p className="text-sm font-medium text-slate-900">
                        {viaje.precio}€
                      </p>
                        <button
                          type="button"
                          onClick={() => navigate('/viajes/' + viaje.slug)}
                          className="mt-3 rounded-lg bg-slate-900 px-3 py-2 text-sm text-white hover:bg-slate-700"
                        >
                          Ver detalle
                        </button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default MisViajes;