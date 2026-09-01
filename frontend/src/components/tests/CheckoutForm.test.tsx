import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import CheckoutForm from '../pagos/CheckoutForm';

const mockConfirmPayment = vi.fn();
let mockStripe: any = null;
let mockElements: any = null;

vi.mock('@stripe/react-stripe-js', () => ({
  useStripe: () => mockStripe,
  useElements: () => mockElements,
  PaymentElement: () => <div data-testid="payment-element">Elemento de Pago de Stripe</div>,
}));

beforeEach(() => {
  vi.clearAllMocks();
  mockConfirmPayment.mockReset();
  mockElements = {};
  mockStripe = {
    confirmPayment: mockConfirmPayment,
  };
});

const mockOnSuccess = vi.fn();
const mockOnError = vi.fn();

const renderComponente = (monto = 15.5) => {
  return render(
    <CheckoutForm
      clientSecret="pi_123_secret_abc"
      onSuccess={mockOnSuccess}
      onError={mockOnError}
      monto={monto}
    />
  );
};

test('Renders form with formatted amount and Stripe payment element', () => {
  renderComponente(25);

  expect(screen.getByTestId('payment-element')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /confirmar pago de 25.00€/i })).toBeInTheDocument();
});

test('Disables submit button when Stripe is not loaded', () => {
  mockStripe = null;

  renderComponente(10);

  const button = screen.getByRole('button', { name: /confirmar pago de 10.00€/i });
  expect(button).toBeDisabled();
});

test('Processes payment successfully and calls onSuccess when status is succeeded', async () => {
  mockConfirmPayment.mockResolvedValueOnce({
    paymentIntent: { id: 'pi_test_123', status: 'succeeded' },
  });

  renderComponente(15);

  const button = screen.getByRole('button', { name: /confirmar pago de 15.00€/i });
  fireEvent.click(button);

  await waitFor(() => {
    expect(mockConfirmPayment).toHaveBeenCalledWith({
      elements: mockElements,
      redirect: 'if_required',
    });
    expect(mockOnSuccess).toHaveBeenCalledWith('pi_test_123');
  });
});

test('Processes payment successfully when status is requires_capture', async () => {
  mockConfirmPayment.mockResolvedValueOnce({
    paymentIntent: { id: 'pi_test_456', status: 'requires_capture' },
  });

  renderComponente(20);

  const button = screen.getByRole('button', { name: /confirmar pago de 20.00€/i });
  fireEvent.click(button);

  await waitFor(() => {
    expect(mockOnSuccess).toHaveBeenCalledWith('pi_test_456');
  });
});

test('Displays backend error message and executes onError callback upon payment failure', async () => {
  mockConfirmPayment.mockResolvedValueOnce({
    error: { message: 'Tarjeta rechazada.' },
  });

  renderComponente(15);

  const button = screen.getByRole('button', { name: /confirmar pago de 15.00€/i });
  fireEvent.click(button);

  expect(await screen.findByText('Tarjeta rechazada.')).toBeInTheDocument();
  expect(mockOnError).toHaveBeenCalledWith('Tarjeta rechazada.');
});

test('Displays fallback error message when Stripe error object has no message', async () => {
  mockConfirmPayment.mockResolvedValueOnce({
    error: {},
  });

  renderComponente(15);

  const button = screen.getByRole('button', { name: /confirmar pago de 15.00€/i });
  fireEvent.click(button);

  expect(await screen.findByText('Ocurrió un error inesperado.')).toBeInTheDocument();
  expect(mockOnError).toHaveBeenCalledWith('Ocurrió un error inesperado.');
});