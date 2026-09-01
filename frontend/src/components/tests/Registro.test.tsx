import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../../setupTests';
import Registro from '../autenticacion/Registro';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const renderComponent = () => {
  return render(
    <MemoryRouter initialEntries={['/registro']}>
      <Routes>
        <Route path="/registro" element={<Registro />} />
        <Route path="/perfil" element={<div>Página de Perfil</div>} />
      </Routes>
    </MemoryRouter>
  );
};

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.clear();
});

test('Muestra errores de validación en cliente si se intenta enviar el formulario vacío', async () => {
  renderComponent();

  const btnSubmit = screen.getByRole('button', { name: /^registrarse$/i });
  fireEvent.click(btnSubmit);

  expect(await screen.findByText(/el nombre no puede estar vacío/i)).toBeInTheDocument();
  expect(screen.getByText(/el primer apellido no puede estar vacío/i)).toBeInTheDocument();
  expect(screen.getByText(/el email no puede estar vacío/i)).toBeInTheDocument();
  expect(screen.getByText(/el teléfono no puede estar vacío/i)).toBeInTheDocument();
  expect(screen.getByText(/la contraseña no puede estar vacía/i)).toBeInTheDocument();
  expect(screen.getByText(/por favor, acepta los términos y la política de privacidad/i)).toBeInTheDocument();
});

test('Muestra error si las contraseñas no coinciden', async () => {
  const user = userEvent.setup();
  renderComponent();

  await user.type(screen.getByLabelText(/^nombre$/i), 'Carlos');
  await user.type(screen.getByLabelText(/^primer apellido$/i), 'García');
  await user.type(screen.getByLabelText(/^teléfono$/i), '600123456');
  await user.type(screen.getByLabelText(/^email$/i), 'carlos@test.com');
  await user.type(screen.getByLabelText(/^contraseña$/i), 'Password123');
  await user.type(screen.getByLabelText(/^confirmar contraseña$/i), 'PasswordDiferente');
  
  const checkbox = screen.getByRole('checkbox');
  await user.click(checkbox);

  await user.click(screen.getByRole('button', { name: /^registrarse$/i }));

  expect(await screen.findByText(/las contraseñas no coinciden/i)).toBeInTheDocument();
});

test('Realiza el registro y login automático de forma exitosa, mostrando el modal de éxito', async () => {
  const user = userEvent.setup();

  server.use(
    http.post('*/api/registro', () => {
      return HttpResponse.json({ success: true }, { status: 201 });
    }),
    http.post('*/api/login', () => {
      return HttpResponse.json({ token: 'fake-jwt-token-registro' }, { status: 200 });
    })
  );

  renderComponent();

  await user.type(screen.getByLabelText(/^nombre$/i), 'Carlos');
  await user.type(screen.getByLabelText(/^primer apellido$/i), 'García');
  await user.type(screen.getByLabelText(/^teléfono$/i), '600123456');
  await user.type(screen.getByLabelText(/^email$/i), 'carlos@test.com');
  await user.type(screen.getByLabelText(/^contraseña$/i), 'Password123');
  await user.type(screen.getByLabelText(/^confirmar contraseña$/i), 'Password123');
  
  await user.click(screen.getByRole('checkbox'));
  await user.click(screen.getByRole('button', { name: /^registrarse$/i }));

  // Verificar que aparece el modal de éxito
  expect(await screen.findByText(/registro completado/i)).toBeInTheDocument();
  expect(localStorage.getItem('token')).toBe('fake-jwt-token-registro');

  // Hacer click en el botón del modal para ir al perfil
  const btnIrPerfil = screen.getByRole('button', { name: /ir a mi perfil/i });
  await user.click(btnIrPerfil);

  expect(mockNavigate).toHaveBeenCalledWith('/perfil');
});

test('Mapea correctamente los errores devueltos por el backend a los campos del formulario', async () => {
  const user = userEvent.setup();

  server.use(
    http.post('*/api/registro', () => {
      return HttpResponse.json(
        { error: 'El email ya está registrado; El teléfono no es válido' },
        { status: 400 }
      );
    })
  );

  renderComponent();

  await user.type(screen.getByLabelText(/^nombre$/i), 'Carlos');
  await user.type(screen.getByLabelText(/^primer apellido$/i), 'García');
  // Usa un teléfono válido para saltar la validación de cliente:
  await user.type(screen.getByLabelText(/^teléfono$/i), '600123456'); 
  await user.type(screen.getByLabelText(/^email$/i), 'repetido@test.com');
  await user.type(screen.getByLabelText(/^contraseña$/i), 'Password123');
  await user.type(screen.getByLabelText(/^confirmar contraseña$/i), 'Password123');
  
  await user.click(screen.getByRole('checkbox'));
  await user.click(screen.getByRole('button', { name: /^registrarse$/i }));

  // Ahora la petición llegará al backend mockeado y responderá con ambos errores:
  expect(await screen.findByText(/el email ya está registrado/i)).toBeInTheDocument();
  expect(screen.getByText(/el teléfono no es válido/i)).toBeInTheDocument();
});