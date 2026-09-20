# CompiCar

Aplicación full-stack con **Spring Boot** (backend) y **React + TypeScript + Vite** (frontend).

---

## Requisitos previos (solo la primera vez)

Instala las siguientes herramientas antes de continuar:

| Herramienta | Versión mínima | Descarga |
|---|---|---|
| Java JDK | 21 | https://adoptium.net/ |
| Node.js | v22+ (LTS) | https://nodejs.org/ |
| PostgreSQL | 15+ (probado en 18.3) | https://www.postgresql.org/download/ |
| Android Studio | Última versión | https://developer.android.com/studio |

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
npm install       # Solo la primera vez: instala las dependencias
npm install leaflet react-leaflet                       # Solo la primera vez
npm install -D @types/leaflet                           # Solo la primera vez
npm install @stripe/stripe-js @stripe/react-stripe-js   # Solo la primera vez
npm run dev
```

La aplicación estará disponible en: http://localhost:5173

También existe un archivo llamado **run-dev.bat** para Windows, el cual se puede ejecutar para levantarlo todo a la vez.

---

## Resumen de puertos

| Servicio | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| PostgreSQL | `localhost:5432` |

> Ambas terminales deben estar corriendo al mismo tiempo.

---

## Generar APK para Android (Capacitor)

Para compilar y generar la aplicación ejecutable (`.apk`) de Android conectada al backend desplegado en producción:

### 1. Archivos de entorno (`frontend/`)

Asegúrate de contar con los siguientes archivos en la raíz de `frontend/` para diferenciar los entornos:

- **`.env.development`** (usado automáticamente con `npm run dev`):
  ```env
  VITE_API_BASE_URL=http://localhost:8080
  ```
- **`.env.production`** (usado automáticamente con `npm run build` para la APK):
  ```env
  VITE_API_BASE_URL=https://tu-aplicacion.koyeb.app
  ```

### 2. Secuencia de comandos

Ejecuta los siguientes comandos desde la carpeta `frontend/`:

```bash
# 1. Compilar los activos estáticos de React para producción
npm run build

# 2. Copiar los archivos compilados al proyecto nativo Android
npx cap sync android

# 3. Abrir el proyecto en Android Studio
npx cap open android
```

### 3. Generación del ejecutable en Android Studio

1. Espera a que Android Studio finalice la sincronización inicial de Gradle.
2. Ve al menú superior **Build** > **Generate App Bundles or APKs** > **Build APK(s)** (o **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**).
3. Tras finalizar la compilación, aparecerá un aviso emergente abajo a la derecha. Haz clic en **locate**.

### 4. Ubicación de la APK compilada

El archivo ejecutable estará ubicado en:

```text
frontend/android/app/build/outputs/apk/debug/app-debug.apk
```

---

## Tests E2E con Selenium

El proyecto incluye una base de tests E2E en Java (JUnit 5 + Selenium) en:

- `backend/src/test/java/com/compicar/e2e`

### Requisitos para ejecutar E2E

1. Tener backend y frontend levantados:
```bash
# Terminal 1
cd backend
.\mvnw.cmd spring-boot:run

# Terminal 2
cd frontend
npm run dev
```

2. Ejecutar solo la suite E2E:

```bash
cd backend
.\mvnw.cmd -Pe2e test
```

Por defecto (`.\mvnw.cmd test`) los E2E quedan excluidos para no romper CI.

Si quieres ejecutar todos los tests (unitarios + E2E), usa:

```bash
cd backend
.\mvnw.cmd test
.\mvnw.cmd -Pe2e test
```