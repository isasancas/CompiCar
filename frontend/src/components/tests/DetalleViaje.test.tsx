import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '../../setupTests';
import DetalleViaje from '../viajes/DetalleViaje';
import userEvent from '@testing-library/user-event';

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

vi.mock('../pagos/CheckoutForm', () => ({
  default: ({ onSuccess, onError, monto }: { onSuccess: (id: string) => void; onError: (msg: string) => void; monto: number }) => (
    <div data-testid="checkout-form">
      <span>Monto: {monto}€</span>
      <button 
        type="button" 
        data-testid="btn-simular-pago-exitoso" 
        onClick={() => onSuccess('pi_mock_123')}
      >
        Simular Pago Exitoso
      </button>
      <button 
        type="button" 
        data-testid="btn-simular-pago-fallido" 
        onClick={() => onError('Tarjeta rechazada')}
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

const mockViajeRecurrentePadre = {
  ...mockViajeBase,
  id: 10,
  slug: 'madrid-valencia-recurrente',
  diasSemana: ['LUNES', 'MIERCOLES', 'VIERNES'],
  fechaFinRecurrencia: '2026-12-31T23:59:59Z',
  viajesRecurrentes: [
    { id: 11, slug: 'madrid-valencia-1', fechaHoraSalida: new Date(Date.now() + 86400000).toISOString(), estado: 'PENDIENTE', plazasDisponibles: 3, precio: 20, vehiculo: mockViajeBase.vehiculo, paradas: mockViajeBase.paradas },
    { id: 12, slug: 'madrid-valencia-2', fechaHoraSalida: new Date(Date.now() + 172800000).toISOString(), estado: 'PENDIENTE', plazasDisponibles: 3, precio: 20, vehiculo: mockViajeBase.vehiculo, paradas: mockViajeBase.paradas }
  ]
};

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.setItem('token', 'fake-jwt-token');
  localStorage.setItem('perfil', JSON.stringify({ id: 5, nombre: 'Juan' }));
});

afterEach(async () => {
  if (typeof window !== 'undefined' && 'happyDOM' in window) {
    await (window as any).happyDOM.whenAsyncComplete();
  }
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

const mockViajeIniciado = {
  ...mockViajeBase,
  conductorId: 5,
  estado: 'INICIADO',
  reservas: [
    {
      ...mockReservaPasajero,
      id: 99,
      personaId: 10,
      estado: 'CONFIRMADA'
    }
  ]
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

test('Inicia el proceso de pago e integra el formulario de Stripe', async () => {
  let crearReservaCalled = false;

  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(mockViajeBase);
    }),
    http.get('*/api/reservas/mis-reservas', () => {
      return HttpResponse.json([]);
    }),
    http.post('*/api/reservas/crear', () => {
      crearReservaCalled = true;
      return HttpResponse.json({ clientSecret: 'pi_test_secret_123', reservaId: 505 });
    })
  );

  renderConRuta({ rol: 'pasajero' });

  expect(await screen.findByText('Toyota Corolla')).toBeInTheDocument();

  const btnReservar = screen.getByRole('button', { name: /reservar ahora/i });
  fireEvent.click(btnReservar);

  const checkboxAviso = screen.getByRole('checkbox');
  fireEvent.click(checkboxAviso);

  const btnPagar = screen.getByRole('button', { name: /pagar 20.00€ y reservar/i });
  fireEvent.click(btnPagar);

  await waitFor(() => {
    expect(crearReservaCalled).toBe(true);
    expect(screen.getByTestId('checkout-form')).toBeInTheDocument();
  });
});

test('El pasajero completa el pago con éxito a través de Stripe', async () => {
  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(mockViajeBase);
    }),
    http.get('*/api/reservas/mis-reservas', () => {
      return HttpResponse.json([]);
    }),
    http.post('*/api/reservas/crear', () => {
      return HttpResponse.json({ clientSecret: 'pi_test_secret_123', reservaId: 505 });
    })
  );

  renderConRuta({ rol: 'pasajero' });

  await screen.findByText('Toyota Corolla');
  fireEvent.click(screen.getByRole('button', { name: /reservar ahora/i }));
  fireEvent.click(screen.getByRole('checkbox'));
  fireEvent.click(screen.getByRole('button', { name: /pagar 20.00€ y reservar/i }));

  await screen.findByTestId('checkout-form');
  fireEvent.click(screen.getByTestId('btn-simular-pago-exitoso'));

  await waitFor(() => {
    expect(screen.getByText(/✅ Pago confirmado. ¡Tu plaza está reservada!/i)).toBeInTheDocument();
  });
});

test('El sistema anula la reserva provisional si el pago falla', async () => {
  let anularPagoCalled = false;

  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(mockViajeBase);
    }),
    http.get('*/api/reservas/mis-reservas', () => {
      return HttpResponse.json([]);
    }),
    http.post('*/api/reservas/crear', () => {
      return HttpResponse.json({ clientSecret: 'pi_test_secret_123', reservaId: 505 });
    }),
    http.put('*/api/reservas/anular-pago-fallido', ({ request }) => {
      const url = new URL(request.url);
      if (url.searchParams.get('reservaId') === '505') {
        anularPagoCalled = true;
        return HttpResponse.json({ message: 'Reserva anulada' });
      }
      return new HttpResponse(null, { status: 400 });
    })
  );

  renderConRuta({ rol: 'pasajero' });

  await screen.findByText('Toyota Corolla');
  fireEvent.click(screen.getByRole('button', { name: /reservar ahora/i }));
  fireEvent.click(screen.getByRole('checkbox'));
  fireEvent.click(screen.getByRole('button', { name: /pagar 20.00€ y reservar/i }));

  await screen.findByTestId('checkout-form');
  fireEvent.click(screen.getByTestId('btn-simular-pago-fallido'));

  await waitFor(() => {
    expect(anularPagoCalled).toBe(true);
    expect(screen.getByText(/❌ Tarjeta rechazada/i)).toBeInTheDocument();
  });
});

test('El conductor marca como PRESENTE a un pasajero mediante su código', async () => {
  let endpointLlamado = false;

  const mockViajeIniciadoConCheckin = {
    ...mockViajeIniciado,
    estado: 'INICIADO',
    checkin: 'CODIGO123',
    reservas: [
      {
        ...mockReservaPasajero,
        id: 99,
        estado: 'PENDIENTE',
      },
    ],
  };

  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(mockViajeIniciadoConCheckin);
    }),
    http.put('*/api/reservas/presentado', ({ request }) => {
      const url = new URL(request.url);
      if (url.searchParams.get('reservaId') === '99') {
        endpointLlamado = true;
        return HttpResponse.json({ ...mockReservaPasajero, estado: 'PRESENTE' });
      }
      return new HttpResponse(null, { status: 400 });
    })
  );

  renderConRuta({ rol: 'conductor' });

  const btnPresente = await screen.findByRole('button', { name: /^Presente$/i });
  fireEvent.click(btnPresente);

  const inputCodigo = screen.getByPlaceholderText(/Introduce el código/i);
  fireEvent.change(inputCodigo, { target: { value: 'CODIGO123' } });

  const btnAceptar = screen.getByRole('button', { name: /^Aceptar$/i });
  fireEvent.click(btnAceptar);

  await screen.findByText(/^Presente$/i, { selector: 'span' });
  expect(endpointLlamado).toBe(true);
});

test('El conductor realiza el check-in global cuando el pasajero ya está PRESENTE', async () => {
  let endpointGlobalLlamado = false;

  const mockViajeListo = {
    ...mockViajeIniciado,
    estado: 'INICIADO',
    checkin: 'CODIGO123',
    reservas: [
      {
        ...mockReservaPasajero,
        id: 99,
        estado: 'PRESENTE',
      },
    ],
  };

  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(mockViajeListo);
    }),
    http.put('*/api/viajes/madrid-barcelona-123/checkin', () => {
      endpointGlobalLlamado = true;
      return HttpResponse.json({ ...mockViajeListo, estado: 'EN_CURSO' });
    })
  );

  renderConRuta({ rol: 'conductor' });

  const btnAbrirGlobal = await screen.findByRole('button', { name: /Realizar check-in global/i });
  fireEvent.click(btnAbrirGlobal);

  const inputCodigo = screen.getByPlaceholderText(/Introduce el código/i);
  fireEvent.change(inputCodigo, { target: { value: 'CODIGO123' } });

  const btnConfirmar = screen.getByRole('button', { name: /Confirmar check-in/i });
  fireEvent.click(btnConfirmar);

  await waitFor(() => {
    expect(screen.queryByPlaceholderText(/Introduce el código/i)).not.toBeInTheDocument();
    expect(endpointGlobalLlamado).toBe(true);
  });
});

test('El conductor marca como no presentado a un pasajero ausente', async () => {
  let noPresentadoCalled = false;

  const viajeMock = {
    ...mockViajeIniciado,
    estado: 'INICIADO',
    reservas: [
      {
        ...mockReservaPasajero,
        id: 99,
        estado: 'CONFIRMADA',
      },
    ],
  };

  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(viajeMock);
    }),
    http.put('*/api/reservas/noPresentado', ({ request }) => {
      const url = new URL(request.url);
      if (url.searchParams.get('reservaId') === '99') {
        noPresentadoCalled = true;
        viajeMock.reservas[0].estado = 'NO_PRESENTADO';
        return HttpResponse.json({ ...mockReservaPasajero, estado: 'NO_PRESENTADO' });
      }
      return new HttpResponse(null, { status: 400 });
    })
  );

  renderConRuta({ rol: 'conductor' });

  const btnNoPresentado = await screen.findByRole('button', { name: /^No presentado$/i });
  fireEvent.click(btnNoPresentado);

  await screen.findByText(/^No presentado$/i, { selector: 'span' });

  expect(noPresentadoCalled).toBe(true);
  expect(screen.queryByRole('button', { name: /^No presentado$/i })).not.toBeInTheDocument();
});

test('Muestra un mensaje de error si falla la llamada de check-in global', async () => {
  const mockViajeConPasajeroPresente = {
    ...mockViajeIniciado,
    estado: 'INICIADO',
    checkin: 'CODIGO123',
    reservas: [
      {
        ...mockReservaPasajero,
        id: 99,
        estado: 'PRESENTE',
      },
    ],
  };

  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(mockViajeConPasajeroPresente);
    }),
    http.put('*/api/viajes/madrid-barcelona-123/checkin', () => {
      return new HttpResponse(JSON.stringify({ message: 'Error al procesar check-in' }), {
        status: 500,
        headers: { 'Content-Type': 'application/json' },
      });
    })
  );

  renderConRuta({ rol: 'conductor' });

  const btnAbrirModal = await screen.findByRole('button', { name: /Realizar check-in global/i });
  fireEvent.click(btnAbrirModal);

  const inputCodigo = screen.getByPlaceholderText(/Introduce el código/i);
  fireEvent.change(inputCodigo, { target: { value: 'CODIGO123' } });

  const btnConfirmar = screen.getByRole('button', { name: /confirmar check-in/i });
  fireEvent.click(btnConfirmar);

  await waitFor(() => {
    expect(screen.getByText(/Error al procesar check-in/i)).toBeInTheDocument();
  });
});

test('Permite al conductor finalizar un viaje que está en curso para proceder a su cierre y cobro', async () => {
  let finalizarCalled = false;

  const mockViajeEnCurso = {
    ...mockViajeBase,
    estado: 'EN_CURSO',
  };

  const mockViajeFinalizado = {
    ...mockViajeEnCurso,
    estado: 'FINALIZADO',
  };

  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(mockViajeEnCurso);
    }),
    http.put('*/api/viajes/madrid-barcelona-123/finalizar', () => {
      finalizarCalled = true;
      return HttpResponse.json(mockViajeFinalizado);
    })
  );

  renderConRuta({ rol: 'conductor' });

  const botonFinalizar = await screen.findByRole('button', { name: /marcar viaje como finalizado/i });
  expect(botonFinalizar).toBeInTheDocument();

  fireEvent.click(botonFinalizar);

  await waitFor(() => {
    expect(finalizarCalled).toBe(true);
    expect(screen.getAllByText('FINALIZADO').length).toBeGreaterThan(0);
  });
});

test('Inicia el proceso de pago en lote para un viaje recurrente e integra Stripe', async () => {
  let crearLoteCalled = false;

  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(mockViajeRecurrentePadre);
    }),
    http.get('*/api/reservas/mis-reservas', () => {
      return HttpResponse.json([]);
    }),
    http.post('*/api/reservas/crear-lote', async ({ request }) => {
      crearLoteCalled = true;
      const body = await request.json() as any;
      if (body.viajeRecurrenteIds && body.viajeRecurrenteIds.length === 2) {
        return HttpResponse.json({ clientSecret: 'pi_test_secret_lote_123', loteId: 900 });
      }
      return new HttpResponse(null, { status: 400 });
    })
  );

  renderConRuta({ rol: 'pasajero' });

  expect(await screen.findByText('Toyota Corolla')).toBeInTheDocument();

  const btnReservarRecurrente = screen.getByRole('button', { name: /reservar viajes recurrentes/i });
  fireEvent.click(btnReservarRecurrente);

  const checkboxAviso = screen.getByRole('checkbox');
  fireEvent.click(checkboxAviso);

  const btnPagarLote = screen.getByRole('button', { name: /pagar 60.00€ y reservar/i });
  fireEvent.click(btnPagarLote);

  await waitFor(() => {
    expect(crearLoteCalled).toBe(true);
    expect(screen.getByTestId('checkout-form')).toBeInTheDocument();
    expect(screen.getByText(/monto: 40€/i)).toBeInTheDocument();
  });
});

test('El pasajero completa con éxito el pago del lote de viajes recurrentes a través de Stripe', async () => {
  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(mockViajeRecurrentePadre);
    }),
    http.get('*/api/reservas/mis-reservas', () => {
      return HttpResponse.json([]);
    }),
    http.post('*/api/reservas/crear-lote', () => {
      return HttpResponse.json({ clientSecret: 'pi_test_secret_lote_123', loteId: 900 });
    })
  );

  renderConRuta({ rol: 'pasajero' });

  await screen.findByText('Toyota Corolla');
  fireEvent.click(screen.getByRole('button', { name: /reservar viajes recurrentes/i }));
  fireEvent.click(screen.getByRole('checkbox'));
  fireEvent.click(screen.getByRole('button', { name: /pagar 60.00€ y reservar/i }));

  await screen.findByTestId('checkout-form');
  fireEvent.click(screen.getByTestId('btn-simular-pago-exitoso'));

  await waitFor(() => {
    expect(screen.getByText(/✅ Pago confirmado. ¡Tu plaza está reservada!/i)).toBeInTheDocument();
  });
});

test('El conductor inicia el viaje correctamente cuando llega la hora de salida', async () => {
  let iniciarCalled = false;
  const viajePasado = {
    ...mockViajeBase,
    conductorId: 5,
    fechaHoraSalida: new Date(Date.now() - 3600000).toISOString()
  };

  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(viajePasado);
    }),
    http.put('*/api/viajes/madrid-barcelona-123/iniciar', () => {
      iniciarCalled = true;
      return HttpResponse.json({ ...viajePasado, estado: 'INICIADO' });
    })
  );

  renderConRuta({ rol: 'conductor' });

  expect(await screen.findByText('Toyota Corolla')).toBeInTheDocument();

  const btnIniciar = screen.getByRole('button', { name: /iniciar viaje/i });
  expect(btnIniciar).not.toBeDisabled();
  fireEvent.click(btnIniciar);

  await waitFor(() => {
    expect(iniciarCalled).toBe(true);
    expect(screen.getByText(/✅ El viaje ha sido iniciado correctamente/i)).toBeInTheDocument();
  });
});

test('El conductor edita los detalles del viaje (fecha y plazas)', async () => {
  let editarCalled = false;
  const viajeModificable = {
    ...mockViajeBase,
    conductorId: 5,
    fechaHoraSalida: new Date(Date.now() + 86400000 * 2).toISOString()
  };

  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(viajeModificable);
    }),
    http.put('*/api/viajes/madrid-barcelona-123', () => {
      editarCalled = true;
      return HttpResponse.json({ ...viajeModificable, plazasDisponibles: 5 });
    })
  );

  renderConRuta({ rol: 'conductor' });

  expect(await screen.findByText('Toyota Corolla')).toBeInTheDocument();

  const btnEditar = screen.getByRole('button', { name: /editar detalles del viaje/i });
  fireEvent.click(btnEditar);

  const btnConfirmar = screen.getByRole('button', { name: /confirmar cambios/i });
  fireEvent.click(btnConfirmar);

  await waitFor(() => {
    expect(editarCalled).toBe(true);
    expect(screen.getByText(/✅ Viaje actualizado con éxito/i)).toBeInTheDocument();
  });
});

test('El pasajero modifica su reserva existente con éxito', async () => {
  const user = userEvent.setup();
  let actualizarReservaCalled = false;

  const reservaInicial = {
    ...mockReservaPasajero,
    id: 99,
    cantidadPlazas: 1,
    paradaOrigenId: 10,
    paradaDestinoId: 12
  };

  const viajeConMargenValido = {
    ...mockViajeBase,
    plazasDisponibles: 4,
    fechaHoraSalida: new Date(Date.now() + 86400000 * 2).toISOString(),
    paradas: [
      { id: 10, nombre: 'Madrid - Estación Sur' },
      { id: 11, nombre: 'Zaragoza - Delicias' },
      { id: 12, nombre: 'Barcelona - Sants' }
    ],
    reservas: [reservaInicial]
  };

  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(viajeConMargenValido);
    }),
    http.get('*/api/reservas/mis-reservas', () => {
      return HttpResponse.json([reservaInicial]);
    }),
    http.all('*/api/reservas*', () => {
      actualizarReservaCalled = true;
      return HttpResponse.json({ ...reservaInicial, paradaOrigenId: 11 });
    })
  );

  renderConRuta({ rol: 'pasajero' });

  const btnModificar = await screen.findByRole('button', { name: /modificar mi reserva/i });
  await user.click(btnModificar);

  const desplegables = await screen.findAllByRole('combobox');
  await user.selectOptions(desplegables[0], '11');

  const btnGuardar = screen.getByRole('button', { name: /guardar cambios/i });
  
  await waitFor(() => {
    expect(btnGuardar).not.toBeDisabled();
  });

  await user.click(btnGuardar);

  await waitFor(() => {
    expect(actualizarReservaCalled).toBe(true);
  });

  const mensajesExito = await screen.findAllByText(/reserva actualizada|actualizada con éxito/i);
  expect(mensajesExito.length).toBeGreaterThan(0);
});

test('Muestra un error cuando el conductor introduce un código de check-in individual erróneo', async () => {
  const mockViajeConCheckin = {
    ...mockViajeIniciado,
    checkin: 'CODIGO_CORRECTO',
    reservas: [{ ...mockReservaPasajero, id: 99, estado: 'PENDIENTE' }]
  };

  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(mockViajeConCheckin);
    })
  );

  renderConRuta({ rol: 'conductor' });

  const btnPresente = await screen.findByRole('button', { name: /^Presente$/i });
  fireEvent.click(btnPresente);

  const inputCodigo = screen.getByPlaceholderText(/introduce el código/i);
  fireEvent.change(inputCodigo, { target: { value: 'CODIGO_ERRONEO' } });

  const btnAceptar = screen.getByRole('button', { name: /^Aceptar$/i });
  fireEvent.click(btnAceptar);

  await waitFor(() => {
    expect(
      screen.getByText(/❌ El código introducido no coincide con el check-in del viaje/i)
    ).toBeInTheDocument();
  });
});

test('Muestra el aviso de autenticación requerida si un usuario no logueado intenta reservar', async () => {
  localStorage.removeItem('token');

  server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
      return HttpResponse.json(mockViajeBase);
    })
  );

  renderConRuta({ rol: 'pasajero' });

  expect(await screen.findByText('Toyota Corolla')).toBeInTheDocument();

  const btnReservar = screen.getByRole('button', { name: /reservar ahora/i });
  fireEvent.click(btnReservar);

  expect(
    screen.getByText(/Debes iniciar sesión o registrarte para poder reservar un viaje/i)
  ).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /iniciar sesión/i })).toBeInTheDocument();
});