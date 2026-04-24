import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { buildApiUrl } from '../../apiConfig';

interface PerfilPublicoData {
  id: number;
  nombre: string;
  primerApellido: string;
  segundoApellido?: string;
  email: string;
  telefono: string;
  reputacion?: number;
  slug: string;
  preferenciasViaje?: string[];

}

type ViajeActividad = {
  id: number;
  fechaHoraSalida: string;
  estado: string;
};

type ResumenActividad = {
  ofrecidosMes: number;
  completados: number;
  cancelados: number;
  tendenciaPct: number;
};

const PerfilPublico: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();

  const [perfil, setPerfil] = useState<PerfilPublicoData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [resumenActividad, setResumenActividad] = useState<ResumenActividad>({
    ofrecidosMes: 0,
    completados: 0,
    cancelados: 0,
    tendenciaPct: 0
  });

  const volver = () => navigate(-1);

  useEffect(() => {
    const fetchPerfilPublico = async () => {
      if (!slug) {
        setError('Perfil no encontrado');
        return;
      }

      try {
        const response = await fetch(buildApiUrl(`/api/personas/${slug}/perfil-publico`), {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json'
          }
        });

        if (!response.ok) {
          setError('No se pudo cargar el perfil público');
          return;
        }

        const data = await response.json();
        setPerfil(data);
      } catch {
        setError('Error de conexión al cargar el perfil');
      }
    };

    const fetchResumenActividad = async () => {
      if (!slug) {
        return;
      }

      try {
        const response = await fetch(buildApiUrl(`/api/viajes/publicos/conductor/${slug}`), {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json'
          }
        });

        if (!response.ok) {
          return;
        }

        const viajes = (await response.json()) as ViajeActividad[];

        const now = new Date();
        const currentMonth = now.getMonth();
        const currentYear = now.getFullYear();

        const prevMonthDate = new Date(currentYear, currentMonth - 1, 1);
        const prevMonth = prevMonthDate.getMonth();
        const prevYear = prevMonthDate.getFullYear();

        const offeredCurrent = viajes.filter((v) => {
          const d = new Date(v.fechaHoraSalida);
          return d.getMonth() === currentMonth && d.getFullYear() === currentYear;
        }).length;

        const offeredPrev = viajes.filter((v) => {
          const d = new Date(v.fechaHoraSalida);
          return d.getMonth() === prevMonth && d.getFullYear() === prevYear;
        }).length;

        const completados = viajes.filter((v) =>
          ['FINALIZADO', 'COMPLETADO'].includes((v.estado || '').toUpperCase())
        ).length;

        const cancelados = viajes.filter((v) =>
          ['CANCELADO', 'CANCELADA'].includes((v.estado || '').toUpperCase())
        ).length;

        const tendenciaPct =
          offeredPrev === 0
            ? offeredCurrent > 0
              ? 100
              : 0
            : Math.round(((offeredCurrent - offeredPrev) / offeredPrev) * 100);

        setResumenActividad({
          ofrecidosMes: offeredCurrent,
          completados,
          cancelados,
          tendenciaPct
        });
      } catch {
        // No bloqueamos la carga del perfil si fallan estadisticas.
      }
    };

    Promise.all([fetchPerfilPublico(), fetchResumenActividad()]).finally(() => setLoading(false));
  }, [slug]);

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-100 py-8 px-4">
        <div className="max-w-5xl mx-auto">
          <div className="bg-white border border-slate-300 rounded-3xl p-8 shadow-sm">
            <p className="text-slate-700 font-semibold">Cargando perfil público...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error || !perfil) {
    return (
      <div className="min-h-screen bg-gray-100 py-8 px-4">
        <div className="max-w-5xl mx-auto">
          <div className="bg-white border border-slate-300 rounded-3xl p-8 shadow-sm">
            <p className="text-red-600 mb-4">{error || 'Perfil no encontrado'}</p>
            <button
              type="button"
              onClick={volver}
              className="rounded-full border border-green-600 px-4 py-2 text-sm text-green-700 transition hover:bg-green-50"
            >
              Volver
            </button>
          </div>
        </div>
      </div>
    );
  }

  const nombreCompleto = [perfil.nombre, perfil.primerApellido, perfil.segundoApellido]
    .filter(Boolean)
    .join(' ');

  return (
    <div className="min-h-screen bg-gray-200 pb-10 pt-4">
      <div className="mx-auto max-w-6xl px-4">
        <button
          type="button"
          onClick={volver}
          className="rounded-full border border-green-600 px-4 py-1 text-sm text-green-700 transition hover:bg-green-50"
        >
          ← Volver al viaje
        </button>

        <div className="mt-4 grid gap-4 lg:grid-cols-[220px_1fr]">
          <aside className="rounded-xl bg-transparent p-2">
            <h2 className="text-4xl font-bold leading-none text-slate-800">
              {nombreCompleto}
            </h2>

            <div className="mt-4 flex h-28 w-28 items-center justify-center rounded-full border-4 border-slate-800 bg-white text-4xl text-slate-700 overflow-hidden">
              <span>{perfil?.nombre?.charAt(0).toUpperCase()}</span>
            </div>
          </aside>

          <section className="grid gap-4 md:grid-cols-2 items-start">
            <div className="rounded-xl border border-slate-500 bg-gray-100 p-5 md:col-span-2">
              <h3 className="text-3xl font-semibold text-slate-800">Datos y actividad</h3>
              <div className="mt-3 space-y-1 text-lg text-slate-700">
                <p>Nombre: {nombreCompleto || '-'}</p>
                <p>Email: {perfil.email || '-'}</p>
                <p>Teléfono: {perfil.telefono || '-'}</p>
              </div>
              <div className="my-4 h-px bg-slate-300" />
              <div className="grid grid-cols-2 gap-3">
                <div className="rounded-xl border border-slate-300 bg-white p-3 shadow-sm">
                  <p className="text-xs font-semibold uppercase text-slate-500">Este mes</p>
                  <p className="mt-1 text-2xl font-bold text-slate-900">{resumenActividad.ofrecidosMes}</p>
                  <p className="text-sm text-slate-600">viajes ofrecidos</p>
                </div>
                <div className="rounded-xl border border-slate-300 bg-white p-3 shadow-sm">
                  <p className="text-xs font-semibold uppercase text-slate-500">Completados</p>
                  <p className="mt-1 text-2xl font-bold text-slate-900">{resumenActividad.completados}</p>
                  <p className="text-sm text-slate-600">histórico</p>
                </div>
                <div className="rounded-xl border border-slate-300 bg-white p-3 shadow-sm">
                  <p className="text-xs font-semibold uppercase text-slate-500">Cancelados</p>
                  <p className="mt-1 text-2xl font-bold text-slate-900">{resumenActividad.cancelados}</p>
                  <p className="text-sm text-slate-600">histórico</p>
                </div>
                <div className="rounded-xl border border-slate-300 bg-white p-3 shadow-sm">
                  <p className="text-xs font-semibold uppercase text-slate-500">Tendencia</p>
                  <p className="mt-1 text-2xl font-bold text-slate-900">
                    {resumenActividad.tendenciaPct > 0 ? '+' : ''}
                    {resumenActividad.tendenciaPct}%
                  </p>
                  <p className="text-sm text-slate-600">vs mes anterior</p>
                </div>
              </div>
            </div>

            <div className="rounded-xl border border-slate-500 bg-gray-100 p-5 md:col-span-2">
              <label className="font-semibold">Preferencias de viaje:</label>
              <div className="flex flex-wrap gap-2 my-2">
                {perfil.preferenciasViaje && perfil.preferenciasViaje.length > 0 ? (
                  perfil.preferenciasViaje.map((pref, idx) => (
                    <span key={idx} className="bg-blue-100 text-blue-800 px-2 py-1 rounded">{pref}</span>
                  ))
                ) : (
                  <span className="text-gray-500">Sin preferencias</span>
                )}
              </div>
            </div>

            <div className="rounded-xl border border-slate-500 bg-gray-100 p-5 md:col-span-2">
              <h3 className="text-3xl font-semibold text-slate-800">Valoraciones</h3>
              <p className="mt-6 text-xl text-slate-700">
                Puntuación media: {Number(perfil.reputacion ?? 0).toFixed(1)} / 5 &nbsp; (0 reseñas)
              </p>
            </div>
          </section>
          </div>
      </div>
    </div>
  );
};

export default PerfilPublico;