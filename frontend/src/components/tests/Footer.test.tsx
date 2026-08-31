import { render, screen, fireEvent } from '@testing-library/react';
import Footer from '../Footer';

vi.mock('../PrivacyModal', () => ({
  default: ({ isOpen, onClose }: { isOpen: boolean; onClose: () => void }) =>
    isOpen ? (
      <div data-testid="privacy-modal">
        <span>Modal de Privacidad</span>
        <button onClick={onClose}>Cerrar Privacidad</button>
      </div>
    ) : null,
}));

vi.mock('../TermsModal', () => ({
  default: ({ isOpen, onClose }: { isOpen: boolean; onClose: () => void }) =>
    isOpen ? (
      <div data-testid="terms-modal">
        <span>Modal de Términos</span>
        <button onClick={onClose}>Cerrar Términos</button>
      </div>
    ) : null,
}));

beforeEach(() => {
  vi.clearAllMocks();
});

test('Renderiza la estructura del footer con secciones, logotipo y texto de derechos de autor', () => {
  render(<Footer />);

  expect(screen.getByAltText('Icono CompiCar')).toBeInTheDocument();
  expect(screen.getByText(/la plataforma líder para compartir coche/i)).toBeInTheDocument();
  expect(screen.getByText('Plataforma')).toBeInTheDocument();
  expect(screen.getByText('Buscar viaje')).toBeInTheDocument();
  expect(screen.getByText('Publicar viaje')).toBeInTheDocument();
  expect(screen.getByText('📧 compicarsa@gmail.com')).toBeInTheDocument();
  expect(screen.getByText(/© 2026 CompiCar/i)).toBeInTheDocument();
});

test('Gestiona la apertura y cierre del modal de Política de Privacidad', () => {
  render(<Footer />);

  expect(screen.queryByTestId('privacy-modal')).not.toBeInTheDocument();

  const btnPrivacidad = screen.getByRole('button', { name: /política de privacidad/i });
  fireEvent.click(btnPrivacidad);

  expect(screen.getByTestId('privacy-modal')).toBeInTheDocument();

  const btnCerrar = screen.getByRole('button', { name: /cerrar privacidad/i });
  fireEvent.click(btnCerrar);

  expect(screen.queryByTestId('privacy-modal')).not.toBeInTheDocument();
});

test('Gestiona la apertura y cierre del modal de Términos y Condiciones', () => {
  render(<Footer />);

  expect(screen.queryByTestId('terms-modal')).not.toBeInTheDocument();

  const btnTerminos = screen.getByRole('button', { name: /términos y condiciones/i });
  fireEvent.click(btnTerminos);

  expect(screen.getByTestId('terms-modal')).toBeInTheDocument();

  const btnCerrar = screen.getByRole('button', { name: /cerrar términos/i });
  fireEvent.click(btnCerrar);

  expect(screen.queryByTestId('terms-modal')).not.toBeInTheDocument();
});