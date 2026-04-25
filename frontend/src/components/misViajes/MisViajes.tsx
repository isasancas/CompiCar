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

interface ViajeConRol extends Viaje {
  rol: 'conductor' | 'pasajero';
}

type EstadoFiltro = 'PENDIENTE' | 'FINALIZADO';

const MisViajes: React.FC = () => {
  const navigate = useNavigate();
  const [todosLosViajes, setTodosLosViajes] = useState<ViajeConRol[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filtroEstado, setFiltroEstado] = useState<EstadoFiltro>('PENDIENTE');

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
      // Obtener viajes como conductor
      const resConductor = await fetch(buildApiUrl('/api/viajes/mis-viajes'), {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
      });

      // Obtener viajes como pasajero
      const resPasajero = await fetch(buildApiUrl('/api/viajes/participados'), {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
      });

      if (!resConductor.ok || !resPasajero.ok) {
        if (resConductor.status === 401 || resPasajero.status === 401) {
          clearLocalSession('/inicio-sesion');
          return;
        }
        setError('Error al cargar los viajes');
        return;
      }

      const viajesConductor: Viaje[] = await resConductor.json();
      const viajesPasajero: Viaje[] = await resPasajero.json();

      // Combinar y añadir rol
      const combinados: ViajeConRol[] = [
        ...viajesConductor.map(v => ({ ...v, rol: 'conductor' as const })),
        ...viajesPasajero.map(v => ({ ...v, rol: 'pasajero' as const }))
      ];

      // Ordenar por fecha descendente
      combinados.sort((a, b) => 
        new Date(b.fechaHoraSalida).getTime() - new Date(a.fechaHoraSalida).getTime()
      );

      setTodosLosViajes(combinados);
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
    const paradasIntermedias = paradas.filter(p => p.tipo === 'INTERMEDIA').sort((a, b) => a.orden - b.orden).map(p => p.localizacion);
    return { origen, destino, paradasIntermedias };
  };

  const viajesFiltrados = todosLosViajes.filter(v => {
    if (filtroEstado === 'PENDIENTE') {
      return v.estado === 'PENDIENTE' || v.estado === 'ACTIVO';
    } else {
      return v.estado === 'FINALIZADO' || v.estado === 'COMPLETADO';
    }
  });

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

            {/* Tabs de filtro */}
            <div className="flex gap-6 mb-6 border-b border-slate-200 pb-2">
              <button
                onClick={() => setFiltroEstado('PENDIENTE')}
                className={`font-medium text-sm transition ${
                  filtroEstado === 'PENDIENTE'
                    ? 'text-green-600'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                Pendientes
              </button>
              <button
                onClick={() => setFiltroEstado('FINALIZADO')}
                className={`font-medium text-sm transition ${
                  filtroEstado === 'FINALIZADO'
                    ? 'text-green-600'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                Finalizados
              </button>
            </div>

            {viajesFiltrados.length === 0 ? (
              <p className="text-slate-600">No hay viajes.</p>
            ) : (
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                {viajesFiltrados.map((viaje) => {
                  const { origen, destino, paradasIntermedias } = getOrigenDestino(viaje.paradas);
                  return (
                    <div key={`${viaje.id}-${viaje.rol}`} className="rounded-2xl border border-slate-300 bg-gray-50 p-4 shadow-sm">
                      <div className="flex justify-between items-start mb-2">
                        <h3 className="font-semibold text-slate-900">
                          {viaje.vehiculo.marca} {viaje.vehiculo.modelo}
                        </h3>
                        <div className="flex flex-col items-end space-y-1">
                          <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                            viaje.estado === 'ACTIVO' ? 'bg-green-100 text-green-800' :
                            viaje.estado === 'COMPLETADO' ? 'bg-blue-100 text-blue-800' :
                            'bg-gray-100 text-gray-800'
                          }`}>
                            {viaje.estado}
                          </span>
                          <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                            viaje.rol === 'conductor'
                              ? 'bg-blue-100 text-blue-800'
                              : 'bg-purple-100 text-purple-800'
                          }`}>
                            {viaje.rol === 'conductor' ? 'Conductor' : 'Pasajero'}
                          </span>
                        </div>
                      </div>
                      <p className="text-sm text-slate-600 mb-1">
                        <span className="font-medium text-green-700">Origen:</span> {origen}
                      </p>
                      <p className="text-sm text-slate-600 mb-1">
                        <span className="font-medium text-red-700">Destino:</span> {destino}
                      </p>
                      {paradasIntermedias.length > 0 && (
                        <div className="text-sm text-slate-600 mb-1">
                          <span className="font-medium text-orange-600">Paradas:</span>
                          <div className="mt-1 flex flex-wrap gap-2">
                            {paradasIntermedias.map((p, i) => (
                              <span key={i} className="inline-block bg-orange-50 px-2 py-1 rounded text-xs border border-orange-200">
                                {i + 1}. {p}
                              </span>
                            ))}
                          </div>
                        </div>
                      )}
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
                        onClick={() =>
                          navigate('/viajes/' + viaje.slug, {
                            state: {
                              backTo: '/mis-viajes',
                              backLabel: 'Volver a Mis Viajes',
                              rol: viaje.rol
                            }
                          })
                        }
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