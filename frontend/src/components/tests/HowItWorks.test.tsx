import { render, screen } from '@testing-library/react';
import HowItWorks from '../HowItWorks';

test('Renderiza el encabezado de sección, badge informativa y subtítulo', () => {
  render(<HowItWorks />);

  expect(screen.getByText('Cómo funciona')).toBeInTheDocument();
  expect(
    screen.getByRole('heading', { level: 2, name: 'Tres pasos para empezar' })
  ).toBeInTheDocument();
  expect(
    screen.getByText(/empezar a compartir vehículo nunca fue tan sencillo/i)
  ).toBeInTheDocument();
});

test('Renderiza las 3 etapas del proceso con sus títulos y descripciones correspondientes', () => {
  render(<HowItWorks />);

  expect(
    screen.getByRole('heading', { level: 3, name: 'Crea tu perfil' })
  ).toBeInTheDocument();
  expect(
    screen.getByRole('heading', { level: 3, name: 'Encuentra tu compañero' })
  ).toBeInTheDocument();
  expect(
    screen.getByRole('heading', { level: 3, name: '¡Comparte y ahorra!' })
  ).toBeInTheDocument();

  expect(screen.getByText(/regístrate en segundos/i)).toBeInTheDocument();
  expect(
    screen.getByText(/buscamos personas con trayectos similares/i)
  ).toBeInTheDocument();
  expect(
    screen.getByText(/coordina el viaje, divide el coste/i)
  ).toBeInTheDocument();
});

test('Muestra los emoticonos / iconos representativos de cada paso', () => {
  render(<HowItWorks />);

  expect(screen.getByText('👤')).toBeInTheDocument();
  expect(screen.getByText('🔍')).toBeInTheDocument();
  expect(screen.getByText('🚗')).toBeInTheDocument();
});

test('Renderiza la sección contenedor con el ID idéntico y exactamente 3 pasos', () => {
  const { container } = render(<HowItWorks />);

  const section = container.querySelector('section#como-funciona');
  expect(section).toBeInTheDocument();

  const pasosH3 = screen.getAllByRole('heading', { level: 3 });
  expect(pasosH3).toHaveLength(3);
});