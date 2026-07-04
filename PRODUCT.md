# Product

## Register

product

## Users

Hinchas de fútbol (usuario principal: Uruguay, es-419) siguiendo la Copa Mundial 2026 desde el teléfono, muchas veces mientras miran el partido por TV de noche. Entran varias veces al día en ráfagas cortas: ¿quién juega hoy y a qué hora (en MI hora)?, ¿cómo va el partido en vivo?, ¿cómo quedó la tabla?, ¿quién es el goleador?

## Product Purpose

App Android nativa (Kotlin + Compose) de seguimiento del Mundial 2026 con datos reales y oficiales: partidos en vivo (minuto/marcador FIFA), resultados, calendario en hora local, llaves de eliminatorias, posiciones, planteles con ficha por jugador, líderes del torneo y noticias. Éxito = la respuesta está a un vistazo y los datos son tan frescos como los de Google.

## Brand Personality

Cancha de noche, oficial, contundente. La app se siente como el estadio bajo los focos: fondo oscuro, marcadores que gritan, la franja verde/rojo/azul como sello del torneo. Datos con autoridad (fuente FIFA), cero adorno.

## Anti-references

- Genérico/plano: una lista más de partidos que podría ser cualquier app de resultados.
- Sopa de widgets desparejos: espaciados y jerarquías distintas por pantalla.
- Marcadores tímidos: el resultado nunca puede ser el texto más chico de la tarjeta.
- Cream/beige claro de app de noticias; el torneo es de noche y bajo focos.

## Design Principles

1. **El marcador manda**: en cualquier tarjeta de partido, el dato más grande es el resultado (o la hora si no se jugó). Todo lo demás lo sirve.
2. **Una sola oscuridad**: un solo sistema de superficies (fondo → tarjeta → tarjeta destacada) en toda la app; la tarjeta negra deja de ser acento y pasa a ser el idioma.
3. **La franja tricolor es el sello**: verde/rojo/azul marca lo que es "del torneo" (héroes, live, destacados). Se usa con intención, no en todo.
4. **Ritmo fijo**: 16dp de margen de pantalla, 24dp entre secciones, 12dp de título a contenido. Sin excepciones por pantalla.
5. **Estado por color, no decoración**: rojo = en vivo, verde = victoria/entra, amarillo = amonestado, azul = marca/acción. Nunca color porque sí.

## Accessibility & Inclusion

- Contraste AA (≥4.5:1) para texto de cuerpo sobre las superficies oscuras; los grises "elegantes" que no lleguen, se aclaran.
- El acento azul de texto sobre fondo oscuro usa la variante clara del azul marca (el azul profundo queda para rellenos con texto blanco).
- Estados live no dependen solo del color: píldora + minuto + punto pulsante.
