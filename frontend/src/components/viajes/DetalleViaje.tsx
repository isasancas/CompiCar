import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import {
  MapContainer,
  TileLayer,
  CircleMarker,
  Polyline,
  Tooltip
} from 'react-leaflet';
import { buildApiUrl } from '../../apiConfig';

interface Parada {
  id: number;
  localizacion: string;
  tipo: string;
  orden: number;
}

interface ParadaConCoordenadas extends Parada {
  lat?: number;
  lng?: number;
}

interface ReservaInfo {
  id: number;
  nombrePasajero: string;
  pasajeroId: number;
  cantidadPlazas: number;
  estado: string;
}

interface Viaje {
  id: number;
  slug: string;
  fechaHoraSalida: string;
  estado: string;
  plazasDisponibles: number;
  precio: number;
  conductorNombre?: string;
  conductorSlug?: string;
  vehiculo: {
    marca: string;
    modelo: string;
    matricula: string;
  };
  paradas: Parada[];
  reservas?: ReservaInfo[];
}

interface Reserva {
  id: number;
  estado: string;
  viajeId: number;
  personaId: number;
  paradaSubidaId: number;
  paradaBajadaId: number;
}

const DetalleViaje: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const [viaje, setViaje] = useState<Viaje | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [paradasConCoordenadas, setParadasConCoordenadas] = useState<ParadaConCoordenadas[]>([]);
  const [routeLine, setRouteLine] = useState<Array<[number, number]>>([]);
  const [mapCenter, setMapCenter] = useState<[number, number]>([40.4168, -3.7038]);
  const token = localStorage.getItem('token') || '';
  const [cantidadPlazas, setCantidadPlazas] = useState(1);
  const [aceptaBloqueoPago, setAceptaBloqueoPago] = useState(false);
  const [reservando, setReservando] = useState(false);
  const [reservaMsg, setReservaMsg] = useState<string | null>(null);
  const [modalReservaAbierto, setModalReservaAbierto] = useState(false);
  const [cancelando, setCancelando] = useState(false);
  const [cancelMsg, setCancelMsg] = useState<string | null>(null);
  const [miReserva, setMiReserva] = useState<Reserva | null>(null);
  const [cancelandoReserva, setCancelandoReserva] = useState(false);
  const [cancelReservaMsg, setCancelReservaMsg] = useState<string | null>(null);


  const isLoggedIn = !!token && token !== 'undefined' && token !== 'null' && token.trim() !== '';
  const totalReserva = Number(viaje?.precio || 0) * cantidadPlazas;

  type DetalleNavState = {
    backTo?: string;
    backLabel?: string;
    rol?: 'conductor' | 'pasajero';
  };

  const location = useLocation();
  const navState = (location.state ?? {}) as DetalleNavState;

  const backTo = navState.backTo || '/';
  const backLabel = navState.backLabel || 'Volver al inicio';

  const volver = () => navigate(backTo);

  useEffect(() => {
    const fetchViaje = async () => {
      if (!slug) {
        setError('No se pudo cargar el viaje');
        setLoading(false);
        return;
      }

      try {
        const response = await fetch(buildApiUrl(`/api/viajes/publicos/${slug}`));

        if (response.ok) {
          const data = await response.json();
          setViaje(data);
        } else {
          setError('No se pudo cargar el viaje');
        }
      } catch {
        setError('Error de conexión');
      } finally {
        setLoading(false);
      }
    };

    fetchViaje();
  }, [slug]);

  // Obtener coordenadas de las paradas
  useEffect(() => {
    if (!viaje || viaje.paradas.length === 0) return;

    const obtenerCoordenadas = async () => {
      const paradasActualizadas: ParadaConCoordenadas[] = [];

      for (const parada of viaje.paradas) {
        try {
          const response = await fetch(
            `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(
              parada.localizacion
            )}&format=json&limit=1`,
            { headers: { 'Accept-Language': 'es' } }
          );

          if (response.ok) {
            const results = await response.json();
            if (results.length > 0) {
              paradasActualizadas.push({
                ...parada,
                lat: parseFloat(results[0].lat),
                lng: parseFloat(results[0].lon)
              });
            } else {
              paradasActualizadas.push(parada);
            }
          } else {
            paradasActualizadas.push(parada);
          }
        } catch {
          paradasActualizadas.push(parada);
        }
      }

      setParadasConCoordenadas(paradasActualizadas);

      // Calcular centro del mapa y ruta
      const paradasConCoords = paradasActualizadas.filter((p) => p.lat && p.lng);
      if (paradasConCoords.length > 0) {
        const lats = paradasConCoords.map((p) => p.lat!);
        const lngs = paradasConCoords.map((p) => p.lng!);
        const centerLat = (Math.min(...lats) + Math.max(...lats)) / 2;
        const centerLng = (Math.min(...lngs) + Math.max(...lngs)) / 2;
        setMapCenter([centerLat, centerLng]);

        // Calcular ruta real usando OpenRouteService
        calcularRutaReal(paradasConCoords);
      }
    };

    obtenerCoordenadas();
  }, [viaje]);

  useEffect(() => {
  const fetchMiReserva = async () => {
    if (!viaje || !isLoggedIn) return;

    try {
      const response = await fetch(buildApiUrl('/api/reservas/mis-reservas'), {
      headers: {
        Authorization: `Bearer ${token}`
      }
      });

      if (!response.ok) return;

      const reservas = await response.json();

      // 🔥 Buscar reserva de ESTE viaje
      const reservaEncontrada = reservas.find(
      (r: Reserva) => r.viajeId === viaje.id && r.estado !== 'CANCELADA'
      );

      setMiReserva(reservaEncontrada || null);

    } catch {
      // silencio (no rompemos la UI)
    }
  };
  fetchMiReserva();
}, [viaje, isLoggedIn]);

const cancelarReserva = async () => {
  if (!miReserva) return;

  const confirmacion = window.confirm('¿Cancelar tu reserva?');
  if (!confirmacion) return;

  setCancelandoReserva(true);
  setCancelReservaMsg(null);

  try {
    const response = await fetch(
      buildApiUrl(`/api/reservas/cancelar?reservaId=${miReserva.id}`),
      {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${token}`
        }
      }
    );

    if (!response.ok) {
      const data = await response.json().catch(() => null);
      const msg = data?.message || 'Error al cancelar reserva';
      throw new Error(msg);
    }

    const reservaActualizada = await response.json();

    // 🔥 actualizar estado local
    setMiReserva(reservaActualizada);

    // 🔥 aumentar plazas disponibles en UI
    setViaje((prev) =>
      prev
        ? { ...prev, plazasDisponibles: prev.plazasDisponibles + 1 }
        : prev
    );

    setCancelReservaMsg('✅ Reserva cancelada correctamente');

  } catch (err) {
    const msg = err instanceof Error ? err.message : 'Error inesperado';
    setCancelReservaMsg(`❌ ${msg}`);
  } finally {
    setCancelandoReserva(false);
  }
};

  const calcularRutaReal = async (paradas: ParadaConCoordenadas[]) => {
    if (paradas.length < 2) return;

    const paradasOrdenadas = paradas.sort((a, b) => a.orden - b.orden);
    const coords = paradasOrdenadas.map((p) => `${p.lng},${p.lat}`).join(';');
    const url = `https://router.project-osrm.org/route/v1/driving/${coords}?overview=full&geometries=geojson`;

    try {
      const response = await fetch(url);
      if (!response.ok) {
        // Fallback: línea recta si falla la API
        const ruta: Array<[number, number]> = paradasOrdenadas.map((p) => [p.lat!, p.lng!]);
        setRouteLine(ruta);
        return;
      }

      const data = await response.json();
      const route = data?.routes?.[0];
      if (!route?.geometry?.coordinates) {
        // Fallback: línea recta si no hay geometría
        const ruta: Array<[number, number]> = paradasOrdenadas.map((p) => [p.lat!, p.lng!]);
        setRouteLine(ruta);
        return;
      }

      // Convertir coordenadas de [lng, lat] a [lat, lng] para Leaflet
      const routeCoords: Array<[number, number]> = route.geometry.coordinates.map(
        (pair: [number, number]) => [pair[1], pair[0]]
      );
      setRouteLine(routeCoords);
    } catch {
      // Fallback: línea recta si hay error
      const ruta: Array<[number, number]> = paradasOrdenadas.map((p) => [p.lat!, p.lng!]);
      setRouteLine(ruta);
    }
  };

  const formatFecha = (fecha: string) => {
    return new Date(fecha).toLocaleString('es-ES', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getOrigenDestino = (paradas: Parada[]) => {
    const origen = paradas.find((p) => p.tipo === 'ORIGEN')?.localizacion || 'Desconocido';
    const destino = paradas.find((p) => p.tipo === 'DESTINO')?.localizacion || 'Desconocido';
    const paradasIntermedias = paradas
      .filter((p) => p.tipo === 'INTERMEDIA')
      .sort((a, b) => a.orden - b.orden);
    return { origen, destino, paradasIntermedias };
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="text-slate-700 font-semibold">Cargando detalles del viaje...</div>
      </div>
    );
  }

  if (error || !viaje) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100 px-4">
        <div className="bg-white p-6 rounded-lg shadow-md max-w-md w-full text-center">
          <p className="text-red-500 mb-4">{error || 'Viaje no encontrado'}</p>
          <button
            type="button"
            onClick={volver}
            className="bg-gradient-compi hover:opacity-90 text-white font-bold py-2 px-4 rounded"
          >
            {backLabel}
          </button>
        </div>
      </div>
    );
  }

  const { origen, destino, paradasIntermedias } = getOrigenDestino(viaje.paradas);

  const reservarPlazas = async () => {
    setReservaMsg(null);

    if (!isLoggedIn) {
      navigate('/inicio-sesion');
      return;
    }

    if (!viaje) return;

    if (!aceptaBloqueoPago) {
      setReservaMsg('Debes aceptar el aviso de cobro antes de reservar.');
      return;
    }

    setReservando(true);

    try {
      const response = await fetch(buildApiUrl('/api/reservas/crear'), {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        // ENVIAMOS EL DTO QUE ESPERA EL BACKEND
        body: JSON.stringify({ 
          viajeId: viaje.id, 
          plazas: cantidadPlazas 
        })
      });

      if (response.ok) {
        // const data = await response.json(); // Si necesitas el objeto reserva
        setReservaMsg(`Reserva completada con éxito por ${cantidadPlazas} plaza(s).`);
        
        // Actualizamos el estado local de plazas disponibles
        setViaje((prev) =>
          prev
            ? { ...prev, plazasDisponibles: prev.plazasDisponibles - cantidadPlazas }
            : prev
        );
        
        // Cerramos el modal después de un breve delay o dejamos el mensaje
        setTimeout(() => setModalReservaAbierto(false), 2000);
      } else {
        const data = await response.json().catch(() => null);
        setReservaMsg(`Error: ${data?.message || 'No se pudo realizar la reserva'}`);
      }
    } catch (err) {
      setReservaMsg('Error de conexión con el servidor.');
    } finally {
      setReservando(false);
    }
  };

  const cancelarViaje = async () => {
  if (!viaje) return;

  const confirmacion = window.confirm('¿Estás seguro de que quieres cancelar este viaje?');
  if (!confirmacion) return;

  setCancelando(true);
  setCancelMsg(null);

  try {
    const response = await fetch(
      buildApiUrl(`/api/viajes/${viaje.slug}/cancelar`),
      {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    );

    if (!response.ok) {
      const data = await response.json().catch(() => null);
      const msg = data?.error || data?.message || 'No se pudo cancelar el viaje';
      throw new Error(msg);
    }

    const viajeActualizado = await response.json();

    // 🔥 Actualiza estado sin recargar
    setViaje(viajeActualizado);

    setCancelMsg('✅ Viaje cancelado correctamente.');

  } catch (err) {
    const msg = err instanceof Error ? err.message : 'Error al cancelar viaje';
    setCancelMsg(`❌ ${msg}`);
  } finally {
    setCancelando(false);
  }
};

  return (
    <div className="min-h-screen bg-gray-100 pb-10 pt-6">
      <div className="mx-auto max-w-4xl px-4">
        <button
          type="button"
          onClick={volver}
          className="rounded-full border border-green-600 px-4 py-1 text-sm text-green-700 transition hover:bg-green-50 mb-6"
        >
          ← {backLabel}
        </button>

        <div className="bg-white rounded-3xl border border-slate-300 shadow-sm p-6 mb-6">
          <div className="flex justify-between items-start mb-6">
            <div>
              <h1 className="text-3xl font-bold text-slate-900 mb-2">
                {viaje.vehiculo.marca} {viaje.vehiculo.modelo}
              </h1>
              <p className="text-slate-600">Matrícula: {viaje.vehiculo.matricula}</p>
            </div>
            <span
              className={`px-3 py-2 rounded-full text-sm font-medium ${
                viaje.estado === 'ACTIVO'
                  ? 'bg-green-100 text-green-800'
                  : viaje.estado === 'COMPLETADO'
                    ? 'bg-blue-100 text-blue-800'
                    : 'bg-gray-100 text-gray-800'
              }`}
            >
              {viaje.estado}
            </span>
          </div>
          <div className="mb-6 rounded-2xl border border-blue-200 bg-blue-50 p-4 shadow-sm">
            <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
              <div className="flex items-center gap-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-full border-2 border-blue-300 bg-white text-lg font-bold text-blue-700">
                  {(viaje.conductorNombre || 'U').charAt(0).toUpperCase()}
                </div>
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wide text-blue-700">Usuario que ofrece el viaje</p>
                  <p className="text-lg font-bold text-slate-900">{viaje.conductorNombre || 'Usuario'}</p>
                </div>
              </div>

              {viaje.conductorSlug && (
                <button
                  type="button"
                  onClick={() => navigate(`/usuarios/${viaje.conductorSlug}/perfil`)}
                  className="rounded-full border border-blue-600 bg-white px-4 py-2 text-sm font-semibold text-blue-700 transition hover:bg-blue-100"
                >
                  Ver perfil público
                </button>
              )}
            </div>
          </div>
          {/* Lista de Pasajeros (Solo visible para el conductor) */}
          {navState.rol === 'conductor' && viaje.reservas && viaje.reservas.length > 0 && (
            <div className="mb-6 border-t border-slate-100 pt-6">
              <h3 className="text-lg font-bold text-slate-900 mb-4 flex items-center gap-2">
                <span className="bg-blue-100 text-blue-600 p-1 rounded-md">👤</span>
                Pasajeros confirmados
              </h3>
              <div className="space-y-3">
                {viaje.reservas.map((res) => (
                  <div key={res.id} className="flex items-center justify-between p-4 bg-slate-50 rounded-xl border border-slate-200">
                    <div className="flex items-center gap-3">
                      <div className="h-10 w-10 rounded-full bg-slate-200 flex items-center justify-center font-bold text-slate-600">
                        {res.nombrePasajero.charAt(0)}
                      </div>
                      <div>
                        <p className="font-semibold text-slate-800">{res.nombrePasajero}</p>
                        <p className="text-xs text-slate-500">{res.cantidadPlazas} plaza(s) • {res.estado}</p>
                      </div>
                    </div>
                    <span className="text-sm font-medium text-slate-400 italic">
                      Ver perfil
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Información del trayecto */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
            <div className="bg-green-50 p-4 rounded-lg border border-green-200">
              <p className="text-sm font-medium text-green-700 mb-1">Origen</p>
              <p className="text-lg font-semibold text-slate-900">{origen}</p>
            </div>

            <div className="bg-red-50 p-4 rounded-lg border border-red-200">
              <p className="text-sm font-medium text-red-700 mb-1">Destino</p>
              <p className="text-lg font-semibold text-slate-900">{destino}</p>
            </div>
          </div>

          {/* Paradas intermedias */}
          {paradasIntermedias.length > 0 && (
            <div className="mb-6 p-4 bg-orange-50 rounded-lg border border-orange-200">
              <p className="text-sm font-medium text-orange-700 mb-3">Paradas Intermedias</p>
              <div className="flex flex-wrap gap-2">
                {paradasIntermedias.map((parada) => (
                  <span
                    key={parada.id}
                    className="inline-block bg-white px-3 py-1 rounded-full text-sm border border-orange-300 text-slate-900"
                  >
                    {parada.orden}. {parada.localizacion}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Información del viaje */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="bg-slate-50 p-4 rounded-lg">
              <p className="text-xs font-medium text-slate-600 mb-1">Fecha y Hora</p>
              <p className="text-sm font-semibold text-slate-900">{formatFecha(viaje.fechaHoraSalida)}</p>
            </div>

            <div className="bg-slate-50 p-4 rounded-lg">
              <p className="text-xs font-medium text-slate-600 mb-1">Plazas Disponibles</p>
              <p className="text-sm font-semibold text-slate-900">{viaje.plazasDisponibles}</p>
            </div>

            <div className="bg-slate-50 p-4 rounded-lg">
              <p className="text-xs font-medium text-slate-600 mb-1">Precio</p>
              <p className="text-sm font-semibold text-slate-900">{viaje.precio}€</p>
            </div>

            <div className="bg-slate-50 p-4 rounded-lg">
              <p className="text-xs font-medium text-slate-600 mb-1">Estado</p>
              <p className="text-sm font-semibold text-slate-900">{viaje.estado}</p>
            </div>
          </div>

          {/* Botón Reservar - Solo mostrar si NO es conductor */}
          {navState.rol !== 'conductor' && (
            <button
              type="button"
              onClick={() => setModalReservaAbierto(true)}
              disabled={viaje.plazasDisponibles <= 0}
              className="w-full rounded-lg bg-gradient-compi px-6 py-3 text-base font-bold text-white disabled:opacity-60 transition-all hover:opacity-90"
            >
              {viaje.plazasDisponibles > 0 ? 'Reservar ahora' : 'Sin plazas disponibles'}
            </button>
          )}
          {/* Botón Cancelar reserva */}
          {miReserva && miReserva.estado !== 'CANCELADA' && (
            <button
              type="button"
              onClick={cancelarReserva}
              disabled={cancelandoReserva}
              className="w-full mt-3 rounded-lg bg-yellow-500 px-6 py-3 text-base font-bold text-white hover:bg-yellow-600 disabled:opacity-60"
            >
              {cancelandoReserva ? 'Cancelando...' : 'Cancelar mi reserva'}
            </button>
          )} 
          {cancelReservaMsg && (
            <div className={`mt-3 p-3 rounded-lg text-sm ${
              cancelReservaMsg.includes('✅')
                ? 'bg-green-50 border border-green-200 text-green-700'
                : 'bg-red-50 border border-red-200 text-red-700'
            }`}>
              {cancelReservaMsg}
            </div>
          )}
          {/* Botón Cancelar - SOLO CONDUCTOR */}
            {navState.rol === 'conductor' && viaje.estado === 'PENDIENTE' && (
              <button
                type="button"
                onClick={() => {cancelarViaje(); volver();}}
                disabled={cancelando}
                className="w-full mt-3 rounded-lg bg-red-600 px-6 py-3 text-base font-bold text-white hover:bg-red-700 disabled:opacity-60 transition-all"
              >
                {cancelando ? 'Cancelando...' : 'Cancelar viaje'}
              </button>
            )}
            {cancelMsg && (
              <div className={`mt-3 p-3 rounded-lg text-sm ${
                cancelMsg.includes('✅')
                  ? 'bg-green-50 border border-green-200 text-green-700'
                  : 'bg-red-50 border border-red-200 text-red-700'
              }`}>
                {cancelMsg}
              </div>
            )}
        </div>

        {/* Mapa */}
        <div className="bg-white rounded-3xl border border-slate-300 shadow-sm overflow-hidden">
          <div className="p-6 border-b border-slate-300">
            <h2 className="text-xl font-bold text-slate-900">Ruta del viaje</h2>
          </div>
          <div className="relative w-full h-96 bg-gray-100">
            <MapContainer
              center={mapCenter}
              zoom={6}
              style={{ height: '100%', width: '100%' }}
            >
              <TileLayer
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                attribution='&copy; OpenStreetMap contributors'
              />

              {/* Mostrar marcadores de paradas */}
              {paradasConCoordenadas.map((parada) => {
                if (!parada.lat || !parada.lng) return null;

                let color = '#888';
                if (parada.tipo === 'ORIGEN') color = '#22c55e';
                else if (parada.tipo === 'DESTINO') color = '#ef4444';
                else if (parada.tipo === 'INTERMEDIA') color = '#f97316';

                return (
                  <CircleMarker
                    key={parada.id}
                    center={[parada.lat, parada.lng]}
                    radius={10}
                    fillColor={color}
                    color={color}
                    weight={2}
                    opacity={1}
                    fillOpacity={0.8}
                  >
                    <Tooltip>{parada.localizacion}</Tooltip>
                  </CircleMarker>
                );
              })}

              {/* Línea de ruta */}
              {routeLine.length > 1 && (
                <Polyline positions={routeLine} color="blue" weight={3} opacity={0.7} />
              )}
            </MapContainer>
          </div>
          <div className="p-4 bg-slate-50 text-sm text-slate-600 text-center">
            <p>
              Los marcadores muestran el origen
              <span className="inline-block ml-1 text-green-600 font-semibold">●</span> (verde), paradas
              intermedias
              <span className="inline-block ml-1 text-orange-600 font-semibold">●</span> (naranja) y destino
              <span className="inline-block ml-1 text-red-600 font-semibold">●</span> (rojo)
            </p>
          </div>
        </div>

        {/* MODAL DE RESERVA */}
        {modalReservaAbierto && (
          <div className="fixed inset-0 z-50 flex items-center justify-center px-4 bg-slate-900/60 backdrop-blur-sm">
            <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full border border-slate-200">
              
              {/* Header Modal */}
              <div className="px-6 py-4 border-b border-slate-200 flex justify-between items-center">
                <h2 className="text-xl font-bold text-slate-900">Reservar viaje</h2>
                <button
                  type="button"
                  onClick={() => setModalReservaAbierto(false)}
                  className="text-slate-400 hover:text-slate-900 text-2xl"
                >
                  ✕
                </button>
              </div>

              {/* Body Modal */}
              <div className="px-6 py-4">
                {!isLoggedIn ? (
                  /* Usuario NO logueado */
                  <div className="space-y-4">
                    <p className="text-slate-700 mb-4">
                      Debes iniciar sesión o registrarte para poder reservar un viaje.
                    </p>
                    <button
                      type="button"
                      onClick={() => {
                        setModalReservaAbierto(false);
                        navigate('/inicio-sesion');
                      }}
                      className="w-full rounded-lg bg-gradient-compi px-4 py-2 text-sm font-bold text-white hover:opacity-90"
                    >
                      Iniciar sesión
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setModalReservaAbierto(false);
                        navigate('/registro');
                      }}
                      className="w-full rounded-lg border border-slate-300 px-4 py-2 text-sm font-bold text-slate-900 hover:bg-slate-50"
                    >
                      Registrarse
                    </button>
                  </div>
                ) : (
                  /* Usuario logueado */
                  <div className="space-y-4">
                    {/* Información del viaje resumida */}
                    <div className="bg-slate-50 p-3 rounded-lg text-sm">
                      <p><span className="font-semibold">Origen:</span> {origen}</p>
                      <p><span className="font-semibold">Destino:</span> {destino}</p>
                      <p><span className="font-semibold">Fecha:</span> {formatFecha(viaje.fechaHoraSalida)}</p>
                    </div>

                    {/* Selector de plazas */}
                    <div>
                      <label className="block text-sm font-semibold text-slate-700 mb-2">Número de plazas</label>
                      <div className="flex gap-2 items-center">
                        <button
                          type="button"
                          onClick={() => setCantidadPlazas(Math.max(1, cantidadPlazas - 1))}
                          disabled={reservando || cantidadPlazas <= 1}
                          className="rounded-lg border border-slate-300 px-3 py-2 font-bold disabled:opacity-50 hover:bg-slate-50"
                        >
                          −
                        </button>
                        <input
                          type="number"
                          min="1"
                          max={viaje.plazasDisponibles}
                          value={cantidadPlazas}
                          onChange={(e) => {
                            const val = Number(e.target.value || 1);
                            if (val >= 1 && val <= viaje.plazasDisponibles) {
                              setCantidadPlazas(val);
                            }
                          }}
                          disabled={reservando}
                          className="w-20 rounded-lg border border-slate-300 px-3 py-2 text-center font-bold"
                        />
                        <button
                          type="button"
                          onClick={() => setCantidadPlazas(Math.min(viaje.plazasDisponibles, cantidadPlazas + 1))}
                          disabled={reservando || cantidadPlazas >= viaje.plazasDisponibles}
                          className="rounded-lg border border-slate-300 px-3 py-2 font-bold disabled:opacity-50 hover:bg-slate-50"
                        >
                          +
                        </button>
                      </div>
                      <p className="text-xs text-slate-500 mt-1">Disponibles: {viaje.plazasDisponibles}</p>
                    </div>

                    {/* Información de precios */}
                    <div className="bg-blue-50 p-3 rounded-lg border border-blue-200 space-y-1 text-sm">
                      <p><span className="text-slate-600">Precio por plaza:</span> <span className="font-bold">{Number(viaje.precio).toFixed(2)}€</span></p>
                      <p className="text-lg"><span className="text-slate-600">Total a cobrar:</span> <span className="font-bold text-blue-600">{(Number(viaje.precio) * cantidadPlazas).toFixed(2)}€</span></p>
                    </div>

                    {/* Checkbox de aceptación */}
                    <label className="flex items-start gap-2 text-sm text-slate-700 p-3 bg-amber-50 rounded-lg border border-amber-200">
                      <input
                        type="checkbox"
                        checked={aceptaBloqueoPago}
                        onChange={(e) => setAceptaBloqueoPago(e.target.checked)}
                        disabled={reservando}
                        className="mt-1 w-4 h-4"
                      />
                      <span>
                        <strong>Entiendo que:</strong> Se cobrará {(Number(viaje.precio) * cantidadPlazas).toFixed(2)}€ de forma inmediata. 
                        Este importe quedará retenido hasta finalizar el viaje, momento en el que se transferirá al conductor.
                      </span>
                    </label>

                    {/* Mensaje de error/éxito */}
                    {reservaMsg && (
                      <div className={`p-3 rounded-lg text-sm ${
                        reservaMsg.includes('✅')
                          ? 'bg-green-50 border border-green-200 text-green-700'
                          : reservaMsg.includes('⚠️')
                            ? 'bg-yellow-50 border border-yellow-200 text-yellow-700'
                            : 'bg-red-50 border border-red-200 text-red-700'
                      }`}>
                        {reservaMsg}
                      </div>
                    )}
                  </div>
                )}
              </div>

              {/* Footer Modal */}
              <div className="px-6 py-4 border-t border-slate-200 flex gap-3 justify-end">
                <button
                  type="button"
                  onClick={() => setModalReservaAbierto(false)}
                  className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-bold text-slate-900 hover:bg-slate-50"
                >
                  Cancelar
                </button>
                {isLoggedIn && (
                  <button
                    type="button"
                    onClick={reservarPlazas}
                    disabled={reservando || !aceptaBloqueoPago || cantidadPlazas < 1}
                    className="rounded-lg bg-gradient-compi px-4 py-2 text-sm font-bold text-white disabled:opacity-60 hover:opacity-90"
                  >
                    {reservando ? 'Procesando...' : `Reservar - ${(Number(viaje.precio) * cantidadPlazas).toFixed(2)}€`}
                  </button>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default DetalleViaje;
