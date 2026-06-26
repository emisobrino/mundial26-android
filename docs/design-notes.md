# Notas de diseño (de screenshots del prototipo deseado)

Detalles observados en las capturas reales que envió el usuario (mandan sobre el README si hay conflicto).
Úsese como referencia fiel al implementar cada pantalla. Tokens de color/tipografía: ver `docs/specs/2026-06-25-mundial26-android-design.md`.

## Convención de nombres de equipo
- **Código (ARG, MEX…):** tarjetas en vivo de Inicio, filas "Resultados & próximos" de Inicio, llaves (bracket), tabla de Grupos (full).
- **Nombre completo (Argentina, México…) + banderas apiladas verticalmente:** Calendario, Partidos (lista En vivo/Próximos), Posiciones compacta de Inicio, Equipo destacado.

## Código de color de estado de partido
- En vivo → minuto en **rojo** (`67'`, `34'`), con punto rojo pulsante.
- Finalizado → `FIN` / `Final` en **gris**.
- Próximo → hora en **azul** (`20:00`).

## Inicio (Home)
- Header blanco: logo "26" + "Mundial 26" / "USA · MÉXICO · CANADÁ" + campana con punto rojo.
- **En vivo:** carrusel horizontal de tarjetas (grupo, badge `● min'` rojo, banderas+códigos, marcador grande centrado, estadio). Link "Ver todo".
- **Resultados & próximos:** filas compactas. Izq = estado/día (Final, Hoy, Mañana, Viernes, Sábado). Centro = código+bandera | marcador o hora | código+bandera.
- **Equipo destacado:** card oscura `#2A2C2C` con franja tricolor superior; bandera grande, tag verde (ej. "Campeón defensor"), nombre grande blanco, "Estrella · <jugador>", chevron →.
- **Posiciones · Grupo B:** tabla **compacta** con columnas `# EQUIPO PJ DG PTS` y **nombres completos**; barra verde top-2; PTS azul bold. Link "Ver tabla".
- **Noticias del Mundial:** lista de tarjetas **compactas** (miniatura cuadrada tintada a la izq + badge categoría + título + hora). Link "Ver todo".

## Partidos
Título "Partidos" + segmentado Partidos / Calendario / Llaves.
- **Partidos (lista):**
  - `EN VIVO`: cards con **borde izquierdo rojo 4px**; nombres completos + banderas apiladas; a la derecha marcador (col) + minuto rojo + grupo.
  - `PRÓXIMOS`: cards con hora azul + día a la izquierda; nombres+banderas apiladas; ronda/grupo arriba a la derecha (Grupo C, Octavos…).
  - `FINALIZADOS`: filas con "Final".
- **Calendario:** agrupado por día con fecha (`HOY 25 jun`, `MAÑANA 26 jun`, `VIERNES 27 jun`…). Cada fila: estado coloreado + grupo/ronda a la izq; nombres completos + banderas apiladas; marcador o `vs` a la derecha.
- **Llaves (bracket):** secciones verticales `OCTAVOS DE FINAL`, `CUARTOS DE FINAL`, `SEMIFINAL`, `FINAL`. Cada cruce = card de 2 filas (bandera + código + marcador). Jugado: ganador en bold/negro `#1C1E1E`, perdedor normal. Pendiente: textos en gris `#9A9E9E` y **sin marcador**.

## Equipos
Título "Selecciones" (siempre) + segmentado Selecciones / Grupos.
- **Selecciones (grilla):** chips pill de filtro (Todos activo azul / UEFA / CONMEBOL / CONCACAF, scroll horizontal); grid 2 columnas. Card: bandera circular arriba-izq, **`#ranking` arriba-der (pill gris)**, nombre bold, confederación (gris, uppercase, pequeño), chip de estilo de juego (fondo claro). Ej: Argentina #1 CONMEBOL "Posesión vertical"; Francia #2 UEFA "Transiciones rápidas"; Brasil #3 CONMEBOL "Ataque por bandas"; España #5 UEFA "Posesión y presión alta"; Inglaterra #4 UEFA "Juego directo estructurado"; México #14 CONCACAF "Intensidad y orden".
- **Grupos (standings):** tablas A–D, header `# EQUIPO PJ G E P DG PTS`; barra verde top-2 (clasifican) / gris resto; **códigos** (MEX, NED…); DG con signo; PTS azul bold. Leyenda inferior: verde = "Clasifican a la siguiente fase".

## Noticias
Título "Noticias" + lista vertical de tarjetas **grandes**: banda de tinte de color arriba a todo el ancho, badge de categoría, título grande bold, resumen, hora.
Categorías con color: `LESIÓN` (rojo), `SORPRESA` (verde), `TÁCTICA` (lavanda/azul). (También: VAR, récords, etc.)

## Detalle de partido
Back + grupo. Scoreboard oscuro con franja tricolor: banderas + marcador 46px + badge `● min'` rojo en vivo + estadio.
- **Estadísticas:** filas valor A (azul, izq) / label (centro) / valor B (der) + barra de proporción (relleno azul sobre pista gris). Ej: Posesión 58-42, Remates 11-6, A puerta 5-2, Córners 6-3, Faltas 8-12.
- **Incidencias:** minuto + bandera del lado + punto de color por tipo (verde Gol, amarillo Amarilla, rojo Roja, azul Cambio) + jugador + etiqueta a la derecha (Gol/Amarilla/…).

## Detalle de equipo
Hero oscuro con franja tricolor: bandera grande, `CONFEDERACIÓN · FIFA #rank`, nombre, chip de estilo.
- **Estrella a destacar:** card con bloque de dorsal azul (con sombra), nombre, posición (azul, ej. "Delantero / Capitán"), nota de scouting, chevron → abre Detalle de jugador.
- **Fortalezas:** lista con check verde.
- **Debilidades:** lista con cruz roja.
- **Forma reciente:** 5 cuadros V (verde) / E (gris) / D (rojo).

## Detalle de jugador
Hero oscuro con bloque de dorsal grande (sombra azul), bandera + selección, nombre, chip de posición (verde sobre oscuro).
- **En el Mundial:** grid 2×2 (Partidos, Goles, Asistencias, Valoración) — número azul grande.
- **Ficha:** filas label/valor (Club, Edad, Posición, Fortaleza).
- **Scouting:** párrafo descriptivo.

## Detalle de noticia
Hero (≈240px) con tinte del color de la categoría, back en círculo translúcido arriba-izq, badge de categoría sobre el borde inferior del hero. Título grande (≈25px/900), línea de autoría (sello "26" + "Redacción Mundial 26 · <hora>"), cuerpo en párrafos.
