# StreamVault (Personal Edition)

<p align="center">
	<a href="https://github.com/AlessandroA586/StreamVault-IPTV/releases/latest"><img src="https://img.shields.io/github/v/release/AlessandroA586/StreamVault-IPTV?display_name=tag&style=for-the-badge&color=0f766e" alt="Latest Release" /></a>
	<a href="https://github.com/Davidona/StreamVault-IPTV"><img src="https://img.shields.io/badge/Upstream-Davidona%2FStreamVault--IPTV-2563eb?style=for-the-badge&logo=github" alt="Upstream Repository" /></a>
	<a href="https://ko-fi.com/davidona"><img src="https://img.shields.io/badge/Support_Author-Ko--fi-ff5f5f?style=for-the-badge&logo=kofi" alt="Support Author on Ko-fi" /></a>
	<a href="LICENSE"><img src="https://img.shields.io/badge/License-StreamVault_OSL-0284c7?style=for-the-badge" alt="License" /></a>
</p>

> [!IMPORTANT]
> ### Reconocimiento de Autoría y Uso Personal (Credits & Personal Disclaimer)
> 
> * **Créditos y Autoría Original:** Este repositorio es una bifurcación (*fork*) personalizada del magnífico proyecto **[StreamVault-IPTV](https://github.com/Davidona/StreamVault-IPTV)**, concebido, diseñado y desarrollado originalmente por **[Davidona (David Nashash)](https://github.com/Davidona)**. Todo el mérito sobre la arquitectura base, interfaz de usuario y capacidades multimedia pertenecen íntegramente a su autor original. Si este software te resulta útil, te invitamos a respaldar su trabajo apoyándolo directamente en **[Ko-fi](https://ko-fi.com/davidona)**.
> * **Propósito del Fork:** Esta versión se mantiene y distribuye con fines **estrictamente privados, personales y educativos**, adaptada para optimizar la experiencia en dispositivos propios sin fines comerciales.
> * **Descargo de Responsabilidad:** StreamVault es un cliente y reproductor multimedia neutral; no proporciona, aloja ni distribuye contenido protegido por derechos de autor. Cada usuario es responsable de los proveedores, listas y emisiones a las que accede.

---

### Mejoras integradas en esta versión (Personal Fork Highlights)

Este fork amplía la experiencia deportiva del reproductor integrando las capacidades de datos de **Nuvio** y fuentes públicas de libre acceso:

* **Hub de Deportes Enriquecido (Zero-Config & Sin Registro):**
  * Marcadores en tiempo real, escudos oficiales de alta definición, estados de partido y calendarios para más de 16 disciplinas deportivas sin necesidad de registrarse ni configurar claves de API.
  * **Deportes y ligas cubiertas:** Fútbol (UEFA Champions/Europa/Conference, LaLiga, Premier League, Serie A, Bundesliga, Ligue 1, Conmebol Libertadores/Sudamericana, Liga MX, MLS, etc.), Baloncesto (NBA, WNBA, NCAA, NBL), Béisbol (MLB, Series del Caribe, Ligas Invernales), Motor (Fórmula 1, NASCAR Cup/Xfinity/Truck, IndyCar), Tenis (ATP y WTA Tour), Artes Marciales (UFC, PFL), Boxeo de campeonato mundial, Lucha Libre (WWE Raw/SmackDown/NXT/PLEs, AEW, TNA), Golf (PGA, LPGA, LIV, DP World Tour), Críquet internacional y Rugby.
  * Sistema de caché inteligente en memoria (`LRU` con TTL adaptativo) que garantiza un rendimiento ultrafluido en Android TV sin peticiones redundantes.
* **Canal de Actualizaciones Independiente:**
  * El actualizador nativo de la aplicación ([`GitHubReleaseChecker.kt`](app/src/main/java/com/streamvault/app/update/GitHubReleaseChecker.kt)) está vinculado a las versiones publicadas en este fork ([AlessandroA586/StreamVault-IPTV](https://github.com/AlessandroA586/StreamVault-IPTV)), garantizando que al actualizar no se pierdan estas personalizaciones ni ocurran conflictos de firmas criptográficas.

---

## Preview

<p align="center">
	<a href="https://github.com/Davidona/StreamVault-IPTV/raw/master/docs/images/LiveTV.png"><img src="docs/images/LiveTV.png" alt="Live TV" width="88%" /></a>
</p>

<p align="center">
	<a href="https://github.com/Davidona/StreamVault-IPTV/raw/master/docs/images/Movies.png"><img src="docs/images/Movies.png" alt="Movies" width="44%" /></a>
	<a href="https://github.com/Davidona/StreamVault-IPTV/raw/master/docs/images/MovieInfo.png"><img src="docs/images/MovieInfo.png" alt="Movie Details" width="44%" /></a>
</p>

<p align="center">
	<a href="https://github.com/Davidona/StreamVault-IPTV/raw/master/docs/images/Sports.png"><img src="docs/images/Sports.png" alt="Sports" width="88%" /></a>
</p>

<p align="center">
	<a href="https://github.com/Davidona/StreamVault-IPTV/raw/master/docs/images/ChannelView.png"><img src="docs/images/ChannelView.png" alt="Channel Preview" width="44%" /></a>
	<a href="https://github.com/Davidona/StreamVault-IPTV/raw/master/docs/images/Guide.png"><img src="docs/images/Guide.png" alt="Guide" width="44%" /></a>
</p>

<p align="center">
	<a href="https://github.com/Davidona/StreamVault-IPTV/raw/master/docs/images/SeriesEpisodes.png"><img src="docs/images/SeriesEpisodes.png" alt="Series Episodes" width="44%" /></a>
</p>

---

## Características Principales de StreamVault

StreamVault es un reproductor para Android TV desarrollado con **Kotlin**, **Jetpack Compose**, **Room**, **Hilt** y **Media3**.

Está diseñado para gestionar listas de gran tamaño, navegación intuitiva con D-pad (mando a distancia), cambio ágil de proveedores y una experiencia de sala de estar de primer nivel:

* **Soporte de Proveedores:**
  * `Xtream Codes`, `Stalker Portal`, `Jellyfin` y listas `M3U` (URL o archivo local).
  * Perfiles combinados M3U para agrupar múltiples listas en una única fuente de TV en Vivo.
  * Sincronización rápida vía código QR desde un teléfono en la misma red local.
* **Guía EPG y Reproductor:**
  * Vista en cuadrícula EPG completa y superposición transparente sobre la reproducción en directo.
  * Soporte de Timeshift, rebobinado en vivo y grabación DVR en segundo plano o almacenamiento en red.
  * Modo multipantalla (*Split Screen*) para visualizar varios canales simultáneamente.
  * Cambio rápido de audios, subtítulos sincronizados (incluyendo búsqueda en OpenSubtitles), control de relación de aspecto y soporte de decodificadores avanzados.
* **Películas y Series (VOD):**
  * Fichas de contenido detalladas, reanudación automática, salto de intros/resúmenes y reproducción consecutiva de episodios.
  * Descargas fuera de línea para ver contenido localmente.
* **Integraciones y Extensiones:**
  * Compatibilidad con complementos Stremio para catálogos y metadatos.
  * Sincronización de historial y listas con **Trakt.tv**.
  * Copias de seguridad automáticas locales y en Google Drive.
  * Control parental con bloqueo de categorías mediante PIN.

---

## Compilación Local

Requisitos:
* Android Studio / Android SDK (API 36)
* JDK 17 (OpenJ9 / Hotspot)

Comandos útiles:

```bash
# Compilar versión de depuración
./gradlew assembleDebug

# Compilar versión final firmada
./gradlew assembleRelease

# Ejecutar pruebas unitarias
./gradlew testDebugUnitTest
```

---

## Créditos y Agradecimientos

* **Autor Original:** [Davidona (David Nashash)](https://github.com/Davidona)
* **Proyecto Upstream:** [Davidona/StreamVault-IPTV](https://github.com/Davidona/StreamVault-IPTV)
* **Apoyo al creador:** [Ko-fi](https://ko-fi.com/davidona)

---

## Licencia

El proyecto original está sujeto a la licencia **StreamVault Source-Available License (Non-Commercial)**. Cualquier uso, redistribución o modificación debe cumplir rigurosamente con los términos establecidos en el archivo [LICENSE](LICENSE).
