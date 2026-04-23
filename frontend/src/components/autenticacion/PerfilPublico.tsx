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
}

const PerfilPublico: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();

  const [perfil, setPerfil] = useState<PerfilPublicoData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const volver = () => navigate(-1);

  useEffect(() => {
    const fetchPerfilPublico = async () => {
      if (!slug) {
        setError('Perfil no encontrado');
        setLoading(false);
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
          setLoading(false);
          return;
        }

        const data = await response.json();
        setPerfil(data);
      } catch {
        setError('Error de conexión al cargar el perfil');
      } finally {
        setLoading(false);
      }
    };

    fetchPerfilPublico();
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
    <div className="min-h-screen bg-gray-100 py-8 px-4">
      <div className="max-w-5xl mx-auto space-y-6">
        <button
          type="button"
          onClick={volver}
          className="rounded-full border border-green-600 px-4 py-2 text-sm text-green-700 transition hover:bg-green-50"
        >
          ← Volver al viaje
        </button>

        <div className="bg-white border border-slate-300 rounded-3xl shadow-sm overflow-hidden">
          <div className="bg-gradient-compi px-6 py-6 text-white">
            <p className="text-sm opacity-90">Perfil público del conductor</p>
            <h1 className="text-2xl font-bold">{nombreCompleto}</h1>
            <p className="text-sm opacity-90 mt-1">@{perfil.slug}</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 p-6">
            <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200">
              <p className="text-xs font-semibold text-slate-500 mb-1">Nombre</p>
              <p className="text-base font-semibold text-slate-900">{nombreCompleto}</p>
            </div>

            <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200">
              <p className="text-xs font-semibold text-slate-500 mb-1">Reputación</p>
              <p className="text-base font-semibold text-slate-900">
                {Number(perfil.reputacion ?? 0).toFixed(1)} / 5
              </p>
            </div>

            <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200">
              <p className="text-xs font-semibold text-slate-500 mb-1">Email</p>
              <p className="text-base font-semibold text-slate-900">{perfil.email}</p>
            </div>

            <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200">
              <p className="text-xs font-semibold text-slate-500 mb-1">Teléfono</p>
              <p className="text-base font-semibold text-slate-900">{perfil.telefono || 'No disponible'}</p>
            </div>
          </div>

          <div className="px-6 pb-6">
            <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
              Este perfil es solo de lectura. No se permiten cambios desde esta vista.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PerfilPublico;