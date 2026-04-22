import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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
  vehiculo: {
    marca: string;
    modelo: string;
    matricula: string;
  };
  paradas: Parada[];
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

  useEffect(() => {
    const fetchViaje = async () => {
      if (!slug || !token) {
        setError('No se pudo cargar el viaje');
        setLoading(false);
        return;
      }

      try {
        const response = await fetch(buildApiUrl(`/api/viajes/${slug}`), {
          headers: { Authorization: `Bearer ${token}` }
        });

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
  }, [slug, token]);

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
            onClick={() => navigate('/mis-viajes')}
            className="bg-gradient-compi hover:opacity-90 text-white font-bold py-2 px-4 rounded"
          >
            Volver a Mis Viajes
          </button>
        </div>
      </div>
    );
  }

  const { origen, destino, paradasIntermedias } = getOrigenDestino(viaje.paradas);

  return (
    <div className="min-h-screen bg-gray-100 pb-10 pt-6">
      <div className="mx-auto max-w-4xl px-4">
        <button
          type="button"
          onClick={() => navigate('/mis-viajes')}
          className="rounded-full border border-green-600 px-4 py-1 text-sm text-green-700 transition hover:bg-green-50 mb-6"
        >
          ← Volver a Mis Viajes
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
      </div>
    </div>
  );
};

export default DetalleViaje;
