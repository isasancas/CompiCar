import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import PerfilPublico from '../autenticacion/PerfilPublico';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockPerfilPublicoData = {
  id: 2,
  nombre: 'Laura',
  primerApellido: 'Martínez',
  email: 'laura@test.com',
  telefono: '611223344',
  reputacion: 4.9,
  slug: 'laura-martinez-2',
  preferenciasViaje: ['Mascotas permitidas', 'Silencioso'],
};

const server = setupServer(
  http.get('*/api/personas/laura-martinez-2/perfil-publico', () => {
    return HttpResponse.json(mockPerfilPublicoData);
  }),
  http.get('*/api/viajes/publicos/conductor/laura-martinez-2', () => {
    return HttpResponse.json([]);
  })
);

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  vi.clearAllMocks();
});
afterAll(() => server.close());

const renderComponentWithSlug = (slugRoute: string = '/perfil/laura-martinez-2') => {
  return render(
    <MemoryRouter initialEntries={[slugRoute]}>
      <Routes>
        <Route path="/perfil/:slug" element={<PerfilPublico />} />
        <Route path="/perfil-sin-slug" element={<PerfilPublico />} />
      </Routes>
    </MemoryRouter>
  );
};

test('Carga y muestra correctamente los datos del perfil público y sus preferencias', async () => {
  renderComponentWithSlug();

  expect(screen.getByText(/cargando perfil público.../i)).toBeInTheDocument();

  expect(await screen.findByText('Laura Martínez')).toBeInTheDocument();
  expect(screen.getByText(/laura@test.com/i)).toBeInTheDocument();
  expect(screen.getByText(/611223344/i)).toBeInTheDocument();

  expect(screen.getByText('Mascotas permitidas')).toBeInTheDocument();
  expect(screen.getByText('Silencioso')).toBeInTheDocument();

  expect(screen.getByText(/4.9 \/ 5/i)).toBeInTheDocument();
});

test('Permite volver a la página anterior mediante el botón de retorno', async () => {
  const user = userEvent.setup();
  renderComponentWithSlug();

  const btnVolver = await screen.findByRole('button', { name: /← volver al viaje/i });
  await user.click(btnVolver);

  expect(mockNavigate).toHaveBeenCalledWith(-1);
});

test('Muestra segundo apellido, procesa estados COMPLETADO/CANCELADA y calcula la tendencia respecto al mes anterior', async () => {
  const now = new Date();
  const fechaEsteMes = new Date(now.getFullYear(), now.getMonth(), 10).toISOString();
  const fechaMesAnterior = new Date(now.getFullYear(), now.getMonth() - 1, 10).toISOString();

  server.use(
    http.get('*/api/personas/laura-martinez-2/perfil-publico', () => {
      return HttpResponse.json({
        ...mockPerfilPublicoData,
        segundoApellido: 'García',
      });
    }),
    http.get('*/api/viajes/publicos/conductor/laura-martinez-2', () => {
      return HttpResponse.json([
        { id: 1, fechaHoraSalida: fechaEsteMes, estado: 'COMPLETADO' },
        { id: 2, fechaHoraSalida: fechaEsteMes, estado: 'COMPLETADO' },
        { id: 3, fechaHoraSalida: fechaMesAnterior, estado: 'CANCELADA' },
      ]);
    })
  );

  renderComponentWithSlug();

  expect(await screen.findByText('Laura Martínez García')).toBeInTheDocument();

  expect(await screen.findByText('+100%')).toBeInTheDocument();
});

test('Muestra "Sin preferencias" y reputación por defecto si no vienen informadas', async () => {
  server.use(
    http.get('*/api/personas/laura-martinez-2/perfil-publico', () => {
      return HttpResponse.json({
        id: 2,
        nombre: 'Carlos',
        primerApellido: 'López',
        email: 'carlos@test.com',
        telefono: '600000000',
        slug: 'laura-martinez-2',
        preferenciasViaje: [],
        reputacion: undefined,
      });
    })
  );

  renderComponentWithSlug();

  expect(await screen.findByText('Carlos López')).toBeInTheDocument();
  expect(screen.getByText('Sin preferencias')).toBeInTheDocument();
  expect(screen.getByText(/0.0 \/ 5/i)).toBeInTheDocument();
});

test('Muestra error si la API del perfil responde con estado 404', async () => {
  server.use(
    http.get('*/api/personas/slug-no-existente/perfil-publico', () => {
      return new HttpResponse(null, { status: 404 });
    })
  );

  renderComponentWithSlug('/perfil/slug-no-existente');

  await waitFor(() => {
    expect(screen.getByText(/no se pudo cargar el perfil público/i)).toBeInTheDocument();
  });

  const user = userEvent.setup();
  const btnVolverError = screen.getByRole('button', { name: /^volver$/i });
  await user.click(btnVolverError);

  expect(mockNavigate).toHaveBeenCalledWith(-1);
});

test('Muestra mensaje de error de conexión si fetchPerfilPublico lanza una excepción', async () => {
  server.use(
    http.get('*/api/personas/laura-martinez-2/perfil-publico', () => {
      return HttpResponse.error();
    })
  );

  renderComponentWithSlug();

  await waitFor(() => {
    expect(screen.getByText(/error de conexión al cargar el perfil/i)).toBeInTheDocument();
  });
});

test('Soporta un fallo en fetchResumenActividad sin interrumpir la visualización del perfil', async () => {
  server.use(
    http.get('*/api/viajes/publicos/conductor/laura-martinez-2', () => {
      return new HttpResponse(null, { status: 500 });
    })
  );

  renderComponentWithSlug();

  expect(await screen.findByText('Laura Martínez')).toBeInTheDocument();
});

test('Soporta una excepción de red en fetchResumenActividad sin bloquear el componente', async () => {
  server.use(
    http.get('*/api/viajes/publicos/conductor/laura-martinez-2', () => {
      return HttpResponse.error();
    })
  );

  renderComponentWithSlug();

  expect(await screen.findByText('Laura Martínez')).toBeInTheDocument();
});

test('Muestra error si se renderiza el componente sin el parámetro slug en la URL', async () => {
  renderComponentWithSlug('/perfil-sin-slug');

  await waitFor(() => {
    expect(screen.getByText(/perfil no encontrado/i)).toBeInTheDocument();
  });
});