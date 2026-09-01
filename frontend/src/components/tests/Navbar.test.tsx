import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../../setupTests';
import Navbar from '../Navbar';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.clear();
});

const renderComponente = () => {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route path="*" element={<Navbar />} />
      </Routes>
    </MemoryRouter>
  );
};

test('Renderiza enlaces de navegación pública y botones de acceso cuando no hay sesión iniciada', () => {
  renderComponente();

  expect(screen.getByAltText('CompiCar')).toBeInTheDocument();
  expect(screen.getByText('Cómo funciona')).toBeInTheDocument();
  expect(screen.getByText('Iniciar sesión')).toBeInTheDocument();
  expect(screen.getByText('Registrarse gratis')).toBeInTheDocument();

  expect(screen.queryByText('Mis viajes')).not.toBeInTheDocument();
  expect(screen.queryByTestId('nav-profile-button')).not.toBeInTheDocument();
});

test('Renderiza opciones privadas y muestra la insignia de notificaciones cuando hay reservas pendientes', async () => {
  localStorage.setItem('token', 'token-valido-123');

  server.use(
    http.get('*/api/reservas/pendientes-conductor', () =>
      HttpResponse.json([{ id: 1, estado: 'PENDIENTE' }])
    )
  );

  renderComponente();

  expect(screen.getByText('Mis viajes')).toBeInTheDocument();
  expect(screen.getByTestId('nav-profile-button')).toBeInTheDocument();

  // Esperar a que la consulta de notificaciones devuelva datos y muestre el indicador
  const btnNotificaciones = await screen.findByRole('button', { name: /notificaciones/i });
  expect(btnNotificaciones.querySelector('span')).toBeInTheDocument();

  fireEvent.click(btnNotificaciones);

  expect(mockNavigate).toHaveBeenCalledWith('/notificaciones');
});

test('Limpia el token de sesión y dispara "authChange" si el endpoint de notificaciones devuelve HTTP 401 o 403', async () => {
  localStorage.setItem('token', 'token-expirado');

  server.use(
    http.get('*/api/reservas/pendientes-conductor', () =>
      HttpResponse.json({ message: 'No autorizado' }, { status: 401 })
    )
  );

  renderComponente();

  await waitFor(() => {
    expect(localStorage.getItem('token')).toBeNull();
  });

  expect(await screen.findByText('Iniciar sesión')).toBeInTheDocument();
});

test('Permite desplegar y cerrar el menú móvil e interactuar con sus enlaces en modo público', () => {
  renderComponente();

  const btnHamburguesa = screen.getByRole('button', { name: /abrir menú móvil/i });
  expect(screen.queryByText('✕')).not.toBeInTheDocument();

  // Abrir menú
  fireEvent.click(btnHamburguesa);
  expect(screen.getByText('✕')).toBeInTheDocument();

  // Hacer clic en un enlace del menú móvil
  const enlaceComoFunciona = screen.getAllByText('Cómo funciona')[1];
  fireEvent.click(enlaceComoFunciona);

  // El menú debe cerrarse de nuevo
  expect(screen.getByText('☰')).toBeInTheDocument();
});

test('Navega correctamente desde las acciones móviles en estado autenticado', async () => {
  localStorage.setItem('token', 'token-valido-123');

  server.use(
    http.get('*/api/reservas/pendientes-conductor', () => HttpResponse.json([]))
  );

  renderComponente();

  const btnHamburguesa = screen.getByRole('button', { name: /abrir menú móvil/i });
  fireEvent.click(btnHamburguesa);

  const btnPerfilMovil = screen.getByText('👤 Mi Perfil');
  fireEvent.click(btnPerfilMovil);

  expect(mockNavigate).toHaveBeenCalledWith('/perfil');
});

test('Actualiza el estado de autenticación al recibir el evento global "authChange"', async () => {
  renderComponente();

  expect(screen.getByText('Iniciar sesión')).toBeInTheDocument();

  // Simular inicio de sesión externo que establece el token y notifica a la app
  localStorage.setItem('token', 'nuevo-token');
  fireEvent(window, new Event('authChange'));

  expect(await screen.findByText('Mis viajes')).toBeInTheDocument();
});