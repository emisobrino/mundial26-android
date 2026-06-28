# Mundial 26 — v3 (Datos reales, Room) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reemplazar los datos mock por datos reales del Mundial 2026, persistidos en **Room** (seed de 48 equipos + planteles, sync por fechas de goleadores/asistidores/posiciones/resultados), detrás de las `Repository`, sin tocar la UI.

**Architecture:** DB local Room = caché/fuente de verdad. `SyncManager` baja de football-data.org y hace upsert de entidades. Repos leen de DAOs y disparan sync si está stale. openfootball aporta eventos de gol; RSS aporta noticias; editorial aporta scouting. Minuto en vivo derivado del kickoff.

**Tech Stack:** Kotlin, Retrofit 2 + OkHttp + kotlinx-serialization, **Room (KSP)**, coroutines/Flow. Spec: `docs/specs/2026-06-27-mundial26-v3-datos-reales-design.md`.

> **Toolchain:** PowerShell `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio1\jbr"` antes de cada `.\gradlew.bat`. Tests JVM puros: `:app:testDebugUnitTest`. Build: `:app:assembleDebug`. (Tests de DAO Room corren con Robolectric o instrumentados; ver Task 5.)

---

## Task 1: Infra — Retrofit + serialization + Room/KSP + BuildConfig token

**Files:** Modify `gradle/libs.versions.toml`, `app/build.gradle.kts`, root `build.gradle.kts`, `local.properties`; Create `app/src/main/java/com/mundial26/app/data/net/HttpClients.kt`

- [ ] **Step 1: `libs.versions.toml`** — `[versions]` agregar:
```toml
retrofit = "2.11.0"
okhttp = "4.12.0"
kotlinxSerialization = "1.7.3"
ksp = "2.1.0-1.0.29"
room = "2.6.1"
```
`[libraries]`:
```toml
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
```
`[plugins]`:
```toml
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 2: root `build.gradle.kts`** — agregar al bloque `plugins`:
```kotlin
alias(libs.plugins.kotlin.serialization) apply false
alias(libs.plugins.ksp) apply false
```

- [ ] **Step 3: `app/build.gradle.kts`** — (a) `plugins`: agregar `alias(libs.plugins.kotlin.serialization)` y `alias(libs.plugins.ksp)`. (b) Tope del archivo (antes de `plugins {`):
```kotlin
import java.util.Properties

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val footballDataToken: String = localProps.getProperty("FOOTBALL_DATA_TOKEN") ?: ""
```
(c) `buildFeatures { compose = true; buildConfig = true }`. (d) En `defaultConfig`: `buildConfigField("String", "FOOTBALL_DATA_TOKEN", "\"$footballDataToken\"")`. (e) `dependencies`:
```kotlin
implementation(libs.retrofit)
implementation(libs.retrofit.kotlinx.serialization)
implementation(libs.okhttp)
implementation(libs.okhttp.logging)
implementation(libs.kotlinx.serialization.json)
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
ksp(libs.androidx.room.compiler)
testImplementation(libs.androidx.room.testing)
```

- [ ] **Step 4: `local.properties`** — agregar (NO commitear): `FOOTBALL_DATA_TOKEN=<TOKEN_FOOTBALL_DATA_ORG>`

- [ ] **Step 5: `data/net/HttpClients.kt`**:
```kotlin
package com.mundial26.app.data.net

import com.mundial26.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

val AppJson: Json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
private val jsonMedia = "application/json".toMediaType()

private fun okHttp(headers: Map<String, String> = emptyMap()) = OkHttpClient.Builder()
    .addInterceptor(Interceptor { chain ->
        val b = chain.request().newBuilder()
        headers.forEach { (k, v) -> b.header(k, v) }
        chain.proceed(b.build())
    }).build()

fun footballDataRetrofit(): Retrofit = Retrofit.Builder()
    .baseUrl("https://api.football-data.org/v4/")
    .client(okHttp(mapOf("X-Auth-Token" to BuildConfig.FOOTBALL_DATA_TOKEN)))
    .addConverterFactory(AppJson.asConverterFactory(jsonMedia)).build()

fun openFootballRetrofit(): Retrofit = Retrofit.Builder()
    .baseUrl("https://raw.githubusercontent.com/")
    .client(okHttp())
    .addConverterFactory(AppJson.asConverterFactory(jsonMedia)).build()
```

- [ ] **Step 6: Build** — `.\gradlew.bat :app:assembleDebug --console=plain` · Expected: BUILD SUCCESSFUL.
- [ ] **Step 7: Commit** — `git add -A && git commit -m "chore(v3): infra Retrofit + Room/KSP + BuildConfig token"` (verificar que `local.properties` NO esté).

---

## Task 2: Dominio `Player` + Repository → suspend

**Files:** Modify `domain/model/Models.kt`, `domain/repository/Repositories.kt`, `data/mock/FakeRepositories.kt`, `app/src/test/.../FakeRepositoriesTest.kt`

- [ ] **Step 1: `Models.kt`** — agregar al final:
```kotlin
/** Jugador con stats acumuladas del torneo (persistido en Room, actualizado por sync). */
data class Player(
    val id: Long,
    val name: String,
    val teamCode: String,
    val position: String,
    val age: Int,
    val goals: Int,
    val assists: Int
)
```

- [ ] **Step 2: `Repositories.kt`** — anteponer `suspend` a TODOS los métodos de las 3 interfaces (firmas idénticas, solo `suspend`).

- [ ] **Step 3: `FakeRepositories.kt`** — `override suspend fun` en cada método (cuerpos sin cambio).

- [ ] **Step 4: `FakeRepositoriesTest.kt`** — envolver cada test que llame métodos de repo en `kotlinx.coroutines.test.runTest { ... }`. Ej: `@Test fun hay_cuatro_grupos() = runTest { assertEquals(4, teams.groups().size) }`.

- [ ] **Step 5: Tests** — `.\gradlew.bat :app:testDebugUnitTest --console=plain` · Expected: PASS.
- [ ] **Step 6: Commit** — `git commit -am "feat(domain): Player + Repository suspend"`

---

## Task 3: `TlaToIso` (TDD)

**Files:** Create `data/net/TlaToIso.kt`, Test `data/net/TlaToIsoTest.kt`

- [ ] **Step 1: Test** (`TlaToIsoTest.kt`):
```kotlin
package com.mundial26.app.data.net
import org.junit.Assert.assertEquals
import org.junit.Test
class TlaToIsoTest {
    @Test fun conocidos() { assertEquals("ar", tlaToIso("ARG")); assertEquals("gb-eng", tlaToIso("ENG")) }
    @Test fun desconocido() { assertEquals("xyz", tlaToIso("XYZ")) }
}
```
- [ ] **Step 2: Correr → FALLA.**
- [ ] **Step 3: `TlaToIso.kt`**:
```kotlin
package com.mundial26.app.data.net

private val TLA_ISO = mapOf(
    "ARG" to "ar","BRA" to "br","MEX" to "mx","USA" to "us","CAN" to "ca","FRA" to "fr",
    "ENG" to "gb-eng","ESP" to "es","GER" to "de","NED" to "nl","POR" to "pt","ITA" to "it",
    "BEL" to "be","CRO" to "hr","URU" to "uy","COL" to "co","JPN" to "jp","KOR" to "kr",
    "SEN" to "sn","MAR" to "ma","SUI" to "ch","DEN" to "dk","POL" to "pl","SRB" to "rs",
    "AUS" to "au","QAT" to "qa","ECU" to "ec","GHA" to "gh","CIV" to "ci","RSA" to "za",
    "CZE" to "cz","SCO" to "gb-sct","WAL" to "gb-wls","TUR" to "tr","AUT" to "at","NOR" to "no",
    "SWE" to "se","PAR" to "py","PER" to "pe","CHI" to "cl","TUN" to "tn","EGY" to "eg",
    "NGA" to "ng","CMR" to "cm","ALG" to "dz","HAI" to "ht","CUW" to "cw","BIH" to "ba","JOR" to "jo","PAN" to "pa"
)
fun tlaToIso(tla: String): String = TLA_ISO[tla.uppercase()] ?: tla.lowercase()
```
- [ ] **Step 4: PASS. Step 5: Commit** — `git commit -am "feat(data): TlaToIso"`

---

## Task 4: football-data DTOs + service

**Files:** Create `data/remote/footballdata/FootballDataDtos.kt`, `FootballDataApi.kt`

- [ ] **Step 1: `FootballDataDtos.kt`** (solo campos usados):
```kotlin
package com.mundial26.app.data.remote.footballdata
import kotlinx.serialization.Serializable

@Serializable data class FdTeamRef(val tla: String = "", val name: String = "", val crest: String = "")
@Serializable data class FdGoals(val home: Int? = null, val away: Int? = null)
@Serializable data class FdScore(val fullTime: FdGoals = FdGoals())
@Serializable data class FdMatch(val id: Int, val utcDate: String = "", val status: String = "", val stage: String = "", val group: String? = null, val homeTeam: FdTeamRef = FdTeamRef(), val awayTeam: FdTeamRef = FdTeamRef(), val score: FdScore = FdScore())
@Serializable data class FdMatchesResponse(val matches: List<FdMatch> = emptyList())
@Serializable data class FdTableRow(val position: Int = 0, val team: FdTeamRef = FdTeamRef(), val playedGames: Int = 0, val won: Int = 0, val draw: Int = 0, val lost: Int = 0, val points: Int = 0, val goalsFor: Int = 0, val goalsAgainst: Int = 0)
@Serializable data class FdStanding(val type: String = "", val group: String? = null, val table: List<FdTableRow> = emptyList())
@Serializable data class FdStandingsResponse(val standings: List<FdStanding> = emptyList())
@Serializable data class FdSquadPlayer(val name: String = "", val position: String? = null, val dateOfBirth: String? = null)
@Serializable data class FdTeam(val tla: String = "", val name: String = "", val crest: String = "", val squad: List<FdSquadPlayer> = emptyList())
@Serializable data class FdTeamsResponse(val teams: List<FdTeam> = emptyList())
@Serializable data class FdPlayer(val name: String = "")
@Serializable data class FdScorer(val player: FdPlayer = FdPlayer(), val team: FdTeamRef = FdTeamRef(), val goals: Int? = null, val assists: Int? = null)
@Serializable data class FdScorersResponse(val scorers: List<FdScorer> = emptyList())
```
- [ ] **Step 2: `FootballDataApi.kt`**:
```kotlin
package com.mundial26.app.data.remote.footballdata
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FootballDataApi {
    @GET("competitions/{c}/matches") suspend fun matches(@Path("c") c: String = "WC"): FdMatchesResponse
    @GET("competitions/{c}/standings") suspend fun standings(@Path("c") c: String = "WC"): FdStandingsResponse
    @GET("competitions/{c}/teams") suspend fun teams(@Path("c") c: String = "WC"): FdTeamsResponse
    @GET("competitions/{c}/scorers") suspend fun scorers(@Path("c") c: String = "WC", @Query("limit") limit: Int = 20): FdScorersResponse
}
```
- [ ] **Step 3: Compilar → SUCCESS. Step 4: Commit** — `git commit -am "feat(data): DTOs + service football-data"`

---

## Task 5: Room — entidades, DAOs, Database (+ test DAO)

**Files:** Create `data/local/db/entities/Entities.kt`, `dao/Daos.kt`, `MundialDatabase.kt`; Test `data/local/db/DaoTest.kt`

- [ ] **Step 1: `entities/Entities.kt`**:
```kotlin
package com.mundial26.app.data.local.db.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey val tla: String, val name: String, val group: String = "",
    val conf: String = "OTHER", val rank: Int = 0, val tag: String = "", val styleLabel: String = "",
    val starNum: Int = 0, val starName: String = "", val starPos: String = "", val starNote: String = "",
    val starClub: String = "", val starRating: Double = 0.0, val starValue: String = ""
)

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teamTla: String, val name: String, val position: String = "",
    val dateOfBirth: String = "", val goals: Int = 0, val assists: Int = 0
)

@Entity(tableName = "standings", primaryKeys = ["grp", "tla"])
data class StandingEntity(
    val grp: String, val tla: String, val position: Int,
    val pj: Int, val g: Int, val e: Int, val p: Int, val gf: Int, val gc: Int, val pts: Int
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String, val utcDate: String, val status: String,
    val stage: String, val grp: String, val homeTla: String, val awayTla: String,
    val scoreHome: Int?, val scoreAway: Int?
)

@Entity(tableName = "sync_meta")
data class SyncMeta(@PrimaryKey val key: String, val lastSyncEpochMs: Long)
```

- [ ] **Step 2: `dao/Daos.kt`**:
```kotlin
package com.mundial26.app.data.local.db.dao
import androidx.room.*
import com.mundial26.app.data.local.db.entities.*

@Dao interface TeamDao {
    @Upsert suspend fun upsertAll(items: List<TeamEntity>)
    @Query("SELECT * FROM teams") suspend fun all(): List<TeamEntity>
    @Query("SELECT * FROM teams WHERE tla = :tla") suspend fun byTla(tla: String): TeamEntity?
    @Query("SELECT COUNT(*) FROM teams") suspend fun count(): Int
}
@Dao interface PlayerDao {
    @Upsert suspend fun upsertAll(items: List<PlayerEntity>)
    @Query("SELECT * FROM players WHERE teamTla = :tla") suspend fun byTeam(tla: String): List<PlayerEntity>
    @Query("SELECT * FROM players ORDER BY goals DESC LIMIT :n") suspend fun topScorers(n: Int): List<PlayerEntity>
    @Query("SELECT * FROM players WHERE assists > 0 ORDER BY assists DESC LIMIT :n") suspend fun topAssists(n: Int): List<PlayerEntity>
    @Query("UPDATE players SET goals = :goals, assists = :assists WHERE teamTla = :tla AND name = :name") suspend fun updateStats(tla: String, name: String, goals: Int, assists: Int)
}
@Dao interface StandingDao {
    @Upsert suspend fun upsertAll(items: List<StandingEntity>)
    @Query("SELECT * FROM standings ORDER BY grp, position") suspend fun all(): List<StandingEntity>
}
@Dao interface MatchDao {
    @Upsert suspend fun upsertAll(items: List<MatchEntity>)
    @Query("SELECT * FROM matches ORDER BY utcDate") suspend fun all(): List<MatchEntity>
    @Query("SELECT * FROM matches WHERE id = :id") suspend fun byId(id: String): MatchEntity?
}
@Dao interface SyncMetaDao {
    @Upsert suspend fun put(meta: SyncMeta)
    @Query("SELECT lastSyncEpochMs FROM sync_meta WHERE key = :key") suspend fun lastSync(key: String): Long?
}
```

- [ ] **Step 3: `MundialDatabase.kt`**:
```kotlin
package com.mundial26.app.data.local.db
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mundial26.app.data.local.db.dao.*
import com.mundial26.app.data.local.db.entities.*

@Database(entities = [TeamEntity::class, PlayerEntity::class, StandingEntity::class, MatchEntity::class, SyncMeta::class], version = 1, exportSchema = false)
abstract class MundialDatabase : RoomDatabase() {
    abstract fun teamDao(): TeamDao
    abstract fun playerDao(): PlayerDao
    abstract fun standingDao(): StandingDao
    abstract fun matchDao(): MatchDao
    abstract fun syncMetaDao(): SyncMetaDao
    companion object {
        fun build(context: Context): MundialDatabase =
            Room.databaseBuilder(context, MundialDatabase::class.java, "mundial.db")
                .fallbackToDestructiveMigration().build()
    }
}
```

- [ ] **Step 4: Test DAO con Robolectric** (`DaoTest.kt`) — agregar dep `testImplementation("org.robolectric:robolectric:4.13")` a `app/build.gradle.kts` y `testOptions { unitTests.isIncludeAndroidResources = true }`:
```kotlin
package com.mundial26.app.data.local.db
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mundial26.app.data.local.db.entities.PlayerEntity
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DaoTest {
    private lateinit var db: MundialDatabase
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MundialDatabase::class.java).allowMainThreadQueries().build() }
    @After fun close() = db.close()

    @Test fun top_scorers_ordenados() = runTest {
        db.playerDao().upsertAll(listOf(
            PlayerEntity(teamTla = "ARG", name = "Messi", goals = 5, assists = 3),
            PlayerEntity(teamTla = "FRA", name = "Mbappé", goals = 4, assists = 2),
        ))
        val top = db.playerDao().topScorers(10)
        assertEquals("Messi", top[0].name); assertEquals(5, top[0].goals)
    }
}
```
> Si Robolectric resulta pesado, alternativa: tests instrumentados en `androidTest/` con `room-testing`. El RUNNER y deps son lo único que cambia.

- [ ] **Step 5: Correr test** — `.\gradlew.bat :app:testDebugUnitTest --console=plain` · Expected: PASS.
- [ ] **Step 6: Commit** — `git commit -am "feat(data): Room entidades + DAOs + DB (test DAO)"`

---

## Task 6: Mappers DTO→Entity y Entity→dominio (+ deriveMinute) (TDD)

**Files:** Create `data/remote/footballdata/FootballDataMappers.kt`, `data/local/db/DbMappers.kt`; Test `.../FootballDataMappersTest.kt`, `.../DbMappersTest.kt`

- [ ] **Step 1: Tests que fallan** (`FootballDataMappersTest.kt`):
```kotlin
package com.mundial26.app.data.remote.footballdata
import com.mundial26.app.domain.model.MatchStatus
import org.junit.Assert.*
import org.junit.Test
class FootballDataMappersTest {
    @Test fun match_a_entity() {
        val m = FdMatch(1, "2026-06-11T19:00:00Z", "FINISHED", "GROUP_STAGE", "GROUP_A", FdTeamRef("MEX"), FdTeamRef("RSA"), FdScore(FdGoals(2,0)))
        val e = m.toEntity()
        assertEquals("1", e.id); assertEquals("MEX", e.homeTla); assertEquals(2, e.scoreHome); assertEquals("GROUP_A", e.grp)
    }
    @Test fun standings_a_entities() {
        val resp = FdStandingsResponse(listOf(
            FdStanding("TOTAL", "Group A", listOf(FdTableRow(1, FdTeamRef("MEX"), 3,3,0,0,9,6,0))),
            FdStanding("HOME", "Group A", emptyList())
        ))
        val es = resp.toEntities(); assertEquals(1, es.size); assertEquals("Group A", es[0].grp); assertEquals(9, es[0].pts)
    }
    @Test fun deriva_minuto_si_live() {
        val kickoff = "2026-06-28T18:00:00Z"
        val now = java.time.Instant.parse("2026-06-28T19:07:00Z").toEpochMilli()
        assertEquals(67, deriveMinute(kickoff, MatchStatus.LIVE, now))
        assertNull(deriveMinute(kickoff, MatchStatus.FINISHED, now))
    }
}
```
(`DbMappersTest.kt`):
```kotlin
package com.mundial26.app.data.local.db
import com.mundial26.app.data.local.db.entities.*
import com.mundial26.app.domain.model.MatchStatus
import org.junit.Assert.*
import org.junit.Test
class DbMappersTest {
    @Test fun standing_entity_a_grouprow() {
        val row = StandingEntity("Group A","MEX",1,3,3,0,0,6,0,9).toDomain()
        assertEquals("MEX", row.code); assertEquals("mx", row.flag); assertEquals(6, row.dg)
    }
    @Test fun match_entity_a_dominio_con_minuto() {
        val e = MatchEntity("1","2026-06-28T18:00:00Z","IN_PLAY","GROUP_STAGE","GROUP_A","ARG","MEX",1,0)
        val now = java.time.Instant.parse("2026-06-28T18:30:00Z").toEpochMilli()
        val m = e.toDomain(now); assertEquals(MatchStatus.LIVE, m.status); assertEquals(30, m.minute)
    }
}
```

- [ ] **Step 2: Correr → FALLA.**

- [ ] **Step 3: `FootballDataMappers.kt`** (DTO→Entity + helpers):
```kotlin
package com.mundial26.app.data.remote.footballdata
import com.mundial26.app.data.local.db.entities.*
import com.mundial26.app.domain.model.MatchStatus
import java.time.Instant

fun statusOf(s: String): MatchStatus = when (s) {
    "FINISHED" -> MatchStatus.FINISHED
    "IN_PLAY","PAUSED","LIVE" -> MatchStatus.LIVE
    else -> MatchStatus.UPCOMING
}

fun deriveMinute(utcDate: String, status: MatchStatus, nowEpochMs: Long): Int? {
    if (status != MatchStatus.LIVE || utcDate.isBlank()) return null
    return try { (((nowEpochMs - Instant.parse(utcDate).toEpochMilli()) / 60000L).toInt()).coerceIn(1,120) }
    catch (e: Exception) { null }
}

fun FdMatch.toEntity() = MatchEntity(
    id = id.toString(), utcDate = utcDate, status = status, stage = stage,
    grp = group ?: "", homeTla = homeTeam.tla, awayTla = awayTeam.tla,
    scoreHome = score.fullTime.home, scoreAway = score.fullTime.away
)
fun FdMatchesResponse.toEntities() = matches.map { it.toEntity() }

fun FdStandingsResponse.toEntities(): List<StandingEntity> =
    standings.filter { it.type == "TOTAL" }.flatMap { s ->
        s.table.map { r -> StandingEntity(s.group ?: "", r.team.tla, r.position, r.playedGames, r.won, r.draw, r.lost, r.goalsFor, r.goalsAgainst, r.points) }
    }

/** squad → PlayerEntity (sin goals/assists; el sync de scorers los actualiza). */
fun FdTeam.toPlayerEntities(): List<PlayerEntity> =
    squad.map { PlayerEntity(teamTla = tla, name = it.name, position = it.position ?: "", dateOfBirth = it.dateOfBirth ?: "") }
```

- [ ] **Step 4: `DbMappers.kt`** (Entity→dominio):
```kotlin
package com.mundial26.app.data.local.db
import com.mundial26.app.data.net.tlaToIso
import com.mundial26.app.data.remote.footballdata.deriveMinute
import com.mundial26.app.data.remote.footballdata.statusOf
import com.mundial26.app.data.local.db.entities.*
import com.mundial26.app.domain.model.*

private fun prettyGroup(stage: String, grp: String): String = when {
    grp.isNotBlank() -> grp.replace("GROUP_", "Grupo ").replace("Group ", "Grupo ")
    stage == "LAST_16" -> "Octavos"; stage == "QUARTER_FINALS" -> "Cuartos"
    stage == "SEMI_FINALS" -> "Semifinal"; stage == "THIRD_PLACE" -> "Tercer puesto"
    stage == "FINAL" -> "Final"; else -> stage.replace("_", " ")
}

fun StandingEntity.toDomain() = GroupRow(code = tla, flag = tlaToIso(tla), pj = pj, g = g, e = e, p = p, gf = gf, gc = gc, pts = pts)

fun List<StandingEntity>.toGroups(): List<Group> =
    groupBy { it.grp }.map { (g, rows) -> Group(g.replace("Group ", "Grupo "), rows.sortedBy { it.position }.map { it.toDomain() }) }

fun MatchEntity.toDomain(nowEpochMs: Long = System.currentTimeMillis()): Match {
    val st = statusOf(status)
    return Match(
        id = id, status = st, minute = deriveMinute(utcDate, st, nowEpochMs),
        time = utcDate.takeIf { it.length >= 16 }?.substring(11, 16), day = utcDate.take(10),
        group = prettyGroup(stage, grp), stadium = "",
        teamA = homeTla, teamB = awayTla, flagA = tlaToIso(homeTla), flagB = tlaToIso(awayTla),
        scoreA = scoreHome, scoreB = scoreAway
    )
}

fun PlayerEntity.toLeader(rank: Int, value: Int) = Leader(rank, name, tlaToIso(teamTla), value)

fun TeamEntity.toDomain() = Team(
    id = tla.lowercase(), name = name, code = tla,
    conf = runCatching { Confederation.valueOf(conf) }.getOrDefault(Confederation.OTHER),
    rank = rank, tag = tag, flag = tlaToIso(tla), styleLabel = styleLabel,
    star = Star(starNum, starName, starPos, starNote, StarStats(0,0,0, starRating, starClub, 0, starValue)),
    pros = emptyList(), cons = emptyList(), form = emptyList()
)
```
> Nota: `pros/cons` y la edad/goals de la estrella se completan en el repo desde `EditorialSource` + `PlayerDao` (Task 11). `toDomain()` del Team da la base.

- [ ] **Step 5: Correr → PASS. Step 6: Commit** — `git commit -am "feat(data): mappers DTO→Entity y Entity→dominio + deriveMinute"`

---

## Task 7: openfootball eventos (TDD)

**Files:** Create `data/remote/openfootball/{OpenFootballDtos,OpenFootballApi,OpenFootballMappers}.kt`; Test `OpenFootballMappersTest.kt`

- [ ] **Step 1: DTOs + API + Test + Mapper** — idéntico al diseño: `OfGoal(name,minute)`, `OfMatch(date,team1,team2,goals1,goals2)`, `OfWorldCup(matches)`; `@GET("openfootball/worldcup.json/master/2026/worldcup.json")`. Test: `toEvents()` produce 2 eventos GOAL con `side` 'A'/'B' ordenados por minuto; `fixEncoding("JuliÃ¡n")=="Julián"`. Implementación:
```kotlin
package com.mundial26.app.data.remote.openfootball
import com.mundial26.app.domain.model.EventKind
import com.mundial26.app.domain.model.MatchEvent
fun fixEncoding(s: String): String = try { String(s.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8) } catch (e: Exception) { s }
fun OfMatch.toEvents(): List<MatchEvent> =
    (goals1.map { MatchEvent(it.minute ?: 0, EventKind.GOAL, 'A', fixEncoding(it.name)) } +
     goals2.map { MatchEvent(it.minute ?: 0, EventKind.GOAL, 'B', fixEncoding(it.name)) }).sortedBy { it.min }
fun List<OfMatch>.findEventsFor(day: String, teamAName: String, teamBName: String): List<MatchEvent> =
    firstOrNull { it.date == day && (it.team1.contains(teamAName, true) || teamAName.contains(it.team1, true)) && (it.team2.contains(teamBName, true) || teamBName.contains(it.team2, true)) }?.toEvents() ?: emptyList()
```
- [ ] **Step 2: Correr → PASS. Commit** — `git commit -am "feat(data): openfootball eventos de gol"`

---

## Task 8: Noticias RSS (TDD)

**Files:** Create `data/remote/news/NewsRssSource.kt`; Test `NewsRssSourceTest.kt`

- [ ] **Step 1: Test** — `parseRss(xml)` con un `<item>` (title CDATA, link, pubDate, source, description) → 1 `NewsItem` con `id=link`, `tag=source`, `body` vacío.
- [ ] **Step 2: FALLA.**
- [ ] **Step 3: Implementación** (parser puro + fetch OkHttp):
```kotlin
package com.mundial26.app.data.remote.news
import com.mundial26.app.domain.model.NewsItem
import okhttp3.OkHttpClient
import okhttp3.Request
private val ITEM = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
private fun tag(b: String, n: String) = Regex("<$n[^>]*>(.*?)</$n>", RegexOption.DOT_MATCHES_ALL).find(b)?.groupValues?.get(1)?.replace("<![CDATA[","")?.replace("]]>","")?.trim() ?: ""
fun parseRss(xml: String): List<NewsItem> = ITEM.findAll(xml).map { it.groupValues[1] }.map { b ->
    NewsItem(id = tag(b,"link"), tag = tag(b,"source").ifBlank { "Noticias" }, colorHex = 0xFF2A398D, time = tag(b,"pubDate"), title = tag(b,"title"), summary = tag(b,"description"), body = emptyList())
}.toList()
class NewsRssSource(private val client: OkHttpClient = OkHttpClient(), private val url: String = "https://news.google.com/rss/search?q=mundial%202026%20futbol&hl=es-419&gl=AR&ceid=AR:es-419") {
    suspend fun headlines(): List<NewsItem> = try {
        client.newCall(Request.Builder().url(url).build()).execute().use { if (it.isSuccessful) parseRss(it.body?.string().orEmpty()) else emptyList() }
    } catch (e: Exception) { emptyList() }
}
```
- [ ] **Step 4: PASS. Commit** — `git commit -am "feat(data): noticias RSS"`

---

## Task 9: EditorialSource

**Files:** Create `data/editorial/EditorialSource.kt`

- [ ] **Step 1:** Objeto `EditorialSource` con `featuredCode = "ARG"` y `byCode: Map<String, TeamEditorial>` portando **todos** los equipos de `MockData.teams` (conf, rank, tag, styleLabel, star, pros, cons). Mostrar ARG completo como ejemplo y portar el resto idéntico:
```kotlin
package com.mundial26.app.data.editorial
import com.mundial26.app.domain.model.*
data class TeamEditorial(val conf: Confederation, val rank: Int, val tag: String, val styleLabel: String, val star: Star, val pros: List<String>, val cons: List<String>)
object EditorialSource {
    const val featuredCode = "ARG"
    val byCode: Map<String, TeamEditorial> = mapOf(
        "ARG" to TeamEditorial(Confederation.CONMEBOL, 1, "Campeón defensor", "Posesión vertical",
            Star(10,"Lionel Messi","Delantero / Capitán","Cerebro creativo del equipo: maneja los tiempos y aparece en los momentos decisivos.", StarStats(6,4,3,8.4,"Inter Miami",38,"€20M")),
            listOf("Jerarquía y experiencia en partidos clave","Mediocampo equilibrado y muy trabajador","Definición letal de sus delanteros"),
            listOf("Defensa con poca velocidad","Dependencia de Messi en la creación","Plantel con varios veteranos"))
        // … portar el resto de MockData.teams igual.
    )
    fun forCode(code: String) = byCode[code.uppercase()]
}
```
- [ ] **Step 2: Compilar → SUCCESS. Commit** — `git commit -am "feat(data): EditorialSource"`

---

## Task 10: SyncManager (seed + refresh) (TDD con fakes)

**Files:** Create `data/sync/SyncManager.kt`; Test `data/sync/SyncManagerTest.kt`

- [ ] **Step 1: Test** — con un `FootballDataApi` fake (devuelve teams con squad + scorers), correr `SyncManager.seedIfEmpty()` y verificar que `teamDao.count()` > 0 y que `playerDao.byTeam("ARG")` trae jugadores; correr `refreshLeaders()` y verificar que `playerDao.topScorers(1)[0].goals` refleja el scorer. (Usar Room in-memory como en Task 5.)

- [ ] **Step 2: `SyncManager.kt`**:
```kotlin
package com.mundial26.app.data.sync
import com.mundial26.app.data.editorial.EditorialSource
import com.mundial26.app.data.local.db.dao.*
import com.mundial26.app.data.local.db.entities.*
import com.mundial26.app.data.remote.footballdata.*

class SyncManager(
    private val api: FootballDataApi,
    private val teamDao: TeamDao, private val playerDao: PlayerDao,
    private val standingDao: StandingDao, private val matchDao: MatchDao,
    private val metaDao: SyncMetaDao,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val ttlMs: Long = 6 * 60 * 60 * 1000L
) {
    private suspend fun stale(key: String) = (metaDao.lastSync(key) ?: 0L) < now() - ttlMs

    suspend fun seedIfEmpty() {
        if (teamDao.count() > 0) return
        runCatching {
            val teams = api.teams().teams
            teamDao.upsertAll(teams.map { t ->
                val ed = EditorialSource.forCode(t.tla)
                TeamEntity(tla = t.tla, name = t.name, conf = ed?.conf?.name ?: "OTHER", rank = ed?.rank ?: 0,
                    tag = ed?.tag ?: "", styleLabel = ed?.styleLabel ?: "",
                    starNum = ed?.star?.num ?: 0, starName = ed?.star?.name ?: "", starPos = ed?.star?.pos ?: "",
                    starNote = ed?.star?.note ?: "", starClub = ed?.star?.stats?.club ?: "",
                    starRating = ed?.star?.stats?.rating ?: 0.0, starValue = ed?.star?.stats?.marketValue ?: "")
            })
            playerDao.upsertAll(teams.flatMap { it.toPlayerEntities() })
            metaDao.put(SyncMeta("teams", now()))
        }
    }

    suspend fun refreshIfStale() {
        if (stale("standings")) runCatching { standingDao.upsertAll(api.standings().toEntities()); metaDao.put(SyncMeta("standings", now())) }
        if (stale("matches")) runCatching { matchDao.upsertAll(api.matches().toEntities()); metaDao.put(SyncMeta("matches", now())) }
        if (stale("scorers")) runCatching {
            api.scorers().scorers.forEach { s -> playerDao.updateStats(s.team.tla, s.player.name, s.goals ?: 0, s.assists ?: 0) }
            metaDao.put(SyncMeta("scorers", now()))
        }
    }

    suspend fun ensureFresh() { seedIfEmpty(); refreshIfStale() }
}
```
> El `updateStats` actualiza jugadores existentes por (TLA+nombre). Los nombres de `/scorers` y `/teams.squad` pueden diferir levemente; si no matchea, queda en 0 (aceptable en v3; mejora futura: upsert del scorer como jugador nuevo).

- [ ] **Step 3: PASS. Commit** — `git commit -am "feat(data): SyncManager (seed + refresh por TTL)"`

---

## Task 11: DefaultTeamRepository

**Files:** Create `data/repository/DefaultTeamRepository.kt`

- [ ] **Step 1:** Lee de DAOs; `ensureFresh()` antes de leer; combina editorial (pros/cons) y stats reales del jugador estrella:
```kotlin
package com.mundial26.app.data.repository
import com.mundial26.app.data.editorial.EditorialSource
import com.mundial26.app.data.local.db.dao.*
import com.mundial26.app.data.local.db.toDomain
import com.mundial26.app.data.local.db.toGroups
import com.mundial26.app.data.local.db.toLeader
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.data.sync.SyncManager
import com.mundial26.app.domain.model.*
import com.mundial26.app.domain.repository.TeamRepository

class DefaultTeamRepository(
    private val sync: SyncManager, private val teamDao: TeamDao,
    private val playerDao: PlayerDao, private val standingDao: StandingDao
) : TeamRepository {

    private suspend fun fresh() = runCatching { sync.ensureFresh() }

    override suspend fun teams(): List<Team> {
        fresh()
        val rows = teamDao.all()
        if (rows.isEmpty()) return MockData.teams
        return rows.map { t ->
            val ed = EditorialSource.forCode(t.tla)
            val players = playerDao.byTeam(t.tla)
            val starP = players.firstOrNull { it.name.equals(t.starName, true) }
            t.toDomain().copy(
                pros = ed?.pros ?: emptyList(), cons = ed?.cons ?: emptyList(),
                star = t.toDomain().star.let { s -> s.copy(stats = s.stats.copy(goals = starP?.goals ?: 0, assists = starP?.assists ?: 0, age = ageFrom(starP?.dateOfBirth))) }
            )
        }
    }
    override suspend fun featured(): Team = teams().firstOrNull { it.code == EditorialSource.featuredCode } ?: MockData.teams.first()
    override suspend fun groups(): List<Group> { fresh(); return standingDao.all().toGroups().ifEmpty { MockData.groups } }
    override suspend fun team(id: String): Team? = teams().firstOrNull { it.id == id || it.code.equals(id, true) }
    override suspend fun topScorers(): List<Leader> { fresh(); return playerDao.topScorers(10).mapIndexed { i, p -> p.toLeader(i+1, p.goals) }.ifEmpty { MockData.topScorers } }
    override suspend fun topAssists(): List<Leader> { fresh(); return playerDao.topAssists(10).mapIndexed { i, p -> p.toLeader(i+1, p.assists) }.ifEmpty { MockData.topAssists } }
}

private fun ageFrom(dob: String?): Int {
    if (dob.isNullOrBlank()) return 0
    return runCatching { java.time.Period.between(java.time.LocalDate.parse(dob.take(10)), java.time.LocalDate.now()).years }.getOrDefault(0)
}
```
- [ ] **Step 2: Compilar → SUCCESS. Commit** — `git commit -am "feat(data): DefaultTeamRepository (Room + editorial)"`

---

## Task 12: DefaultMatchRepository

**Files:** Create `data/repository/DefaultMatchRepository.kt`

- [ ] **Step 1:**
```kotlin
package com.mundial26.app.data.repository
import com.mundial26.app.data.local.db.dao.MatchDao
import com.mundial26.app.data.local.db.toDomain
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.data.remote.openfootball.OpenFootballApi
import com.mundial26.app.data.remote.openfootball.findEventsFor
import com.mundial26.app.data.sync.SyncManager
import com.mundial26.app.domain.model.*
import com.mundial26.app.domain.repository.MatchRepository

class DefaultMatchRepository(
    private val sync: SyncManager, private val matchDao: MatchDao, private val of: OpenFootballApi
) : MatchRepository {
    private suspend fun all(): List<Match> { runCatching { sync.ensureFresh() }; return matchDao.all().map { it.toDomain() }.ifEmpty { MockData.matches } }
    override suspend fun liveMatches() = all().filter { it.status == MatchStatus.LIVE }
    override suspend fun resultsAndUpcoming() = all().filter { it.status != MatchStatus.LIVE }
    override suspend fun byDay() = all().filter { it.day != null }.groupBy { it.day!! }
    override suspend fun bracket(): List<BracketRound> = MockData.bracket  // derivar de stage: iteración futura
    override suspend fun match(id: String): Match? {
        val m = all().firstOrNull { it.id == id } ?: return null
        val events = runCatching { of.worldCup().matches.findEventsFor(m.day ?: "", m.teamA, m.teamB) }.getOrDefault(emptyList())
        return m.copy(events = events)
    }
}
```
- [ ] **Step 2: Compilar → SUCCESS. Commit** — `git commit -am "feat(data): DefaultMatchRepository (Room + openfootball)"`

---

## Task 13: DefaultNewsRepository

**Files:** Create `data/repository/DefaultNewsRepository.kt`

- [ ] **Step 1:**
```kotlin
package com.mundial26.app.data.repository
import com.mundial26.app.data.mock.MockData
import com.mundial26.app.data.remote.news.NewsRssSource
import com.mundial26.app.domain.model.NewsItem
import com.mundial26.app.domain.repository.NewsRepository

class DefaultNewsRepository(private val rss: NewsRssSource) : NewsRepository {
    private var cache: List<NewsItem> = emptyList()
    private var at: Long = 0
    override suspend fun news(): List<NewsItem> {
        if (cache.isEmpty() || System.currentTimeMillis() - at > 3 * 60 * 60 * 1000L) {
            val fresh = rss.headlines()
            if (fresh.isNotEmpty()) { cache = fresh; at = System.currentTimeMillis() }
        }
        return cache.ifEmpty { MockData.news }
    }
    override suspend fun item(id: String): NewsItem? = news().firstOrNull { it.id == id }
}
```
- [ ] **Step 2: Compilar → SUCCESS. Commit** — `git commit -am "feat(data): DefaultNewsRepository (RSS)"`

---

## Task 14: Wire AppContainer → Room/Default* + verificación real

**Files:** Modify `di/AppContainer.kt`, `MundialApplication.kt` (pasar `Context`)

- [ ] **Step 1: `AppContainer.kt`** — recibe `Context`, construye DB + APIs + SyncManager + Default*:
```kotlin
package com.mundial26.app.di
import android.content.Context
import com.mundial26.app.data.local.db.MundialDatabase
import com.mundial26.app.data.net.footballDataRetrofit
import com.mundial26.app.data.net.openFootballRetrofit
import com.mundial26.app.data.remote.footballdata.FootballDataApi
import com.mundial26.app.data.remote.news.NewsRssSource
import com.mundial26.app.data.remote.openfootball.OpenFootballApi
import com.mundial26.app.data.repository.*
import com.mundial26.app.data.sync.SyncManager
import com.mundial26.app.domain.repository.*

interface AppContainer { val matchRepository: MatchRepository; val teamRepository: TeamRepository; val newsRepository: NewsRepository }

class DefaultAppContainer(context: Context) : AppContainer {
    private val db = MundialDatabase.build(context.applicationContext)
    private val fdApi = footballDataRetrofit().create(FootballDataApi::class.java)
    private val ofApi = openFootballRetrofit().create(OpenFootballApi::class.java)
    private val sync = SyncManager(fdApi, db.teamDao(), db.playerDao(), db.standingDao(), db.matchDao(), db.syncMetaDao())
    override val teamRepository: TeamRepository by lazy { DefaultTeamRepository(sync, db.teamDao(), db.playerDao(), db.standingDao()) }
    override val matchRepository: MatchRepository by lazy { DefaultMatchRepository(sync, db.matchDao(), ofApi) }
    override val newsRepository: NewsRepository by lazy { DefaultNewsRepository(NewsRssSource()) }
}
```

- [ ] **Step 2: `MundialApplication.kt`** — `container = DefaultAppContainer(this)` (pasar el contexto).

- [ ] **Step 3: Tests + build** — `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain` · Expected: PASS + BUILD SUCCESSFUL.

- [ ] **Step 4: Emulador (red real)** — `adb install -r`, lanzar. Confirmar: Equipos 48 reales + grupos A–L reales; Inicio: líderes reales (Messi/Mbappé…), posiciones reales, noticias RSS; Partidos: calendario/resultados reales, live con minuto derivado; detalle: eventos de gol. Cerrar y reabrir **sin red** → la app sigue mostrando lo persistido (offline). Primer arranque sin red → cae a mock sin crashear.

- [ ] **Step 5: Commit** — `git commit -am "feat(di): AppContainer con Room + repos reales"`

---

## Task 15: Link-out de noticias

**Files:** Modify `ui/newsdetail/NewsDetailScreen.kt`

- [ ] **Step 1:** En `NewsDetailContent`, debajo del resumen, si `item.id` empieza con "http" agregar botón con `LocalUriHandler`:
```kotlin
val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
if (item.id.startsWith("http")) { Button(onClick = { uriHandler.openUri(item.id) }) { Text("Leer la nota") } }
```
- [ ] **Step 2: Emulador** — abrir noticia → "Leer la nota" abre navegador. **Step 3: Commit** — `git commit -am "feat(newsdetail): link-out RSS"`

---

## Task 16: Verificación final v3

- [ ] **Step 1: Tests** — `:app:testDebugUnitTest` PASS (mappers, DAOs, sync, tlaToIso, openfootball, RSS).
- [ ] **Step 2: Build** — `:app:assembleDebug` BUILD SUCCESSFUL.
- [ ] **Step 3: Emulador con red** — datos reales en las 4 pantallas + detalles + widget; offline tras seed; degradación a mock sin datos.
- [ ] **Step 4: Token** — `git log -p | grep FOOTBALL_DATA_TOKEN` no muestra el valor real.
- [ ] **Step 5: Commit final** — `git commit -am "chore: v3 datos reales (Room) completa y verificada"`

---

## Definición de "Hecho" (v3)
- La app lee de Room, sembrada con 48 equipos + planteles y refrescada por `SyncManager` (goleadores/asistidores derivados de `Player`); muestra datos reales WC2026 detrás de las `Repository`, sin cambios de UI salvo link-out.
- Offline tras el primer seed; degrada a `MockData` sin datos.
- Tests de mappers/DAOs/sync en verde; token solo en `local.properties` (gitignored).
