# Sunday - Versión Android

Versión Android de [Sunday](https://github.com/jackjackbits/sunday), una aplicación integral para el seguimiento de exposición UV y generación de vitamina D, completamente migrada desde la implementación original en iOS Swift.

## 🌟 Resumen Completo de Migración de Características

Esta versión Android logra **100% de paridad de características** con la aplicación iOS original a través de una migración sistemática en tres fases:

### 📊 Tabla de Comparación de Características

| Característica | iOS Original | Implementación Android | Estado | Fase de Implementación |
|----------------|-------------|------------------------|---------|---------------------|
| **Seguimiento UV Central** | ✅ | ✅ | Completo | Base |
| Índice UV en Tiempo Real | HealthKit + OpenWeather | API Open-Meteo + Cache Room | ✅ Completo | Base |
| Cálculo de Vitamina D | Algoritmo Swift Personalizado | VitaminDCalculator Kotlin | ✅ Completo | Base |
| Selección Tipo de Piel | 6 Tipos Fitzpatrick | 6 Tipos Fitzpatrick | ✅ Completo | Base |
| Ajuste Nivel de Ropa | 4 Niveles | 4 Niveles (Mínima→Pesada) | ✅ Completo | Base |
| **Integración de Salud** | ✅ | ✅ | Completo | Base |
| Sincronización Datos Salud | HealthKit | API Google Fit | ✅ Completo | Base |
| Seguimiento de Sesiones | Core Data | Base de Datos Room | ✅ Completo | Base |
| **Sistema de Fases Lunares** | ✅ | ✅ | Completo | Fase 1 |
| Visualización Fases Lunares | Cálculo Manual | Integración API Farmsense | ✅ Completo | Fase 1 |
| Interfaz Modo Nocturno | Iconos Luna Estáticos | Iconos Fase Dinámicos + Animación | ✅ Mejorado | Fase 1 |
| Cache Datos Lunares | En Memoria | Cache Base de Datos Room | ✅ Mejorado | Fase 1 |
| **Temporización Solar y Notificaciones** | ✅ | ✅ | Completo | Fase 2 |
| Cálculo Mediodía Solar | Core Location | SolarCalculator.kt | ✅ Completo | Fase 2 |
| Notificaciones Sol Óptimo | UserNotifications | AlarmManager + NotificationService | ✅ Completo | Fase 2 |
| Temporización Basada en Ubicación | CLLocationManager | LocationManager + Geocoding | ✅ Completo | Fase 2 |
| **Sistema de Widget** | ✅ | ✅ | Completo | Fase 2 |
| Widget Pantalla Inicio | WidgetKit | Glance para Android | ✅ Completo | Fase 2 |
| Visualización Índice UV | Layout Estático | Alternancia Dinámica UV/Luna | ✅ Mejorado | Fase 2 |
| Actualizaciones Widget | Timeline Provider | Work Manager Periódico | ✅ Completo | Fase 2 |
| Modos Widget Día/Noche | Cambio Manual | Automático Basado en Solar | ✅ Mejorado | Fase 2 |
| **Características UI/UX** | ✅ | ✅ | Completo | Fase 3 |
| Pantalla de Configuraciones | Formularios SwiftUI | Jetpack Compose | ✅ Completo | Fase 3 |
| Animaciones Suaves | Transiciones SwiftUI | Compose AnimatedVisibility | ✅ Completo | Fase 3 |
| Material Design | Sistema de Diseño iOS | Material Design 3 | ✅ Optimizado para Plataforma | Fase 3 |
| Soporte Accesibilidad | VoiceOver | TalkBack + Semantics | ✅ Completo | Fase 3 |
| **Rendimiento y Optimización** | ✅ | ✅ | Mejorado | Fase 3 |
| Gestión de Memoria | ARC | Kotlin Coroutines + StateFlow | ✅ Mejorado | Fase 3 |
| Procesamiento en Segundo Plano | BackgroundTasks | WorkManager | ✅ Optimizado para Plataforma | Fase 3 |
| Monitoreo de Red | Network.framework | ConnectivityManager | ✅ Completo | Fase 3 |
| Manejo de Errores | Result Types | Sealed Classes + Exception Handling | ✅ Mejorado | Fase 3 |
| **Herramientas de Desarrollo** | ✅ | ✅ | Mejorado | Fase 3 |
| Diagnósticos de Debug | Logging Básico | DiagnosticService.kt | ✅ Mejorado | Fase 3 |
| Sistema de Migración | Migración Core Data | Migración Room + MigrationService | ✅ Mejorado | Fase 3 |

### 🚀 Mejoras Específicas de Android

Más allá de la paridad con iOS, la versión Android incluye mejoras específicas de la plataforma:

- **Widget Mejorado**: Cambio automático modo día/noche basado en cálculos solares
- **Cache Avanzado**: Base de datos Room con limpieza inteligente y soporte offline
- **Material Design 3**: Lenguaje de diseño nativo de la plataforma con temas dinámicos
- **Canales de Notificación**: Gestión de notificaciones específica de Android
- **Optimización en Segundo Plano**: Integración WorkManager para tareas eficientes
- **Gestión de Permisos**: Integración sistema de permisos granular de Android

## 🔧 Arquitectura Técnica

### Componentes Centrales
- **Arquitectura MVVM** con StateFlow para actualizaciones reactivas de UI
- **Base de Datos Room** para persistencia local y cache
- **Retrofit + OkHttp** para operaciones de red con reintento automático
- **Jetpack Compose** para desarrollo de UI moderno y declarativo
- **API Google Fit** para integración de datos de salud
- **WorkManager** para ejecución confiable de tareas en segundo plano

### Integraciones API
- **API Open-Meteo**: Datos de índice UV y clima
- **API Farmsense**: Cálculos precisos de fases lunares
- **Google Fit**: Seguimiento de salud y vitamina D
- **Servicios de Ubicación Android**: Ubicación GPS y de red

### Optimizaciones de Rendimiento
- Cache inteligente de datos con limpieza automática
- Operaciones async basadas en corrutinas
- Gestión de estado eficiente en memoria
- Optimización de tareas en segundo plano

## 📋 Requisitos

### Requisitos Técnicos
- **Android 8.0 (API 26)** o superior
- **Google Play Services** instalado y actualizado
- **Servicios de Ubicación** habilitados para datos UV precisos
- **Conexión a Internet** para datos meteorológicos y llamadas API

### Permisos
La aplicación requiere los siguientes permisos:
- **Acceso a Ubicación**: Ubicación precisa para índice UV y cálculos solares
- **Google Fit**: Integración de datos de salud para seguimiento vitamina D
- **Notificaciones**: Alertas de temporización solar y advertencias UV
- **Internet**: Datos meteorológicos y comunicación API
- **Procesamiento en Segundo Plano**: Actualizaciones widget y notificaciones programadas

## 🚀 Instalación y Configuración

### Configuración de Desarrollo
```bash
# Clonar el repositorio
git clone [repository-url]
cd sunday---Android

# Abrir en Android Studio
# Asegúrate de tener Android Studio Arctic Fox o más nuevo

# Sincronizar dependencias Gradle
./gradlew build

# Ejecutar la aplicación
./gradlew assembleDebug
```

### Configuración API
1. **API Open-Meteo**: No requiere clave API (servicio gratuito)
2. **API Farmsense**: No requiere clave API (servicio gratuito)
3. **Google Fit**: Habilitar en Google Cloud Console y agregar tu huella SHA-1

### Variantes de Build
- **Debug**: Build de desarrollo con logging detallado
- **Release**: Build de producción con optimizaciones

## 🔧 Estructura del Proyecto

```
app/src/main/java/com/gmolate/sunday/
├── MainActivity.kt                 # Punto de entrada de la app
├── SundayApplication.kt           # Clase de aplicación
├── model/                         # Modelos de datos y base de datos
│   ├── AppDatabase.kt            # Configuración base datos Room
│   ├── UserPreferences.kt        # Entidad configuraciones usuario
│   ├── VitaminDSession.kt        # Seguimiento de sesiones
│   ├── CachedUVData.kt          # Cache datos UV
│   └── CachedMoonData.kt        # Cache fases lunares
├── service/                       # Servicios lógica de negocio
│   ├── UVService.kt              # Gestión datos UV
│   ├── VitaminDCalculator.kt     # Cálculos vitamina D
│   ├── MoonPhaseService.kt       # Gestión fases lunares
│   ├── SolarCalculator.kt        # Cálculos temporización solar
│   ├── NotificationService.kt    # Gestión notificaciones
│   ├── MigrationService.kt       # Migración de datos
│   └── DiagnosticService.kt      # Diagnósticos desarrollo
├── ui/                           # Interfaz de usuario
│   ├── view/
│   │   ├── ContentView.kt        # Pantalla principal app
│   │   └── SettingsView.kt       # Pantalla configuraciones
│   └── viewmodel/
│       └── MainViewModel.kt      # Gestión de estado
└── widget/
    └── SundayWidget.kt           # Widget pantalla inicio
```

## 🧪 Pruebas

### Pruebas Unitarias
```bash
./gradlew test
```

### Pruebas de Integración
```bash
./gradlew connectedAndroidTest
```

### Pruebas del Widget
Probar la funcionalidad del widget de pantalla de inicio:
1. Mantener presionado en pantalla de inicio
2. Agregar "Sunday UV Widget"
3. Verificar visualización UV/fases lunares

## 📱 Características en Detalle

### Aplicación Principal
- **Seguimiento UV en tiempo real** con precisión basada en ubicación
- **Cálculo vitamina D** usando algoritmos científicamente precisos
- **Notificaciones inteligentes** para temporización óptima exposición solar
- **Configuraciones integrales** para personalización
- **Modo offline** con cache inteligente de datos

### Widget Pantalla de Inicio
- **Visualización dinámica** alternando entre índice UV y fases lunares
- **Detección automática día/noche** basada en cálculos solares
- **Actualizaciones resistentes a errores** con estados de respaldo elegantes
- **Estilo Material Design 3** con integración tema del sistema

### Servicios en Segundo Plano
- **Notificaciones mediodía solar** calculadas para ubicación precisa
- **Actualizaciones automáticas de datos** con monitoreo de red
- **Optimización de batería** a través de programación inteligente
- **Soporte de migración** para actualizaciones de app sin problemas

## 🤝 Contribución

### Guías de Desarrollo
1. Seguir mejores prácticas de desarrollo Android
2. Usar convenciones de codificación Kotlin
3. Escribir pruebas unitarias para nuevas características
4. Actualizar documentación para cambios significativos

### Reporte de Problemas
Al reportar problemas, incluye:
- Versión Android y modelo de dispositivo
- Pasos para reproducir el problema
- Comportamiento esperado vs real
- Capturas de pantalla si aplica

## 📄 Licencia

Este proyecto está licenciado bajo la [Licencia MIT](LICENSE).

## 🙏 Créditos

Basado en el proyecto original [Sunday](https://github.com/jackjackbits/sunday) para iOS por **jackjackbits**.

### Migración Android
- **Migración completa de características iOS** a Android/Kotlin
- **Optimizaciones específicas de plataforma** para ecosistema Android
- **Funcionalidad de widget mejorada** con modos día/noche
- **Arquitectura Android moderna** con Jetpack Compose y Room

### Servicios de Terceros
- **API Open-Meteo**: Datos de clima y UV
- **API Farmsense**: Cálculos de fases lunares
- **Google Fit**: Integración de datos de salud
- **Material Design 3**: Sistema de diseño UI

---

**Nota**: Esta versión Android mantiene paridad completa de características con la aplicación iOS original mientras proporciona mejoras y optimizaciones específicas de Android. La migración se completó a través de un enfoque sistemático de tres fases asegurando que no se perdiera funcionalidad en la traducción.
