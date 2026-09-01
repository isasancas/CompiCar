import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../../setupTests';
import MisViajes from '../viajes/MisViajes';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockViajeConductor = {
  id: 1,
  slug: 'madrid-barcelona-1',
  fechaHoraSalida: '2026-09-01T10:00:00Z',
  estado: 'PENDIENTE',
  plazasDisponibles: 3,
  precio: 25,
  vehiculo: { marca: 'Seat', modelo: 'Leon', matricula: '1234ABC' },
  paradas: [
    { localizacion: 'Madrid', tipo: 'ORIGEN', orden: 1 },
    { localizacion: 'Zaragoza', tipo: 'INTERMEDIA', orden: 2 },
    { localizacion: 'Barcelona', tipo: 'DESTINO', orden: 3 },
  ],
};

const mockViajePasajero = {
  id: 2,
  slug: 'valencia-sevilla-2',
  fechaHoraSalida: '2026-08-01T10:00:00Z',
  estado: 'FINALIZADO',
  plazasDisponibles: 0,
  precio: 30,
  vehiculo: { marca: 'Renault', modelo: 'Clio', matricula: '5678DEF' },
  paradas: [
    { localizacion: 'Valencia', tipo: 'ORIGEN', orden: 1 },
    { localizacion: 'Sevilla', tipo: 'DESTINO', orden: 2 },
  ],
};

const mockViajeRecurrente = {
  id: 3,
  slug: 'bilbao-madrid-3',
  fechaHoraSalida: '2026-09-05T08:00:00Z',
  estado: 'PENDIENTE',
  plazasDisponibles: 2,
  precio: 20,
  diasSemana: ['Lunes', 'Miércoles'],
  fechaFinRecurrencia: '2027-12-31T23:59:59Z',
  vehiculo: { marca: 'Toyota', modelo: 'Corolla', matricula: '9999XYZ' },
  paradas: [
    { localizacion: 'Bilbao', tipo: 'ORIGEN', orden: 1 },
    { localizacion: 'Madrid', tipo: 'DESTINO', orden: 2 },
  ],
  viajesRecurrentes: [
    {
      id: 301,
      slug: 'bilbao-madrid-301',
      fechaHoraSalida: '2026-09-07T08:00:00Z',
      estado: 'PENDIENTE',
      plazasDisponibles: 2,
      precio: 20,
      vehiculo: { marca: 'Toyota', modelo: 'Corolla', matricula: '9999XYZ' },
      paradas: [],
    },
  ],
};

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.setItem('token', 'fake-jwt-token');
});

const renderComponente = () => {
  return render(
    <MemoryRouter initialEntries={['/mis-viajes']}>
      <Routes>
        <Route path="/mis-viajes" element={<MisViajes />} />
      </Routes>
    </MemoryRouter>
  );
};

test('Muestra la pantalla de carga inicial y luego renderiza los viajes pendientes con sus roles y paradas', async () => {
  server.use(
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([mockViajeConductor])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([]))
  );

  renderComponente();

  expect(screen.getByText('Cargando...')).toBeInTheDocument();

  expect(await screen.findByText('Mis viajes')).toBeInTheDocument();
  expect(screen.getByText('Seat Leon')).toBeInTheDocument();
  expect(screen.getByText('Conductor')).toBeInTheDocument();
  expect(screen.getByText('Madrid')).toBeInTheDocument();
  expect(screen.getByText('Barcelona')).toBeInTheDocument();
  expect(screen.getByText('1. Zaragoza')).toBeInTheDocument();
});

test('Alterna correctamente entre la pestaña de viajes activos/pendientes y la de finalizados/cancelados', async () => {
  server.use(
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([mockViajeConductor])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([mockViajePasajero]))
  );

  renderComponente();

  await screen.findByText('Mis viajes');

  expect(screen.getByText('Seat Leon')).toBeInTheDocument();
  expect(screen.queryByText('Renault Clio')).not.toBeInTheDocument();

  const tabFinalizados = screen.getByRole('button', { name: /finalizados y cancelados/i });
  fireEvent.click(tabFinalizados);

  expect(screen.getByText('Renault Clio')).toBeInTheDocument();
  expect(screen.getByText('Pasajero')).toBeInTheDocument();
  expect(screen.queryByText('Seat Leon')).not.toBeInTheDocument();
});

test('Renderiza correctamente la información de un viaje recurrente y despliega sus próximas fechas', async () => {
  server.use(
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([mockViajeRecurrente])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([]))
  );

  renderComponente();

  await screen.findByText('Mis viajes');

  expect(screen.getByText('🔄 Recurrente')).toBeInTheDocument();
  expect(screen.getByText('Lunes, Miércoles')).toBeInTheDocument();

  const details = screen.getByText(/ver próximas fechas/i);
  expect(details).toBeInTheDocument();
});

test('Muestra un estado vacío si no hay viajes que coincidan con el filtro seleccionado', async () => {
  server.use(
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([]))
  );

  renderComponente();

  await screen.findByText('Mis viajes');
  expect(screen.getByText('No hay viajes en esta sección.')).toBeInTheDocument();
});

test('Redirige al usuario al detalle del viaje conservando el estado de navegación al hacer clic en "Ver detalle"', async () => {
  server.use(
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([mockViajeConductor])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([]))
  );

  renderComponente();

  await screen.findByText('Mis viajes');

  const btnDetalle = screen.getByRole('button', { name: /ver detalle/i });
  fireEvent.click(btnDetalle);

  expect(mockNavigate).toHaveBeenCalledWith('/viajes/madrid-barcelona-1', {
    state: {
      backTo: '/mis-viajes',
      backLabel: 'Volver a Mis Viajes',
      rol: 'conductor',
    },
  });
});

test('Muestra pantalla de error y redirige a /inicio-sesion al limpiar sesión si falla la carga de datos', async () => {
  server.use(
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json({ error: 'Error interno' }, { status: 500 })),
    http.get('*/api/viajes/participados', () => HttpResponse.json([], { status: 200 }))
  );

  renderComponente();

  expect(await screen.findByText('Hubo un problema al cargar los viajes.')).toBeInTheDocument();

  const btnLogin = screen.getByRole('button', { name: /ir a iniciar sesión/i });
  fireEvent.click(btnLogin);

  expect(localStorage.getItem('token')).toBeNull();
  expect(mockNavigate).toHaveBeenCalledWith('/inicio-sesion', { replace: true });
});

test('Navega de vuelta al perfil al hacer clic en "Volver al perfil"', async () => {
  server.use(
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([]))
  );

  renderComponente();

  const btnVolver = await screen.findByRole('button', { name: /volver al perfil/i });
  fireEvent.click(btnVolver);

  expect(mockNavigate).toHaveBeenCalledWith('/perfil');
});