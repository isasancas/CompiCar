import { http, HttpResponse } from 'msw';

export const handlers = [
  // 1. Mock para el servicio de trazado de rutas OSRM
  http.get('https://router.project-osrm.org/*', () => {
    return HttpResponse.json({
      routes: [
        {
          geometry: {
            type: 'LineString',
            coordinates: [
              [-3.70379, 40.416775],
              [2.1734, 41.3851],
            ],
          },
          distance: 600000,
          duration: 21600,
        },
      ],
    });
  }),

  // 2. Mock para geolocalización de OpenStreetMap (Nominatim)
  http.get('https://nominatim.openstreetmap.org/*', () => {
    return HttpResponse.json([
      { lat: '40.416775', lon: '-3.703790', display_name: 'Madrid, España' }
    ]);
  }),

  // 3. Mock por defecto para reservas del usuario
  http.get('*/api/reservas/mis-reservas', () => {
    return HttpResponse.json([]);
  }),

  // 4. Mock por defecto para notificaciones del conductor (evita AbortError en Navbar)
  http.get('*/api/reservas/pendientes-conductor', () => {
    return HttpResponse.json([]);
  }),

  // 5. Autenticación de usuarios
  http.post('*/api/login', async ({ request }) => {
    const body = (await request.clone().json()) as { email?: string; contrasena?: string };

    if (body.email === 'usuario@test.com' && body.contrasena === 'Password123') {
      return HttpResponse.json({ token: 'mocked-jwt-token-123' }, { status: 200 });
    }

    if (body.email === 'error@test.com') {
      return HttpResponse.json({ error: 'La contraseña es incorrecta' }, { status: 400 });
    }

    return HttpResponse.json({ error: 'Error en el inicio de sesión' }, { status: 401 });
  }),
];