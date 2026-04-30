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
  reservas?: Reserva[];
}

interface Reserva {
  id: number;
  estado: string;
  viajeId: number;
  personaId: number;
  paradaSubidaId: number;
  paradaBajadaId: number;
  pasajeroSlug?: string; 
  cantidadPlazas: number;
  nombrePasajero: string;
  slug?: string;
  fechaHoraReserva?: string;
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
  const [modalEditarViajeAbierto, setModalEditarViajeAbierto] = useState(false);
  const [editando, setEditando] = useState(false);
  const [paradaSubidaId, setParadaSubidaId] = useState<number | null>(null);
  const [paradaBajadaId, setParadaBajadaId] = useState<number | null>(null);
  const [nuevaFecha, setNuevaFecha] = useState<string>('');
  const [nuevasPlazas, setNuevasPlazas] = useState<number>(0);
  const [errorEdicion, setErrorEdicion] = useState<string | null>(null);


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

    setMiReserva(reservaActualizada);

    setViaje((prev) =>
      prev
        ? { ...prev, plazasDisponibles: prev.plazasDisponibles + miReserva.cantidadPlazas }
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
        body: JSON.stringify({ 
          viajeId: viaje.id, 
          plazas: cantidadPlazas,
          paradaSubidaId: paradaSubidaId,
          paradaBajadaId: paradaBajadaId
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
  
  const actualizarReserva = async () => {
    // 1. Verificación de seguridad para TypeScript y lógica
    if (!miReserva) {
      setReservaMsg("❌ Error: No se encontró la información de la reserva original.");
      return;
    }

    setReservaMsg(null);
    setReservando(true);

    // Aseguramos el ID (por si acaso tu modelo usa reservaId en lugar de id)
    const reservaId = miReserva.id || (miReserva as any).reservaId;

    // DEBUG: Esto es vital para ver qué viaja realmente
    console.log("Datos que saldrán hacia el Backend:", {
      urlId: reservaId,
      plazas: cantidadPlazas,
      subida: paradaSubidaId,
      bajada: paradaBajadaId
    });

    // 2. Validación de campos obligatorios
    if (!paradaSubidaId || !paradaBajadaId || !reservaId) {
      setReservaMsg("❌ Error: Faltan datos obligatorios (ID o Paradas)");
      setReservando(false);
      return;
    }

    try {
      const token = localStorage.getItem('token');
      
      // 3. Petición Fetch
      const response = await fetch(buildApiUrl(`/api/reservas/actualizar/${reservaId}`), {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          id: reservaId, // Algunos backend requieren el ID también en el cuerpo
          cantidadPlazas: Number(cantidadPlazas),
          paradaSubida: { id: Number(paradaSubidaId) },
          paradaBajada: { id: Number(paradaBajadaId) },
          // Incluimos el viaje por si el backend valida la relación
          viaje: { id: viaje.id } 
        })
      });

      if (response.ok) {
        setReservaMsg("✅ Reserva actualizada con éxito");
        // Recarga la página para refrescar los datos del viaje y la reserva
        setTimeout(() => window.location.reload(), 1500);
      } else {
        // Intentamos leer el mensaje de error del backend
        const errorText = await response.text();
        let errorMessage = "No se pudo actualizar";
        
        try {
          const errorData = JSON.parse(errorText);
          errorMessage = errorData.message || errorMessage;
        } catch (e) {
          // Si no es JSON, usamos el texto plano
          errorMessage = errorText || errorMessage;
        }

        console.error("Error del servidor (Status " + response.status + "):", errorMessage);
        setReservaMsg(`❌ Error: ${errorMessage}`);
      }
    } catch (error: any) {
      // 4. Captura de errores de red (CORS, Servidor caído, etc.)
      console.error("DETALLE DEL FALLO DE RED:", error);
      setReservaMsg(`❌ Error de conexión: ${error.message || 'El servidor no responde'}`);
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

const esModificable = (fechaSalida: string) => {
  const ahora = new Date();
  const salida = new Date(fechaSalida);
  const diferenciaMs = salida.getTime() - ahora.getTime();
  const horasRestantes = diferenciaMs / (1000 * 60 * 60);
  return horasRestantes > 12;
};

const handleGuardarCambiosViaje = async () => {
    console.log("Iniciando guardado...");
    setErrorEdicion(null);

    // 1. Calculamos las plazas ocupadas para la validación de seguridad en el Front
    const plazasOcupadas = viaje?.reservas?.reduce((acc, r) => acc + r.cantidadPlazas, 0) || 0;
    
    if (Number(nuevasPlazas) < plazasOcupadas) {
        setErrorEdicion(`❌ No puedes bajar de ${plazasOcupadas} plazas (ya están reservadas).`);
        return;
    }

    setEditando(true);

    try {
        const url = buildApiUrl(`/api/viajes/${viaje?.slug}`);
        
        // Preparamos el cuerpo del JSON para inspeccionarlo en consola
        const bodyEnvio = {
            fechaHoraSalida: nuevaFecha,
            // Enviamos el número que el usuario puso en el input
            plazasDisponibles: Number(nuevasPlazas),
            precio: Number(viaje?.precio)
        };

        console.log("📤 Enviando al Backend:", bodyEnvio);

        const response = await fetch(url, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(bodyEnvio)
        });

        if (response.ok) {
            const viajeActualizado = await response.json();
            console.log("📥 Recibido del Servidor:", viajeActualizado);
            
            // Actualizamos el estado local
            setViaje(viajeActualizado); 
            setModalEditarViajeAbierto(false);

            // 2. Feedback al usuario y recarga
            // Mostramos el valor que el servidor calculó finalmente
            alert(`✅ ¡Actualizado! Plazas resultantes: ${viajeActualizado.plazasDisponibles}`);
            
            // Forzamos recarga para limpiar cualquier desajuste de estado
            window.location.reload();

        } else {
            const errorData = await response.json().catch(() => null);
            const mensajeError = errorData?.message || errorData?.error || 'Error desconocido';
            setErrorEdicion(`❌ Error ${response.status}: ${mensajeError}`);
        }
    } catch (err) {
        console.error("Fallo crítico en la petición:", err);
        setErrorEdicion("❌ Error de conexión. Revisa si el servidor Java está corriendo.");
    } finally {
        setEditando(false);
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
                    {res.pasajeroSlug && ( // <-- Condición para mostrar el botón solo si hay slug
                      <button
                        type="button"
                        onClick={() => navigate(`/usuarios/${res.pasajeroSlug}/perfil`)} // <-- Usamos el slug
                        className="rounded-full border border-blue-600 bg-white px-4 py-2 text-sm font-semibold text-blue-700 transition hover:bg-blue-100"
                      >
                        Ver perfil público
                      </button>
                    )}
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

        {/* SECCIÓN DE BOTONES DINÁMICOS */}
          <div className="space-y-3">
            
            {/* CASO: USUARIO ES EL PASAJERO */}
            {navState.rol !== 'conductor' && (
              <>
                {/* Si NO tiene reserva activa: Botón Reservar */}
                {(!miReserva || miReserva.estado === 'CANCELADA') ? (
                  <button
                    type="button"
                    className="w-full mt-4 rounded-xl bg-gradient-compi px-6 py-3.5 text-base font-bold text-white shadow-lg shadow-indigo-100 hover:opacity-95 transition-all active:scale-[0.98] disabled:bg-slate-300 disabled:shadow-none disabled:cursor-not-allowed"
                    disabled={viaje.plazasDisponibles <= 0}
                    onClick={() => {
                      setReservaMsg(null);
                      setAceptaBloqueoPago(false);
                      
                      // Si miReserva existe (el usuario ya tiene una reserva previa que quiere sobreescribir o retomar)
                      setCantidadPlazas(miReserva?.cantidadPlazas || 1);
                      
                      // Cargamos paradas: de la reserva existente o las de por defecto del viaje
                      setParadaSubidaId(miReserva?.paradaSubidaId || viaje.paradas.find(p => p.tipo === 'ORIGEN')?.id || null);
                      setParadaBajadaId(miReserva?.paradaBajadaId || viaje.paradas.find(p => p.tipo === 'DESTINO')?.id || null);
                      
                      setModalReservaAbierto(true);
                    }}
                  >
                    {viaje.plazasDisponibles > 0 ? (
                      <span className="flex items-center justify-center gap-2">
                        ✨ Reservar ahora
                      </span>
                    ) : (
                      '🚫 Sin plazas disponibles'
                    )}
                  </button>
                ) : (
                  /* Si TIENE reserva: Botones Modificar y Cancelar */
                  <div className="space-y-3">
                    {esModificable(viaje.fechaHoraSalida) ? (
                      <button
                        type="button"
                        onClick={() => {
                          setReservaMsg(null);
                          setAceptaBloqueoPago(false);
                          const idS = miReserva.paradaSubidaId ? Number(miReserva.paradaSubidaId) : null;
                          const idB = miReserva.paradaBajadaId ? Number(miReserva.paradaBajadaId) : null;
                          setCantidadPlazas(miReserva.cantidadPlazas);
                          setParadaSubidaId(idS);
                          setParadaBajadaId(idB);
                          setModalReservaAbierto(true);
                        }}
                        className="w-full rounded-lg bg-blue-600 px-6 py-3 text-base font-bold text-white hover:bg-blue-700 transition-all shadow-sm"
                      >
                        🔄 Modificar mi reserva
                      </button>
                    ) : (
                      <div className="text-center p-3 bg-slate-100 rounded-xl text-slate-500 text-sm italic border border-dashed border-slate-300">
                        La reserva ya no se puede modificar (falta menos de 12h)
                      </div>
                    )}

                    <button
                      type="button"
                      onClick={cancelarReserva}
                      disabled={cancelandoReserva}
                      className="w-full rounded-lg bg-yellow-500 px-6 py-3 text-base font-bold text-white hover:bg-yellow-600 disabled:opacity-60 transition-all shadow-sm"
                    >
                      {cancelandoReserva ? 'Cancelando...' : 'Cancelar mi reserva'}
                    </button>
                  </div>
                )}
              </>
            )}

            {/* CASO: USUARIO ES EL CONDUCTOR */}
            {navState.rol === 'conductor' && (
              <div className="space-y-3">
                {esModificable(viaje.fechaHoraSalida) ? (
                  <button
                    type="button"
                    onClick={() => {
                      setNuevaFecha(viaje.fechaHoraSalida.substring(0, 16));
                      setNuevasPlazas(viaje.plazasDisponibles + (viaje.reservas?.reduce((acc, r) => acc + r.cantidadPlazas, 0) || 0));
                      setErrorEdicion(null);
                      setModalEditarViajeAbierto(true);
                    }}
                    className="w-full rounded-lg bg-indigo-600 px-6 py-3 text-base font-bold text-white hover:bg-indigo-700 transition-all shadow-md"
                  >
                    ✏️ Editar detalles del viaje
                  </button>
                ) : (
                  <div className="text-center p-3 bg-slate-100 rounded-xl text-slate-500 text-sm italic border border-dashed border-slate-300">
                    El viaje está bloqueado para edición (falta menos de 12h)
                  </div>
                )}

                {viaje.estado === 'PENDIENTE' && (
                  <button
                    type="button"
                    onClick={() => { cancelarViaje(); }}
                    disabled={cancelando}
                    className="w-full rounded-lg bg-red-600 px-6 py-3 text-base font-bold text-white hover:bg-red-700 disabled:opacity-60 transition-all shadow-sm"
                  >
                    {cancelando ? 'Cancelando...' : 'Cancelar viaje'}
                  </button>
                )}
              </div>
            )}
          </div>
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
          <div className="fixed inset-0 z-[9999] flex items-center justify-center px-4 bg-slate-900/60 backdrop-blur-sm overflow-hidden">
            <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full border border-slate-200 flex flex-col max-h-[90vh]">
              
              {/* Header Modal (Fijo) */}
              <div className="px-6 py-4 border-b border-slate-200 flex justify-between items-center bg-white rounded-t-2xl">
                <h2 className="text-xl font-bold text-slate-900">
                  {reservaMsg?.includes('✅') 
                    ? (miReserva ? 'Actualización completada' : '¡Reserva realizada!') 
                    : (miReserva ? 'Modificar mi reserva' : 'Reservar viaje')
                  }
                </h2>
                <button
                  type="button"
                  onClick={() => setModalReservaAbierto(false)}
                  className="text-slate-400 hover:text-slate-900 text-2xl p-2"
                >
                  ✕
                </button>
              </div>

              {/* Body Modal (CON SCROLL) */}
              <div className="px-6 py-4 overflow-y-auto flex-1 space-y-6 custom-scrollbar">
                {!isLoggedIn ? (
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
                  <div className="space-y-6">
                    
                    {/* Resumen de ruta visual */}
                    <div className="relative pl-8 py-1">
                      <div className="absolute left-[11px] top-3 bottom-3 w-0.5 border-l-2 border-dashed border-slate-200"></div>
                      
                      <div className="relative mb-4">
                        <div className="absolute -left-[27px] top-1 w-4 h-4 rounded-full border-4 border-white bg-indigo-600 shadow-sm"></div>
                        <p className="text-[10px] font-bold text-slate-400 uppercase leading-none mb-1">Origen</p>
                        <p className="text-sm font-semibold text-slate-700">{origen}</p>
                      </div>

                      <div className="relative">
                        <div className="absolute -left-[27px] top-1 w-4 h-4 rounded-full border-4 border-white bg-emerald-600 shadow-sm"></div>
                        <p className="text-[10px] font-bold text-slate-400 uppercase leading-none mb-1">Destino</p>
                        <p className="text-sm font-semibold text-slate-700">{destino}</p>
                      </div>
                      
                      <div className="mt-4 pt-3 border-t border-slate-100 flex items-center gap-2 text-slate-500">
                        <span className="text-xs">📅 {formatFecha(viaje.fechaHoraSalida)}</span>
                      </div>
                    </div>

                    {/* Selectores de Paradas */}
                    <div className="grid grid-cols-1 gap-4 py-2 border-y border-slate-100">
                      <div>
                        <label className="block text-sm font-semibold text-slate-700 mb-1">Punto de subida</label>
                        <select 
                          value={paradaSubidaId || ''} 
                          onChange={(e) => setParadaSubidaId(Number(e.target.value))}
                          className="w-full rounded-lg border border-slate-300 p-2 text-sm bg-white outline-none focus:ring-2 focus:ring-indigo-500"
                        >
                          {viaje.paradas
                            .sort((a, b) => a.orden - b.orden)
                            .filter(p => p.tipo !== 'DESTINO')
                            .map(p => (
                              <option key={p.id} value={p.id}>{p.localizacion}</option>
                            ))}
                        </select>
                      </div>

                      <div>
                        <label className="block text-sm font-semibold text-slate-700 mb-1">Punto de bajada</label>
                        <select 
                          value={paradaBajadaId || ''} 
                          onChange={(e) => setParadaBajadaId(Number(e.target.value))}
                          className="w-full rounded-lg border border-slate-300 p-2 text-sm bg-white outline-none focus:ring-2 focus:ring-indigo-500"
                        >
                          {viaje.paradas
                            .sort((a, b) => a.orden - b.orden)
                            .filter(p => {
                              const paradaSubida = viaje.paradas.find(s => s.id === paradaSubidaId);
                              return p.tipo !== 'ORIGEN' && (paradaSubida ? p.orden > paradaSubida.orden : true);
                            })
                            .map(p => (
                              <option key={p.id} value={p.id}>{p.localizacion}</option>
                            ))}
                        </select>
                      </div>
                    </div>

                    {/* Selector de plazas */}
                    <div className="space-y-3">
                      <label className="block text-sm font-semibold text-slate-700">Número de plazas</label>
                      {(() => {
                        const misPlazasActuales = miReserva?.cantidadPlazas || 0;
                        const plazasLibresTotales = (viaje?.plazasDisponibles || 0) + misPlazasActuales;

                        return (
                          <div className="flex items-center gap-3">
                            <button
                              type="button"
                              onClick={() => setCantidadPlazas(Math.max(1, cantidadPlazas - 1))}
                              disabled={reservando || cantidadPlazas <= 1}
                              className="rounded-lg border border-slate-300 w-10 h-10 flex items-center justify-center font-bold disabled:opacity-50 hover:bg-slate-50 transition-colors"
                            > − </button>
                            <input
                              type="number"
                              readOnly
                              value={cantidadPlazas}
                              className="w-16 rounded-lg border border-slate-300 h-10 text-center font-bold bg-slate-50"
                            />
                            <button
                              type="button"
                              onClick={() => setCantidadPlazas(Math.min(plazasLibresTotales, cantidadPlazas + 1))}
                              disabled={reservando || cantidadPlazas >= plazasLibresTotales}
                              className="rounded-lg border border-slate-300 w-10 h-10 flex items-center justify-center font-bold disabled:opacity-50 hover:bg-slate-50 transition-colors"
                            > + </button>
                            <span className="text-xs text-slate-500 font-medium">Máximo: {plazasLibresTotales}</span>
                          </div>
                        );
                      })()}
                    </div>

                    {/* SECCIÓN DE PAGO Y CONCILIACIÓN */}
                    <div className="pt-2">
                      {miReserva ? (
                        (() => {
                          const precioUnitario = Number(viaje?.precio || 0);
                          const plazasOriginales = miReserva.cantidadPlazas;
                          const diferencia = (cantidadPlazas - plazasOriginales) * precioUnitario;

                          return (
                            <div className="space-y-4">
                              <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-2">
                                <div className="flex justify-between text-xs text-slate-500 uppercase font-bold tracking-wider">
                                  <span>Importe anterior</span>
                                  <span>{(plazasOriginales * precioUnitario).toFixed(2)}€</span>
                                </div>
                                <div className="flex justify-between text-xs text-slate-500 uppercase font-bold tracking-wider border-b border-slate-200 pb-2">
                                  <span>Nuevo importe</span>
                                  <span>{(cantidadPlazas * precioUnitario).toFixed(2)}€</span>
                                </div>

                                {diferencia !== 0 ? (
                                  <div className={`flex justify-between items-center pt-1 ${diferencia > 0 ? 'text-amber-700' : 'text-green-700'}`}>
                                    <span className="text-xs font-black uppercase">{diferencia > 0 ? 'Cargo adicional:' : 'Devolución:'}</span>
                                    <span className="text-xl font-black">{diferencia > 0 ? '+' : ''}{diferencia.toFixed(2)}€</span>
                                  </div>
                                ) : (
                                  <p className="text-center pt-1 text-xs text-slate-400 italic font-medium">Sin cambios en el coste</p>
                                )}
                              </div>

                              {diferencia !== 0 && (
                                <label className="flex items-start gap-3 p-3 bg-indigo-50 rounded-lg border border-indigo-100 cursor-pointer group">
                                  <input
                                    type="checkbox"
                                    checked={aceptaBloqueoPago}
                                    onChange={(e) => setAceptaBloqueoPago(e.target.checked)}
                                    className="mt-1 h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
                                  />
                                  <span className="text-xs text-slate-700 leading-snug group-hover:text-slate-900 transition-colors">
                                    <strong>Confirmar:</strong> Entiendo que se realizará un {diferencia > 0 ? 'cargo' : 'reembolso'} de <strong>{Math.abs(diferencia).toFixed(2)}€</strong> de forma inmediata.
                                  </span>
                                </label>
                              )}
                            </div>
                          );
                        })()
                      ) : (
                        <div className="space-y-4">
                          <div className="bg-indigo-50 p-4 rounded-xl border border-indigo-100 flex justify-between items-center">
                            <div>
                              <p className="text-[10px] text-indigo-600 font-bold uppercase tracking-widest leading-none mb-1">Total a pagar ahora</p>
                              <p className="text-2xl font-black text-indigo-900">{(cantidadPlazas * (viaje?.precio || 0)).toFixed(2)}€</p>
                            </div>
                          </div>
                          <label className="flex items-start gap-3 p-3 bg-amber-50 rounded-lg border border-amber-200 cursor-pointer">
                            <input
                              type="checkbox"
                              checked={aceptaBloqueoPago}
                              onChange={(e) => setAceptaBloqueoPago(e.target.checked)}
                              className="mt-1 h-4 w-4 rounded border-slate-300 text-amber-600 focus:ring-amber-500"
                            />
                            <span className="text-xs text-slate-700 leading-snug">
                              Acepto el cargo de <strong>{(cantidadPlazas * (viaje?.precio || 0)).toFixed(2)}€</strong> para confirmar mi plaza en el viaje.
                            </span>
                          </label>
                        </div>
                      )}
                    </div>

                    {/* Mensaje de error/éxito */}
                    {reservaMsg && (
                      <div className={`p-3 rounded-xl text-xs font-bold border animate-in fade-in slide-in-from-top-2 ${
                        reservaMsg.includes('✅') || reservaMsg.toLowerCase().includes('éxito') 
                          ? 'bg-emerald-50 border-emerald-200 text-emerald-700' 
                          : 'bg-red-50 border-red-200 text-red-700'
                      }`}>
                        {reservaMsg}
                      </div>
                    )}
                  </div>
                )}
              </div>

              {/* Footer Modal (Fijo abajo) */}
              <div className="px-6 py-4 border-t border-slate-200 bg-slate-50 rounded-b-2xl flex flex-col gap-2">
                {isLoggedIn && (
                  (() => {
                    const precioUnitario = Number(viaje?.precio || 0);
                    const diferencia = (cantidadPlazas - (miReserva?.cantidadPlazas || 0)) * precioUnitario;
                    
                    // 1. Detectar si hay algún cambio real (Plazas O Paradas)
                    const haCambiadoPlazas = miReserva && cantidadPlazas !== miReserva.cantidadPlazas;
                    const haCambiadoParadas = miReserva && (
                      Number(paradaSubidaId) !== Number(miReserva.paradaSubidaId) || 
                      Number(paradaBajadaId) !== Number(miReserva.paradaBajadaId)
                    );

                    const hayCambios = haCambiadoPlazas || haCambiadoParadas;

                    // 2. Lógica del Botón:
                    // Si es edición (miReserva existe), se bloquea si:
                    // - No hay ningún cambio (hayCambios es false)
                    // - O si hay cambio de precio (diferencia !== 0) y no ha marcado el check
                    // Si es reserva nueva, se bloquea si no ha marcado el check.
                    const botonBloqueado = 
                      reservando || 
                      (miReserva 
                        ? (!hayCambios || (diferencia !== 0 && !aceptaBloqueoPago))
                        : !aceptaBloqueoPago
                      );

                    return (
                      <button
                        type="button"
                        onClick={miReserva ? actualizarReserva : reservarPlazas}
                        disabled={botonBloqueado}
                        className={`w-full py-3.5 rounded-xl font-bold text-white shadow-lg transition-all active:scale-[0.98] ${
                          botonBloqueado 
                            ? 'bg-slate-300 cursor-not-allowed shadow-none' 
                            : 'bg-gradient-compi hover:opacity-95 shadow-indigo-200'
                        }`}
                      >
                        {reservando ? (
                          <span className="flex items-center justify-center gap-2">
                            <svg className="animate-spin h-5 w-5 text-white" viewBox="0 0 24 24">
                              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                            </svg>
                            Procesando...
                          </span>
                        ) : reservaMsg?.includes('✅') ? (
                          "✨ ¡Todo listo!"
                        ) : miReserva ? (
                          "Confirmar y Guardar Cambios"
                        ) : (
                          `Pagar ${(cantidadPlazas * (viaje?.precio || 0)).toFixed(2)}€ y Reservar`
                        )}
                      </button>
                    );
                  })()
                )}
                <button
                  type="button"
                  onClick={() => setModalReservaAbierto(false)}
                  className="w-full py-2 text-xs font-bold text-slate-400 hover:text-slate-600 transition-colors"
                >
                  Cancelar y volver
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
      {modalEditarViajeAbierto && viaje && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center px-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto border border-slate-200">
            
            <div className="sticky top-0 bg-white px-6 py-4 border-b border-slate-200 flex justify-between items-center z-10">
              <h2 className="text-xl font-bold text-slate-900">Editar mi viaje</h2>
              <button onClick={() => setModalEditarViajeAbierto(false)} className="text-slate-400 hover:text-slate-900 text-2xl">✕</button>
            </div>

            <div className="p-6 space-y-6">
              <p className="text-sm text-slate-500 italic">
                Por seguridad de los pasajeros, solo puedes aumentar plazas y modificar el horario con antelación.
              </p>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* CAMPO FECHA/HORA */}
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-2">Nueva Fecha y Hora</label>
                  <input 
                    type="datetime-local" 
                    className="w-full rounded-lg border border-slate-300 p-2.5 focus:ring-2 focus:ring-indigo-500 outline-none"
                    defaultValue={viaje.fechaHoraSalida.substring(0, 16)} 
                    onChange={(e) => setNuevaFecha(e.target.value)}
                    /* Ponemos un mínimo de hoy + 12 horas para guiar al usuario */
                    min={new Date(Date.now() + 12 * 60 * 60 * 1000).toISOString().substring(0, 16)}
                  />
                </div>

                {/* CAMPO PLAZAS (SOLO AUMENTAR) */}
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-2">
                    Plazas totales disponibles
                  </label>
                  <input 
                    type="number" 
                    className="w-full rounded-lg border border-slate-300 p-2.5 focus:ring-2 focus:ring-indigo-500 outline-none bg-slate-50"
                    value={nuevasPlazas} 
                    min={viaje.reservas?.reduce((acc, r) => acc + r.cantidadPlazas, 0) || 0}
                    onChange={(e) => setNuevasPlazas(parseInt(e.target.value) || 0)}
                  />
                  <p className="text-[10px] text-slate-400 mt-1">
                    Actualmente tienes {viaje.plazasDisponibles} plazas. Solo puedes añadir más.
                  </p>
                </div>
              </div>

              {/* ALERTA DE INFORMACIÓN */}
              <div className="bg-amber-50 p-4 rounded-xl border border-amber-100 flex items-start gap-3">
                <span className="text-amber-600 font-bold">⚠️</span>
                <p className="text-xs text-amber-700 leading-relaxed">
                  La nueva fecha debe ser al menos <strong>12 horas posterior a este momento</strong>. 
                  Si cambias el horario, notificaremos a tus pasajeros actuales para que confirmen si les sigue interesando.
                </p>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
                <button 
                  onClick={() => setModalEditarViajeAbierto(false)}
                  className="px-4 py-2 text-sm font-bold text-slate-600 hover:text-slate-900"
                >
                  Cancelar
                </button>
                <button 
                  className="rounded-lg bg-indigo-600 px-6 py-2 text-sm font-bold text-white hover:bg-indigo-700 transition-all disabled:bg-slate-300"
                  disabled={editando}
                  onClick={handleGuardarCambiosViaje}
                >
                  {editando ? 'Guardando...' : 'Confirmar cambios'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DetalleViaje;
