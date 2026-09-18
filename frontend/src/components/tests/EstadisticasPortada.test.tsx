import { render, screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../setupTests';
import EstadisticasPortada from '../EstadisticasPortada';

const mockRutasTop5 = [
['Madrid', 120],
['Barcelona', 95],
['Valencia', 60],
['Sevilla', 40],
['Zaragoza', 25],
];

const mockContaminacionValida = {
datos: JSON.stringify({
    fuente: 'Red de Control Ambiental',
    momentoConsulta: '2026-09-18T10:00:00Z',
    ciudades: [
    {
        ciudad: 'Madrid',
        indiceCalidadAire: '15',
        categoria: 'Buena',
        contaminantes: { pm25: '5 µg/m³' },
    },
    {
        ciudad: 'Barcelona',
        indiceCalidadAire: '45',
        categoria: 'Regular',
        contaminantes: { pm25: '12 µg/m³' },
    },
    ],
}),
fechaConsulta: '2026-09-18T10:00:00Z',
};

test('muestra los indicadores de carga inicialmente', () => {
server.use(
    http.get('*/api/paradas/top5-localizaciones', () => new Promise(() => {})),
    http.get('*/api/contaminacion/actual', () => new Promise(() => {}))
);

render(<EstadisticasPortada />);

expect(screen.getByText('Cargando localizaciones...')).toBeInTheDocument();
expect(screen.getByText('Consultando datos ambientales...')).toBeInTheDocument();
});

test('carga y muestra las rutas frecuentes y los datos de contaminación correctamente', async () => {
server.use(
    http.get('*/api/paradas/top5-localizaciones', () => {
    return HttpResponse.json(mockRutasTop5);
    }),
    http.get('*/api/contaminacion/actual', () => {
    return HttpResponse.json(mockContaminacionValida);
    })
);

render(<EstadisticasPortada />);

// Esperar cargado mediante un texto unívoco
expect(await screen.findByText('120 paradas')).toBeInTheDocument();
expect(screen.getByText('95 paradas')).toBeInTheDocument();

// Comprobar coincidencia en ambas secciones (2 elementos por ciudad)
expect(screen.getAllByText('Madrid').length).toBe(2);
expect(screen.getAllByText('Barcelona').length).toBe(2);

// Validar datos de contaminación
expect(screen.getByText('15')).toBeInTheDocument();
expect(screen.getByText('Buena')).toBeInTheDocument();
expect(screen.getByText('PM2.5: 5 µg/m³')).toBeInTheDocument();

expect(screen.getByText('45')).toBeInTheDocument();
expect(screen.getByText('Regular')).toBeInTheDocument();

// Validar metadatos
expect(screen.getByText(/Fuente: Red de Control Ambiental/i)).toBeInTheDocument();
expect(screen.getByText(/Actualizado el/i)).toBeInTheDocument();
});

test('muestra mensaje alternativo cuando la API de rutas falla o devuelve un array vacío', async () => {
server.use(
    http.get('*/api/paradas/top5-localizaciones', () => {
    return new HttpResponse(null, { status: 500 });
    }),
    http.get('*/api/contaminacion/actual', () => {
    return HttpResponse.json(mockContaminacionValida);
    })
);

render(<EstadisticasPortada />);

expect(await screen.findByText('Todavía no hay datos suficientes.')).toBeInTheDocument();
});

test('maneja un JSON malformado en el campo "datos" de la respuesta ambiental', async () => {
server.use(
    http.get('*/api/paradas/top5-localizaciones', () => {
    return HttpResponse.json(mockRutasTop5);
    }),
    http.get('*/api/contaminacion/actual', () => {
    return HttpResponse.json({
        datos: 'Texto no serializado en JSON',
        fechaConsulta: '2026-09-18T10:00:00Z',
    });
    })
);

render(<EstadisticasPortada />);

expect(await screen.findByText('120 paradas')).toBeInTheDocument();
expect(screen.getByText(/Fuente: Texto no serializado en JSON/i)).toBeInTheDocument();
});

test('muestra el mensaje de error cuando falla la API de contaminación', async () => {
server.use(
    http.get('*/api/paradas/top5-localizaciones', () => {
    return HttpResponse.json(mockRutasTop5);
    }),
    http.get('*/api/contaminacion/actual', () => {
    return new HttpResponse(null, { status: 500 });
    })
);

render(<EstadisticasPortada />);

expect(
    await screen.findByText('Información no disponible en este momento')
).toBeInTheDocument();
});

test('filtra y limita las rutas a un máximo de 5 elementos descartando elementos malformados', async () => {
const rutasConMasDeCincoYMalformadas = [
    ['Madrid', 100],
    ['Barcelona', 80],
    ['Valencia', 60],
    ['Sevilla', 40],
    ['Zaragoza', 20],
    ['Bilbao', 10],
    ['Invalida'],
];

server.use(
    http.get('*/api/paradas/top5-localizaciones', () => {
    return HttpResponse.json(rutasConMasDeCincoYMalformadas);
    }),
    http.get('*/api/contaminacion/actual', () => {
    return HttpResponse.json({});
    })
);

render(<EstadisticasPortada />);

// Esperar a que rendericen las rutas
expect(await screen.findByText('100 paradas')).toBeInTheDocument();

// Delimitar la búsqueda al bloque exclusivo de "Rutas y destinos frecuentes"
const articuloRutas = screen
    .getByRole('heading', { name: /rutas y destinos frecuentes/i })
    .closest('article')!;

expect(within(articuloRutas).getByText('Madrid')).toBeInTheDocument();
expect(within(articuloRutas).getByText('Zaragoza')).toBeInTheDocument();
expect(within(articuloRutas).queryByText('Bilbao')).not.toBeInTheDocument();
});