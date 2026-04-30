const rawBaseUrl = import.meta.env.DEV ? 'http://localhost:8080' : '';

export const API_BASE_URL = rawBaseUrl.replace(/\/+$/, '').trim();

export const buildApiUrl = (path: string) => {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return API_BASE_URL + normalizedPath;
};