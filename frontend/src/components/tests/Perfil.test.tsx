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
  telefono: '600123456',
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
    // Simulamos un array de 2 elementos para que coincida con las 2 reseñas mostradas
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([
      { id: 1, puntuacion: 5 },
      { id: 2, puntuacion: 4 }
    ]))
  );

  renderComponent();

  expect(screen.getByText(/cargando.../i)).toBeInTheDocument();

  // Verificar datos personales cargados
  expect(await screen.findByText(/Carlos García/i)).toBeInTheDocument();
  expect(screen.getByText(/carlos@test.com/i)).toBeInTheDocument();
  expect(screen.getByText(/600123456/i)).toBeInTheDocument();

  // Verificar monedero
  expect(screen.getByText('25.00 €')).toBeInTheDocument();
  expect(screen.getByText('100.00 €')).toBeInTheDocument();

  // Verificar vehículo propio
  expect(screen.getByText('Renault Clio (1234ABC)')).toBeInTheDocument();

  // Verificar valoraciones
  expect(screen.getByText(/4.5 \/ 5/i)).toBeInTheDocument();
  // Cambia esto en tu test (línea 89):
  expect(screen.getByText((content) => content.includes('reseñas'))).toBeInTheDocument();
});

test('Realiza la retirada de fondos de forma exitosa cuando hay saldo suficiente', async () => {
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
  expect(btnRetirar).not.toBeDisabled();
  
  fireEvent.click(btnRetirar);

  await waitFor(() => {
    expect(retirarCalled).toBe(true);
    expect(screen.getByText(/¡Retiro completado con éxito!/i)).toBeInTheDocument();
  });
});

test('Permite borrar un vehículo de la lista de vehículos propios', async () => {
  let deleteCalled = false;
  
  // Inicializamos window.confirm antes de espiarlo para evitar el error de undefined
  window.confirm = vi.fn().mockReturnValue(true);

  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json(mockVehiculosData)),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.delete('*/api/vehiculos/10', () => {
      deleteCalled = true;
      return new HttpResponse(null, { status: 200 });
    })
  );

  renderComponent();

  const btnBorrar = await screen.findByRole('button', { name: /^borrar$/i });
  fireEvent.click(btnBorrar);

  await waitFor(() => {
    expect(deleteCalled).toBe(true);
  });
});

test('Abre el modal de cierre de sesión y redirige al confirmar', async () => {
  server.use(
    http.get('*/api/personas/perfil', () => HttpResponse.json(mockPerfilData)),
    http.get('*/api/vehiculos/propios', () => HttpResponse.json([])),
    http.get('*/api/viajes/mis-viajes', () => HttpResponse.json([])),
    http.get('*/api/valoraciones/valorado/1', () => HttpResponse.json([])),
    http.post('*/api/logout', () => HttpResponse.json({ success: true }))
  );

  renderComponent();

  const btnCerrarSesion = await screen.findByRole('button', { name: /cerrar sesión/i });
  fireEvent.click(btnCerrarSesion);

  // Aparece el modal de confirmación
  expect(screen.getByText(/confirmar cierre de sesión/i)).toBeInTheDocument();

  const btnConfirmar = screen.getByRole('button', { name: /sí, cerrar sesión/i });
  fireEvent.click(btnConfirmar);

  await waitFor(() => {
    expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true });
    expect(localStorage.getItem('token')).toBeNull();
  });
});