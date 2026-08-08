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
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const handleAuthChange = () => {
      setIsLoggedIn(hasValidToken());
    };

    window.addEventListener('authChange', handleAuthChange);
    window.addEventListener('storage', handleAuthChange);

    const checkNotifications = async () => {
      if (hasValidToken()) {
        try {
          const response = await fetch(buildApiUrl('/api/reservas/pendientes-conductor'), {
            headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
          });
          
          if (response.ok) {
            const data = await response.json();
            setHasNotifications(data.length > 0);
          } else if (response.status === 401 || response.status === 403) {
            localStorage.removeItem('token');
            window.dispatchEvent(new Event('authChange'));
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
  }, [isLoggedIn]);

  return (
    <nav className="flex items-center justify-between px-6 sm:px-10 md:px-16 py-4 md:py-5 bg-[#cfd1cc] border-b border-gray-200 sticky top-0 z-50 shadow-sm">
      
      {/* LOGO */}
      <div className="flex items-center">
        <Link to="/" onClick={() => setMobileMenuOpen(false)}>
          <img src="/images/LogoCompleto.png" alt="CompiCar" className="h-9 sm:h-11 md:h-12 w-auto object-contain cursor-pointer" />
        </Link>
      </div>

      {/* --- VISTA ORDENADOR (DESKTOP) --- */}
      {!isLoggedIn ? (
        <>
          {/* Enlaces de navegación central */}
          <ul className="hidden md:flex gap-8 text-sm lg:text-base font-semibold text-slate-700">
            <li><a href="#como-funciona" className="hover:text-[#1E50D6] transition-colors">Cómo funciona</a></li>
            <li><a href="#publicar" className="hover:text-[#1E50D6] transition-colors">Publicar un viaje</a></li>
            <li><a href="#contacto" className="hover:text-[#1E50D6] transition-colors">Contacto</a></li>
          </ul>

          {/* Botones de acción desktop */}
          <div className="hidden md:flex gap-5 items-center">
            <button className="text-gray-600 hover:text-gray-900 cursor-pointer text-xl p-1" aria-label="Buscar">
              🔍
            </button>
            <Link to="/inicio-sesion">
              <button className="text-sm lg:text-base font-bold text-slate-700 hover:text-slate-950 transition-colors px-3 py-2">
                Iniciar sesión
              </button>
            </Link>
            <Link to="/registro">
              <button className="bg-gradient-compi hover:opacity-90 text-white px-6 py-2.5 rounded-full text-sm lg:text-base font-bold transition-all shadow-md active:scale-95">
                Registrarse gratis
              </button>
            </Link>
          </div>
        </>
      ) : (
        /* Acciones de usuario logueado en Desktop */
        <div className="hidden md:flex items-center gap-5 lg:gap-6">
          <button
            type="button"
            className="text-sm lg:text-base font-semibold text-slate-800 hover:text-slate-950 transition-colors"
            onClick={() => navigate('/mis-viajes')}
          >
            Mis viajes
          </button>

          <button
            type="button"
            onClick={() => navigate('/ofrecer-trayecto')}
            className="bg-gradient-compi hover:opacity-90 text-white px-5 py-2 rounded-full text-sm lg:text-base font-bold transition-all shadow-md"
          >
            Publicar un viaje
          </button>

          <button type="button" className="text-xl text-slate-700 hover:text-slate-900 p-1" aria-label="Buscar">
            🔍
          </button>

          <button 
            type="button" 
            className="relative text-2xl text-slate-700 hover:text-slate-900 transition-transform active:scale-90 p-1" 
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
              className="h-10 w-10 lg:h-11 lg:w-11 rounded-full border-2 border-slate-700 bg-gradient-compi flex items-center justify-center text-white text-base font-bold shadow-sm"
            >
              👤
            </div>
          </Link>
        </div>
      )}

      {/* --- BOTÓN MENÚ HAMBURGUESA (MÓVIL) --- */}
      <div className="flex md:hidden items-center gap-3">
        {/* Notificación rápida visible en móvil si está logueado */}
        {isLoggedIn && (
          <button 
            type="button" 
            className="relative text-xl text-slate-700 p-1" 
            onClick={() => {
              setHasNotifications(false);
              navigate('/notificaciones');
            }}
          >
            🔔
            {hasNotifications && (
              <span className="absolute top-0 right-0 block h-2.5 w-2.5 rounded-full bg-red-500 border-2 border-white"></span>
            )}
          </button>
        )}

        <button
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          className="p-2 rounded-xl text-slate-800 hover:bg-slate-300/50 transition-colors focus:outline-none"
          aria-label="Abrir menú móvil"
        >
          <span className="text-2xl">{mobileMenuOpen ? '✕' : '☰'}</span>
        </button>
      </div>

      {/* --- PANEL DESPLEGABLE MÓVIL --- */}
      {mobileMenuOpen && (
        <div className="absolute top-full left-0 w-full bg-[#cfd1cc] border-b border-gray-300 shadow-xl py-5 px-6 flex flex-col gap-4 md:hidden animate-fadeIn">
          {!isLoggedIn ? (
            <>
              <a 
                href="#como-funciona" 
                onClick={() => setMobileMenuOpen(false)}
                className="text-base font-semibold text-slate-700 py-2 border-b border-gray-300/40"
              >
                Cómo funciona
              </a>
              <a 
                href="#publicar" 
                onClick={() => setMobileMenuOpen(false)}
                className="text-base font-semibold text-slate-700 py-2 border-b border-gray-300/40"
              >
                Publicar un viaje
              </a>
              <a 
                href="#contacto" 
                onClick={() => setMobileMenuOpen(false)}
                className="text-base font-semibold text-slate-700 py-2 border-b border-gray-300/40"
              >
                Contacto
              </a>
              
              <div className="flex flex-col gap-3 pt-3">
                <Link to="/inicio-sesion" onClick={() => setMobileMenuOpen(false)}>
                  <button className="w-full py-3 text-center text-sm font-bold text-slate-800 bg-white/60 rounded-xl hover:bg-white transition-colors">
                    Iniciar sesión
                  </button>
                </Link>
                <Link to="/registro" onClick={() => setMobileMenuOpen(false)}>
                  <button className="w-full bg-gradient-compi text-white py-3 rounded-xl text-sm font-bold shadow-md text-center">
                    Registrarse gratis
                  </button>
                </Link>
              </div>
            </>
          ) : (
            <>
              <button
                onClick={() => { setMobileMenuOpen(false); navigate('/mis-viajes'); }}
                className="text-left text-base font-semibold text-slate-800 py-2 border-b border-gray-300/40"
              >
                Mis viajes
              </button>
              <button
                onClick={() => { setMobileMenuOpen(false); navigate('/ofrecer-trayecto'); }}
                className="text-left text-base font-semibold text-slate-800 py-2 border-b border-gray-300/40"
              >
                Publicar un viaje
              </button>
              <button
                onClick={() => { setMobileMenuOpen(false); navigate('/notificaciones'); }}
                className="text-left text-base font-semibold text-slate-800 py-2 border-b border-gray-300/40 flex justify-between items-center"
              >
                <span>Notificaciones</span>
                {hasNotifications && <span className="h-2.5 w-2.5 rounded-full bg-red-500"></span>}
              </button>
              <button
                onClick={() => { setMobileMenuOpen(false); navigate('/perfil'); }}
                className="text-left text-base font-semibold text-slate-800 py-2 flex items-center gap-2"
              >
                <span>👤 Mi Perfil</span>
              </button>
            </>
          )}
        </div>
      )}
    </nav>
  );
};

export default Navbar;