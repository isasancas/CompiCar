import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../../setupTests';
import NuevoVehiculo from '../vehiculos/NuevoVehiculo';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.setItem('token', 'fake-jwt-token');
});

const renderComponente = () => {
  return render(
    <MemoryRouter initialEntries={['/vehiculo/nuevo']}>
      <Routes>
        <Route path="/vehiculo/nuevo" element={<NuevoVehiculo />} />
      </Routes>
    </MemoryRouter>
  );
};

// Función auxiliar para rellenar los inputs del formulario de forma consistente
const llenarFormulario = (container: HTMLElement, datosCustom = {}) => {
  const valores = {
    matricula: '1234ABC',
    marca: 'Seat',
    modelo: 'Ibiza',
    plazas: '5',
    consumo: '5.5',
    anio: '2020',
    tipo: 'COCHE',
    ...datosCustom,
  };

  const matriculaInput = container.querySelector<HTMLInputElement>('input[name="matricula"]')!;
  const marcaInput = container.querySelector<HTMLInputElement>('input[name="marca"]')!;
  const modeloInput = container.querySelector<HTMLInputElement>('input[name="modelo"]')!;
  const plazasInput = container.querySelector<HTMLInputElement>('input[name="plazas"]')!;
  const consumoInput = container.querySelector<HTMLInputElement>('input[name="consumo"]')!;
  const anioInput = container.querySelector<HTMLInputElement>('input[name="anio"]')!;
  const tipoSelect = container.querySelector<HTMLSelectElement>('select[name="tipo"]')!;

  fireEvent.change(matriculaInput, { target: { value: valores.matricula, name: 'matricula' } });
  fireEvent.change(marcaInput, { target: { value: valores.marca, name: 'marca' } });
  fireEvent.change(modeloInput, { target: { value: valores.modelo, name: 'modelo' } });
  fireEvent.change(plazasInput, { target: { value: valores.plazas, name: 'plazas' } });
  fireEvent.change(consumoInput, { target: { value: valores.consumo, name: 'consumo' } });
  fireEvent.change(anioInput, { target: { value: valores.anio, name: 'anio' } });
  fireEvent.change(tipoSelect, { target: { value: valores.tipo, name: 'tipo' } });
};

// Función auxiliar para enviar el formulario
const submitFormulario = (container: HTMLElement) => {
  const form = container.querySelector('form')!;
  fireEvent.submit(form);
};

test('Redirige a /inicio-sesion si el usuario no tiene token de autenticación', async () => {
  localStorage.removeItem('token');

  renderComponente();

  await waitFor(() => {
    expect(mockNavigate).toHaveBeenCalledWith('/inicio-sesion', { replace: true });
  });
});

test('Muestra error de validación local cuando la matrícula no tiene un formato válido', async () => {
  const { container } = renderComponente();

  llenarFormulario(container, { matricula: 'INVALIDA' });
  submitFormulario(container);

  expect(await screen.findByText('El formato de matrícula no es válido.')).toBeInTheDocument();
});

test('Permite crear un vehículo correctamente y navega a /perfil', async () => {
  let postBody: any = null;

  server.use(
    http.post('*/api/vehiculos', async ({ request }) => {
      postBody = await request.json();
      return HttpResponse.json({ id: 1, ...postBody }, { status: 201 });
    })
  );

  const { container } = renderComponente();

  llenarFormulario(container);
  submitFormulario(container);

  await waitFor(() => {
    expect(postBody).toEqual({
      matricula: '1234ABC',
      marca: 'Seat',
      modelo: 'Ibiza',
      plazas: 5,
      consumo: 5.5,
      anio: 2020,
      tipo: 'COCHE',
    });
    expect(mockNavigate).toHaveBeenCalledWith('/perfil');
  });
});

test('Muestra el mensaje de error devuelto por la API al fallar la creación', async () => {
  server.use(
    http.post('*/api/vehiculos', () => {
      return HttpResponse.json({ error: 'La matrícula ya está registrada.' }, { status: 400 });
    })
  );

  const { container } = renderComponente();

  llenarFormulario(container);
  submitFormulario(container);

  expect(await screen.findByText('La matrícula ya está registrada.')).toBeInTheDocument();
});

test('Muestra un mensaje de error de conexión si falla la red', async () => {
  server.use(
    http.post('*/api/vehiculos', () => {
      return HttpResponse.error();
    })
  );

  const { container } = renderComponente();

  llenarFormulario(container);
  submitFormulario(container);

  expect(await screen.findByText('Error de conexión al crear el vehículo.')).toBeInTheDocument();
});

test('Navega de vuelta a la pantalla de perfil al pulsar "Volver al perfil"', () => {
  renderComponente();

  const btnVolver = screen.getByRole('button', { name: /volver al perfil/i });
  fireEvent.click(btnVolver);

  expect(mockNavigate).toHaveBeenCalledWith('/perfil');
});