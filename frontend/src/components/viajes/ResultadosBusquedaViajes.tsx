import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { buildApiUrl } from '../../apiConfig';

type ParadaDTO = {
  id: number;
  localizacion: string;
  tipo: 'ORIGEN' | 'INTERMEDIA' | 'DESTINO' | string;
  orden: number;
};

type VehiculoDTO = {
  id: number;
  marca: string;
  modelo: string;
  matricula: string;
};

type ViajeDTO = {
  id: number;
  slug: string;
  fechaHoraSalida: string;
  estado: string;
  plazasDisponibles: number;
  precio: number;
  vehiculo: VehiculoDTO;
  paradas: ParadaDTO[];
};

const formatFecha = (fecha: string) =>
  new Date(fecha).toLocaleString('es-ES', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });

const getParadasOrdenadas = (paradas: ParadaDTO[]) =>
  [...(paradas || [])].sort((a, b) => (a.orden || 0) - (b.orden || 0));

const ResultadosBusquedaViajes: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [origen, setOrigen] = useState(searchParams.get('origen') || '');
  const [destino, setDestino] = useState(searchParams.get('destino') || '');
  const [fecha, setFecha] = useState(searchParams.get('fecha') || '');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [viajes, setViajes] = useState<ViajeDTO[]>([]);

  const queryKey = useMemo(
    () => `${searchParams.get('origen') || ''}|${searchParams.get('destino') || ''}|${searchParams.get('fecha') || ''}`,
    [searchParams]
  );

  const lanzarBusqueda = () => {
    const params = new URLSearchParams();
    if (origen.trim()) params.set('origen', origen.trim());
    if (destino.trim()) params.set('destino', destino.trim());
    if (fecha) params.set('fecha', fecha);
    setSearchParams(params);
  };

  useEffect(() => {
    const fetchResultados = async () => {
      setLoading(true);
      setError('');

      try {
        const url = buildApiUrl('/api/viajes/publicos?' + searchParams.toString());
        const response = await fetch(url);

        if (!response.ok) {
          throw new Error('No se pudieron cargar los viajes');
        }

        const data = await response.json();
        const lista = Array.isArray(data) ? data : [];
        setViajes(lista);
      } catch (err) {
        const msg = err instanceof Error ? err.message : 'Error de red';
        setError(msg);
        setViajes([]);
      } finally {
        setLoading(false);
      }
    };

    fetchResultados();
  }, [queryKey, searchParams]);

  return (
    <section className="min-h-[calc(100vh-96px)] bg-gray-100 px-4 py-8 md:px-8">
      <div className="mx-auto max-w-6xl">
        <h1 className="text-3xl md:text-4xl font-semibold text-slate-900">Resultados de busqueda</h1>

        <div className="mt-4 rounded-2xl border border-slate-300 bg-white p-4">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              lanzarBusqueda();
            }}
            className="grid gap-3 md:grid-cols-4"
          >
            <input
              className="rounded-xl border border-slate-300 px-4 py-2"
              placeholder="Origen"
              value={origen}
              onChange={(e) => setOrigen(e.target.value)}
            />
            <input
              className="rounded-xl border border-slate-300 px-4 py-2"
              placeholder="Destino"
              value={destino}
              onChange={(e) => setDestino(e.target.value)}
            />
            <input
              type="date"
              className="rounded-xl border border-slate-300 px-4 py-2"
              value={fecha}
              onChange={(e) => setFecha(e.target.value)}
            />
            <button className="rounded-full bg-gradient-compi px-6 py-2 font-bold text-white">
              Buscar
            </button>
          </form>
        </div>

        {loading && <p className="mt-4 text-slate-700">Cargando viajes...</p>}
        {error && <p className="mt-4 rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>}

        {!loading && !error && viajes.length === 0 && (
          <div className="mt-4 rounded-2xl border border-slate-300 bg-white p-5 text-slate-700">
            No hay viajes para esos filtros.
          </div>
        )}

        {!loading && !error && viajes.length > 0 && (
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            {viajes.map((viaje) => {
              const paradasOrdenadas = getParadasOrdenadas(viaje.paradas || []);
              const origenViaje = paradasOrdenadas.find((p) => p.tipo === 'ORIGEN')?.localizacion || paradasOrdenadas[0]?.localizacion || 'Origen';
              const destinoViaje = paradasOrdenadas.find((p) => p.tipo === 'DESTINO')?.localizacion || paradasOrdenadas[paradasOrdenadas.length - 1]?.localizacion || 'Destino';
              const intermedias = paradasOrdenadas.filter((p) => p.tipo === 'INTERMEDIA');

              return (
                <article key={viaje.id} className="rounded-2xl border border-slate-300 bg-white p-5">
                  <h3 className="text-lg font-bold text-slate-900">{origenViaje} → {destinoViaje}</h3>

                  <p className="mt-2 text-sm text-slate-700">Fecha: {formatFecha(viaje.fechaHoraSalida)}</p>
                  <p className="text-sm text-slate-700">Plazas: {viaje.plazasDisponibles}</p>
                  <p className="text-sm text-slate-700">Precio: {Number(viaje.precio || 0).toFixed(2)} €</p>
                  <p className="text-sm text-slate-700">Vehiculo: {viaje.vehiculo?.marca} {viaje.vehiculo?.modelo}</p>

                  {intermedias.length > 0 && (
                    <p className="mt-2 text-sm text-slate-700">
                      Intermedias: {intermedias.map((p) => p.localizacion).join(' · ')}
                    </p>
                  )}

                  <button
                    type="button"
                    onClick={() =>
                        navigate('/viajes/' + viaje.slug, {
                        state: {
                            backTo: '/buscar?' + searchParams.toString(),
                            backLabel: 'Volver a resultados'
                        }
                        })
                    }
                    className="mt-4 rounded-full border border-slate-400 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                    >
                    Ver detalle
                  </button>
                </article>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
};

export default ResultadosBusquedaViajes;