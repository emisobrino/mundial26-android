# Design

Sistema visual de Mundial 26 (rediseño v4, tema oscuro completo). Fuente de verdad: `app/src/main/java/com/mundial26/app/ui/theme/` (Color.kt, Type.kt, Shape.kt). La UI consume tokens vía `MundialTheme.colors` — nunca colores literales en pantallas.

## Theme

Oscuro único (sin variante clara). Escena: hincha mirando el partido de noche, teléfono en mano, TV encendida. Tres niveles de superficie:

| Token | Valor | Uso |
|---|---|---|
| `appBg` | `#121414` | Fondo de app |
| `surface` | `#1D2020` | Tarjetas y filas |
| `surfaceDark` | `#2A2C2C` | Tarjetas destacadas (hero, live, líderes) — el "negro" identidad, ahora nivel elevado |
| `tableHeaderBg` | `#242727` | Encabezados de tabla |
| `segmentInactive` | `#232626` | Pista de segmentados |
| `border` | `#303434` | Divisores y pistas de barras |

## Color

Estrategia: **Restrained** sobre oscuro; el color es estado, no decoración.

| Token | Valor | Uso |
|---|---|---|
| `brand` (azul claro) | `#8B99F0` | Acentos de TEXTO sobre oscuro: horas, PTS, valores de stats, links |
| `brandDeep` (azul identidad) | `#2A398D` | RELLENOS con texto blanco: segmentado activo, botones, dorsal, toggle |
| `live` | `#E61D25` | En vivo, rojas, derrota. Píldora de minuto en rojo sólido |
| `win` | `#3CAC3B` | Victoria, clasificado, entra (cambio) |
| `yellow` | `#E8B500` | Amarillas, empate |
| `textPrimary` | `#F4F5F5` | Texto principal |
| `textSecondary` | `#C6C9C9` | Secundario |
| `textTertiary` | `#969C9C` | Etiquetas de sección, metadatos |
| `textFaint` | `#6C7272` | Deshabilitado, "A definir" |

**Franja tricolor** (win/live/brandDeep, 4dp): sello del torneo. Va en: header de app, tarjetas destacadas (hero de detalle, live, líderes, campeón defensor, stats oficiales). No va en tarjetas comunes.

## Typography

Una sola familia: **Archivo variable** (500–900). Escala fija:

- Título de pantalla: 28sp / 900 / -0.6 (`headlineSmall`)
- Título de sección: 15sp / 900 (`titleMedium`); etiqueta de sección 13sp / 900 / +0.3 en mayúsculas (`labelLarge`, color textTertiary)
- Cuerpo: 15sp/600 (`bodyLarge`), 14sp/500 (`bodyMedium`)
- Marcador detalle: 46sp / 900 / -2 (`ScoreboardBig`); marcador tarjeta live: 34sp / 900 (`ScoreboardCard`)
- Micro: 11sp/800 (`labelSmall`), 9sp/700 (`MicroLabel`)

El marcador es siempre el elemento más pesado de su tarjeta.

## Layout

- Margen de pantalla: 16dp. Entre secciones: 24dp. Título→contenido: 12dp. Interior de tarjeta: 14–16dp.
- Radios: tarjetas 16dp (`medium`), destacadas 20dp (`large`), chips/píldoras 6–20dp según tamaño.
- Listas densas permitidas (posiciones, planteles); el aire se gasta entre secciones, no dentro de las tablas.

## Components

- **Tarjeta destacada**: surfaceDark + franja tricolor arriba + texto blanco. (LiveMatchCard, hero de detalle, TournamentLeadersCard, FeaturedTeamCard, StatsSection.)
- **Fila de partido** (`MatchRow`): marcador/hora en brand claro o blanco bold; equipos con bandera; "A definir" en textFaint.
- **Píldora live** (`LiveMinutePill`): rojo sólido, minuto centrado, blanco 900.
- **Segmentado**: pista segmentInactive, activo brandDeep con texto blanco.
- **Estados**: cargando = spinner centrado en brand claro; error = mensaje + botón Reintentar; vacío = texto en textTertiary que explica ("Sin datos disponibles todavía").
- **Eventos de partido**: ⚽ gol, mini-tarjeta amarilla/roja, ↑verde/↓roja para cambios.

## Motion

- Transiciones 150–250 ms, ease-out. Nada decorativo.
- `LivePulseDot`: único pulso permanente (estado en vivo).
- Pagers (grupos, llaves, live) con snap nativo de HorizontalPager.
