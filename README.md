# CompiCar

Aplicación full-stack con **Spring Boot** (backend) y **React + TypeScript + Vite** (frontend).

---

## Requisitos previos (solo la primera vez)

Instala las siguientes herramientas antes de continuar:

| Herramienta | Versión mínima | Descarga |
|---|---|---|
| Java JDK | 21 | https://adoptium.net/ |
| Node.js | LTS | https://nodejs.org/ |
| PostgreSQL | 15+ (probado en 18.3) | https://www.postgresql.org/download/ |

Verifica que estén correctamente instaladas:

```bash
java -version
node -v
npm -v
psql --version
```

---

## Configurar PostgreSQL (solo la primera vez)

### 1. Instalar PostgreSQL

Descarga el instalador desde https://www.postgresql.org/download/windows/ y ejecútalo.

Durante la instalación:
- **Contraseña del superusuario** (`postgres`): elige una contraseña y anótala
- **Puerto**: deja `5432` (por defecto)
- **Stack Builder**: puedes desmarcar, no es necesario

### 2. Crear la base de datos

Abre **SQL Shell (psql)** desde el menú Inicio, pulsa Enter en todas las opciones hasta que pida la contraseña, introdúcela y ejecuta:

```sql
CREATE DATABASE compicar;
```

### 3. Ajustar credenciales en el proyecto

Abre `backend/src/main/resources/application.yml` y asegúrate de que el `password` coincide con el que pusiste al instalar:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/compicar
    username: postgres
    password: TU_CONTRASEÑA
```

### 4. (Opcional) Conectar con DBeaver

Si quieres explorar la base de datos visualmente, descarga DBeaver desde https://dbeaver.io/ y crea una nueva conexión:

| Campo | Valor |
|---|---|
| Tipo | PostgreSQL |
| Host | `localhost` |
| Port | `5432` |
| Database | `compicar` |
| Username | `postgres` |
| Password | la que pusiste al instalar |

> La primera vez DBeaver te pedirá descargar el driver de PostgreSQL, acepta.

---

## Arrancar la aplicación

### 1. Backend (Spring Boot)

Abre una terminal en la carpeta `backend` y ejecuta:

```bash
cd backend
.\mvnw.cmd spring-boot:run   # Windows
./mvnw spring-boot:run        # Mac / Linux
```

> **Primera vez:** Maven descargará todas las dependencias automáticamente. Puede tardar unos minutos.

El servidor estará listo cuando veas en los logs:
```
Started CompiCarApplication
```

- API disponible en: http://localhost:8080

---

### 2. Frontend (React + Vite)

Abre **otra terminal** en la carpeta `frontend` y ejecuta:

```bash
cd frontend
npm install       # ⚠️ Solo la primera vez: instala las dependencias
npm run dev
```

La aplicación estará disponible en: http://localhost:5173

---

## Resumen de puertos

| Servicio | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| PostgreSQL | `localhost:5432` |

> Ambas terminales deben estar corriendo al mismo tiempo.

