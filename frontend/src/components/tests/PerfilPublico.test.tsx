import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../../setupTests';
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

beforeEach(() => {
  server.use(
    http.get('*/api/personas/laura-martinez-2/perfil-publico', () => {
      return HttpResponse.json(mockPerfilPublicoData);
    }),
    http.get('*/api/valoraciones/valorado/2', () => {
      return HttpResponse.json([]);
    }),
    http.get('*/api/viajes/publicos/conductor/laura-martinez-2/exitosos', () => {
      return HttpResponse.json(0);
    }),
    http.get('*/api/viajes/publicos/conductor/laura-martinez-2/participados', () => {
      return HttpResponse.json(0);
    }),
    http.get('*/api/reservas/ratio-exito', () => {
      return HttpResponse.json(0);
    })
  );
  vi.clearAllMocks();
});

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

test('Muestra las valoraciones recibidas con el nombre completo del autor', async () => {
  server.use(
    http.get('*/api/valoraciones/valorado/2', () => HttpResponse.json([
      {
        id: 101,
        puntuacion: 5,
        comentario: 'Excelente pasajera, muy puntual.',
        fecha: '2026-01-10T10:00:00Z',
        autorId: 3,
        autorNombre: 'Carlos García López',
      },
    ]))
  );

  const user = userEvent.setup();
  renderComponentWithSlug();

  const btnValoraciones = await screen.findByRole('button', { name: 'Ver valoraciones recibidas' });
  await user.click(btnValoraciones);

  expect(screen.getByText('Carlos García López')).toBeInTheDocument();
  expect(screen.getByText('Excelente pasajera, muy puntual.')).toBeInTheDocument();
  expect(screen.getByText('5/5')).toBeInTheDocument();
  expect(screen.getByLabelText('5 de 5 estrellas')).toBeInTheDocument();
  expect(screen.getByText(/\(1 reseña\)/)).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'Ocultar valoraciones' }));
  expect(screen.queryByText('Carlos García López')).not.toBeInTheDocument();
});

test('Muestra el estado vacío cuando el perfil no tiene valoraciones recibidas', async () => {
  const user = userEvent.setup();
  renderComponentWithSlug();

  await user.click(await screen.findByRole('button', { name: 'Ver valoraciones recibidas' }));

  expect(screen.getByText('Todavía no tiene valoraciones recibidas.')).toBeInTheDocument();
  expect(screen.getByText(/\(0 reseñas\)/)).toBeInTheDocument();
});

test('Permite volver a la página anterior mediante el botón de retorno', async () => {
  const user = userEvent.setup();
  renderComponentWithSlug();

  const btnVolver = await screen.findByRole('button', { name: /← volver al viaje/i });
  await user.click(btnVolver);

  expect(mockNavigate).toHaveBeenCalledWith(-1);
});

test('Muestra el segundo apellido y las estadísticas del perfil público', async () => {
  server.use(
    http.get('*/api/personas/laura-martinez-2/perfil-publico', () => {
      return HttpResponse.json({
        ...mockPerfilPublicoData,
        segundoApellido: 'García',
      });
    }),
    http.get('*/api/viajes/publicos/conductor/laura-martinez-2/exitosos', () => HttpResponse.json(2)),
    http.get('*/api/viajes/publicos/conductor/laura-martinez-2/participados', () => HttpResponse.json(3)),
    http.get('*/api/reservas/ratio-exito?slug=laura-martinez-2', () => HttpResponse.json(75))
  );

  renderComponentWithSlug();

  expect(await screen.findByText('Laura Martínez García')).toBeInTheDocument();

  expect(screen.getByText('Viajes completados').parentElement).toHaveTextContent('2');
  expect(screen.getByText('Viajes participados').parentElement).toHaveTextContent('3');
  expect(screen.getByText('Ratio de éxito reservas').parentElement).toHaveTextContent('75%');
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
    http.get('*/api/viajes/publicos/conductor/laura-martinez-2/exitosos', () => {
      return new HttpResponse(null, { status: 500 });
    })
  );

  renderComponentWithSlug();

  expect(await screen.findByText('Laura Martínez')).toBeInTheDocument();
});

test('Soporta una excepción de red en fetchResumenActividad sin bloquear el componente', async () => {
  server.use(
    http.get('*/api/viajes/publicos/conductor/laura-martinez-2/exitosos', () => {
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