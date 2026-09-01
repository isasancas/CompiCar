import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../../setupTests';
import TodosLosViajes from '../viajes/TodosLosViajes';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockViajePosterior = {
  id: 1,
  slug: 'madrid-barcelona',
  fechaHoraSalida: '2026-10-20T12:00:00Z',
  estado: 'ACTIVO',
  plazasDisponibles: 3,
  precio: 25,
  vehiculo: { marca: 'Seat', modelo: 'Leon', matricula: '1234ABC' },
  paradas: [
    { localizacion: 'Madrid', tipo: 'ORIGEN', orden: 1 },
    { localizacion: 'Zaragoza', tipo: 'INTERMEDIA', orden: 2 },
    { localizacion: 'Barcelona', tipo: 'DESTINO', orden: 3 },
  ],
};

const mockViajeAnterior = {
  id: 2,
  slug: 'sevilla-malaga',
  fechaHoraSalida: '2026-09-10T08:00:00Z',
  estado: 'COMPLETADO',
  plazasDisponibles: 1,
  precio: 15,
  vehiculo: { marca: 'Renault', modelo: 'Clio', matricula: '5678DEF' },
  paradas: [
    { localizacion: 'Sevilla', tipo: 'ORIGEN', orden: 1 },
    { localizacion: 'Málaga', tipo: 'DESTINO', orden: 2 },
  ],
};

beforeEach(() => {
  vi.clearAllMocks();
});

const renderComponente = () => {
  return render(
    <MemoryRouter initialEntries={['/explorar']}>
      <Routes>
        <Route path="/explorar" element={<TodosLosViajes />} />
      </Routes>
    </MemoryRouter>
  );
};

test('Muestra el estado de carga inicial y luego ordena y renderiza los viajes por fecha ascendente', async () => {
  server.use(
    http.get('*/api/viajes/publicos', () => HttpResponse.json([mockViajePosterior, mockViajeAnterior]))
  );

  renderComponente();

  expect(screen.getByText('Cargando viajes disponibles...')).toBeInTheDocument();

  expect(await screen.findByText('Todos los viajes disponibles')).toBeInTheDocument();
  expect(screen.getByText('2 viajes encontrados')).toBeInTheDocument();

  const titulosVehiculos = screen.getAllByRole('heading', { level: 3 });
  expect(titulosVehiculos[0]).toHaveTextContent('Renault Clio');
  expect(titulosVehiculos[1]).toHaveTextContent('Seat Leon');

  expect(screen.getByText('1. Zaragoza')).toBeInTheDocument();
  expect(screen.getByText('25€')).toBeInTheDocument();
});

test('Muestra el texto en singular cuando solo existe 1 viaje disponible', async () => {
  server.use(
    http.get('*/api/viajes/publicos', () => HttpResponse.json([mockViajePosterior]))
  );

  renderComponente();

  expect(await screen.findByText('1 viaje encontrado')).toBeInTheDocument();
});

test('Renderiza el mensaje de lista vacía cuando no se encuentran viajes', async () => {
  server.use(
    http.get('*/api/viajes/publicos', () => HttpResponse.json([]))
  );

  renderComponente();

  await screen.findByText('Todos los viajes disponibles');
  expect(screen.getByText('0 viajes encontrados')).toBeInTheDocument();
  expect(screen.getByText('No hay viajes disponibles en este momento.')).toBeInTheDocument();
});

test('Muestra mensaje de error cuando la llamada API responde con error HTTP no exitoso', async () => {
  server.use(
    http.get('*/api/viajes/publicos', () => HttpResponse.json({}, { status: 500 }))
  );

  renderComponente();

  expect(await screen.findByText('No se pudieron cargar los viajes disponibles')).toBeInTheDocument();

  const btnInicio = screen.getByRole('button', { name: /volver a inicio/i });
  fireEvent.click(btnInicio);

  expect(mockNavigate).toHaveBeenCalledWith('/');
});

test('Muestra "Error de conexión" cuando falla la red a nivel de socket', async () => {
  server.use(
    http.get('*/api/viajes/publicos', () => HttpResponse.error())
  );

  renderComponente();

  expect(await screen.findByText('Error de conexión')).toBeInTheDocument();
});

test('Navega al detalle del viaje enviando el estado de retorno correcto en la ubicación', async () => {
  server.use(
    http.get('*/api/viajes/publicos', () => HttpResponse.json([mockViajePosterior]))
  );

  renderComponente();

  await screen.findByText('Todos los viajes disponibles');

  const btnDetalle = screen.getByRole('button', { name: /ver detalle/i });
  fireEvent.click(btnDetalle);

  expect(mockNavigate).toHaveBeenCalledWith('/viajes/madrid-barcelona', {
    state: {
      backTo: '/explorar',
      backLabel: 'Volver a Explorar',
    },
  });
});

test('Navega al inicio al hacer clic en el botón superior "← Volver al inicio"', async () => {
  server.use(
    http.get('*/api/viajes/publicos', () => HttpResponse.json([]))
  );

  renderComponente();

  const btnVolver = await screen.findByRole('button', { name: /← volver al inicio/i });
  fireEvent.click(btnVolver);

  expect(mockNavigate).toHaveBeenCalledWith('/');
});