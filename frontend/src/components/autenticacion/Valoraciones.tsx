import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { buildApiUrl } from '../../apiConfig';

type PerfilData = {
  id?: number;
  nombre: string;
  primerApellido: string;
  reputacion?: number;
};

type ViajeParticipado = {
  id: number;
  slug: string;
  fechaHoraSalida: string;
  estado: string;
  conductorId?: number;
  conductorNombre?: string;
  conductorSlug?: string;
  vehiculo?: {
    marca: string;
    modelo: string;
    matricula: string;
  };
};

type ValoracionDTO = {
  id: number;
  puntuacion: number;
  comentario?: string;
  fecha: string;
  autorId?: number;
  autorNombre?: string;
  valoradoId?: number;
  valoradoNombre?: string;
  viajeId?: number;
  slug?: string;
};

const Valoraciones: React.FC = () => {
  const navigate = useNavigate();
  const [perfil, setPerfil] = useState<PerfilData | null>(null);
  const [valoracionesRecibidas, setValoracionesRecibidas] = useState<ValoracionDTO[]>([]);
  const [valoracionesEmitidas, setValoracionesEmitidas] = useState<ValoracionDTO[]>([]);
  const [viajesParticipados, setViajesParticipados] = useState<ViajeParticipado[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [showEditForm, setShowEditForm] = useState(false);
  const [sending, setSending] = useState(false);
  const [formError, setFormError] = useState('');
  const [editError, setEditError] = useState('');
  const [form, setForm] = useState({
    viajeId: '',
    puntuacion: '5',
    comentario: ''
  });
  const [editForm, setEditForm] = useState({
    id: 0,
    puntuacion: '5',
    comentario: '',
    viajeId: 0,
    valoradoId: 0
  });

  const getValidToken = () => {
    const token = localStorage.getItem('token');
    if (!token || token === 'undefined' || token === 'null' || token.trim() === '') {
      return null;
    }

    return token;
  };

  const clearLocalSession = useCallback((redirectTo: string) => {
    localStorage.removeItem('token');
    window.dispatchEvent(new Event('authChange'));
    navigate(redirectTo, { replace: true });
  }, [navigate]);

  const fetchJson = async (path: string, token: string) => {
    const response = await fetch(buildApiUrl(path), {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      const body = await response.json().catch(() => null);
      const message = typeof body?.message === 'string' ? body.message : 'Error al cargar datos';
      throw new Error(message);
    }

    return response.json();
  };

  const cargarDatos = useCallback(async () => {
    const token = getValidToken();
    if (!token) {
      clearLocalSession('/inicio-sesion');
      return;
    }

    try {
      const perfilData = await fetchJson('/api/personas/perfil', token) as PerfilData;
      setPerfil(perfilData);

      const [recibidas, emitidas, viajes] = await Promise.all([
        fetchJson(`/api/valoraciones/valorado/${perfilData.id}`, token),
        fetchJson(`/api/valoraciones/autor/${perfilData.id}`, token),
        fetchJson('/api/viajes/participados', token)
      ]);

      setValoracionesRecibidas(Array.isArray(recibidas) ? recibidas : []);
      setValoracionesEmitidas(Array.isArray(emitidas) ? emitidas : []);
      setViajesParticipados(Array.isArray(viajes) ? viajes : []);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Error al cargar valoraciones';
      if (message.toLowerCase().includes('no autenticado') || message.toLowerCase().includes('unauthorized')) {
        clearLocalSession('/inicio-sesion');
        return;
      }
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [clearLocalSession]);

  useEffect(() => {
    cargarDatos();
  }, [cargarDatos]);

  const viajesElegibles = useMemo(() => {
    const ahora = new Date();
    return viajesParticipados.filter((viaje) => {
      const fechaViaje = new Date(viaje.fechaHoraSalida);
      const estado = (viaje.estado || '').toUpperCase();
      return fechaViaje <= ahora || estado === 'FINALIZADO' || estado === 'COMPLETADO';
    });
  }, [viajesParticipados]);

  const viajeSeleccionado = useMemo(() => (
    viajesElegibles.find((viaje) => viaje.id === Number(form.viajeId)) || null
  ), [form.viajeId, viajesElegibles]);

  const formatFecha = (fecha: string) => new Date(fecha).toLocaleString('es-ES', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setFormError('');
  };

  const handleEditChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setEditForm((prev) => ({ ...prev, [name]: value }));
    setEditError('');
  };

  const openEditForm = (valoracion: ValoracionDTO) => {
    setEditForm({
      id: valoracion.id,
      puntuacion: String(valoracion.puntuacion),
      comentario: valoracion.comentario || '',
      viajeId: valoracion.viajeId || 0,
      valoradoId: valoracion.valoradoId || 0
    });
    setEditError('');
    setShowEditForm(true);
  };

  const handleDeleteValoracion = async (valoracionId: number) => {
    if (!window.confirm('¿Seguro que quieres eliminar esta valoración?')) {
      return;
    }

    const token = getValidToken();
    if (!token) {
      clearLocalSession('/inicio-sesion');
      return;
    }

    try {
      const response = await fetch(buildApiUrl(`/api/valoraciones/${valoracionId}`), {
        method: 'DELETE',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        await cargarDatos();
      } else if (response.status === 401 || response.status === 403) {
        clearLocalSession('/inicio-sesion');
      } else {
        setError('No se pudo eliminar la valoración');
      }
    } catch {
      setError('Error de conexión al eliminar la valoración');
    }
  };

  const handleEditSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!perfil?.id) {
      setEditError('No se pudo identificar tu perfil.');
      return;
    }

    const puntuacion = Number(editForm.puntuacion);
    if (!Number.isInteger(puntuacion) || puntuacion < 1 || puntuacion > 5) {
      setEditError('La puntuación debe estar entre 1 y 5.');
      return;
    }

    const token = getValidToken();
    if (!token) {
      clearLocalSession('/inicio-sesion');
      return;
    }

    setSending(true);

    try {
      const response = await fetch(buildApiUrl(`/api/valoraciones/${editForm.id}`), {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          puntuacion,
          comentario: editForm.comentario.trim() || null,
          autorId: perfil.id,
          valoradoId: editForm.valoradoId,
          viajeId: editForm.viajeId
        })
      });

      if (response.ok) {
        setShowEditForm(false);
        await cargarDatos();
      } else if (response.status === 401 || response.status === 403) {
        clearLocalSession('/inicio-sesion');
        return;
      } else {
        const body = await response.json().catch(() => null);
        setEditError(typeof body?.message === 'string' ? body.message : 'No se pudo actualizar la valoración');
      }
    } catch {
      setEditError('Error de conexión al actualizar la valoración');
    } finally {
      setSending(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!perfil?.id) {
      setFormError('No se pudo identificar tu perfil.');
      return;
    }

    if (!form.viajeId) {
      setFormError('Selecciona un viaje.');
      return;
    }

    if (!viajeSeleccionado?.conductorId) {
      setFormError('No se pudo determinar el conductor de este viaje.');
      return;
    }

    const puntuacion = Number(form.puntuacion);
    if (!Number.isInteger(puntuacion) || puntuacion < 1 || puntuacion > 5) {
      setFormError('La puntuación debe estar entre 1 y 5.');
      return;
    }

    const token = getValidToken();
    if (!token) {
      clearLocalSession('/inicio-sesion');
      return;
    }

    setSending(true);

    try {
      const response = await fetch(buildApiUrl('/api/valoraciones'), {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          puntuacion,
          comentario: form.comentario.trim() || null,
          autorId: perfil.id,
          valoradoId: viajeSeleccionado.conductorId,
          viajeId: viajeSeleccionado.id
        })
      });

      if (response.ok) {
        setForm({ viajeId: '', puntuacion: '5', comentario: '' });
        setShowForm(false);
        await cargarDatos();
      } else if (response.status === 401 || response.status === 403) {
        clearLocalSession('/inicio-sesion');
        return;
      } else {
        const body = await response.json().catch(() => null);
        setFormError(typeof body?.message === 'string' ? body.message : 'No se pudo crear la valoración');
      }
    } catch {
      setFormError('Error de conexión al crear la valoración');
    } finally {
      setSending(false);
    }
  };

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center bg-gray-100">Cargando valoraciones...</div>;
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100 px-4">
        <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 text-center shadow-lg">
          <p className="text-red-600">{error}</p>
          <button
            type="button"
            className="mt-4 rounded-full bg-gradient-compi px-5 py-2 font-semibold text-white shadow hover:opacity-90"
            onClick={() => navigate('/perfil')}
          >
            Volver al perfil
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-100 via-white to-slate-100 px-4 py-6 md:px-8">
      <div className="mx-auto max-w-6xl">
        <button
          type="button"
          onClick={() => navigate('/perfil')}
          className="rounded-full border border-green-600 px-4 py-1 text-sm text-green-700 transition hover:bg-green-50"
        >
          Volver al perfil
        </button>

        <div className="mt-6 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.24em] text-green-700">Valoraciones</p>
              <h1 className="mt-2 text-4xl font-bold text-slate-900">Tu reputación y tus valoraciones</h1>
              <p className="mt-2 max-w-2xl text-slate-600">
                Consulta las valoraciones que has recibido, revisa las que has enviado y crea una nueva valoración desde un viaje en el que hayas participado.
              </p>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-5 py-4">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">Reputación</p>
              <p className="mt-1 text-3xl font-bold text-slate-900">{(perfil?.reputacion ?? 0).toFixed(1)} / 5</p>
            </div>
          </div>

          <div className="mt-6 grid gap-4 lg:grid-cols-2">
            <section className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
              <div className="flex items-center justify-between gap-4">
                <h2 className="text-2xl font-semibold text-slate-900">Valoraciones recibidas</h2>
                <span className="rounded-full bg-slate-900 px-3 py-1 text-xs font-semibold text-white">
                  {valoracionesRecibidas.length}
                </span>
              </div>

              <div className="mt-4 space-y-3">
                {valoracionesRecibidas.length === 0 ? (
                  <p className="text-slate-600">Todavía no tienes valoraciones recibidas.</p>
                ) : (
                  valoracionesRecibidas.map((valoracion) => (
                    <article key={valoracion.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                      <div className="flex items-start justify-between gap-4">
                        <div>
                          <p className="text-sm font-semibold text-slate-900">
                            {valoracion.autorNombre ? `De ${valoracion.autorNombre}` : `Valoración #${valoracion.id}`}
                          </p>
                          <p className="text-xs text-slate-500">{formatFecha(valoracion.fecha)}</p>
                        </div>
                        <span className="rounded-full bg-amber-100 px-3 py-1 text-sm font-bold text-amber-800">
                          {valoracion.puntuacion}/5
                        </span>
                      </div>
                      {valoracion.comentario && (
                        <p className="mt-3 text-sm leading-6 text-slate-700">{valoracion.comentario}</p>
                      )}
                    </article>
                  ))
                )}
              </div>
            </section>

            <section className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
              <div className="flex items-center justify-between gap-4">
                <h2 className="text-2xl font-semibold text-slate-900">Valoraciones emitidas</h2>
                <span className="rounded-full bg-slate-900 px-3 py-1 text-xs font-semibold text-white">
                  {valoracionesEmitidas.length}
                </span>
              </div>

              <div className="mt-4 space-y-3">
                {valoracionesEmitidas.length === 0 ? (
                  <p className="text-slate-600">Todavía no has enviado ninguna valoración.</p>
                ) : (
                  valoracionesEmitidas.map((valoracion) => (
                    <article key={valoracion.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                      <div className="flex items-start justify-between gap-4">
                        <div>
                          <p className="text-sm font-semibold text-slate-900">
                            {valoracion.valoradoNombre ? `A ${valoracion.valoradoNombre}` : `Valoración #${valoracion.id}`}
                          </p>
                          <p className="text-xs text-slate-500">{formatFecha(valoracion.fecha)}</p>
                        </div>
                        <span className="rounded-full bg-emerald-100 px-3 py-1 text-sm font-bold text-emerald-800">
                          {valoracion.puntuacion}/5
                        </span>
                      </div>
                      {valoracion.comentario && (
                        <p className="mt-3 text-sm leading-6 text-slate-700">{valoracion.comentario}</p>
                      )}
                      <div className="mt-4 flex flex-wrap gap-2">
                        <button
                          type="button"
                          className="rounded-full border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                          onClick={() => openEditForm(valoracion)}
                        >
                          Editar
                        </button>
                        <button
                          type="button"
                          className="rounded-full bg-red-500 px-4 py-2 text-sm font-semibold text-white hover:bg-red-600"
                          onClick={() => handleDeleteValoracion(valoracion.id)}
                        >
                          Eliminar
                        </button>
                      </div>
                    </article>
                  ))
                )}
              </div>
            </section>
          </div>

          <div className="mt-6 flex flex-wrap items-center gap-3">
            <button
              type="button"
              className="rounded-full bg-gradient-compi px-5 py-2.5 text-sm font-semibold text-white shadow hover:opacity-90"
              onClick={() => setShowForm((prev) => !prev)}
            >
              {showForm ? 'Cerrar formulario' : 'Añadir nueva valoración'}
            </button>
            <p className="text-sm text-slate-600">
              Podrás valorar solo los viajes ya realizados.
            </p>
          </div>

          {showForm && (
            <form onSubmit={handleSubmit} className="mt-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="text-2xl font-semibold text-slate-900">Nueva valoración</h2>

              {formError && (
                <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                  {formError}
                </div>
              )}

              <div className="mt-4 grid gap-4 lg:grid-cols-2">
                <label className="space-y-2">
                  <span className="block text-sm font-semibold text-slate-700">Viaje</span>
                  <select
                    name="viajeId"
                    value={form.viajeId}
                    onChange={handleChange}
                    className="w-full rounded-xl border border-slate-300 px-4 py-3"
                  >
                    <option value="">Selecciona un viaje</option>
                    {viajesElegibles.map((viaje) => (
                      <option key={viaje.id} value={viaje.id}>
                        {new Date(viaje.fechaHoraSalida).toLocaleDateString('es-ES')} - {viaje.conductorNombre || 'Conductor'} - {viaje.vehiculo?.marca} {viaje.vehiculo?.modelo}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="space-y-2">
                  <span className="block text-sm font-semibold text-slate-700">Puntuación</span>
                  <select
                    name="puntuacion"
                    value={form.puntuacion}
                    onChange={handleChange}
                    className="w-full rounded-xl border border-slate-300 px-4 py-3"
                  >
                    <option value="5">5 - Excelente</option>
                    <option value="4">4 - Muy buena</option>
                    <option value="3">3 - Correcta</option>
                    <option value="2">2 - Mejorable</option>
                    <option value="1">1 - Mala</option>
                  </select>
                </label>

                <label className="space-y-2 lg:col-span-2">
                  <span className="block text-sm font-semibold text-slate-700">Comentario</span>
                  <textarea
                    name="comentario"
                    rows={4}
                    value={form.comentario}
                    onChange={handleChange}
                    className="w-full rounded-xl border border-slate-300 px-4 py-3"
                    placeholder="Cuenta tu experiencia con este viaje"
                  />
                </label>
              </div>

              {viajeSeleccionado && (
                <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
                  <p className="font-semibold text-slate-900">Conductor del viaje</p>
                  <p>{viajeSeleccionado.conductorNombre || 'Conductor no disponible'}</p>
                  <p className="mt-1 text-slate-600">
                    {new Date(viajeSeleccionado.fechaHoraSalida).toLocaleString('es-ES')} · {viajeSeleccionado.vehiculo?.marca} {viajeSeleccionado.vehiculo?.modelo}
                  </p>
                </div>
              )}

              <div className="mt-5 flex flex-wrap gap-3">
                <button
                  type="submit"
                  disabled={sending || viajesElegibles.length === 0}
                  className="rounded-full bg-slate-900 px-5 py-2.5 text-sm font-semibold text-white shadow disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {sending ? 'Guardando...' : 'Crear valoración'}
                </button>
                <button
                  type="button"
                  className="rounded-full border border-slate-300 px-5 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                  onClick={() => setShowForm(false)}
                >
                  Cancelar
                </button>
              </div>
            </form>
          )}

          {showEditForm && (
            <form onSubmit={handleEditSubmit} className="mt-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="text-2xl font-semibold text-slate-900">Editar valoración</h2>

              {editError && (
                <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                  {editError}
                </div>
              )}

              <div className="mt-4 grid gap-4 lg:grid-cols-2">
                <label className="space-y-2">
                  <span className="block text-sm font-semibold text-slate-700">Puntuación</span>
                  <select
                    name="puntuacion"
                    value={editForm.puntuacion}
                    onChange={handleEditChange}
                    className="w-full rounded-xl border border-slate-300 px-4 py-3"
                  >
                    <option value="5">5 - Excelente</option>
                    <option value="4">4 - Muy buena</option>
                    <option value="3">3 - Correcta</option>
                    <option value="2">2 - Mejorable</option>
                    <option value="1">1 - Mala</option>
                  </select>
                </label>

                <label className="space-y-2 lg:col-span-2">
                  <span className="block text-sm font-semibold text-slate-700">Comentario</span>
                  <textarea
                    name="comentario"
                    rows={4}
                    value={editForm.comentario}
                    onChange={handleEditChange}
                    className="w-full rounded-xl border border-slate-300 px-4 py-3"
                    placeholder="Modifica tu comentario"
                  />
                </label>
              </div>

              <div className="mt-5 flex flex-wrap gap-3">
                <button
                  type="submit"
                  disabled={sending}
                  className="rounded-full bg-slate-900 px-5 py-2.5 text-sm font-semibold text-white shadow disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {sending ? 'Guardando...' : 'Guardar cambios'}
                </button>
                <button
                  type="button"
                  className="rounded-full border border-slate-300 px-5 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                  onClick={() => setShowEditForm(false)}
                >
                  Cancelar
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};

export default Valoraciones;