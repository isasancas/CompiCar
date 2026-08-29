import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../../setupTests';
import Valoraciones from '../autenticacion/Valoraciones';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// Datos de prueba reutilizables
const mockPerfil = {
  id: 1,
  nombre: 'Juan',
  primerApellido: 'Pérez',
  reputacion: 4.8,
};

const mockValoracionRecibida = {
  id: 101,
  puntuacion: 5,
  comentario: 'Excelente pasajero, muy puntual.',
  fecha: '2026-01-10T10:00:00Z',
  autorId: 2,
  autorNombre: 'Carlos Conductor',
};

const mockValoracionEmitida = {
  id: 201,
  puntuacion: 4,
  comentario: 'Buen viaje en general.',
  fecha: '2026-01-12T12:00:00Z',
  autorId: 1,
  valoradoId: 3,
  valoradoNombre: 'María Conductora',
  viajeId: 10,
};

const mockViajeParticipado = {
  id: 10,
  slug: 'madrid-barcelona-10',
  fechaHoraSalida: '2025-01-01T10:00:00Z', // Fecha en el pasado para ser elegible
  estado: 'FINALIZADO',
  conductorId: 3,
  conductorNombre: 'María Conductora',
  vehiculo: {
    marca: 'Seat',
    modelo: 'Ibiza',
    matricula: '1234ABC',
  },
};

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.setItem('token', 'fake-jwt-token');
});

const renderComponente = () => {
  return render(
    <MemoryRouter initialEntries={['/valoraciones']}>
      <Routes>
        <Route path="/valoraciones" element={<Valoraciones />} />
      </Routes>
    </MemoryRouter>
  );
};

test('Carga y muestra el perfil, la reputación y las valoraciones del usuario', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfil)),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([mockValoracionRecibida])),
    http.get('*/api/valoraciones/autor/1', () => HttpResponse.json([mockValoracionEmitida])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([mockViajeParticipado]))
  );

  renderComponente();

  // Muestra estado de carga inicial
  expect(screen.getByText(/Cargando valoraciones.../i)).toBeInTheDocument();

  // Espera a que carguen los datos principales
  expect(await screen.findByText('Tu reputación y tus valoraciones')).toBeInTheDocument();
  expect(screen.getByText('4.8 / 5')).toBeInTheDocument();

  // Verifica valoración recibida
  expect(screen.getByText('De Carlos Conductor')).toBeInTheDocument();
  expect(screen.getByText('Excelente pasajero, muy puntual.')).toBeInTheDocument();

  // Verifica valoración emitida
  expect(screen.getByText('A María Conductora')).toBeInTheDocument();
  expect(screen.getByText('Buen viaje en general.')).toBeInTheDocument();
});

test('Redirige a /inicio-sesion si el usuario no tiene token de autenticación', async () => {
  localStorage.removeItem('token');

  renderComponente();

  await waitFor(() => {
    expect(mockNavigate).toHaveBeenCalledWith('/inicio-sesion', { replace: true });
  });
});

test('Permite crear una nueva valoración correctamente', async () => {
  let postBody: any = null;

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfil)),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/autor/1', () => HttpResponse.json([])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([mockViajeParticipado])),
    http.post('*/api/valoraciones', async ({ request }) => {
      postBody = await request.json();
      return HttpResponse.json({ id: 301, ...postBody });
    })
  );

  renderComponente();

  await screen.findByText('Tu reputación y tus valoraciones');

  // Abrir formulario
  const btnAbrirForm = screen.getByRole('button', { name: /añadir nueva valoración/i });
  fireEvent.click(btnAbrirForm);

  // Obtener todos los desplegables y seleccionar el de viaje (índice 0 o 1 según el orden en tu JSX)
  const desplegables = screen.getAllByRole('combobox');
  const selectViaje = desplegables[0]; 
  fireEvent.change(selectViaje, { target: { value: '10' } });

  // Escribir comentario
  const inputComentario = screen.getByPlaceholderText(/cuenta tu experiencia con este viaje/i);
  fireEvent.change(inputComentario, { target: { value: 'Excelente viaje con María.' } });

  // Enviar formulario
  const btnGuardar = screen.getByRole('button', { name: /crear valoración/i });
  fireEvent.click(btnGuardar);

  await waitFor(() => {
    expect(postBody).toEqual({
      puntuacion: 5,
      comentario: 'Excelente viaje con María.',
      autorId: 1,
      valoradoId: 3,
      viajeId: 10,
    });
  });
});

test('Permite editar una valoración emitida previamente', async () => {
  let putCalled = false;
  let putBody: any = null;

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfil)),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/autor/1', () => HttpResponse.json([mockValoracionEmitida])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([mockViajeParticipado])),
    http.put('*/api/valoraciones/201', async ({ request }) => {
      putCalled = true;
      putBody = await request.json();
      return HttpResponse.json({ ...mockValoracionEmitida, ...putBody });
    })
  );

  renderComponente();

  await screen.findByText('A María Conductora');

  // Abrir formulario de edición
  const btnEditar = screen.getByRole('button', { name: /editar/i });
  fireEvent.click(btnEditar);

  expect(screen.getByText('Editar valoración')).toBeInTheDocument();

  // Modificar comentario
  const inputComentario = screen.getByPlaceholderText(/modifica tu comentario/i);
  fireEvent.change(inputComentario, { target: { value: 'Viaje modificado y perfecto.', name: 'comentario' } });

  // Guardar cambios
  const btnGuardarCambios = screen.getByRole('button', { name: /guardar cambios/i });
  fireEvent.click(btnGuardarCambios);

  await waitFor(() => {
    expect(putCalled).toBe(true);
    expect(putBody.comentario).toBe('Viaje modificado y perfecto.');
  });
});

test('Permite eliminar una valoración emitida tras confirmar el modal de confirmación', async () => {
  let deleteCalled = false;
  
  // Asignación directa de mock para evitar error cuando window.confirm es undefined
  window.confirm = vi.fn().mockReturnValue(true);

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfil)),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/autor/1', () => HttpResponse.json([mockValoracionEmitida])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([])),
    http.delete('*/api/valoraciones/201', () => {
      deleteCalled = true;
      return new HttpResponse(null, { status: 200 });
    })
  );

  renderComponente();

  await screen.findByText('A María Conductora');

  const btnEliminar = screen.getByRole('button', { name: /eliminar/i });
  fireEvent.click(btnEliminar);

  await waitFor(() => {
    expect(window.confirm).toHaveBeenCalledWith('¿Seguro que quieres eliminar esta valoración?');
    expect(deleteCalled).toBe(true);
  });
});

test('Navega de vuelta a la pantalla de perfil al pulsar "Volver al perfil"', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfil)),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/autor/1', () => HttpResponse.json([])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([]))
  );

  renderComponente();

  const btnVolver = await screen.findByRole('button', { name: /volver al perfil/i });
  fireEvent.click(btnVolver);

  expect(mockNavigate).toHaveBeenCalledWith('/perfil');
});