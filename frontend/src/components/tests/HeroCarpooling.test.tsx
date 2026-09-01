import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import HeroCarpooling from '../HeroCarpooling';

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
});

const renderComponente = () => {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route path="/" element={<HeroCarpooling />} />
      </Routes>
    </MemoryRouter>
  );
};

test('Renderiza correctamente la información principal, entradas del formulario e imagen', () => {
  renderComponente();

  expect(screen.getByText(/comparte el camino,/i)).toBeInTheDocument();
  expect(screen.getByText(/reduce el gasto./i)).toBeInTheDocument();
  expect(screen.getByPlaceholderText('Donde empiezas?')).toBeInTheDocument();
  expect(screen.getByPlaceholderText('Adonde vas?')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /explorar todos los viajes/i })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /buscar viaje/i })).toBeInTheDocument();
  expect(screen.getByAltText('Amigos en coche')).toBeInTheDocument();
});

test('Navega hacia /explorar al pulsar en el botón "Explorar todos los viajes"', () => {
  renderComponente();

  const btnExplorar = screen.getByRole('button', { name: /explorar todos los viajes/i });
  fireEvent.click(btnExplorar);

  expect(mockNavigate).toHaveBeenCalledWith('/explorar');
});

test('Procesa el formulario de búsqueda formateando los query params y eliminando espacios en blanco', () => {
  const { container } = renderComponente();

  const inputOrigen = screen.getByPlaceholderText('Donde empiezas?');
  const inputDestino = screen.getByPlaceholderText('Adonde vas?');
  const inputFecha = container.querySelector<HTMLInputElement>('input[type="date"]')!;
  const btnBuscar = screen.getByRole('button', { name: /buscar viaje/i });

  fireEvent.change(inputOrigen, { target: { value: '  Madrid  ' } });
  fireEvent.change(inputDestino, { target: { value: 'Barcelona' } });
  fireEvent.change(inputFecha, { target: { value: '2026-10-15' } });

  fireEvent.click(btnBuscar);

  expect(mockNavigate).toHaveBeenCalledWith('/buscar?origen=Madrid&destino=Barcelona&fecha=2026-10-15');
});

test('Permite enviar el formulario sin parámetros cuando los campos se encuentran vacíos', () => {
  renderComponente();

  const btnBuscar = screen.getByRole('button', { name: /buscar viaje/i });
  fireEvent.click(btnBuscar);

  expect(mockNavigate).toHaveBeenCalledWith('/buscar?');
});