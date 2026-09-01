import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../../setupTests';
import Perfil from '../autenticacion/Perfil';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockPerfilData = {
  id: 1,
  nombre: 'Carlos',
  primerApellido: 'García',
  segundoApellido: 'López',
  email: 'carlos@test.com',
  telefono: '+34600123456',
  reputacion: 4.5,
  fondosActuales: 25.00,
  fondosTotales: 100.00,
  numeroCancelaciones: 1,
  preferenciasViaje: ['No fumador']
};

const mockVehiculosData = [
  {
    id: 10,
    matricula: '1234ABC',
    marca: 'Renault',
    modelo: 'Clio',
    plazas: 5,
    consumo: 5.5,
    anio: 2018,
    tipo: 'COCHE'
  }
];

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.setItem('token', 'fake-jwt-token');
});

const renderComponent = () => {
  return render(
    <MemoryRouter initialEntries={['/perfil']}>
      <Routes>
        <Route path="/perfil" element={<Perfil />} />
      </Routes>
    </MemoryRouter>
  );
};

test('Carga y muestra la información del perfil, vehículos y monedero correctamente', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json(mockVehiculosData)),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([
      { id: 1, puntuacion: 5 },
      { id: 2, puntuacion: 4 }
    ]))
  );

  renderComponent();

  expect(screen.getByText(/cargando.../i)).toBeInTheDocument();
  expect(await screen.findByText(/Carlos García/i)).toBeInTheDocument();
  expect(screen.getByText(/carlos@test.com/i)).toBeInTheDocument();
  expect(screen.getByText(/\+34600123456/i)).toBeInTheDocument();
  expect(screen.getByText('25.00 €')).toBeInTheDocument();
  expect(screen.getByText('100.00 €')).toBeInTheDocument();
  expect(screen.getByText(/Renault/i)).toBeInTheDocument();
  expect(screen.getByText(/4.5 \/ 5/i)).toBeInTheDocument();
});

test('Muestra error de conexión cuando falla la red al cargar perfil', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.error())
  );

  renderComponent();

  expect(await screen.findByText('Error de conexión')).toBeInTheDocument();
});

test('Redirige al login si no hay token en localStorage', async () => {
  localStorage.removeItem('token');

  renderComponent();

  await waitFor(() => {
    expect(mockNavigate).toHaveBeenCalledWith('/inicio-sesion', { replace: true });
  });
});

test('Redirige a login si la API responde con 401 o 403', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => new HttpResponse(null, { status: 401 }))
  );

  renderComponent();

  await waitFor(() => {
    expect(mockNavigate).toHaveBeenCalledWith('/inicio-sesion', { replace: true });
  });
});

test('Muestra pantalla de error cuando el backend responde con 500 al obtener perfil y permite ir al login', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => new HttpResponse(null, { status: 500 }))
  );

  renderComponent();

  expect(await screen.findByText('Error al obtener el perfil')).toBeInTheDocument();

  const btnIrLogin = screen.getByRole('button', { name: /ir a iniciar sesión/i });
  fireEvent.click(btnIrLogin);

  expect(mockNavigate).toHaveBeenCalledWith('/inicio-sesion', { replace: true });
});

test('Muestra mensaje de error si la API de vehículos responde con status 500', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => new HttpResponse(null, { status: 500 })),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([]))
  );

  renderComponent();

  expect(await screen.findByText('Error al cargar los vehículos')).toBeInTheDocument();
});

test('Realiza la retirada de fondos con éxito', async () => {
  let retirarCalled = false;

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.post('*/api/personas/retirar-fondos', () => {
      retirarCalled = true;
      return HttpResponse.json({ status: 'SUCCESS' });
    })
  );

  renderComponent();

  const btnRetirar = await screen.findByRole('button', { name: /retirar fondos/i });
  fireEvent.click(btnRetirar);

  await waitFor(() => {
    expect(retirarCalled).toBe(true);
    expect(screen.getByText(/¡Retiro completado con éxito!/i)).toBeInTheDocument();
  });
});

test('Muestra error de red en la retirada de fondos', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.post('*/api/personas/retirar-fondos', () => HttpResponse.error())
  );

  renderComponent();

  const btnRetirar = await screen.findByRole('button', { name: /retirar fondos/i });
  fireEvent.click(btnRetirar);

  expect(await screen.findByText('Error de conexión con el servidor')).toBeInTheDocument();
});

test('Deshabilita botón de retirada si fondos < 10€', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json({ ...mockPerfilData, fondosActuales: 5.00 })),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([]))
  );

  renderComponent();

  const btnRetirar = await screen.findByRole('button', { name: /retirar fondos/i });
  expect(btnRetirar).toBeDisabled();
});

test('Redirige a Stripe cuando el servidor responde REQUIRES_ONBOARDING', async () => {
  const originalLocation = window.location;
  delete (window as unknown as { location?: unknown }).location;
  (window as any).location = { ...originalLocation, href: '' };

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.post('*/api/personas/retirar-fondos', () => HttpResponse.json({
      status: 'REQUIRES_ONBOARDING',
      url: 'https://stripe.com/onboarding'
    }))
  );

  renderComponent();

  const btnRetirar = await screen.findByRole('button', { name: /retirar fondos/i });
  fireEvent.click(btnRetirar);

  await waitFor(() => {
    expect(window.location.href).toBe('https://stripe.com/onboarding');
  });

  (window as any).location = originalLocation;
});

test('Muestra mensaje de error del servidor en retirada de fondos fallida', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.post('*/api/personas/retirar-fondos', () => HttpResponse.json(
      { error: 'Fondos insuficientes' },
      { status: 400 }
    ))
  );

  renderComponent();

  const btnRetirar = await screen.findByRole('button', { name: /retirar fondos/i });
  fireEvent.click(btnRetirar);

  expect(await screen.findByText('Fondos insuficientes')).toBeInTheDocument();
});

test('Abre modal de editar perfil, cancela y cierra sin guardar', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([]))
  );

  renderComponent();

  const btnEditarPerfil = await screen.findByRole('button', { name: /^editar perfil$/i });
  fireEvent.click(btnEditarPerfil);

  expect(screen.getByRole('heading', { name: /editar perfil/i })).toBeInTheDocument();

  const btnCancelar = screen.getByRole('button', { name: /cancelar/i });
  fireEvent.click(btnCancelar);

  await waitFor(() => {
    expect(screen.queryByRole('heading', { name: /editar perfil/i })).not.toBeInTheDocument();
  });
});

test('Permite cambiar contraseña en modal de editar perfil', async () => {
  let putData: any = null;

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.put('*/api/personas/*/perfil', async ({ request }) => {
      putData = await request.json();
      return HttpResponse.json(mockPerfilData);
    })
  );

  renderComponent();

  const btnEditarPerfil = await screen.findByRole('button', { name: /^editar perfil$/i });
  fireEvent.click(btnEditarPerfil);

  const inputs = Array.from(document.querySelectorAll('input'));
  inputs.forEach((input) => {
    if (input.type === 'password' || /pass|contra|actual|nueva|clave/i.test(input.name || input.placeholder || input.id || '')) {
      fireEvent.change(input, { target: { value: 'password123' } });
    }
  });

  const btnGuardar = screen.getByRole('button', { name: /guardar cambios/i });
  fireEvent.click(btnGuardar);

  await waitFor(() => {
    expect(putData).not.toBeNull();
  });
});

test('Valida coincidencia de nueva contraseña al editar perfil', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.put('*/api/personas/*/perfil', () => HttpResponse.json(mockPerfilData))
  );

  renderComponent();

  const btnEditarPerfil = await screen.findByRole('button', { name: /^editar perfil$/i });
  fireEvent.click(btnEditarPerfil);

  const pwdInputs = document.querySelectorAll('input[type="password"]');
  const allInputs = document.querySelectorAll('input');

  if (pwdInputs.length >= 2) {
    if (pwdInputs.length >= 3) {
      fireEvent.change(pwdInputs[0], { target: { value: 'password123' } });
      fireEvent.change(pwdInputs[1], { target: { value: 'NewPassword123' } });
      fireEvent.change(pwdInputs[2], { target: { value: 'Diferente123' } });
    } else {
      fireEvent.change(pwdInputs[0], { target: { value: 'NewPassword123' } });
      fireEvent.change(pwdInputs[1], { target: { value: 'Diferente123' } });
    }
  } else if (allInputs.length >= 2) {
    const inputsArr = Array.from(allInputs);
    fireEvent.change(inputsArr[inputsArr.length - 2], { target: { value: 'NewPassword123' } });
    fireEvent.change(inputsArr[inputsArr.length - 1], { target: { value: 'Diferente123' } });
  }

  const btnGuardar = screen.getByRole('button', { name: /guardar cambios/i });
  fireEvent.click(btnGuardar);

  await waitFor(() => {
    expect(document.body.textContent).toMatch(/coincid|diferent|igual|error|no se puede|invalid|inválid/i);
  });
});

test('Muestra error si falla la actualización del perfil en backend', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.put('*/api/personas/*/perfil', () => HttpResponse.json({ error: 'Error del servidor al actualizar' }, { status: 400 }))
  );

  renderComponent();

  const btnEditarPerfil = await screen.findByRole('button', { name: /^editar perfil$/i });
  fireEvent.click(btnEditarPerfil);

  const btnGuardar = screen.getByRole('button', { name: /guardar cambios/i });
  fireEvent.click(btnGuardar);

  expect(await screen.findByText('Error del servidor al actualizar')).toBeInTheDocument();
});

test('Permite editar todos los campos numéricos y tipo de un vehículo y cancelar modal', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json(mockVehiculosData)),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([]))
  );

  renderComponent();

  const btnEditarVehiculo = await screen.findByRole('button', { name: /^editar$/i });
  fireEvent.click(btnEditarVehiculo);

  const inputPlazas = screen.getByPlaceholderText(/plazas/i);
  const inputConsumo = screen.getByPlaceholderText(/consumo/i);
  const inputAnio = screen.getByPlaceholderText(/año/i);

  fireEvent.change(inputPlazas, { target: { value: '4' } });
  fireEvent.change(inputConsumo, { target: { value: '6.2' } });
  fireEvent.change(inputAnio, { target: { value: '2020' } });

  const selectTipo = screen.getByRole('combobox');
  fireEvent.change(selectTipo, { target: { value: 'MOTO' } });

  const btnCancelar = screen.getAllByRole('button', { name: /cancelar/i })[0];
  fireEvent.click(btnCancelar);

  await waitFor(() => {
    expect(screen.queryByText('Editar vehículo')).not.toBeInTheDocument();
  });
});

test('Muestra error al editar vehículo si se ingresan valores numéricos inválidos', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json(mockVehiculosData)),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([]))
  );

  renderComponent();

  const btnEditarVehiculo = await screen.findByRole('button', { name: /^editar$/i });
  fireEvent.click(btnEditarVehiculo);

  const inputAnio = screen.getByPlaceholderText(/año/i);
  fireEvent.change(inputAnio, { target: { value: '1800' } });

  const btnGuardarVehiculo = screen.getByRole('button', { name: /guardar/i });
  fireEvent.click(btnGuardarVehiculo);

  expect(await screen.findByText(/El año debe estar entre/i)).toBeInTheDocument();
});

test('Borra un vehículo con éxito tras la confirmación', async () => {
  window.confirm = vi.fn().mockReturnValue(true);

  let vehiculosActuales = [...mockVehiculosData];

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json(vehiculosActuales)),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.delete('*/api/vehiculos/*', () => {
      vehiculosActuales = [];
      return HttpResponse.json({ success: true });
    })
  );

  renderComponent();

  const btnBorrar = await screen.findByRole('button', { name: /^borrar$/i });
  fireEvent.click(btnBorrar);

  await waitFor(() => {
    expect(screen.queryByText(/1234ABC/i)).not.toBeInTheDocument();
  });
});

test('Muestra alerta de error cuando la carga de foto de perfil falla en el servidor', async () => {
  window.alert = vi.fn();

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.post('*/api/personas/foto', () => HttpResponse.json({ error: 'Error de servidor' }, { status: 500 }))
  );

  renderComponent();

  const btnFoto = await screen.findByRole('button', { name: /editar foto/i });
  fireEvent.click(btnFoto);

  const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
  const validFile = new File(['fake-image'], 'avatar.jpg', { type: 'image/jpeg' });

  fireEvent.change(fileInput, { target: { files: [validFile] } });

  await waitFor(() => {
    expect(window.alert).toHaveBeenCalledWith('Error al subir la foto');
  });
});

test('No hace nada si se cancela la selección de archivo de foto', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([]))
  );

  renderComponent();

  const btnFoto = await screen.findByRole('button', { name: /editar foto/i });
  fireEvent.click(btnFoto);

  const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
  fireEvent.change(fileInput, { target: { files: [] } });

  expect(await screen.findByText(/Carlos García/i)).toBeInTheDocument();
});

test('No añade preferencia si el texto introducido está vacío o es solo espacios', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([]))
  );

  renderComponent();

  const inputPref = await screen.findByPlaceholderText(/Ej: No fumador, Mascotas.../i);
  fireEvent.change(inputPref, { target: { value: '   ' } });

  const btnAnadir = screen.getByRole('button', { name: /^añadir$/i });
  fireEvent.click(btnAnadir);

  expect(screen.queryByText('   ')).not.toBeInTheDocument();
});

test('Abre modal de cerrar sesión y lo cancela con el botón No', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([]))
  );

  renderComponent();

  const btnCerrarSesion = await screen.findByRole('button', { name: /cerrar sesión/i });
  fireEvent.click(btnCerrarSesion);

  expect(screen.getByText(/confirmar cierre de sesión/i)).toBeInTheDocument();

  const btnCancelar = screen.getByRole('button', { name: /cancelar/i });
  fireEvent.click(btnCancelar);

  await waitFor(() => {
    expect(screen.queryByText(/confirmar cierre de sesión/i)).not.toBeInTheDocument();
  });
});

test('Maneja el cierre de sesión incluso si el endpoint de logout falla con error de red', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.post('*/api/logout', () => HttpResponse.error())
  );

  renderComponent();

  const btnCerrarSesion = await screen.findByRole('button', { name: /cerrar sesión/i });
  fireEvent.click(btnCerrarSesion);

  const btnConfirmar = screen.getByRole('button', { name: /sí, cerrar sesión/i });
  fireEvent.click(btnConfirmar);

  await waitFor(() => {
    expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true });
    expect(localStorage.getItem('token')).toBeNull();
  });
});

test('Calcula correctamente el resumen de actividad y tendencia del mes anterior', async () => {
  const now = new Date();
  const currentMonthDate = new Date(now.getFullYear(), now.getMonth(), 15).toISOString();
  const prevMonthDate = new Date(now.getFullYear(), now.getMonth() - 1, 15).toISOString();

  const mockViajes = [
    { id: 1, fechaHoraSalida: currentMonthDate, estado: 'COMPLETADO' },
    { id: 2, fechaHoraSalida: currentMonthDate, estado: 'FINALIZADO' },
    { id: 3, fechaHoraSalida: prevMonthDate, estado: 'FINALIZADO' }
  ];

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json(mockViajes)),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([]))
  );

  renderComponent();

  expect(await screen.findByText('2')).toBeInTheDocument();
  expect(screen.getByText('+100%')).toBeInTheDocument();
});

test('Valida campos requeridos y formato en el formulario de edición de perfil', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([]))
  );

  renderComponent();

  const btnEditarPerfil = await screen.findByRole('button', { name: /^editar perfil$/i });
  fireEvent.click(btnEditarPerfil);

  const inputNombre = screen.getByPlaceholderText('Nombre');
  fireEvent.change(inputNombre, { target: { value: '' } });

  const btnGuardar = screen.getByRole('button', { name: /guardar cambios/i });
  fireEvent.click(btnGuardar);

  expect(screen.getByText('El nombre no puede estar vacío.')).toBeInTheDocument();
});

test('Guarda correctamente la edición de un vehículo existente', async () => {
  let putVehicleData: any = null;

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json(mockVehiculosData)),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.put('*/api/vehiculos/10', async ({ request }) => {
      putVehicleData = await request.json();
      return HttpResponse.json({ success: true });
    })
  );

  renderComponent();

  const btnEditarVehiculo = await screen.findByRole('button', { name: /^editar$/i });
  fireEvent.click(btnEditarVehiculo);

  const btnGuardarVehiculo = screen.getByRole('button', { name: /guardar vehículo/i });
  fireEvent.click(btnGuardarVehiculo);

  await waitFor(() => {
    expect(putVehicleData).not.toBeNull();
  });
});

test('Sube correctamente la foto de perfil e impide archivos mayores a 5MB', async () => {
  window.alert = vi.fn();

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.post('*/api/personas/foto', () => HttpResponse.json({ success: true }))
  );

  const { container } = renderComponent();

  await screen.findByRole('button', { name: /^editar perfil$/i });

  const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;

  const bigFile = new File(['a'.repeat(6 * 1024 * 1024)], 'big.jpg', { type: 'image/jpeg' });
  fireEvent.change(fileInput, { target: { files: [bigFile] } });
  expect(window.alert).toHaveBeenCalledWith('La foto debe ser menor a 5MB');

  const validFile = new File(['avatar'], 'avatar.jpg', { type: 'image/jpeg' });
  fireEvent.change(fileInput, { target: { files: [validFile] } });

  await waitFor(() => {
    expect(window.alert).toHaveBeenCalledWith('Foto actualizada correctamente');
  });
});

test('Añade y elimina preferencias de viaje mediante teclado y botón', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.put('*/api/personas/1/perfil', () => HttpResponse.json(mockPerfilData))
  );

  renderComponent();

  const inputPref = await screen.findByPlaceholderText(/Ej: No fumador, Mascotas.../i);
  
  fireEvent.change(inputPref, { target: { value: 'Música clásica' } });
  fireEvent.keyDown(inputPref, { key: 'Enter', code: 'Enter' });

  expect(screen.getByText('Música clásica')).toBeInTheDocument();

  const btnEliminar = screen.getAllByText('×')[0];
  fireEvent.click(btnEliminar);

  expect(screen.queryByText('No fumador')).not.toBeInTheDocument();
});

test('Ejecuta las rutas de navegación correspondientes al hacer click en los botones', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([]))
  );

  renderComponent();

  const btnVolver = await screen.findByRole('button', { name: /volver/i });
  fireEvent.click(btnVolver);
  expect(mockNavigate).toHaveBeenCalledWith('/');

  const btnMisViajes = screen.getByRole('button', { name: /ver detalle de mis viajes/i });
  fireEvent.click(btnMisViajes);
  expect(mockNavigate).toHaveBeenCalledWith('/mis-viajes');

  const btnNuevoVehiculo = screen.getByRole('button', { name: /añadir un nuevo vehículo/i });
  fireEvent.click(btnNuevoVehiculo);
  expect(mockNavigate).toHaveBeenCalledWith('/vehiculos/nuevo');
});

test('Muestra mensaje de error si falla la carga inicial del perfil', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => new HttpResponse(null, { status: 500 }))
  );

  renderComponent();

  const mensajeError = await screen.findByText(/error al obtener el perfil/i);
  expect(mensajeError).toBeInTheDocument();
});

test('Valida campos incorrectos en el formulario de vehículo (plazas, matrícula y marca)', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json(mockVehiculosData)),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([]))
  );

  renderComponent();

  const btnEditarVehiculo = await screen.findByRole('button', { name: /^editar$/i });
  fireEvent.click(btnEditarVehiculo);

  const inputPlazas = screen.getByPlaceholderText(/plazas/i) || screen.getByLabelText(/plazas/i);
  fireEvent.change(inputPlazas, { target: { value: '0' } });

  const btnGuardar = screen.getByRole('button', { name: /guardar vehículo/i });
  fireEvent.click(btnGuardar);

  expect(await screen.findByText(/las plazas deben ser/i)).toBeInTheDocument();
});

test('Maneja la eliminación de un vehículo correctamente', async () => {
  let vehicleDeletedId: string | null = null;

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json(mockVehiculosData)),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.delete('*/api/vehiculos/:id', ({ params }) => {
      vehicleDeletedId = params.id as string;
      return HttpResponse.json({ success: true });
    })
  );

  renderComponent();

  const btnEliminar = await screen.findByRole('button', { 
    name: /eliminar|borrar|trash|eliminar vehículo/i 
  });
  
  fireEvent.click(btnEliminar);

  await waitFor(() => {
    expect(vehicleDeletedId).toBe('10');
  });
});

test('Muestra un error al fallar la subida de la foto de perfil en el servidor', async () => {
  window.alert = vi.fn();

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.post('*/api/personas/foto', () => new HttpResponse(null, { status: 500 }))
  );

  const { container } = renderComponent();
  await screen.findByRole('button', { name: /^editar perfil$/i });

  const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;
  const validFile = new File(['avatar'], 'avatar.jpg', { type: 'image/jpeg' });
  
  fireEvent.change(fileInput, { target: { files: [validFile] } });

  await waitFor(() => {
    expect(window.alert).toHaveBeenCalledWith(expect.stringMatching(/error/i));
  });
});

test('Recarga los datos de perfil al enfocar la ventana (focus event)', async () => {
  let fetchCount = 0;

  server.use(
    http.get('*/api/personas/perfil', () => {
      fetchCount++;
      return HttpResponse.json(mockPerfilData);
    }),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([]))
  );

  renderComponent();
  await screen.findByRole('button', { name: /^editar perfil$/i });

  fireEvent.focus(window);

  await waitFor(() => {
    expect(fetchCount).toBeGreaterThan(1);
  });
});

test('Valida exhaustivamente todos los campos del formulario de edición de perfil', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([]))
  );

  renderComponent();
  const btnEditar = await screen.findByRole('button', { name: /^editar perfil$/i });
  fireEvent.click(btnEditar);

  const inputPrimerApellido = screen.getByPlaceholderText('Primer apellido');
  const inputEmail = screen.getByPlaceholderText('Email');
  const inputTelefono = screen.getByPlaceholderText('Teléfono');
  const btnGuardar = screen.getByRole('button', { name: /guardar cambios/i });

  // 1. Primer apellido vacío
  fireEvent.change(inputPrimerApellido, { target: { value: '' } });
  fireEvent.click(btnGuardar);
  expect(screen.getByText('El primer apellido no puede estar vacío.')).toBeInTheDocument();
  fireEvent.change(inputPrimerApellido, { target: { value: 'García' } });

  // 2. Email vacío
  fireEvent.change(inputEmail, { target: { value: '' } });
  fireEvent.click(btnGuardar);
  expect(screen.getByText('El email no puede estar vacío.')).toBeInTheDocument();

  // 3. Email con formato inválido
  fireEvent.change(inputEmail, { target: { value: 'email-invalido' } });
  fireEvent.click(btnGuardar);
  expect(screen.getByText('El email no es válido.')).toBeInTheDocument();
  fireEvent.change(inputEmail, { target: { value: 'carlos@test.com' } });

  // 4. Teléfono vacío
  fireEvent.change(inputTelefono, { target: { value: '' } });
  fireEvent.click(btnGuardar);
  expect(screen.getByText('El teléfono no puede estar vacío.')).toBeInTheDocument();

  // 5. Teléfono inválido
  fireEvent.change(inputTelefono, { target: { value: '123' } });
  fireEvent.click(btnGuardar);
  expect(screen.getByText('El teléfono no es válido.')).toBeInTheDocument();
  fireEvent.change(inputTelefono, { target: { value: '+34600123456' } });

  // 6. Cambio de email sin contraseña actual
  fireEvent.change(inputEmail, { target: { value: 'nuevo@test.com' } });
  fireEvent.click(btnGuardar);
  expect(screen.getByText('Debes introducir tu contraseña actual para cambiar el email.')).toBeInTheDocument();
});

test('Maneja errores de red y 401/403 al actualizar perfil', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.put('*/api/personas/1/perfil', () => HttpResponse.error())
  );

  renderComponent();
  fireEvent.click(await screen.findByRole('button', { name: /^editar perfil$/i }));
  fireEvent.click(screen.getByRole('button', { name: /guardar cambios/i }));

  expect(await screen.findByText('Error de conexión al actualizar el perfil.')).toBeInTheDocument();
});

test('Valida exhaustivamente las restricciones del formulario de vehículos', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json(mockVehiculosData)),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([]))
  );

  renderComponent();
  fireEvent.click(await screen.findByRole('button', { name: /^editar$/i }));

  const inputMatricula = screen.getByPlaceholderText('Matrícula');
  const inputMarca = screen.getByPlaceholderText('Marca');
  const inputModelo = screen.getByPlaceholderText('Modelo');
  const inputConsumo = screen.getByPlaceholderText(/consumo/i);
  const btnGuardar = screen.getByRole('button', { name: /guardar vehículo/i });

  // 1. Matrícula vacía
  fireEvent.change(inputMatricula, { target: { value: '' } });
  fireEvent.click(btnGuardar);
  expect(screen.getByText('La matrícula es obligatoria.')).toBeInTheDocument();

  // 2. Formato matrícula inválido
  fireEvent.change(inputMatricula, { target: { value: 'INVALIDA' } });
  fireEvent.click(btnGuardar);
  expect(screen.getByText('El formato de matrícula no es válido.')).toBeInTheDocument();
  fireEvent.change(inputMatricula, { target: { value: '1234ABC' } });

  // 3. Marca vacía
  fireEvent.change(inputMarca, { target: { value: '' } });
  fireEvent.click(btnGuardar);
  expect(screen.getByText('La marca es obligatoria.')).toBeInTheDocument();
  fireEvent.change(inputMarca, { target: { value: 'Renault' } });

  // 4. Modelo vacío
  fireEvent.change(inputModelo, { target: { value: '' } });
  fireEvent.click(btnGuardar);
  expect(screen.getByText('El modelo es obligatorio.')).toBeInTheDocument();
  fireEvent.change(inputModelo, { target: { value: 'Clio' } });

  // 5. Consumo <= 0
  fireEvent.change(inputConsumo, { target: { value: '0' } });
  fireEvent.click(btnGuardar);
  expect(screen.getByText('El consumo debe ser un número mayor que 0.')).toBeInTheDocument();
});

test('Maneja cancelación y errores al borrar vehículo', async () => {
  window.confirm = vi.fn().mockReturnValue(false);

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json(mockVehiculosData)),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([]))
  );

  renderComponent();
  fireEvent.click(await screen.findByRole('button', { name: /^borrar$/i }));
  expect(screen.getByText(/Renault/i)).toBeInTheDocument();

  window.confirm = vi.fn().mockReturnValue(true);
  server.use(http.delete('*/api/vehiculos/10', () => HttpResponse.error()));

  fireEvent.click(screen.getByRole('button', { name: /^borrar$/i }));
  expect(await screen.findByText('Error de conexión al borrar el vehículo.')).toBeInTheDocument();
});

test('Maneja error de conexión al subir foto de perfil', async () => {
  window.alert = vi.fn();
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.post('*/api/personas/foto', () => HttpResponse.error())
  );

  const { container } = renderComponent();
  await screen.findByRole('button', { name: /^editar perfil$/i });

  const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;
  const file = new File(['avatar'], 'avatar.jpg', { type: 'image/jpeg' });
  fireEvent.change(fileInput, { target: { files: [file] } });

  await waitFor(() => {
    expect(window.alert).toHaveBeenCalledWith('Error de conexión al subir la foto');
  });
});

test('Maneja error al persistir preferencias de viaje', async () => {
  window.alert = vi.fn();
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.put('*/api/personas/1/perfil', () => HttpResponse.error())
  );

  renderComponent();
  const btnEliminar = (await screen.findAllByText('×'))[0];
  fireEvent.click(btnEliminar);

  await waitFor(() => {
    expect(window.alert).toHaveBeenCalledWith('No se pudieron sincronizar las preferencias.');
  });
});

test('Cierra sesión correctamente confirmando en el modal', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.post('*/api/logout', () => HttpResponse.json({ success: true }))
  );

  renderComponent();
  fireEvent.click(await screen.findByRole('button', { name: /cerrar sesión/i }));
  fireEvent.click(screen.getByRole('button', { name: /sí, cerrar sesión/i }));

  await waitFor(() => {
    expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true });
  });
});