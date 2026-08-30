import { render, screen, waitFor } from '@testing-library/react';
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
  preferenciasViaje: ['Mascotas permitidas', 'Silencioso']
};

const renderComponentWithSlug = (slug: string = 'laura-martinez-2') => {
  return render(
    <MemoryRouter initialEntries={[`/perfil/${slug}`]}>
      <Routes>
        <Route path="/perfil/:slug" element={<PerfilPublico />} />
      </Routes>
    </MemoryRouter>
  );
};

test('Carga y muestra correctamente los datos del perfil público y sus preferencias', async () => {
  server.use(
    http.get('*/api/personas/laura-martinez-2/perfil-publico', () => {
      return HttpResponse.json(mockPerfilPublicoData);
    }),
    http.get('*/api/viajes/publicos/conductor/laura-martinez-2', () => {
      return HttpResponse.json([
        { id: 1, fechaHoraSalida: new Date().toISOString(), estado: 'FINALIZADO' }
      ]);
    })
  );

  renderComponentWithSlug();

  // Estado inicial de carga
  expect(screen.getByText(/cargando perfil público.../i)).toBeInTheDocument();

  // Validar renderizado de nombre y datos
  expect(await screen.findByText('Laura Martínez')).toBeInTheDocument();
  expect(screen.getByText(/laura@test.com/i)).toBeInTheDocument();
  expect(screen.getByText(/611223344/i)).toBeInTheDocument();

  // Validar preferencias de viaje públicas
  expect(screen.getByText('Mascotas permitidas')).toBeInTheDocument();
  expect(screen.getByText('Silencioso')).toBeInTheDocument();

  // Validar reputación
  expect(screen.getByText(/4.9 \/ 5/i)).toBeInTheDocument();
});

test('Muestra un mensaje de error si el perfil público no existe o falla la API', async () => {
  server.use(
    http.get('*/api/personas/slug-no-existente/perfil-publico', () => {
      return new HttpResponse(null, { status: 404 });
    }),
    http.get('*/api/viajes/publicos/conductor/slug-no-existente', () => {
      return HttpResponse.json([]);
    })
  );

  renderComponentWithSlug('slug-no-existente');

  await waitFor(() => {
    expect(screen.getByText(/no se pudo cargar el perfil público/i)).toBeInTheDocument();
  });
});