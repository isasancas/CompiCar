import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../../setupTests';
import CrearViaje from '../viajes/CrearViaje';

// Mock de Leaflet
vi.mock('react-leaflet', () => ({
  MapContainer: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="map-container">{children}</div>
  ),
  TileLayer: () => null,
  CircleMarker: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Polyline: () => null,
  Tooltip: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  useMapEvents: () => null,
  useMap: () => ({ fitBounds: vi.fn() }),
}));

// Mock API Config
vi.mock('../../apiConfig', () => ({
  buildApiUrl: (path: string) => path,
}));

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockVehiculos = [
  { id: 10, marca: 'Toyota', modelo: 'Corolla', matricula: '1234ABC', plazas: 4, consumo: 5.5 },
  { id: 20, marca: 'Ford', modelo: 'Focus', matricula: '5678DEF', plazas: 3, consumo: 6.0 },
];

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.clear();
  localStorage.setItem('token', 'fake-jwt-token');
  server.use(
    http.get('*/api/vehiculos/propios', () => HttpResponse.json(mockVehiculos))
  );
});

afterEach(() => {
  server.resetHandlers();
  cleanup();
});

const renderComponent = () => {
  return render(
    <MemoryRouter initialEntries={['/crear-viaje']}>
      <Routes>
        <Route path="/crear-viaje" element={<CrearViaje />} />
        <Route path="/mis-viajes" element={<div>Mis Viajes Screen</div>} />
        <Route path="/perfil" element={<div>Perfil Screen</div>} />
      </Routes>
    </MemoryRouter>
  );
};

// Función auxiliar para esperar a que carguen las opciones antes de seleccionar vehículo
const seleccionarVehiculo = async (id = '10') => {
  await screen.findByRole('option', { name: /Toyota Corolla/i });
  const select = screen.getByRole('combobox') as HTMLSelectElement;
  fireEvent.change(select, { target: { value: id } });
  return select;
};

// Función auxiliar para forzar el envío del formulario sin bloqueo HTML5
const submitForm = () => {
  const btnPublicar = screen.getByRole('button', { name: /Crear y publicar trayecto/i });
  const form = btnPublicar.closest('form') || btnPublicar.parentElement!;
  fireEvent.submit(form);
};

test('Carga los vehículos del usuario y los muestra en el selector', async () => {
  renderComponent();

  expect(await screen.findByRole('option', { name: /Toyota Corolla/i })).toBeInTheDocument();

  await waitFor(() => {
    const select = screen.getByRole('combobox') as HTMLSelectElement;
    expect(select.children.length).toBe(2);
    expect(select.value).toBe('10');
  });

  expect(screen.getByText('Toyota Corolla - 1234ABC')).toBeInTheDocument();
  expect(screen.getByText('Ford Focus - 5678DEF')).toBeInTheDocument();
});

test('Permite añadir y eliminar paradas intermedias', async () => {
  renderComponent();

  await seleccionarVehiculo('10');

  const btnAdd = screen.getByRole('button', { name: /Añadir parada/i });
  fireEvent.click(btnAdd);

  const inputIntermedia1 = screen.getByPlaceholderText('Parada intermedia 1');
  expect(inputIntermedia1).toBeInTheDocument();

  fireEvent.click(btnAdd);
  expect(screen.getByPlaceholderText('Parada intermedia 2')).toBeInTheDocument();

  const botonesQuitar = screen.getAllByRole('button', { name: /Quitar/i });
  fireEvent.click(botonesQuitar[0]);

  expect(screen.queryByPlaceholderText('Parada intermedia 2')).not.toBeInTheDocument();
  expect(screen.getByPlaceholderText('Parada intermedia 1')).toBeInTheDocument();
});

test('Calcula la horquilla de precio correctamente desde el backend', async () => {
  server.use(
    http.post('*/api/viajes/precio/calcular', () => {
      return HttpResponse.json({
        precioMinimoPasajero: 12.5,
        precioMaximoPasajero: 25.0,
        fuente: 'Calculadora Oficial',
        detalle: 'Cálculo basado en consumo',
      });
    })
  );

  renderComponent();

  await seleccionarVehiculo('10');

  const inputDistancia = screen.getAllByRole('spinbutton')[0];
  fireEvent.change(inputDistancia, { target: { value: '150' } });

  const btnCalcular = screen.getByRole('button', { name: /^Calcular$/i });
  fireEvent.click(btnCalcular);

  expect(await screen.findByText(/Horquilla calculada: 12.50€ - 25.00€/i)).toBeInTheDocument();
  expect(screen.getByText(/Puedes elegir un precio dentro de este rango: 12.50€ - 25.00€/i)).toBeInTheDocument();
});

test('Muestra errores de validación si faltan campos requeridos al publicar', async () => {
  const { container } = renderComponent();

  await seleccionarVehiculo('10');

  submitForm();

  expect(await screen.findByText(/Selecciona fecha y hora de salida\./i)).toBeInTheDocument();

  const inputFecha = container.querySelector('input[type="date"]')!;
  const inputHora = container.querySelector('input[type="time"]')!;
  fireEvent.change(inputFecha, { target: { value: '2026-09-01' } });
  fireEvent.change(inputHora, { target: { value: '10:00' } });

  submitForm();
  expect(await screen.findByText(/Debes indicar una parada inicial\./i)).toBeInTheDocument();
});

test('Muestra un error de validación para viajes recurrentes sin días ni fecha de fin', async () => {
  const { container } = renderComponent();

  await seleccionarVehiculo('10');

  fireEvent.change(screen.getByPlaceholderText(/Ciudad\/dirección de salida/i), {
    target: { value: 'BU-900, Quintanilla del Coco' },
  });
  fireEvent.change(screen.getByPlaceholderText(/Ciudad\/dirección de llegada/i), {
    target: { value: 'Calle Cyesa, Madrid' },
  });

  const inputsFecha = container.querySelectorAll('input[type="date"]');
  const inputHora = container.querySelector('input[type="time"]') as HTMLInputElement;
  
  fireEvent.change(inputsFecha[0], { target: { value: '2026-09-01' } });
  fireEvent.change(inputHora, { target: { value: '18:19' } });

  const spinbuttons = screen.getAllByRole('spinbutton');
  if (spinbuttons.length > 0) {
    fireEvent.change(spinbuttons[0], { target: { value: '205.39' } });
  }
  fireEvent.change(screen.getByPlaceholderText(/Elige precio/i), {
    target: { value: '41.98' },
  });

  const checkboxRepetir = screen.getByLabelText(/¿Se repite este viaje de forma recurrente\?/i);
  fireEvent.click(checkboxRepetir);

  const diaLunes = await screen.findByText('L');

  submitForm();

  expect(
    await screen.findByText(/Selecciona al menos un día de la semana para la recurrencia\./i)
  ).toBeInTheDocument();

  fireEvent.click(diaLunes);
  submitForm();

  expect(
    await screen.findByText(/Indica la fecha de fin de recurrencia\./i)
  ).toBeInTheDocument();
});

test('Publica con éxito un viaje puntual y redirige a mis-viajes', async () => {
  let payloadEnviado: any = null;

  server.use(
    http.post('*/api/viajes/crear', async ({ request }) => {
      payloadEnviado = await request.json();
      return HttpResponse.json({ id: 999, status: 'CREATED' });
    })
  );

  const { container } = renderComponent();

  await seleccionarVehiculo('10');

  fireEvent.change(screen.getByPlaceholderText(/Ciudad\/dirección de salida/i), { target: { value: 'Madrid' } });
  fireEvent.change(screen.getByPlaceholderText(/Ciudad\/dirección de llegada/i), { target: { value: 'Valencia' } });

  const inputsFecha = container.querySelectorAll('input[type="date"]');
  const inputHora = container.querySelector('input[type="time"]') as HTMLInputElement;
  fireEvent.change(inputsFecha[0], { target: { value: '2026-09-15' } });
  fireEvent.change(inputHora, { target: { value: '14:30' } });

  fireEvent.change(screen.getByPlaceholderText(/Elige precio/i), { target: { value: '18.50' } });

  submitForm();

  expect(await screen.findByText(/Trayecto creado correctamente\./i)).toBeInTheDocument();

  expect(payloadEnviado).toMatchObject({
    fechaHoraSalida: '2026-09-15T14:30:00',
    estado: 'PENDIENTE',
    plazasDisponibles: 2,
    precio: 18.5,
    vehiculo: { id: 10 },
    diasSemana: [],
    fechaFinRecurrencia: null,
  });

  expect(payloadEnviado.paradas.length).toBe(2);
  expect(payloadEnviado.paradas[0]).toMatchObject({ localizacion: 'Madrid', tipo: 'ORIGEN', orden: 1 });
  expect(payloadEnviado.paradas[1]).toMatchObject({ localizacion: 'Valencia', tipo: 'DESTINO', orden: 2 });

  await waitFor(() => {
    expect(mockNavigate).toHaveBeenCalledWith('/mis-viajes');
  }, { timeout: 2000 });
});

test('Publica con éxito un viaje recurrente', async () => {
  let payloadEnviado: any = null;

  server.use(
    http.post('*/api/viajes/crear', async ({ request }) => {
      payloadEnviado = await request.json();
      return HttpResponse.json({ id: 1000, status: 'CREATED' });
    })
  );

  const { container } = renderComponent();

  await seleccionarVehiculo('10');

  fireEvent.change(screen.getByPlaceholderText(/Ciudad\/dirección de salida/i), { target: { value: 'Sevilla' } });
  fireEvent.change(screen.getByPlaceholderText(/Ciudad\/dirección de llegada/i), { target: { value: 'Córdoba' } });

  const inputHora = container.querySelector('input[type="time"]') as HTMLInputElement;
  fireEvent.change(inputHora, { target: { value: '07:00' } });

  fireEvent.change(screen.getByPlaceholderText(/Elige precio/i), { target: { value: '10' } });

  const checkboxRepetir = screen.getByLabelText(/¿Se repite este viaje de forma recurrente\?/i);
  fireEvent.click(checkboxRepetir);

  await waitFor(() => {
    expect(container.querySelectorAll('input[type="date"]').length).toBe(2);
  });

  const inputsFecha = container.querySelectorAll('input[type="date"]');
  fireEvent.change(inputsFecha[0], { target: { value: '2026-10-01' } });
  fireEvent.change(inputsFecha[1], { target: { value: '2026-10-31' } });

  fireEvent.click(screen.getByText('L'));
  fireEvent.click(screen.getByText('X'));

  submitForm();

  expect(await screen.findByText(/Trayecto creado correctamente\./i)).toBeInTheDocument();

  expect(payloadEnviado).toMatchObject({
    diasSemana: ['L', 'X'],
    fechaFinRecurrencia: '2026-10-31T23:59:59',
  });
});

test('Muestra mensaje de error devuelto por la API cuando falla la creación', async () => {
  server.use(
    http.post('*/api/viajes/crear', () => {
      return new HttpResponse(
        JSON.stringify({ message: 'El vehículo especificado no está activo' }),
        {
          status: 400,
          headers: { 'Content-Type': 'application/json' },
        }
      );
    })
  );

  const { container } = renderComponent();

  await seleccionarVehiculo('10');

  fireEvent.change(screen.getByPlaceholderText(/Ciudad\/dirección de salida/i), {
    target: { value: 'Bilbao' },
  });
  fireEvent.change(screen.getByPlaceholderText(/Ciudad\/dirección de llegada/i), {
    target: { value: 'San Sebastián' },
  });

  const inputsFecha = container.querySelectorAll('input[type="date"]');
  const inputHora = container.querySelector('input[type="time"]') as HTMLInputElement;
  fireEvent.change(inputsFecha[0], { target: { value: '2026-09-20' } });
  fireEvent.change(inputHora, { target: { value: '09:00' } });

  fireEvent.change(screen.getByPlaceholderText(/Elige precio/i), {
    target: { value: '15' },
  });

  submitForm();

  expect(
    await screen.findByText(/El vehículo especificado no está activo/i)
  ).toBeInTheDocument();
});