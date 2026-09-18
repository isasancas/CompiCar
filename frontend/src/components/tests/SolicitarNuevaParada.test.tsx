import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { vi } from 'vitest';
import { server } from '../../setupTests';
import SolicitarNuevaParada from '../viajes/SolicitarNuevaParada';

// Mock de react-leaflet para evitar errores de renderizado en el entorno de pruebas
vi.mock('react-leaflet', () => ({
  MapContainer: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="map-container">{children}</div>
  ),
  TileLayer: () => null,
  Polyline: () => null,
  CircleMarker: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="circle-marker">{children}</div>
  ),
  Tooltip: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  useMapEvents: () => null,
}));

const renderConRuta = (slug = 'madrid-barcelona-123') => {
  return render(
    <MemoryRouter initialEntries={[`/viajes/${slug}/solicitar-parada`]}>
      <Routes>
        <Route
          path="/viajes/:slug/solicitar-parada"
          element={<SolicitarNuevaParada />}
        />
        <Route path="/viajes/:slug" element={<div>Página Detalle Viaje</div>} />
      </Routes>
    </MemoryRouter>
  );
};

const mockViaje = {
id: 1,
slug: 'madrid-barcelona-123',
fechaHoraSalida: '2026-10-01T08:00:00.000Z',
paradas: [
    { id: 10, localizacion: 'Madrid', tipo: 'ORIGEN', orden: 1 },
    { id: 11, localizacion: 'Barcelona', tipo: 'DESTINO', orden: 2 },
],
};

const mockReservaConfirmada = {
id: 99,
viajeId: 1,
estado: 'CONFIRMADA',
};

beforeEach(() => {
localStorage.setItem('token', 'fake-jwt-token');
});

afterEach(() => {
localStorage.clear();
});

test('muestra el indicador de carga inicial', () => {
server.use(
    http.get('*/api/viajes/publicos/*', () => new Promise(() => {})),
    http.get('*/api/reservas/mis-reservas', () => new Promise(() => {}))
);

renderConRuta();

expect(screen.getByText('Cargando solicitud...')).toBeInTheDocument();
});

test('muestra error si el usuario no tiene una reserva confirmada para el viaje', async () => {
server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
    return HttpResponse.json(mockViaje);
    }),
    http.get('*/api/reservas/mis-reservas', () => {
    return HttpResponse.json([]);
    })
);

renderConRuta();

expect(
    await screen.findByText('Necesitas una reserva confirmada para solicitar una parada')
).toBeInTheDocument();
expect(screen.getByRole('button', { name: /volver/i })).toBeInTheDocument();
});

test('carga correctamente la interfaz cuando el usuario tiene una reserva confirmada', async () => {
server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
    return HttpResponse.json(mockViaje);
    }),
    http.get('*/api/reservas/mis-reservas', () => {
    return HttpResponse.json([mockReservaConfirmada]);
    }),
    http.get('*/api/paradas/solicitud-pendiente*', () => {
    return HttpResponse.json(false);
    }),
    http.get('https://nominatim.openstreetmap.org/*', () => {
    return HttpResponse.json([{ lat: '40.4168', lon: '-3.7038' }]);
    }),
    http.get('https://router.project-osrm.org/*', () => {
    return HttpResponse.json({
        routes: [{ geometry: { coordinates: [[-3.7038, 40.4168], [2.1734, 41.3851]] } }],
    });
    })
);

renderConRuta();

expect(await screen.findByRole('heading', { name: /solicitar una nueva parada/i })).toBeInTheDocument();
expect(screen.getByLabelText(/localización de la nueva parada/i)).toBeInTheDocument();
expect(screen.getByRole('button', { name: /ver en mapa/i })).toBeInTheDocument();
expect(screen.getByRole('button', { name: /confirmar solicitud/i })).toBeInTheDocument();
});

test('muestra error si la ubicación buscada está a más de 500 metros de la ruta', async () => {
server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
    return HttpResponse.json(mockViaje);
    }),
    http.get('*/api/reservas/mis-reservas', () => {
    return HttpResponse.json([mockReservaConfirmada]);
    }),
    http.get('*/api/paradas/solicitud-pendiente*', () => {
    return HttpResponse.json(false);
    }),
    http.get('https://nominatim.openstreetmap.org/search*', ({ request }) => {
    const url = new URL(request.url);
    const query = url.searchParams.get('q');
    if (query === 'Sevilla') {
        // Coordenadas muy alejadas de la ruta Madrid-Barcelona
        return HttpResponse.json([{ lat: '37.3891', lon: '-5.9845' }]);
    }
    return HttpResponse.json([{ lat: '40.4168', lon: '-3.7038' }]);
    }),
    http.get('https://router.project-osrm.org/*', () => {
    return HttpResponse.json({
        routes: [{ geometry: { coordinates: [[-3.7038, 40.4168], [2.1734, 41.3851]] } }],
    });
    })
);

renderConRuta();

const inputLocalizacion = await screen.findByLabelText(/localización de la nueva parada/i);
fireEvent.change(inputLocalizacion, { target: { value: 'Sevilla' } });

const btnVerEnMapa = screen.getByRole('button', { name: /ver en mapa/i });
fireEvent.click(btnVerEnMapa);

expect(
    await screen.findByText('La localización debe estar sobre la ruta del viaje o a menos de 500 metros de ella.')
).toBeInTheDocument();
});

test('permite solicitar una parada en una ubicación válida y redirige al confirmar', async () => {
let solicitudEnviada = false;
let payloadEnviado: any = null;

server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
    return HttpResponse.json(mockViaje);
    }),
    http.get('*/api/reservas/mis-reservas', () => {
    return HttpResponse.json([mockReservaConfirmada]);
    }),
    http.get('*/api/paradas/solicitud-pendiente*', () => {
    return HttpResponse.json(false);
    }),
    http.get('https://nominatim.openstreetmap.org/search*', ({ request }) => {
    const url = new URL(request.url);
    const query = url.searchParams.get('q');
    if (query === 'Alcalá de Henares') {
        // Punto sobre la ruta Madrid (40.4168, -3.7038)
        return HttpResponse.json([{ lat: '40.4170', lon: '-3.7035' }]);
    }
    return HttpResponse.json([{ lat: '40.4168', lon: '-3.7038' }]);
    }),
    http.get('https://router.project-osrm.org/*', () => {
    return HttpResponse.json({
        routes: [{ geometry: { coordinates: [[-3.7038, 40.4168], [2.1734, 41.3851]] } }],
    });
    }),
    http.post('*/api/paradas/solicitud-nueva-parada', async ({ request }) => {
    solicitudEnviada = true;
    payloadEnviado = await request.json();
    return HttpResponse.json({ status: 'ok' });
    })
);

renderConRuta();

const inputLocalizacion = await screen.findByLabelText(/localización de la nueva parada/i);
fireEvent.change(inputLocalizacion, { target: { value: 'Alcalá de Henares' } });

fireEvent.click(screen.getByRole('button', { name: /ver en mapa/i }));

// Esperar a que la localización quede fijada en mapa
await waitFor(() => {
    expect(screen.getByRole('button', { name: /ver en mapa/i })).not.toBeDisabled();
});

// Abrir modal de confirmación
fireEvent.click(screen.getByRole('button', { name: /confirmar solicitud/i }));

// Modal abierto
expect(await screen.findByRole('heading', { name: /confirmar nueva parada/i })).toBeInTheDocument();

// Enviar solicitud desde el modal
fireEvent.click(screen.getByRole('button', { name: /enviar solicitud para este viaje/i }));

await waitFor(() => {
    expect(solicitudEnviada).toBe(true);
    expect(payloadEnviado).toMatchObject({
    reservaId: 99,
    localizacion: 'Alcalá de Henares',
    todaRecurrencia: false,
    });
    // Redirección al detalle
    expect(screen.getByText('Página Detalle Viaje')).toBeInTheDocument();
});
});

test('muestra las opciones de recurrencia cuando el viaje tiene viajes recurrentes', async () => {
const viajeRecurrente = {
    ...mockViaje,
    viajesRecurrentes: [{ id: 2, slug: 'madrid-barcelona-456' }],
};

server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
    return HttpResponse.json(viajeRecurrente);
    }),
    http.get('*/api/reservas/mis-reservas', () => {
    return HttpResponse.json([mockReservaConfirmada]);
    }),
    http.get('*/api/paradas/solicitud-pendiente*', () => {
    return HttpResponse.json(false);
    }),
    http.get('https://nominatim.openstreetmap.org/search*', () => {
    return HttpResponse.json([{ lat: '40.4170', lon: '-3.7035' }]);
    }),
    http.get('https://router.project-osrm.org/*', () => {
    return HttpResponse.json({
        routes: [{ geometry: { coordinates: [[-3.7038, 40.4168], [2.1734, 41.3851]] } }],
    });
    })
);

renderConRuta();

const inputLocalizacion = await screen.findByLabelText(/localización de la nueva parada/i);
fireEvent.change(inputLocalizacion, { target: { value: 'Guadalajara' } });

fireEvent.click(screen.getByRole('button', { name: /ver en mapa/i }));

await waitFor(() => {
    expect(screen.getByRole('button', { name: /ver en mapa/i })).not.toBeDisabled();
});

fireEvent.click(screen.getByRole('button', { name: /confirmar solicitud/i }));

// Validar opciones de recurrencia en el modal
expect(await screen.findByText('Solo este viaje')).toBeInTheDocument();
expect(screen.getByText('Toda la recurrencia')).toBeInTheDocument();
});

test('muestra aviso si ya existe una solicitud pendiente de aprobación', async () => {
server.use(
    http.get('*/api/viajes/publicos/madrid-barcelona-123', () => {
    return HttpResponse.json(mockViaje);
    }),
    http.get('*/api/reservas/mis-reservas', () => {
    return HttpResponse.json([mockReservaConfirmada]);
    }),
    http.get('*/api/paradas/solicitud-pendiente*', () => {
    return HttpResponse.json(true);
    }),
    http.get('https://nominatim.openstreetmap.org/*', () => {
    return HttpResponse.json([{ lat: '40.4168', lon: '-3.7038' }]);
    }),
    http.get('https://router.project-osrm.org/*', () => {
    return HttpResponse.json({
        routes: [{ geometry: { coordinates: [[-3.7038, 40.4168], [2.1734, 41.3851]] } }],
    });
    })
);

renderConRuta();

expect(
    await screen.findByText(/ya tienes una solicitud de parada pendiente para este viaje/i)
).toBeInTheDocument();
expect(screen.getByRole('button', { name: /confirmar solicitud/i })).toBeDisabled();
});