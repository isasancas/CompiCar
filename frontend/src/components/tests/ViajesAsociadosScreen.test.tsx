import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../../setupTests';
import ViajesAsociadosScreen from '../viajes/ViajesAsociadosScreen';

// Mock de Stripe
vi.mock('@stripe/stripe-js', () => ({
  loadStripe: vi.fn(() => Promise.resolve({})),
}));

vi.mock('@stripe/react-stripe-js', () => ({
  Elements: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

// Mock del formulario de pago
vi.mock('../pagos/CheckoutForm', () => ({
  default: ({ onSuccess, onError, monto }: { onSuccess: () => void; onError: (msg: string) => void; monto: number }) => (
    <div data-testid="checkout-form">
      <span>Monto: {monto}€</span>
      <button 
        type="button" 
        data-testid="btn-simular-pago-exitoso" 
        onClick={() => onSuccess()}
      >
        Simular Pago Exitoso
      </button>
      <button 
        type="button" 
        data-testid="btn-simular-pago-fallido" 
        onClick={() => onError('Error en el procesamiento del pago')}
      >
        Simular Fallo Pago
      </button>
    </div>
  ),
}));

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// Datos de prueba
const mockParadas = [
  { id: 1, localizacion: 'Madrid', tipo: 'ORIGEN', orden: 1 },
  { id: 2, localizacion: 'Zaragoza', tipo: 'INTERMEDIA', orden: 2 },
  { id: 3, localizacion: 'Barcelona', tipo: 'DESTINO', orden: 3 },
];

const mockPadre = {
  id: 100,
  slug: 'madrid-barcelona-padre',
  origen: 'Madrid',
  destino: 'Barcelona',
  precio: 20,
  estado: 'PENDIENTE',
  paradas: mockParadas,
  reservas: [],
};

const mockInstancias = [
  {
    id: 101,
    slug: 'madrid-barcelona-instancia-1',
    fechaHoraSalida: new Date(Date.now() + 86400000).toISOString(),
    estado: 'PENDIENTE',
    plazasDisponibles: 3,
    precio: 20,
    paradas: mockParadas,
    reservas: [],
  },
  {
    id: 102,
    slug: 'madrid-barcelona-instancia-2',
    fechaHoraSalida: new Date(Date.now() + 172800000).toISOString(),
    estado: 'PENDIENTE',
    plazasDisponibles: 2,
    precio: 20,
    paradas: mockParadas,
    reservas: [],
  },
];

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.setItem('token', 'fake-jwt-token');
  localStorage.setItem('userId', '5');
});

afterEach(async () => {
  if (typeof window !== 'undefined' && 'happyDOM' in window) {
    await (window as any).happyDOM.whenAsyncComplete();
  }
});

const renderConRuta = (stateExtra = {}) => {
  const navState = {
    viajesRecurrentes: mockInstancias,
    viajePadre: mockPadre,
    slugPadre: 'madrid-barcelona-padre',
    rol: 'PASAJERO',
    usuarioId: 5,
    ...stateExtra,
  };

  return render(
    <MemoryRouter initialEntries={[{ pathname: '/viajes/asociados', state: navState }]}>
      <Routes>
        <Route path="/viajes/asociados" element={<ViajesAsociadosScreen />} />
        <Route path="/viajes/:slug" element={<div>Pagina Detalle Viaje</div>} />
      </Routes>
    </MemoryRouter>
  );
};

test('Muestra la interfaz para el conductor sin checkboxes de reserva', async () => {
  server.use(
    http.get('*/api/viajes/madrid-barcelona-padre', () => {
      return HttpResponse.json({ ...mockPadre, viajesRecurrentes: mockInstancias });
    })
  );

  renderConRuta({ rol: 'CONDUCTOR' });

  expect(await screen.findByText('Vista de Conductor')).toBeInTheDocument();
  expect(screen.getAllByText('Eres el conductor').length).toBeGreaterThan(0);
  
  // Verificamos que no existan checkboxes de selección de reserva
  expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: /Configurar y Pagar/i })).not.toBeInTheDocument();
});

test('El pasajero selecciona todos los viajes y abre el modal de reserva', async () => {
  server.use(
    http.get('*/api/viajes/madrid-barcelona-padre', () => {
      return HttpResponse.json({ ...mockPadre, viajesRecurrentes: mockInstancias });
    })
  );

  renderConRuta({ rol: 'PASAJERO' });

  // Esperar a la carga
  expect(await screen.findByText('Madrid → Barcelona')).toBeInTheDocument();

  // Seleccionar la opción de "Seleccionar todo"
  const checkboxTodos = screen.getByLabelText(/Seleccionar todo/i);
  fireEvent.click(checkboxTodos);

  // Aparece la barra flotante con el total (3 viajes x 20€ = 60€)
  const btnConfigurar = screen.getByRole('button', { name: /Configurar y Pagar \(3\)/i });
  expect(btnConfigurar).toBeInTheDocument();

  // Abrir modal
  fireEvent.click(btnConfigurar);
  expect(screen.getByText('Configurar Reservas Múltiples')).toBeInTheDocument();
});

test('Inicia el proceso de reserva en lote y muestra el formulario de Stripe', async () => {
  let loteCalled = false;

  server.use(
    http.get('*/api/viajes/madrid-barcelona-padre', () => {
      return HttpResponse.json({ ...mockPadre, viajesRecurrentes: mockInstancias });
    }),
    http.post('*/api/reservas/crear-lote', async ({ request }) => {
      const body = await request.json() as any;
      if (body.viajeId === 100 && body.viajeRecurrenteIds.length === 2) {
        loteCalled = true;
        return HttpResponse.json({ clientSecret: 'pi_test_lote_secret_123' });
      }
      return new HttpResponse(null, { status: 400 });
    })
  );

  renderConRuta({ rol: 'PASAJERO' });

  expect(await screen.findByText('Madrid → Barcelona')).toBeInTheDocument();

  // Seleccionar todo
  fireEvent.click(screen.getByLabelText(/Seleccionar todo/i));
  fireEvent.click(screen.getByRole('button', { name: /Configurar y Pagar/i }));

  // Aceptar términos en el modal
  const checkboxTerminos = screen.getByRole('checkbox', { name: /Acepto el cargo total/i });
  fireEvent.click(checkboxTerminos);

  // Clic en Pagar
  const btnPagar = screen.getByRole('button', { name: /Pagar 60.00€/i });
  fireEvent.click(btnPagar);

  await waitFor(() => {
    expect(loteCalled).toBe(true);
    expect(screen.getByTestId('checkout-form')).toBeInTheDocument();
  });
});

test('Completa el pago del lote con éxito y navega de regreso', async () => {
  server.use(
    http.get('*/api/viajes/madrid-barcelona-padre', () => {
      return HttpResponse.json({ ...mockPadre, viajesRecurrentes: mockInstancias });
    }),
    http.post('*/api/reservas/crear-lote', () => {
      return HttpResponse.json({ clientSecret: 'pi_test_lote_secret_123' });
    })
  );

  renderConRuta({ rol: 'PASAJERO' });

  await screen.findByText('Madrid → Barcelona');
  fireEvent.click(screen.getByLabelText(/Seleccionar todo/i));
  fireEvent.click(screen.getByRole('button', { name: /Configurar y Pagar/i }));
  fireEvent.click(screen.getByRole('checkbox', { name: /Acepto el cargo total/i }));
  fireEvent.click(screen.getByRole('button', { name: /Pagar 60.00€/i }));

  // Esperar a Stripe
  await screen.findByTestId('checkout-form');
  fireEvent.click(screen.getByTestId('btn-simular-pago-exitoso'));

  await waitFor(() => {
    expect(screen.getByText(/✅ ¡Reservas y pagos completados con éxito!/i)).toBeInTheDocument();
  });

  // Verificar navegación tras timeout
  await waitFor(() => {
    expect(mockNavigate).toHaveBeenCalledWith('/viajes/madrid-barcelona-padre', expect.objectContaining({
      state: expect.objectContaining({
        mensajeExito: '¡Reservas y pagos completados con éxito!',
      }),
    }));
  }, { timeout: 3000 });
});

test('Muestra un mensaje de error si falla la creación de la reserva en lote', async () => {
  server.use(
    http.get('*/api/viajes/madrid-barcelona-padre', () => {
      return HttpResponse.json({ ...mockPadre, viajesRecurrentes: mockInstancias });
    }),
    http.post('*/api/reservas/crear-lote', () => {
      return new HttpResponse(JSON.stringify({ message: 'Sin plazas suficientes en uno de los viajes' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' },
      });
    })
  );

  renderConRuta({ rol: 'PASAJERO' });

  await screen.findByText('Madrid → Barcelona');
  fireEvent.click(screen.getByLabelText(/Seleccionar todo/i));
  fireEvent.click(screen.getByRole('button', { name: /Configurar y Pagar/i }));
  fireEvent.click(screen.getByRole('checkbox', { name: /Acepto el cargo total/i }));
  fireEvent.click(screen.getByRole('button', { name: /Pagar 60.00€/i }));

  await waitFor(() => {
    expect(screen.getAllByText(/❌ Sin plazas suficientes en uno de los viajes/i)[0]).toBeInTheDocument();
  });
});

test('Muestra un mensaje de error si el pago con Stripe es rechazado', async () => {
  server.use(
    http.get('*/api/viajes/madrid-barcelona-padre', () => {
      return HttpResponse.json({ ...mockPadre, viajesRecurrentes: mockInstancias });
    }),
    http.post('*/api/reservas/crear-lote', () => {
      return HttpResponse.json({ clientSecret: 'pi_test_lote_secret_123' });
    })
  );

  renderConRuta({ rol: 'PASAJERO' });

  await screen.findByText('Madrid → Barcelona');
  fireEvent.click(screen.getByLabelText(/Seleccionar todo/i));
  fireEvent.click(screen.getByRole('button', { name: /Configurar y Pagar/i }));
  fireEvent.click(screen.getByRole('checkbox', { name: /Acepto el cargo total/i }));
  fireEvent.click(screen.getByRole('button', { name: /Pagar 60.00€/i }));

  await screen.findByTestId('checkout-form');
  fireEvent.click(screen.getByTestId('btn-simular-pago-fallido'));

  await waitFor(() => {
    expect(screen.getAllByText(/❌ Error en el procesamiento del pago/i)[0]).toBeInTheDocument();
  });
});