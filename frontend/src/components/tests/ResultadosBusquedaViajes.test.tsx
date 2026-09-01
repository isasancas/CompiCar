import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../../setupTests';
import ResultadosBusquedaViajes from '../viajes/ResultadosBusquedaViajes';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockViaje = {
  id: 101,
  slug: 'madrid-valencia-express',
  fechaHoraSalida: '2026-10-15T09:00:00Z',
  estado: 'PENDIENTE',
  plazasDisponibles: 2,
  precio: 18.5,
  vehiculo: { id: 1, marca: 'Ford', modelo: 'Focus', matricula: '9876LMN' },
  paradas: [
    { id: 1, localizacion: 'Madrid', tipo: 'ORIGEN', orden: 1 },
    { id: 2, localizacion: 'Cuenca', tipo: 'INTERMEDIA', orden: 2 },
    { id: 3, localizacion: 'Valencia', tipo: 'DESTINO', orden: 3 },
  ],
};

beforeEach(() => {
  vi.clearAllMocks();
});

const renderComponente = (initialUrl = '/buscar') => {
  return render(
    <MemoryRouter initialEntries={[initialUrl]}>
      <Routes>
        <Route path="/buscar" element={<ResultadosBusquedaViajes />} />
      </Routes>
    </MemoryRouter>
  );
};

test('Carga los parámetros de la URL en los inputs y solicita la lista de viajes a la API', async () => {
  let endpointSolicitado = '';

  server.use(
    http.get('*/api/viajes/publicos', ({ request }) => {
      endpointSolicitado = request.url;
      return HttpResponse.json([mockViaje]);
    })
  );

  renderComponente('/buscar?origen=Madrid&destino=Valencia');

  expect(screen.getByPlaceholderText('Origen')).toHaveValue('Madrid');
  expect(screen.getByPlaceholderText('Destino')).toHaveValue('Valencia');
  expect(screen.getByText('Cargando viajes...')).toBeInTheDocument();

  expect(await screen.findByText('Madrid → Valencia')).toBeInTheDocument();
  expect(screen.getByText('Plazas: 2')).toBeInTheDocument();
  expect(screen.getByText('Precio: 18.50 €')).toBeInTheDocument();
  expect(screen.getByText('Vehículo: Ford Focus')).toBeInTheDocument();
  expect(screen.getByText('Intermedias: Cuenca')).toBeInTheDocument();

  expect(endpointSolicitado).toContain('origen=Madrid');
  expect(endpointSolicitado).toContain('destino=Valencia');
});

test('Dispara una nueva búsqueda y actualiza los parámetros al enviar el formulario', async () => {
  server.use(
    http.get('*/api/viajes/publicos', () => HttpResponse.json([]))
  );

  renderComponente('/buscar');

  await screen.findByText('No hay viajes para esos filtros.');

  const inputOrigen = screen.getByPlaceholderText('Origen');
  const inputDestino = screen.getByPlaceholderText('Destino');
  const btnBuscar = screen.getByRole('button', { name: /buscar/i });

  fireEvent.change(inputOrigen, { target: { value: 'Sevilla' } });
  fireEvent.change(inputDestino, { target: { value: 'Granada' } });
  fireEvent.click(btnBuscar);

  expect(inputOrigen).toHaveValue('Sevilla');
  expect(inputDestino).toHaveValue('Granada');
});

test('Fuerza el re-fetch de resultados si los parámetros de búsqueda no han cambiado', async () => {
  let contadorPeticiones = 0;

  server.use(
    http.get('*/api/viajes/publicos', () => {
      contadorPeticiones++;
      return HttpResponse.json([mockViaje]);
    })
  );

  renderComponente('/buscar?origen=Madrid');

  await screen.findByText('Madrid → Valencia');
  expect(contadorPeticiones).toBe(1);

  const btnBuscar = screen.getByRole('button', { name: /buscar/i });
  fireEvent.click(btnBuscar);

  await waitFor(() => {
    expect(contadorPeticiones).toBe(2);
  });
});

test('Muestra un mensaje de estado vacío cuando no existen resultados coincidentes', async () => {
  server.use(
    http.get('*/api/viajes/publicos', () => HttpResponse.json([]))
  );

  renderComponente('/buscar');

  expect(await screen.findByText('No hay viajes para esos filtros.')).toBeInTheDocument();
});

test('Muestra un mensaje de error si falla la llamada HTTP a la API', async () => {
  server.use(
    http.get('*/api/viajes/publicos', () =>
      HttpResponse.json({ message: 'No se pudieron cargar los viajes' }, { status: 500 })
    )
  );

  renderComponente('/buscar');

  expect(await screen.findByText('No se pudieron cargar los viajes')).toBeInTheDocument();
});

test('Redirige a la pantalla de detalle del viaje transmitiendo los parámetros de navegación', async () => {
  server.use(
    http.get('*/api/viajes/publicos', () => HttpResponse.json([mockViaje]))
  );

  renderComponente('/buscar?origen=Madrid');

  await screen.findByText('Madrid → Valencia');

  const btnDetalle = screen.getByRole('button', { name: /ver detalle/i });
  fireEvent.click(btnDetalle);

  expect(mockNavigate).toHaveBeenCalledWith('/viajes/madrid-valencia-express', {
    state: {
      backTo: '/buscar?origen=Madrid',
      backLabel: 'Volver a resultados',
    },
  });
});

test('Redirige a la página principal al pulsar en el botón "Volver"', async () => {
  server.use(
    http.get('*/api/viajes/publicos', () => HttpResponse.json([]))
  );

  renderComponente('/buscar');

  const btnVolver = screen.getByRole('button', { name: /volver/i });
  fireEvent.click(btnVolver);

  expect(mockNavigate).toHaveBeenCalledWith('/');
});