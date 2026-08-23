<p align="center">
  <img src="src/main/resources/static/images/furent-logo.png" alt="Furent Logo" width="120" height="120"/>
</p>

# 🪑 Furent — Plataforma de Alquiler de Mobiliarios

Furent es una solución SaaS completa para la gestión integral y alquiler de mobiliario para eventos. El sistema está diseñado en una arquitectura robusta de capas sobre Spring Boot y MongoDB, proporcionando una interfaz moderna, responsiva y veloz para clientes finales, y un potente panel de administración con analítica predictiva avanzada para los gerentes del negocio.

---

## 👥 Colaboradores

* **Luis Troconis** (@Ldtro)

---

## 📋 Descripción General

La plataforma automatiza el flujo completo del negocio de alquileres, desde la navegación interactiva del catálogo de productos y el armado de cotizaciones personalizadas, hasta la confirmación de reservas mediante pasarelas de pago, gestión de logística y entregas en terreno, y auditoría administrativa de acciones críticas.

Adicionalmente, incorpora un **módulo de inteligencia artificial** con el algoritmo J48 de Weka para predecir la demanda futura, permitiendo optimizar el inventario y planificar recursos de forma científica.

---

## ✨ Características Principales

### 👤 Para Clientes (Portal Público)
* 🔍 **Buscador Inteligente en Tiempo Real**: Filtrado dinámico por nombre, material, precio y categorías.
* 📋 **Carrito de Cotizaciones**: Permite armar una cotización, simular fechas de alquiler y calcular subtotales.
* 💳 **Pasarela de Pago Simulada**: Flujo completo de pagos integrando múltiples métodos (Transferencia, Efectivo, Nequi, Daviplata).
* 🎟️ **Cupones de Descuento**: Aplicación y validación automática de códigos promocionales activos.
* ⭐ **Calificaciones y Reseñas**: Sistema de valoraciones por producto con promedio de estrellas.
* 🔐 **Autenticación Social (Google OAuth2)**: Inicio de sesión rápido y seguro mediante cuentas de Google.
* ❤️ **Lista de Favoritos**: Guardado persistente de productos de interés para el cliente.
* 🔔 **Bandeja de Notificaciones**: Alertas en tiempo real sobre aprobaciones de reservas, pagos exitosos y cambios de estado.

### 🛡️ Para Administradores (Panel de Control)
* 📊 **Dashboard Ejecutivo**: Resumen de indicadores clave de rendimiento (Ingresos mensuales, Reservas activas, Registros nuevos).
* 📈 **Módulo Predictivo (J48 & Weka)**: Visualización y afinamiento de proyecciones de demanda diaria y recomendaciones de inventario basadas en IA.
* 📦 **Gestión de Mobiliario y Stock**: Control total del inventario de productos, asignación a mantenimiento, y alertas automáticas de bajo stock.
* 🗓️ **Calendario y Logística**: Calendario mensual con entregas/recogidas agendadas y exportación de hojas de ruta en PDF.
* 📝 **Gestión de Reservas y Estados**: Máquina de estados para rastrear órdenes (Pendiente ➜ Confirmada ➜ Entregada ➜ Completada / Cancelada).
* 👥 **Administración de Personal y Usuarios**: Asignación de roles, auditoría de logs, y suspensión temporal o permanente de cuentas.
* 🎫 **Gestión de Cupones**: Creación de descuentos con fechas de vigencia y límites de uso.
* 📄 **Generación de PDFs**: Contratos formales de alquiler y reportes de logística generados dinámicamente.

---

## 🧠 Módulo de Analítica y Predicciones (Weka J48)

Furent cuenta con un motor predictivo que estima la demanda de mobiliario para los siguientes **14 días**. 

### ¿Cómo funciona el modelo?
1. **Clasificación de Demanda**: Agrupa los días en tres niveles de demanda basados en percentiles históricos: **BAJA**, **MEDIA** o **ALTA**.
2. **Entrenamiento con Árbol J48**: El clasificador evalúa atributos clave del día como:
   * Día de la semana (y si es fin de semana).
   * Mes del año (para detectar estacionalidad vacacional o navideña).
   * Promedio móvil de unidades del último bloque de 7 días.
   * Total de ingresos esperados y cantidad de reservas creadas.
3. **Conversión a Unidades**: A partir del nivel de demanda predicho, calcula las unidades de mobiliario proyectadas utilizando el promedio de uso de días similares en el pasado.
4. **Recomendaciones Inteligentes**: Genera acciones automatizadas de gestión de stock, asignación de personal para días de carga máxima y oportunidades de precios dinámicos (suspender cupones en días de sobredemanda).

---

## 🛠️ Stack Tecnológico

| Capa | Tecnología |
| :--- | :--- |
| **Backend Core** | Java 17 · Spring Boot 4.0.3 · Spring Data MongoDB |
| **Seguridad** | Spring Security 6 · OAuth2 Client (Google) · BCrypt |
| **Base de Datos** | MongoDB 7.0 |
| **Inteligencia Artificial** | Weka 3.8 (Clasificador J48 / Árboles de Decisión C4.5) |
| **Frontend** | Thymeleaf · Tailwind CSS 4 · Chart.js · Alpine.js |
| **Reportes y PDFs** | OpenHTMLToPDF |
| **Documentación API** | SpringDoc OpenAPI (Swagger UI) |
| **Entorno e Infraestructura** | Docker · Docker Compose · GitHub Actions |

---

## 🚀 Inicio Rápido

### Prerrequisitos
* **Java SDK 17** o superior.
* **MongoDB 7.0** ejecutándose de forma local o en la nube (Atlas).
* **Maven 3.9+** (o utilizar el wrapper `./mvnw` provisto).

### Ejecución Local
1. Clonar el repositorio:
   ```bash
   git clone https://github.com/whosjeiv/Furent-Alquiler.git
   cd Furent-Alquiler
   ```
2. Asegurar que MongoDB esté corriendo en `localhost:27017` o configurar tu URI en las variables de entorno.
3. Arrancar la aplicación con Maven:
   ```bash
   ./mvnw spring-boot:run
   ```
4. Abrir en el navegador: **http://localhost:8080**

### Con Docker Compose
Puedes levantar todo el entorno (Aplicación + Base de datos MongoDB) en un solo paso:
```bash
docker compose up -d
```

---

## ⚙️ Variables de Entorno Clave

| Variable | Descripción | Valor por Defecto |
| :--- | :--- | :--- |
| `SPRING_DATA_MONGODB_URI` | Dirección de conexión a la BD | `mongodb://localhost:27017/furent` |
| `SPRING_PROFILES_ACTIVE` | Perfil de ejecución activo | `dev` (opciones: `dev`, `prod`) |
| `FURENT_ADMIN_PASSWORD` | Contraseña por defecto del admin inicial | `admin123` |
| `GOOGLE_CLIENT_ID` | Client ID de Google Console para OAuth2 | *Opcional* |
| `GOOGLE_CLIENT_SECRET` | Client Secret de Google Console para OAuth2 | *Opcional* |

---

## 📁 Estructura del Proyecto

```text
furent/
├── src/main/java/com/alquiler/furent/
│   ├── config/        # Configuración de Seguridad, Inicialización de Datos (DataInitializer)
│   ├── controller/    # Controladores de Thymeleaf (Públicos) y de Administración (Admin)
│   ├── dto/           # Data Transfer Objects para respuestas y peticiones API
│   ├── enums/         # Estados de reservas, pagos, roles y categorías
│   ├── exception/     # Manejador global de excepciones del sistema
│   ├── model/         # Entidades de MongoDB (User, Product, Reservation, Payment, etc.)
│   ├── repository/    # Interfaces de Spring Data MongoRepository
│   └── service/       # Lógica de negocio e IA (PredictiveService, EmailService, etc.)
├── src/main/resources/
│   ├── templates/     # Páginas y layouts en Thymeleaf (carpetas public, admin, fragments)
│   ├── static/        # Hojas de estilo CSS, scripts JS, y recursos gráficos (images/logo)
│   └── application.properties # Parámetros globales del framework
├── docs/              # Documentación detallada del proyecto (UML, arquitectura, flujo de reservas)
├── Dockerfile         # Archivo de construcción de imagen Docker
├── docker-compose.yml # Orquestación local de contenedores
└── pom.xml            # Dependencias y configuración de Maven
```

---

## 🛡️ Seguridad y Buenas Prácticas
* **Protección CSRF**: Habilitada por defecto en todos los formularios Thymeleaf.
* **Content Security Policy (CSP)**: Cabeceras estrictas configuradas para evitar inyecciones XSS.
* **Control de Acceso basado en Roles (RBAC)**: Rutas administrativas protegidas estrictamente bajo el rol `ADMIN`.
* **Cifrado Seguro**: Contraseñas de usuario cifradas mediante `BCryptPasswordEncoder`.
* **Registro de Auditoría**: Trazabilidad completa de las acciones del panel de control almacenadas en la colección `audit_logs`.

---

## 🧪 Pruebas Unitarias e Integración
Para validar que todos los servicios y el modelo predictivo funcionen correctamente, ejecuta:
```bash
./mvnw test
```

---

## 👤 Credenciales Administrativas Iniciales

Al arrancar por primera vez, el sistema autogenera un administrador base si no existe ninguno:
* **Usuario**: `admin@furent.com`
* **Contraseña**: `admin123` (se recomienda redefinir mediante la variable de entorno `FURENT_ADMIN_PASSWORD`).

---

## 📜 Licencia
Este proyecto está distribuido bajo la licencia MIT. Consúltala en el archivo correspondiente para más información.
