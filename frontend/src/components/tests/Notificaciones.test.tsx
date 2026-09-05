import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import Notificaciones from '../notificacion/Notificaciones';

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockReservas = [
  {
    id: 1,
    cantidadPlazas: 2,
    persona: {
      nombre: 'Ana Gómez',
      slug: 'ana-gomez',
    },
    viaje: {
      fechaHoraSalida: '2026-06-10T10:00:00Z',
    },
  },
];

const mockAvisos = [
  {
    id: 101,
    tipo: 'VIAJE_CANCELADO',
    mensaje: 'El viaje a Madrid ha sido cancelado por el conductor.',
    fechaCreacion: new Date().toISOString(),
    leida: false,
  },
];

const server = setupServer(
  http.get('*/api/reservas/pendientes-conductor', () => HttpResponse.json(mockReservas)),
  http.get('*/api/notificaciones/mis-notificaciones', () => HttpResponse.json(mockAvisos)),
  http.put('*/api/reservas/confirmar', () => new HttpResponse(null, { status: 200 })),
  http.put('*/api/reservas/rechazar', () => new HttpResponse(null, { status: 200 }))
);

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  localStorage.clear();
  vi.clearAllMocks();
});
afterAll(() => server.close());

const renderComponent = () => {
  render(
    <MemoryRouter>
      <Notificaciones />
    </MemoryRouter>
  );
};

test('Carga y muestra correctamente las solicitudes de reserva y avisos recientes', async () => {
  renderComponent();

  expect(screen.getByText(/cargando bandeja.../i)).toBeInTheDocument();

  expect(await screen.findByText(/ana gómez quiere viajar contigo/i)).toBeInTheDocument();
  expect(screen.getByText(/reserva para 2 plazas/i)).toBeInTheDocument();
  expect(screen.getByText(/viaje cancelado/i)).toBeInTheDocument();
  expect(screen.getByText(/el viaje a madrid ha sido cancelado/i)).toBeInTheDocument();
});

test('Permite aceptar una solicitud de reserva correctamente', async () => {
  const user = userEvent.setup();
  renderComponent();

  const botonAceptar = await screen.findByRole('button', { name: /^aceptar$/i });
  await user.click(botonAceptar);

  await waitFor(() => {
    expect(screen.queryByText(/ana gómez quiere viajar contigo/i)).not.toBeInTheDocument();
  });

  expect(screen.getByText(/no hay solicitudes pendientes/i)).toBeInTheDocument();
});

test('Permite rechazar una solicitud de reserva correctamente', async () => {
  const user = userEvent.setup();
  renderComponent();

  const botonRechazar = await screen.findByRole('button', { name: /^rechazar$/i });
  await user.click(botonRechazar);

  await waitFor(() => {
    expect(screen.queryByText(/ana gómez quiere viajar contigo/i)).not.toBeInTheDocument();
  });

  expect(screen.getByText(/no hay solicitudes pendientes/i)).toBeInTheDocument();
});

test('Navega al perfil del usuario al hacer clic en "Ver perfil"', async () => {
  const user = userEvent.setup();
  renderComponent();

  const botonPerfil = await screen.findByRole('button', { name: /ver perfil/i });
  await user.click(botonPerfil);

  expect(mockNavigate).toHaveBeenCalledWith('/usuarios/ana-gomez/perfil');
});

test('Muestra fechas de viaje desde reserva.viajeRecurrente o fechaHoraSalida directa', async () => {
  server.use(
    http.get('*/api/reservas/pendientes-conductor', () =>
      HttpResponse.json([
        {
          id: 2,
          cantidadPlazas: 1,
          persona: { nombre: 'Carlos Vives', slug: 'carlos-vives' },
          viajeRecurrente: { fechaHoraSalida: '2026-07-01T08:00:00Z' },
        },
        {
          id: 3,
          cantidadPlazas: 3,
          persona: { nombre: 'Marta Sánchez', slug: 'marta-sanchez' },
          fechaHoraSalida: '2026-08-15T12:00:00Z',
        },
        {
          id: 4,
          cantidadPlazas: 1,
          persona: { nombre: 'Sin Fecha', slug: 'sin-fecha' },
        },
      ])
    )
  );

  renderComponent();

  expect(await screen.findByText(/carlos vives quiere viajar contigo/i)).toBeInTheDocument();
  expect(screen.getByText(/marta sánchez quiere viajar contigo/i)).toBeInTheDocument();
  expect(screen.getByText(/sin fecha quiere viajar contigo/i)).toBeInTheDocument();
});

test('Filtra notificaciones antiguas de más de 7 días y renderiza varios tipos y estados de leída', async () => {
  const hace10Dias = new Date(Date.now() - 10 * 24 * 60 * 60 * 1000).toISOString();
  const hace1Dia = new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString();

  server.use(
    http.get('*/api/notificaciones/mis-notificaciones', () =>
      HttpResponse.json([
        {
          id: 201,
          tipo: 'RESERVA_CANCELADA',
          mensaje: 'Un pasajero ha cancelado su reserva.',
          fechaCreacion: hace1Dia,
          leida: true,
        },
        {
          id: 202,
          tipo: 'INFO_GENERAL',
          mensaje: 'Bienvenido a la plataforma.',
          fechaCreacion: hace1Dia,
          leida: false,
        },
        {
          id: 203,
          tipo: 'NUEVA_RESERVA',
          mensaje: 'Aviso sin fecha de creación.',
          leida: false,
        },
        {
          id: 204,
          tipo: 'VIAJE_CANCELADO',
          mensaje: 'Aviso muy antiguo que debe filtrarse.',
          fechaCreacion: hace10Dias,
          leida: false,
        },
      ])
    )
  );

  renderComponent();

  expect(await screen.findByText(/un pasajero ha cancelado su reserva/i)).toBeInTheDocument();
  expect(screen.getByText(/bienvenido a la plataforma/i)).toBeInTheDocument();
  expect(screen.getByText(/aviso sin fecha de creación/i)).toBeInTheDocument();
  expect(screen.queryByText(/aviso muy antiguo que debe filtrarse/i)).not.toBeInTheDocument();
});

test('Muestra avisos de listas vacías cuando no hay datos', async () => {
  server.use(
    http.get('*/api/reservas/pendientes-conductor', () => HttpResponse.json([])),
    http.get('*/api/notificaciones/mis-notificaciones', () => HttpResponse.json([]))
  );

  renderComponent();

  expect(await screen.findByText(/no hay solicitudes pendientes/i)).toBeInTheDocument();
  expect(screen.getByText(/no hay actividad reciente/i)).toBeInTheDocument();
});

test('Captura errores de red al gestionar una reserva y muestra alerta', async () => {
  window.alert = vi.fn();
  server.use(
    http.put('*/api/reservas/confirmar', () => HttpResponse.error())
  );

  const user = userEvent.setup();
  renderComponent();

  const botonAceptar = await screen.findByRole('button', { name: /^aceptar$/i });
  await user.click(botonAceptar);

  await waitFor(() => {
    expect(window.alert).toHaveBeenCalledWith('Error de conexión');
  });
});

test('Captura errores en fetchTodo sin romper la interfaz', async () => {
  const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
  server.use(
    http.get('*/api/reservas/pendientes-conductor', () => HttpResponse.error())
  );

  renderComponent();

  await waitFor(() => {
    expect(screen.queryByText(/cargando bandeja.../i)).not.toBeInTheDocument();
  });

  expect(screen.getByText(/no hay solicitudes pendientes/i)).toBeInTheDocument();
  consoleSpy.mockRestore();
});