import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import Notificaciones from '../notificacion/Notificaciones';

const server = setupServer(
  http.get('*/api/reservas/pendientes-conductor', () => {
    return HttpResponse.json([
      {
        id: 1,
        cantidadPlazas: 2,
        persona: {
          nombre: 'Ana Gómez',
          slug: 'ana-gomez'
        },
        viaje: {
          fechaHoraSalida: '2026-06-10T10:00:00Z'
        }
      }
    ]);
  }),

  http.get('*/api/notificaciones/mis-notificaciones', () => {
    return HttpResponse.json([
      {
        id: 101,
        tipo: 'VIAJE_CANCELADO',
        mensaje: 'El viaje a Madrid ha sido cancelado por el conductor.',
        fechaCreacion: new Date().toISOString(),
        leida: false
      }
    ]);
  }),

  http.put('*/api/reservas/confirmar', () => {
    return new HttpResponse(null, { status: 200 });
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const renderComponent = () => {
  render(
    <MemoryRouter>
      <Notificaciones />
    </MemoryRouter>
  );
};

test('Carga y muestra correctamente las solicitudes de reserva y avisos recientes', async () => {
  renderComponent();

  expect(screen.getByText(/cargando bandeja.../i)).toBeInTheDocument();

  expect(await screen.findByText(/ana gómez quiere viajar contigo/i)).toBeInTheDocument();
  expect(screen.getByText(/reserva para 2 plazas/i)).toBeInTheDocument();
  expect(screen.getByText(/viaje cancelado/i)).toBeInTheDocument();
  expect(screen.getByText(/el viaje a madrid ha sido cancelado/i)).toBeInTheDocument();
});

test('Permite aceptar una solicitud de reserva correctamente', async () => {
  const user = userEvent.setup();
  renderComponent();

  const botonAceptar = await screen.findByRole('button', { name: /^aceptar$/i });
  await user.click(botonAceptar);

  await waitFor(() => {
    expect(screen.queryByText(/ana gómez quiere viajar contigo/i)).not.toBeInTheDocument();
  });
  
  expect(screen.getByText(/no hay solicitudes pendientes/i)).toBeInTheDocument();
});