import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { buildApiUrl } from '../../apiConfig';
import PrivacyModal from '../PrivacyModal';
import TermsModal from '../TermsModal';

const Registro: React.FC = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    nombre: '',
    primerApellido: '',
    segundoApellido: '',
    telefono: '',
    email: '',
    password: '',
    confirmPassword: ''
  });
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [isPrivacyOpen, setIsPrivacyOpen] = useState(false);
  const [isTermsOpen, setIsTermsOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showSuccessModal, setShowSuccessModal] = useState(false);
  const [generalError, setGeneralError] = useState('');
  const [termsError, setTermsError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({
    nombre: '',
    primerApellido: '',
    segundoApellido: '',
    telefono: '',
    email: '',
    password: '',
    confirmPassword: ''
  });

  const resetFieldErrors = () => ({
    nombre: '',
    primerApellido: '',
    segundoApellido: '',
    telefono: '',
    email: '',
    password: '',
    confirmPassword: ''
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target;
    setFormData(prev => ({ ...prev, [id]: value }));
    setGeneralError('');
    setFieldErrors(prev => ({ ...prev, [id]: '' }));
  };

  const handleTermsChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setAcceptedTerms(e.target.checked);
    if (e.target.checked) {
      setTermsError('');
    }
  };

  const mapBackendErrorToFields = (errorMsg: string) => {
    const nextErrors = resetFieldErrors();
    let fallbackError = '';

    errorMsg
      .split(';')
      .map(msg => msg.trim())
      .filter(Boolean)
      .forEach((msg) => {
        const normalized = msg.toLowerCase();

        if (normalized.includes('primer apellido')) {
          nextErrors.primerApellido = msg;
          return;
        }

        if (normalized.includes('nombre')) {
          nextErrors.nombre = msg;
          return;
        }

        if (normalized.includes('email')) {
          nextErrors.email = msg;
          return;
        }

        if (normalized.includes('teléfono') || normalized.includes('telefono')) {
          nextErrors.telefono = msg;
          return;
        }

        if (normalized.includes('contraseña') || normalized.includes('contrasena')) {
          nextErrors.password = msg;
          return;
        }

        fallbackError = fallbackError || msg;
      });

    setFieldErrors(nextErrors);
    setGeneralError(fallbackError);
  };

  const validateClient = () => {
    const nextErrors = resetFieldErrors();
    let hasError = false;

    if (!formData.nombre.trim()) {
      nextErrors.nombre = 'El nombre no puede estar vacío';
      hasError = true;
    }

    if (!formData.primerApellido.trim()) {
      nextErrors.primerApellido = 'El primer apellido no puede estar vacío';
      hasError = true;
    }

    if (!formData.email.trim()) {
      nextErrors.email = 'El email no puede estar vacío';
      hasError = true;
    } else if (!/^\S+@\S+\.\S+$/.test(formData.email.trim())) {
      nextErrors.email = 'El email no es válido';
      hasError = true;
    }

    if (!formData.telefono.trim()) {
      nextErrors.telefono = 'El teléfono no puede estar vacío';
      hasError = true;
    } else if (!/^\+?[0-9]{7,15}$/.test(formData.telefono.trim())) {
      nextErrors.telefono = 'El teléfono no es válido';
      hasError = true;
    }

    if (!formData.password) {
      nextErrors.password = 'La contraseña no puede estar vacía';
      hasError = true;
    }

    if (!formData.confirmPassword) {
      nextErrors.confirmPassword = 'Debes confirmar la contraseña';
      hasError = true;
    } else if (formData.password !== formData.confirmPassword) {
      nextErrors.confirmPassword = 'Las contraseñas no coinciden';
      hasError = true;
    }

    if (!acceptedTerms) {
      setTermsError('Por favor, acepta los términos y la política de privacidad.');
      hasError = true;
    } else {
      setTermsError('');
    }

    setFieldErrors(nextErrors);
    return !hasError;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    setGeneralError('');
    if (!validateClient()) {
      return;
    }

    setIsSubmitting(true);

    const dataToSend = {
      nombre: formData.nombre.trim(),
      primerApellido: formData.primerApellido.trim(),
      segundoApellido: formData.segundoApellido.trim() || null,
      email: formData.email.trim(),
      contrasena: formData.password,
      numTelefono: formData.telefono.trim()
    };

    try {
      const response = await fetch(buildApiUrl('/api/registro'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(dataToSend)
      });

      if (response.ok) {
        const loginData = {
          email: formData.email.trim(),
          contrasena: formData.password
        };

        const loginResponse = await fetch(buildApiUrl('/api/login'), {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(loginData)
        });

        if (loginResponse.ok) {
          const loginResult = await loginResponse.json();
          const token = typeof loginResult?.token === 'string' ? loginResult.token.trim() : '';
          if (!token) {
            setGeneralError('Registro exitoso, pero no se recibió un token válido. Inicia sesión manualmente.');
            return;
          }

          localStorage.setItem('token', token);
          window.dispatchEvent(new Event('authChange'));
          setShowSuccessModal(true);
        } else {
          localStorage.removeItem('token');
          window.dispatchEvent(new Event('authChange'));
          setGeneralError('Registro exitoso, pero error en el inicio de sesión automático.');
        }
      } else {
        let backendError = 'Error al registrar usuario';
        try {
          const errorBody = await response.json();
          backendError = typeof errorBody?.error === 'string' ? errorBody.error : backendError;
        } catch {
          // Si no viene JSON válido, dejamos el mensaje por defecto.
        }

        mapBackendErrorToFields(backendError);
      }
    } catch (error) {
      console.error('Error:', error);
      setGeneralError('Error de conexión');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="bg-white p-8 rounded-lg shadow-lg max-w-md w-full">
        <h2 className="text-2xl font-bold text-center mb-6">Registro</h2>
        {generalError && (
          <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {generalError}
          </div>
        )}
        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="nombre">
              Nombre
            </label>
            <input
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
              id="nombre"
              type="text"
              placeholder="Nombre"
              value={formData.nombre}
              onChange={handleChange}
            />
            {fieldErrors.nombre && <p className="mt-1 text-sm text-red-600">{fieldErrors.nombre}</p>}
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="primerApellido">
              Primer Apellido
            </label>
            <input
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
              id="primerApellido"
              type="text"
              placeholder="Primer apellido"
              value={formData.primerApellido}
              onChange={handleChange}
            />
            {fieldErrors.primerApellido && <p className="mt-1 text-sm text-red-600">{fieldErrors.primerApellido}</p>}
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="segundoApellido">
              Segundo Apellido
            </label>
            <input
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
              id="segundoApellido"
              type="text"
              placeholder="Segundo apellido (opcional)"
              value={formData.segundoApellido}
              onChange={handleChange}
            />
            {fieldErrors.segundoApellido && <p className="mt-1 text-sm text-red-600">{fieldErrors.segundoApellido}</p>}
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="telefono">
              Teléfono
            </label>
            <input
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
              id="telefono"
              type="text"
              placeholder="666 666 666"
              value={formData.telefono}
              onChange={handleChange}
            />
            {fieldErrors.telefono && <p className="mt-1 text-sm text-red-600">{fieldErrors.telefono}</p>}
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="email">
              Email
            </label>
            <input
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
              id="email"
              type="email"
              placeholder="ejemplo@dominio.com"
              value={formData.email}
              onChange={handleChange}
            />
            {fieldErrors.email && <p className="mt-1 text-sm text-red-600">{fieldErrors.email}</p>}
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="password">
              Contraseña
            </label>
            <input
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
              id="password"
              type="password"
              placeholder="Contraseña"
              value={formData.password}
              onChange={handleChange}
            />
            {fieldErrors.password && <p className="mt-1 text-sm text-red-600">{fieldErrors.password}</p>}
          </div>
          <div className="mb-6">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="confirmPassword">
              Confirmar Contraseña
            </label>
            <input
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
              id="confirmPassword"
              type="password"
              placeholder="Confirma tu contraseña"
              value={formData.confirmPassword}
              onChange={handleChange}
            />
            {fieldErrors.confirmPassword && <p className="mt-1 text-sm text-red-600">{fieldErrors.confirmPassword}</p>}
          </div>
          <div className="mb-4">
            <label className="inline-flex items-start">
              <input
                type="checkbox"
                checked={acceptedTerms}
                onChange={handleTermsChange}
                className="form-checkbox h-5 w-5 text-blue-600 mt-1"
              />
              <span className="ml-2 text-gray-700 text-sm">
                He leído y acepto los{' '}
                <button
                  type="button"
                  onClick={() => setIsTermsOpen(true)}
                  className="text-blue-600 hover:underline"
                >
                  términos y condiciones
                </button>{' '}
                y la{' '}
                <button
                  type="button"
                  onClick={() => setIsPrivacyOpen(true)}
                  className="text-blue-600 hover:underline"
                >
                  política de privacidad
                </button>
                .
              </span>
            </label>
            {termsError && <p className="mt-2 text-sm text-red-600">{termsError}</p>}
          </div>

          <PrivacyModal isOpen={isPrivacyOpen} onClose={() => setIsPrivacyOpen(false)} />
          <TermsModal isOpen={isTermsOpen} onClose={() => setIsTermsOpen(false)} />
          <div className="flex items-center justify-between">
            <button
              className="bg-gradient-compi hover:opacity-90 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline"
              type="submit"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Registrando...' : 'Registrarse'}
            </button>
          </div>
        </form>
      </div>

      {showSuccessModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/55 px-4">
          <div className="w-full max-w-md rounded-2xl border border-green-100 bg-white p-6 shadow-2xl">
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-green-100">
              <svg viewBox="0 0 24 24" className="h-8 w-8 text-green-600" fill="none" stroke="currentColor" strokeWidth="2.5">
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              </svg>
            </div>

            <h3 className="text-center text-2xl font-extrabold text-slate-900">Registro completado</h3>
            <p className="mt-2 text-center text-sm text-slate-600">
              Tu cuenta se ha creado correctamente y tu sesión ya está iniciada.
            </p>

            <div className="mt-6 flex justify-center">
              <button
                type="button"
                className="rounded-full bg-gradient-compi px-6 py-2 text-sm font-bold text-white shadow-md transition hover:opacity-90"
                onClick={() => {
                  setShowSuccessModal(false);
                  navigate('/perfil');
                }}
              >
                Ir a mi perfil
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Registro;