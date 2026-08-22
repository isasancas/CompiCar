import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { buildApiUrl } from '../../apiConfig';

interface Viaje {
  id: number;
  slug: string;
  fechaHoraSalida: string;
  estado: 'INICIADO' | 'PENDIENTE' | 'EN_CURSO' | 'FINALIZADO' | 'CANCELADO' | string;
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
  diasSemana?: string[]; // Para viajes recurrentes
  viajesRecurrentes?: Viaje[]; // Para viajes recurrentes
  fechaFinRecurrencia?: string; // Para viajes recurrentes
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

  const clearLocalSession = useCallback((redirectTo: string) => {
    localStorage.removeItem('token');
    window.dispatchEvent(new Event('authChange'));
    navigate(redirectTo, { replace: true });
  }, [navigate]);

  const fetchViajes = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem('token'); 
      const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}` 
      };

      const [resConductor, resPasajero] = await Promise.all([
        fetch(buildApiUrl('/api/viajes/mis-viajes'), { headers }),
        fetch(buildApiUrl('/api/viajes/participados'), { headers })
      ]);

      if (!resConductor.ok || !resPasajero.ok) {
        throw new Error('Hubo un problema al cargar los viajes.');
      }

      const [datosConductor, datosPasajero] = await Promise.all([
        resConductor.json(),
        resPasajero.json()
      ]);

      const conductorConRol: ViajeConRol[] = datosConductor.map((v: Viaje) => ({ ...v, rol: 'conductor' }));
      const pasajeroConRol: ViajeConRol[] = datosPasajero.map((v: Viaje) => ({ ...v, rol: 'pasajero' }));

      setTodosLosViajes([...conductorConRol, ...pasajeroConRol]);

    } catch (err) {
      console.error("Error al cargar los viajes:", err);
      setError(err instanceof Error ? err.message : 'Error desconocido');
    } finally {
      setLoading(false);
    }
  }, []);

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

  const getEstadoBadgeStyle = (estado: string) => {
    switch (estado?.toUpperCase()) {
      case 'PENDIENTE':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'INICIADO':
      case 'EN_CURSO':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'FINALIZADO':
        return 'bg-gray-100 text-gray-800 border-gray-200';
      case 'CANCELADO':
        return 'bg-red-100 text-red-800 border-red-200';
      default:
        return 'bg-slate-100 text-slate-800 border-slate-200';
    }
  };

  const esViajeRecurrentePadre = (viaje: Viaje): boolean => {
    const tieneDias = viaje.diasSemana && viaje.diasSemana.length > 0;
    const tieneInstancias = viaje.viajesRecurrentes && viaje.viajesRecurrentes.length > 0;
    return Boolean(tieneDias || tieneInstancias);
  };

  // 🛠️ FILTRADO CORREGIDO PARA VIAJES RECURRENTES
  const viajesFiltrados = todosLosViajes.filter(v => {
    const estado = v.estado?.toUpperCase();
    const esCancelado = estado === 'CANCELADO';
    
    // Verificamos si es un viaje recurrente y si su fecha de fin aún es posterior al momento actual
    const esRecurrente = esViajeRecurrentePadre(v);
    const tieneRecurrenciaVigente = esRecurrente && v.fechaFinRecurrencia 
      ? new Date(v.fechaFinRecurrencia).getTime() > new Date().getTime() 
      : false;

    // Un viaje se considera realmente finalizado si su estado es cancelado o 
    // (siendo recurrente, ya pasó su fecha fin) o (siendo normal, está finalizado).
    const esEfectivamenteFinalizado = esCancelado || (esRecurrente ? !tieneRecurrenciaVigente && estado === 'FINALIZADO' : estado === 'FINALIZADO');

    if (filtroEstado === 'FINALIZADO') {
      return esEfectivamenteFinalizado;
    }

    // Para la pestaña 'PENDIENTE (Activos)': 
    // Mostramos el viaje si NO está cancelado y (está pendiente/en curso o tiene una recurrencia vigente aunque el padre marque finalizado)
    if (esCancelado) return false;
    
    return estado !== 'FINALIZADO' || tieneRecurrenciaVigente;
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
                type="button"
                onClick={() => setFiltroEstado('PENDIENTE')}
                className={`font-medium text-sm transition ${
                  filtroEstado === 'PENDIENTE'
                    ? 'text-green-600 font-bold border-b-2 border-green-600 pb-2 -mb-[9px]'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                Pendientes (Activos)
              </button>
              <button
                type="button"
                onClick={() => setFiltroEstado('FINALIZADO')}
                className={`font-medium text-sm transition ${
                  filtroEstado === 'FINALIZADO'
                    ? 'text-green-600 font-bold border-b-2 border-green-600 pb-2 -mb-[9px]'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                Finalizados y Cancelados
              </button>
            </div>

            {viajesFiltrados.length === 0 ? (
              <p className="text-slate-600">No hay viajes en esta sección.</p>
            ) : (
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                {viajesFiltrados.map((viaje) => {
                  const { origen, destino, paradasIntermedias } = getOrigenDestino(viaje.paradas);
                  const esRecurrente = esViajeRecurrentePadre(viaje);

                  return (
                    <div key={`${viaje.id}-${viaje.rol}`} className="rounded-2xl border border-slate-300 bg-gray-50 p-4 shadow-sm flex flex-col justify-between">
                      <div>
                        <div className="flex justify-between items-start mb-2">
                          <h3 className="font-semibold text-slate-900">
                            {viaje.vehiculo.marca} {viaje.vehiculo.modelo}
                          </h3>
                          <div className="flex flex-col items-end space-y-1">
                            {esRecurrente && (
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-300">
                                🔄 Recurrente
                              </span>
                            )}
                            <span className={`px-2 py-1 rounded-full text-xs font-medium border ${getEstadoBadgeStyle(viaje.estado)}`}>
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

                        {esRecurrente ? (
                          <div className="my-2 p-2 bg-amber-50/50 rounded-lg border border-amber-200 text-sm text-slate-700">
                            <p className="mb-1">
                              <span className="font-medium text-amber-900">Días activos:</span> {viaje.diasSemana?.join(', ')}
                            </p>
                            {viaje.fechaFinRecurrencia && (
                              <p className="text-xs text-slate-500 mb-2">
                                Fin serie: {formatFecha(viaje.fechaFinRecurrencia)}
                              </p>
                            )}

                            {viaje.viajesRecurrentes && viaje.viajesRecurrentes.length > 0 && (
                              <details className="mt-2 text-xs text-slate-600 group">
                                <summary className="cursor-pointer font-medium text-amber-900 hover:underline">
                                  Ver próximas fechas ({viaje.viajesRecurrentes.length})
                                </summary>
                                <ul className="mt-2 space-y-1 pl-2 border-l-2 border-amber-300">
                                  {viaje.viajesRecurrentes.map((instancia) => (
                                    <li key={instancia.id} className="flex justify-between items-center py-1">
                                      <span>{formatFecha(instancia.fechaHoraSalida)}</span>
                                      <span className="text-[10px] bg-slate-200 px-1.5 py-0.5 rounded text-slate-700">
                                        Plazas: {instancia.plazasDisponibles}
                                      </span>
                                    </li>
                                  ))}
                                </ul>
                              </details>
                            )}
                          </div>
                        ) : (
                          <p className="text-sm text-slate-600 mb-1">
                            {formatFecha(viaje.fechaHoraSalida)}
                          </p>
                        )}

                        <p className="text-sm text-slate-600 mb-1">
                          Plazas disponibles: {viaje.plazasDisponibles}
                        </p>
                        <p className="text-sm font-medium text-slate-900">
                          {viaje.precio}€
                        </p>
                      </div>
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
                        className="mt-3 w-full rounded-lg bg-slate-900 px-3 py-2 text-sm text-white hover:bg-slate-700 transition-colors"
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