import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../../setupTests';
import HomeLoggedIn from '../HomeLoggedIn';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockPerfil = {
  id: 10,
  nombre: 'Carlos',
  primerApellido: 'García',
};

const mockViajePasajero = {
  id: 101,
  fechaHoraSalida: '2026-09-15T10:00:00Z',
  estado: 'ACTIVO',
  plazasDisponibles: 2,
  precio: 20,
  slug: 'madrid-valencia',
  conductorId: 99,
  conductorNombre: 'Ana Lopez',
  paradas: [
    { id: 1, localizacion: 'Madrid', tipo: 'ORIGEN', orden: 1 },
    { id: 2, localizacion: 'Valencia', tipo: 'DESTINO', orden: 2 },
  ],
};

const mockViajeConductor = {
  ...mockViajePasajero,
  conductorId: 10,
  conductorNombre: 'Carlos',
};

const mockTopConductores = [
  { id: 1, nombre: 'Laura', primerApellido: 'Pérez', valoracionMedia: 4.85 },
  { id: 2, nombre: 'David', primerApellido: 'Ruiz', reputacion: 5 },
];

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.setItem('token', 'token-valido-123');
});

afterEach(() => {
  localStorage.clear();
});

const renderComponente = () => {
  return render(
    <MemoryRouter initialEntries={['/inicio']}>
      <Routes>
        <Route path="/inicio" element={<HomeLoggedIn />} />
      </Routes>
    </MemoryRouter>
  );
};

test('Carga y muestra el perfil del usuario, el próximo viaje como pasajero y el listado de top conductores', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfil)),
    http.get('*/api/viajes/proximo', () => HttpResponse.json(mockViajePasajero)),
    http.get('*/api/personas/top-conductores', () => HttpResponse.json(mockTopConductores))
  );

  renderComponente();

  // Verificación de saludo personalizado
  expect(await screen.findByText('Hola, Carlos')).toBeInTheDocument();

  // Verificación del próximo viaje (rol Pasajero)
  expect(screen.getByText('Pasajero')).toBeInTheDocument();
  expect(screen.getByText('Ana Lopez')).toBeInTheDocument();
  expect(screen.getByText('Madrid')).toBeInTheDocument();
  expect(screen.getByText('Valencia')).toBeInTheDocument();

  // Verificación de top conductores con formateo de valoración
  expect(screen.getByText('👤 Laura Pérez')).toBeInTheDocument();
  expect(screen.getByText('4.8 ⭐')).toBeInTheDocument();
  expect(screen.getByText('👤 David Ruiz')).toBeInTheDocument();
  expect(screen.getByText('5.0 ⭐')).toBeInTheDocument();
});

test('Detecta cuando el usuario es el conductor del próximo viaje y navega enviando el rol correcto', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfil)),
    http.get('*/api/viajes/proximo', () => HttpResponse.json(mockViajeConductor)),
    http.get('*/api/personas/top-conductores', () => HttpResponse.json([]))
  );

  renderComponente();

  expect(await screen.findByText('Eres el conductor')).toBeInTheDocument();
  expect(screen.getByText('Conductor')).toBeInTheDocument();

  const btnDetalles = screen.getByRole('button', { name: /ver detalles/i });
  fireEvent.click(btnDetalles);

  expect(mockNavigate).toHaveBeenCalledWith('/viajes/madrid-valencia', {
    state: {
      backTo: '/inicio',
      backLabel: 'Volver al inicio',
      rol: 'conductor',
    },
  });
});

test('Muestra adecuadamente los estados vacíos de viajes y conductores cuando las APIs devuelven HTTP 204 o listas vacías', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfil)),
    http.get('*/api/viajes/proximo', () => new HttpResponse(null, { status: 204 })),
    http.get('*/api/personas/top-conductores', () => HttpResponse.json([]))
  );

  renderComponente();

  expect(await screen.findByText('No tienes ningún viaje próximo programado.')).toBeInTheDocument();
  expect(screen.getByText('Aún no hay conductores destacados disponibles.')).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: /ver detalles/i })).not.toBeInTheDocument();
});

test('Procesa el formulario de búsqueda redirigiendo con los parámetros ingresados', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfil)),
    http.get('*/api/viajes/proximo', () => new HttpResponse(null, { status: 204 })),
    http.get('*/api/personas/top-conductores', () => HttpResponse.json([]))
  );

  const { container } = renderComponente();

  await screen.findByText('Hola, Carlos');

  const inputOrigen = screen.getByPlaceholderText('Origen');
  const inputDestino = screen.getByPlaceholderText('Destino');
  const inputFecha = container.querySelector<HTMLInputElement>('input[type="date"]')!;
  const btnBuscar = screen.getByRole('button', { name: /buscar viaje/i });

  fireEvent.change(inputOrigen, { target: { value: 'Sevilla' } });
  fireEvent.change(inputDestino, { target: { value: 'Córdoba' } });
  fireEvent.change(inputFecha, { target: { value: '2026-11-01' } });

  fireEvent.click(btnBuscar);

  expect(mockNavigate).toHaveBeenCalledWith('/buscar?origen=Sevilla&destino=C%C3%B3rdoba&fecha=2026-11-01');
});

test('Navega a /ofrecer-trayecto al presionar el botón "Publicar un viaje"', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfil)),
    http.get('*/api/viajes/proximo', () => new HttpResponse(null, { status: 204 })),
    http.get('*/api/personas/top-conductores', () => HttpResponse.json([]))
  );

  renderComponente();

  const btnPublicar = await screen.findByRole('button', { name: /publicar un viaje/i });
  fireEvent.click(btnPublicar);

  expect(mockNavigate).toHaveBeenCalledWith('/ofrecer-trayecto');
});

test('Usa el nombre genérico "usuario" si no existe token en localStorage', async () => {
  localStorage.removeItem('token');

  renderComponente();

  expect(screen.getByText('Hola, usuario')).toBeInTheDocument();
});