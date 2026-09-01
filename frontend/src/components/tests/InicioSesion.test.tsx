import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import { handlers } from '../../mocks/handlers';
import InicioSesion from '../autenticacion/InicioSesion';

// Mock del hook useNavigate de react-router-dom
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const server = setupServer(...handlers);

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  localStorage.clear();
  vi.clearAllMocks();
});
afterAll(() => server.close());

const renderComponent = () =>
  render(
    <MemoryRouter>
      <InicioSesion />
    </MemoryRouter>
  );

test('Guarda el token en localStorage, dispara authChange y navega a /perfil al recibir HTTP 200', async () => {
  const authListener = vi.fn();
  window.addEventListener('authChange', authListener);

  const user = userEvent.setup();
  renderComponent();

  await user.type(screen.getByTestId('input-email'), 'usuario@test.com');
  await user.type(screen.getByTestId('input-password'), 'Password123');
  await user.click(screen.getByTestId('btn-login'));

  await waitFor(() => {
    expect(localStorage.getItem('token')).toBe('mocked-jwt-token-123');
    expect(authListener).toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith('/perfil');
  });

  window.removeEventListener('authChange', authListener);
});

test('Muestra errores de validación en cliente cuando los campos están vacíos o el email es inválido', async () => {
  const user = userEvent.setup();
  renderComponent();

  fireEvent.submit(screen.getByTestId('login-form'));
  expect(await screen.findByTestId('login-email-error')).toHaveTextContent('El email no puede estar vacío');
  expect(screen.getByTestId('login-password-error')).toHaveTextContent('La contraseña no puede estar vacía');

  await user.type(screen.getByTestId('input-email'), 'email-invalido');
  fireEvent.submit(screen.getByTestId('login-form'));
  expect(await screen.findByTestId('login-email-error')).toHaveTextContent('El email no es válido');
});

test('Limpia los errores al modificar el texto de los campos', async () => {
  const user = userEvent.setup();
  renderComponent();

  fireEvent.submit(screen.getByTestId('login-form'));
  expect(await screen.findByTestId('login-email-error')).toBeInTheDocument();

  await user.type(screen.getByTestId('input-email'), 'a');
  expect(screen.queryByTestId('login-email-error')).not.toBeInTheDocument();
});

test('muestra error si la respuesta 200 no incluye un token válido', async () => {
  server.use(
    http.post('*/api/login', () => {
      return HttpResponse.json({ token: '   ' });
    })
  );

  const user = userEvent.setup();
  renderComponent();

  await user.type(screen.getByTestId('input-email'), 'usuario@test.com');
  await user.type(screen.getByTestId('input-password'), 'Password123');
  await user.click(screen.getByTestId('btn-login'));

  expect(await screen.findByTestId('login-general-error')).toHaveTextContent(
    'No se recibió un token válido del servidor.'
  );
});

test('Asigna el error al campo de email si el mensaje del backend contiene la palabra email', async () => {
  server.use(
    http.post('*/api/login', () => {
      return HttpResponse.json({ error: 'El email especificado no está registrado' }, { status: 400 });
    })
  );

  const user = userEvent.setup();
  renderComponent();

  await user.type(screen.getByTestId('input-email'), 'desconocido@test.com');
  await user.type(screen.getByTestId('input-password'), 'Password123');
  await user.click(screen.getByTestId('btn-login'));

  expect(await screen.findByTestId('login-email-error')).toHaveTextContent(
    'El email especificado no está registrado'
  );
});

test('Obtiene el mensaje de error si viene en la propiedad message en lugar de error', async () => {
  server.use(
    http.post('*/api/login', () => {
      return HttpResponse.json({ message: 'La contraseña introducida es errónea' }, { status: 400 });
    })
  );

  const user = userEvent.setup();
  renderComponent();

  await user.type(screen.getByTestId('input-email'), 'usuario@test.com');
  await user.type(screen.getByTestId('input-password'), 'BadPass123');
  await user.click(screen.getByTestId('btn-login'));

  expect(await screen.findByTestId('login-password-error')).toHaveTextContent(
    'La contraseña introducida es errónea'
  );
});

test('Muestra error general si la respuesta del backend no atañe ni a email ni a contraseña', async () => {
  server.use(
    http.post('*/api/login', () => {
      return HttpResponse.json({ error: 'Cuenta bloqueada temporalmente' }, { status: 403 });
    })
  );

  const user = userEvent.setup();
  renderComponent();

  await user.type(screen.getByTestId('input-email'), 'bloqueado@test.com');
  await user.type(screen.getByTestId('input-password'), 'Password123');
  await user.click(screen.getByTestId('btn-login'));

  expect(await screen.findByTestId('login-general-error')).toHaveTextContent(
    'Cuenta bloqueada temporalmente'
  );
});

test('Utiliza el mensaje por defecto cuando la respuesta de error no es un JSON válido', async () => {
  server.use(
    http.post('*/api/login', () => {
      return new HttpResponse('Internal Server Error', {
        status: 500,
        headers: { 'Content-Type': 'text/plain' },
      });
    })
  );

  const user = userEvent.setup();
  renderComponent();

  await user.type(screen.getByTestId('input-email'), 'error500@test.com');
  await user.type(screen.getByTestId('input-password'), 'Password123');
  await user.click(screen.getByTestId('btn-login'));

  expect(await screen.findByTestId('login-general-error')).toHaveTextContent(
    'Error en el inicio de sesión'
  );
});

test('Captura errores de conexión de red cuando el servidor se cae', async () => {
  server.use(
    http.post('*/api/login', () => {
      return HttpResponse.error();
    })
  );

  const user = userEvent.setup();
  renderComponent();

  await user.type(screen.getByTestId('input-email'), 'conexion@test.com');
  await user.type(screen.getByTestId('input-password'), 'Password123');
  await user.click(screen.getByTestId('btn-login'));

  expect(await screen.findByTestId('login-general-error')).toHaveTextContent(
    'Error de conexión'
  );
});