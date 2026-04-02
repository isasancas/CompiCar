import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const InicioSesion: React.FC = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [generalError, setGeneralError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({
    email: '',
    password: ''
  });

  const resetFieldErrors = () => ({
    email: '',
    password: ''
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target;
    setFormData(prev => ({ ...prev, [id]: value }));
    setGeneralError('');
    setFieldErrors(prev => ({ ...prev, [id]: '' }));
  };

  const validateClient = () => {
    const nextErrors = resetFieldErrors();
    let hasError = false;

    if (!formData.email.trim()) {
      nextErrors.email = 'El email no puede estar vacío';
      hasError = true;
    } else if (!/^\S+@\S+\.\S+$/.test(formData.email.trim())) {
      nextErrors.email = 'El email no es válido';
      hasError = true;
    }

    if (!formData.password) {
      nextErrors.password = 'La contraseña no puede estar vacía';
      hasError = true;
    }

    setFieldErrors(nextErrors);
    return !hasError;
  };

  const mapBackendError = (errorMsg: string) => {
    const normalized = errorMsg.toLowerCase();
    const nextErrors = resetFieldErrors();

    if (normalized.includes('email')) {
      nextErrors.email = errorMsg;
      setFieldErrors(nextErrors);
      return;
    }

    if (normalized.includes('contraseña') || normalized.includes('contrasena')) {
      nextErrors.password = errorMsg;
      setFieldErrors(nextErrors);
      return;
    }

    setFieldErrors(nextErrors);
    setGeneralError(errorMsg);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    setGeneralError('');
    if (!validateClient()) {
      return;
    }

    setIsSubmitting(true);

    const dataToSend = {
      email: formData.email.trim(),
      contrasena: formData.password
    };

    try {
      const response = await fetch('http://localhost:8080/api/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(dataToSend)
      });

      if (response.ok) {
        const result = await response.json();
        const token = typeof result?.token === 'string' ? result.token.trim() : '';
        if (!token) {
          setGeneralError('No se recibió un token válido del servidor.');
          return;
        }

        localStorage.setItem('token', token);
        window.dispatchEvent(new Event('authChange'));
        navigate('/perfil');
      } else {
        localStorage.removeItem('token');
        window.dispatchEvent(new Event('authChange'));

        let backendError = 'Error en el inicio de sesión';
        try {
          const errorBody = await response.json();
          backendError =
            (typeof errorBody?.error === 'string' && errorBody.error) ||
            (typeof errorBody?.message === 'string' && errorBody.message) ||
            backendError;
        } catch {
          // Si no viene un JSON parseable, se mantiene el mensaje por defecto.
        }

        mapBackendError(backendError);
      }
    } catch (error) {
      console.error('Error:', error);
      setGeneralError('Error de conexión');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            Iniciar Sesión
          </h2>
        </div>
        {generalError && (
          <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {generalError}
          </div>
        )}
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          <div className="rounded-md shadow-sm -space-y-px">
            <div>
              <label htmlFor="email" className="sr-only">Email</label>
              <input
                id="email"
                name="email"
                type="email"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-t-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
                placeholder="Email"
                value={formData.email}
                onChange={handleChange}
              />
              {fieldErrors.email && <p className="mt-1 text-sm text-red-600">{fieldErrors.email}</p>}
            </div>
            <div>
              <label htmlFor="password" className="sr-only">Contraseña</label>
              <input
                id="password"
                name="password"
                type="password"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-b-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
                placeholder="Contraseña"
                value={formData.password}
                onChange={handleChange}
              />
              {fieldErrors.password && <p className="mt-1 text-sm text-red-600">{fieldErrors.password}</p>}
            </div>
          </div>

          <div>
            <button
              type="submit"
              className="bg-gradient-compi hover:opacity-90 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Iniciando...' : 'Iniciar Sesión'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default InicioSesion;