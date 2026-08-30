import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../../setupTests';
import DetalleViaje from '../viajes/DetalleViaje';

// Mock de React Leaflet para evitar fallos de renderizado en JSDOM
vi.mock('react-leaflet', () => ({
  MapContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  TileLayer: () => <div>TileLayer</div>,
  CircleMarker: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Polyline: () => <div>Polyline</div>,
  Tooltip: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

// Mock de Stripe
vi.mock('@stripe/stripe-js', () => ({
  loadStripe: vi.fn(() => Promise.resolve({})),
}));

vi.mock('@stripe/react-stripe-js', () => ({
  Elements: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// Datos de prueba reutilizables
const mockViajeBase = {
  id: 1,
  slug: 'madrid-barcelona-123',
  fechaHoraSalida: new Date(Date.now() + 86400000).toISOString(),
  estado: 'PENDIENTE',
  plazasDisponibles: 3,
  precio: 20,
  vehiculo: { marca: 'Toyota', modelo: 'Corolla', matricula: '1234ABC' },
  paradas: [
    { id: 10, localizacion: 'Madrid', tipo: 'ORIGEN', orden: 1 },
    { id: 11, localizacion: 'Barcelona', tipo: 'DESTINO', orden: 2 }
  ],
  reservas: []
};

const mockReservaPasajero = {
  id: 99,
  estado: 'CONFIRMADA',
  viajeId: 1,
  personaId: 5,
  paradaSubidaId: 10,
  paradaBajadaId: 11,
  cantidadPlazas: 1,
  nombrePasajero: 'Juan Pérez'
};

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.setItem('token', 'fake-jwt-token');
  localStorage.setItem('perfil', JSON.stringify({ id: 5, nombre: 'Juan' }));
});

const renderConRuta = (navState = {}) => {
  return render(
    <MemoryRouter initialEntries={[{ pathname: '/viajes/madrid-barcelona-123', state: navState }]}>
      <Routes>
        <Route path="/viajes/:slug" element={<DetalleViaje />} />
      </Routes>
    </MemoryRouter>
  );
};

test('El conductor cancela exitosamente un viaje individual', async () => {
  let cancelCalled = false;

  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(mockViajeBase);
    }),
    http.put('*/api/viajes/madrid-barcelona-123/cancelar', () => {
      cancelCalled = true;
      return HttpResponse.json({ ...mockViajeBase, estado: 'CANCELADO' });
    })
  );

  renderConRuta({ rol: 'conductor' });

  expect(await screen.findByText('Toyota Corolla')).toBeInTheDocument();

  const btnCancelar = screen.getByRole('button', { name: /cancelar viaje/i });
  fireEvent.click(btnCancelar);

  // Ajustado al texto exacto del botón dentro de modalCancelarViajeAbierto ("Sí, cancelar viaje")
  const btnConfirmar = await screen.findByRole('button', { name: /sí, cancelar viaje/i });
  fireEvent.click(btnConfirmar);

  await waitFor(() => {
    expect(cancelCalled).toBe(true);
    expect(screen.getByText(/✅ Viaje cancelado correctamente/i)).toBeInTheDocument();
  });
});

test('El conductor cancela la serie completa de viajes recurrentes', async () => {
  let cancelConjuntoCalled = false;

  const viajeRecurrentePadre = {
    ...mockViajeBase,
    fechaFinRecurrencia: '2026-12-31T23:59:59Z',
    diasSemana: ['LUNES', 'MIERCOLES']
  };

  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(viajeRecurrentePadre);
    }),
    http.put('*/api/viajes/madrid-barcelona-123/cancelar-conjunto', () => {
      cancelConjuntoCalled = true;
      return HttpResponse.json({ ...viajeRecurrentePadre, estado: 'CANCELADO' });
    })
  );

  renderConRuta({ rol: 'conductor' });

  expect(await screen.findByText(/Configuración de Viaje Recurrente/i)).toBeInTheDocument();

  const btnCancelarRecurrente = screen.getByRole('button', { name: /cancelar viaje/i });
  fireEvent.click(btnCancelarRecurrente);

  const btnConfirmarConjunto = await screen.findByRole('button', { name: /cancelar toda la serie/i });
  fireEvent.click(btnConfirmarConjunto);

  await waitFor(() => {
    expect(cancelConjuntoCalled).toBe(true);
    expect(screen.getByText(/✅ Viajes cancelados en conjunto correctamente/i)).toBeInTheDocument();
  });
});

test('El pasajero cancela su reserva activa correctamente', async () => {
  let cancelReservaCalled = false;

  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(mockViajeBase);
    }),
    http.get('*/api/reservas/mis-reservas', () => {
      return HttpResponse.json([mockReservaPasajero]);
    }),
    http.put('*/api/reservas/cancelar', ({ request }) => {
      const url = new URL(request.url);
      if (url.searchParams.get('reservaId') === '99') {
        cancelReservaCalled = true;
        return HttpResponse.json({ ...mockReservaPasajero, estado: 'CANCELADA' });
      }
      return new HttpResponse(null, { status: 400 });
    })
  );

  renderConRuta({ rol: 'pasajero' });

  const btnCancelarReserva = await screen.findByRole('button', { name: /cancelar mi reserva/i });
  fireEvent.click(btnCancelarReserva);

  // Ajustado al texto exacto del botón en modalCancelarReservaAbierto ("Confirmar cancelación")
  const btnConfirmarModal = await screen.findByRole('button', { name: /confirmar cancelación/i });
  fireEvent.click(btnConfirmarModal);

  await waitFor(() => {
    expect(cancelReservaCalled).toBe(true);
    expect(screen.getByText(/✅ Reserva cancelada correctamente/i)).toBeInTheDocument();
  });
});

test('El pasajero reporta incomparecencia del conductor si este no se presenta', async () => {
  let incomparecenciaCalled = false;

  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(mockViajeBase);
    }),
    http.get('*/api/reservas/mis-reservas', () => {
      return HttpResponse.json([mockReservaPasajero]);
    }),
    http.put('*/api/viajes/madrid-barcelona-123/cancelarIncompareceConductor', () => {
      incomparecenciaCalled = true;
      return HttpResponse.json({ ...mockViajeBase, estado: 'CANCELADO' });
    })
  );

  renderConRuta({ rol: 'pasajero' });

  const btnIncomparecencia = await screen.findByRole('button', { name: /el conductor no se ha presentado/i });
  fireEvent.click(btnIncomparecencia);

  await waitFor(() => {
    expect(incomparecenciaCalled).toBe(true);
    expect(screen.getByText(/✅ Incomparecencia reportada correctamente/i)).toBeInTheDocument();
  });
});