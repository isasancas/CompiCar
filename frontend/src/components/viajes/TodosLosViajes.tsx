import React, { useEffect, useState } from 'react';
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

const TodosLosViajes: React.FC = () => {
  const navigate = useNavigate();
  const [viajes, setViajes] = useState<Viaje[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchViajes = async () => {
      try {
        const response = await fetch(buildApiUrl('/api/viajes/publicos'));

        if (response.ok) {
          const data = await response.json();
          // Ordenar por fecha ascendente
          data.sort((a: Viaje, b: Viaje) =>
            new Date(a.fechaHoraSalida).getTime() - new Date(b.fechaHoraSalida).getTime()
          );
          setViajes(data);
        } else {
          setError('No se pudieron cargar los viajes disponibles');
        }
      } catch {
        setError('Error de conexión');
      } finally {
        setLoading(false);
      }
    };

    fetchViajes();
  }, []);

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

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-200 pb-10 pt-6 flex items-center justify-center">
        <p className="text-slate-700 font-semibold">Cargando viajes disponibles...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-200 pb-10 pt-6 flex items-center justify-center px-4">
        <div className="bg-white p-6 rounded-lg shadow-md max-w-md w-full text-center">
          <p className="text-red-500 mb-4">{error}</p>
          <button
            type="button"
            onClick={() => navigate('/')}
            className="bg-gradient-compi hover:opacity-90 text-white font-bold py-2 px-4 rounded"
          >
            Volver a Inicio
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
          onClick={() => navigate('/')}
          className="rounded-full border border-green-600 px-4 py-1 text-sm text-green-700 transition hover:bg-green-50"
        >
          ← Volver al inicio
        </button>

        <div className="mt-6 space-y-8">
          <div className="rounded-3xl border border-slate-300 bg-white p-6 shadow-sm">
            <h1 className="text-3xl font-bold text-slate-900 mb-2">Todos los viajes disponibles</h1>
            <p className="text-slate-600 mb-6">
              {viajes.length} {viajes.length === 1 ? 'viaje encontrado' : 'viajes encontrados'}
            </p>

            {viajes.length === 0 ? (
              <p className="text-slate-600 text-center py-8">No hay viajes disponibles en este momento.</p>
            ) : (
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                {viajes.map((viaje) => {
                  const { origen, destino, paradasIntermedias } = getOrigenDestino(viaje.paradas);
                  return (
                    <div key={viaje.id} className="rounded-2xl border border-slate-300 bg-gray-50 p-4 shadow-sm hover:shadow-md transition">
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
                              backTo: '/explorar',
                              backLabel: 'Volver a Explorar'
                            }
                          })
                        }
                        className="mt-3 w-full rounded-lg bg-slate-900 px-3 py-2 text-sm text-white hover:bg-slate-700"
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

export default TodosLosViajes;