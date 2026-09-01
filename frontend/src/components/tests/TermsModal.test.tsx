import { render, screen, fireEvent } from '@testing-library/react';
import TermsModal from '../TermsModal';

const mockOnClose = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
});

test('No renderiza nada en el DOM cuando isOpen es false', () => {
  const { container } = render(<TermsModal isOpen={false} onClose={mockOnClose} />);
  expect(container).toBeEmptyDOMElement();
});

test('Renderiza el modal con el título principal y las cláusulas cuando isOpen es true', () => {
  render(<TermsModal isOpen={true} onClose={mockOnClose} />);

  expect(screen.getByRole('heading', { level: 2, name: 'Términos y Condiciones' })).toBeInTheDocument();
  expect(screen.getByText('CompiCar · Acuerdo de Uso')).toBeInTheDocument();

  // Verificación de cláusulas y secciones legales
  expect(screen.getByText('1. Naturaleza del Servicio')).toBeInTheDocument();
  expect(screen.getByText('2. Requisitos del Conductor')).toBeInTheDocument();
  expect(screen.getByText('3. Reservas y Pagos')).toBeInTheDocument();
  expect(screen.getByText('4. Cálculo de Costes y Comisión de la Plataforma')).toBeInTheDocument();
  expect(screen.getByText('5. Política de Cancelación')).toBeInTheDocument();
  expect(screen.getByText('6. Normas de Comportamiento')).toBeInTheDocument();
  expect(screen.getByText('7. Responsabilidad')).toBeInTheDocument();
});

test('Ejecuta la función onClose al hacer clic en el botón de cierre superior (✕)', () => {
  render(<TermsModal isOpen={true} onClose={mockOnClose} />);

  const btnCerrarIcono = screen.getByRole('button', { name: '✕' });
  fireEvent.click(btnCerrarIcono);

  expect(mockOnClose).toHaveBeenCalledTimes(1);
});

test('Ejecuta la función onClose al hacer clic en el botón inferior "Aceptar"', () => {
  render(<TermsModal isOpen={true} onClose={mockOnClose} />);

  const btnAceptar = screen.getByRole('button', { name: /aceptar/i });
  fireEvent.click(btnAceptar);

  expect(mockOnClose).toHaveBeenCalledTimes(1);
});