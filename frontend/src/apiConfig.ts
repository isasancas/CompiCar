const rawBaseUrl =
  import.meta.env.VITE_API_BASE_URL ||
  (import.meta.env.DEV ? 'http://localhost:8080' : 'https://tu-aplicacion.koyeb.app'); // Sustituye por tu URL de Koyeb por seguridad

export const API_BASE_URL = rawBaseUrl.replace(/\/+$/, '').trim();

export const buildApiUrl = (path: string) => {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return API_BASE_URL + normalizedPath;
};