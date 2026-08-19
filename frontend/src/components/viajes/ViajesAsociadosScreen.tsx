import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { loadStripe } from '@stripe/stripe-js';
import { Elements } from '@stripe/react-stripe-js';
import CheckoutForm from '../pagos/CheckoutForm';
import { buildApiUrl } from '../../apiConfig';

interface PerfilData {
  id?: number;
  nombre: string;
  primerApellido: string;
  segundoApellido?: string;
  email: string;
  telefono: string;
  reputacion?: number;
  preferenciasViaje?: string[];
  fondosActuales?: number | string;
  fondosTotales?: number | string;
  numeroCancelaciones?: number;
}

interface ViajeInstancia {
  id: number;
  slug: string;
  fechaHoraSalida: string;
  estado: string;
  plazasDisponibles: number;
  precio: number;
  paradas?: { id: number; localizacion: string; tipo: string; orden: number }[];
  reserva?: Reserva | null;
  reservas?: Reserva[] | null;
}

interface ViajePadreInfo {
  id: number;
  slug: string;
  origen?: string;
  destino?: string;
  precio: number;
  estado: string;
  diasSemana?: string[];
  fechaFinRecurrencia?: string;
  viajesRecurrentes?: ViajeInstancia[];
  paradas?: { id: number; localizacion: string; tipo: string; orden: number }[];
  reservas?: Reserva[] | Reserva | null;
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

const ViajesAsociadosScreen: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const state = (location.state || {}) as {
    viajesRecurrentes?: ViajeInstancia[];
    viajePadre?: ViajePadreInfo;
    slugPadre?: string;
    rol?: string;
    reserva?: Reserva;
    usuarioId?: number;
    usuarioActual?: PerfilData;
  };

  const viajes = state.viajesRecurrentes || [];
  const padre = state.viajePadre || null;
  const slugPadre = state.slugPadre || null;
  const rol = state.rol ? state.rol.toUpperCase() : '';

  const esConductor = rol.includes('CONDUCTOR');

  const [selectedIds, setSelectedIds] = useState<(number | string)[]>([]);
  const [modalReservaAbierto, setModalReservaAbierto] = useState(false);

  // Configuración Genérica para la reserva múltiple
  const [globalPlazas, setGlobalPlazas] = useState<number>(1);
  const [globalSubidaId, setGlobalSubidaId] = useState<number | null>(null);
  const [globalBajadaId, setGlobalBajadaId] = useState<number | null>(null);

  const [stripePromise] = useState(() => loadStripe('pk_test_51TSKGgAXE3CISlOUTVA8Rt2KEaJ4iJ1GsWXmfrLVY5DzxkgwGRt1YL5S3NnI3igffl3mpFd24TYBweb7baOCMfIh002314JX8u'));
  const [clientSecret, setClientSecret] = useState<string | null>(null);
  const [mostrarStripe, setMostrarStripe] = useState(false);
  const [reservando, setReservando] = useState(false);
  const [reservaMsg, setReservaMsg] = useState<string | null>(null);
  const [aceptaBloqueoPago, setAceptaBloqueoPago] = useState(false);

  const token = localStorage.getItem('token') || '';

  const formatFecha = (fecha: string) => {
    return new Date(fecha).toLocaleString('es-ES', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const handleVolver = () => {
    if (slugPadre) {
      navigate(`/viajes/${slugPadre}`, { state: { rol: state.rol } });
    } else {
      navigate(-1);
    }
  };

  const obtenerParadasPorDefecto = (paradas?: { id: number; localizacion: string; tipo: string; orden: number }[]) => {
    if (!paradas || paradas.length === 0) return { subida: null, bajada: null };
    const ordenadas = [...paradas].sort((a, b) => a.orden - b.orden);
    const origen = ordenadas.find(p => p.tipo === 'ORIGEN')?.id || ordenadas[0]?.id || null;
    const destino = ordenadas.find(p => p.tipo === 'DESTINO')?.id || ordenadas[ordenadas.length - 1]?.id || null;
    return { subida: origen, bajada: destino };
  };

  const obtenerUsuarioId = (): number | null => {
    if (state.usuarioId && !isNaN(Number(state.usuarioId))) return Number(state.usuarioId);
    if (state.usuarioActual?.id && !isNaN(Number(state.usuarioActual.id))) return Number(state.usuarioActual.id);

    const possibleKeys = ['userId', 'id', 'user_id', 'usuarioId', 'personaId'];
    for (const key of possibleKeys) {
      const val = localStorage.getItem(key);
      if (val && !isNaN(Number(val))) return Number(val);
    }

    try {
      const tokenStr = localStorage.getItem('token');
      if (tokenStr) {
        const base64Url = tokenStr.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(
          atob(base64).split('').map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')
        );
        const payload = JSON.parse(jsonPayload);
        const tokenUserId = payload.userId || payload.id || payload.sub || payload.personaId || payload.usuarioId;
        if (tokenUserId && !isNaN(Number(tokenUserId))) return Number(tokenUserId);
      }
    } catch (e) {}

    return null;
  };

  const usuarioIdActual = obtenerUsuarioId();

  const esSeleccionableParaCliente = (
    reservasData?: Reserva[] | Reserva | null,
    estadoViaje?: string
  ): boolean => {
    if (esConductor) return false;

    // Si el viaje está cancelado, bloqueamos la selección inmediatamente
    if (estadoViaje && estadoViaje.trim().toUpperCase() === 'CANCELADO') return false;
    if (!reservasData) return true;

    const lista = Array.isArray(reservasData) ? reservasData : [reservasData];
    if (lista.length === 0) return true;

    if (!usuarioIdActual) return true;

    const tieneReservaDelUsuarioActiva = lista.some(r => {
      if (!r) return false;
      const esDelUsuario = Number(r.personaId) === Number(usuarioIdActual);
      const estadoActiva = r.estado ? r.estado.trim().toUpperCase() !== 'CANCELADA' : true;
      return esDelUsuario && estadoActiva;
    });

    return !tieneReservaDelUsuarioActiva;
  };

  const padreSeleccionable = padre ? esSeleccionableParaCliente(padre.reservas, padre.estado) : false;
  const padreTieneReservaActiva = padre && padre.reservas ? !esSeleccionableParaCliente(padre.reservas, padre.estado) : false;
  
  const viajesSeleccionables = viajes.filter(v => esSeleccionableParaCliente(v.reservas || v.reserva, v.estado));

  const totalElementosSeleccionables = viajesSeleccionables.length + (padreSeleccionable ? 1 : 0);
  const isAllSelected = totalElementosSeleccionables > 0 && selectedIds.length === totalElementosSeleccionables;
  const isSomeSelected = selectedIds.length > 0 && !isAllSelected;

  const handleToggleInstancia = (instancia: ViajeInstancia) => {
    if (selectedIds.includes(instancia.id)) {
      setSelectedIds(selectedIds.filter((item) => item !== instancia.id));
    } else {
      setSelectedIds([...selectedIds, instancia.id]);
    }
  };

  const handleTogglePadreUnico = () => {
    if (selectedIds.includes('padre')) {
      setSelectedIds(selectedIds.filter((item) => item !== 'padre'));
    } else {
      setSelectedIds([...selectedIds, 'padre']);
    }
  };

  const handleToggleTodo = () => {
    if (isAllSelected) {
      setSelectedIds([]);
    } else {
      const allIds: (number | string)[] = viajesSeleccionables.map((v) => v.id);
      if (padreSeleccionable) allIds.push('padre');
      setSelectedIds(allIds);
    }
  };

  const instanciasSeleccionadas = viajes.filter((v) => selectedIds.includes(v.id));
  const isPadreSelected = selectedIds.includes('padre');
  const totalSeleccionadosCount = instanciasSeleccionadas.length + (isPadreSelected ? 1 : 0);

  const precioBaseTotal = instanciasSeleccionadas.reduce((acc, v) => acc + v.precio, 0) + (isPadreSelected && padre ? padre.precio : 0);
  const precioTotal = precioBaseTotal * globalPlazas;

  // Obtener la lista de paradas genéricas del viaje padre o de la primera instancia seleccionada
  const paradasGenericas = [...(padre?.paradas || instanciasSeleccionadas[0]?.paradas || viajes[0]?.paradas || [])].sort((a, b) => a.orden - b.orden);
  const paradasSubida = paradasGenericas.filter(p => p.tipo !== 'DESTINO');
  
  const paradaSubidaActual = paradasGenericas.find(p => p.id === globalSubidaId);
  const ordenSubidaActual = paradaSubidaActual ? paradaSubidaActual.orden : -1;
  const paradasBajada = paradasGenericas.filter(p => p.tipo !== 'ORIGEN' && p.orden > ordenSubidaActual);

  // Inicializar/Actualizar paradas genéricas por defecto
  const abrirModalReserva = () => {
    const { subida, bajada } = obtenerParadasPorDefecto(paradasGenericas);
    setGlobalPlazas(1);
    setGlobalSubidaId(subida);
    setGlobalBajadaId(bajada);
    setReservaMsg(null);
    setAceptaBloqueoPago(false);
    setMostrarStripe(false);
    setModalReservaAbierto(true);
  };

  const iniciarProcesoPagoMultiple = async () => {
    setReservaMsg(null);
    if (!aceptaBloqueoPago) {
      setReservaMsg('Debes aceptar el aviso de cobro antes de reservar.');
      return;
    }

    setReservando(true);
    try {
      const incluyePadre = selectedIds.includes('padre') && padre && padre.fechaFinRecurrencia;
      const instanciasSeleccionadasIds = selectedIds.filter(id => id !== 'padre').map(Number);

      let clientSecretFinal = null;

      if (incluyePadre && padre) {
        const payloadPadre = {
          viajeId: Number(padre.id),
          plazas: Number(globalPlazas),
          paradaSubidaId: globalSubidaId ? Number(globalSubidaId) : null,
          paradaBajadaId: globalBajadaId ? Number(globalBajadaId) : null,
        };

        const responsePadre = await fetch(buildApiUrl('/api/reservas/crear'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
          body: JSON.stringify(payloadPadre)
        });

        if (!responsePadre.ok) {
          const data = await responsePadre.json().catch(() => null);
          throw new Error(data?.message || 'No se pudo procesar la reserva del viaje padre');
        }

        const dataPadre = await responsePadre.json();
        if (dataPadre.clientSecret) clientSecretFinal = dataPadre.clientSecret;
      }

      if (instanciasSeleccionadasIds.length > 0) {
        const payloadRecurrentes = {
          viajeRecurrenteIds: instanciasSeleccionadasIds,
          plazas: Number(globalPlazas),
          paradaSubidaId: globalSubidaId ? Number(globalSubidaId) : null,
          paradaBajadaId: globalBajadaId ? Number(globalBajadaId) : null,
        };

        const responseRecurrentes = await fetch(buildApiUrl('/api/reservas/crear-recurrentes'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
          body: JSON.stringify(payloadRecurrentes)
        });

        if (!responseRecurrentes.ok) {
          const data = await responseRecurrentes.json().catch(() => null);
          throw new Error(data?.message || 'No se pudieron procesar las reservas de las instancias');
        }

        const dataRecurrentes = await responseRecurrentes.json();
        if (dataRecurrentes.clientSecret) clientSecretFinal = dataRecurrentes.clientSecret;
      }

      if (!clientSecretFinal) {
        throw new Error('No se recibió el clientSecret de la pasarela de pago');
      }

      setClientSecret(clientSecretFinal);
      setMostrarStripe(true);

    } catch (err) {
      setReservaMsg(`❌ ${err instanceof Error ? err.message : 'Error inesperado'}`);
    } finally {
      setReservando(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 pb-28 pt-6">
      <div className="mx-auto max-w-3xl px-4">
        <button
          type="button"
          onClick={handleVolver}
          className="rounded-full border border-green-600 px-4 py-1 text-sm text-green-700 transition hover:bg-green-50 mb-6"
        >
          ← Volver al detalle del viaje
        </button>

        {/* 1. VIAJE PADRE */}
        {padre && (
          <div className={`bg-white rounded-3xl border transition-all p-6 mb-6 ${isPadreSelected ? 'border-slate-900 bg-slate-50/50 shadow-sm' : 'border-slate-300 shadow-sm'}`}>
            <div className="flex items-center justify-between mb-4">
              <span className="text-xs px-2.5 py-0.5 rounded-full bg-amber-100 text-amber-800 font-bold border border-amber-300">
                Viaje Padre / Patrón Recurrente
              </span>
              
              <div className="flex items-center gap-3">
                {esConductor ? (
                  <div className="flex items-center gap-2">
                    <span className="text-xs px-3 py-1.5 rounded-xl bg-blue-50 text-blue-700 font-semibold border border-blue-200">
                      Vista de Conductor
                    </span>
                    <button
                      type="button"
                      onClick={() => navigate(`/viajes/${padre.slug}`, { state: { rol: state.rol } })}
                      className="rounded-xl bg-slate-800 px-4 py-1.5 text-sm font-bold text-white hover:bg-slate-900 transition shadow-sm"
                    >
                      Ver detalles
                    </button>
                  </div>
                ) : padreTieneReservaActiva ? (
                  <button
                    type="button"
                    onClick={() => navigate(`/viajes/${padre.slug}`, { state: { rol: state.rol } })}
                    className="rounded-xl bg-indigo-600 px-4 py-1.5 text-sm font-bold text-white hover:bg-indigo-700 transition shadow-sm"
                  >
                    Ver detalle / Gestionar reserva
                  </button>
                ) : (
                  <>
                    {padreSeleccionable && (
                      <label className="flex items-center gap-2 cursor-pointer text-sm font-medium text-slate-700 bg-slate-50 px-3 py-1.5 rounded-xl border border-slate-200 hover:bg-slate-100 transition">
                        <input
                          type="checkbox"
                          checked={isPadreSelected}
                          onChange={handleTogglePadreUnico}
                          className="w-4 h-4 text-slate-900 rounded border-slate-300 focus:ring-slate-900"
                        />
                        <span>Seleccionar viaje padre</span>
                      </label>
                    )}
                    {totalElementosSeleccionables > 0 && (
                      <label className="flex items-center gap-2 cursor-pointer text-sm font-medium text-slate-700 bg-slate-50 px-3 py-1.5 rounded-xl border border-slate-200 hover:bg-slate-100 transition">
                        <input
                          type="checkbox"
                          checked={isAllSelected}
                          ref={(input) => { if (input) input.indeterminate = isSomeSelected; }}
                          onChange={handleToggleTodo}
                          className="w-4 h-4 text-slate-900 rounded border-slate-300 focus:ring-slate-900"
                        />
                        <span>Seleccionar todo ({totalElementosSeleccionables})</span>
                      </label>
                    )}
                  </>
                )}
              </div>
            </div>

            <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
              <div>
                <h1 className="text-2xl font-bold text-slate-900 mb-1">
                  {padre.origen && padre.destino ? `${padre.origen} → ${padre.destino}` : 'Viaje Recurrente'}
                </h1>
                <p className="text-slate-600 text-sm">
                  Patrón base recurrente para las instancias generadas.
                </p>
              </div>
              <div className="text-left md:text-right">
                <span className="text-xs text-slate-500 block">Precio base</span>
                <span className="text-xl font-bold text-slate-900">{padre.precio}€</span>
              </div>
            </div>
          </div>
        )}

        {/* 2. LISTADO DE INSTANCIAS */}
        <div className="bg-white rounded-3xl border border-slate-300 shadow-sm p-6">
          <h2 className="text-xl font-bold text-slate-900 mb-2">🔄 Instancias Generadas</h2>
          <p className="text-slate-600 text-sm mb-6">
            {esConductor 
              ? 'Listado de instancias creadas para tu viaje:' 
              : 'Selecciona los viajes individuales que deseas reservar:'}
          </p>

          <div className="space-y-4">
            {viajes.map((instancia) => {
              const isSelected = selectedIds.includes(instancia.id);
              const reservasInstancia = instancia.reservas || instancia.reserva;
              const esHijaSeleccionable = esSeleccionableParaCliente(reservasInstancia, instancia.estado);

              const tieneReservaActiva = reservasInstancia 
                ? !esSeleccionableParaCliente(reservasInstancia)
                : false;

              return (
                <div
                  key={instancia.id}
                  className={`bg-white p-5 rounded-2xl border transition-all flex flex-col md:flex-row justify-between items-start md:items-center gap-4 ${
                    isSelected ? 'border-slate-900 bg-slate-50/50 shadow-sm' : 'border-slate-200 hover:border-slate-300'
                  }`}
                >
                  <div className="flex items-start gap-3.5">
                    {esHijaSeleccionable && (
                      <input
                        type="checkbox"
                        checked={isSelected}
                        onChange={() => handleToggleInstancia(instancia)}
                        className="mt-1 w-4 h-4 text-slate-900 rounded border-slate-300 focus:ring-slate-900 cursor-pointer"
                      />
                    )}

                    <div className="space-y-1">
                      <p className="font-bold text-slate-900 text-base capitalize">
                        {formatFecha(instancia.fechaHoraSalida)}
                      </p>
                      <div className="flex flex-wrap items-center gap-2 pt-1">
                        <span className="text-xs px-2.5 py-0.5 rounded-full bg-amber-50 text-amber-800 font-medium border border-amber-200">
                          Estado: {instancia.estado}
                        </span>
                        <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-700 font-medium border border-slate-200">
                          Plazas libres: <strong className="text-slate-900">{instancia.plazasDisponibles}</strong>
                        </span>
                        {tieneReservaActiva && (
                          <span className="text-xs px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 font-bold border border-emerald-300">
                            Reservado por ti
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center justify-between w-full md:w-auto pt-3 md:pt-0 border-t md:border-t-0 border-slate-100 gap-4">
                    <div className="text-left md:text-right">
                      <span className="text-xs text-slate-500 block">Precio</span>
                      <span className="text-lg font-bold text-slate-900">{instancia.precio}€</span>
                    </div>

                    {(esConductor || tieneReservaActiva) && (
                      <button
                        type="button"
                        onClick={() =>
                          navigate(`/viajes/${instancia.slug}`, {
                            state: {
                              ...state,
                              esInstanciaRecurrente: true,
                            },
                          })
                        }
                        className="rounded-xl bg-indigo-600 px-4 py-2 text-xs font-bold text-white hover:bg-indigo-700 transition shadow-sm"
                      >
                        {esConductor ? 'Ver detalles' : 'Ver detalle / Gestionar'}
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* BARRA INFERIOR FLOTANTE */}
      {!esConductor && totalSeleccionadosCount > 0 && (
        <div className="fixed bottom-0 left-0 right-0 bg-white/95 backdrop-blur-md border-t border-slate-200 p-4 shadow-lg z-40">
          <div className="max-w-3xl mx-auto flex items-center justify-between">
            <div>
              <p className="text-xs text-slate-500">
                Seleccionados: <strong className="text-slate-900">{totalSeleccionadosCount}</strong>
              </p>
              <p className="text-lg font-bold text-slate-900">Total base: {precioBaseTotal.toFixed(2)}€</p>
            </div>
            <button
              type="button"
              onClick={abrirModalReserva}
              className="rounded-2xl bg-gradient-compi px-6 py-3 text-sm font-bold text-white hover:opacity-95 transition shadow-md"
            >
              Configurar y Pagar ({totalSeleccionadosCount})
            </button>
          </div>
        </div>
      )}

      {/* MODAL DE RESERVA / PASARELA DE PAGO */}
      {modalReservaAbierto && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center px-4 bg-slate-900/60 backdrop-blur-sm overflow-hidden">
          <div className="bg-white rounded-2xl shadow-2xl max-w-lg w-full border border-slate-200 flex flex-col max-h-[90vh]">
            <div className="px-6 py-4 border-b border-slate-200 flex justify-between items-center bg-white rounded-t-2xl">
              <h2 className="text-xl font-bold text-slate-900">
                {mostrarStripe ? 'Completar Pago' : 'Configurar Reservas Múltiples'}
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
              {mostrarStripe && clientSecret ? (
                <div className="py-4 animate-in fade-in">
                  <Elements stripe={stripePromise} options={{ clientSecret }}>
                    <CheckoutForm 
                      clientSecret={clientSecret} 
                      monto={precioTotal}
                      onSuccess={() => { 
                        setReservaMsg('✅ ¡Reservas y pagos completados con éxito!');
                        setTimeout(() => {
                          setModalReservaAbierto(false);
                          navigate('/mis-viajes', { state: { rol: state.rol } });
                        }, 1500);
                      }}
                      onError={(message) => {
                        setReservaMsg(`❌ ${message || 'El pago no pudo completarse'}`);
                        setMostrarStripe(false);
                      }}
                    />
                  </Elements>
                </div>
              ) : (
                <div className="space-y-6">
                  {/* MENSAJE INFORMATIVO DE CONFIGURACIÓN ÚNICA */}
                  <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 text-xs text-amber-900 space-y-2 shadow-xs">
                    <p className="flex items-center gap-1.5 font-bold text-amber-900 text-sm">
                      <span>🔄</span> Reserva múltiple para viajes recurrentes
                    </p>
                    <p className="leading-relaxed">
                      Esta reserva se aplicará a todos los viajes seleccionados de la serie con el mismo número de plazas y la misma parada de subida y bajada.
                    </p>
                    <p className="text-amber-800 italic leading-relaxed pt-1.5 border-t border-amber-200/60">
                      Si quieres cambiar el número de plazas de un viaje concreto o las paradas, puedes modificar la reserva posteriormente o hacerla de manera individual.
                    </p>
                  </div>

                  {/* DESPLEGABLES GENÉRICOS */}
                  <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-4">
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                      <div>
                        <label className="block text-xs font-semibold text-slate-700 mb-1">Plazas por viaje</label>
                        <select
                          value={globalPlazas}
                          onChange={(e) => setGlobalPlazas(Number(e.target.value))}
                          className="w-full rounded-lg border border-slate-300 p-2 text-sm bg-white font-medium focus:ring-2 focus:ring-slate-900"
                        >
                          {[1, 2, 3, 4].map((num) => (
                            <option key={num} value={num}>
                              {num} {num === 1 ? 'plaza' : 'plazas'}
                            </option>
                          ))}
                        </select>
                      </div>

                      <div>
                        <label className="block text-xs font-semibold text-slate-700 mb-1">Subida</label>
                        <select
                          value={globalSubidaId || ''}
                          onChange={(e) => {
                            const nuevaSubidaId = Number(e.target.value);
                            setGlobalSubidaId(nuevaSubidaId);
                            const nuevaSubidaObj = paradasGenericas.find(p => p.id === nuevaSubidaId);
                            if (nuevaSubidaObj) {
                              const bajadasValidas = paradasGenericas.filter(p => p.tipo !== 'ORIGEN' && p.orden > nuevaSubidaObj.orden);
                              if (bajadasValidas.length > 0 && !bajadasValidas.some(p => p.id === globalBajadaId)) {
                                setGlobalBajadaId(bajadasValidas[0].id);
                              }
                            }
                          }}
                          className="w-full rounded-lg border border-slate-300 p-2 text-sm bg-white font-medium focus:ring-2 focus:ring-slate-900"
                        >
                          {paradasSubida.map(p => (
                            <option key={p.id} value={p.id}>{p.localizacion}</option>
                          ))}
                        </select>
                      </div>

                      <div>
                        <label className="block text-xs font-semibold text-slate-700 mb-1">Bajada</label>
                        <select
                          value={globalBajadaId || ''}
                          onChange={(e) => setGlobalBajadaId(Number(e.target.value))}
                          className="w-full rounded-lg border border-slate-300 p-2 text-sm bg-white font-medium focus:ring-2 focus:ring-slate-900"
                        >
                          {paradasBajada.map(p => (
                            <option key={p.id} value={p.id}>{p.localizacion}</option>
                          ))}
                        </select>
                      </div>
                    </div>
                  </div>

                  <div className="bg-indigo-50 p-4 rounded-xl border border-indigo-100 flex justify-between items-center">
                    <div>
                      <p className="text-[10px] text-indigo-600 font-bold uppercase tracking-widest leading-none mb-1">Total a pagar</p>
                      <p className="text-2xl font-black text-indigo-900">{precioTotal.toFixed(2)}€</p>
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
                      Acepto el cargo total de <strong>{precioTotal.toFixed(2)}€</strong> para confirmar las plazas en los {totalSeleccionadosCount} viajes seleccionados.
                    </span>
                  </label>

                  {reservaMsg && (
                    <div className={`p-3 rounded-xl text-xs font-bold border ${reservaMsg.includes('✅') ? 'bg-emerald-50 border-emerald-200 text-emerald-700' : 'bg-red-50 border-red-200 text-red-700'}`}>
                      {reservaMsg}
                    </div>
                  )}
                </div>
              )}
            </div>

            {!mostrarStripe && (
              <div className="px-6 py-4 border-t border-slate-200 bg-slate-50 rounded-b-2xl flex gap-3">
                <button
                  type="button"
                  onClick={() => setModalReservaAbierto(false)}
                  className="flex-1 py-3 rounded-xl font-bold text-slate-700 border border-slate-300 hover:bg-slate-100 transition"
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  onClick={iniciarProcesoPagoMultiple}
                  disabled={reservando || !aceptaBloqueoPago}
                  className="flex-1 py-3 rounded-xl font-bold text-white bg-gradient-compi hover:opacity-95 shadow-lg shadow-indigo-100 transition disabled:bg-slate-300 disabled:cursor-not-allowed"
                >
                  {reservando ? 'Procesando...' : `Pagar ${precioTotal.toFixed(2)}€`}
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default ViajesAsociadosScreen;