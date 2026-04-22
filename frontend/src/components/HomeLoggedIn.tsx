import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { buildApiUrl } from '../apiConfig';

interface PerfilData {
  nombre: string;
  primerApellido: string;
}

const HomeLoggedIn: React.FC = () => {
  const navigate = useNavigate();
  const [perfil, setPerfil] = useState<PerfilData | null>(null);

  useEffect(() => {
    const fetchPerfil = async () => {
      const token = localStorage.getItem('token');
      if (!token || token === 'undefined' || token === 'null' || token.trim() === '') {
        return;
      }

      try {
        const response = await fetch(buildApiUrl('/api/personas/perfil'), {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });

        if (response.ok) {
          const data = await response.json();
          setPerfil({
            nombre: data?.nombre || '',
            primerApellido: data?.primerApellido || ''
          });
        }
      } catch {
        // Si falla, mantenemos placeholders sin romper la UI.
      }
    };

    fetchPerfil();
  }, []);

  const nombreMostrado = perfil?.nombre?.trim() || 'usuario';
  const nombreCompleto = [perfil?.nombre, perfil?.primerApellido].filter(Boolean).join(' ').trim() || nombreMostrado;

  return (
    <section className="min-h-[calc(100vh-96px)] bg-gray-100 px-4 py-8 md:px-8">
      <div className="mx-auto max-w-6xl">
        <h1 className="text-3xl md:text-4xl font-semibold text-slate-900">¡Hola, {nombreMostrado}!</h1>

        <div className="mt-4 rounded-2xl border border-slate-400 bg-gray-100 p-5">
          <h2 className="text-3xl md:text-4xl font-medium text-slate-900">¿A dónde quieres ir hoy?</h2>

          <div className="mt-5 grid gap-3 md:grid-cols-3">
            <input
              placeholder="Origen"
              className="rounded-xl border border-slate-500 px-4 py-2 text-base placeholder:text-slate-600"
            />
            <input
              placeholder="Destino"
              className="rounded-xl border border-slate-500 px-4 py-2 text-base placeholder:text-slate-600"
            />
            <input
              type="date"
              className="rounded-xl border border-slate-500 px-4 py-2 text-base text-slate-700"
            />
          </div>

          <div className="mt-5 flex justify-center">
            <button className="rounded-full bg-gradient-compi px-8 py-2 text-sm font-bold text-white shadow">
              Buscar viaje
            </button>
          </div>
        </div>

        <div className="mt-6 grid gap-6 md:grid-cols-2">
          <article className="rounded-2xl border border-slate-400 bg-gray-100 p-6">
            <h3 className="text-3xl font-medium text-slate-900">Próximo viaje</h3>
            <div className="mt-5 space-y-3 text-xl text-slate-800">
              <p>🚙 Con {nombreCompleto}</p>
              <p>📅 El [fecha]</p>
              <p>📍 En [ubicación]</p>
            </div>
          </article>

          <article className="rounded-2xl border border-slate-400 bg-gray-100 p-6">
            <h3 className="text-3xl font-medium text-slate-900">Top conductores</h3>
            <div className="mt-5 space-y-3 text-xl text-slate-800">
              <p>👤 {nombreCompleto} 4,8 ⭐</p>
              <p>👤 {nombreCompleto} 4,7 ⭐</p>
              <p>👤 {nombreCompleto} 4,5 ⭐</p>
            </div>

            <button
              type="button"
              onClick={() => navigate('/ofrecer-trayecto')}
              className="mt-6 rounded-full bg-gradient-compi px-6 py-2 text-sm font-bold text-white shadow"
            >
              Publicar un viaje
            </button>
          </article>
        </div>
      </div>
    </section>
  );
};

export default HomeLoggedIn;