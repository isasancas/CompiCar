import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { CircleMarker, MapContainer, Polyline, TileLayer, Tooltip, useMapEvents } from 'react-leaflet';
import { buildApiUrl } from '../../apiConfig';

interface Parada {
  id: number;
  localizacion: string;
  tipo: string;
  orden: number;
}

interface Viaje {
  id: number;
  slug: string;
  fechaHoraSalida: string;
  paradas: Parada[];
  viajePadreId?: number;
  viajesRecurrentes?: Viaje[];
  conductorNombre?: string;
}

interface Reserva {
  id: number;
  viajeId: number;
  estado: string;
}

interface Coordenada {
  lat: number;
  lng: number;
}

const DISTANCIA_MAXIMA_RUTA_METROS = 500;

const distanciaEntrePuntos = (origen: Coordenada, destino: Coordenada) => {
  const latitudMedia = ((origen.lat + destino.lat) / 2) * (Math.PI / 180);
  const diferenciaLatitud = (destino.lat - origen.lat) * 111_320;
  const diferenciaLongitud = (destino.lng - origen.lng) * 111_320 * Math.cos(latitudMedia);
  return Math.sqrt(diferenciaLatitud ** 2 + diferenciaLongitud ** 2);
};

const puntoMasCercanoDeLaRuta = (punto: Coordenada, ruta: Array<[number, number]>) => {
  if (ruta.length === 0) return null;

  let puntoCercano = { lat: ruta[0][0], lng: ruta[0][1] };
  let distanciaMinima = distanciaEntrePuntos(punto, puntoCercano);

  for (let indice = 1; indice < ruta.length; indice += 1) {
    const inicio = { lat: ruta[indice - 1][0], lng: ruta[indice - 1][1] };
    const fin = { lat: ruta[indice][0], lng: ruta[indice][1] };
    const latitudMedia = ((inicio.lat + fin.lat) / 2) * (Math.PI / 180);
    const escalaLongitud = Math.cos(latitudMedia);
    const puntoX = punto.lng * escalaLongitud;
    const puntoY = punto.lat;
    const inicioX = inicio.lng * escalaLongitud;
    const inicioY = inicio.lat;
    const finX = fin.lng * escalaLongitud;
    const finY = fin.lat;
    const vectorX = finX - inicioX;
    const vectorY = finY - inicioY;
    const longitudCuadrado = vectorX ** 2 + vectorY ** 2;
    const proporcion = longitudCuadrado === 0
      ? 0
      : Math.max(0, Math.min(1, ((puntoX - inicioX) * vectorX + (puntoY - inicioY) * vectorY) / longitudCuadrado));
    const candidato = {
      lat: inicio.lat + proporcion * (fin.lat - inicio.lat),
      lng: inicio.lng + proporcion * (fin.lng - inicio.lng)
    };
    const distancia = distanciaEntrePuntos(punto, candidato);

    if (distancia < distanciaMinima) {
      distanciaMinima = distancia;
      puntoCercano = candidato;
    }
  }

  return { punto: puntoCercano, distanciaMetros: distanciaMinima };
};

function MapClickHandler({ onPick }: { onPick: (lat: number, lng: number) => void }) {
  useMapEvents({
    click(event) {
      onPick(event.latlng.lat, event.latlng.lng);
    }
  });
  return null;
}

const SolicitarNuevaParada: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const token = localStorage.getItem('token') || '';
  const [viaje, setViaje] = useState<Viaje | null>(null);
  const [reserva, setReserva] = useState<Reserva | null>(null);
  const [coordenadas, setCoordenadas] = useState<(Parada & Coordenada)[]>([]);
  const [ruta, setRuta] = useState<Array<[number, number]>>([]);
  const [centro, setCentro] = useState<[number, number]>([40.4168, -3.7038]);
  const [localizacion, setLocalizacion] = useState('');
  const [fechaHora, setFechaHora] = useState('');
  const [puntoSolicitado, setPuntoSolicitado] = useState<Coordenada | null>(null);
  const [mostrarConfirmacion, setMostrarConfirmacion] = useState(false);
  const [solicitudPendiente, setSolicitudPendiente] = useState(false);
  const [cargando, setCargando] = useState(true);
  const [buscando, setBuscando] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [mensaje, setMensaje] = useState<string | null>(null);

  useEffect(() => {
    const cargar = async () => {
      if (!slug) return;
      try {
        const [viajeResponse, reservasResponse] = await Promise.all([
          fetch(buildApiUrl(`/api/viajes/publicos/${slug}`)),
          fetch(buildApiUrl('/api/reservas/mis-reservas'), {
            headers: { Authorization: `Bearer ${token}` }
          })
        ]);
        if (!viajeResponse.ok || !reservasResponse.ok) throw new Error('No se pudo cargar el viaje o la reserva');
        const viajeData = await viajeResponse.json() as Viaje;
        const reservas = await reservasResponse.json() as Reserva[];
        const reservaActiva = reservas.find(
          item => item.viajeId === viajeData.id && item.estado === 'CONFIRMADA'
        );
        if (!reservaActiva) throw new Error('Necesitas una reserva confirmada para solicitar una parada');
        const solicitudResponse = await fetch(
          buildApiUrl(`/api/paradas/solicitud-pendiente?reservaId=${reservaActiva.id}`),
          { headers: { Authorization: `Bearer ${token}` } }
        );
        if (solicitudResponse.ok && await solicitudResponse.json()) {
          setSolicitudPendiente(true);
        }
        setViaje(viajeData);
        setReserva(reservaActiva);
        setFechaHora(viajeData.fechaHoraSalida.slice(0, 16));
      } catch (error) {
        setMensaje(error instanceof Error ? error.message : 'No se pudo cargar la solicitud');
      } finally {
        setCargando(false);
      }
    };
    cargar();
  }, [slug, token]);

  useEffect(() => {
    if (!viaje || viaje.paradas.length === 0) return;
    const geocodificar = async () => {
      const resultado: (Parada & Coordenada)[] = [];
      for (const parada of viaje.paradas) {
        try {
          const response = await fetch(
            `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(parada.localizacion)}&format=json&limit=1`,
            { headers: { 'Accept-Language': 'es' } }
          );
          const lugares = response.ok ? await response.json() : [];
          if (lugares[0]) resultado.push({ ...parada, lat: Number(lugares[0].lat), lng: Number(lugares[0].lon) });
        } catch {
          // Una parada sin coordenadas no impide enviar la solicitud.
        }
      }
      setCoordenadas(resultado);
      if (resultado.length > 0) {
        const latitudes = resultado.map(item => item.lat);
        const longitudes = resultado.map(item => item.lng);
        setCentro([(Math.min(...latitudes) + Math.max(...latitudes)) / 2, (Math.min(...longitudes) + Math.max(...longitudes)) / 2]);
        const puntos = resultado.sort((a, b) => a.orden - b.orden);
        try {
          const response = await fetch(`https://router.project-osrm.org/route/v1/driving/${puntos.map(item => `${item.lng},${item.lat}`).join(';')}?overview=full&geometries=geojson`);
          const data = response.ok ? await response.json() : null;
          const geometria = data?.routes?.[0]?.geometry?.coordinates;
          setRuta(geometria ? geometria.map((point: [number, number]) => [point[1], point[0]]) : puntos.map(item => [item.lat, item.lng]));
        } catch {
          setRuta(puntos.map(item => [item.lat, item.lng]));
        }
      }
    };
    geocodificar();
  }, [viaje]);

  const buscarLocalizacion = async () => {
    if (!localizacion.trim()) return;
    setBuscando(true);
    setMensaje(null);
    try {
      const response = await fetch(`https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(localizacion)}&format=json&limit=1`, { headers: { 'Accept-Language': 'es' } });
      const lugares = response.ok ? await response.json() : [];
      if (!lugares[0]) throw new Error('No se ha encontrado esa localización');
      const lat = Number(lugares[0].lat);
      const lng = Number(lugares[0].lon);
      const resultadoRuta = puntoMasCercanoDeLaRuta({ lat, lng }, ruta);
      if (!resultadoRuta || resultadoRuta.distanciaMetros > DISTANCIA_MAXIMA_RUTA_METROS) {
        throw new Error('La localización debe estar sobre la ruta del viaje o a menos de 500 metros de ella.');
      }
      setPuntoSolicitado(resultadoRuta.punto);
      setCentro([resultadoRuta.punto.lat, resultadoRuta.punto.lng]);
    } catch (error) {
      setMensaje(error instanceof Error ? error.message : 'No se pudo localizar la parada');
    } finally {
      setBuscando(false);
    }
  };

  const seleccionarEnMapa = async (lat: number, lng: number) => {
    setBuscando(true);
    setMensaje(null);
    const resultadoRuta = puntoMasCercanoDeLaRuta({ lat, lng }, ruta);
    if (!resultadoRuta || resultadoRuta.distanciaMetros > DISTANCIA_MAXIMA_RUTA_METROS) {
      setPuntoSolicitado(null);
      setMensaje('Solo puedes seleccionar una parada sobre la ruta o a menos de 500 metros de ella.');
      setBuscando(false);
      return;
    }

    const puntoAjustado = resultadoRuta.punto;
    setPuntoSolicitado(puntoAjustado);
    setCentro([puntoAjustado.lat, puntoAjustado.lng]);
    try {
      const response = await fetch(
        `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${encodeURIComponent(String(puntoAjustado.lat))}&lon=${encodeURIComponent(String(puntoAjustado.lng))}&accept-language=es`,
        { headers: { Accept: 'application/json' } }
      );
      const data = response.ok ? await response.json() : null;
      setLocalizacion(data?.display_name || `${puntoAjustado.lat.toFixed(5)}, ${puntoAjustado.lng.toFixed(5)}`);
    } catch {
      setLocalizacion(`${puntoAjustado.lat.toFixed(5)}, ${puntoAjustado.lng.toFixed(5)}`);
    } finally {
      setBuscando(false);
    }
  };

  const enviarSolicitud = async (event: React.FormEvent) => {
    event.preventDefault();
    if (solicitudPendiente) return;
    if (!reserva || !slug || !localizacion.trim() || !puntoSolicitado) {
      setMensaje('Busca la localización en el mapa antes de enviar la solicitud.');
      return;
    }
    setMostrarConfirmacion(true);
  };

  const confirmarSolicitud = async (aplicarATodaRecurrencia: boolean) => {
    if (!reserva || !slug || !localizacion.trim() || !puntoSolicitado || solicitudPendiente) return;
    setEnviando(true);
    setMostrarConfirmacion(false);
    setMensaje(null);
    try {
      const response = await fetch(buildApiUrl('/api/paradas/solicitud-nueva-parada'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({
          reservaId: reserva.id,
          localizacion: localizacion.trim(),
          fechaHora: new Date(fechaHora).toISOString(),
          latitud: puntoSolicitado.lat,
          longitud: puntoSolicitado.lng,
          todaRecurrencia: aplicarATodaRecurrencia
        })
      });
      if (!response.ok) {
        const data = await response.json().catch(() => null);
        if (response.status === 409) {
          setSolicitudPendiente(true);
        }
        throw new Error(data?.message || 'No se pudo enviar la solicitud');
      }
      setSolicitudPendiente(true);
      navigate(`/viajes/${slug}`, { replace: true });
    } catch (error) {
      setMensaje(error instanceof Error ? error.message : 'No se pudo enviar la solicitud');
    } finally {
      setEnviando(false);
    }
  };

  if (cargando) return <div className="min-h-screen bg-gray-100 p-8 text-center text-slate-600">Cargando solicitud...</div>;
  if (!viaje || !reserva) return <div className="min-h-screen bg-gray-100 p-8 text-center"><p className="mb-4 text-red-600">{mensaje || 'Solicitud no disponible'}</p><button type="button" onClick={() => navigate(-1)} className="rounded-lg bg-slate-800 px-4 py-2 text-white">Volver</button></div>;

  const tieneRecurrencia = Boolean(viaje.viajePadreId || viaje.viajesRecurrentes?.length);

  return (
    <div className="min-h-screen bg-gray-100 px-4 py-8">
      <div className="mx-auto max-w-4xl">
        <button type="button" onClick={() => navigate(`/viajes/${viaje.slug}`)} className="mb-5 text-sm font-semibold text-green-700 hover:underline">← Volver al detalle</button>
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          <h1 className="text-2xl font-bold text-slate-900">Solicitar una nueva parada</h1>
          <p className="mt-1 text-sm text-slate-600">El conductor deberá revisar y aprobar la solicitud antes de modificar la ruta.</p>
          <div className="mt-6 overflow-hidden rounded-xl border border-slate-200">
            <MapContainer center={centro} zoom={6} style={{ height: 360, width: '100%' }} scrollWheelZoom>
              <MapClickHandler onPick={seleccionarEnMapa} />
              <TileLayer attribution="&copy; OpenStreetMap contributors" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
              {ruta.length > 1 && <Polyline positions={ruta} pathOptions={{ color: '#2563eb', weight: 5 }} />}
              {coordenadas.map(parada => <CircleMarker key={parada.id} center={[parada.lat, parada.lng]} radius={8} pathOptions={{ color: parada.tipo === 'ORIGEN' ? '#16a34a' : parada.tipo === 'DESTINO' ? '#dc2626' : '#f97316' }}><Tooltip>{parada.localizacion}</Tooltip></CircleMarker>)}
              {puntoSolicitado && <CircleMarker center={[puntoSolicitado.lat, puntoSolicitado.lng]} radius={10} pathOptions={{ color: '#7c3aed' }}><Tooltip>{localizacion}</Tooltip></CircleMarker>}
            </MapContainer>
          </div>
          <p className="mt-2 text-xs text-slate-500">Haz clic sobre la ruta para marcar la nueva parada, o escríbela abajo y pulsa “Ver en mapa”. Solo se aceptan ubicaciones sobre la ruta o a menos de 500 metros; el punto se ajustará al trayecto existente.</p>
          <form onSubmit={enviarSolicitud} className="mt-6 space-y-4">
            <div>
              <label htmlFor="localizacion" className="mb-1 block text-sm font-semibold text-slate-700">Localización de la nueva parada</label>
              <div className="flex gap-2">
                <input id="localizacion" value={localizacion} onChange={event => { setLocalizacion(event.target.value); setPuntoSolicitado(null); }} placeholder="Ej. Alcalá de Henares" className="min-w-0 flex-1 rounded-lg border border-slate-300 px-3 py-2" />
                <button type="button" onClick={buscarLocalizacion} disabled={buscando} className="rounded-lg border border-blue-600 px-3 py-2 text-sm font-semibold text-blue-700 disabled:opacity-50">{buscando ? 'Buscando...' : 'Ver en mapa'}</button>
              </div>
            </div>
            <div>
              <label htmlFor="fechaHora" className="mb-1 block text-sm font-semibold text-slate-700">Hora prevista de paso</label>
              <input id="fechaHora" type="datetime-local" value={fechaHora} onChange={event => setFechaHora(event.target.value)} className="w-full rounded-lg border border-slate-300 px-3 py-2" required />
            </div>
            {solicitudPendiente && <p className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">Ya tienes una solicitud de parada pendiente para este viaje. Espera a que el conductor la acepte o la rechace.</p>}
            {mensaje && <p className={`rounded-lg p-3 text-sm ${mensaje.startsWith('Solicitud enviada') ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'}`}>{mensaje}</p>}
            <button type="submit" disabled={enviando || solicitudPendiente} className="w-full rounded-lg bg-green-600 px-4 py-3 font-bold text-white hover:bg-green-700 disabled:opacity-50">{enviando ? 'Enviando solicitud...' : 'Confirmar solicitud'}</button>
          </form>
        </div>
      </div>

      {mostrarConfirmacion && (
        <div className="fixed inset-0 z-[1000] flex items-center justify-center bg-slate-950/60 px-4">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl">
            <h2 className="text-xl font-bold text-slate-900">Confirmar nueva parada</h2>
            {tieneRecurrencia ? (
              <>
                <p className="mt-2 text-sm text-slate-600">Este viaje tiene recurrencia. ¿Quieres añadir la parada a este viaje en concreto o a todos los viajes recurrentes en los que tienes una reserva confirmada?</p>
                <div className="mt-5 space-y-3">
                  <button type="button" onClick={() => void confirmarSolicitud(false)} className="w-full rounded-xl border border-slate-200 bg-slate-50 p-4 text-left hover:bg-slate-100">
                    <strong className="block text-slate-900">Solo este viaje</strong>
                    <span className="text-sm text-slate-600">La parada se añadirá únicamente a esta salida.</span>
                  </button>
                  <button type="button" onClick={() => void confirmarSolicitud(true)} className="w-full rounded-xl border border-amber-200 bg-amber-50 p-4 text-left hover:bg-amber-100">
                    <strong className="block text-slate-900">Toda la recurrencia</strong>
                    <span className="text-sm text-slate-600">Se aplicará a las salidas recurrentes donde tienes una reserva confirmada.</span>
                  </button>
                </div>
              </>
            ) : (
              <>
                <p className="mt-2 text-sm text-slate-600">La parada se solicitará únicamente para este viaje. El conductor recibirá la solicitud y podrá aceptarla o rechazarla.</p>
                <button type="button" onClick={() => void confirmarSolicitud(false)} className="mt-5 w-full rounded-xl bg-green-600 p-4 font-bold text-white hover:bg-green-700 disabled:opacity-50">
                  Enviar solicitud para este viaje
                </button>
              </>
            )}
            <button type="button" onClick={() => setMostrarConfirmacion(false)} className="mt-5 w-full rounded-lg px-4 py-2 font-semibold text-slate-600 hover:bg-slate-100">Cancelar</button>
          </div>
        </div>
      )}
    </div>
  );
};

export default SolicitarNuevaParada;
