# CámaraConMapa

App Android para registrar recorridos con fotos geolocalizadas. Tomas una foto, queda marcada en el mapa con miniatura, y la ruta entre fotos se dibuja siguiendo las calles reales.

Hecho con Jetpack Compose, CameraX y Google Maps.

## Setup

Necesitas una API key de Google Maps con **Maps SDK for Android** y **Directions API** habilitadas. La metes en `local.properties`:

```
MAPS_API_KEY=AIzaSy...
```

Sync Gradle y run. Usa emulador con Google Play (no solo Google APIs), o un dispositivo real.

Para conseguir la SHA-1 de debug si restringes la key:

```bash
./gradlew signingReport
```

## Qué hace

- Preview de cámara con botón de captura y cambio entre lente frontal/trasera
- Botón play/pause arriba a la derecha del mapa para iniciar recorrido
- Cada foto se guarda en la galería (`Pictures/CamaraConMapa/`) y se agrega al mapa como marker con la miniatura
- La ruta entre fotos se traza por las calles, no en línea recta (Directions API, modo caminando)
- Lista horizontal de fotos tomadas mientras el recorrido está activo
- Galería fullscreen con pinch-to-zoom
- Layout adaptativo: portrait con cámara arriba y mapa abajo; landscape con mapa izquierda y cámara derecha
- Al pausar se limpia la lista y la ruta, pero las fotos quedan en la galería del teléfono

## Permisos

Se piden en runtime: `CAMERA`, `ACCESS_FINE_LOCATION`. En API ≤ 28 también `WRITE_EXTERNAL_STORAGE`.

## Estructura

```
app/src/main/java/com/example/camaraconmapa/
├── MainActivity.kt           Entry + splash + decide portrait/landscape
├── PermissionsGate.kt        Bloquea hasta conceder permisos
├── TourViewModel.kt          Estado: isRecording, photos, path
├── CameraSection.kt          CameraX + captura + rotación dinámica
├── MapSection.kt             Mapa, markers, polyline, botón ubicar
├── PhotoBar.kt               Lista horizontal de miniaturas
├── PhotoGalleryDialog.kt     Grid fullscreen + zoom
└── DirectionsRepository.kt   Llama Directions API y decodifica polyline
```

## Decisiones que vale la pena mencionar

**Rotación de fotos.** `Configuration.ORIENTATION_LANDSCAPE` no distingue si giraste a izquierda o derecha, así que uso `OrientationEventListener` para leer el acelerómetro y fijar `imageCapture.targetRotation` en tiempo real. Sin esto, las fotos salían de cabeza la mitad de las veces.

**Aspecto real de la foto.** En vez de asumir 3:4 o 4:3, leo las dimensiones reales con `BitmapFactory.Options(inJustDecodeBounds = true)` y aplico la rotación EXIF. Así la miniatura coincide siempre con lo capturado.

**Markers con imagen.** `MarkerComposable` captura el composable a bitmap una sola vez. Si metés un `AsyncImage` directo, al momento del capture Coil todavía no cargó y el marker queda vacío. Solución: precargo con `ImageLoader.execute()` y uso el bitmap como parte de los `keys` del `MarkerComposable` para que regenere cuando esté listo. También `allowHardware(false)` porque los markers no aceptan hardware bitmaps.

**Agrupación de markers.** Si hay varias fotos en el mismo punto (bucket de 4 decimales, ~11 m), se dibuja un solo marker apilado con badge de cantidad.

**Ruta.** Al tomar foto, llamo Directions API entre la foto anterior y la nueva, decodifico la polyline con `PolyUtil` y la concateno al `path`. Si falla, cae en línea recta.

## Problemas comunes

**Mapa en blanco.** Revisa que la key esté en `local.properties`, que **Maps SDK for Android** esté habilitada, y la SHA-1 autorizada. Logcat filtrando `Google Maps Android API` te dice el error exacto.

**Ruta en línea recta en vez de por calles.** Falta habilitar **Directions API** o activar billing. Mira logcat con filtro `DirectionsRepo`.

**Ubicación no aparece en emulador.** Extended Controls → Location → setea lat/lng manualmente.

**Foto de cabeza al girar el celular.** Verifica que la rotación automática esté activada en el sistema (si no, `OrientationEventListener` no emite).

## Build release

```bash
./gradlew :app:assembleRelease
```

Si firmás, restringe la API key con la SHA-1 de release, no solo la de debug.

## Dependencias

CameraX 1.3.4, Maps Compose 4.4.1, play-services-location 21.3.0, Coil 2.7.0, Compose BOM 2024.09, android-maps-utils 3.8.2 (para `PolyUtil`), exifinterface 1.3.7, core-splashscreen 1.0.1.

Catalog completo en `gradle/libs.versions.toml`.
