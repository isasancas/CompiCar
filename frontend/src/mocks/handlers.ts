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

  // 3. Reservas del usuario y notificaciones del conductor
  http.get('*/api/reservas/mis-reservas', () => {
    return HttpResponse.json([]);
  }),

  http.get('*/api/reservas/pendientes-conductor', () => {
    return HttpResponse.json([]);
  }),

  // 4. Perfil de usuario (evita peticiones colgadas al montar Perfil/Navbar)
  http.get('*/api/personas/perfil', () => {
    return HttpResponse.json({
      id: 1,
      nombre: 'Usuario',
      primerApellido: 'Test',
      segundoApellido: 'Ejemplo',
      email: 'usuario@test.com',
      telefono: '600000000',
      reputacion: 5,
      fondosActuales: 0,
      fondosTotales: 0,
      numeroCancelaciones: 0,
      preferenciasViaje: []
    });
  }),

  // 5. Vehículos propios por defecto
  http.get('*/api/vehiculos/propios', () => {
    return HttpResponse.json([]);
  }),

  // 6. Mis viajes (resumen de actividad)
  http.get('*/api/viajes/mis-viajes', () => {
    return HttpResponse.json([]);
  }),

  // 7. Valoraciones recibidas por defecto
  http.get('*/api/valoraciones/valorado/*', () => {
    return HttpResponse.json([]);
  }),

  // 8. Autenticación y Cierre de sesión
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

  http.post('*/api/logout', () => {
    return HttpResponse.json({ success: true }, { status: 200 });
  })
];