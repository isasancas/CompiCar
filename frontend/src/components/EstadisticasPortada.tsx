import React, { useEffect, useState } from 'react';
import { buildApiUrl } from '../apiConfig';

interface RutaFrecuente {
  localizacion: string;
  cantidad: number;
}

interface CiudadContaminacion {
  ciudad: string;
  indiceCalidadAire?: string;
  categoria?: string;
  contaminantes?: {
    pm25?: string;
    pm10?: string;
    no2?: string;
    o3?: string;
  };
}

interface DatosContaminacion {
  momentoConsulta?: string;
  fuente?: string;
  ciudades?: CiudadContaminacion[];
}

interface RespuestaContaminacion {
  datos?: string;
  fechaConsulta?: string;
}

const ciudadesEsperadas = ['Madrid', 'Barcelona', 'Valencia', 'Sevilla', 'Zaragoza'];

const obtenerRutasFrecuentes = async (): Promise<RutaFrecuente[]> => {
  const response = await fetch(buildApiUrl('/api/paradas/top5-localizaciones'));
  if (!response.ok) throw new Error('No se pudieron cargar las rutas frecuentes');

  const data: unknown = await response.json();
  if (!Array.isArray(data)) return [];

  return data.slice(0, 5).flatMap((item): RutaFrecuente[] => {
    if (!Array.isArray(item) || typeof item[0] !== 'string') return [];
    const cantidad = Number(item[1]);
    return [{ localizacion: item[0], cantidad: Number.isFinite(cantidad) ? cantidad : 0 }];
  });
};

const obtenerContaminacion = async (): Promise<{ datos: DatosContaminacion; fecha?: string }> => {
  const response = await fetch(buildApiUrl('/api/contaminacion/actual'));
  if (!response.ok) throw new Error('No se pudo cargar la contaminación');

  const data = (await response.json()) as RespuestaContaminacion;
  if (!data.datos) return { datos: {} };

  try {
    const datos = JSON.parse(data.datos) as DatosContaminacion;
    return { datos, fecha: data.fechaConsulta };
  } catch {
    return { datos: { fuente: data.datos }, fecha: data.fechaConsulta };
  }
};

const formatearFecha = (fecha?: string) => {
  if (!fecha) return '';
  const date = new Date(fecha);
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleDateString('es-ES');
};

const EstadisticasPortada: React.FC = () => {
  const [rutas, setRutas] = useState<RutaFrecuente[]>([]);
  const [ciudades, setCiudades] = useState<CiudadContaminacion[]>([]);
  const [fuente, setFuente] = useState('');
  const [fechaConsulta, setFechaConsulta] = useState('');
  const [cargandoRutas, setCargandoRutas] = useState(true);
  const [cargandoContaminacion, setCargandoContaminacion] = useState(true);

  useEffect(() => {
    obtenerRutasFrecuentes()
      .then(setRutas)
      .catch(() => setRutas([]))
      .finally(() => setCargandoRutas(false));

    obtenerContaminacion()
      .then(({ datos, fecha }) => {
        const ciudadesRecibidas = Array.isArray(datos.ciudades) ? datos.ciudades : [];
        setCiudades(ciudadesEsperadas.map((nombre) => (
          ciudadesRecibidas.find((ciudad) => ciudad.ciudad.toLowerCase() === nombre.toLowerCase())
          || { ciudad: nombre }
        )));
        setFuente(datos.fuente || 'Fuente no disponible');
        setFechaConsulta(formatearFecha(datos.momentoConsulta || fecha));
      })
      .catch(() => {
        setCiudades([]);
        setFuente('Información no disponible en este momento');
      })
      .finally(() => setCargandoContaminacion(false));
  }, []);

  return (
    <section className="bg-white px-4 py-10 text-slate-900 md:px-8 md:py-12">
      <div className="mx-auto max-w-7xl">
        <div className="mb-8 max-w-2xl">
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-emerald-600">Datos de la comunidad</p>
          <h2 className="mt-3 text-3xl font-extrabold tracking-tight text-slate-950 md:text-4xl">Muévete con más contexto</h2>
          <p className="mt-3 text-slate-600">Descubre dónde se concentra la movilidad y consulta la calidad del aire de las principales ciudades españolas.</p>
        </div>

        <div className="grid min-w-0 gap-5 lg:grid-cols-[minmax(0,0.85fr)_minmax(0,1.5fr)]">
          <article className="min-w-0 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-2xl" aria-hidden="true">📍</p>
                <h3 className="mt-3 text-xl font-bold text-slate-900 md:text-2xl">Rutas y destinos frecuentes</h3>
                <p className="mt-2 text-sm text-slate-500">Paradas más repetidas por la comunidad</p>
              </div>
              <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-bold text-emerald-700">Top 5</span>
            </div>

            {cargandoRutas ? (
              <p className="mt-8 animate-pulse text-slate-500">Cargando localizaciones...</p>
            ) : rutas.length > 0 ? (
              <ol className="mt-7 space-y-3">
                {rutas.map((ruta, index) => (
                  <li key={`${ruta.localizacion}-${index}`} className="flex min-w-0 items-center gap-3">
                    <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-emerald-500 text-sm font-extrabold text-white">{index + 1}</span>
                    <span className="min-w-0 flex-1 truncate text-sm font-semibold text-slate-800">{ruta.localizacion}</span>
                    <span className="shrink-0 text-xs text-slate-500">{ruta.cantidad} paradas</span>
                  </li>
                ))}
              </ol>
            ) : (
              <p className="mt-8 text-slate-500">Todavía no hay datos suficientes.</p>
            )}
          </article>

          <article className="min-w-0 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <p className="text-2xl" aria-hidden="true">🌿</p>
                <h3 className="mt-3 text-xl font-bold text-slate-900 md:text-2xl">Calidad del aire en España</h3>
                <p className="mt-2 text-sm text-slate-500">Cinco ciudades actualizadas semanalmente</p>
              </div>
              {fechaConsulta && <span className="text-xs text-slate-500">Actualizado el {fechaConsulta}</span>}
            </div>

            {cargandoContaminacion ? (
              <p className="mt-8 animate-pulse text-slate-500">Consultando datos ambientales...</p>
            ) : ciudades.length > 0 ? (
              <div className="mt-7 grid min-w-0 gap-3 sm:grid-cols-2 xl:grid-cols-5">
                {ciudades.map((ciudad) => (
                  <div key={ciudad.ciudad} className="min-w-0 rounded-xl border border-slate-200 bg-slate-50 p-3">
                    <div className="flex items-center justify-between gap-2">
                      <h4 className="truncate text-sm font-bold text-slate-800">{ciudad.ciudad}</h4>
                      <span className="text-lg text-emerald-500" aria-hidden="true">{ciudad.categoria?.toLowerCase() === 'buena' ? '●' : '◐'}</span>
                    </div>
                    <p className="mt-3 truncate text-xl font-extrabold text-emerald-600">{ciudad.indiceCalidadAire || 'N/D'}</p>
                    <p className="mt-1 truncate text-xs capitalize text-slate-500">{ciudad.categoria || 'Sin categoría'}</p>
                    {ciudad.contaminantes?.pm25 && <p className="mt-3 truncate text-xs text-slate-600">PM2.5: {ciudad.contaminantes.pm25}</p>}
                  </div>
                ))}
              </div>
            ) : (
              <p className="mt-8 text-slate-500">{fuente || 'No hay información ambiental disponible.'}</p>
            )}
            {fuente && ciudades.length > 0 && <p className="mt-5 truncate text-xs text-slate-500">Fuente: {fuente}</p>}
          </article>
        </div>
      </div>
    </section>
  );
};

export default EstadisticasPortada;