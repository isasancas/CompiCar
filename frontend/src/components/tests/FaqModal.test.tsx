import { render, screen, fireEvent } from '@testing-library/react';
import FaqModal from '../FaqModal';

const defaultProps = {
  isOpen: true,
  onClose: vi.fn(),
};

beforeEach(() => {
  vi.clearAllMocks();
});

test('No renderiza el modal cuando isOpen es false', () => {
render(<FaqModal isOpen={false} onClose={defaultProps.onClose} />);
expect(screen.queryByText('Preguntas Frecuentes (FAQ)')).not.toBeInTheDocument();
});

test('Renderiza el modal con el título, buscador, categorías y preguntas cuando isOpen es true', () => {
render(<FaqModal {...defaultProps} />);

expect(screen.getByText('Preguntas Frecuentes (FAQ)')).toBeInTheDocument();
expect(screen.getByPlaceholderText(/buscar por palabra clave/i)).toBeInTheDocument();
expect(screen.getByText('🚗 Generales y Cuenta')).toBeInTheDocument();
expect(screen.getByText('¿Qué es CompiCar?')).toBeInTheDocument();
});

test('Llama a la función onClose al pulsar el botón de cerrar superior (×) o el botón Cerrar del pie', () => {
render(<FaqModal {...defaultProps} />);

const btnCerrarX = screen.getByRole('button', { name: '×' });
fireEvent.click(btnCerrarX);
expect(defaultProps.onClose).toHaveBeenCalledTimes(1);

const btnCerrarPie = screen.getByRole('button', { name: /cerrar/i });
fireEvent.click(btnCerrarPie);
expect(defaultProps.onClose).toHaveBeenCalledTimes(2);
});

test('Filtra las preguntas en tiempo real según el término introducido en el buscador', () => {
render(<FaqModal {...defaultProps} />);

const searchInput = screen.getByPlaceholderText(/buscar por palabra clave/i);
fireEvent.change(searchInput, { target: { value: 'check-in' } });

expect(screen.getByText('¿Qué es el código de check-in y para qué sirve?')).toBeInTheDocument();
expect(screen.queryByText('¿Qué es CompiCar?')).not.toBeInTheDocument();
});

test('Muestra el mensaje de aviso cuando ningún resultado coincide con la búsqueda', () => {
render(<FaqModal {...defaultProps} />);

const searchInput = screen.getByPlaceholderText(/buscar por palabra clave/i);
fireEvent.change(searchInput, { target: { value: 'termino_inexistente_123' } });

expect(screen.getByText(/no se encontraron preguntas relacionadas con/i)).toBeInTheDocument();
});

test('Limpia el campo de búsqueda al pulsar el botón "Limpiar"', () => {
render(<FaqModal {...defaultProps} />);

const searchInput = screen.getByPlaceholderText(/buscar por palabra clave/i);
fireEvent.change(searchInput, { target: { value: 'Stripe' } });

const btnLimpiar = screen.getByRole('button', { name: /limpiar/i });
fireEvent.click(btnLimpiar);

expect(searchInput).toHaveValue('');
expect(screen.getByText('¿Qué es CompiCar?')).toBeInTheDocument();
});

test('Muestra el enlace de contacto directo con compicarsa@gmail.com en el pie', () => {
render(<FaqModal {...defaultProps} />);

const mailLink = screen.getByRole('link', { name: /compicarsa@gmail.com/i });
expect(mailLink).toHaveAttribute('href', 'mailto:compicarsa@gmail.com');
});