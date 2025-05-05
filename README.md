# wKoth - Sistema de King of the Hill para Minecraft

Plugin desarrollado por wwishh que implementa un sistema avanzado de King of the Hill (KoTH) con múltiples arenas, sistemas de recompensa flexibles y programación automática de eventos.

## Características principales

- Crear y gestionar múltiples arenas KoTH
- Sistema de captura con tiempo configurable
- Recompensas por comandos y/o cofres de botín
- Programación automática de eventos KoTH
- Mensajes personalizables

## Placeholders disponibles

El plugin ofrece los siguientes placeholders que puedes usar en tus mensajes o con otros plugins:

- `%koth_active%` - Muestra "Sí" o "No" si hay algún KoTH activo
- `%koth_name%` - Nombre del KoTH activo (o "Ninguno")
- `%koth_time%` - Tiempo restante del KoTH activo (o "N/A")
- `%koth_capturing%` - Jugador capturando el KoTH (o "Nadie")
- `%koth_specific_time_[nombre]%` - Tiempo restante de un KoTH específico
- `%koth_specific_active_[nombre]%` - Si un KoTH específico está activo
- `%koth_specific_capturing_[nombre]%` - Jugador capturando un KoTH específico

Para usar estos placeholders con PlaceholderAPI, necesitarás instalar la expansión correspondiente.

## Comandos principales

- `/koth create <nombre>` - Crea un nuevo KoTH
- `/koth set` - Establece la zona del KoTH
- `/koth start <nombre>` - Inicia un KoTH
- `/koth stop <nombre>` - Detiene un KoTH
- `/koth set-capture-time <nombre> <segundos>` - Establece el tiempo de captura
- `/koth set-duration <nombre> <segundos>` - Establece la duración máxima del KoTH
- `/koth set-chest <nombre>` - Define la ubicación del cofre de recompensas
- `/koth set-loot <nombre>` - Configura el loot del cofre
