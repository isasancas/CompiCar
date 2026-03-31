import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

interface PerfilData {
  nombre: string;
  primerApellido: string;
  segundoApellido?: string;
  email: string;
  telefono: string;
  // Agregar otros campos si es necesario
}

const Perfil: React.FC = () => {
  const [perfil, setPerfil] = useState<PerfilData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchPerfil = async () => {
      const token = localStorage.getItem('token');
      if (!token) {
        setError('No hay token de autenticación');
        setLoading(false);
        return;
      }

      try {
        const response = await fetch('http://localhost:8080/api/personas/perfil', {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });

        if (response.ok) {
          const data = await response.json();
          setPerfil(data);
        } else {
          setError('Error al obtener el perfil');
        }
      } catch {
        setError('Error de conexión');
      } finally {
        setLoading(false);
      }
    };

    fetchPerfil();
  }, []);

  const handleLogout = async () => {
    const token = localStorage.getItem('token'); 
    if (!token) {
      setError('No hay token de autenticación');
      return;
    }
    try {
      const response = await fetch('http://localhost:8080/api/logout', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      if (response.ok) {
        localStorage.removeItem('token');
        window.dispatchEvent(new Event('authChange'));
        navigate('/');
      } else {
        setError('Error al cerrar sesión');
      }
    } catch {
      setError('Error de conexión al cerrar sesión');
    }
  };

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center">Cargando...</div>;
  }

  if (error) {
    return <div className="min-h-screen flex items-center justify-center text-red-500">{error}</div>;
  }


  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="bg-white p-8 rounded-lg shadow-lg max-w-md w-full">
        <h2 className="text-2xl font-bold text-center mb-6">Mi Perfil</h2>
        {perfil && (
          <div>
            <p><strong>Nombre:</strong> {perfil.nombre}</p>
            <p><strong>Primer Apellido:</strong> {perfil.primerApellido}</p>
            {perfil.segundoApellido && <p><strong>Segundo Apellido:</strong> {perfil.segundoApellido}</p>}
            <p><strong>Email:</strong> {perfil.email}</p>
            <p><strong>Teléfono:</strong> {perfil.telefono}</p>
          </div>
        )}
        <div>
        {
            <button
              type="submit"
              className="bg-gradient-compi hover:opacity-90 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline"
              onClick={handleLogout}
            >
              Cerrar Sesión
            </button>
        }
      </div>
      </div>
    </div>
  );
};

export default Perfil;