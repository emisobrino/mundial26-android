# Mundial 26 — v3 (Datos reales) Design / Spec

**Fecha:** 2026-06-27 · **Fase:** v3 (API real) de `docs/specs/2026-06-25-mundial26-android-design.md`

## 1. Objetivo

Reemplazar los datos mock por **datos reales del Mundial 2026** detrás de las interfaces `Repository` existentes, **sin tocar la UI** (Home, Partidos, Equipos, Noticias, los 4 detalles y el widget de líderes). Costo **$0**, sin infra propia.

## 2. Decisión de fuentes (testeada con keys/requests reales, 2026-06-27/28)

Tras probar todo: API-Football free (solo 2022-2024), footballdata.io free (WC2026 = basura), Sportmonks free (sin Mundial), Sofascore (**403 Cloudflare**, bloquea scraping). Lo que **funciona, gratis y para WC2026**:

| Capa | Fuente | Da (real 2026) | Estado |
|---|---|---|---|
| **API primaria** | **football-data.org** v4 (token `X-Auth-Token`, comp `WC`, season 2026) | 12 grupos, calendario/resultados, goleadores, 48 equipos, **plantel (squad)**, live (estado+score), bracket, assists parcial | ✅ testeado |
| **Eventos de gol** | **openfootball** (`worldcup.json/2026`, sin key) | goles + minuto por partido → timeline; seed offline | ✅ testeado |
| **Noticias** | **RSS Google News** (`news.google.com/rss/search?q=mundial+2026`) | titular + fuente + hora + link | ✅ testeado (111 ítems) |
| **Editorial** | local (hoy en `MockData`) | pros/contras, `marketValue`, rating/club de estrella, `styleLabel`, conf, rank, posesión/stats de partido | — |

**Minuto en vivo:** NO se scrapea — se **deriva del kickoff** (`utcDate` + `status=IN_PLAY`): `min ≈ (now − kickoff)` con tope ~90+ y ajuste de entretiempo. Gratis, confiable, aproximado.

**Fuera del core (opcional futuro, NO se construye en v3):** scraping de Sofascore/Google para minuto exacto, stats de partido y asistidores completos — Sofascore da 403 (Cloudflare), Google tiene anti-bot; requerirían proxies/servicio pago. La app **no depende** de scraping.

## 3. Arquitectura

MVVM + Repository de v1/v2 intacto. `AppContainer` cambia las impl `Fake*` por `Default*` (las `Fake*` quedan para tests/previews). Reconciliación entre fuentes por **TLA / código de equipo** (`Team.code`); openfootball↔football-data por (equipos + fecha).

```
data/
├── remote/footballdata/
│   ├── FootballDataApi.kt        (Retrofit service)
│   ├── dto/                      (DTOs kotlinx-serialization)
│   └── FootballDataMappers.kt    (DTO → dominio)
├── remote/openfootball/
│   ├── OpenFootballApi.kt        (GET raw JSON 2026)
│   └── OpenFootballMappers.kt    (goles+minuto → MatchEvent; fix latin1→utf8)
├── remote/news/
│   └── NewsRssSource.kt          (fetch + parse RSS → NewsItem)
├── editorial/EditorialSource.kt  (datos curados; hoy en MockData)
├── local/Cache.kt                (caché en memoria por TTL/“temperatura”)
├── TlaToIso.kt                   (mapa TLA→ISO-2 para flagcdn, 48 entradas)
└── repository/
    ├── DefaultTeamRepository.kt
    ├── DefaultMatchRepository.kt
    └── DefaultNewsRepository.kt
```

## 4. Mapeos → dominio (campos reales verificados)

**Match** ← football-data `/v4/competitions/WC/matches` → `matches[]`:
- `id.toString()`→`id`; `status`→`MatchStatus` (`FINISHED`→FINISHED; `TIMED|SCHEDULED|POSTPONED`→UPCOMING; `IN_PLAY|PAUSED|LIVE`→LIVE)
- `minute` **derivado**: si LIVE, `((now − utcDate).minutes).coerceIn(1,120)` (ajuste HT); si no, `null`
- `utcDate`→`time` (HH:mm local) y `day` (Hoy/Mañana/fecha); `group`/`stage`→`Match.group` (`"GROUP_A"`→"Grupo A", `LAST_16`→"Octavos", …)
- `homeTeam.tla`/`awayTeam.tla`→`teamA`/`teamB`; `flagA`/`flagB`←`TlaToIso(tla)`; `score.fullTime.home/away`→`scoreA`/`scoreB`
- `stats`=editorial/`[]`; `events`←openfootball (match por equipos+fecha); `stadium`← match detail `venue` si existe, si no editorial/`null`

**Group / GroupRow** ← `/v4/competitions/WC/standings` (`type=="TOTAL"`): `group`→`Group.name`; por fila `team.tla`→`code`, `TlaToIso`→`flag`, `playedGames`→`pj`, `won`→`g`, `draw`→`e`, `lost`→`p`, `goalsFor`→`gf`, `goalsAgainst`→`gc`, `points`→`pts` (`dg` computado).

**Team** ← `/v4/competitions/WC/teams` → `teams[]` (48): `tla.lowercase()`→`id`, `name`, `tla`→`code`, `TlaToIso`→`flag`. `conf/rank/styleLabel/pros/cons/form`←**editorial**.

**Jugadores / Estrella** ← team `squad[]` (nombre, `position`, `dateOfBirth`→edad, nacionalidad) + scorers (goles/assists si aplica). `Star.stats`: `pj/goals/assists`←scorers (parcial), `age`←dob, `club/rating/marketValue/note`←**editorial**.

**Leader (goleadores/asistidores)** ← `/v4/competitions/WC/scorers?limit=10`: goleadores `player.name`/`TlaToIso(team.tla)`/`goals`; asistidores ordenar por `assists` (parcial → completar/ordenar con editorial). `rank`=índice+1.

**NewsItem** ← **RSS Google News**: `<title>`→`title`, `<source>`→tag/fuente, `<pubDate>`→`time`, `<link>`→URL (abrir en navegador). `summary`←`<description>`. La pantalla de detalle muestra resumen + "Leer la nota" (link-out); `body` deja de ser editorial. (Editorial puede aportar 1-2 notas destacadas si se quiere.)

**Eventos** ← openfootball `goals1/goals2` (`{name, minute}`) → `MatchEvent(min, GOAL, side, player)` (fix encoding). Tarjetas/cambios no disponibles gratis → no se muestran.

## 5. Transversal

- **Deps:** Retrofit, OkHttp (+logging), kotlinx-serialization (+converter). RSS: parser XML simple. Caché **en memoria por TTL** (sin Room).
- **Config/keys:** `local.properties`→`FOOTBALL_DATA_TOKEN`→`BuildConfig.FOOTBALL_DATA_TOKEN` (`buildFeatures{buildConfig=true}`+`buildConfigField`). `local.properties` ya gitignored. Header `X-Auth-Token`. openfootball/RSS sin key.
- **Caché por temperatura:** teams/openfootball (frío) larga; standings/scorers/news (tibio) ~horas; matches (caliente) ~minutos en ventana de partido. Free = 10 req/min → cachear agresivo.
- **Cadena de fallback:** repo intenta API → caché → editorial/seed. `ViewModel` ya manejan Loading/Error/retry.

## 6. Tests

- Unit de **mappers** (DTO→dominio) con **fixtures JSON capturados** (match FINISHED/IN_PLAY, fila de standings, scorer con/sin assists, team con squad, ítem RSS). Sin red.
- Derivación de minuto (kickoff→minuto) y reconciliación openfootball↔football-data con datos fake.

## 7. Fases (incremental; la app funciona en cada paso)

1. **Infra:** deps + `BuildConfig` token + cliente Retrofit/kotlinx base.
2. **football-data → equipos/grupos/goleadores/plantel:** service + DTOs + mappers + `TlaToIso`; `DefaultTeamRepository`. Swap en `AppContainer`. Verificar Equipos + widget de líderes reales.
3. **football-data → partidos:** mapper de matches → `DefaultMatchRepository` (calendario, resultados, **live por status + minuto derivado**, bracket por `stage`).
4. **openfootball → eventos:** goles+minuto en el detalle de partido; seed offline.
5. **Noticias RSS:** `NewsRssSource` + `DefaultNewsRepository` (titular/fuente/hora/link); detalle con link-out.
6. **Editorial:** extraer campos editoriales de `MockData` a `EditorialSource` y combinarlos (pros/contras, marketValue, rating/club estrella, styleLabel, conf/rank, posesión).
7. **Caché por temperatura + fallback** afinados.
8. **Verificación final** en emulador vs `screens/`, con datos reales.

## 8. Fuera de alcance (YAGNI)

- **Scraping de live** (Sofascore 403/Cloudflare, Google anti-bot): minuto exacto, stats de partido (posesión/remates), tarjetas/cambios, asistidores completos → minuto **derivado**, stats/asist. **editorial**. Scraping queda como experimento opcional on-device a futuro, fuera del core.
- Endpoints pagos (odds, predictions, player stats detallados); Room/persistencia en disco; push reales.

## 9. Definición de "Hecho" (v3)

- Las pantallas existentes muestran **datos reales WC2026** (grupos, calendario/resultados, goleadores, equipos, plantel, live estado+score+minuto-derivado, bracket, noticias RSS) detrás de los `Repository`, sin cambios de UI.
- `comp`/`season` configurable. Tests de mappers en verde; la app degrada con elegancia donde no hay dato gratis (stats de partido, rating/valor → editorial).
