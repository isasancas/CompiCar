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
  fechaHoraSalida: '2025-01-01T10:00:00Z',
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

  expect(screen.getByText(/Cargando valoraciones.../i)).toBeInTheDocument();

  expect(await screen.findByText('Tu reputación y tus valoraciones')).toBeInTheDocument();
  expect(screen.getByText('4.8 / 5')).toBeInTheDocument();

  expect(screen.getByText('De Carlos Conductor')).toBeInTheDocument();
  expect(screen.getByText('Excelente pasajero, muy puntual.')).toBeInTheDocument();

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

  const btnAbrirForm = screen.getByRole('button', { name: /añadir nueva valoración/i });
  fireEvent.click(btnAbrirForm);

  const desplegables = screen.getAllByRole('combobox');
  const selectViaje = desplegables[0]; 
  fireEvent.change(selectViaje, { target: { value: '10' } });

  const inputComentario = screen.getByPlaceholderText(/cuenta tu experiencia con este viaje/i);
  fireEvent.change(inputComentario, { target: { value: 'Excelente viaje con María.' } });

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

  const btnEditar = screen.getByRole('button', { name: /editar/i });
  fireEvent.click(btnEditar);

  expect(screen.getByText('Editar valoración')).toBeInTheDocument();

  const inputComentario = screen.getByPlaceholderText(/modifica tu comentario/i);
  fireEvent.change(inputComentario, { target: { value: 'Viaje modificado y perfecto.', name: 'comentario' } });

  const btnGuardarCambios = screen.getByRole('button', { name: /guardar cambios/i });
  fireEvent.click(btnGuardarCambios);

  await waitFor(() => {
    expect(putCalled).toBe(true);
    expect(putBody.comentario).toBe('Viaje modificado y perfecto.');
  });
});

test('Permite eliminar una valoración emitida tras confirmar el modal de confirmación', async () => {
  let deleteCalled = false;
  
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

test('Muestra la pantalla de error si falla la API en la carga y permite volver al perfil', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => {
      return new HttpResponse(JSON.stringify({ message: 'Error en el servidor' }), { status: 500 });
    })
  );

  renderComponente();

  const mensajeError = await screen.findByText('Error en el servidor');
  expect(mensajeError).toBeInTheDocument();

  const btnVolver = screen.getByRole('button', { name: /Volver al perfil/i });
  fireEvent.click(btnVolver);
  expect(mockNavigate).toHaveBeenCalledWith('/perfil');
});

test('Cierra sesión si la API devuelve error de falta de autenticación en la carga', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => {
      return new HttpResponse(JSON.stringify({ message: 'No autenticado' }), { status: 401 });
    })
  );

  renderComponente();

  await waitFor(() => {
    expect(mockNavigate).toHaveBeenCalledWith('/inicio-sesion', { replace: true });
  });
});

test('Trata los tokens inválidos ("undefined", "null", espaciados) como sesión ausente', async () => {
  localStorage.setItem('token', 'undefined');

  renderComponente();

  await waitFor(() => {
    expect(mockNavigate).toHaveBeenCalledWith('/inicio-sesion', { replace: true });
  });
});

test('Muestra fallbacks de reputación y valoraciones sin nombres ni comentarios', async () => {
  const perfilSinReputacion = { id: 1, nombre: 'Juan', primerApellido: 'Pérez' };
  const valRecibidaSinAutor = { id: 101, puntuacion: 3, fecha: '2026-01-10T10:00:00Z' };
  const valEmitidaSinValorado = { id: 201, puntuacion: 4, fecha: '2026-01-12T12:00:00Z' };

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(perfilSinReputacion)),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([valRecibidaSinAutor])),
    http.get('*/api/valoraciones/autor/1', () => HttpResponse.json([valEmitidaSinValorado])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([]))
  );

  renderComponente();

  await screen.findByText('Tu reputación y tus valoraciones');

  expect(screen.getByText('0.0 / 5')).toBeInTheDocument();

  expect(screen.getByText('Valoración #101')).toBeInTheDocument();
  expect(screen.getByText('Valoración #201')).toBeInTheDocument();
});

test('Muestra los mensajes de estado vacío cuando no hay valoraciones recibidas ni emitidas', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfil)),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/autor/1', () => HttpResponse.json([])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([]))
  );

  renderComponente();

  await screen.findByText('Tu reputación y tus valoraciones');

  expect(screen.getByText('Todavía no tienes valoraciones recibidas.')).toBeInTheDocument();
  expect(screen.getByText('Todavía no has enviado ninguna valoración.')).toBeInTheDocument();
});

test('Muestra errores de validación en el formulario de creación (sin viaje o sin conductor)', async () => {
  const viajeSinConductor = {
    id: 99,
    slug: 'viaje-sin-conductor',
    fechaHoraSalida: '2025-01-01T10:00:00Z',
    estado: 'FINALIZADO',
  };

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfil)),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/autor/1', () => HttpResponse.json([])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([viajeSinConductor]))
  );

  renderComponente();
  await screen.findByText('Tu reputación y tus valoraciones');

  fireEvent.click(screen.getByRole('button', { name: /añadir nueva valoración/i }));

  // 1. Intentar enviar sin seleccionar viaje
  fireEvent.click(screen.getByRole('button', { name: /crear valoración/i }));
  expect(await screen.findByText('Selecciona un viaje.')).toBeInTheDocument();

  // 2. Seleccionar viaje sin conductorId
  const selectViaje = screen.getAllByRole('combobox')[0];
  fireEvent.change(selectViaje, { target: { value: '99' } });
  fireEvent.click(screen.getByRole('button', { name: /crear valoración/i }));

  expect(await screen.findByText('No se pudo determinar el conductor de este viaje.')).toBeInTheDocument();
});

test('Muestra errores devueltos por la API o fallos de red al crear una valoración', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfil)),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/autor/1', () => HttpResponse.json([])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([mockViajeParticipado])),
    http.post('*/api/valoraciones', () => {
      return new HttpResponse(JSON.stringify({ message: 'Ya has valorado este viaje' }), { status: 400 });
    })
  );

  renderComponente();
  await screen.findByText('Tu reputación y tus valoraciones');

  fireEvent.click(screen.getByRole('button', { name: /añadir nueva valoración/i }));
  const selectViaje = screen.getAllByRole('combobox')[0];
  fireEvent.change(selectViaje, { target: { value: '10' } });

  fireEvent.click(screen.getByRole('button', { name: /crear valoración/i }));

  expect(await screen.findByText('Ya has valorado este viaje')).toBeInTheDocument();
});

test('Muestra errores devueltos por la API o fallos de red al editar una valoración', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfil)),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/autor/1', () => HttpResponse.json([mockValoracionEmitida])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([])),
    http.put('*/api/valoraciones/201', () => {
      return HttpResponse.error();
    })
  );

  renderComponente();
  await screen.findByText('A María Conductora');

  fireEvent.click(screen.getByRole('button', { name: /editar/i }));
  fireEvent.click(screen.getByRole('button', { name: /guardar cambios/i }));

  expect(await screen.findByText('Error de conexión al actualizar la valoración')).toBeInTheDocument();
});

test('Permite abrir, alternar y cerrar los formularios mediante los botones Cancelar y Toggle', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfil)),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/autor/1', () => HttpResponse.json([mockValoracionEmitida])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([mockViajeParticipado]))
  );

  renderComponente();
  await screen.findByText('Tu reputación y tus valoraciones');

  const btnToggle = screen.getByRole('button', { name: /añadir nueva valoración/i });
  
  fireEvent.click(btnToggle);
  expect(screen.getByText('Nueva valoración')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /cerrar formulario/i })).toBeInTheDocument();

  const btnsCancelar = screen.getAllByRole('button', { name: /cancelar/i });
  fireEvent.click(btnsCancelar[0]);
  expect(screen.queryByText('Nueva valoración')).not.toBeInTheDocument();

  fireEvent.click(screen.getByRole('button', { name: /editar/i }));
  expect(screen.getByText('Editar valoración')).toBeInTheDocument();
  
  const btnCancelarEdicion = screen.getByRole('button', { name: /cancelar/i });
  fireEvent.click(btnCancelarEdicion);
  expect(screen.queryByText('Editar valoración')).not.toBeInTheDocument();
});

test('Gestiona la cancelación del usuario y los errores al eliminar una valoración', async () => {
  // 1. El usuario cancela el confirm dialog
  window.confirm = vi.fn().mockReturnValue(false);

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfil)),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/autor/1', () => HttpResponse.json([mockValoracionEmitida])),
    http.get('*/api/viajes/participados', () => HttpResponse.json([])),
    http.delete('*/api/valoraciones/201', () => {
      return new HttpResponse(null, { status: 500 });
    })
  );

  renderComponente();
  await screen.findByText('A María Conductora');

  const btnEliminar = screen.getByRole('button', { name: /eliminar/i });
  fireEvent.click(btnEliminar);

  expect(window.confirm).toHaveBeenCalled();

  // 2. El usuario confirma pero la API da error 500
  window.confirm = vi.fn().mockReturnValue(true);
  fireEvent.click(btnEliminar);

  expect(await screen.findByText('No se pudo eliminar la valoración')).toBeInTheDocument();
});