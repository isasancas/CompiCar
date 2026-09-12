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
  numeroCancelaciones?: number;
  slug: string;
  preferenciasViaje?: string[];
  fechaAntiguedad?: string;
}

const formatearFechaAntiguedad = (fecha?: string): string => {
  if (!fecha) return '-';
  const fechaCuenta = new Date(`${fecha}T00:00:00`);
  if (Number.isNaN(fechaCuenta.getTime())) return '-';
  return fechaCuenta.toLocaleDateString('es-ES');
};

const PerfilPublico: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();

  const [perfil, setPerfil] = useState<PerfilPublicoData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [totalValoracionesRecibidas, setTotalValoracionesRecibidas] = useState(0);
  const [totalViajesExitosos, setTotalViajesExitosos] = useState(0);
  const [totalViajesParticipados, setTotalViajesParticipados] = useState(0);

  const porcentajeViajesCompletados = totalViajesParticipados > 0
    ? Math.round((totalViajesExitosos / totalViajesParticipados) * 100)
    : 0;
  const tasaCancelacion = totalViajesParticipados > 0
    ? Math.round(((perfil?.numeroCancelaciones ?? 0) / totalViajesParticipados) * 100)
    : 0;
  const esUsuarioFiable = totalViajesParticipados > 0 && tasaCancelacion < 5;

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

        const valoracionesResponse = await fetch(buildApiUrl(`/api/valoraciones/valorado/${data.id}`), {
          method: 'GET',
          headers: { 'Content-Type': 'application/json' }
        });

        if (valoracionesResponse.ok) {
          const valoraciones = await valoracionesResponse.json();
          setTotalValoracionesRecibidas(Array.isArray(valoraciones) ? valoraciones.length : 0);
        }
      } catch {
        setError('Error de conexión al cargar el perfil');
      }
    };

    const fetchViajesExitosos = async () => {
      if (!slug) {
        return;
      }

      try {
        const response = await fetch(buildApiUrl(`/api/viajes/publicos/conductor/${slug}/exitosos`), {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json'
          }
        });

        if (!response.ok) {
          return;
        }

        const total = Number(await response.json());
        setTotalViajesExitosos(Number.isFinite(total) ? total : 0);
      } catch {
        // No bloqueamos la carga del perfil si fallan estadisticas.
      }
    };

    const fetchViajesParticipados = async () => {
      if (!slug) return;

      try {
        const response = await fetch(buildApiUrl(`/api/viajes/publicos/conductor/${slug}/participados`), {
          method: 'GET',
          headers: { 'Content-Type': 'application/json' }
        });

        if (response.ok) {
          const total = Number(await response.json());
          setTotalViajesParticipados(Number.isFinite(total) ? total : 0);
        }
      } catch {
        // No bloqueamos la carga del perfil si fallan estadisticas.
      }
    };

    Promise.all([fetchPerfilPublico(), fetchViajesExitosos(), fetchViajesParticipados()]).finally(() => setLoading(false));
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
          <aside className="rounded-xl bg-transparent p-2 text-center">
            <h2 className="text-4xl font-bold leading-none text-slate-800">
              {nombreCompleto}
            </h2>

            <div className="mx-auto mt-4 flex h-28 w-28 items-center justify-center rounded-full border-4 border-slate-800 bg-white text-4xl text-slate-700 overflow-hidden">
              <span>{perfil?.nombre?.charAt(0).toUpperCase()}</span>
            </div>

            {esUsuarioFiable && (
              <div className="mx-auto mt-4 flex w-fit items-center gap-2 rounded-full border-2 border-amber-400 bg-gradient-to-b from-amber-100 to-amber-300 px-3 py-2 text-amber-950 shadow-md">
                <span className="flex h-7 w-7 items-center justify-center rounded-full border-2 border-amber-600 bg-amber-400 text-lg font-black leading-none text-white">✓</span>
                <div>
                  <p className="text-xs font-extrabold uppercase tracking-wide">Perfil fiable</p>
                  <p className="text-[10px] font-medium">Cancelaciones: {tasaCancelacion}%</p>
                </div>
              </div>
            )}
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
              <div className="grid gap-3 sm:grid-cols-2 md:grid-cols-3">
                <div className="rounded-xl border border-slate-300 bg-white p-3 shadow-sm">
                  <p className="text-xs font-semibold uppercase text-slate-500">Antigüedad de la cuenta</p>
                  <p className="mt-1 text-2xl font-bold text-slate-900">{formatearFechaAntiguedad(perfil.fechaAntiguedad)}</p>
                  <p className="text-sm text-slate-600">fecha de registro</p>
                </div>
                <div className="rounded-xl border border-slate-300 bg-white p-3 shadow-sm">
                  <p className="text-xs font-semibold uppercase text-slate-500">Viajes participados</p>
                  <p className="mt-1 text-2xl font-bold text-slate-900">{totalViajesParticipados}</p>
                  <p className="text-sm text-slate-600">total histórico</p>
                </div>
                <div className="rounded-xl border border-slate-300 bg-white p-3 shadow-sm">
                  <p className="text-xs font-semibold uppercase text-slate-500">Viajes completados</p>
                  <p className="mt-1 text-2xl font-bold text-slate-900">{totalViajesExitosos}</p>
                  <p className="text-sm text-slate-600">como conductor o pasajero</p>
                </div>
                <div className="rounded-xl border border-slate-300 bg-white p-3 shadow-sm">
                  <p className="text-xs font-semibold uppercase text-slate-500">Ratio de éxito viajes</p>
                  <p className="mt-1 text-2xl font-bold text-slate-900">{porcentajeViajesCompletados}%</p>
                  <p className="text-sm text-slate-600">completados sobre participados</p>
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
                Puntuación media: {Number(perfil.reputacion ?? 0).toFixed(1)} / 5 &nbsp;
                ({totalValoracionesRecibidas} {totalValoracionesRecibidas === 1 ? 'reseña' : 'reseñas'})
              </p>
            </div>

          </section>
          </div>
      </div>
    </div>
  );
};

export default PerfilPublico;