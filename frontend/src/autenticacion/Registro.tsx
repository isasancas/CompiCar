import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PrivacyModal from '../components/PrivacyModal';
import TermsModal from '../components/TermsModal';

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

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target;
    setFormData(prev => ({ ...prev, [id]: value }));
  };

  const handleTermsChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setAcceptedTerms(e.target.checked);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Validar campos requeridos
    if (!formData.nombre || !formData.primerApellido || !formData.email || !formData.password || !formData.telefono) {
      alert('Por favor, rellena todos los campos requeridos.');
      return;
    }

    if (!acceptedTerms) {
      alert('Por favor, acepta los términos y políticas para poder registrarte.');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      alert('Las contraseñas no coinciden.');
      return;
    }

    // Preparar datos para el POST
    const dataToSend = {
      nombre: formData.nombre,
      primerApellido: formData.primerApellido,
      segundoApellido: formData.segundoApellido || null,
      email: formData.email,
      contrasena: formData.password,
      numTelefono: formData.telefono
    };

    try {
      const response = await fetch('http://localhost:8080/api/registro', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(dataToSend)
      });

      if (response.ok) {
        // Después del registro, hacer login automáticamente
        const loginData = {
          email: formData.email,
          contrasena: formData.password
        };

        const loginResponse = await fetch('http://localhost:8080/api/login', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(loginData)
        });

        if (loginResponse.ok) {
          const loginResult = await loginResponse.json();
          localStorage.setItem('token', loginResult.token);
          window.dispatchEvent(new Event('authChange'));
          navigate('/perfil');
        } else {
          alert('Registro exitoso, pero error en el inicio de sesión automático');
        }
      } else {
        alert('Error al registrar usuario');
      }
    } catch (error) {
      console.error('Error:', error);
      alert('Error de conexión');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="bg-white p-8 rounded-lg shadow-lg max-w-md w-full">
        <h2 className="text-2xl font-bold text-center mb-6">Registro</h2>
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
          </div>

          <PrivacyModal isOpen={isPrivacyOpen} onClose={() => setIsPrivacyOpen(false)} />
          <TermsModal isOpen={isTermsOpen} onClose={() => setIsTermsOpen(false)} />
          <div className="flex items-center justify-between">
            <button
              className="bg-gradient-compi hover:opacity-90 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline"
              type="submit"
            >
              Registrarse
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Registro;