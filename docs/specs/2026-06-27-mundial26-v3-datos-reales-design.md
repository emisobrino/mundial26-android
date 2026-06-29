# Mundial 26 — v3 (Datos reales) Design / Spec

**Fecha:** 2026-06-27 (rev. 2026-06-28: persistencia Room) · **Fase:** v3 de `docs/specs/2026-06-25-mundial26-android-design.md`

## 1. Objetivo

Reemplazar los datos mock por **datos reales del Mundial 2026** detrás de las `Repository`, **sin tocar la UI** (salvo link-out de noticias y, opcional, pull-to-refresh). Costo **$0**, sin infra propia. Arquitectura **DB local (Room) + sincronización**, no API on-demand.

## 2. Fuentes (testeadas, 2026-06-27/28)

Único free que da WC2026 real: **football-data.org** (API-Football free no tiene 2026; footballdata.io 2026 = basura; Sportmonks free sin Mundial; Sofascore = 403 Cloudflare).

| Dato | Fuente | Rol |
|---|---|---|
| grupos, calendario/resultados, goleadores, 48 equipos, **planteles (squad)**, live (estado+score), assists parcial | **football-data.org** v4 (token `X-Auth-Token`, comp `WC`) | sincroniza a la DB |
| goles + minuto por partido | **openfootball** (`worldcup.json/2026`, sin key) | timeline del detalle |
| noticias | **RSS Google News** | titulares + link-out |
| pros/contras, marketValue, rating/club estrella, styleLabel, conf, rank, posesión | **editorial** (local) | mezcla en seed |

**Minuto en vivo:** derivado del kickoff (`utcDate`+`status=IN_PLAY`), no scraping. **Fuera del core:** scraping (Sofascore/Google) de stats de partido/tarjetas → editorial o ausente.

## 3. Arquitectura: DB local + sync

**Por qué:** los 48 países y sus planteles son **lista fija** (se siembran una vez); goleadores/asistidores/posiciones **cambian por fecha** → un **sync** los refresca en la DB y la UI lee de la DB (rápido, offline, pocas requests). Los rankings se **derivan** de la DB (`ORDER BY goals/assists`), no de un endpoint volátil.

```
data/
├── local/db/
│   ├── entities/   TeamEntity, PlayerEntity, StandingEntity, MatchEntity, SyncMeta
│   ├── dao/        TeamDao, PlayerDao, StandingDao, MatchDao, SyncMetaDao
│   ├── MundialDatabase.kt          (RoomDatabase)
│   └── DbMappers.kt                (Entity ↔ dominio)
├── sync/SyncManager.kt             (football-data → upsert entidades; controla frescura)
├── remote/footballdata/            (DTOs + service + DTO→Entity)
├── remote/openfootball/            (eventos de gol)
├── remote/news/NewsRssSource.kt    (RSS → NewsItem)
├── editorial/EditorialSource.kt    (campos curados, keyed por TLA)
├── net/{HttpClients,TlaToIso}.kt
└── repository/Default{Team,Match,News}Repository.kt   (leen de DAOs; disparan sync si stale)
```

**Dominio (cambios):**
- **NUEVO** `Player(id, name, teamCode, position, age, goals, assists)`.
- `Star.stats.goals/assists` se completan desde el `Player` que matchea (real, actualizado); `club/rating/marketValue/note` siguen editoriales.
- `Leader` (sin cambios) se **deriva** de `Player` ordenado.

## 4. Entidades Room (mapeo desde football-data)

- **TeamEntity** (`tla` PK): `name`, `group`, `conf`, `rank`, `tag`, `styleLabel`, `starNum/starName/starPos/starNote/starClub/starRating/starValue` (editorial). ← `/teams` + `EditorialSource`.
- **PlayerEntity** (`id` PK auto): `teamTla`, `name`, `position`, `dateOfBirth`, `goals`, `assists`. ← `/teams.squad` (seed) + `/scorers` (update goals/assists por nombre+equipo).
- **StandingEntity** (`group`+`tla` PK): `group`, `position`, `pj`, `g`, `e`, `p`, `gf`, `gc`, `pts`. ← `/standings` (`type=="TOTAL"`).
- **MatchEntity** (`id` PK): `utcDate`, `status`, `stage`, `group`, `homeTla`, `awayTla`, `scoreHome`, `scoreAway`. ← `/matches`.
- **SyncMeta** (`key` PK): `lastSyncEpochMs` por dominio (teams/leaders/standings/matches).

**Mappers DTO→Entity** y **Entity→dominio** (campos exactos verificados):
- Match status: `FINISHED`→FINISHED; `IN_PLAY|PAUSED|LIVE`→LIVE; resto→UPCOMING. `score.fullTime.home/away`→score. `group`/`stage`→nombre legible. `minute` derivado al leer (no se persiste).
- GroupRow ← StandingEntity (`won→g, draw→e, lost→p, goalsFor→gf, …`); `flag`=`tlaToIso(tla)`.
- Leader ← `PlayerDao.topScorers(limit)` (`ORDER BY goals DESC`) / `topAssists` (`ORDER BY assists DESC, assists>0`).
- NewsItem ← RSS (`title`, `source`→tag, `pubDate`→time, `link`→id; `body` vacío → link-out).
- Eventos ← openfootball (gol+minuto, fix encoding), match por equipos+fecha.

## 5. Sync y frescura

`SyncManager`:
- **Seed (primer arranque o DB vacía):** `/teams` → TeamEntity + PlayerEntity (planteles); mezclar `EditorialSource`. Si no hay red en el primer arranque → seed desde `MockData` (degradado).
- **Refresh (stale o pull-to-refresh):** `/standings`→StandingEntity; `/scorers`→update goals/assists en PlayerEntity (match por nombre+TLA); `/matches`→MatchEntity. Actualiza `SyncMeta`.
- **Frescura:** `lastSync` por dominio; **stale si > 6 h**. Disparo: al abrir la app (si stale) desde el repo/VM; **pull-to-refresh** opcional (no obligatorio en v3).
- **Live:** `liveMatches()` lee MatchEntity con `status=LIVE` y deriva el minuto al vuelo; el sync de `matches` usa ventana corta (ej. 2 min) cuando hay partidos en curso.
- **Caché:** la DB ES la caché; no hace falta `TtlCache` en memoria (se reemplaza por `SyncMeta`).

## 6. Transversal

- **Deps:** Retrofit + OkHttp + kotlinx-serialization; **Room** (`room-runtime`, `room-ktx`, `room-compiler` vía **KSP**). Coroutines/Flow para DAOs.
- **Config:** `local.properties`→`FOOTBALL_DATA_TOKEN`→`BuildConfig` (gitignored). Header `X-Auth-Token`.
- **Fallback:** repo lee DB; si vacía y sin red → `MockData`. `ViewModel` ya manejan Loading/Error/retry.

## 7. Tests

- **Mappers** DTO→Entity y Entity→dominio con fixtures JSON (sin red).
- **DAOs** con Room in-memory (`Room.inMemoryDatabaseBuilder`, `androidx.room:room-testing`) — instrumentados o Robolectric; mínimo: upsert + queries de top scorers/assists.
- `deriveMinute`, `tlaToIso`, parser RSS, openfootball events: unit puros.
- `SyncManager` con services fake (sin red): seed escribe entidades, scorers actualizan goals/assists.

## 8. Fases

1. Infra: Retrofit/OkHttp/serialization + **Room/KSP** + `BuildConfig` token + cliente HTTP.
2. Dominio: `Player`; `Repository` → `suspend` (+ tests `runTest`).
3. `TlaToIso` (TDD).
4. football-data DTOs + service.
5. Entidades Room + DAOs + `MundialDatabase` (+ tests DAO).
6. Mappers DTO→Entity y Entity→dominio (+ deriveMinute) (TDD fixtures).
7. openfootball eventos (TDD).
8. RSS noticias (TDD).
9. `EditorialSource` (curado).
10. `SyncManager` (seed + refresh + frescura) (TDD con fakes).
11. `DefaultTeamRepository` (lee DAOs; dispara sync si stale).
12. `DefaultMatchRepository` (DAOs + openfootball eventos).
13. `DefaultNewsRepository` (RSS).
14. Wire `AppContainer` → Default* + verificación en emulador (datos reales).
15. Link-out de noticias.
16. Verificación final + check token no commiteado.

## 9. Fuera de alcance (YAGNI)

- Scraping de live (Sofascore 403/Google anti-bot): stats de partido/tarjetas → editorial. Minuto = derivado.
- WorkManager / sync en background (el sync on-open + pull-to-refresh alcanza); pull-to-refresh es opcional.
- Endpoints pagos; migraciones complejas de Room (esquema nuevo, `fallbackToDestructiveMigration` en v3).

## 10. Definición de "Hecho" (v3)

- La app lee de **Room**, sembrada con 48 equipos + planteles y refrescada por `SyncManager`; muestra datos reales WC2026 (grupos, calendario/resultados, goleadores/asistidores derivados de jugadores, equipos, live estado+score+minuto-derivado, noticias RSS) detrás de las `Repository`, sin cambios de UI (salvo link-out).
- Funciona **offline** tras el primer seed; degrada a `MockData` si no hay datos.
- Tests de mappers/DAOs/sync en verde; token solo en `local.properties`.
