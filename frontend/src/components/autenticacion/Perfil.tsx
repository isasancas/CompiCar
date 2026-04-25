import React, { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { buildApiUrl } from '../../apiConfig';

interface PerfilData {
  id?: number;
  nombre: string;
  primerApellido: string;
  segundoApellido?: string;
  email: string;
  telefono: string;
  reputacion?: number;
}

interface VehiculoData {
  id: number;
  matricula: string;
  marca: string;
  modelo: string;
  plazas: number;
  consumo: number;
  anio: number;
  tipo: string;
}

type ViajeActividad = {
  id: number;
  fechaHoraSalida: string;
  estado: string;
};

type ResumenActividad = {
  ofrecidosMes: number;
  completados: number;
  cancelados: number;
  tendenciaPct: number;
};

const Perfil: React.FC = () => {
  const [perfil, setPerfil] = useState<PerfilData | null>(null);
  const [vehiculos, setVehiculos] = useState<VehiculoData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
  const [vehiculosError, setVehiculosError] = useState<string | null>(null);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [editError, setEditError] = useState('');
  const [editForm, setEditForm] = useState({
    nombre: '',
    primerApellido: '',
    segundoApellido: '',
    email: '',
    telefono: '',
    contrasenaActual: ''
  });
  const [vehicleEditModal, setVehicleEditModal] = useState(false);
  const [selectedVehiculo, setSelectedVehiculo] = useState<VehiculoData | null>(null);
  const [editVehiculoError, setEditVehiculoError] = useState('');
  const [isSavingVehiculo, setIsSavingVehiculo] = useState(false);
  const [deleteVehiculoError, setDeleteVehiculoError] = useState('');
  const [editVehiculoForm, setEditVehiculoForm] = useState({
    matricula: '',
    marca: '',
    modelo: '',
    plazas: '',
    consumo: '',
    anio: '',
    tipo: 'COCHE'
  });
  const [resumenActividad, setResumenActividad] = useState<ResumenActividad>({
    ofrecidosMes: 0,
    completados: 0,
    cancelados: 0,
    tendenciaPct: 0
  });

  const misDatosRef = useRef<HTMLDivElement | null>(null);
  const [misDatosHeight, setMisDatosHeight] = useState<number | null>(null);
  useLayoutEffect(() => {
    const updateHeight = () => {
      if (misDatosRef.current) {
        setMisDatosHeight(misDatosRef.current.offsetHeight);
      }
    };

    updateHeight();
    window.addEventListener('resize', updateHeight);

    return () => {
      window.removeEventListener('resize', updateHeight);
    };
  }, [perfil, resumenActividad]);
  const navigate = useNavigate();

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

  const fetchPerfil = useCallback(async () => {
    const token = getValidToken();
    if (!token) {
      clearLocalSession('/inicio-sesion');
      return;
    }

    try {
      const response = await fetch(buildApiUrl('/api/personas/perfil'), {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        setPerfil(data);
      } else if (response.status === 401 || response.status === 403) {
        clearLocalSession('/inicio-sesion');
        return;
      } else {
        setError('Error al obtener el perfil');
      }
    } catch {
      setError('Error de conexión');
    }
  }, [clearLocalSession]);

  const fetchVehiculos = useCallback(async () => {
    const token = getValidToken();
    if (!token) {
      clearLocalSession('/inicio-sesion');
      return;
    }

    try {
      const response = await fetch(buildApiUrl('/api/vehiculos/propios'), {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        setVehiculos(data);
      } else if (response.status === 401 || response.status === 403) {
        clearLocalSession('/inicio-sesion');
        return;
      } else {
        setVehiculosError('Error al cargar los vehículos');
      }
    } catch {
      setVehiculosError('Error de conexión al cargar los vehículos');
    }
  }, [clearLocalSession]);

  const fetchResumenActividad = useCallback(async () => {
    const token = getValidToken();
    if (!token) {
      clearLocalSession('/inicio-sesion');
      return;
    }

    try {
      const response = await fetch(buildApiUrl('/api/viajes/mis-viajes'), {
        method: 'GET',
        headers: {
          Authorization: 'Bearer ' + token,
          'Content-Type': 'application/json'
        }
      });

      if (response.status === 401 || response.status === 403) {
        clearLocalSession('/inicio-sesion');
        return;
      }

      if (!response.ok) {
        return;
      }

      const viajes = (await response.json()) as ViajeActividad[];

      const now = new Date();
      const currentMonth = now.getMonth();
      const currentYear = now.getFullYear();

      const prevMonthDate = new Date(currentYear, currentMonth - 1, 1);
      const prevMonth = prevMonthDate.getMonth();
      const prevYear = prevMonthDate.getFullYear();

      const offeredCurrent = viajes.filter((v) => {
        const d = new Date(v.fechaHoraSalida);
        return d.getMonth() === currentMonth && d.getFullYear() === currentYear;
      }).length;

      const offeredPrev = viajes.filter((v) => {
        const d = new Date(v.fechaHoraSalida);
        return d.getMonth() === prevMonth && d.getFullYear() === prevYear;
      }).length;

      const completados = viajes.filter((v) =>
        ['FINALIZADO', 'COMPLETADO'].includes((v.estado || '').toUpperCase())
      ).length;

      const cancelados = viajes.filter((v) =>
        ['CANCELADO', 'CANCELADA'].includes((v.estado || '').toUpperCase())
      ).length;

      const tendenciaPct =
        offeredPrev === 0
          ? offeredCurrent > 0
            ? 100
            : 0
          : Math.round(((offeredCurrent - offeredPrev) / offeredPrev) * 100);

      setResumenActividad({
        ofrecidosMes: offeredCurrent,
        completados,
        cancelados,
        tendenciaPct
      });
    } catch {
      // Si falla, dejamos valores por defecto.
    }
  }, [clearLocalSession]);

  useEffect(() => {
    Promise.all([fetchPerfil(), fetchVehiculos(), fetchResumenActividad()])
      .catch(() => {
        // Errores individuales ya se manejan en cada función.
      })
      .finally(() => setLoading(false));
  }, [fetchPerfil, fetchVehiculos, fetchResumenActividad]);

  const handleLogout = async () => {
    setIsLoggingOut(true);
    const token = getValidToken();
    if (!token) {
      setIsLoggingOut(false);
      clearLocalSession('/inicio-sesion');
      return;
    }

    try {
      await fetch(buildApiUrl('/api/logout'), {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
    } catch {
      // Si el backend falla, cerramos sesión local igualmente para no bloquear al usuario.
    } finally {
      setIsLoggingOut(false);
      setShowLogoutConfirm(false);
      clearLocalSession('/');
    }
  };

  const openEditModal = () => {
    if (!perfil) {
      return;
    }

    setEditError('');
    setEditForm({
      nombre: perfil.nombre || '',
      primerApellido: perfil.primerApellido || '',
      segundoApellido: perfil.segundoApellido || '',
      email: perfil.email || '',
      telefono: perfil.telefono || '',
      contrasenaActual: ''
    });
    setShowEditModal(true);
  };

  const handleEditChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setEditForm((prev) => ({ ...prev, [name]: value }));
    setEditError('');
  };

  const openEditVehiculoModal = (vehiculo: VehiculoData) => {
    setSelectedVehiculo(vehiculo);
    setEditVehiculoError('');
    setDeleteVehiculoError('');
    setEditVehiculoForm({
      matricula: vehiculo.matricula,
      marca: vehiculo.marca,
      modelo: vehiculo.modelo,
      plazas: vehiculo.plazas.toString(),
      consumo: vehiculo.consumo.toString(),
      anio: vehiculo.anio.toString(),
      tipo: vehiculo.tipo
    });
    setVehicleEditModal(true);
  };

  const handleEditVehiculoChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setEditVehiculoForm((prev) => ({ ...prev, [name]: value }));
    setEditVehiculoError('');
  };

  const validateVehiculoForm = () => {
    const matricula = editVehiculoForm.matricula.trim().toUpperCase();
    if (!matricula) {
      setEditVehiculoError('La matrícula es obligatoria.');
      return false;
    }

    const matriculaRegex = /^[0-9]{4}[A-Z]{3}$|^[A-Z]{1,2}[0-9]{4}[A-Z]{1,2}$/;
    if (!matriculaRegex.test(matricula)) {
      setEditVehiculoError('El formato de matrícula no es válido.');
      return false;
    }

    if (!editVehiculoForm.marca.trim()) {
      setEditVehiculoError('La marca es obligatoria.');
      return false;
    }

    if (!editVehiculoForm.modelo.trim()) {
      setEditVehiculoError('El modelo es obligatorio.');
      return false;
    }

    const plazas = Number(editVehiculoForm.plazas);
    if (!Number.isInteger(plazas) || plazas < 1 || plazas > 9) {
      setEditVehiculoError('Las plazas deben ser un número entero entre 1 y 9.');
      return false;
    }

    const consumo = Number(editVehiculoForm.consumo);
    if (Number.isNaN(consumo) || consumo <= 0) {
      setEditVehiculoError('El consumo debe ser un número mayor que 0.');
      return false;
    }

    const anio = Number(editVehiculoForm.anio);
    if (!Number.isInteger(anio) || anio < 1950 || anio > 2100) {
      setEditVehiculoError('El año debe estar entre 1950 y 2100.');
      return false;
    }

    if (!['COCHE', 'MOTO', 'FURGONETA'].includes(editVehiculoForm.tipo)) {
      setEditVehiculoError('Debes seleccionar un tipo de vehículo válido.');
      return false;
    }

    return true;
  };

  const handleSaveVehiculo = async () => {
    if (!selectedVehiculo) {
      setEditVehiculoError('No se ha seleccionado ningún vehículo para editar.');
      return;
    }

    if (!validateVehiculoForm()) {
      return;
    }

    const token = getValidToken();
    if (!token) {
      clearLocalSession('/inicio-sesion');
      return;
    }

    setIsSavingVehiculo(true);

    try {
      const response = await fetch(buildApiUrl(`/api/vehiculos/${selectedVehiculo.id}`), {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          matricula: editVehiculoForm.matricula.trim().toUpperCase(),
          marca: editVehiculoForm.marca.trim(),
          modelo: editVehiculoForm.modelo.trim(),
          plazas: Number(editVehiculoForm.plazas),
          consumo: Number(editVehiculoForm.consumo),
          anio: Number(editVehiculoForm.anio),
          tipo: editVehiculoForm.tipo
        })
      });

      if (response.ok) {
        setVehicleEditModal(false);
        setSelectedVehiculo(null);
        await fetchVehiculos();
      } else if (response.status === 401 || response.status === 403) {
        clearLocalSession('/inicio-sesion');
      } else {
        const body = await response.json().catch(() => null);
        setEditVehiculoError(body?.error || body?.message || 'No se pudo actualizar el vehículo.');
      }
    } catch {
      setEditVehiculoError('Error de conexión al actualizar el vehículo.');
    } finally {
      setIsSavingVehiculo(false);
    }
  };

  const handleDeleteVehiculo = async (vehiculoId: number) => {
    if (!window.confirm('¿Seguro que quieres borrar este vehículo?')) {
      return;
    }

    const token = getValidToken();
    if (!token) {
      clearLocalSession('/inicio-sesion');
      return;
    }

    try {
      const response = await fetch(buildApiUrl(`/api/vehiculos/${vehiculoId}`), {
        method: 'DELETE',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        setDeleteVehiculoError('');
        await fetchVehiculos();
      } else if (response.status === 401 || response.status === 403) {
        clearLocalSession('/inicio-sesion');
      } else {
        const body = await response.json().catch(() => null);
        setDeleteVehiculoError(body?.error || body?.message || 'No se pudo borrar el vehículo.');
      }
    } catch {
      setDeleteVehiculoError('Error de conexión al borrar el vehículo.');
    }
  };

  const isEmailChanged = perfil
    ? editForm.email.trim().toLowerCase() !== (perfil.email || '').trim().toLowerCase()
    : false;

  const validateEditForm = () => {
    if (!editForm.nombre.trim()) {
      setEditError('El nombre no puede estar vacío.');
      return false;
    }

    if (!editForm.primerApellido.trim()) {
      setEditError('El primer apellido no puede estar vacío.');
      return false;
    }

    if (!editForm.email.trim()) {
      setEditError('El email no puede estar vacío.');
      return false;
    }

    if (!/^\S+@\S+\.\S+$/.test(editForm.email.trim())) {
      setEditError('El email no es válido.');
      return false;
    }

    if (!editForm.telefono.trim()) {
      setEditError('El teléfono no puede estar vacío.');
      return false;
    }

    if (!/^\+?[0-9]{7,15}$/.test(editForm.telefono.trim())) {
      setEditError('El teléfono no es válido.');
      return false;
    }

    if (isEmailChanged && !editForm.contrasenaActual) {
      setEditError('Debes introducir tu contraseña actual para cambiar el email.');
      return false;
    }

    return true;
  };

  const handleSaveProfile = async () => {
    if (!perfil?.id) {
      setEditError('No se pudo identificar tu perfil para actualizarlo.');
      return;
    }

    if (!validateEditForm()) {
      return;
    }

    const token = getValidToken();
    if (!token) {
      clearLocalSession('/inicio-sesion');
      return;
    }

    setIsSavingProfile(true);

    try {
      const response = await fetch(buildApiUrl(`/api/personas/${perfil.id}/perfil`), {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          nombre: editForm.nombre.trim(),
          primerApellido: editForm.primerApellido.trim(),
          segundoApellido: editForm.segundoApellido.trim() || null,
          email: editForm.email.trim(),
          telefono: editForm.telefono.trim(),
          contrasenaActual: isEmailChanged ? editForm.contrasenaActual : null
        })
      });

      if (response.ok) {
        const updated = await response.json();
        setPerfil((prev) => ({
          ...(prev || {}),
          id: prev?.id,
          nombre: updated.nombre,
          primerApellido: updated.primerApellido,
          segundoApellido: updated.segundoApellido,
          email: updated.email,
          telefono: updated.telefono,
          reputacion: prev?.reputacion
        } as PerfilData));
        setShowEditModal(false);
        setEditError('');
      } else if (response.status === 401 || response.status === 403) {
        clearLocalSession('/inicio-sesion');
      } else {
        let backendError = 'No se pudo actualizar el perfil.';
        try {
          const body = await response.json();
          backendError = typeof body?.error === 'string' ? body.error : backendError;
        } catch {
          // Si falla parseo JSON, mantenemos el mensaje por defecto.
        }
        setEditError(backendError);
      }
    } catch {
      setEditError('Error de conexión al actualizar el perfil.');
    } finally {
      setIsSavingProfile(false);
    }
  };

  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [fotoPerfil, setFotoPerfil] = useState<string | null>(null);

  const handleEditarFoto = () => {
    fileInputRef.current?.click();
  };

  const handleFotoChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validar tamaño (máx 5MB)
    if (file.size > 5 * 1024 * 1024) {
      alert('La foto debe ser menor a 5MB');
      return;
    }

    const reader = new FileReader();
    reader.onload = async (event) => {
      const base64 = event.target?.result as string;
      setUploading(true);

      try {
        const token = getValidToken();
        if (!token) {
          clearLocalSession('/inicio-sesion');
          return;
        }

        const response = await fetch(buildApiUrl('/api/personas/foto'), {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ foto: base64 })
        });

        if (response.ok) {
          setFotoPerfil(base64);
          alert('Foto actualizada correctamente');
        } else {
          alert('Error al subir la foto');
        }
      } catch (error) {
        alert('Error de conexión al subir la foto');
      } finally {
        setUploading(false);
      }
    };
    reader.readAsDataURL(file);
  };

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center">Cargando...</div>;
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
        <div className="bg-white p-6 rounded-lg shadow-md max-w-md w-full text-center">
          <p className="text-red-500 mb-4">{error}</p>
          <button
            type="button"
            className="bg-gradient-compi hover:opacity-90 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline"
            onClick={() => clearLocalSession('/inicio-sesion')}
          >
            Ir a iniciar sesión
          </button>
        </div>
      </div>
    );
  }
  return (
    <div data-testid="perfil-page" className="min-h-screen bg-gray-200 pb-10 pt-4">
      <div className="mx-auto max-w-6xl px-4">
        <button
          type="button"
          onClick={() => navigate('/')}
          className="rounded-full border border-green-600 px-4 py-1 text-sm text-green-700 transition hover:bg-green-50"
        >
          Volver
        </button>

        <div className="mt-4 grid gap-4 lg:grid-cols-[220px_1fr]">
          <aside className="rounded-xl bg-transparent p-2">
            <h2 className="text-4xl font-bold leading-none text-slate-800">Mi perfil</h2>

            <div className="mt-4 flex h-28 w-28 items-center justify-center rounded-full border-4 border-slate-800 bg-white text-4xl text-slate-700 overflow-hidden">
              {fotoPerfil ? (
                <img src={fotoPerfil} alt="Foto perfil" className="h-full w-full object-cover" />
              ) : (
                <span>{perfil?.nombre?.charAt(0).toUpperCase()}</span>
              )}
            </div>

            <input
              type="file"
              ref={fileInputRef}
              hidden
              accept="image/*"
              onChange={handleFotoChange}
            />

            <button
              type="button"
              onClick={handleEditarFoto}
              disabled={uploading}
              className="mt-4 rounded-full bg-gradient-compi px-5 py-2 text-sm font-semibold text-white shadow disabled:opacity-60"
            >
              {uploading ? 'Subiendo...' : 'Editar foto'}
            </button>
          </aside>

          <section className="grid gap-4 md:grid-cols-2 items-start">
            <div ref={misDatosRef} className="self-start rounded-xl border border-slate-500 bg-gray-100 p-5">
              <h3 className="text-3xl font-semibold text-slate-800">Mis datos y actividad</h3>

              <div className="mt-3 space-y-1 text-lg text-slate-700">
                <p>Nombre: {perfil ? `${perfil.nombre} ${perfil.primerApellido}` : '-'}</p>
                <p>Email: {perfil?.email || '-'}</p>
                <p>Teléfono: {perfil?.telefono || '-'}</p>
              </div>

              <div className="my-4 h-px bg-slate-300" />

              <div className="grid grid-cols-2 gap-3">
                <div className="rounded-xl border border-slate-300 bg-white p-3 shadow-sm">
                  <p className="text-xs font-semibold uppercase text-slate-500">Este mes</p>
                  <p className="mt-1 text-2xl font-bold text-slate-900">{resumenActividad.ofrecidosMes}</p>
                  <p className="text-sm text-slate-600">viajes ofrecidos</p>
                </div>

                <div className="rounded-xl border border-slate-300 bg-white p-3 shadow-sm">
                  <p className="text-xs font-semibold uppercase text-slate-500">Completados</p>
                  <p className="mt-1 text-2xl font-bold text-slate-900">{resumenActividad.completados}</p>
                  <p className="text-sm text-slate-600">histórico</p>
                </div>

                <div className="rounded-xl border border-slate-300 bg-white p-3 shadow-sm">
                  <p className="text-xs font-semibold uppercase text-slate-500">Cancelados</p>
                  <p className="mt-1 text-2xl font-bold text-slate-900">{resumenActividad.cancelados}</p>
                  <p className="text-sm text-slate-600">histórico</p>
                </div>

                <div className="rounded-xl border border-slate-300 bg-white p-3 shadow-sm">
                  <p className="text-xs font-semibold uppercase text-slate-500">Tendencia</p>
                  <p className="mt-1 text-2xl font-bold text-slate-900">
                    {resumenActividad.tendenciaPct > 0 ? '+' : ''}
                    {resumenActividad.tendenciaPct}%
                  </p>
                  <p className="text-sm text-slate-600">vs mes anterior</p>
                </div>
              </div>

              <div className="mt-4">
                <button
                  type="button"
                  className="rounded-full bg-gradient-compi px-4 py-2 text-sm font-semibold text-white shadow hover:opacity-90"
                  onClick={() => navigate('/mis-viajes')}
                >
                  Ver detalle de mis viajes
                </button>
              </div>
            </div>

            <div
                className="rounded-xl border border-slate-500 bg-gray-100 p-5 flex flex-col"
                style={misDatosHeight ? { height: `${misDatosHeight}px` } : undefined}
              >
              <h3 className="text-3xl font-semibold text-slate-800">Mis vehículos</h3>

              <div className="mt-3 flex-1 min-h-0 text-slate-700">
                {vehiculosError && (
                  <p className="mb-3 text-red-600">{vehiculosError}</p>
                )}
                {deleteVehiculoError && (
                  <p className="mb-3 text-red-600">{deleteVehiculoError}</p>
                )}

                {vehiculos.length === 0 && !vehiculosError ? (
                  <p>No tienes vehículos registrados aún.</p>
                ) : (
                  <div className="h-full overflow-y-auto pr-2 space-y-4">
                    {vehiculos.map((vehiculo) => (
                      <div key={vehiculo.id} className="rounded-2xl border border-slate-300 bg-white p-4 shadow-sm">
                        <p className="font-semibold text-slate-900">
                          {vehiculo.marca} {vehiculo.modelo} ({vehiculo.matricula})
                        </p>
                        <p>Tipo: {vehiculo.tipo}</p>
                        <p>Plazas: {vehiculo.plazas}</p>
                        <p>Año: {vehiculo.anio}</p>
                        <p>Consumo: {vehiculo.consumo.toFixed(1)} l/100km</p>
                        <div className="mt-3 flex flex-wrap gap-2">
                          <button
                            type="button"
                            className="rounded-full border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100"
                            onClick={() => openEditVehiculoModal(vehiculo)}
                          >
                            Editar
                          </button>
                          <button
                            type="button"
                            className="rounded-full bg-red-500 px-4 py-2 text-sm font-semibold text-white hover:bg-red-600"
                            onClick={() => handleDeleteVehiculo(vehiculo.id)}
                          >
                            Borrar
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <button
                type="button"
                className="mt-4 rounded-full bg-gradient-compi px-5 py-2 text-sm font-semibold text-white shadow"
                onClick={() => navigate('/vehiculos/nuevo')}
              >
                Añadir un nuevo vehículo
              </button>
            </div>

            <div className="rounded-xl border border-slate-500 bg-gray-100 p-5">
              <h3 className="text-3xl font-semibold text-slate-800">Preferencias de viaje</h3>
              <div className="mt-4 space-y-2 text-lg text-slate-700">
                <label className="flex items-center gap-2">
                  <input type="checkbox" className="h-4 w-4" readOnly />
                  Se permite fumar
                </label>
                <label className="flex items-center gap-2">
                  <input type="checkbox" className="h-4 w-4" readOnly />
                  Se permiten mascotas
                </label>
                <label className="flex items-center gap-2">
                  <input type="checkbox" className="h-4 w-4" readOnly />
                  Música
                </label>
              </div>
            </div>

            <div className="rounded-xl border border-slate-500 bg-gray-100 p-5">
              <h3 className="text-3xl font-semibold text-slate-800">Valoraciones</h3>
              <p className="mt-6 text-xl text-slate-700">
                Puntuación media: {(perfil?.reputacion ?? 0).toFixed(1)} / 5 &nbsp; (0 reseñas)
              </p>
            </div>
          </section>
        </div>

        <div className="mt-4 flex flex-wrap items-center justify-center gap-4">
          <button
            type="button"
            className="rounded-full bg-gradient-compi px-9 py-2 text-base font-semibold text-white shadow"
            onClick={openEditModal}
          >
            Editar perfil
          </button>
          <button
            type="button"
            className="rounded-full bg-red-500 px-9 py-2 text-base font-semibold text-white shadow hover:bg-red-600"
            onClick={() => setShowLogoutConfirm(true)}
          >
            Cerrar sesión
          </button>
        </div>
      </div>

      {showEditModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/55 px-4">
          <div className="w-full max-w-xl rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl">
            <h3 className="text-center text-2xl font-bold text-slate-900">Editar perfil</h3>

            {editError && (
              <div className="mt-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                {editError}
              </div>
            )}

            <div className="mt-4 grid gap-3 md:grid-cols-2">
              <input
                name="nombre"
                type="text"
                placeholder="Nombre"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={editForm.nombre}
                onChange={handleEditChange}
              />
              <input
                name="primerApellido"
                type="text"
                placeholder="Primer apellido"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={editForm.primerApellido}
                onChange={handleEditChange}
              />
              <input
                name="segundoApellido"
                type="text"
                placeholder="Segundo apellido (opcional)"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={editForm.segundoApellido}
                onChange={handleEditChange}
              />
              <input
                name="telefono"
                type="text"
                placeholder="Teléfono"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={editForm.telefono}
                onChange={handleEditChange}
              />
              <input
                name="email"
                type="email"
                placeholder="Email"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm md:col-span-2"
                value={editForm.email}
                onChange={handleEditChange}
              />
              {isEmailChanged && (
                <input
                  name="contrasenaActual"
                  type="password"
                  placeholder="Contraseña actual (necesaria para cambiar email)"
                  className="rounded-md border border-slate-300 px-3 py-2 text-sm md:col-span-2"
                  value={editForm.contrasenaActual}
                  onChange={handleEditChange}
                  autoComplete="new-password"
                />
              )}
            </div>

            <p className="mt-3 text-xs text-slate-500">
              La contraseña no se puede cambiar desde este formulario.
            </p>

            <div className="mt-6 flex justify-center gap-3">
              <button
                type="button"
                className="rounded-full border border-slate-300 px-5 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100"
                onClick={() => setShowEditModal(false)}
                disabled={isSavingProfile}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="rounded-full bg-gradient-compi px-5 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-70"
                onClick={handleSaveProfile}
                disabled={isSavingProfile}
              >
                {isSavingProfile ? 'Guardando...' : 'Guardar cambios'}
              </button>
            </div>
          </div>
        </div>
      )}

      {vehicleEditModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/55 px-4">
          <div className="w-full max-w-xl rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl">
            <h3 className="text-center text-2xl font-bold text-slate-900">Editar vehículo</h3>

            {editVehiculoError && (
              <div className="mt-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                {editVehiculoError}
              </div>
            )}

            <div className="mt-4 grid gap-3 md:grid-cols-2">
              <input
                name="matricula"
                type="text"
                placeholder="Matrícula"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={editVehiculoForm.matricula}
                onChange={handleEditVehiculoChange}
                autoComplete="off"
              />
              <input
                name="marca"
                type="text"
                placeholder="Marca"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={editVehiculoForm.marca}
                onChange={handleEditVehiculoChange}
              />
              <input
                name="modelo"
                type="text"
                placeholder="Modelo"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={editVehiculoForm.modelo}
                onChange={handleEditVehiculoChange}
              />
              <input
                name="plazas"
                type="number"
                min={1}
                max={9}
                placeholder="Plazas"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={editVehiculoForm.plazas}
                onChange={handleEditVehiculoChange}
              />
              <input
                name="consumo"
                type="number"
                min={1}
                step="0.1"
                placeholder="Consumo (l/100km)"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={editVehiculoForm.consumo}
                onChange={handleEditVehiculoChange}
              />
              <input
                name="anio"
                type="number"
                min={1950}
                max={2100}
                placeholder="Año"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
                value={editVehiculoForm.anio}
                onChange={handleEditVehiculoChange}
              />
              <select
                name="tipo"
                value={editVehiculoForm.tipo}
                onChange={handleEditVehiculoChange}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm md:col-span-2"
              >
                <option value="COCHE">Coche</option>
                <option value="MOTO">Moto</option>
                <option value="FURGONETA">Furgoneta</option>
              </select>
            </div>

            <div className="mt-6 flex justify-center gap-3">
              <button
                type="button"
                className="rounded-full border border-slate-300 px-5 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100"
                onClick={() => {
                  setVehicleEditModal(false);
                  setSelectedVehiculo(null);
                }}
                disabled={isSavingVehiculo}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="rounded-full bg-gradient-compi px-5 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-70"
                onClick={handleSaveVehiculo}
                disabled={isSavingVehiculo}
              >
                {isSavingVehiculo ? 'Guardando...' : 'Guardar vehículo'}
              </button>
            </div>
          </div>
        </div>
      )}

      {showLogoutConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/55 px-4">
          <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl">
            <h3 className="text-center text-2xl font-bold text-slate-900">Confirmar cierre de sesión</h3>
            <p className="mt-2 text-center text-sm text-slate-600">
              ¿Seguro que quieres cerrar tu sesión actual?
            </p>

            <div className="mt-6 flex justify-center gap-3">
              <button
                type="button"
                className="rounded-full border border-slate-300 px-5 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100"
                onClick={() => setShowLogoutConfirm(false)}
                disabled={isLoggingOut}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="rounded-full bg-red-500 px-5 py-2 text-sm font-semibold text-white hover:bg-red-600 disabled:cursor-not-allowed disabled:opacity-70"
                onClick={handleLogout}
                disabled={isLoggingOut}
              >
                {isLoggingOut ? 'Cerrando...' : 'Sí, cerrar sesión'}
              </button>
            </div>
          </div>
        </div>
      )}
      </div>
  );
};

export default Perfil;