# Diseño — App Android "Mundial 26"

**Fecha:** 2026-06-25
**Estado:** Aprobado para plan de implementación
**Origen del diseño:** Handoff de Claude Design en `C:\Users\emiso\OneDrive\Desktop\design_handoff_mundial2026` (prototipo HTML `Mundial 2026.dc.html`, README de handoff y capturas en `screens/`).

## 1. Objetivo

Construir una app Android nativa (Kotlin + Jetpack Compose + Material 3) que recree con **alta fidelidad** el prototipo "Mundial 26" — app informativa del Mundial 2026 (USA · México · Canadá): resultados en vivo, calendario, llaves, tabla de posiciones por grupos, selecciones, fichas de jugador y noticias. Conectar datos reales de deportes en una fase posterior.

**Criterio de aceptación de diseño:** cada pantalla debe verse **igual o mejor** que su captura de referencia en `screens/`. Los tokens de diseño (color, tipografía, radios, sombras, espaciado) del README se reproducen 1:1.

## 2. Decisiones tomadas (brainstorming)

- **Entorno:** Android Studio + SDK ya instalados (compila/corre en emulador o dispositivo).
- **Fuente de datos real:** **API-Football** (API-Sports, api-football.com). FIFA no expone API pública. Plan gratuito (~100 req/día). Integración en **fase 2**.
- **Datos editoriales (no vienen de API):** fortalezas/debilidades por equipo, nota de scouting de jugador y **noticias** → contenido propio en la capa de datos.
- **Banderas:** **CDN remoto** `flagcdn.com` vía Coil (con placeholder de código, ej. "ARG", mientras carga / sin señal). Coil también sirve para fotos de jugadores y noticias.
- **Alcance v1 = rebanada vertical:** las 4 pantallas principales (Inicio, Partidos, Equipos, Noticias) con navegación, theming fiel y **datos mock**. Detalles y API real en fases siguientes.

## 3. Stack y librerías

| Capa | Elección |
|------|----------|
| UI | Jetpack Compose + Material 3 |
| Navegación | Navigation-Compose (`NavHost`) |
| Estado | `ViewModel` + `StateFlow` (un VM por sección) |
| Datos remotos (fase 2) | Retrofit + OkHttp + kotlinx-serialization |
| Imágenes | Coil (banderas, fotos) |
| Inyección de dependencias | Manual (`AppContainer`); Hilt solo si crece |
| Build | Gradle (Kotlin DSL) + version catalog (`libs.versions.toml`) |

- `minSdk` 26, `targetSdk` 35.
- Package: `com.mundial26.app` (a confirmar en revisión).
- Una sola Activity, 100% Compose.

## 4. Sistema de diseño (theming)

`ui/theme/` mapea los tokens del README:

- **Color:** `ColorScheme` M3 + colores semánticos extra expuestos por un CompositionLocal `LocalMundialColors` (`MundialTheme.colors.live`, `.win`, `.warn`, etc.). Valores: primario `#2A398D`, en vivo/alertas `#E61D25`, victorias/clasificados `#3CAC3B`, amarilla `#E8B500`, texto `#1C1E1E`/`#474A4A`/`#6B6F6F`/`#9A9E9E`, fondo `#F2F3F3`, superficie `#FFFFFF`, superficie oscura `#2A2C2C`, bordes `#EAEBEB`/`#E2E4E2`, etc.
- **Tipografía:** fuente **Archivo** empaquetada en `res/font/` (no Google Fonts en runtime). `Typography` con pesos 500–900 y letter-spacing negativos (títulos -0.6px, marcador grande -2px).
- **Formas:** radios 12–20px como tokens.
- **Componentes reutilizables** (`ui/components/`): `FlagAvatar`, `BrandLogo26` (sello "26" italic + franja tricolor verde/rojo/azul), `SegmentedControl`, `MatchRow`, `LiveMatchCard`, `StandingsTable`, `NewsCard`, `SectionHeader`, `BottomNav`, `LivePulseDot`.

**Mejoras sobre el prototipo:** banderas reales, micro-animaciones nativas (pulso en vivo, transiciones de navegación, ripple/elevación en tarjetas tocables), respeto de `WindowInsets`.

## 5. Navegación y estado

- 1 Activity → `MundialApp` con `Scaffold` + `NavigationBar` (Inicio, Partidos, Equipos, Noticias).
- `NavHost` de 2 niveles:
  - Destinos raíz: las 4 pestañas (back-stack propio).
  - Detalles encima de la pestaña activa: `matchDetail/{id}`, `teamDetail/{id}`, `playerDetail/{id}`, `newsDetail/{id}`; vuelven con chevron atrás. El id viaja como argumento de ruta (reemplaza `view`+`matchId/...` del prototipo).
- Estado por pantalla en ViewModels con `StateFlow`:
  - Partidos: `matchesView` (list / calendario / bracket).
  - Equipos: `teamsView` (selecciones / grupos) + `conf` (filtro confederación: all/uefa/conmebol/concacaf).
  - Todas exponen `uiState`: Loading / Success / Error.
- **Estados de carga/error agregados** (skeletons al cargar, mensaje + reintento al fallar) — no estaban en el prototipo, obligatorios con datos remotos.

## 6. Arquitectura de datos (3 capas, dependencia unidireccional UI → dominio ← datos)

```
com.mundial26.app
├── ui/
│   ├── theme/        (Color, Type, Shape, MundialColors)
│   ├── components/   (FlagAvatar, BrandLogo26, SegmentedControl, …)
│   ├── home/         (HomeScreen + HomeViewModel)
│   ├── matches/      (MatchesScreen + VM, MatchDetail)
│   ├── teams/        (TeamsScreen + VM, TeamDetail, PlayerDetail)
│   ├── news/         (NewsScreen + VM, NewsDetail)
│   └── navigation/   (NavHost, rutas, BottomNav)
├── domain/
│   ├── model/        (Team, Match, Group, BracketGame, Player, NewsItem)
│   └── repository/   (MatchRepository, TeamRepository, NewsRepository)  ← interfaces
└── data/
    ├── mock/         (Fake repos con datos del prototipo)   ← v1
    ├── remote/       (Retrofit API-Football: DTOs, service, mappers) ← fase 2
    ├── editorial/    (fortalezas/debilidades, scouting, noticias propias)
    └── di/           (AppContainer: arma e inyecta repos)
```

**Regla clave:** la UI solo conoce los modelos de `domain/` y las interfaces `Repository`. En v1 `AppContainer` inyecta implementaciones **mock**; en fase 2 se cambian por las de `remote/` (Retrofit) **sin tocar la UI**. Los datos editoriales se combinan con los de la API dentro de `data/`, nunca en la UI.

### Modelo de dominio (del README)
- `Team`: id, name, code, conf, confId, rank, tag, flag, styleLabel, star{num,name,pos,note}, pros[], cons[], form[].
- `Match`: id, status (live/upcoming/finished), minute, time, day, group, stadium, teamA, teamB, scoreA, scoreB, stats[{label,a,b}], events[{min,kind,side,player}].
- `Group`: name, rows[{code,pj,g,e,p,gf,gc,pts}].
- `BracketGame`: round, games[{a,b,sa,sb,done}].
- `NewsItem`: id, tag, color, time, title, summary, body[].
- `StarStats` (por equipo): pj, g, a, rt, club, edad, val.

### Mapeo a API-Football (fase 2)
- En vivo / marcadores / minuto → fixtures live (polling).
- Tabla de grupos → standings.
- Calendario → fixtures por fecha.
- Eliminatorias → fixtures de fase final → bracket.
- Equipos / jugadores (stats) → teams / players / topscorers.
- Pros/contras, scouting y noticias → editorial propio (no API).

## 7. Pantallas (del handoff)

Las 4 raíz (v1) + 4 detalles (fases siguientes), según `screens/01-inicio.png` … `11-detalle-noticia.png`:
1. Inicio · 2. Partidos (list/calendario/llaves) · 3. Equipos (selecciones/grupos) · 4. Noticias · 5. Detalle partido · 6. Detalle equipo · 7. Detalle jugador · 8. Detalle noticia.

## 8. Fases de entrega

- **v1 (rebanada vertical):** proyecto Gradle compilando; theme M3 fiel; 4 pantallas raíz + bottom nav + segmentados; datos mock; carga/error. Verificación visual contra `screens/`.
- **v2 (detalles):** las 4 pantallas de detalle + navegación con argumentos.
- **v3 (API real):** integración API-Football detrás de los `Repository`, capa editorial, banderas/fotos vía Coil ya activas.

## 9. Skills a aplicar
- `source-driven-development` (+ Context7 MCP) para anclar patrones de Compose/Retrofit/Coil a doc oficial.
- `api-and-interface-design` para los límites de capa y el contrato `Repository`.
- `writing-plans` para el plan de implementación (paso siguiente).
- `frontend-ui-engineering` adaptada (principios, no patrones web literales) en la fase de UI.

## 10. Fuera de alcance (YAGNI por ahora)
- Hilt (DI manual hasta que justifique).
- Modo oscuro completo (el diseño es claro; superficie oscura es solo de héroes/scoreboard).
- Tests instrumentados de UI exhaustivos (sí tests de mappers/VM cuando entre la API).
- Notificaciones push reales (la campana es visual en v1).
