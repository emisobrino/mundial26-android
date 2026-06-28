# Mundial 26 — v3 (Datos reales) Design / Spec

**Fecha:** 2026-06-27 · **Fase:** v3 (API real) de `docs/specs/2026-06-25-mundial26-android-design.md`

## 1. Objetivo

Reemplazar los datos mock por **datos reales del Mundial 2026** detrás de las interfaces `Repository` existentes, **sin tocar la UI** (Home, Partidos, Equipos, Noticias, los 4 detalles y el widget de líderes). Costo **$0**.

## 2. Decisión de fuentes (testeada con keys reales, 2026-06-27)

Tras probar 4 keys, **ningún free tier daba WC2026 usable** salvo football-data.org:
- API-Football free → solo 2022-2024 (no 2026). footballdata.io free → "World Cup" devuelve basura (clubes de Camerún). Sportmonks free → ni Mundial.

**Stack final (gratis, WC2026 real):**

| Capa | Fuente | Da |
|---|---|---|
| **Primaria** | **football-data.org** v4 (token `X-Auth-Token`, comp `WC`, temporada 2026 confirmada) | standings (12 grupos), scorers (goles + assists parcial), matches (estado/score), 48 teams |
| **Eventos de gol** | **openfootball** (`worldcup.json/2026`, sin key) | goles con minuto por partido → timeline del detalle; seed offline |
| **Editorial** | local (lo que ya está en `MockData`) | pros/contras, noticias, `marketValue`, nota/`styleLabel` de estrella, conf, rank, stats de posesión, stadium |

**Huecos sin fuente gratis 2026 (→ editorial o degradar):** minuto en vivo real-time, stats de partido (posesión/remates), perfil de jugador (club/edad/valor/rating), asistidores completos. La UI ya degrada: badge "EN VIVO" sale del `status`, no del minuto.

## 3. Arquitectura

MVVM + Repository de v1/v2 intacto. `AppContainer` cambia las impl `Fake*` por `Default*` (las `Fake*` quedan para tests/previews). Reconciliación entre fuentes por **TLA / código de equipo** (`Team.code`).

```
data/
├── remote/footballdata/
│   ├── FootballDataApi.kt        (Retrofit service)
│   ├── dto/                      (DTOs kotlinx-serialization)
│   └── FootballDataMappers.kt    (DTO → dominio)
├── structural/openfootball/
│   ├── OpenFootballApi.kt        (GET raw JSON)
│   └── OpenFootballMappers.kt    (goles+minuto → MatchEvent)
├── editorial/EditorialSource.kt  (datos curados; hoy en MockData)
├── local/Cache.kt                (caché en memoria por TTL/“temperatura”)
├── TlaToIso.kt                   (mapa TLA→ISO-2 para flagcdn, 48 entradas)
└── repository/
    ├── DefaultTeamRepository.kt
    ├── DefaultMatchRepository.kt
    └── DefaultNewsRepository.kt
```

## 4. Mapeos DTO → dominio (campos reales verificados)

**Match** ← `GET /v4/competitions/WC/matches` → `matches[]`:
- `id.toString()` → `Match.id`
- `status` → `MatchStatus`: `FINISHED`→FINISHED; `TIMED|SCHEDULED|POSTPONED`→UPCOMING; `IN_PLAY|PAUSED|LIVE`→LIVE
- `minute` → `null` (no en free) → badge live por `status`
- `utcDate` → `time` (HH:mm local) y `day` (Hoy/Mañana/fecha)
- `group` (`"GROUP_A"`→`"Grupo A"`) o `stage` (`LAST_16`→"Octavos", etc.) → `Match.group`
- `homeTeam.tla`/`awayTeam.tla` → `teamA`/`teamB`; `flagA`/`flagB` ← `TlaToIso(tla)`
- `score.fullTime.home`/`.away` → `scoreA`/`scoreB`
- `stats` = `emptyList()` (→ editorial); `events` ← openfootball (match por equipos+fecha); `stadium` ← editorial/`null`

**Group / GroupRow** ← `GET /v4/competitions/WC/standings` → `standings[]` (filtrar `type=="TOTAL"`):
- `group` (`"Group A"`→`"Grupo A"`) → `Group.name`
- por fila: `team.tla`→`code`, `TlaToIso(tla)`→`flag`, `playedGames`→`pj`, `won`→`g`, `draw`→`e`, `lost`→`p`, `goalsFor`→`gf`, `goalsAgainst`→`gc`, `points`→`pts` (`dg` se computa)

**Team** ← `GET /v4/competitions/WC/teams` → `teams[]` (48):
- `tla.lowercase()`→`id`, `name`→`name`, `tla`→`code`, `TlaToIso(tla)`→`flag`
- `conf`, `rank`, `styleLabel`, `star`, `pros`, `cons`, `form` ← **editorial** (football-data no da FIFA rank ni scouting)

**Leader (goleadores/asistidores)** ← `GET /v4/competitions/WC/scorers?limit=10`:
- goleadores: `player.name`→`name`, `TlaToIso(team.tla)`→`flag`, `goals`→`value`
- asistidores: ordenar por `assists` (parcial; si `null`, completar/ordenar con editorial)

**NewsItem / StarStats** ← **editorial** (ninguna fuente gratis los da).

**Eventos** ← openfootball `goals1/goals2` (`{name, minute}`) → `MatchEvent(min, GOAL, side, player)`; fix encoding `latin1→utf8`. (Tarjetas/cambios no disponibles → solo goles.)

## 5. Transversal

- **Deps nuevas:** Retrofit, OkHttp (+logging interceptor), kotlinx-serialization (+converter). Sin Room: **caché en memoria por TTL**.
- **Config/keys:** `local.properties` → `FOOTBALL_DATA_TOKEN` → `BuildConfig.FOOTBALL_DATA_TOKEN` (gradle `buildFeatures { buildConfig = true }` + `buildConfigField`). `local.properties` ya está en `.gitignore`. Header `X-Auth-Token`.
- **Caché por temperatura:** teams/openfootball (frío) larga; standings/scorers (tibio) ~horas; matches (caliente) ~minutos en ventana de partido. Free = 10 req/min → cachear agresivo.
- **Cadena de fallback:** repo intenta API → caché → editorial/seed. Los `ViewModel` ya manejan Loading/Error/retry.
- **Reconciliación:** openfootball ↔ football-data por (equipos + fecha). Dominio canónico keyed por `code`/TLA.

## 6. Tests

- Unit de **mappers** (DTO→dominio) con **fixtures JSON capturados** (un match FINISHED, una fila de standings, un scorer con/sin assists). Sin red.
- Reconciliación openfootball↔football-data y la lógica de fallback con services fake.

## 7. Fases (incremental; la app queda funcionando en cada paso)

1. **Infra:** deps + `BuildConfig` token + cliente Retrofit/kotlinx base.
2. **football-data → equipos/grupos/goleadores:** service + DTOs + mappers + `TlaToIso`; `DefaultTeamRepository` (teams, groups, topScorers/topAssists). Swap en `AppContainer`. Verificar Equipos + widget de líderes con datos reales.
3. **football-data → partidos:** mapper de matches → `DefaultMatchRepository` (calendario, resultados, live-por-status, bracket por `stage`).
4. **openfootball → eventos:** goles+minuto en el detalle de partido; seed offline.
5. **Editorial:** extraer los campos editoriales de `MockData` a `EditorialSource` y combinarlos en los repos (pros/contras, noticias, marketValue, estrella, styleLabel, posesión).
6. **Caché por temperatura + fallback** afinados.
7. **Verificación final** en emulador vs `screens/`, con datos reales.

## 8. Fuera de alcance (YAGNI)

- Minuto en vivo real-time y stats de partido vía API (no hay free 2026) — quedan editorial/degradado.
- Endpoints pagos (odds, predictions, player stats detallados).
- Push reales, Room/persistencia en disco (caché en memoria alcanza).

## 9. Definición de "Hecho" (v3)

- Las pantallas existentes muestran **datos reales WC2026** (grupos, calendario/resultados, goleadores/asistidores, equipos) detrás de los `Repository`, sin cambios de UI.
- `season`/comp configurable; cambiar a futuras ediciones o a un plan pago = config, no refactor.
- Tests de mappers en verde; app degrada con elegancia donde no hay dato gratis.
