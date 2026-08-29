import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import { handlers } from '../../mocks/handlers';
import InicioSesion from '../autenticacion/InicioSesion';

const server = setupServer(...handlers);

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  localStorage.clear();
});
afterAll(() => server.close());

const renderComponent = () =>
  render(
    <MemoryRouter>
      <InicioSesion />
    </MemoryRouter>
  );

test('guarda el token en localStorage al recibir HTTP 200', async () => {
  const user = userEvent.setup();
  renderComponent();

  await user.type(screen.getByTestId('input-email'), 'usuario@test.com');
  await user.type(screen.getByTestId('input-password'), 'Password123');
  await user.click(screen.getByTestId('btn-login'));

  // Comprueba que se haya guardado el JWT en localStorage sin usar un navegador real
  expect(await screen.findByRole('button')).not.toBeDisabled();
  expect(localStorage.getItem('token')).toBe('mocked-jwt-token-123');
});

test('muestra el mensaje de error procesado del backend', async () => {
  const user = userEvent.setup();
  renderComponent();

  await user.type(screen.getByTestId('input-email'), 'error@test.com');
  await user.type(screen.getByTestId('input-password'), 'Password123');
  await user.click(screen.getByTestId('btn-login'));

  expect(await screen.findByTestId('login-password-error')).toHaveTextContent(
    'La contraseña es incorrecta'
  );
});

test('captura errores de conexión de red cuando el servidor se cae', async () => {
  // Sobrescribe el handler por defecto simulando fallo de red total
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