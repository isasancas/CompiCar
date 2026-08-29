import { http, HttpResponse } from 'msw';

export const handlers = [
  http.post('*/api/login', async ({ request }) => {
    const body = (await request.json()) as { email?: string; contrasena?: string };

    // Simulación de login correcto
    if (body.email === 'usuario@test.com' && body.contrasena === 'Password123') {
      return HttpResponse.json({ token: 'mocked-jwt-token-123' }, { status: 200 });
    }

    // Simulación de error de credenciales devuelto por Spring Boot
    if (body.email === 'error@test.com') {
      return HttpResponse.json({ error: 'La contraseña es incorrecta' }, { status: 400 });
    }

    return HttpResponse.json({ error: 'Error en el inicio de sesión' }, { status: 401 });
  }),
];