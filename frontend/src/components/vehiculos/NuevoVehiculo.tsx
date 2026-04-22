import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { buildApiUrl } from '../../apiConfig';

const tiposVehiculo = ['COCHE', 'MOTO', 'FURGONETA'] as const;
type TipoVehiculo = (typeof tiposVehiculo)[number];

const NuevoVehiculo: React.FC = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    matricula: '',
    marca: '',
    modelo: '',
    plazas: '',
    consumo: '',
    anio: '',
    tipo: 'COCHE'
  });
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const getValidToken = () => {
    const token = localStorage.getItem('token');
    if (!token || token === 'undefined' || token === 'null' || token.trim() === '') {
      return null;
    }
    return token;
  };

  useEffect(() => {
    const token = getValidToken();
    if (!token) {
      navigate('/inicio-sesion', { replace: true });
    }
  }, [navigate]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setError(null);
    setSuccess(null);
  };

  const validateForm = () => {
    const matricula = form.matricula.trim().toUpperCase();
    if (!matricula) {
      setError('La matrícula es obligatoria.');
      return false;
    }

    const matriculaRegex = /^[0-9]{4}[A-Z]{3}$|^[A-Z]{1,2}[0-9]{4}[A-Z]{1,2}$/;
    if (!matriculaRegex.test(matricula)) {
      setError('El formato de matrícula no es válido.');
      return false;
    }

    if (!form.marca.trim()) {
      setError('La marca es obligatoria.');
      return false;
    }

    if (!form.modelo.trim()) {
      setError('El modelo es obligatorio.');
      return false;
    }

    const plazas = Number(form.plazas);
    if (!Number.isInteger(plazas) || plazas < 1 || plazas > 9) {
      setError('Las plazas deben ser un número entero entre 1 y 9.');
      return false;
    }

    const consumo = Number(form.consumo);
    if (Number.isNaN(consumo) || consumo <= 0) {
      setError('El consumo debe ser un número mayor que 0.');
      return false;
    }

    const anio = Number(form.anio);
    if (!Number.isInteger(anio) || anio < 1950 || anio > 2100) {
      setError('El año debe estar entre 1950 y 2100.');
      return false;
    }

    if (!tiposVehiculo.includes(form.tipo as TipoVehiculo)) {
      setError('Debes seleccionar un tipo de vehículo válido.');
      return false;
    }

    return true;
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!validateForm()) {
      return;
    }

    const token = getValidToken();
    if (!token) {
      navigate('/inicio-sesion', { replace: true });
      return;
    }

    setIsSaving(true);

    try {
      const response = await fetch(buildApiUrl('/api/vehiculos'), {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          matricula: form.matricula.trim().toUpperCase(),
          marca: form.marca.trim(),
          modelo: form.modelo.trim(),
          plazas: Number(form.plazas),
          consumo: Number(form.consumo),
          anio: Number(form.anio),
          tipo: form.tipo
        })
      });

      if (response.ok) {
        navigate('/perfil');
        return;
      } else {
        const body = await response.json().catch(() => null);
        const backendError = body?.error || body?.message || 'No se pudo crear el vehículo.';
        setError(backendError);
      }
    } catch {
      setError('Error de conexión al crear el vehículo.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-200 pb-10 pt-6">
      <div className="mx-auto max-w-3xl px-4">
        <button
          type="button"
          onClick={() => navigate('/perfil')}
          className="rounded-full border border-green-600 px-4 py-1 text-sm text-green-700 transition hover:bg-green-50"
        >
          Volver al perfil
        </button>

        <div className="mt-6 rounded-3xl border border-slate-300 bg-white p-6 shadow-sm">
          <h1 className="text-3xl font-bold text-slate-900">Añadir vehículo</h1>
          <p className="mt-2 text-slate-600">Introduce los datos de tu vehículo para poder usarlo en los trayectos.</p>

          {error && (
            <div className="mt-4 rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {error}
            </div>
          )}

          {success && (
            <div className="mt-4 rounded-md border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">
              {success}
            </div>
          )}

          <form className="mt-6 grid gap-4" onSubmit={handleSubmit}>
            <input
              name="matricula"
              value={form.matricula}
              onChange={handleChange}
              placeholder="Matrícula"
              className="rounded-xl border border-slate-300 px-4 py-3 text-sm"
              autoComplete="off"
            />
            <div className="grid gap-4 md:grid-cols-2">
              <input
                name="marca"
                value={form.marca}
                onChange={handleChange}
                placeholder="Marca"
                className="rounded-xl border border-slate-300 px-4 py-3 text-sm"
              />
              <input
                name="modelo"
                value={form.modelo}
                onChange={handleChange}
                placeholder="Modelo"
                className="rounded-xl border border-slate-300 px-4 py-3 text-sm"
              />
            </div>

            <div className="grid gap-4 md:grid-cols-3">
              <input
                name="plazas"
                type="number"
                min={1}
                max={9}
                value={form.plazas}
                onChange={handleChange}
                placeholder="Plazas"
                className="rounded-xl border border-slate-300 px-4 py-3 text-sm"
              />
              <input
                name="consumo"
                type="number"
                min={1}
                step="0.1"
                value={form.consumo}
                onChange={handleChange}
                placeholder="Consumo (l/100km)"
                className="rounded-xl border border-slate-300 px-4 py-3 text-sm"
              />
              <input
                name="anio"
                type="number"
                min={1950}
                max={2100}
                value={form.anio}
                onChange={handleChange}
                placeholder="Año"
                className="rounded-xl border border-slate-300 px-4 py-3 text-sm"
              />
            </div>

            <select
              name="tipo"
              value={form.tipo}
              onChange={handleChange}
              className="rounded-xl border border-slate-300 px-4 py-3 text-sm"
            >
              {tiposVehiculo.map((tipo) => (
                <option key={tipo} value={tipo}>
                  {tipo.charAt(0) + tipo.slice(1).toLowerCase()}
                </option>
              ))}
            </select>

            <button
              type="submit"
              disabled={isSaving}
              className="inline-flex items-center justify-center rounded-full bg-gradient-compi px-6 py-3 text-sm font-semibold text-white shadow transition hover:opacity-95 disabled:cursor-not-allowed disabled:opacity-70"
            >
              {isSaving ? 'Guardando...' : 'Guardar vehículo'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default NuevoVehiculo;
