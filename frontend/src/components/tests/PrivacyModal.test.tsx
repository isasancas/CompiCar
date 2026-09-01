import { render, screen, fireEvent } from '@testing-library/react';
import PrivacyModal from '../PrivacyModal';

const mockOnClose = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
});

test('no renderiza nada en el DOM cuando isOpen es false', () => {
  const { container } = render(<PrivacyModal isOpen={false} onClose={mockOnClose} />);
  expect(container).toBeEmptyDOMElement();
});

test('renderiza todo el modal y sus 10 secciones cuando isOpen es true', () => {
  render(<PrivacyModal isOpen={true} onClose={mockOnClose} />);

  expect(screen.getByText('Política de Privacidad')).toBeInTheDocument();
  expect(screen.getByText('CompiCar · Protección de Datos')).toBeInTheDocument();

  expect(screen.getByText('1. Responsable del tratamiento')).toBeInTheDocument();
  expect(screen.getByText('2. Datos personales recogidos')).toBeInTheDocument();
  expect(screen.getByText('3. Finalidad del tratamiento')).toBeInTheDocument();
  expect(screen.getByText('4. Base legal del tratamiento')).toBeInTheDocument();
  expect(screen.getByText('5. Principios de protección de datos')).toBeInTheDocument();
  expect(screen.getByText('6. Derechos del usuario')).toBeInTheDocument();
  expect(screen.getByText('7. Protección de datos de pago')).toBeInTheDocument();
  expect(screen.getByText('8. Conservación y eliminación')).toBeInTheDocument();
  expect(screen.getByText('9. Consentimiento y aceptación')).toBeInTheDocument();
  expect(screen.getByText('10. Contacto')).toBeInTheDocument();
});

test('ejecuta onClose al pulsar el botón superior de cierre', () => {
  render(<PrivacyModal isOpen={true} onClose={mockOnClose} />);

  const botones = screen.getAllByRole('button');
  fireEvent.click(botones[0]);

  expect(mockOnClose).toHaveBeenCalledTimes(1);
});

test('ejecuta onClose al pulsar el botón inferior "Cerrar y volver"', () => {
  render(<PrivacyModal isOpen={true} onClose={mockOnClose} />);

  const btnCerrar = screen.getByText('Cerrar y volver');
  fireEvent.click(btnCerrar);

  expect(mockOnClose).toHaveBeenCalledTimes(1);
});