import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { buildApiUrl } from '../apiConfig';

const hasValidToken = () => {
  const token = localStorage.getItem('token');
  return !!token && token !== 'undefined' && token !== 'null' && token.trim() !== '';
};

const Navbar: React.FC = () => {
  const [isLoggedIn, setIsLoggedIn] = useState(hasValidToken());
  const [hasNotifications, setHasNotifications] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    // 1. Lógica de eventos de Auth (lo que ya tenías)
    const handleAuthChange = () => {
      setIsLoggedIn(hasValidToken());
    };

    window.addEventListener('authChange', handleAuthChange);
    window.addEventListener('storage', handleAuthChange);

    // 2. Nueva lógica: Cargar notificaciones si ya está logueado al montar
    const checkNotifications = async () => {
      if (hasValidToken()) {
        try {
          const response = await fetch(buildApiUrl('/api/reservas/pendientes-conductor'), {
            headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
          });
          if (response.ok) {
            const data = await response.json();
            setHasNotifications(data.length > 0);
          }
        } catch (err) {
          console.error("Error al verificar notificaciones");
        }
      }
    };

    checkNotifications();

    return () => {
      window.removeEventListener('authChange', handleAuthChange);
      window.removeEventListener('storage', handleAuthChange);
    };
  }, [isLoggedIn]); // Añadimos isLoggedIn aquí para que re-ejecute al iniciar sesión

  return (
    <nav className="flex items-center justify-between px-10 md:px-16 py-6 bg-[#cfd1cc] border-b border-gray-100 sticky top-0 z-50 shadow-sm">
      <div className="flex items-center">
        <Link to="/">
          <img src="/images/LogoCompleto.png" alt="CompiCar" className="h-12 w-auto object-contain cursor-pointer" />
        </Link>
      </div>

      {!isLoggedIn ? (
        <>
          <ul className="hidden md:flex gap-10 text-base font-semibold text-slate-700">
            <li><a href="#como-funciona" className="hover:text-[#1E50D6] transition-colors">Cómo funciona</a></li>
            <li><a href="#publicar" className="hover:text-[#1E50D6] transition-colors">Publicar un viaje</a></li>
            <li><a href="#contacto" className="hover:text-[#1E50D6] transition-colors">Contacto</a></li>
          </ul>

          <div className="flex gap-6 items-center">
            <span className="text-gray-500 hover:text-gray-900 cursor-pointer text-xl">🔍</span>
            <Link to="/inicio-sesion">
              <button className="text-base font-bold text-gray-600 hover:text-gray-900 transition-colors">
                Iniciar sesión
              </button>
            </Link>
            <Link to="/registro">
              <button className="bg-gradient-compi hover:opacity-90 text-white px-8 py-3 rounded-full text-base font-bold transition-all shadow-lg active:scale-95">
                Registrarse gratis
              </button>
            </Link>
          </div>
        </>
      ) : (
        <div className="flex items-center gap-6">
          <button
            type="button"
            className="text-base font-semibold text-slate-800 hover:text-slate-950 transition-colors"
            onClick={() => navigate('/mis-viajes')}
          >
            Mis viajes
          </button>

          <button
            type="button"
            onClick={() => navigate('/ofrecer-trayecto')}
            className="bg-gradient-compi hover:opacity-90 text-white px-6 py-2 rounded-full text-base font-bold transition-all shadow-md"
          >
            Publicar un viaje
          </button>

          <button type="button" className="text-xl text-slate-700 hover:text-slate-900" aria-label="Buscar">
            🔍
          </button>

          <button 
            type="button" 
            className="relative text-2xl text-slate-700 hover:text-slate-900 transition-transform active:scale-90" 
            aria-label="Notificaciones"
            onClick={() => {
              setHasNotifications(false);
              navigate('/notificaciones');
            }}
          >
            🔔
            {hasNotifications && (
              <span className="absolute top-0 right-0 block h-3 w-3 rounded-full bg-red-500 border-2 border-white"></span>
            )}
          </button>

          <Link to="/perfil" aria-label="Ir al perfil">
            <div
              data-testid="nav-profile-button"
              className="h-12 w-12 rounded-full border-2 border-slate-700 bg-gradient-compi flex items-center justify-center text-white text-lg font-bold"
            >
              👤
            </div>
          </Link>
        </div>
      )}
    </nav>
  );
};

export default Navbar;