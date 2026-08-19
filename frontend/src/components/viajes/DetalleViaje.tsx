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
import { Elements } from '@stripe/react-stripe-js';
import CheckoutForm from '../pagos/CheckoutForm';
import { loadStripe } from '@stripe/stripe-js';

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
  conductorId?: number; // Añadido para identificar al conductor
  vehiculo: {
    marca: string;
    modelo: string;
    matricula: string;
  };
  paradas: Parada[];
  reservas?: Reserva[];
  checkin?: string; // Añadido opcionalmente por si el viaje contiene el código de check-in
  diasSemana?: string[]; // Para viajes recurrentes
  viajesRecurrentes?: Viaje[]; // Para viajes recurrentes
  fechaFinRecurrencia?: string; // Para viajes recurrentes
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
  const location = useLocation();

  type DetalleNavState = {
    backTo?: string;
    backLabel?: string;
    rol?: 'conductor' | 'pasajero';
    esRecurrente?: boolean;
    esInstanciaRecurrente?: boolean;
    slugPadre?: string;
    viajePadre?: any;
    viajesRecurrentes?: any[];
    usuarioId?: number;
    usuarioActual?: any;
  };

  const navState = (location.state ?? {}) as DetalleNavState;

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
  const [mostrarStripe, setMostrarStripe] = useState(false);
  const [clientSecret, setClientSecret] = useState<string | null>(null);
  const [reservaEnProcesoId, setReservaEnProcesoId] = useState<number | null>(null);
  const [stripePromise] = useState(() => loadStripe('pk_test_51TSKGgAXE3CISlOUTVA8Rt2KEaJ4iJ1GsWXmfrLVY5DzxkgwGRt1YL5S3NnI3igffl3mpFd24TYBweb7baOCMfIh002314JX8u'));
  const [iniciando, setIniciando] = useState(false);
  const [iniciarMsg, setIniciarMsg] = useState<string | null>(null);
  
  // Nuevos estados para el flujo de check-in de pasajeros y global
  const [estadosPasajeros, setEstadosPasajeros] = useState<Record<number, 'PRESENTE' | 'NO_PRESENTADO'>>({});
  const [modalCheckinGlobalAbierto, setModalCheckinGlobalAbierto] = useState(false);
  const [codigoCheckinGlobal, setCodigoCheckinGlobal] = useState('');
  const [verificandoCheckinGlobal, setVerificandoCheckinGlobal] = useState(false);
  const [checkinGlobalMsg, setCheckinGlobalMsg] = useState<string | null>(null);

  // Estados añadidos específicamente para el pop-up individual de presente con código de check-in
  const [modalPresenteAbierto, setModalPresenteAbierto] = useState(false);
  const [reservaSeleccionadaParaPresente, setReservaSeleccionadaParaPresente] = useState<number | null>(null);
  const [codigoCheckinIndividual, setCodigoCheckinIndividual] = useState('');
  const [errorCheckinIndividual, setErrorCheckinIndividual] = useState<string | null>(null);

  const [finalizando, setFinalizando] = useState(false);
  const [finalizarMsg, setFinalizarMsg] = useState<string | null>(null);
  const [modalCancelarReservaAbierto, setModalCancelarReservaAbierto] = useState(false);
  const [modalCancelarViajeAbierto, setModalCancelarViajeAbierto] = useState(false);
  
  // Nuevo estado para el modal de cancelación de viaje recurrente (padre)
  const [modalCancelarViajeRecurrenteAbierto, setModalCancelarViajeRecurrenteAbierto] = useState(false);

  const [reportandoIncomparecencia, setReportandoIncomparecencia] = useState(false);
  const [incomparecenciaMsg, setIncomparecenciaMsg] = useState<string | null>(null);

  const isLoggedIn = !!token && token !== 'undefined' && token !== 'null' && token.trim() !== '';

  const usuarioActual = JSON.parse(localStorage.getItem('perfil') || '{}');
  const usuarioIdActual = usuarioActual.id;

  // Esta es la lógica maestra: compara el ID del usuario con el ID del conductor del viaje
  const esConductor = Boolean(viaje?.conductorId && usuarioIdActual && viaje.conductorId === usuarioIdActual);
  const rolActual: 'conductor' | 'pasajero' = esConductor ? 'conductor' : 'pasajero';

  const esInstanciaRecurrente = Boolean(navState.esInstanciaRecurrente);

  const backLabel = esInstanciaRecurrente
    ? 'Volver a viajes asociados'
    : 'Volver al inicio';

  const volver = () => {
    if (esInstanciaRecurrente) {
      if (navState.slugPadre) {
        navigate(`/viajes/${navState.slugPadre}/asociados`, { state: navState });
      } else {
        navigate(-1);
      }
    } else {
      navigate('/');
    }
  };

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

  useEffect(() => {
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

        calcularRutaReal(paradasConCoords);
      }
    };

    obtenerCoordenadas();
  }, [viaje]);

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

      const reservaEncontrada = reservas.find(
        (r: Reserva) => r.viajeId === viaje.id && r.estado !== 'CANCELADA' && r.estado !== 'RECHAZADA'
      );

      setMiReserva(reservaEncontrada || null);

    } catch {
      // Silencio
    }
  };

  useEffect(() => {
    fetchMiReserva();
  }, [viaje, isLoggedIn]);

  const confirmarCancelarReserva = async () => {
    if (!miReserva) return;

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
      setTimeout(() => {
        setModalCancelarReservaAbierto(false);
        setCancelReservaMsg(null);
      }, 1500);

    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error inesperado';
      setCancelReservaMsg(`❌ ${msg}`);
    } finally {
      setCancelandoReserva(false);
    }
  };

  const iniciarViaje = async () => {
    if (!viaje) return;

    setIniciando(true);
    setIniciarMsg(null);
    try {
      const response = await fetch(
        buildApiUrl(`/api/viajes/${viaje.slug}/iniciar`),
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          }
        }
      );

      if (!response.ok) {
        const data = await response.json().catch(() => null);
        const msg = data?.message || 'No se pudo iniciar el viaje';
        throw new Error(msg);
      }

      const viajeActualizado = await response.json();
      setViaje(viajeActualizado);
      setIniciarMsg('✅ El viaje ha sido iniciado correctamente.');
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error al iniciar el viaje';
      setIniciarMsg(`❌ ${msg}`);
    } finally {
      setIniciando(false);
    }
  };

  // Función para manejar el botón PRESENTE de un pasajero llamando al endpoint
  const marcarPresentePasajero = async (reservaId: number) => {
    try {
      const response = await fetch(
        buildApiUrl(`/api/reservas/presentado?reservaId=${reservaId}`),
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          }
        }
      );

      if (!response.ok) {
        const data = await response.json().catch(() => null);
        throw new Error(data?.message || 'No se pudo marcar como presente');
      }

      setEstadosPasajeros(prev => ({ ...prev, [reservaId]: 'PRESENTE' }));
      await fetchViaje();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error al marcar presente';
      console.error(msg);
    }
  };

  const reportarIncomparecenciaConductor = async () => {
    if (!viaje) return;

    setReportandoIncomparecencia(true);
    setIncomparecenciaMsg(null);

    try {
      const response = await fetch(
        buildApiUrl(`/api/viajes/${viaje.slug}/cancelarIncompareceConductor`),
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          }
        }
      );

      if (!response.ok) {
        const data = await response.json().catch(() => null);
        const msg = data?.message || 'No se pudo reportar la incomparecencia';
        throw new Error(msg);
      }

      const viajeActualizado = await response.json();
      setViaje(viajeActualizado);
      setIncomparecenciaMsg('✅ Incomparecencia reportada correctamente. El viaje ha sido cancelado.');
      
      setTimeout(async () => {
        await fetchViaje();
        await fetchMiReserva();
      }, 1500);

    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error inesperado';
      setIncomparecenciaMsg(`❌ ${msg}`);
    } finally {
      setReportandoIncomparecencia(false);
    }
  };

  const handleAceptarCheckinIndividual = async () => {
    if (!reservaSeleccionadaParaPresente || !viaje) return;

    const checkinViaje = viaje.checkin || '';
    const codigoIngresadoLimpiado = codigoCheckinIndividual.trim().toUpperCase();
    const checkinViajeLimpiado = String(checkinViaje).trim().toUpperCase();

    if (codigoIngresadoLimpiado !== checkinViajeLimpiado) {
      setErrorCheckinIndividual('❌ El código introducido no coincide con el check-in del viaje.');
      return;
    }
    setErrorCheckinIndividual(null);
    try {
      await marcarPresentePasajero(reservaSeleccionadaParaPresente);
      setModalPresenteAbierto(false);
      setCodigoCheckinIndividual('');
      setReservaSeleccionadaParaPresente(null);
    } catch {
      setErrorCheckinIndividual('❌ Error al procesar la solicitud.');
    }
  };

  const marcarNoPresentadoPasajero = async (reservaId: number) => {
    try {
      const response = await fetch(
        buildApiUrl(`/api/reservas/noPresentado?reservaId=${reservaId}`),
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          }
        }
      );

      if (!response.ok) {
        const data = await response.json().catch(() => null);
        throw new Error(data?.message || 'No se pudo marcar como no presentado');
      }

      setEstadosPasajeros(prev => ({ ...prev, [reservaId]: 'NO_PRESENTADO' }));

      if (viaje && viaje.reservas && viaje.reservas.length === 1) {
        try {
          const cursoResponse = await fetch(
            buildApiUrl(`/api/viajes/${viaje.slug}/en-curso`),
            {
              method: 'PUT',
              headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
              }
            }
          );

          if (!cursoResponse.ok) {
            const data = await cursoResponse.json().catch(() => null);
            throw new Error(data?.message || 'No se pudo poner el viaje en curso automáticamente');
          }

          const viajeActualizado = await cursoResponse.json();
          setViaje(viajeActualizado);
          setIniciarMsg('✅ Único pasajero no presentado. Viaje pasado a EN_CURSO automáticamente.');
        } catch (err) {
          const msg = err instanceof Error ? err.message : 'Error al poner en curso';
          setIniciarMsg(`❌ ${msg}`);
        }
      } else {
        await fetchViaje();
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error al marcar no presentado';
      console.error(msg);
    }
  };

  const confirmarCheckinGlobal = async () => {
    if (!viaje) return;

    const checkinNormalizado = codigoCheckinGlobal.trim();
    if (!checkinNormalizado) {
      setCheckinGlobalMsg('❌ Debes introducir el código de check-in');
      return;
    }

    setVerificandoCheckinGlobal(true);
    setCheckinGlobalMsg(null);

    try {
      const response = await fetch(
        buildApiUrl(`/api/viajes/${viaje.slug}/checkin?checkin=${encodeURIComponent(checkinNormalizado)}`),
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          }
        }
      );

      if (!response.ok) {
        const data = await response.json().catch(() => null);
        const msg = data?.message || 'No se pudo validar el check-in';
        throw new Error(msg);
      }

      const viajeActualizado = await response.json();
      setViaje(viajeActualizado);
      setCodigoCheckinGlobal('');
      setModalCheckinGlobalAbierto(false);
      setCheckinGlobalMsg(null);
      setIniciarMsg('✅ Check-in validado correctamente. El viaje ha pasado a EN_CURSO.');
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error al validar el check-in';
      setCheckinGlobalMsg(`❌ ${msg}`);
    } finally {
      setVerificandoCheckinGlobal(false);
    }
  };

  const finalizarViaje = async () => {
    if (!viaje) return;

    setFinalizando(true);
    setFinalizarMsg(null);

    try {
      const response = await fetch(
        buildApiUrl(`/api/viajes/${viaje.slug}/finalizar`),
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          }
        }
      );

      if (!response.ok) {
        const data = await response.json().catch(() => null);
        const msg = data?.message || 'No se pudo finalizar el viaje';
        throw new Error(msg);
      }

      const viajeActualizado = await response.json();
      setViaje(viajeActualizado);
      setFinalizarMsg('✅ El viaje ha sido finalizado correctamente.');
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error al finalizar el viaje';
      setFinalizarMsg(`❌ ${msg}`);
    } finally {
      setFinalizando(false);
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
        const ruta: Array<[number, number]> = paradasOrdenadas.map((p) => [p.lat!, p.lng!]);
        setRouteLine(ruta);
        return;
      }

      const data = await response.json();
      const route = data?.routes?.[0];
      if (!route?.geometry?.coordinates) {
        const ruta: Array<[number, number]> = paradasOrdenadas.map((p) => [p.lat!, p.lng!]);
        setRouteLine(ruta);
        return;
      }

      const routeCoords: Array<[number, number]> = route.geometry.coordinates.map(
        (pair: [number, number]) => [pair[1], pair[0]]
      );
      setRouteLine(routeCoords);
    } catch {
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

  const esViajeRecurrentePadre = (viaje: Viaje): boolean => {
    const tieneDias = viaje.diasSemana && viaje.diasSemana.length > 0;
    const tieneInstancias = viaje.viajesRecurrentes && viaje.viajesRecurrentes.length > 0;
    
    return Boolean(tieneDias || tieneInstancias);
  };

  const iniciarProcesoPago = async () => {
    setReservaMsg(null);

    if (!aceptaBloqueoPago) {
      setReservaMsg('Debes aceptar el aviso de cobro antes de reservar.');
      return;
    }

    if (!paradaSubidaId || !paradaBajadaId) {
      setReservaMsg('Selecciona los puntos de subida y bajada.');
      return;
    }

    setReservando(true);

    try {
      const resReserva = await fetch(buildApiUrl('/api/reservas/crear'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          viajeId: viaje?.id,
          plazas: cantidadPlazas,
          paradaSubidaId: paradaSubidaId,
          paradaBajadaId: paradaBajadaId,
          esRecurrente: esViajeRecurrentePadre(viaje)
        })
      });

      if (!resReserva.ok) {
        const data = await resReserva.json().catch(() => null);
        throw new Error(data?.message || 'No se pudo crear la reserva');
      }

      const data = await resReserva.json();
      if (!data.clientSecret) throw new Error('No se recibió el clientSecret');

      setReservaEnProcesoId(data.reservaId);
      setClientSecret(data.clientSecret);
      setMostrarStripe(true);

    } catch (err) {
      setReservaMsg(`❌ ${err instanceof Error ? err.message : 'Error inesperado'}`);
    } finally {
      setReservando(false);
    }
  };

  const { origen, destino, paradasIntermedias } = getOrigenDestino(viaje.paradas);

  const reservarPlazas = async () => {
    setReservaMsg('✅ Pago confirmado. ¡Tu plaza está reservada!');
    setReservaEnProcesoId(null);

    setTimeout(async () => {
      await fetchViaje();
      await fetchMiReserva();
      setReservaMsg(null);
      setModalReservaAbierto(false);
    }, 1500);
  };

  const deshacerReservaPorFalloPago = async (motivo: string) => {
    if (!reservaEnProcesoId) {
      setReservaMsg(`❌ ${motivo}`);
      return;
    }

    try {
      const response = await fetch(buildApiUrl(`/api/reservas/anular-pago-fallido?reservaId=${reservaEnProcesoId}`), {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (!response.ok) {
        throw new Error('No se pudo revertir la reserva');
      }

      setReservaEnProcesoId(null);
      setClientSecret(null);
      setMostrarStripe(false);
      setReservaMsg(`❌ ${motivo}`);
    } catch {
      setReservaMsg(`❌ ${motivo}`);
    }
  };
  
  const actualizarReserva = async () => {
    if (!miReserva) {
      setReservaMsg("❌ Error: No se encontró la información de la reserva original.");
      return;
    }

    setReservaMsg(null);
    setReservando(true);

    const reservaId = miReserva.id || (miReserva as any).reservaId;

    if (!paradaSubidaId || !paradaBajadaId || !reservaId) {
      setReservaMsg("❌ Error: Faltan datos obligatorios (ID o Paradas)");
      setReservando(false);
      return;
    }

    try {
      const token = localStorage.getItem('token');

      const response = await fetch(buildApiUrl(`/api/reservas/actualizar/${reservaId}`), {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          viajeId: Number(viaje.id), 
          plazas: Number(cantidadPlazas),
          paradaSubidaId: Number(paradaSubidaId),
          paradaBajadaId: Number(paradaBajadaId)
        })
      });

      if (response.ok) {
        setReservaMsg("✅ Reserva actualizada con éxito");
        setTimeout(async () => {
          setReservaMsg(null);
          await fetchViaje(); 
          await fetchMiReserva();
          setModalReservaAbierto(false); 
        }, 1500);
      } else {
        const errorText = await response.text();
        let errorMessage = "No se pudo actualizar";
        
        try {
          const errorData = JSON.parse(errorText);
          errorMessage = errorData.message || errorMessage;
        } catch {
          errorMessage = errorText || errorMessage;
        }

        setReservaMsg(`❌ Error: ${errorMessage}`);
      }
    } catch (error: any) {
      setReservaMsg(`❌ Error de conexión: ${error.message || 'El servidor no responde'}`);
    } finally {
      setReservando(false);
    }
  };

  // Función para cancelar un viaje individual (o solo el padre si se prefiere)
  const confirmarCancelarViaje = async () => {
    if (!viaje) return;

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
      setViaje(viajeActualizado);
      setCancelMsg('✅ Viaje cancelado correctamente.');
      setTimeout(() => {
        setModalCancelarViajeAbierto(false);
        setModalCancelarViajeRecurrenteAbierto(false); // Cierra también el recurrente si estuviera abierto
        setCancelMsg(null);
      }, 1500);

    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error al cancelar viaje';
      setCancelMsg(`❌ ${msg}`);
    } finally {
      setCancelando(false);
    }
  };

  // Función para cancelar el viaje en conjunto (usando tu segundo endpoint)
  const confirmarCancelarViajeConjunto = async () => {
    if (!viaje) return;

    setCancelando(true);
    setCancelMsg(null);

    try {
      const response = await fetch(
        buildApiUrl(`/api/viajes/${viaje.slug}/cancelar-conjunto`),
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
        const msg = data?.error || data?.message || 'No se pudo cancelar el viaje en conjunto';
        throw new Error(msg);
      }

      const viajeActualizado = await response.json();
      setViaje(viajeActualizado);
      setCancelMsg('✅ Viajes cancelados en conjunto correctamente.');
      setTimeout(() => {
        setModalCancelarViajeRecurrenteAbierto(false);
        setCancelMsg(null);
      }, 1500);

    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error al cancelar el conjunto de viajes';
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
    setErrorEdicion(null);

    const plazasOcupadas = viaje?.reservas?.reduce((acc, r) => acc + r.cantidadPlazas, 0) || 0;
    
    if (Number(nuevasPlazas) < plazasOcupadas) {
        setErrorEdicion(`❌ No puedes bajar de ${plazasOcupadas} plazas (ya están reservadas).`);
        return;
    }

    setEditando(true);

    try {
        const url = buildApiUrl(`/api/viajes/${viaje?.slug}`);
        
        const bodyEnvio = {
            fechaHoraSalida: nuevaFecha,
            plazasDisponibles: Number(nuevasPlazas),
            precio: Number(viaje?.precio)
        };

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
            setViaje(viajeActualizado);
            setErrorEdicion("✅ Viaje actualizado con éxito");

            setTimeout(() => {
                setErrorEdicion(null);
                fetchViaje();
                setModalEditarViajeAbierto(false);
            }, 1500);

        } else {
            const errorData = await response.json().catch(() => null);
            const mensajeError = errorData?.message || errorData?.error || 'Error desconocido';
            setErrorEdicion(`❌ Error ${response.status}: ${mensajeError}`);
        }
    } catch {
        setErrorEdicion("❌ Error de conexión. Revisa si el servidor Java está corriendo.");
    } finally {
        setEditando(false);
    }
  };

  const yaEsHoraDeSalida = viaje ? new Date() >= new Date(viaje.fechaHoraSalida) : false;

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
                viaje.estado === 'INICIADO' || viaje.estado === 'EN_CURSO'
                  ? 'bg-blue-100 text-blue-800'
                  : viaje.estado === 'PENDIENTE' || viaje.estado === 'PUBLICADO'
                    ? 'bg-green-100 text-green-800'
                    : viaje.estado === 'FINALIZADO'
                      ? 'bg-gray-100 text-gray-800'
                      : 'bg-red-100 text-red-800'
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
                Pasajeros
              </h3>
              <div className="space-y-3">
                {viaje.reservas.map((res) => {
                  const paradaSubida = viaje.paradas.find(p => p.id === res.paradaSubidaId);
                  const estadoActualPasajero = estadosPasajeros[res.id];

                  return (
                    <div key={res.id} className="flex flex-col md:flex-row items-start md:items-center justify-between p-4 bg-slate-50 rounded-xl border border-slate-200 gap-4">
                      <div className="flex items-center gap-3">
                        <div className="h-10 w-10 rounded-full bg-slate-200 flex items-center justify-center font-bold text-slate-600">
                          {res.nombrePasajero.charAt(0)}
                        </div>
                        <div>
                          <p className="font-semibold text-slate-800">{res.nombrePasajero}</p>
                          <p className="text-xs text-slate-500">
                            {res.cantidadPlazas} plaza(s) • {res.estado}
                          </p>
                          <p className="text-xs font-medium text-indigo-600 mt-0.5">
                            📍 Se sube en: {paradaSubida ? paradaSubida.localizacion : 'Parada no especificada'}
                          </p>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 w-full md:w-auto justify-end">
                        {viaje.estado === 'INICIADO' && (
                          <div className="flex items-center gap-2 mr-2">
                            <button
                              type="button"
                              onClick={() => {
                                setReservaSeleccionadaParaPresente(res.id);
                                setCodigoCheckinIndividual('');
                                setErrorCheckinIndividual(null);
                                setModalPresenteAbierto(true);
                              }}
                              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
                                estadoActualPasajero === 'PRESENTE'
                                  ? 'bg-emerald-600 text-white shadow-sm'
                                  : 'bg-white border border-emerald-600 text-emerald-700 hover:bg-emerald-50'
                              }`}
                            >
                              Presente
                            </button>
                            <button
                              type="button"
                              onClick={() => marcarNoPresentadoPasajero(res.id)}
                              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
                                estadoActualPasajero === 'NO_PRESENTADO'
                                  ? 'bg-rose-600 text-white shadow-sm'
                                  : 'bg-white border border-rose-600 text-rose-700 hover:bg-rose-50'
                              }`}
                            >
                              No presentado
                            </button>
                          </div>
                        )}

                        {res.pasajeroSlug && (
                          <button
                            type="button"
                            onClick={() => navigate(`/usuarios/${res.pasajeroSlug}/perfil`)}
                            className="rounded-full border border-blue-600 bg-white px-4 py-2 text-sm font-semibold text-blue-700 transition hover:bg-blue-100"
                          >
                            Ver perfil público
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
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
            <div className="mb-6 space-y-4">
              {esViajeRecurrentePadre(viaje) ? (
                <div className="bg-amber-50 border border-amber-200 rounded-2xl p-5 shadow-xs">
                  <h4 className="text-amber-900 font-semibold mb-3 flex items-center gap-2 text-base">
                    <span>🔄</span> Configuración de Viaje Recurrente
                  </h4>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4 text-sm text-slate-700">
                    <p>
                      <strong>Días activos:</strong> {viaje.diasSemana?.join(', ')}
                    </p>
                    {viaje.fechaFinRecurrencia && (
                      <p>
                        <strong>Válido hasta:</strong> {formatFecha(viaje.fechaFinRecurrencia)}
                      </p>
                    )}
                  </div>

                  <div className="mt-4 pt-3 border-t border-amber-200/80 flex justify-end">
                    <button
                      type="button"
                      onClick={() => navigate(`/viajes/${viaje.slug}/asociados`, {
                        state: {
                          rol: navState.rol,
                          viajesRecurrentes: viaje.viajesRecurrentes,
                          usuarioActual: usuarioActual,       // <-- Añadido aquí
                          usuarioId: usuarioIdActual,
                          slugPadre: viaje.slug,
                          esRecurrente: true,
                          viajePadre: {
                            id: viaje.id,
                            slug: viaje.slug,
                            origen: viaje.paradas.find(p => p.tipo === 'ORIGEN')?.localizacion || 'Desconocido',
                            destino: viaje.paradas.find(p => p.tipo === 'DESTINO')?.localizacion || 'Desconocido',
                            precio: viaje.precio,
                            diasSemana: viaje.diasSemana,
                            fechaFinRecurrencia: viaje.fechaFinRecurrencia,
                            paradas: viaje.paradas,
                            reservas: viaje.reservas
                          }
                        }
                      })}
                      className="rounded-xl bg-amber-600 px-4 py-2.5 text-sm font-bold text-white shadow-sm hover:bg-amber-700 transition-all flex items-center gap-2"
                    >
                      <span>🔍 Ver viajes asociados ({viaje.viajesRecurrentes?.length || 0})</span>
                    </button>
                  </div>
                </div>
              ) : (
                <div className="bg-slate-50 border border-slate-200 rounded-xl p-4">
                  <p className="text-sm text-slate-700">
                    <strong className="text-slate-900">Fecha y hora de salida:</strong> {formatFecha(viaje.fechaHoraSalida)}
                  </p>
                </div>
              )}

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="bg-slate-50 border border-slate-200 p-4 rounded-xl shadow-xs">
                  <p className="text-xs font-medium text-slate-500 uppercase tracking-wider mb-1">Fecha y hora de salida</p>
                  <p className="text-base font-bold text-slate-900">{formatFecha(viaje.fechaHoraSalida)}</p>
                </div>
                <div className="bg-slate-50 border border-slate-200 p-4 rounded-xl shadow-xs">
                  <p className="text-xs font-medium text-slate-500 uppercase tracking-wider mb-1">Plazas Disponibles</p>
                  <p className="text-base font-bold text-slate-900">{viaje.plazasDisponibles}</p>
                </div>

                <div className="bg-slate-50 border border-slate-200 p-4 rounded-xl shadow-xs">
                  <p className="text-xs font-medium text-slate-500 uppercase tracking-wider mb-1">Precio por plaza</p>
                  <p className="text-base font-bold text-slate-900">{viaje.precio}€</p>
                </div>

                <div className="bg-slate-50 border border-slate-200 p-4 rounded-xl shadow-xs">
                  <p className="text-xs font-medium text-slate-500 uppercase tracking-wider mb-1">Estado actual</p>
                  <p className="text-base font-bold text-slate-900">{viaje.estado}</p>
                </div>
              </div>
            </div>

          {/* SECCIÓN DE BOTONES DINÁMICOS */}
          <div className="space-y-3">
            {navState.rol !== 'conductor' && (
              <>
                {viaje.estado === 'FINALIZADO' ? (
                  <div className="text-center p-4 bg-slate-100 rounded-xl text-slate-600 text-sm italic border border-slate-200">
                    Este viaje ha finalizado. No hay acciones disponibles.
                  </div>
                ) : (!miReserva || miReserva.estado === 'CANCELADA') ? (
                  <button
                    type="button"
                    className="w-full mt-4 rounded-xl bg-gradient-compi px-6 py-3.5 text-base font-bold text-white shadow-lg shadow-indigo-100 hover:opacity-95 transition-all active:scale-[0.98] disabled:bg-slate-300 disabled:shadow-none disabled:cursor-not-allowed"
                    disabled={
                      viaje.plazasDisponibles <= 0 ||
                      viaje.estado === 'CANCELADO' ||
                      viaje.estado === 'INICIADO' ||
                      viaje.estado === 'EN_CURSO' ||
                      yaEsHoraDeSalida
                    }
                    onClick={() => {
                      setReservaMsg(null);
                      setAceptaBloqueoPago(false);
                      setCantidadPlazas(miReserva?.cantidadPlazas || 1);
                      setParadaSubidaId(miReserva?.paradaSubidaId || viaje.paradas.find(p => p.tipo === 'ORIGEN')?.id || null);
                      setParadaBajadaId(miReserva?.paradaBajadaId || viaje.paradas.find(p => p.tipo === 'DESTINO')?.id || null);
                      setModalReservaAbierto(true);
                    }}
                  >
                    {viaje.plazasDisponibles <= 0 ? (
                      '🚫 Sin plazas disponibles'
                    ) : viaje.estado === 'INICIADO' || viaje.estado === 'EN_CURSO' ? (
                      '🚫 Viaje iniciado'
                    ) : yaEsHoraDeSalida ? (
                      '🚫 La hora de salida ya ha pasado'
                    ) : (
                      <span className="flex items-center justify-center gap-2">
                        ✨ {esViajeRecurrentePadre(viaje) ? 'Reservar viajes recurrentes' : 'Reservar ahora'}
                      </span>
                    )}
                  </button>
                ) : (
                  /* SI YA TIENE UNA RESERVA ACTIVA */
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
                      onClick={() => {
                        setCancelReservaMsg(null);
                        setModalCancelarReservaAbierto(true);
                      }}
                      disabled={cancelandoReserva}
                      className="w-full rounded-lg bg-yellow-500 px-6 py-3 text-base font-bold text-white hover:bg-yellow-600 disabled:opacity-60 transition-all shadow-sm"
                    >
                      {cancelandoReserva ? 'Cancelando...' : 'Cancelar mi reserva'}
                    </button>
                    
                    <div className="pt-2">
                      <button
                        type="button"
                        onClick={reportarIncomparecenciaConductor}
                        disabled={reportandoIncomparecencia}
                        className="w-full rounded-lg bg-rose-600 px-6 py-3 text-base font-bold text-white hover:bg-rose-700 disabled:opacity-60 transition-all shadow-sm flex items-center justify-center gap-2"
                      >
                        {reportandoIncomparecencia ? 'Procesando...' : '⚠️ El conductor no se ha presentado'}
                      </button>
                      <p className="mt-1 text-[11px] text-slate-500 text-center">
                        Usa este botón si ha pasado la hora de salida y el conductor no ha acudido.
                      </p>

                      {incomparecenciaMsg && (
                        <div className={`mt-2 p-3 rounded-xl text-xs font-bold border ${
                          incomparecenciaMsg.includes('✅') 
                            ? 'bg-emerald-50 border-emerald-200 text-emerald-700' 
                            : 'bg-red-50 border-red-200 text-red-700'
                        }`}>
                          {incomparecenciaMsg}
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </>
            )}


            {navState.rol === 'conductor' && (
              <div className="space-y-3">
                {viaje.estado !== 'CANCELADO' && viaje.estado !== 'FINALIZADO' && viaje.estado !== 'INICIADO' && viaje.estado !== 'EN_CURSO' && (
                  <div>
                    <button
                      type="button"
                      onClick={iniciarViaje}
                      disabled={iniciando || !yaEsHoraDeSalida}
                      className="w-full rounded-lg bg-emerald-600 px-6 py-3 text-base font-bold text-white hover:bg-emerald-700 disabled:bg-slate-300 disabled:cursor-not-allowed transition-all shadow-md flex items-center justify-center gap-2"
                    >
                      {iniciando ? 'Iniciando viaje...' : '🚀 Iniciar viaje'}
                    </button>

                    {!yaEsHoraDeSalida && (
                      <p className="mt-2 text-xs text-amber-700 bg-amber-50 p-2.5 rounded-lg border border-amber-200 text-center font-medium">
                        ⏳ El botón se activará cuando llegue la hora de salida ({formatFecha(viaje.fechaHoraSalida)}).
                      </p>
                    )}
                  </div>
                )}

                {iniciarMsg && (
                  <div className={`p-3 rounded-xl text-xs font-bold border ${iniciarMsg.includes('✅') ? 'bg-emerald-50 border-emerald-200 text-emerald-700' : 'bg-red-50 border-red-200 text-red-700'}`}>
                    {iniciarMsg}
                  </div>
                )}

                {viaje.estado === 'INICIADO' && (
                  <div className="space-y-2">
                    {(() => {
                      const todosRevisados = Boolean(
                        viaje?.reservas &&
                        viaje.reservas.length > 0 &&
                        viaje.reservas.every(r => estadosPasajeros[r.id] !== undefined || r.estado === 'PRESENTE' || r.estado === 'NO_PRESENTADO')
                      );

                      if (!todosRevisados) {
                        return (
                          <div className="text-center p-3 bg-amber-50 rounded-xl text-amber-700 text-xs font-medium border border-amber-200">
                            ⚠️ Debes revisar el estado (presente o no presentado) de todos los pasajeros antes de continuar.
                          </div>
                        );
                      }

                      const hayPasajeroPresente = viaje.reservas?.some(res => 
                        estadosPasajeros[res.id] === 'PRESENTE' || res.estado === 'PRESENTE'
                      );

                      if (hayPasajeroPresente) {
                        return (
                          <button
                            type="button"
                            onClick={() => {
                              setCheckinGlobalMsg(null);
                              setCodigoCheckinGlobal('');
                              setModalCheckinGlobalAbierto(true);
                            }}
                            className="w-full rounded-lg bg-blue-600 px-6 py-3 text-base font-bold text-white hover:bg-blue-700 transition-all shadow-md flex items-center justify-center gap-2"
                          >
                            📲 Realizar check-in global
                          </button>
                        );
                      } else {
                        return (
                          <button
                            type="button"
                            onClick={async () => {
                              setIniciando(true);
                              setIniciarMsg(null);
                              try {
                                const response = await fetch(
                                  buildApiUrl(`/api/viajes/${viaje.slug}/en-curso`),
                                  {
                                    method: 'PUT',
                                    headers: {
                                      'Content-Type': 'application/json',
                                      'Authorization': `Bearer ${token}`
                                    }
                                  }
                                );

                                if (!response.ok) {
                                  const data = await response.json().catch(() => null);
                                  throw new Error(data?.message || 'No se pudo poner el viaje en curso');
                                }

                                const viajeActualizado = await response.json();
                                setViaje(viajeActualizado);
                                setIniciarMsg('✅ El viaje ha pasado a EN_CURSO automáticamente.');
                              } catch (err) {
                                const msg = err instanceof Error ? err.message : 'Error al poner en curso';
                                setIniciarMsg(`❌ ${msg}`);
                              } finally {
                                setIniciando(false);
                              }
                            }}
                            disabled={iniciando}
                            className="w-full rounded-lg bg-indigo-600 px-6 py-3 text-base font-bold text-white hover:bg-indigo-700 transition-all shadow-md flex items-center justify-center gap-2 disabled:bg-slate-300"
                          >
                            {iniciando ? 'Procesando...' : '⚡ Poner viaje en curso automáticamente'}
                          </button>
                        );
                      }
                    })()}

                    <p className="text-xs text-slate-500 text-center">
                      El viaje ya está iniciado. Gestiona la presencia de los pasajeros para habilitar la opción correspondiente.
                    </p>
                  </div>
                )}

                {viaje.estado === 'EN_CURSO' && (
                  <div>
                    <button
                      type="button"
                      onClick={finalizarViaje}
                      disabled={finalizando}
                      className="w-full rounded-lg bg-slate-800 px-6 py-3 text-base font-bold text-white hover:bg-slate-900 disabled:opacity-60 transition-all shadow-md flex items-center justify-center gap-2"
                    >
                      {finalizando ? 'Finalizando viaje...' : '🏁 Marcar viaje como finalizado'}
                    </button>

                    {finalizarMsg && (
                      <div className={`mt-2 p-3 rounded-xl text-xs font-bold border ${finalizarMsg.includes('✅') ? 'bg-emerald-50 border-emerald-200 text-emerald-700' : 'bg-red-50 border-red-200 text-red-700'}`}>
                        {finalizarMsg}
                      </div>
                    )}
                  </div>
                )}

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

                {(viaje.estado === 'PENDIENTE' || viaje.estado === 'PUBLICADO') && (
                  <button
                    type="button"
                    onClick={() => {
                      if (viaje.fechaFinRecurrencia) {
                        setModalCancelarViajeRecurrenteAbierto(true);
                      } else {
                        setCancelMsg(null);
                        setModalCancelarViajeAbierto(true);
                      }
                    }}
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
              <div className="px-6 py-4 border-b border-slate-200 flex justify-between items-center bg-white rounded-t-2xl">
                <h2 className="text-xl font-bold text-slate-900">
                  {reservaMsg?.includes('✅') 
                    ? (miReserva ? 'Actualización completada' : '¡Reserva realizada!') 
                    : (miReserva 
                        ? 'Modificar mi reserva' 
                        : (esViajeRecurrentePadre(viaje) ? 'Reservar viajes recurrentes' : 'Reservar viaje')
                      )
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
                ) : mostrarStripe && clientSecret ? (
                    <div className="py-4 animate-in fade-in">
                      <Elements stripe={stripePromise} options={{ clientSecret }}>
                          <CheckoutForm 
                            clientSecret={clientSecret} 
                            monto={
                              esViajeRecurrentePadre(viaje) && viaje.viajesRecurrentes && viaje.viajesRecurrentes.length > 0
                                ? cantidadPlazas * (viaje?.precio || 0) * viaje.viajesRecurrentes.length
                                : cantidadPlazas * (viaje?.precio || 0)
                            }
                            onSuccess={(id) => { 
                                setMostrarStripe(false);
                                reservarPlazas(); 
                            }}
                            onError={(message) => {
                              void deshacerReservaPorFalloPago(message || 'El pago no pudo completarse');
                            }}
                          />
                      </Elements>
                    </div>
                  ) : (
                  <div className="space-y-6">
                    {/* Mensaje informativo para viajes recurrentes */}
                    {esViajeRecurrentePadre(viaje) && (
                      <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 text-xs text-amber-900 space-y-2 shadow-xs">
                        <div className="flex items-center gap-1.5 font-bold text-amber-900 text-sm">
                          <span>🔄</span> Reserva múltiple para viajes recurrentes
                        </div>
                        <p className="leading-relaxed">
                          Esta reserva se aplicará a todos los viajes seleccionados de la serie con el mismo número de plazas y la misma parada de subida y bajada.
                        </p>
                        <p className="text-amber-800 italic leading-relaxed pt-1.5 border-t border-amber-200/60">
                          Si quieres cambiar el número de plazas de un viaje concreto o las paradas, puedes modificar la reserva posteriormente o hacerla de manera individual.
                        </p>
                      </div>
                    )}

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

                    <div className="grid grid-cols-1 gap-4 py-2 border-y border-slate-100">
                      <div>
                        <label className="block text-sm font-semibold text-slate-700 mb-1">
                          Punto de subida {esViajeRecurrentePadre(viaje) && '(para todos los viajes)'}
                        </label>
                        <select 
                          value={paradaSubidaId || ''} 
                          onChange={(e) => setParadaSubidaId(Number(e.target.value))}
                          className="w-full rounded-lg border border-slate-300 p-2 text-sm bg-white outline-none focus:ring-2 focus:ring-indigo-500"
                        >
                          <option value="" disabled>Selecciona parada de subida</option>
                          {viaje.paradas
                            .sort((a, b) => a.orden - b.orden)
                            .filter(p => p.tipo !== 'DESTINO')
                            .map(p => (
                              <option key={p.id} value={p.id}>{p.localizacion}</option>
                            ))}
                        </select>
                      </div>

                      <div>
                        <label className="block text-sm font-semibold text-slate-700 mb-1">
                          Punto de bajada {esViajeRecurrentePadre(viaje) && '(para todos los viajes)'}
                        </label>
                        <select 
                          value={paradaBajadaId || ''} 
                          onChange={(e) => setParadaBajadaId(Number(e.target.value))}
                          className="w-full rounded-lg border border-slate-300 p-2 text-sm bg-white outline-none focus:ring-2 focus:ring-indigo-500"
                        >
                          <option value="" disabled>Selecciona parada de bajada</option>
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

                    <div className="space-y-3">
                      <label className="block text-sm font-semibold text-slate-700">
                        Número de plazas {esViajeRecurrentePadre(viaje) && '(para todos los viajes)'}
                      </label>
                      {esViajeRecurrentePadre(viaje) ? (
                        <select
                          value={cantidadPlazas}
                          onChange={(e) => setCantidadPlazas(Number(e.target.value))}
                          className="w-full rounded-lg border border-slate-300 p-2.5 text-sm bg-white outline-none focus:ring-2 focus:ring-indigo-500 font-bold text-slate-800"
                        >
                          {Array.from({ length: Math.min(viaje.plazasDisponibles || 1, 8) }, (_, i) => i + 1).map((num) => (
                            <option key={num} value={num}>
                              {num} {num === 1 ? 'plaza' : 'plazas'}
                            </option>
                          ))}
                        </select>
                      ) : (
                        (() => {
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
                        })()
                      )}
                    </div>

                    <div className="pt-2">
                      {miReserva ? (
                        (() => {
                          const numViajes = (esViajeRecurrentePadre(viaje) && viaje.viajesRecurrentes && viaje.viajesRecurrentes.length > 0) ? viaje.viajesRecurrentes.length : 1;
                          const precioUnitario = Number(viaje?.precio || 0) * numViajes;
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
                          {(() => {
                            const numViajes = (esViajeRecurrentePadre(viaje) && viaje.viajesRecurrentes && viaje.viajesRecurrentes.length > 0) ? viaje.viajesRecurrentes.length : 1;
                            const totalCalculado = cantidadPlazas * (viaje?.precio || 0) * numViajes;

                            return (
                              <>
                                <div className="bg-indigo-50 p-4 rounded-xl border border-indigo-100 flex justify-between items-center">
                                  <div>
                                    <p className="text-[10px] text-indigo-600 font-bold uppercase tracking-widest leading-none mb-1">
                                      Total a pagar ahora {esViajeRecurrentePadre(viaje) && `(${numViajes} viajes)`}
                                    </p>
                                    <p className="text-2xl font-black text-indigo-900">{totalCalculado.toFixed(2)}€</p>
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
                                    Acepto el cargo de <strong>{totalCalculado.toFixed(2)}€</strong> para confirmar mi plaza en {esViajeRecurrentePadre(viaje) ? `los ${numViajes} viajes seleccionados` : 'el viaje'}.
                                  </span>
                                </label>
                              </>
                            );
                          })()}
                        </div>
                      )}
                    </div>

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

              <div className="px-6 py-4 border-t border-slate-200 bg-slate-50 rounded-b-2xl flex flex-col gap-2">
                {isLoggedIn && (
                  (() => {
                    const numViajes = (esViajeRecurrentePadre(viaje) && viaje.viajesRecurrentes && viaje.viajesRecurrentes.length > 0) ? viaje.viajesRecurrentes.length : 1;
                    const precioTotalCalculado = cantidadPlazas * (viaje?.precio || 0) * numViajes;
                    const precioUnitario = Number(viaje?.precio || 0) * numViajes;
                    const diferencia = (cantidadPlazas - (miReserva?.cantidadPlazas || 0)) * precioUnitario;
                    
                    const haCambiadoPlazas = miReserva && cantidadPlazas !== miReserva.cantidadPlazas;
                    const haCambiadoParadas = miReserva && (
                      Number(paradaSubidaId) !== Number(miReserva.paradaSubidaId) || 
                      Number(paradaBajadaId) !== Number(miReserva.paradaBajadaId)
                    );

                    const hayCambios = haCambiadoPlazas || haCambiadoParadas;

                    const botonBloqueado = 
                      reservando || 
                      (miReserva 
                        ? (!hayCambios || (diferencia !== 0 && !aceptaBloqueoPago))
                        : !aceptaBloqueoPago
                      );

                    return (
                      <button
                        type="button"
                        onClick={miReserva ? actualizarReserva : (mostrarStripe ? undefined : iniciarProcesoPago)}
                        disabled={botonBloqueado || (mostrarStripe)}
                        className={`w-full py-3.5 rounded-xl font-bold text-white shadow-lg transition-all active:scale-[0.98] ${
                          botonBloqueado 
                            ? 'bg-slate-300 cursor-not-allowed shadow-none' 
                            : 'bg-gradient-compi hover:opacity-95 shadow-indigo-200'
                        }`}
                      >
                        {mostrarStripe ? "Esperando pago..." 
                        : (miReserva ? "Guardar Cambios" 
                        : `Pagar ${precioTotalCalculado.toFixed(2)}€ y Reservar`)
                      }
                      </button>
                    );
                  })()
                )}
                {mostrarStripe && (
                  <button 
                    onClick={() => setMostrarStripe(false)}
                    className="text-xs text-indigo-600 mt-2"
                  >
                    « Volver a editar reserva
                  </button>
                )}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Modal Editar Viaje */}
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
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-2">Nueva Fecha y Hora</label>
                  <input 
                    type="datetime-local" 
                    className="w-full rounded-lg border border-slate-300 p-2.5 focus:ring-2 focus:ring-indigo-500 outline-none"
                    defaultValue={viaje.fechaHoraSalida.substring(0, 16)} 
                    onChange={(e) => setNuevaFecha(e.target.value)}
                    min={new Date(Date.now() + 12 * 60 * 60 * 1000).toISOString().substring(0, 16)}
                  />
                </div>

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

              <div className="bg-amber-50 p-4 rounded-xl border border-amber-100 flex items-start gap-3">
                <span className="text-amber-600 font-bold">⚠️</span>
                <p className="text-xs text-amber-700 leading-relaxed">
                  La nueva fecha debe ser al menos <strong>12 horas posterior a este momento</strong>. 
                  Si cambias el horario, notificaremos a tus pasajeros actuales para que confirmen si les sigue interesando.
                </p>
              </div>

              {errorEdicion && (
                <div
                  className={`p-3 rounded-xl text-xs font-bold border animate-in fade-in slide-in-from-top-2 ${
                    errorEdicion.includes('✅')
                      ? 'bg-emerald-50 border-emerald-200 text-emerald-700'
                      : 'bg-red-50 border-red-200 text-red-700'
                  }`}
                >
                  {errorEdicion}
                </div>
              )}

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
                  {editando ? 'Guardando...' : errorEdicion?.includes('✅') ? '✨ ¡Todo listo!' : 'Confirmar cambios'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: POP-UP DE VERIFICACIÓN DE CÓDIGO DE CHECK-IN PARA EL BOTÓN PRESENTE */}
      {modalPresenteAbierto && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 backdrop-blur-sm px-4">
          <div className="w-full max-w-md rounded-2xl bg-white shadow-2xl border border-slate-100 overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50">
              <div>
                <h3 className="text-lg font-bold text-slate-900">Verificar Check-in de Pasajero</h3>
                <p className="text-xs text-slate-500">Introduce el código para confirmar la presencia.</p>
              </div>
              <button
                onClick={() => {
                  setModalPresenteAbierto(false);
                  setCodigoCheckinIndividual('');
                  setErrorCheckinIndividual(null);
                }}
                className="text-slate-400 hover:text-slate-900 text-2xl"
              >
                ✕
              </button>
            </div>

            <div className="px-6 py-5 space-y-4">
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">
                  Código del viaje
                </label>
                <input
                  type="text"
                  value={codigoCheckinIndividual}
                  onChange={(e) => setCodigoCheckinIndividual(e.target.value)}
                  placeholder="Introduce el código"
                  className="w-full rounded-xl border border-slate-300 px-4 py-3 text-sm font-medium outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 uppercase tracking-[0.2em]"
                  autoComplete="off"
                />
              </div>

              {errorCheckinIndividual && (
                <div className="p-3 rounded-xl text-xs font-bold border bg-red-50 border-red-200 text-red-700">
                  {errorCheckinIndividual}
                </div>
              )}

              <div className="flex justify-end gap-3 pt-2 border-t border-slate-100">
                <button
                  onClick={() => {
                    setModalPresenteAbierto(false);
                    setCodigoCheckinIndividual('');
                    setErrorCheckinIndividual(null);
                  }}
                  className="px-4 py-2 text-sm font-bold text-slate-600 hover:text-slate-900"
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  onClick={handleAceptarCheckinIndividual}
                  className="rounded-lg bg-emerald-600 px-6 py-2 text-sm font-bold text-white hover:bg-emerald-700 transition-all"
                >
                  Aceptar
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal Checkin Global */}
      {modalCheckinGlobalAbierto && viaje && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 backdrop-blur-sm px-4">
          <div className="w-full max-w-md rounded-2xl bg-white shadow-2xl border border-slate-100 overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50">
              <div>
                <h3 className="text-lg font-bold text-slate-900">Realizar check-in</h3>
                <p className="text-xs text-slate-500">Introduce el código para pasar el viaje a EN_CURSO.</p>
              </div>
              <button
                onClick={() => {
                  setModalCheckinGlobalAbierto(false);
                  setCheckinGlobalMsg(null);
                }}
                className="text-slate-400 hover:text-slate-900 text-2xl"
              >
                ✕
              </button>
            </div>

            <div className="px-6 py-5 space-y-4">
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">
                  Código de check-in
                </label>
                <input
                  type="text"
                  value={codigoCheckinGlobal}
                  onChange={(e) => setCodigoCheckinGlobal(e.target.value)}
                  placeholder="Introduce el código"
                  className="w-full rounded-xl border border-slate-300 px-4 py-3 text-sm font-medium outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 uppercase tracking-[0.2em]"
                  autoComplete="off"
                />
              </div>

              {checkinGlobalMsg && (
                <div className={`p-3 rounded-xl text-xs font-bold border ${checkinGlobalMsg.includes('✅') ? 'bg-emerald-50 border-emerald-200 text-emerald-700' : 'bg-red-50 border-red-200 text-red-700'}`}>
                  {checkinGlobalMsg}
                </div>
              )}

              <div className="flex justify-end gap-3 pt-2 border-t border-slate-100">
                <button
                  onClick={() => {
                    setModalCheckinGlobalAbierto(false);
                    setCheckinGlobalMsg(null);
                  }}
                  className="px-4 py-2 text-sm font-bold text-slate-600 hover:text-slate-900"
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  onClick={confirmarCheckinGlobal}
                  disabled={verificandoCheckinGlobal}
                  className="rounded-lg bg-blue-600 px-6 py-2 text-sm font-bold text-white hover:bg-blue-700 transition-all disabled:bg-slate-300"
                >
                  {verificandoCheckinGlobal ? 'Verificando...' : 'Confirmar check-in'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* MODAL DE CONFIRMACIÓN: CANCELAR RESERVA */}
      {modalCancelarReservaAbierto && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-slate-900/60 backdrop-blur-sm px-4">
          <div className="w-full max-w-md rounded-2xl bg-white shadow-2xl border border-slate-100 overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50">
              <div>
                <h3 className="text-lg font-bold text-slate-900">Cancelar reserva</h3>
                <p className="text-xs text-slate-500">Confirma la cancelación de tus plazas</p>
              </div>
              <button
                type="button"
                onClick={() => {
                  setModalCancelarReservaAbierto(false);
                  setCancelReservaMsg(null);
                }}
                className="text-slate-400 hover:text-slate-900 text-2xl"
              >
                ✕
              </button>
            </div>

            <div className="px-6 py-5 space-y-4">
              <p className="text-sm text-slate-700">
                ¿Estás seguro de que deseas cancelar tu reserva para este viaje? Liberarás tus plazas asignadas.
              </p>

              {cancelReservaMsg && (
                <div className={`p-3 rounded-xl text-xs font-bold border ${
                  cancelReservaMsg.includes('✅') 
                    ? 'bg-emerald-50 border-emerald-200 text-emerald-700' 
                    : 'bg-red-50 border-red-200 text-red-700'
                }`}>
                  {cancelReservaMsg}
                </div>
              )}

              <div className="flex justify-end gap-3 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => {
                    setModalCancelarReservaAbierto(false);
                    setCancelReservaMsg(null);
                  }}
                  className="px-4 py-2 text-sm font-bold text-slate-600 hover:text-slate-900"
                >
                  Volver
                </button>
                <button
                  type="button"
                  onClick={confirmarCancelarReserva}
                  disabled={cancelandoReserva}
                  className="rounded-lg bg-yellow-500 px-6 py-2 text-sm font-bold text-white hover:bg-yellow-600 transition-all disabled:bg-slate-300"
                >
                  {cancelandoReserva ? 'Procesando...' : 'Confirmar cancelación'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* MODAL DE CONFIRMACIÓN: CANCELAR VIAJE */}
      {modalCancelarViajeAbierto && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-slate-900/60 backdrop-blur-sm px-4">
          <div className="w-full max-w-md rounded-2xl bg-white shadow-2xl border border-slate-100 overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50">
              <div>
                <h3 className="text-lg font-bold text-slate-900">Cancelar viaje</h3>
                <p className="text-xs text-slate-500">Confirma la cancelación total del trayecto</p>
              </div>
              <button
                type="button"
                onClick={() => {
                  setModalCancelarViajeAbierto(false);
                  setCancelMsg(null);
                }}
                className="text-slate-400 hover:text-slate-900 text-2xl"
              >
                ✕
              </button>
            </div>

            <div className="px-6 py-5 space-y-4">
              <p className="text-sm text-slate-700">
                ¿Estás seguro de que quieres cancelar este viaje? Esta acción no se puede deshacer y notificará a todos los pasajeros con reservas asociadas.
              </p>

              {cancelMsg && (
                <div className={`p-3 rounded-xl text-xs font-bold border ${
                  cancelMsg.includes('✅') 
                    ? 'bg-emerald-50 border-emerald-200 text-emerald-700' 
                    : 'bg-red-50 border-red-200 text-red-700'
                }`}>
                  {cancelMsg}
                </div>
              )}

              <div className="flex justify-end gap-3 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => {
                    setModalCancelarViajeAbierto(false);
                    setCancelMsg(null);
                  }}
                  className="px-4 py-2 text-sm font-bold text-slate-600 hover:text-slate-900"
                >
                  Volver
                </button>
                <button
                  type="button"
                  onClick={confirmarCancelarViaje}
                  disabled={cancelando}
                  className="rounded-lg bg-red-600 px-6 py-2 text-sm font-bold text-white hover:bg-red-700 transition-all disabled:bg-slate-300"
                >
                  {cancelando ? 'Procesando...' : 'Sí, cancelar viaje'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: CANCELAR VIAJE RECURRENTE (PADRE) */}
      {modalCancelarViajeRecurrenteAbierto && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-slate-900/60 backdrop-blur-sm px-4">
          <div className="w-full max-w-md rounded-2xl bg-white shadow-2xl border border-slate-100 overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50">
              <div>
                <h3 className="text-lg font-bold text-slate-900">Cancelar viaje recurrente</h3>
                <p className="text-xs text-slate-500">Este viaje es un viaje padre de otros viajes</p>
              </div>
              <button
                type="button"
                onClick={() => setModalCancelarViajeRecurrenteAbierto(false)}
                className="text-slate-400 hover:text-slate-900 text-2xl"
              >
                ✕
              </button>
            </div>

            <div className="px-6 py-5 space-y-4">
              <p className="text-sm text-slate-700">
                Has seleccionado cancelar un viaje que contiene una configuración recurrente. ¿Cómo deseas proceder?
              </p>

              {/* Mensaje de estado por si ocurre algún error o éxito */}
              {cancelMsg && (
                <div className={`p-3 rounded-xl text-xs font-bold border ${
                  cancelMsg.includes('✅') 
                    ? 'bg-emerald-50 border-emerald-200 text-emerald-700' 
                    : 'bg-red-50 border-red-200 text-red-700'
                }`}>
                  {cancelMsg}
                </div>
              )}

              <div className="space-y-2 pt-2">
                <button
                  type="button"
                  onClick={confirmarCancelarViaje}
                  disabled={cancelando}
                  className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-all text-left disabled:opacity-50"
                >
                  {cancelando ? 'Procesando...' : '1. Cancelar solo este viaje'}
                </button>

                <button
                  type="button"
                  onClick={confirmarCancelarViajeConjunto}
                  disabled={cancelando}
                  className="w-full rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-700 hover:bg-red-100 transition-all text-left disabled:opacity-50"
                >
                  {cancelando ? 'Procesando...' : '2. Cancelar todos los viajes en conjunto'}
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