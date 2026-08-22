import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { buildApiUrl } from '../apiConfig';

interface PerfilData {
  id?: number;
  nombre: string;
  primerApellido: string;
}

interface ParadaDTO {
  id: number;
  localizacion: string;
  tipo: string; // "ORIGEN", "DESTINO", "INTERMEDIA"
  orden: number;
}

interface ViajeDTO {
  id: number;
  fechaHoraSalida: string;
  estado: string;
  plazasDisponibles: number;
  precio: number;
  slug: string;
  conductorId: number;
  conductorNombre: string;
  conductorSlug?: string;
  paradas: ParadaDTO[];
}

interface TopConductor {
  id: number;
  nombre: string;
  primerApellido?: string;
  reputacion?: number;
  valoracionMedia: number;
}

const HomeLoggedIn: React.FC = () => {
  const navigate = useNavigate();
  const [perfil, setPerfil] = useState<PerfilData | null>(null);

  // Estados para datos dinámicos
  const [proximoViaje, setProximoViaje] = useState<ViajeDTO | null>(null);
  const [topConductores, setTopConductores] = useState<TopConductor[]>([]);
  const [cargandoProximo, setCargandoProximo] = useState(true);
  const [cargandoTop, setCargandoTop] = useState(true);

  // Estados del formulario de búsqueda
  const [origen, setOrigen] = useState('');
  const [destino, setDestino] = useState('');
  const [fecha, setFecha] = useState('');

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token || token === 'undefined' || token === 'null' || token.trim() === '') {
      return;
    }

    const headers = {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    };

    // 1. Cargar perfil del usuario
    fetch(buildApiUrl('/api/personas/perfil'), { headers })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (data) {
          setPerfil({
            id: data?.id, // 👈 Guardar el ID
            nombre: data?.nombre || '',
            primerApellido: data?.primerApellido || ''
          });
        }
      })
      .catch(() => {});

    // 2. Cargar próximo viaje (Soporta HTTP 200 y HTTP 204 No Content)
    fetch(buildApiUrl('/api/viajes/proximo'), { headers })
      .then((res) => {
        if (res.status === 204 || !res.ok) return null;
        return res.json();
      })
      .then((data: ViajeDTO | null) => setProximoViaje(data))
      .catch(() => setProximoViaje(null))
      .finally(() => setCargandoProximo(false));

    // 3. Cargar top conductores
    fetch(buildApiUrl('/api/personas/top-conductores'), { headers })
      .then((res) => (res.ok ? res.json() : []))
      .then((data) => setTopConductores(Array.isArray(data) ? data : []))
      .catch(() => setTopConductores([]))
      .finally(() => setCargandoTop(false));

  }, []);

  const handleBuscar = (e: React.FormEvent) => {
    e.preventDefault();
    const params = new URLSearchParams();
    if (origen.trim()) params.set('origen', origen.trim());
    if (destino.trim()) params.set('destino', destino.trim());
    if (fecha) params.set('fecha', fecha);

    navigate('/buscar?' + params.toString());
  };

  // Helper para extraer origen y destino del array de paradas
  const getOrigenDestino = (paradas?: ParadaDTO[]) => {
    if (!paradas || paradas.length === 0) {
      return { origen: 'Origen', destino: 'Destino' };
    }
    const paradasOrdenadas = [...paradas].sort((a, b) => a.orden - b.orden);
    const origenLoc = paradasOrdenadas.find((p) => p.tipo === 'ORIGEN')?.localizacion || paradasOrdenadas[0]?.localizacion;
    const destinoLoc = paradasOrdenadas.find((p) => p.tipo === 'DESTINO')?.localizacion || paradasOrdenadas[paradasOrdenadas.length - 1]?.localizacion;

    return {
      origen: origenLoc || 'Origen',
      destino: destinoLoc || 'Destino'
    };
  };

  // Helper para formatear fechaHoraSalida ISO (e.g. 2026-08-20T10:30:00)
  const formatFechaHora = (isoString?: string) => {
    if (!isoString) return { fechaFormatted: '', horaFormatted: '' };
    const date = new Date(isoString);
    const fechaFormatted = date.toLocaleDateString('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
    const horaFormatted = date.toLocaleTimeString('es-ES', {
      hour: '2-digit',
      minute: '2-digit'
    });
    return { fechaFormatted, horaFormatted };
  };

  const nombreMostrado = perfil?.nombre?.trim() || 'usuario';

  return (
    <section className="min-h-[calc(100vh-96px)] bg-gray-100 px-4 py-8 md:px-8">
      <div className="mx-auto max-w-6xl">
        <h1 className="text-3xl md:text-4xl font-semibold text-slate-900">Hola, {nombreMostrado}</h1>

        {/* Bloque de búsqueda */}
        <div className="mt-4 rounded-2xl border border-slate-400 bg-gray-100 p-5 shadow-sm">
          <h2 className="text-3xl md:text-4xl font-medium text-slate-900">¿A dónde quieres ir?</h2>

          <form onSubmit={handleBuscar} className="mt-5 grid gap-3 md:grid-cols-4">
            <input
              placeholder="Origen"
              className="rounded-xl border border-slate-500 px-4 py-2 text-base placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-sky-500"
              value={origen}
              onChange={(e) => setOrigen(e.target.value)}
            />
            <input
              placeholder="Destino"
              className="rounded-xl border border-slate-500 px-4 py-2 text-base placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-sky-500"
              value={destino}
              onChange={(e) => setDestino(e.target.value)}
            />
            <input
              type="date"
              className="rounded-xl border border-slate-500 px-4 py-2 text-base text-slate-700 focus:outline-none focus:ring-2 focus:ring-sky-500"
              value={fecha}
              onChange={(e) => setFecha(e.target.value)}
            />
            <button
              type="submit"
              className="rounded-full bg-gradient-compi px-8 py-2 text-sm font-bold text-white shadow hover:opacity-95 transition-opacity"
            >
              Buscar viaje
            </button>
          </form>
        </div>

        {/* Sección inferior con 2 columnas */}
        <div className="mt-6 grid gap-6 md:grid-cols-2">
          
          {/* Tarjeta: Próximo viaje */}
          <article className="flex flex-col justify-between rounded-2xl border border-slate-400 bg-gray-100 p-6 shadow-sm">
            <div>
              <h3 className="text-3xl font-medium text-slate-900">Próximo viaje</h3>

              {cargandoProximo ? (
                <div className="mt-5 space-y-2 text-slate-500 animate-pulse">
                  <p>Cargando información del viaje...</p>
                </div>
              ) : proximoViaje ? (() => {
                const { origen: orig, destino: dest } = getOrigenDestino(proximoViaje.paradas);
                const { fechaFormatted, horaFormatted } = formatFechaHora(proximoViaje.fechaHoraSalida);

                // Comprobar si el usuario actual es el conductor del viaje
                const esConductor = perfil?.id 
                  ? proximoViaje.conductorId === perfil.id
                  : proximoViaje.conductorNombre?.trim().toLowerCase() === perfil?.nombre?.trim().toLowerCase();

                return (
                  <div className="mt-5 space-y-3 text-xl text-slate-800">
                    <div className="flex items-center gap-3">
                      <p>
                        {esConductor ? (
                          <>🚘 <span className="font-semibold">Eres el conductor</span></>
                        ) : (
                          <>🚙 Con <span className="font-semibold">{proximoViaje.conductorNombre}</span></>
                        )}
                      </p>

                      {/* Etiqueta / Badge de rol */}
                      <span className={`text-xs px-3 py-1 rounded-full font-bold uppercase tracking-wider ${
                        esConductor 
                          ? 'bg-sky-100 text-sky-800 border border-sky-300' 
                          : 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                      }`}>
                        {esConductor ? 'Conductor' : 'Pasajero'}
                      </span>
                    </div>

                    <p>
                      📅 El <span className="font-semibold">{fechaFormatted}</span>
                      {horaFormatted && ` a las ${horaFormatted}`}
                    </p>
                    <p>
                      📍 <span className="font-semibold">{orig}</span> ➔{' '}
                      <span className="font-semibold">{dest}</span>
                    </p>
                  </div>
                );
              })() : (
                <div className="mt-5 text-slate-600">
                  <p className="text-lg">No tienes ningún viaje próximo programado.</p>
                </div>
              )}
            </div>

            {proximoViaje && (
              <button
                type="button"
                onClick={() => {
                  // Comprobamos el rol de nuevo para la navegación
                  const esConductor = perfil?.id 
                    ? proximoViaje.conductorId === perfil.id
                    : proximoViaje.conductorNombre?.trim().toLowerCase() === perfil?.nombre?.trim().toLowerCase();

                  navigate(`/viajes/${proximoViaje.slug}`, {
                    state: {
                      backTo: '/inicio', // Ajusta esto si tu ruta home se llama diferente
                      backLabel: 'Volver al inicio',
                      rol: esConductor ? 'conductor' : 'pasajero'
                    }
                  });
                }}
                className="mt-6 w-fit rounded-full bg-slate-800 px-6 py-2 text-sm font-bold text-white shadow hover:bg-slate-700 transition-colors"
              >
                Ver detalles
              </button>
            )}
          </article>

          {/* Tarjeta: Top conductores */}
          <article className="flex flex-col justify-between rounded-2xl border border-slate-400 bg-gray-100 p-6 shadow-sm">
            <div>
              <h3 className="text-3xl font-medium text-slate-900">Top conductores</h3>

              {cargandoTop ? (
                <div className="mt-5 space-y-2 text-slate-500 animate-pulse">
                  <p>Cargando conductores destacados...</p>
                </div>
              ) : topConductores.length > 0 ? (
                <div className="mt-5 space-y-3 text-xl text-slate-800">
                  {topConductores.slice(0, 3).map((conductor) => {
                    // Obtiene la reputación o valoracionMedia; si no tiene ninguna, muestra 0.0
                    const nota = conductor.reputacion ?? conductor.valoracionMedia ?? 0;

                    return (
                      <div key={conductor.id} className="flex items-center justify-between">
                        <p>
                          👤 {[conductor.nombre, conductor.primerApellido].filter(Boolean).join(' ')}
                        </p>
                        <span className="font-semibold text-amber-600">
                          {nota.toFixed(1)} ⭐
                        </span>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div className="mt-5 text-slate-600">
                  <p className="text-lg">Aún no hay conductores destacados disponibles.</p>
                </div>
              )}
            </div>

            <button
              type="button"
              onClick={() => navigate('/ofrecer-trayecto')}
              className="mt-6 w-fit rounded-full bg-gradient-compi px-6 py-2 text-sm font-bold text-white shadow hover:opacity-95 transition-opacity"
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