-- Seeder manual de personas para entorno local/desarrollo.
-- Solo inserta datos de la tabla persona.
-- Requiere extensión pgcrypto para generar hash BCrypt compatible con Spring Security.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO persona (nombre, primer_apellido, segundo_apellido, contrasena, email, telefono, reputacion)
VALUES
	(
		'Selenium',
		'Tester',
		NULL,
		crypt('Selenium123!', gen_salt('bf', 10)),
		'selenium@compicar.test',
		'+34600000001',
		0
	),
	(
		'Alicia',
		'Martin',
		'Lopez',
		crypt('123456', gen_salt('bf', 10)),
		'alicia.martin@compicar.test',
		'+34600000002',
		4.6
	),
	(
		'Bruno',
		'Garcia',
		'Sanz',
		crypt('123456', gen_salt('bf', 10)),
		'bruno.garcia@compicar.test',
		'+34600000003',
		4.2
	),
	(
		'Clara',
		'Ruiz',
		NULL,
		crypt('123456', gen_salt('bf', 10)),
		'clara.ruiz@compicar.test',
		'+34600000004',
		4.9
	)
ON CONFLICT (email) DO UPDATE
SET
	nombre = EXCLUDED.nombre,
	primer_apellido = EXCLUDED.primer_apellido,
	segundo_apellido = EXCLUDED.segundo_apellido,
	telefono = EXCLUDED.telefono,
	reputacion = EXCLUDED.reputacion;
