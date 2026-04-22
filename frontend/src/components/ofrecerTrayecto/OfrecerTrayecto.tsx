import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  MapContainer,
  TileLayer,
  CircleMarker,
  Polyline,
  Tooltip,
  useMapEvents
} from 'react-leaflet';
import { buildApiUrl } from '../../apiConfig';

type TipoParada = 'ORIGEN' | 'INTERMEDIA' | 'DESTINO';

type Parada = {
  localizacion: string;
  lat?: number;
  lng?: number;
};

type Vehiculo = {
  id: number;
  marca: string;
  modelo: string;
  matricula: string;
  plazas: number;
  consumo: number;
};

type Horquilla = {
  min: number;
  max: number;
  fuente: string;
  detalle: string;
};

function MapClickHandler({ onPick }: { onPick: (lat: number, lng: number) => void }) {
  useMapEvents({
    click(e) {
      onPick(e.latlng.lat, e.latlng.lng);
    }
  });
  return null;
}

const diasSemana = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];

const OfrecerTrayecto: React.FC = () => {
  const navigate = useNavigate();

  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([]);
  const [vehiculoId, setVehiculoId] = useState<number | null>(null);

  const [fecha, setFecha] = useState('');
  const [hora, setHora] = useState('');
  const [repetir, setRepetir] = useState(false);
  const [diasSeleccionados, setDiasSeleccionados] = useState<string[]>([]);

  const [plazasDisponibles, setPlazasDisponibles] = useState(2);

  const [origen, setOrigen] = useState<Parada>({ localizacion: '' });
  const [destino, setDestino] = useState<Parada>({ localizacion: '' });
  const [intermedias, setIntermedias] = useState<Parada[]>([]);

  const [targetMapa, setTargetMapa] = useState<TipoParada>('ORIGEN');
  const [intermediaActiva, setIntermediaActiva] = useState<number>(0);

  const [routeLine, setRouteLine] = useState<Array<[number, number]>>([]);
  const [distanciaRutaKm, setDistanciaRutaKm] = useState<number | null>(null);
  const [distanciaManualKm, setDistanciaManualKm] = useState('');

  const [horquilla, setHorquilla] = useState<Horquilla | null>(null);
  const [precioElegido, setPrecioElegido] = useState('');

  const [loadingVehiculos, setLoadingVehiculos] = useState(false);
  const [calculandoPrecio, setCalculandoPrecio] = useState(false);
  const [publicando, setPublicando] = useState(false);

  const [mapError, setMapError] = useState('');
  const [error, setError] = useState('');
  const [okMsg, setOkMsg] = useState('');

  const token = localStorage.getItem('token') || '';

  const vehiculoSeleccionado = useMemo(
    () => vehiculos.find((v) => v.id === vehiculoId) || null,
    [vehiculos, vehiculoId]
  );

  useEffect(() => {
    const fetchVehiculos = async () => {
      if (!token) return;
      setLoadingVehiculos(true);
      try {
        const res = await fetch(buildApiUrl('/api/vehiculos/propios'), {
          headers: {
            Authorization: `Bearer ${token}`
          }
        });

        if (!res.ok) {
          throw new Error('No se pudieron cargar los vehículos');
        }

        const data: Vehiculo[] = await res.json();
        setVehiculos(data);

        if (data.length > 0) {
          setVehiculoId(data[0].id);
          setPlazasDisponibles(Math.min(2, data[0].plazas));
        }
      } catch {
        setError('No se pudieron cargar tus vehículos.');
      } finally {
        setLoadingVehiculos(false);
      }
    };

    fetchVehiculos();
  }, [token]);

  useEffect(() => {
    if (!vehiculoSeleccionado) return;
    setPlazasDisponibles((prev) => {
      if (prev < 1) return 1;
      if (prev > vehiculoSeleccionado.plazas) return vehiculoSeleccionado.plazas;
      return prev;
    });
  }, [vehiculoSeleccionado]);

  const coordsParadas = useMemo(() => {
    const coords: Array<{ lat: number; lng: number }> = [];
    if (origen.lat !== undefined && origen.lng !== undefined) {
      coords.push({ lat: origen.lat, lng: origen.lng });
    }
    intermedias.forEach((p) => {
      if (p.lat !== undefined && p.lng !== undefined) {
        coords.push({ lat: p.lat, lng: p.lng });
      }
    });
    if (destino.lat !== undefined && destino.lng !== undefined) {
      coords.push({ lat: destino.lat, lng: destino.lng });
    }
    return coords;
  }, [origen, intermedias, destino]);

  useEffect(() => {
    const drawRoute = async () => {
      setRouteLine([]);
      setDistanciaRutaKm(null);

      if (coordsParadas.length < 2) return;

      try {
        const coords = coordsParadas.map((c) => `${c.lng},${c.lat}`).join(';');
        const url = `https://router.project-osrm.org/route/v1/driving/${coords}?overview=full&geometries=geojson`;

        const res = await fetch(url);
        if (!res.ok) return;

        const data = await res.json();
        const route = data?.routes?.[0];
        if (!route?.geometry?.coordinates) return;

        const line: Array<[number, number]> = route.geometry.coordinates.map(
          (pair: [number, number]) => [pair[1], pair[0]]
        );

        setRouteLine(line);

        if (typeof route.distance === 'number') {
          setDistanciaRutaKm(Number((route.distance / 1000).toFixed(2)));
        }
      } catch {
        // no bloquear flujo
      }
    };

    drawRoute();
  }, [coordsParadas]);

  const reverseGeocode = async (lat: number, lng: number): Promise<string> => {
    try {
      const url =
        `https://nominatim.openstreetmap.org/reverse?format=jsonv2`
        + `&lat=${encodeURIComponent(String(lat))}`
        + `&lon=${encodeURIComponent(String(lng))}`
        + `&accept-language=es`;

      const res = await fetch(url, {
        headers: {
          Accept: 'application/json'
        }
      });

      if (!res.ok) {
        return `${lat.toFixed(5)}, ${lng.toFixed(5)}`;
      }

      const data = await res.json();
      return data?.display_name || `${lat.toFixed(5)}, ${lng.toFixed(5)}`;
    } catch {
      return `${lat.toFixed(5)}, ${lng.toFixed(5)}`;
    }
  };

  const onMapPick = async (lat: number, lng: number) => {
    const localizacion = await reverseGeocode(lat, lng);

    if (targetMapa === 'ORIGEN') {
      setOrigen({ localizacion, lat, lng });
      setTargetMapa('DESTINO');
      setOkMsg('✅ Origen seleccionado. Ahora haz clic para marcar el destino.');
      return;
    }

    if (targetMapa === 'DESTINO') {
      setDestino({ localizacion, lat, lng });
      setTargetMapa('INTERMEDIA');
      setOkMsg('✅ Destino seleccionado. Ahora puedes añadir paradas intermedias (opcional).');
      return;
    }

    // INTERMEDIA
    setIntermedias((prev) => {
      const copy = [...prev];
      if (!copy[intermediaActiva]) copy[intermediaActiva] = { localizacion: '' };
      copy[intermediaActiva] = { localizacion, lat, lng };
      return copy;
    });

    setOkMsg(`✅ Parada intermedia ${intermediaActiva + 1} seleccionada.`);
  };

  const addIntermedia = () => {
    setIntermedias((prev) => [...prev, { localizacion: '' }]);
    setTargetMapa('INTERMEDIA');
    setIntermediaActiva(intermedias.length);
  };

  const removeIntermedia = (idx: number) => {
    setIntermedias((prev) => prev.filter((_, i) => i !== idx));
    setIntermediaActiva(0);
  };

  const updateIntermediaText = (idx: number, value: string) => {
    setIntermedias((prev) => {
      const copy = [...prev];
      copy[idx] = { ...copy[idx], localizacion: value, lat: undefined, lng: undefined };
      return copy;
    });
  };

  const distanciaFinalKm = useMemo(() => {
    if (distanciaRutaKm !== null && distanciaRutaKm > 0) return distanciaRutaKm;
    const manual = Number(distanciaManualKm);
    if (!Number.isNaN(manual) && manual > 0) return manual;
    return 0;
  }, [distanciaRutaKm, distanciaManualKm]);

  const fechaHoraSalida = useMemo(() => {
    if (!fecha || !hora) return '';
    return `${fecha}T${hora}:00`;
  }, [fecha, hora]);

  const handleCalcularPrecio = async () => {
    setError('');
    setOkMsg('');

    if (!vehiculoId) {
      setError('Selecciona un vehículo.');
      return;
    }

    if (distanciaFinalKm <= 0) {
      setError('Indica una distancia válida (por ruta o manual).');
      return;
    }

    setCalculandoPrecio(true);
    try {
      const res = await fetch(buildApiUrl('/api/viajes/precio/calcular'), {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          vehiculoId,
          distanciaKm: distanciaFinalKm
        })
      });

      if (!res.ok) {
        throw new Error();
      }

      const data = await res.json();

      const min = Number(data?.precioMinimoPasajero ?? 0);
      const max = Number(data?.precioMaximoPasajero ?? 0);

      setHorquilla({
        min,
        max,
        fuente: String(data?.fuente || ''),
        detalle: String(data?.detalle || '')
      });

      if (!precioElegido || Number(precioElegido) < min || Number(precioElegido) > max) {
        setPrecioElegido(min.toFixed(2));
      }

      setOkMsg(`Horquilla calculada: ${min.toFixed(2)}€ - ${max.toFixed(2)}€`);
    } catch {
      setError('No se pudo calcular el precio.');
    } finally {
      setCalculandoPrecio(false);
    }
  };

  const handlePublicar = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setOkMsg('');

    if (!vehiculoId) {
      setError('Selecciona un vehículo.');
      return;
    }

    if (!fechaHoraSalida) {
      setError('Selecciona fecha y hora de salida.');
      return;
    }

    if (!origen.localizacion.trim()) {
      setError('Debes indicar una parada inicial.');
      return;
    }

    if (!destino.localizacion.trim()) {
      setError('Debes indicar una parada final.');
      return;
    }

    const precioNum = Number(precioElegido);
    if (Number.isNaN(precioNum) || precioNum <= 0) {
      setError('El precio elegido no es válido.');
      return;
    }

    if (horquilla && (precioNum < horquilla.min || precioNum > horquilla.max)) {
      setError('El precio elegido debe estar dentro de la horquilla.');
      return;
    }

    const paradasPayload = [
      {
        localizacion: origen.localizacion.trim(),
        tipo: 'ORIGEN',
        orden: 1,
        fechaHora: fechaHoraSalida,
        latitud: origen.lat ?? null,
        longitud: origen.lng ?? null
      },
      ...intermedias
        .filter((p) => p.localizacion.trim())
        .map((p, idx) => ({
          localizacion: p.localizacion.trim(),
          tipo: 'INTERMEDIA',
          orden: idx + 2,
          fechaHora: fechaHoraSalida,
          latitud: p.lat ?? null,
          longitud: p.lng ?? null
        })),
      {
        localizacion: destino.localizacion.trim(),
        tipo: 'DESTINO',
        orden: intermedias.filter((p) => p.localizacion.trim()).length + 2,
        fechaHora: fechaHoraSalida,
        latitud: destino.lat ?? null,
        longitud: destino.lng ?? null
      }
    ];

    setPublicando(true);
    try {
      const res = await fetch(buildApiUrl('/api/viajes/crear'), {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          fechaHoraSalida,
          estado: 'PENDIENTE',
          plazasDisponibles,
          precio: precioNum,
          vehiculo: { id: vehiculoId },
          paradas: paradasPayload
        })
      });

      if (!res.ok) {
        const data = await res.json().catch(() => null);
        throw new Error(data?.message || data?.error || 'Error al crear trayecto');
      }

      setOkMsg('Trayecto creado correctamente.');
      setTimeout(() => {
        navigate('/mis-viajes');
      }, 1000);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'No se pudo crear el trayecto.';
      setError(msg);
    } finally {
      setPublicando(false);
    }
  };

  const toggleDia = (dia: string) => {
    setDiasSeleccionados((prev) =>
      prev.includes(dia) ? prev.filter((d) => d !== dia) : [...prev, dia]
    );
  };

  return (
    <div className="min-h-screen bg-gray-100 pb-10">
      <div className="mx-auto max-w-6xl px-3 py-4">
        <button
          type="button"
          onClick={() => navigate('/perfil')}
          className="rounded-full border border-green-600 px-4 py-1 text-sm text-green-700 transition hover:bg-green-50"
        >
          Volver
        </button>

        <h1 className="mt-3 text-4xl font-semibold text-slate-800">Publicar un viaje</h1>

        {error && <p className="mt-3 rounded bg-red-100 px-3 py-2 text-red-700">{error}</p>}
        {okMsg && <p className="mt-3 rounded bg-green-100 px-3 py-2 text-green-700">{okMsg}</p>}
        {mapError && <p className="mt-3 rounded bg-amber-100 px-3 py-2 text-amber-700">{mapError}</p>}

        <form onSubmit={handlePublicar} className="mt-4 space-y-5">
          <section className="rounded-2xl border border-slate-400 bg-gray-100 p-4">
            <h2 className="text-3xl font-medium text-slate-800">1. Resumen del viaje</h2>

            <div className="mt-4 grid gap-4 lg:grid-cols-[1fr_340px]">
              <div className="space-y-3">
                <div>
                  <label className="mb-1 block text-sm font-semibold text-slate-700">Trayecto inicial</label>
                  <input
                    className="w-full rounded-md border border-slate-400 px-3 py-2"
                    value={origen.localizacion}
                    onChange={(e) => setOrigen((prev) => ({ ...prev, localizacion: e.target.value, lat: undefined, lng: undefined }))}
                    placeholder="Ciudad/dirección de salida"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-sm font-semibold text-slate-700">Paradas intermedias</label>
                  <div className="space-y-2">
                    {intermedias.map((p, idx) => (
                      <div key={idx} className="flex gap-2">
                        <input
                          className="w-full rounded-md border border-slate-400 px-3 py-2"
                          value={p.localizacion}
                          onChange={(e) => updateIntermediaText(idx, e.target.value)}
                          placeholder={`Parada intermedia ${idx + 1}`}
                        />
                        <button
                          type="button"
                          className="rounded-md bg-blue-100 hover:bg-blue-200 border border-blue-400 px-3 py-2 text-xs font-medium text-blue-700"
                          onClick={() => {
                            setTargetMapa('INTERMEDIA');
                            setIntermediaActiva(idx);
                            setOkMsg(`Selecciona parada intermedia ${idx + 1} en el mapa...`);
                          }}
                        >
                          Usar en mapa
                        </button>
                        <button
                          type="button"
                          className="rounded-md border border-slate-400 px-3 py-2 text-sm"
                          onClick={() => removeIntermedia(idx)}
                        >
                          Quitar
                        </button>
                      </div>
                    ))}
                    <button
                      type="button"
                      className="rounded-full bg-gradient-compi px-4 py-2 text-sm font-semibold text-white"
                      onClick={addIntermedia}
                    >
                      Añadir parada
                    </button>
                  </div>
                </div>

                <div>
                  <label className="mb-1 block text-sm font-semibold text-slate-700">Punto final</label>
                  <input
                    className="w-full rounded-md border border-slate-400 px-3 py-2"
                    value={destino.localizacion}
                    onChange={(e) => setDestino((prev) => ({ ...prev, localizacion: e.target.value, lat: undefined, lng: undefined }))}
                    placeholder="Ciudad/dirección de llegada"
                  />
                </div>
              </div>

              <div className="rounded-xl border border-slate-400 bg-white p-2">
                <div className="mb-2 rounded border border-slate-300 bg-slate-50 p-2">
                  <p className="text-xs font-semibold text-slate-700">
                    Paso actual:
                    {' '}
                    {targetMapa === 'ORIGEN' ? 'Seleccionando ORIGEN' : targetMapa === 'DESTINO' ? 'Seleccionando DESTINO' : 'Añadiendo INTERMEDIA'}
                  </p>
                  <p className="text-xs text-slate-600">
                    Haz clic en el mapa para colocar el punto.
                  </p>

                  <div className="mt-2 flex flex-wrap gap-2">
                    <button 
                      type="button" 
                      className={`rounded px-3 py-2 text-xs font-semibold transition cursor-pointer ${
                        targetMapa === 'ORIGEN' 
                          ? 'bg-green-600 text-white shadow-md' 
                          : 'bg-green-100 text-green-700 hover:bg-green-200'
                      }`} 
                      onClick={() => setTargetMapa('ORIGEN')}
                    >
                      Editar origen
                    </button>
                    <button 
                      type="button" 
                      className={`rounded px-3 py-2 text-xs font-semibold transition cursor-pointer ${
                        targetMapa === 'DESTINO' 
                          ? 'bg-red-600 text-white shadow-md' 
                          : 'bg-red-100 text-red-700 hover:bg-red-200'
                      }`} 
                      onClick={() => setTargetMapa('DESTINO')}
                    >
                      Editar destino
                    </button>
                    <button
                      type="button"
                      className={`rounded px-3 py-2 text-xs font-semibold transition cursor-pointer ${
                        targetMapa === 'INTERMEDIA' 
                          ? 'bg-blue-600 text-white shadow-md' 
                          : 'bg-blue-100 text-blue-700 hover:bg-blue-200'
                      }`}
                      onClick={() => {
                        if (intermedias.length === 0) addIntermedia();
                        setTargetMapa('INTERMEDIA');
                      }}
                    >
                      Añadir intermedia
                    </button>
                  </div>
                </div>

                <MapContainer
                  center={[40.4168, -3.7038]}
                  zoom={11}
                  style={{ height: 360, width: '100%' }}
                  scrollWheelZoom
                  whenReady={() => setMapError('')}
                >
                  <TileLayer
                    attribution="&copy; OpenStreetMap contributors"
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                  />
                  <MapClickHandler onPick={onMapPick} />

                  {origen.lat !== undefined && origen.lng !== undefined && (
                    <CircleMarker center={[origen.lat, origen.lng]} radius={8} pathOptions={{ color: '#16a34a' }}>
                      <Tooltip permanent direction="top">Origen</Tooltip>
                    </CircleMarker>
                  )}

                  {destino.lat !== undefined && destino.lng !== undefined && (
                    <CircleMarker center={[destino.lat, destino.lng]} radius={8} pathOptions={{ color: '#dc2626' }}>
                      <Tooltip permanent direction="top">Destino</Tooltip>
                    </CircleMarker>
                  )}

                  {intermedias.map((p, idx) =>
                    p.lat !== undefined && p.lng !== undefined ? (
                      <CircleMarker
                        key={idx}
                        center={[p.lat, p.lng]}
                        radius={7}
                        pathOptions={{ color: '#2563eb' }}
                      >
                        <Tooltip permanent direction="top">{`P${idx + 1}`}</Tooltip>
                      </CircleMarker>
                    ) : null
                  )}

                  {routeLine.length > 1 && (
                    <Polyline positions={routeLine} pathOptions={{ color: '#1d4ed8', weight: 5 }} />
                  )}
                </MapContainer>
              </div>
            </div>
          </section>

          <section className="rounded-2xl border border-slate-400 bg-gray-100 p-4">
            <h2 className="text-3xl font-medium text-slate-800">2. ¿Cuándo sales?</h2>

            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              <div>
                <label className="mb-1 block text-sm font-semibold text-slate-700">Fecha</label>
                <input
                  className="w-full rounded-md border border-slate-400 px-3 py-2"
                  type="date"
                  value={fecha}
                  onChange={(e) => setFecha(e.target.value)}
                />
              </div>

              <div>
                <label className="mb-1 block text-sm font-semibold text-slate-700">Hora</label>
                <input
                  className="w-full rounded-md border border-slate-400 px-3 py-2"
                  type="time"
                  value={hora}
                  onChange={(e) => setHora(e.target.value)}
                />
              </div>
            </div>

            <div className="mt-4">
              <label className="flex items-center gap-2 text-slate-700">
                <input type="checkbox" checked={repetir} onChange={(e) => setRepetir(e.target.checked)} />
                ¿Se repite este viaje?
              </label>

              {repetir && (
                <div className="mt-2 flex flex-wrap gap-2">
                  {diasSemana.map((dia) => (
                    <label key={dia} className="flex items-center gap-1 rounded border border-slate-300 px-2 py-1">
                      <input
                        type="checkbox"
                        checked={diasSeleccionados.includes(dia)}
                        onChange={() => toggleDia(dia)}
                      />
                      <span>{dia}</span>
                    </label>
                  ))}
                </div>
              )}
            </div>
          </section>

          <section className="rounded-2xl border border-slate-400 bg-gray-100 p-4">
            <h2 className="text-3xl font-medium text-slate-800">3. Detalles del vehículo y plazas</h2>

            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              <div>
                <label className="mb-1 block text-sm font-semibold text-slate-700">Seleccionar vehículo</label>
                <select
                  className="w-full rounded-md border border-slate-400 px-3 py-2"
                  value={vehiculoId ?? ''}
                  onChange={(e) => setVehiculoId(Number(e.target.value))}
                  disabled={loadingVehiculos || vehiculos.length === 0}
                >
                  {vehiculos.length === 0 && <option value="">Sin vehículos</option>}
                  {vehiculos.map((v) => (
                    <option key={v.id} value={v.id}>
                      {v.marca} {v.modelo} - {v.matricula}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="mb-1 block text-sm font-semibold text-slate-700">Plazas disponibles</label>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    className="rounded border border-slate-400 px-3 py-1"
                    onClick={() => setPlazasDisponibles((p) => Math.max(1, p - 1))}
                  >
                    -
                  </button>
                  <span className="min-w-10 text-center text-lg font-semibold">{plazasDisponibles}</span>
                  <button
                    type="button"
                    className="rounded border border-slate-400 px-3 py-1"
                    onClick={() =>
                      setPlazasDisponibles((p) =>
                        Math.min(vehiculoSeleccionado?.plazas ?? 9, p + 1)
                      )
                    }
                  >
                    +
                  </button>
                </div>
              </div>
            </div>
          </section>

          <section className="rounded-2xl border border-slate-400 bg-gray-100 p-4">
            <h2 className="text-3xl font-medium text-slate-800">4. Precio y distancia</h2>

            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              <div>
                <label className="mb-1 block text-sm font-semibold text-slate-700">Distancia del trayecto (km)</label>
                <input
                  className="w-full rounded-md border border-slate-400 px-3 py-2"
                  type="number"
                  min="0"
                  step="any"
                  value={distanciaRutaKm !== null ? distanciaRutaKm : distanciaManualKm}
                  onChange={(e) => {
                    setDistanciaRutaKm(null);
                    setDistanciaManualKm(e.target.value);
                  }}
                />
                <p className="mt-1 text-xs text-slate-500">
                  Si hay ruta en mapa, se rellena automáticamente.
                </p>
              </div>

              <div>
                <label className="mb-1 block text-sm font-semibold text-slate-700">Precio por pasajero</label>
                <input
                  className={`w-full rounded-md border px-3 py-2 ${
                    horquilla && precioElegido
                      ? Number(precioElegido) < horquilla.min || Number(precioElegido) > horquilla.max
                        ? 'border-red-400 bg-red-50'
                        : 'border-green-400 bg-green-50'
                      : 'border-slate-400'
                  }`}
                  type="number"
                  min={horquilla?.min ?? 0}
                  max={horquilla?.max ?? 1000}
                  step="0.01"
                  value={precioElegido}
                  onChange={(e) => setPrecioElegido(e.target.value)}
                  placeholder="Elige precio"
                />
                {horquilla && precioElegido && (
                  <p className={`text-xs mt-1 ${
                    Number(precioElegido) < horquilla.min || Number(precioElegido) > horquilla.max
                      ? 'text-red-600'
                      : 'text-green-600'
                  }`}>
                    {Number(precioElegido) < horquilla.min || Number(precioElegido) > horquilla.max
                      ? `❌ Fuera de rango. Rango válido: ${horquilla.min.toFixed(2)}€ - ${horquilla.max.toFixed(2)}€`
                      : `✅ Precio válido (${horquilla.min.toFixed(2)}€ - ${horquilla.max.toFixed(2)}€)`
                    }
                  </p>
                )}
              </div>
            </div>

            <div className="mt-4 flex flex-wrap items-center gap-3">
              <button
                type="button"
                className="rounded-full bg-gradient-compi px-6 py-2 font-semibold text-white shadow"
                onClick={handleCalcularPrecio}
                disabled={calculandoPrecio}
              >
                {calculandoPrecio ? 'Calculando...' : 'Calcular'}
              </button>

              {horquilla && (
                <span className="text-sm text-slate-700">
                  Puedes elegir un precio dentro de este rango: {horquilla.min.toFixed(2)}€ - {horquilla.max.toFixed(2)}€
                </span>
              )}
            </div>
          </section>

          <div className="flex justify-center">
            <button
              type="submit"
              className="rounded-full bg-gradient-compi px-8 py-3 text-xl font-bold text-white shadow-lg disabled:opacity-60"
              disabled={publicando}
            >
              {publicando ? 'Publicando...' : 'Crear y publicar trayecto'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default OfrecerTrayecto;