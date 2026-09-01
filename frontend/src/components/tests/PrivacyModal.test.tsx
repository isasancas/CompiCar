import { render, screen, fireEvent } from '@testing-library/react';
import PrivacyModal from '../PrivacyModal';

const mockOnClose = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
});

test('No renderiza nada en el DOM cuando isOpen es false', () => {
  const { container } = render(<PrivacyModal isOpen={false} onClose={mockOnClose} />);
  expect(container).toBeEmptyDOMElement();
});

test('Renderiza el modal con el título principal y las secciones informativas cuando isOpen es true', () => {
  render(<PrivacyModal isOpen={true} onClose={mockOnClose} />);

  expect(screen.getByRole('heading', { level: 2, name: 'Política de Privacidad' })).toBeInTheDocument();
  expect(screen.getByText('CompiCar · Protección de Datos')).toBeInTheDocument();

  // Verificación de secciones clave
  expect(screen.getByText('1. Responsable del tratamiento')).toBeInTheDocument();
  expect(screen.getByText('2. Datos personales recogidos')).toBeInTheDocument();
  expect(screen.getByText('7. Protección de datos de pago')).toBeInTheDocument();
  expect(screen.getAllByText('compicarsa@gmail.com').length).toBeGreaterThan(0);
});

test('Ejecuta la función onClose al hacer clic en el botón de cierre superior (✕)', () => {
  render(<PrivacyModal isOpen={true} onClose={mockOnClose} />);

  const btnCerrarIcono = screen.getByRole('button', { name: '✕' });
  fireEvent.click(btnCerrarIcono);

  expect(mockOnClose).toHaveBeenCalledTimes(1);
});

test('Ejecuta la función onClose al hacer clic en el botón inferior "Cerrar y volver"', () => {
  render(<PrivacyModal isOpen={true} onClose={mockOnClose} />);

  const btnCerrarBoton = screen.getByRole('button', { name: /cerrar y volver/i });
  fireEvent.click(btnCerrarBoton);

  expect(mockOnClose).toHaveBeenCalledTimes(1);
});