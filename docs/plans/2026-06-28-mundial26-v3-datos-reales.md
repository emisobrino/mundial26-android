# Mundial 26 — v3 (Datos reales) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reemplazar los datos mock por datos reales del Mundial 2026 detrás de las interfaces `Repository`, sin tocar la UI. Fuentes: football-data.org (API), openfootball (JSON), RSS Google News, y editorial local.

**Architecture:** MVVM + Repository (v1/v2 intacto). Se agregan `data/remote/*`, `data/editorial`, `data/local` y `data/repository/Default*`. Las interfaces `Repository` pasan a `suspend`. `AppContainer` cambia `Fake*` → `Default*`. Reconciliación por código/TLA de equipo. Minuto en vivo **derivado** del kickoff (sin scraping).

**Tech Stack:** Kotlin, Retrofit 2 + OkHttp + kotlinx-serialization, coroutines, caché en memoria. Spec: `docs/specs/2026-06-27-mundial26-v3-datos-reales-design.md`.

> **Toolchain:** PowerShell `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio1\jbr"` antes de cada `.\gradlew.bat`. Tests: `:app:testDebugUnitTest`. Build: `:app:assembleDebug`.

> **Verificación de red real:** durante el desarrollo, los mappers se testean con **fixtures JSON** (sin red). La verificación contra la API real se hace en emulador en las fases de repos.

---

## Estructura de archivos (v3)

```
app/build.gradle.kts                         (MODIFY: plugin serialization, buildConfig, deps, token)
gradle/libs.versions.toml                    (MODIFY: retrofit/okhttp/serialization)
local.properties                             (MODIFY: FOOTBALL_DATA_TOKEN — gitignored)
app/src/main/java/com/mundial26/app/
├── domain/repository/Repositories.kt         (MODIFY: métodos → suspend)
├── data/mock/FakeRepositories.kt             (MODIFY: override suspend)
├── data/net/
│   ├── HttpClients.kt                        (CREATE: OkHttp + Retrofit + Json)
│   └── TlaToIso.kt                           (CREATE: mapa TLA→ISO-2)
├── data/remote/footballdata/
│   ├── FootballDataApi.kt                    (CREATE: Retrofit service)
│   ├── FootballDataDtos.kt                   (CREATE: DTOs)
│   └── FootballDataMappers.kt                (CREATE: DTO→dominio + deriveMinute)
├── data/remote/openfootball/
│   ├── OpenFootballApi.kt                    (CREATE)
│   ├── OpenFootballDtos.kt                   (CREATE)
│   └── OpenFootballMappers.kt                (CREATE: goles→MatchEvent + reconciliación)
├── data/remote/news/
│   └── NewsRssSource.kt                      (CREATE: fetch + parse RSS)
├── data/editorial/EditorialSource.kt         (CREATE: campos curados)
├── data/local/TtlCache.kt                    (CREATE: caché en memoria)
└── data/repository/
    ├── DefaultTeamRepository.kt              (CREATE)
    ├── DefaultMatchRepository.kt             (CREATE)
    └── DefaultNewsRepository.kt              (CREATE)
└── di/AppContainer.kt                        (MODIFY: Default* + construcción de red)
app/src/test/java/com/mundial26/app/data/    (CREATE: tests de mappers con fixtures)
```

---

## Task 1: Infra — dependencias, BuildConfig, cliente HTTP

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `local.properties`
- Create: `app/src/main/java/com/mundial26/app/data/net/HttpClients.kt`

- [ ] **Step 1: `libs.versions.toml`** — agregar a `[versions]`:
```toml
retrofit = "2.11.0"
okhttp = "4.12.0"
kotlinxSerialization = "1.7.3"
```
a `[libraries]`:
```toml
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
```
a `[plugins]`:
```toml
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 2: `app/build.gradle.kts`** — (a) agregar al bloque `plugins`: `alias(libs.plugins.kotlin.serialization)`. (b) Al principio del archivo (antes de `plugins {`), leer el token:
```kotlin
import java.util.Properties

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val footballDataToken: String = localProps.getProperty("FOOTBALL_DATA_TOKEN") ?: ""
```
(c) En `android { buildFeatures { ... } }` agregar `buildConfig = true`. (d) En `defaultConfig { }` agregar:
```kotlin
buildConfigField("String", "FOOTBALL_DATA_TOKEN", "\"$footballDataToken\"")
```
(e) En `dependencies`:
```kotlin
implementation(libs.retrofit)
implementation(libs.retrofit.kotlinx.serialization)
implementation(libs.okhttp)
implementation(libs.okhttp.logging)
implementation(libs.kotlinx.serialization.json)
```

- [ ] **Step 3: `local.properties`** — agregar (NO se commitea, ya está en `.gitignore`):
```
FOOTBALL_DATA_TOKEN=<TOKEN_FOOTBALL_DATA_ORG>
```

- [ ] **Step 4: `data/net/HttpClients.kt`**:
```kotlin
package com.mundial26.app.data.net

import com.mundial26.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Json tolerante: ignora campos desconocidos de las APIs. */
val AppJson: Json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

private val jsonMedia = "application/json".toMediaType()

private fun okHttp(extraHeaders: Map<String, String> = emptyMap()): OkHttpClient =
    OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            val b = chain.request().newBuilder()
            extraHeaders.forEach { (k, v) -> b.header(k, v) }
            chain.proceed(b.build())
        })
        .build()

/** Retrofit para football-data.org (token en header X-Auth-Token). */
fun footballDataRetrofit(): Retrofit = Retrofit.Builder()
    .baseUrl("https://api.football-data.org/v4/")
    .client(okHttp(mapOf("X-Auth-Token" to BuildConfig.FOOTBALL_DATA_TOKEN)))
    .addConverterFactory(AppJson.asConverterFactory(jsonMedia))
    .build()

/** Retrofit para openfootball (raw GitHub, sin auth). */
fun openFootballRetrofit(): Retrofit = Retrofit.Builder()
    .baseUrl("https://raw.githubusercontent.com/")
    .client(okHttp())
    .addConverterFactory(AppJson.asConverterFactory(jsonMedia))
    .build()
```

- [ ] **Step 5: Compilar** — `.\gradlew.bat :app:assembleDebug --console=plain` · Expected: BUILD SUCCESSFUL (BuildConfig.FOOTBALL_DATA_TOKEN existe).
- [ ] **Step 6: Commit** — `git add -A && git commit -m "chore(v3): infra Retrofit/OkHttp/serialization + BuildConfig token"` (verificar que `local.properties` NO esté en el commit).

---

## Task 2: Interfaces Repository → suspend

**Files:**
- Modify: `app/src/main/java/com/mundial26/app/domain/repository/Repositories.kt`
- Modify: `app/src/main/java/com/mundial26/app/data/mock/FakeRepositories.kt`

> Los `ViewModel` ya llaman a los repos dentro de `viewModelScope.launch`, así que llamar funciones `suspend` no requiere cambios en ellos.

- [ ] **Step 1: `Repositories.kt`** — anteponer `suspend` a TODOS los métodos de las 3 interfaces. Ej:
```kotlin
interface MatchRepository {
    suspend fun liveMatches(): List<Match>
    suspend fun resultsAndUpcoming(): List<Match>
    suspend fun byDay(): Map<String, List<Match>>
    suspend fun bracket(): List<BracketRound>
    suspend fun match(id: String): Match?
}
interface TeamRepository {
    suspend fun teams(): List<Team>
    suspend fun featured(): Team
    suspend fun groups(): List<Group>
    suspend fun team(id: String): Team?
    suspend fun topScorers(): List<Leader>
    suspend fun topAssists(): List<Leader>
}
interface NewsRepository {
    suspend fun news(): List<NewsItem>
    suspend fun item(id: String): NewsItem?
}
```

- [ ] **Step 2: `FakeRepositories.kt`** — anteponer `override suspend fun` a cada implementación (el cuerpo no cambia). Ej: `override suspend fun teams() = MockData.teams`.

- [ ] **Step 3: Compilar + tests** — `.\gradlew.bat :app:testDebugUnitTest --console=plain`. Si `FakeRepositoriesTest` falla por llamar funciones suspend fuera de coroutine, envolver cada test en `kotlinx.coroutines.test.runTest { ... }` (la dep `kotlinx-coroutines-test` ya está). Ej:
```kotlin
@Test fun hay_cuatro_grupos() = runTest { assertEquals(4, teams.groups().size) }
```
Aplicar `runTest { }` a todos los tests que llamen métodos de repo. Expected: PASS.

- [ ] **Step 4: Commit** — `git commit -am "refactor(domain): Repository suspend + tests con runTest"`

---

## Task 3: TlaToIso (mapa código→bandera)

**Files:**
- Create: `app/src/main/java/com/mundial26/app/data/net/TlaToIso.kt`
- Test: `app/src/test/java/com/mundial26/app/data/net/TlaToIsoTest.kt`

- [ ] **Step 1: Test que falla** (`TlaToIsoTest.kt`):
```kotlin
package com.mundial26.app.data.net
import org.junit.Assert.assertEquals
import org.junit.Test

class TlaToIsoTest {
    @Test fun mapea_codigos_conocidos() {
        assertEquals("ar", tlaToIso("ARG"))
        assertEquals("br", tlaToIso("BRA"))
        assertEquals("mx", tlaToIso("MEX"))
        assertEquals("gb-eng", tlaToIso("ENG"))
    }
    @Test fun desconocido_devuelve_minuscula() {
        assertEquals("xyz", tlaToIso("XYZ"))
    }
}
```

- [ ] **Step 2: Correr, verificar FALLA** — `.\gradlew.bat :app:testDebugUnitTest --console=plain` (no existe `tlaToIso`).

- [ ] **Step 3: `TlaToIso.kt`** — mapa FIFA TLA → ISO-2 (flagcdn). Incluir las 48 selecciones del Mundial 2026; fallback = `tla.lowercase()`:
```kotlin
package com.mundial26.app.data.net

private val TLA_ISO: Map<String, String> = mapOf(
    "ARG" to "ar", "BRA" to "br", "MEX" to "mx", "USA" to "us", "CAN" to "ca",
    "FRA" to "fr", "ENG" to "gb-eng", "ESP" to "es", "GER" to "de", "NED" to "nl",
    "POR" to "pt", "ITA" to "it", "BEL" to "be", "CRO" to "hr", "URU" to "uy",
    "COL" to "co", "JPN" to "jp", "KOR" to "kr", "SEN" to "sn", "MAR" to "ma",
    "SUI" to "ch", "DEN" to "dk", "POL" to "pl", "SRB" to "rs", "AUS" to "au",
    "QAT" to "qa", "ECU" to "ec", "GHA" to "gh", "CIV" to "ci", "RSA" to "za",
    "CZE" to "cz", "SCO" to "gb-sct", "WAL" to "gb-wls", "TUR" to "tr", "AUT" to "at",
    "NOR" to "no", "SWE" to "se", "PAR" to "py", "PER" to "pe", "CHI" to "cl",
    "TUN" to "tn", "EGY" to "eg", "NGA" to "ng", "CMR" to "cm", "ALG" to "dz",
    "HAI" to "ht", "CUW" to "cw", "BIH" to "ba", "JOR" to "jo", "PAN" to "pa"
)

/** FIFA TLA (3 letras) → código ISO-2 para flagcdn. Fallback: minúscula. */
fun tlaToIso(tla: String): String = TLA_ISO[tla.uppercase()] ?: tla.lowercase()
```

- [ ] **Step 4: Correr, verificar PASS**. **Step 5: Commit** — `git commit -am "feat(data): TlaToIso para banderas flagcdn"`

---

## Task 4: football-data — DTOs + service Retrofit

**Files:**
- Create: `app/src/main/java/com/mundial26/app/data/remote/footballdata/FootballDataDtos.kt`, `FootballDataApi.kt`

- [ ] **Step 1: `FootballDataDtos.kt`** (solo campos usados; `Json` ignora el resto):
```kotlin
package com.mundial26.app.data.remote.footballdata

import kotlinx.serialization.Serializable

@Serializable data class FdTeamRef(val id: Int? = null, val name: String = "", val shortName: String = "", val tla: String = "", val crest: String = "")
@Serializable data class FdScore(val fullTime: FdGoals = FdGoals(), val halfTime: FdGoals = FdGoals(), val winner: String? = null)
@Serializable data class FdGoals(val home: Int? = null, val away: Int? = null)

@Serializable data class FdMatch(
    val id: Int, val utcDate: String = "", val status: String = "",
    val matchday: Int? = null, val stage: String = "", val group: String? = null,
    val homeTeam: FdTeamRef = FdTeamRef(), val awayTeam: FdTeamRef = FdTeamRef(),
    val score: FdScore = FdScore()
)
@Serializable data class FdMatchesResponse(val matches: List<FdMatch> = emptyList())

@Serializable data class FdTableRow(
    val position: Int = 0, val team: FdTeamRef = FdTeamRef(),
    val playedGames: Int = 0, val won: Int = 0, val draw: Int = 0, val lost: Int = 0,
    val points: Int = 0, val goalsFor: Int = 0, val goalsAgainst: Int = 0
)
@Serializable data class FdStanding(val type: String = "", val group: String? = null, val table: List<FdTableRow> = emptyList())
@Serializable data class FdStandingsResponse(val standings: List<FdStanding> = emptyList())

@Serializable data class FdSquadPlayer(val name: String = "", val position: String? = null, val dateOfBirth: String? = null, val nationality: String? = null)
@Serializable data class FdTeam(val id: Int = 0, val name: String = "", val tla: String = "", val crest: String = "", val coach: FdCoach? = null, val squad: List<FdSquadPlayer> = emptyList())
@Serializable data class FdCoach(val name: String? = null)
@Serializable data class FdTeamsResponse(val teams: List<FdTeam> = emptyList())

@Serializable data class FdScorer(val player: FdPlayer = FdPlayer(), val team: FdTeamRef = FdTeamRef(), val goals: Int? = null, val assists: Int? = null)
@Serializable data class FdPlayer(val name: String = "")
@Serializable data class FdScorersResponse(val scorers: List<FdScorer> = emptyList())
```

- [ ] **Step 2: `FootballDataApi.kt`**:
```kotlin
package com.mundial26.app.data.remote.footballdata

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FootballDataApi {
    @GET("competitions/{code}/matches")
    suspend fun matches(@Path("code") code: String = "WC"): FdMatchesResponse

    @GET("competitions/{code}/standings")
    suspend fun standings(@Path("code") code: String = "WC"): FdStandingsResponse

    @GET("competitions/{code}/teams")
    suspend fun teams(@Path("code") code: String = "WC"): FdTeamsResponse

    @GET("competitions/{code}/scorers")
    suspend fun scorers(@Path("code") code: String = "WC", @Query("limit") limit: Int = 10): FdScorersResponse
}
```

- [ ] **Step 3: Compilar** — `.\gradlew.bat :app:compileDebugKotlin` · Expected: SUCCESS.
- [ ] **Step 4: Commit** — `git commit -am "feat(data): DTOs + service football-data.org"`

---

## Task 5: football-data — mappers de equipos, grupos y líderes (TDD)

**Files:**
- Create: `app/src/main/java/com/mundial26/app/data/remote/footballdata/FootballDataMappers.kt`
- Test: `app/src/test/java/com/mundial26/app/data/remote/footballdata/FootballDataMappersTest.kt`

- [ ] **Step 1: Test que falla** — usar DTOs construidos a mano (no red):
```kotlin
package com.mundial26.app.data.remote.footballdata
import com.mundial26.app.domain.model.MatchStatus
import org.junit.Assert.*
import org.junit.Test

class FootballDataMappersTest {
    @Test fun standings_mapea_a_grupos() {
        val resp = FdStandingsResponse(listOf(
            FdStanding(type = "TOTAL", group = "Group A", table = listOf(
                FdTableRow(1, FdTeamRef(tla = "MEX", name = "Mexico"), playedGames = 3, won = 3, draw = 0, lost = 0, points = 9, goalsFor = 6, goalsAgainst = 0)
            )),
            FdStanding(type = "HOME", group = "Group A", table = emptyList()) // debe ignorarse
        ))
        val groups = resp.toGroups()
        assertEquals(1, groups.size)
        assertEquals("Grupo A", groups[0].name)
        val row = groups[0].rows[0]
        assertEquals("MEX", row.code); assertEquals("mx", row.flag)
        assertEquals(9, row.pts); assertEquals(6, row.dg)
    }

    @Test fun scorers_mapea_a_goleadores() {
        val resp = FdScorersResponse(listOf(
            FdScorer(FdPlayer("Lionel Messi"), FdTeamRef(tla = "ARG"), goals = 5, assists = 2)
        ))
        val top = resp.toTopScorers()
        assertEquals("Lionel Messi", top[0].name)
        assertEquals("ar", top[0].flag); assertEquals(5, top[0].value); assertEquals(1, top[0].rank)
    }

    @Test fun match_status_y_score() {
        val m = FdMatch(id = 1, status = "FINISHED", group = "GROUP_A",
            homeTeam = FdTeamRef(tla = "MEX"), awayTeam = FdTeamRef(tla = "RSA"),
            score = FdScore(fullTime = FdGoals(2, 0)))
        val dm = m.toDomain(nowEpochMs = 0L)
        assertEquals(MatchStatus.FINISHED, dm.status)
        assertEquals(2, dm.scoreA); assertEquals(0, dm.scoreB)
        assertEquals("Grupo A", dm.group); assertEquals("MEX", dm.teamA)
    }
}
```

- [ ] **Step 2: Correr, verificar FALLA**.

- [ ] **Step 3: `FootballDataMappers.kt`** (incluye `toDomain` de match con minuto derivado; usado también en Task 6):
```kotlin
package com.mundial26.app.data.remote.footballdata

import com.mundial26.app.data.net.tlaToIso
import com.mundial26.app.domain.model.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun prettyGroup(stage: String, group: String?): String = when {
    group != null -> group.replace("GROUP_", "Grupo ").replace("Group ", "Grupo ")
    stage == "LAST_16" -> "Octavos"
    stage == "QUARTER_FINALS" -> "Cuartos"
    stage == "SEMI_FINALS" -> "Semifinal"
    stage == "THIRD_PLACE" -> "Tercer puesto"
    stage == "FINAL" -> "Final"
    stage == "LAST_32" -> "Dieciseisavos"
    else -> stage.replace("_", " ")
}

private fun statusOf(s: String): MatchStatus = when (s) {
    "FINISHED" -> MatchStatus.FINISHED
    "IN_PLAY", "PAUSED", "LIVE" -> MatchStatus.LIVE
    else -> MatchStatus.UPCOMING
}

/** Minuto aproximado derivado del kickoff. Solo válido si está en vivo. */
fun deriveMinute(utcDate: String, status: MatchStatus, nowEpochMs: Long): Int? {
    if (status != MatchStatus.LIVE || utcDate.isBlank()) return null
    return try {
        val kickoff = Instant.parse(utcDate).toEpochMilli()
        val mins = ((nowEpochMs - kickoff) / 60000L).toInt()
        mins.coerceIn(1, 120)
    } catch (e: Exception) { null }
}

fun FdMatch.toDomain(nowEpochMs: Long = System.currentTimeMillis()): Match {
    val st = statusOf(status)
    val timeStr = try {
        DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault()).format(Instant.parse(utcDate))
    } catch (e: Exception) { null }
    return Match(
        id = id.toString(), status = st,
        minute = deriveMinute(utcDate, st, nowEpochMs),
        time = timeStr, day = utcDate.take(10),
        group = prettyGroup(stage, group), stadium = "",
        teamA = homeTeam.tla, teamB = awayTeam.tla,
        flagA = tlaToIso(homeTeam.tla), flagB = tlaToIso(awayTeam.tla),
        scoreA = score.fullTime.home, scoreB = score.fullTime.away
    )
}

fun FdStandingsResponse.toGroups(): List<Group> =
    standings.filter { it.type == "TOTAL" }.map { s ->
        Group(
            name = (s.group ?: "").replace("Group ", "Grupo "),
            rows = s.table.map { r ->
                GroupRow(
                    code = r.team.tla, flag = tlaToIso(r.team.tla),
                    pj = r.playedGames, g = r.won, e = r.draw, p = r.lost,
                    gf = r.goalsFor, gc = r.goalsAgainst, pts = r.points
                )
            }
        )
    }

fun FdScorersResponse.toTopScorers(): List<Leader> =
    scorers.mapIndexed { i, s -> Leader(i + 1, s.player.name, tlaToIso(s.team.tla), s.goals ?: 0) }

fun FdScorersResponse.toTopAssists(): List<Leader> =
    scorers.filter { (it.assists ?: 0) > 0 }
        .sortedByDescending { it.assists }
        .mapIndexed { i, s -> Leader(i + 1, s.player.name, tlaToIso(s.team.tla), s.assists ?: 0) }
```

- [ ] **Step 4: Correr, verificar PASS**. **Step 5: Commit** — `git commit -am "feat(data): mappers football-data (grupos, líderes, match + minuto derivado)"`

---

## Task 6: openfootball — eventos de gol (TDD)

**Files:**
- Create: `.../data/remote/openfootball/OpenFootballDtos.kt`, `OpenFootballApi.kt`, `OpenFootballMappers.kt`
- Test: `.../data/remote/openfootball/OpenFootballMappersTest.kt`

- [ ] **Step 1: DTOs + API**:
```kotlin
// OpenFootballDtos.kt
package com.mundial26.app.data.remote.openfootball
import kotlinx.serialization.Serializable

@Serializable data class OfGoal(val name: String = "", val minute: Int? = null)
@Serializable data class OfMatch(
    val date: String = "", val team1: String = "", val team2: String = "",
    val goals1: List<OfGoal> = emptyList(), val goals2: List<OfGoal> = emptyList()
)
@Serializable data class OfWorldCup(val name: String = "", val matches: List<OfMatch> = emptyList())
```
```kotlin
// OpenFootballApi.kt
package com.mundial26.app.data.remote.openfootball
import retrofit2.http.GET

interface OpenFootballApi {
    @GET("openfootball/worldcup.json/master/2026/worldcup.json")
    suspend fun worldCup(): OfWorldCup
}
```

- [ ] **Step 2: Test que falla** (`OpenFootballMappersTest.kt`):
```kotlin
package com.mundial26.app.data.remote.openfootball
import com.mundial26.app.domain.model.EventKind
import org.junit.Assert.*
import org.junit.Test

class OpenFootballMappersTest {
    @Test fun goles_a_eventos_por_lado() {
        val m = OfMatch(team1 = "Mexico", team2 = "South Africa",
            goals1 = listOf(OfGoal("Quiñones", 9)), goals2 = listOf(OfGoal("Mokoena", 80)))
        val events = m.toEvents()
        assertEquals(2, events.size)
        assertEquals('A', events[0].side); assertEquals(EventKind.GOAL, events[0].kind)
        assertEquals(9, events[0].min); assertEquals('B', events[1].side)
    }
    @Test fun arregla_encoding_doble() {
        assertEquals("Julián", fixEncoding("JuliÃ¡n"))
    }
}
```

- [ ] **Step 3: `OpenFootballMappers.kt`**:
```kotlin
package com.mundial26.app.data.remote.openfootball
import com.mundial26.app.domain.model.EventKind
import com.mundial26.app.domain.model.MatchEvent

/** openfootball viene con UTF-8 doble-codificado; lo revierte. */
fun fixEncoding(s: String): String = try {
    String(s.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
} catch (e: Exception) { s }

fun OfMatch.toEvents(): List<MatchEvent> {
    val a = goals1.map { MatchEvent(it.minute ?: 0, EventKind.GOAL, 'A', fixEncoding(it.name)) }
    val b = goals2.map { MatchEvent(it.minute ?: 0, EventKind.GOAL, 'B', fixEncoding(it.name)) }
    return (a + b).sortedBy { it.min }
}

/** Reconciliación con football-data: por fecha (YYYY-MM-DD) + nombres de equipo (substring, case-insensitive). */
fun List<OfMatch>.findEventsFor(day: String, teamAName: String, teamBName: String): List<MatchEvent> {
    val m = firstOrNull {
        it.date == day &&
            (it.team1.contains(teamAName, true) || teamAName.contains(it.team1, true)) &&
            (it.team2.contains(teamBName, true) || teamBName.contains(it.team2, true))
    }
    return m?.toEvents() ?: emptyList()
}
```

- [ ] **Step 4: Correr, verificar PASS**. **Step 5: Commit** — `git commit -am "feat(data): openfootball eventos de gol + reconciliación"`

---

## Task 7: Noticias por RSS (TDD)

**Files:**
- Create: `app/src/main/java/com/mundial26/app/data/remote/news/NewsRssSource.kt`
- Test: `.../data/remote/news/NewsRssSourceTest.kt`

> RSS se parsea con `android.util.Xml`/regex; para testear sin Android, el parser puro recibe un `String` y se testea con JUnit. El fetch HTTP queda en el source.

- [ ] **Step 1: Test que falla** (`NewsRssSourceTest.kt`):
```kotlin
package com.mundial26.app.data.remote.news
import org.junit.Assert.*
import org.junit.Test

class NewsRssSourceTest {
    private val xml = """
        <rss><channel>
          <item>
            <title><![CDATA[Argentina venció a Jordania]]></title>
            <link>https://example.com/n1</link>
            <pubDate>Sun, 28 Jun 2026 18:00:00 GMT</pubDate>
            <source url="x">La Nación</source>
            <description><![CDATA[Resumen del partido]]></description>
          </item>
        </channel></rss>
    """.trimIndent()

    @Test fun parsea_items_rss() {
        val items = parseRss(xml)
        assertEquals(1, items.size)
        assertEquals("Argentina venció a Jordania", items[0].title)
        assertEquals("La Nación", items[0].tag)
        assertEquals("https://example.com/n1", items[0].id)
        assertTrue(items[0].body.isEmpty())
    }
}
```

- [ ] **Step 2: Correr, verificar FALLA**.

- [ ] **Step 3: `NewsRssSource.kt`** (parser puro + fetch). `NewsItem.id` = URL (para link-out); `tag` = fuente; `summary` = description; `body` = vacío:
```kotlin
package com.mundial26.app.data.remote.news

import com.mundial26.app.domain.model.NewsItem
import okhttp3.OkHttpClient
import okhttp3.Request

private val ITEM = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
private fun tag(block: String, name: String): String {
    val m = Regex("<$name[^>]*>(.*?)</$name>", RegexOption.DOT_MATCHES_ALL).find(block) ?: return ""
    return m.groupValues[1].replace("<![CDATA[", "").replace("]]>", "").trim()
}

/** Parser puro (testeable sin red). */
fun parseRss(xml: String): List<NewsItem> =
    ITEM.findAll(xml).map { it.groupValues[1] }.map { b ->
        NewsItem(
            id = tag(b, "link"),
            tag = tag(b, "source").ifBlank { "Noticias" },
            colorHex = 0xFF2A398D,
            time = tag(b, "pubDate"),
            title = tag(b, "title"),
            summary = tag(b, "description"),
            body = emptyList()
        )
    }.toList()

class NewsRssSource(
    private val client: OkHttpClient = OkHttpClient(),
    private val url: String =
        "https://news.google.com/rss/search?q=mundial%202026%20futbol&hl=es-419&gl=AR&ceid=AR:es-419"
) {
    /** Devuelve titulares o lista vacía ante error (el repo decide el fallback). */
    suspend fun headlines(): List<NewsItem> = try {
        val resp = client.newCall(Request.Builder().url(url).build()).execute()
        resp.use { if (it.isSuccessful) parseRss(it.body?.string().orEmpty()) else emptyList() }
    } catch (e: Exception) { emptyList() }
}
```

- [ ] **Step 4: Correr, verificar PASS**. **Step 5: Commit** — `git commit -am "feat(data): noticias por RSS (Google News)"`

---

## Task 8: EditorialSource (campos curados)

**Files:**
- Create: `app/src/main/java/com/mundial26/app/data/editorial/EditorialSource.kt`

> Mueve a una sola fuente los datos que NINGUNA API gratis da. Reusa los valores que ya están en `MockData` (pros/cons, styleLabel, conf, rank, star note/club/marketValue/rating). No borrar `MockData` (lo usan los Fake*/previews).

- [ ] **Step 1: `EditorialSource.kt`** — un objeto con lookups por código de equipo:
```kotlin
package com.mundial26.app.data.editorial

import com.mundial26.app.domain.model.Confederation
import com.mundial26.app.domain.model.Star

/** Datos editoriales curados, keyed por código FIFA (ARG, BRA…). */
data class TeamEditorial(
    val conf: Confederation, val rank: Int, val tag: String, val styleLabel: String,
    val star: Star, val pros: List<String>, val cons: List<String>
)

object EditorialSource {
    val featuredCode = "ARG"

    // Portar desde MockData.teams (mismos valores) los equipos con scouting curado.
    val byCode: Map<String, TeamEditorial> = mapOf(
        "ARG" to TeamEditorial(
            conf = Confederation.CONMEBOL, rank = 1, tag = "Campeón defensor",
            styleLabel = "Posesión vertical",
            star = Star(10, "Lionel Messi", "Delantero / Capitán",
                "Cerebro creativo del equipo: maneja los tiempos y aparece en los momentos decisivos.",
                com.mundial26.app.domain.model.StarStats(6, 4, 3, 8.4, "Inter Miami", 38, "€20M")),
            pros = listOf("Jerarquía y experiencia en partidos clave", "Mediocampo equilibrado y muy trabajador", "Definición letal de sus delanteros"),
            cons = listOf("Defensa con poca velocidad", "Dependencia de Messi en la creación", "Plantel con varios veteranos")
        )
        // … portar el resto de los equipos de MockData.teams del mismo modo.
    )

    fun forCode(code: String): TeamEditorial? = byCode[code.uppercase()]
}
```

- [ ] **Step 2: Compilar** — `.\gradlew.bat :app:compileDebugKotlin` · Expected: SUCCESS.
- [ ] **Step 3: Commit** — `git commit -am "feat(data): EditorialSource (scouting/estrella curados)"`

---

## Task 9: TtlCache (caché en memoria, TDD)

**Files:**
- Create: `app/src/main/java/com/mundial26/app/data/local/TtlCache.kt`
- Test: `.../data/local/TtlCacheTest.kt`

- [ ] **Step 1: Test que falla** (`TtlCacheTest.kt`):
```kotlin
package com.mundial26.app.data.local
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TtlCacheTest {
    @Test fun cachea_y_revalida() = runTest {
        var calls = 0
        var now = 0L
        val cache = TtlCache(ttlMs = 1000L) { now }
        suspend fun load() = cache.getOrLoad("k") { calls++; "v$calls" }
        assertEquals("v1", load())
        assertEquals("v1", load())          // dentro de TTL → no recarga
        now = 2000L
        assertEquals("v2", load())           // expiró → recarga
        assertEquals(2, calls)
    }
}
```

- [ ] **Step 2: Correr, verificar FALLA**.

- [ ] **Step 3: `TtlCache.kt`**:
```kotlin
package com.mundial26.app.data.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Caché en memoria por clave con expiración (TTL). `now` inyectable para tests. */
class TtlCache(
    private val ttlMs: Long,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    private data class Entry(val value: Any?, val at: Long)
    private val map = HashMap<String, Entry>()
    private val mutex = Mutex()

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> getOrLoad(key: String, loader: suspend () -> T): T = mutex.withLock {
        val e = map[key]
        if (e != null && now() - e.at < ttlMs) return e.value as T
        val v = loader()
        map[key] = Entry(v, now())
        v
    }
}
```

- [ ] **Step 4: Correr, verificar PASS**. **Step 5: Commit** — `git commit -am "feat(data): TtlCache en memoria"`

---

## Task 10: DefaultTeamRepository (equipos, grupos, líderes)

**Files:**
- Create: `app/src/main/java/com/mundial26/app/data/repository/DefaultTeamRepository.kt`

- [ ] **Step 1: Implementación** — combina football-data + editorial + caché; fallback a `MockData` ante error:
```kotlin
package com.mundial26.app.data.repository

import com.mundial26.app.data.editorial.EditorialSource
import com.mundial26.app.data.local.TtlCache
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.data.net.tlaToIso
import com.mundial26.app.data.remote.footballdata.FootballDataApi
import com.mundial26.app.data.remote.footballdata.toGroups
import com.mundial26.app.data.remote.footballdata.toTopAssists
import com.mundial26.app.data.remote.footballdata.toTopScorers
import com.mundial26.app.domain.model.*
import com.mundial26.app.domain.repository.TeamRepository

class DefaultTeamRepository(
    private val api: FootballDataApi,
    private val cacheWarm: TtlCache,   // ttl ~ horas
) : TeamRepository {

    override suspend fun teams(): List<Team> = runCatching {
        cacheWarm.getOrLoad("teams") {
            api.teams().teams.map { t ->
                val ed = EditorialSource.forCode(t.tla)
                Team(
                    id = t.tla.lowercase(), name = t.name, code = t.tla,
                    conf = ed?.conf ?: Confederation.OTHER, rank = ed?.rank ?: 0,
                    tag = ed?.tag ?: "", flag = tlaToIso(t.tla),
                    styleLabel = ed?.styleLabel ?: "",
                    star = ed?.star ?: Star(0, "", "", ""),
                    pros = ed?.pros ?: emptyList(), cons = ed?.cons ?: emptyList(),
                    form = emptyList()
                )
            }
        }
    }.getOrElse { MockData.teams }

    override suspend fun featured(): Team =
        teams().firstOrNull { it.code == EditorialSource.featuredCode } ?: MockData.teams.first()

    override suspend fun groups(): List<Group> = runCatching {
        cacheWarm.getOrLoad("groups") { api.standings().toGroups() }
    }.getOrElse { MockData.groups }

    override suspend fun team(id: String): Team? =
        teams().firstOrNull { it.id == id || it.code.equals(id, true) }

    override suspend fun topScorers(): List<Leader> = runCatching {
        cacheWarm.getOrLoad("scorers") { api.scorers().toTopScorers() }
    }.getOrElse { MockData.topScorers }

    override suspend fun topAssists(): List<Leader> = runCatching {
        cacheWarm.getOrLoad("assists") {
            api.scorers().toTopAssists().ifEmpty { MockData.topAssists }
        }
    }.getOrElse { MockData.topAssists }
}
```

- [ ] **Step 2: Compilar** — `.\gradlew.bat :app:compileDebugKotlin` · Expected: SUCCESS.
- [ ] **Step 3: Commit** — `git commit -am "feat(data): DefaultTeamRepository (equipos/grupos/líderes reales)"`

---

## Task 11: DefaultMatchRepository (partidos + eventos)

**Files:**
- Create: `app/src/main/java/com/mundial26/app/data/repository/DefaultMatchRepository.kt`

- [ ] **Step 1: Implementación** — matches de football-data + eventos de openfootball; caché corta para live:
```kotlin
package com.mundial26.app.data.repository

import com.mundial26.app.data.local.TtlCache
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.data.remote.footballdata.FootballDataApi
import com.mundial26.app.data.remote.footballdata.toDomain
import com.mundial26.app.data.remote.openfootball.OpenFootballApi
import com.mundial26.app.data.remote.openfootball.findEventsFor
import com.mundial26.app.domain.model.Match
import com.mundial26.app.domain.model.MatchStatus
import com.mundial26.app.domain.model.BracketRound
import com.mundial26.app.domain.repository.MatchRepository

class DefaultMatchRepository(
    private val fd: FootballDataApi,
    private val of: OpenFootballApi,
    private val cacheHot: TtlCache,   // ttl ~ minutos
) : MatchRepository {

    private suspend fun allMatches(): List<Match> = cacheHot.getOrLoad("matches") {
        fd.matches().matches.map { it.toDomain() }
    }

    override suspend fun liveMatches(): List<Match> =
        runCatching { allMatches().filter { it.status == MatchStatus.LIVE } }.getOrElse { MockData.matches.filter { it.status == MatchStatus.LIVE } }

    override suspend fun resultsAndUpcoming(): List<Match> =
        runCatching { allMatches().filter { it.status != MatchStatus.LIVE } }.getOrElse { MockData.matches.filter { it.status != MatchStatus.LIVE } }

    override suspend fun byDay(): Map<String, List<Match>> =
        runCatching { allMatches().filter { it.day != null }.groupBy { it.day!! } }.getOrElse { emptyMap() }

    override suspend fun bracket(): List<BracketRound> =
        runCatching { MockData.bracket }.getOrElse { MockData.bracket } // bracket: derivar de stage en iteración futura; por ahora editorial

    override suspend fun match(id: String): Match? = runCatching {
        val m = allMatches().firstOrNull { it.id == id } ?: return null
        val events = of.worldCup().matches.findEventsFor(m.day ?: "", m.teamA, m.teamB)
        m.copy(events = events)
    }.getOrElse { MockData.matches.find { it.id == id } }
}
```

- [ ] **Step 2: Compilar** · Expected: SUCCESS. **Step 3: Commit** — `git commit -am "feat(data): DefaultMatchRepository (partidos reales + eventos openfootball)"`

---

## Task 12: DefaultNewsRepository (RSS)

**Files:**
- Create: `app/src/main/java/com/mundial26/app/data/repository/DefaultNewsRepository.kt`

- [ ] **Step 1: Implementación**:
```kotlin
package com.mundial26.app.data.repository

import com.mundial26.app.data.local.TtlCache
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.data.remote.news.NewsRssSource
import com.mundial26.app.domain.model.NewsItem
import com.mundial26.app.domain.repository.NewsRepository

class DefaultNewsRepository(
    private val rss: NewsRssSource,
    private val cacheWarm: TtlCache,
) : NewsRepository {
    override suspend fun news(): List<NewsItem> = runCatching {
        cacheWarm.getOrLoad("news") { rss.headlines().ifEmpty { MockData.news } }
    }.getOrElse { MockData.news }

    override suspend fun item(id: String): NewsItem? = news().firstOrNull { it.id == id }
}
```

- [ ] **Step 2: Compilar** · Expected: SUCCESS. **Step 3: Commit** — `git commit -am "feat(data): DefaultNewsRepository (RSS)"`

---

## Task 13: Wire AppContainer → Default* + verificación real

**Files:**
- Modify: `app/src/main/java/com/mundial26/app/di/AppContainer.kt`

- [ ] **Step 1: `AppContainer.kt`** — construir red y exponer los Default*:
```kotlin
package com.mundial26.app.di

import com.mundial26.app.data.local.TtlCache
import com.mundial26.app.data.net.footballDataRetrofit
import com.mundial26.app.data.net.openFootballRetrofit
import com.mundial26.app.data.remote.footballdata.FootballDataApi
import com.mundial26.app.data.remote.news.NewsRssSource
import com.mundial26.app.data.remote.openfootball.OpenFootballApi
import com.mundial26.app.data.repository.DefaultMatchRepository
import com.mundial26.app.data.repository.DefaultNewsRepository
import com.mundial26.app.data.repository.DefaultTeamRepository
import com.mundial26.app.domain.repository.*

interface AppContainer {
    val matchRepository: MatchRepository
    val teamRepository: TeamRepository
    val newsRepository: NewsRepository
}

class DefaultAppContainer : AppContainer {
    private val fdApi by lazy { footballDataRetrofit().create(FootballDataApi::class.java) }
    private val ofApi by lazy { openFootballRetrofit().create(OpenFootballApi::class.java) }
    private val warm by lazy { TtlCache(ttlMs = 3 * 60 * 60 * 1000L) }   // 3 h
    private val hot by lazy { TtlCache(ttlMs = 60 * 1000L) }              // 1 min

    override val matchRepository: MatchRepository by lazy { DefaultMatchRepository(fdApi, ofApi, hot) }
    override val teamRepository: TeamRepository by lazy { DefaultTeamRepository(fdApi, warm) }
    override val newsRepository: NewsRepository by lazy { DefaultNewsRepository(NewsRssSource(), warm) }
}
```

- [ ] **Step 2: Permiso INTERNET** — verificar que `AndroidManifest.xml` tiene `<uses-permission android:name="android.permission.INTERNET" />` (ya está desde v1). Si falta, agregarlo.

- [ ] **Step 3: Tests + build** — `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain` · Expected: PASS + BUILD SUCCESSFUL.

- [ ] **Step 4: Verificación en emulador (datos reales)** — bootear `Pixel_9a`, `adb install -r`, lanzar. Confirmar con red:
  - Equipos: 48 selecciones reales; grupos A–L con tablas reales.
  - Inicio: widget de líderes con goleadores reales (Messi, Mbappé…); posiciones reales; noticias = titulares RSS reales (tap abre… ver nota siguiente sobre link-out).
  - Partidos: calendario/resultados reales; en vivo (si hay) con minuto derivado.
  - Detalle de partido: eventos de gol (de openfootball) cuando matchean.
  - Sin red / error: la app cae a mock sin crashear.

- [ ] **Step 5: Commit** — `git commit -am "feat(di): AppContainer usa repos reales (football-data + openfootball + RSS)"`

---

## Task 14: Link-out de noticias (abrir nota en navegador)

**Files:**
- Modify: `app/src/main/java/com/mundial26/app/ui/newsdetail/NewsDetailScreen.kt`

> Como el RSS no trae cuerpo, el detalle muestra el resumen + botón "Leer la nota" que abre `item.id` (URL) en el navegador.

- [ ] **Step 1:** En `NewsDetailContent`, debajo del `summary`/body, agregar un botón que use `LocalUriHandler`:
```kotlin
val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
// dentro del contenido, si item.id empieza con "http":
if (item.id.startsWith("http")) {
    Button(onClick = { uriHandler.openUri(item.id) }) { Text("Leer la nota") }
}
```
(Mantener el render de `item.body` para cuando haya notas editoriales con cuerpo.)

- [ ] **Step 2: Compilar + emulador** — abrir una noticia, tocar "Leer la nota", confirmar que abre el navegador. · Expected: SUCCESS.
- [ ] **Step 3: Commit** — `git commit -am "feat(newsdetail): botón Leer la nota (link-out RSS)"`

---

## Task 15: Verificación final v3

- [ ] **Step 1: Tests** — `.\gradlew.bat :app:testDebugUnitTest` · Expected: PASS (mappers, TlaToIso, cache, openfootball, RSS).
- [ ] **Step 2: Build** — `.\gradlew.bat :app:assembleDebug` · Expected: BUILD SUCCESSFUL.
- [ ] **Step 3: Recorrido en emulador con red real** — las 4 pantallas + detalles + widget muestran datos reales WC2026; el modo sin red degrada a mock.
- [ ] **Step 4: Confirmar que `local.properties` (token) nunca se commiteó** — `git log -p | grep FOOTBALL_DATA_TOKEN` no debe mostrar el valor real.
- [ ] **Step 5: Commit final** — `git commit -am "chore: v3 datos reales completa y verificada"`

---

## Definición de "Hecho" (v3)
- Las pantallas existentes muestran datos reales WC2026 (grupos, calendario/resultados, goleadores, equipos, plantel, live estado+score+minuto-derivado, noticias RSS) detrás de los `Repository`, sin cambios de UI salvo el botón link-out de noticias.
- Mappers testeados con fixtures; la app degrada a mock sin red/error.
- Token solo en `local.properties` (gitignored); nunca en git.
- `comp`/season configurable para futuras ediciones.
