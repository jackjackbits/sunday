# Sun Day

Seguimiento de rayos UV y calculadora de vitamina D para iOS.

[📖 Lee la metodologia detallada](METHODOLOGY.md) | [🔒 Politica de Privacidad](PRIVACY.md)

<img height="500" alt="SunDay_1290x2796_v2" src="https://github.com/user-attachments/assets/b712cc98-1cc5-4e6f-8297-cabf8f801013" />

## Caracteristicas

- Indice UV en tiempo real de tu ubicacion
- Calculo de vitamina D basado en los rayos UV, el tipo de piel y la ropa
- Visualizacion de la fase lunar por la noche
- Horas de salida y puesta del sol
- Guarda en Apple Health
- No se requieren claves de API
- Widgets pequeños y medianos para tu pantalla de inicio

## Requisitos

- iOS 17.0+
- Solo para iPhone
- Xcode 15+

## Configuracion

1. Clona el repositorio
2. Ejecuta `xcodegen generate` para crear el proyecto de Xcode
3. Abre `Sunday.xcodeproj`
4. Selecciona tu equipo de desarrollo
5. Compila y ejecuta

## Uso

1. Permite los permisos de ubicacion y salud
2. Pulsa el boton del sol para iniciar el seguimiento
3. Selecciona tu nivel de ropa y tipo de piel
4. La aplicacion calcula la ingesta de vitamina D automaticamente

## APIs utilizadas

- Open-Meteo para datos UV (gratis, sin clave)
- Farmsense para las fases lunares (gratis, sin clave)

## Licencia

Dominio publico. Usalo como quieras.
