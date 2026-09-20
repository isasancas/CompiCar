import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../../setupTests';
import CrearViaje from '../viajes/CrearViaje';

// Mock de Leaflet con captura del evento de clic para el mapa
vi.mock('react-leaflet', () => ({
  MapContainer: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="map-container">{children}</div>
  ),
  TileLayer: () => null,
  CircleMarker: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Polyline: () => null,
  Tooltip: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  useMapEvents: (events: any) => {
    if (events?.click) {
      (window as any).__mapClick = events.click;
    }
    return null;
  },
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

const seleccionarVehiculo = async (id = '10') => {
  await screen.findByRole('option', { name: /Toyota Corolla/i });
  const select = screen.getByRole('combobox') as HTMLSelectElement;
  fireEvent.change(select, { target: { value: id } });
  return select;
};

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

test('Muestra error si falla la carga inicial de vehículos', async () => {
  server.use(
    http.get('*/api/vehiculos/propios', () => new HttpResponse(null, { status: 500 }))
  );
  renderComponent();
  expect(await screen.findByText('No se pudieron cargar tus vehículos.')).toBeInTheDocument();
});

test('Navega a /perfil al pulsar el botón Volver', async () => {
  renderComponent();
  const btnVolver = await screen.findByRole('button', { name: /Volver/i });
  fireEvent.click(btnVolver);
  expect(mockNavigate).toHaveBeenCalledWith('/perfil');
});

test('Ajusta el número de plazas respetando los límites del vehículo', async () => {
  renderComponent();
  await seleccionarVehiculo('10');

  const btnMinus = screen.getByText('-');
  const btnPlus = screen.getByText('+');

  fireEvent.click(btnMinus);
  expect(screen.getByText('1')).toBeInTheDocument();

  fireEvent.click(btnMinus);
  expect(screen.getByText('1')).toBeInTheDocument();

  fireEvent.click(btnPlus);
  fireEvent.click(btnPlus);
  fireEvent.click(btnPlus);
  fireEvent.click(btnPlus);
  expect(screen.getByText('4')).toBeInTheDocument();
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

test('Muestra errores al calcular precio si la distancia es 0 o falla la API', async () => {
  renderComponent();
  await seleccionarVehiculo('10');

  const btnCalcular = screen.getByRole('button', { name: /^Calcular$/i });
  fireEvent.click(btnCalcular);
  expect(await screen.findByText(/Indica una distancia válida/i)).toBeInTheDocument();

  const inputDistancia = screen.getAllByRole('spinbutton')[0];
  fireEvent.change(inputDistancia, { target: { value: '100' } });

  server.use(
    http.post('*/api/viajes/precio/calcular', () => new HttpResponse(null, { status: 500 }))
  );

  fireEvent.click(btnCalcular);
  expect(await screen.findByText(/No se pudo calcular el precio\./i)).toBeInTheDocument();
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

test('Valida destino vacío, precio no válido y precio fuera de horquilla', async () => {
  const { container } = renderComponent();
  await seleccionarVehiculo('10');

  const inputFecha = container.querySelector('input[type="date"]')!;
  const inputHora = container.querySelector('input[type="time"]')!;
  fireEvent.change(inputFecha, { target: { value: '2026-09-01' } });
  fireEvent.change(inputHora, { target: { value: '10:00' } });
  fireEvent.change(screen.getByPlaceholderText(/Ciudad\/dirección de salida/i), {
    target: { value: 'Madrid' },
  });

  submitForm();
  expect(await screen.findByText(/Debes indicar una parada final\./i)).toBeInTheDocument();

  fireEvent.change(screen.getByPlaceholderText(/Ciudad\/dirección de llegada/i), {
    target: { value: 'Barcelona' },
  });

  fireEvent.change(screen.getByPlaceholderText(/Elige precio/i), { target: { value: '0' } });
  submitForm();
  expect(await screen.findByText(/El precio elegido no es válido\./i)).toBeInTheDocument();

  server.use(
    http.post('*/api/viajes/precio/calcular', () =>
      HttpResponse.json({ precioMinimoPasajero: 10, precioMaximoPasajero: 20 })
    )
  );
  const inputDistancia = screen.getAllByRole('spinbutton')[0];
  fireEvent.change(inputDistancia, { target: { value: '100' } });
  fireEvent.click(screen.getByRole('button', { name: /^Calcular$/i }));

  await screen.findByText(/Horquilla calculada/i);

  fireEvent.change(screen.getByPlaceholderText(/Elige precio/i), { target: { value: '50' } });
  submitForm();
  expect(await screen.findByText(/El precio elegido debe estar dentro de la horquilla\./i)).toBeInTheDocument();
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

test('Interacción con el mapa: cambiar modos de selección y geocodificación inversa', async () => {
  server.use(
    http.get('https://nominatim.openstreetmap.org/reverse', () =>
      HttpResponse.json({ display_name: 'Plaza Mayor, Madrid' })
    )
  );

  renderComponent();
  await seleccionarVehiculo('10');

  await waitFor(() => expect((window as any).__mapClick).toBeDefined());

  (window as any).__mapClick({ latlng: { lat: 40.416, lng: -3.703 } });
  expect(await screen.findByText(/Origen seleccionado/i)).toBeInTheDocument();

  (window as any).__mapClick({ latlng: { lat: 41.385, lng: 2.173 } });
  expect(await screen.findByText(/Destino seleccionado/i)).toBeInTheDocument();

  fireEvent.click(screen.getByRole('button', { name: /Editar origen/i }));
  fireEvent.click(screen.getByRole('button', { name: /Editar destino/i }));
  fireEvent.click(screen.getByRole('button', { name: /Añadir intermedia/i }));
});

test('Geocodifica direcciones automáticamente al escribir y calcula la ruta OSRM', async () => {
  server.use(
    http.get('https://nominatim.openstreetmap.org/search', ({ request }) => {
      const url = new URL(request.url);
      const q = url.searchParams.get('q');
      if (q?.includes('Madrid')) {
        return HttpResponse.json([{ lat: '40.416', lon: '-3.703' }]);
      }
      return HttpResponse.json([{ lat: '41.385', lon: '2.173' }]);
    }),
    http.get('https://router.project-osrm.org/route/v1/driving/*', () => {
      return HttpResponse.json({
        routes: [
          {
            geometry: { coordinates: [[-3.703, 40.416], [2.173, 41.385]] },
            distance: 620000,
          },
        ],
      });
    })
  );

  renderComponent();
  await seleccionarVehiculo('10');

  fireEvent.click(screen.getByRole('button', { name: /Añadir parada/i }));
  const inputIntermedia = screen.getByPlaceholderText('Parada intermedia 1');
  fireEvent.click(screen.getAllByRole('button', { name: /Usar en mapa/i })[0]);

  fireEvent.change(screen.getByPlaceholderText(/Ciudad\/dirección de salida/i), {
    target: { value: 'Madrid' },
  });
  fireEvent.change(inputIntermedia, { target: { value: 'Zaragoza' } });
  fireEvent.change(screen.getByPlaceholderText(/Ciudad\/dirección de llegada/i), {
    target: { value: 'Barcelona' },
  });

  await waitFor(
    () => {
      const inputDistancia = screen.getAllByRole('spinbutton')[0] as HTMLInputElement;
      expect(inputDistancia.value).toBe('620');
    },
    { timeout: 3000 }
  );
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
    kilometrosRecorridos: expect.any(Number),
    vehiculo: { id: 10 },
    diasSemana: [],
    fechaFinRecurrencia: null,
  });

  await waitFor(() => {
    expect(mockNavigate).toHaveBeenCalledWith('/mis-viajes');
  }, { timeout: 2000 });
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