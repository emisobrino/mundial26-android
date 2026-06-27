# Mundial 26 — v2 (Pantallas de Detalle) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Agregar las 4 pantallas de detalle (partido, equipo, jugador, noticia) con navegación por argumentos, llegando a ellas desde las pantallas raíz de v1, sin refactorizar la arquitectura existente.

**Architecture:** Mismo MVVM + Repository de v1. Cada detalle = un `ViewModel` (estado `sealed UiState` Loading/Success/Error, factory que recibe el repo del `AppContainer`) + un `Screen` Composable. Las rutas de detalle viven en el **mismo `NavHost`** dentro del `Scaffold`; la `BottomNav` se **oculta** en rutas de detalle. Los datos ya existen en los repos mock (`match(id)`, `team(id)`, `item(id)`, `featured()`); solo falta extender el modelo con las stats del jugador.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3, Navigation-Compose (rutas con `{id}` + `NavType.StringType`), ViewModel + StateFlow, Coil. (Sin dependencias nuevas.)

> **Referencia visual:** cada pantalla de detalle se compara contra su captura en
> `C:\Users\emiso\OneDrive\Desktop\design_handoff_mundial2026\screens\` — `08-detalle-partido.png`, `09-detalle-equipo.png`, `10-detalle-jugador.png`, `11-detalle-noticia.png`.
> Spec: `docs/specs/2026-06-25-mundial26-android-design.md` (sección 7). Plan v1: `docs/plans/2026-06-25-mundial26-v1-rebanada-vertical.md`.

> **Toolchain:** no hay `JAVA_HOME` global. Correr Gradle con el JBR de Android Studio:
> en PowerShell `\$env:JAVA_HOME = "C:\Program Files\Android\Android Studio1\jbr"` antes de cada `.\gradlew.bat …`.
> (Sugerencia opcional: fijar `org.gradle.java.home` en `gradle.properties`.)

---

## Estado de partida (verificado)

- v1 completo: 4 pantallas raíz + nav + theme + mock + tests verdes.
- Los componentes de lista **ya exponen hooks de click** con placeholders `{}`:
  - `MatchRow(..., onClick: (() -> Unit)? = null)` — `ui/components/MatchRow.kt:47`
  - `LiveMatchCard(..., onClick: (() -> Unit)? = null)` — `ui/components/LiveMatchCard.kt:38`
  - `NewsCardLarge(..., onClick: (() -> Unit)? = null)` y `NewsCardCompact(...)` — `ui/components/NewsCard.kt:37,90`
  - `TeamCard` usa `.clickable { }` vacío — `ui/teams/TeamsScreen.kt:230`
- Rutas de detalle declaradas pero **sin registrar**: `ui/navigation/Destinations.kt` (`object DetailRoutes`, faltan jugador + builders).
- `MockData.matches` ya trae `stats` y `events` poblados (ver `data/mock/MockData.kt:321+`).
- Único gap de datos: `Star` no tiene stats numéricas (pj/goles/asistencias/valoración/club/edad/valor) que pide `10-detalle-jugador.png`.

## Estructura de archivos (v2)

```
app/src/main/java/com/mundial26/app/
├── domain/model/Models.kt              (MODIFY: + StarStats, Star.stats)
├── data/mock/MockData.kt               (MODIFY: poblar stats de estrellas)
├── ui/navigation/Destinations.kt       (MODIFY: + PLAYER_DETAIL + builders de ruta)
├── MundialApp.kt                       (MODIFY: registrar 4 rutas + ocultar bottom nav en detalle + threading de callbacks)
├── ui/home/HomeScreen.kt               (MODIFY: params onMatchClick/onNewsClick)
├── ui/matches/MatchesScreen.kt         (MODIFY: param onMatchClick)
├── ui/teams/TeamsScreen.kt             (MODIFY: param onTeamClick)
├── ui/news/NewsScreen.kt               (MODIFY: param onNewsClick)
├── ui/matchdetail/MatchDetailViewModel.kt   (CREATE)
├── ui/matchdetail/MatchDetailScreen.kt      (CREATE)
├── ui/newsdetail/NewsDetailViewModel.kt     (CREATE)
├── ui/newsdetail/NewsDetailScreen.kt        (CREATE)
├── ui/teamdetail/TeamDetailViewModel.kt     (CREATE)
├── ui/teamdetail/TeamDetailScreen.kt        (CREATE)
├── ui/playerdetail/PlayerDetailViewModel.kt (CREATE)
└── ui/playerdetail/PlayerDetailScreen.kt    (CREATE)

app/src/test/java/com/mundial26/app/data/mock/FakeRepositoriesTest.kt  (MODIFY: + lookups y StarStats)
```

**Orden de ejecución:** primero el modelo (Task 1), después cada detalle como composable previsualizable y aislado (Tasks 2–5), y al final el cableado de navegación que conecta todo y se verifica en emulador (Task 6) + verificación final (Task 7).

---

## Task 1: Extender el modelo con stats de jugador (StarStats)

**Files:**
- Modify: `app/src/main/java/com/mundial26/app/domain/model/Models.kt:9`
- Modify: `app/src/main/java/com/mundial26/app/data/mock/MockData.kt` (constructores de `Star(...)`)
- Test: `app/src/test/java/com/mundial26/app/data/mock/FakeRepositoriesTest.kt`

- [ ] **Step 1: Escribir el test que falla** — agregar al final de la clase `FakeRepositoriesTest`:

```kotlin
    @Test fun estrella_destacada_tiene_stats_de_mundial() {
        val star = teams.featured().star
        assertTrue("la estrella destacada debe tener goles > 0", star.stats.goals > 0)
        assertTrue("debe tener club", star.stats.club.isNotBlank())
        assertTrue("valoración en rango", star.stats.rating in 0.0..10.0)
    }

    @Test fun lookup_partido_por_id_funciona() {
        val first = matches.resultsAndUpcoming().firstOrNull() ?: matches.liveMatches().first()
        assertEquals(first, matches.match(first.id))
        assertNull(matches.match("no-existe"))
    }

    @Test fun lookup_noticia_por_id_funciona() {
        val first = news.news().first()
        assertEquals(first, news.item(first.id))
        assertNull(news.item("no-existe"))
    }
```

Asegurar el import `import org.junit.Assert.assertNull` (junto a los `assert*` ya presentes).

- [ ] **Step 2: Correr el test, verificar que falla** — PowerShell:

```
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio1\jbr"
.\gradlew.bat :app:testDebugUnitTest --console=plain
```
Expected: FALLA al compilar (`star.stats` no existe).

- [ ] **Step 3: Agregar `StarStats` y el campo `stats` a `Star`** en `Models.kt` (reemplazar la línea 9):

```kotlin
data class StarStats(
    val pj: Int = 0,
    val goals: Int = 0,
    val assists: Int = 0,
    val rating: Double = 0.0,
    val club: String = "",
    val age: Int = 0,
    val marketValue: String = ""   // ej. "€120M"
)

data class Star(
    val num: Int,
    val name: String,
    val pos: String,
    val note: String,
    val stats: StarStats = StarStats()
)
```

> El valor por defecto `StarStats()` evita romper los `Star(...)` existentes en `MockData.kt`; solo poblamos los que importan.

- [ ] **Step 4: Poblar stats reales para la estrella destacada (y 2–3 más visibles)** en `MockData.kt`. Localizar el `Star(...)` del equipo `arg` (featured) y agregar el argumento `stats`. Valores fieles a `10-detalle-jugador.png`:

```kotlin
// Argentina → Lionel Messi (estrella destacada, screen 10)
star = Star(
    num = 10,
    name = "Lionel Messi",
    pos = "Delantero / Capitán",
    note = "Cerebro creativo del equipo: maneja los tiempos y aparece en los momentos decisivos.",
    stats = StarStats(pj = 6, goals = 4, assists = 3, rating = 8.4, club = "Inter Miami", age = 38, marketValue = "€20M")
),
```

Poblar de forma análoga las estrellas de al menos 2 equipos más (ej. `bra`, `fra`) con valores plausibles, para que el detalle de jugador no muestre ceros al navegar desde otros equipos. (No es necesario poblar los 8+; los no poblados quedan en `StarStats()` por defecto.)

- [ ] **Step 5: Correr el test, verificar PASS** — `.\gradlew.bat :app:testDebugUnitTest --console=plain` · Expected: PASS (todos los tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mundial26/app/domain/model/Models.kt app/src/main/java/com/mundial26/app/data/mock/MockData.kt app/src/test/java/com/mundial26/app/data/mock/FakeRepositoriesTest.kt
git commit -m "feat(domain): StarStats para detalle de jugador + lookups testeados"
```

---

## Task 2: Detalle de partido (ViewModel + Screen)  ·  `screens/08`

**Files:**
- Create: `app/src/main/java/com/mundial26/app/ui/matchdetail/MatchDetailViewModel.kt`
- Create: `app/src/main/java/com/mundial26/app/ui/matchdetail/MatchDetailScreen.kt`

- [ ] **Step 1: `MatchDetailViewModel.kt`** (sigue el patrón de `HomeViewModel`):

```kotlin
package com.mundial26.app.ui.matchdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mundial26.app.domain.model.Match
import com.mundial26.app.domain.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MatchDetailUiState {
    data object Loading : MatchDetailUiState
    data class Success(val match: Match) : MatchDetailUiState
    data class Error(val message: String) : MatchDetailUiState
}

class MatchDetailViewModel(
    private val matchRepository: MatchRepository,
    private val matchId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchDetailUiState>(MatchDetailUiState.Loading)
    val uiState: StateFlow<MatchDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() = load()

    private fun load() {
        _uiState.value = MatchDetailUiState.Loading
        viewModelScope.launch {
            val match = matchRepository.match(matchId)
            _uiState.value = if (match != null) {
                MatchDetailUiState.Success(match)
            } else {
                MatchDetailUiState.Error("Partido no encontrado")
            }
        }
    }

    companion object {
        fun Factory(matchRepository: MatchRepository, matchId: String) = viewModelFactory {
            initializer { MatchDetailViewModel(matchRepository, matchId) }
        }
    }
}
```

- [ ] **Step 2: `MatchDetailScreen.kt`** — implementar fiel a `08-detalle-partido.png`. Estructura (usar `MundialTheme.colors`/`typography`, `FlagAvatar`, `LivePulseDot`):

  - **Top bar** (fondo claro): ícono back `Icons.AutoMirrored.Filled.ArrowBack` (botón) + texto `match.group` (ej. "Grupo A") en `titleMedium`/Bold. `onBack: () -> Unit`.
  - **Hero oscuro** (`MundialTheme.colors.surfaceDark`, esquinas redondeadas, franja tricolor verde/rojo/azul de 4dp arriba — reutilizar el patrón de `BrandLogo26`): a la izq `FlagAvatar(iso=match.flagA, code=match.teamA, size=64.dp)` + código `teamA` en blanco/900; al centro marcador `"${match.scoreA} - ${match.scoreB}"` grande (estilo `ScoreboardCard` o 46sp/900, blanco) y, si `status==LIVE`, badge `● ${minute}'` rojo con `LivePulseDot`; a la der `FlagAvatar` + `teamB`. Debajo, centrado, `match.stadium` en `textTertiary`.
  - **ESTADÍSTICAS** (`SectionHeader` o label 15sp/900): tarjeta blanca con una fila por `MatchStat`: valor `a` (izq, brand/Bold) — `label` (centro, textSecondary) — valor `b` (der). Debajo una barra de proporción: dos segmentos con ancho relativo `a/(a+b)` y `b/(a+b)` (brand vs border/gris). Iterar `match.stats`.
  - **EVENTOS** (si `match.events` no vacío): lista vertical; cada `MatchEvent` = minuto `${min}'` + ícono según `kind` (GOAL ⚽, YELLOW 🟨, RED 🟥, SUB 🔁 — o un punto coloreado) + `player`, alineado a izq (side 'A') o der (side 'B').
  - Todo dentro de un `LazyColumn`/`Column` scrollable con fondo `appBg`. Params: `viewModel: MatchDetailViewModel`, `onBack: () -> Unit`.
  - Manejar `Loading` (`CircularProgressIndicator` brand) y `Error` (mensaje + botón "Reintentar" → `viewModel::retry`), igual que las pantallas raíz.

- [ ] **Step 3: `@Preview`** privado con un `MatchDetailScreen` envuelto en `MundialTheme`, construyendo el VM con `FakeMatchRepository()` y `MockData.matches[0].id` (live ARG-MEX). Compilar:

```
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio1\jbr"
.\gradlew.bat :app:compileDebugKotlin --console=plain
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/mundial26/app/ui/matchdetail/
git commit -m "feat(matchdetail): pantalla detalle de partido + VM"
```

---

## Task 3: Detalle de noticia (ViewModel + Screen)  ·  `screens/11`

**Files:**
- Create: `app/src/main/java/com/mundial26/app/ui/newsdetail/NewsDetailViewModel.kt`
- Create: `app/src/main/java/com/mundial26/app/ui/newsdetail/NewsDetailScreen.kt`

- [ ] **Step 1: `NewsDetailViewModel.kt`** (patrón idéntico al de Task 2, con `NewsRepository.item(id)`):

```kotlin
package com.mundial26.app.ui.newsdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mundial26.app.domain.model.NewsItem
import com.mundial26.app.domain.repository.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NewsDetailUiState {
    data object Loading : NewsDetailUiState
    data class Success(val item: NewsItem) : NewsDetailUiState
    data class Error(val message: String) : NewsDetailUiState
}

class NewsDetailViewModel(
    private val newsRepository: NewsRepository,
    private val newsId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewsDetailUiState>(NewsDetailUiState.Loading)
    val uiState: StateFlow<NewsDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() = load()

    private fun load() {
        _uiState.value = NewsDetailUiState.Loading
        viewModelScope.launch {
            try {
                val item = newsRepository.item(newsId)
                _uiState.value = if (item != null) {
                    NewsDetailUiState.Success(item)
                } else {
                    NewsDetailUiState.Error("Noticia no encontrada")
                }
            } catch (e: Exception) {
                _uiState.value = NewsDetailUiState.Error(e.message ?: "Error al cargar la noticia")
            }
        }
    }

    companion object {
        fun Factory(newsRepository: NewsRepository, newsId: String) = viewModelFactory {
            initializer { NewsDetailViewModel(newsRepository, newsId) }
        }
    }
}
```

- [ ] **Step 2: `NewsDetailScreen.kt`** — fiel a `11-detalle-noticia.png`:
  - **Hero band** a sangre completa (alto ~220dp), fondo `Color(item.colorHex).copy(alpha = 0.16f)`; botón back circular flotante (fondo `surface`, ícono back) en la esquina sup-izq sobre el hero; badge de categoría `item.tag.uppercase()` (fondo sólido `Color(item.colorHex)`, texto blanco) anclado abajo-izq del hero.
  - **Título** `item.title` grande (`headlineSmall`/Bold, textPrimary), bajo el hero, con padding 16dp.
  - **Byline**: mini-logo "26" (reusar `BrandLogo26(size = 22.dp)`) + "Redacción Mundial 26" (Bold) + "· ${item.time}" (textTertiary).
  - **Cuerpo**: iterar `item.body` como párrafos (`bodyLarge`/textSecondary, espaciados 12dp) en `Column` scrollable.
  - Params: `viewModel`, `onBack`. Estados Loading/Error como siempre.

- [ ] **Step 3: `@Preview`** con `FakeNewsRepository()` y `MockData.news[0].id`. Compilar `:app:compileDebugKotlin` · Expected: SUCCESS.
- [ ] **Step 4: Commit** — `git add app/src/main/java/com/mundial26/app/ui/newsdetail/ && git commit -m "feat(newsdetail): pantalla detalle de noticia + VM"`

---

## Task 4: Detalle de equipo (ViewModel + Screen)  ·  `screens/09`

**Files:**
- Create: `app/src/main/java/com/mundial26/app/ui/teamdetail/TeamDetailViewModel.kt`
- Create: `app/src/main/java/com/mundial26/app/ui/teamdetail/TeamDetailScreen.kt`

- [ ] **Step 1: `TeamDetailViewModel.kt`** (patrón Task 2, con `TeamRepository.team(id)` → `Team`):

```kotlin
package com.mundial26.app.ui.teamdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mundial26.app.domain.model.Team
import com.mundial26.app.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TeamDetailUiState {
    data object Loading : TeamDetailUiState
    data class Success(val team: Team) : TeamDetailUiState
    data class Error(val message: String) : TeamDetailUiState
}

class TeamDetailViewModel(
    private val teamRepository: TeamRepository,
    private val teamId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<TeamDetailUiState>(TeamDetailUiState.Loading)
    val uiState: StateFlow<TeamDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() = load()

    private fun load() {
        _uiState.value = TeamDetailUiState.Loading
        viewModelScope.launch {
            try {
                val team = teamRepository.team(teamId)
                _uiState.value = if (team != null) {
                    TeamDetailUiState.Success(team)
                } else {
                    TeamDetailUiState.Error("Equipo no encontrado")
                }
            } catch (e: Exception) {
                _uiState.value = TeamDetailUiState.Error(e.message ?: "Error al cargar el equipo")
            }
        }
    }

    companion object {
        fun Factory(teamRepository: TeamRepository, teamId: String) = viewModelFactory {
            initializer { TeamDetailViewModel(teamRepository, teamId) }
        }
    }
}
```

- [ ] **Step 2: `TeamDetailScreen.kt`** — fiel a `09-detalle-equipo.png`:
  - **Top bar** claro: solo botón back (sin título).
  - **Hero oscuro** (`surfaceDark`, redondeado, franja tricolor arriba): `FlagAvatar(iso=team.flag, code=team.code, size=64.dp)` a la izq; a la der columna con `"${team.conf.label} · FIFA #${team.rank}"` (conf en verde `win`/Bold, resto textFaint), nombre `team.name` grande (blanco/900) y chip `team.styleLabel` (fondo translúcido, texto blanco).
  - **ESTRELLA A DESTACAR**: tarjeta blanca **clickable** (`onPlayerClick: (String) -> Unit` con el `teamId`): badge cuadrado `surfaceDark` con `star.num` (blanco/900), `star.name` (Bold), `star.pos` (brand), `star.note` (textSecondary, máx 3 líneas) + chevron `Icons.AutoMirrored.Filled.KeyboardArrowRight` a la der.
  - **FORTALEZAS**: label verde; lista `team.pros` con check verde (`Icons.Outlined.CheckCircle` tint `win`) + texto.
  - **DEBILIDADES**: label; lista `team.cons` con punto/ícono rojo (`tint = live`) + texto.
  - (Opcional, si entra fácil) **RACHA**: fila de chips con `team.form` ('V' verde / 'E' amarillo / 'D' rojo).
  - Params: `viewModel`, `onBack`, `onPlayerClick: (String) -> Unit`. Estados Loading/Error.

- [ ] **Step 3: `@Preview`** con `FakeTeamRepository()` y `"arg"`, `onPlayerClick = {}`. Compilar `:app:compileDebugKotlin` · Expected: SUCCESS.
- [ ] **Step 4: Commit** — `git add app/src/main/java/com/mundial26/app/ui/teamdetail/ && git commit -m "feat(teamdetail): pantalla detalle de equipo + VM"`

---

## Task 5: Detalle de jugador (ViewModel + Screen)  ·  `screens/10`

**Files:**
- Create: `app/src/main/java/com/mundial26/app/ui/playerdetail/PlayerDetailViewModel.kt`
- Create: `app/src/main/java/com/mundial26/app/ui/playerdetail/PlayerDetailScreen.kt`

> El jugador es la `star` de un equipo, así que la ruta lleva el **teamId** y el VM resuelve `team(teamId)`.

- [ ] **Step 1: `PlayerDetailViewModel.kt`**:

```kotlin
package com.mundial26.app.ui.playerdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mundial26.app.domain.model.Star
import com.mundial26.app.domain.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlayerDetailUiState {
    data object Loading : PlayerDetailUiState
    data class Success(
        val star: Star,
        val teamName: String,
        val teamFlag: String
    ) : PlayerDetailUiState
    data class Error(val message: String) : PlayerDetailUiState
}

class PlayerDetailViewModel(
    private val teamRepository: TeamRepository,
    private val teamId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerDetailUiState>(PlayerDetailUiState.Loading)
    val uiState: StateFlow<PlayerDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() = load()

    private fun load() {
        _uiState.value = PlayerDetailUiState.Loading
        viewModelScope.launch {
            try {
                val team = teamRepository.team(teamId)
                _uiState.value = if (team != null) {
                    PlayerDetailUiState.Success(team.star, team.name, team.flag)
                } else {
                    PlayerDetailUiState.Error("Jugador no encontrado")
                }
            } catch (e: Exception) {
                _uiState.value = PlayerDetailUiState.Error(e.message ?: "Error al cargar el jugador")
            }
        }
    }

    companion object {
        fun Factory(teamRepository: TeamRepository, teamId: String) = viewModelFactory {
            initializer { PlayerDetailViewModel(teamRepository, teamId) }
        }
    }
}
```

- [ ] **Step 2: `PlayerDetailScreen.kt`** — fiel a `10-detalle-jugador.png`:
  - **Hero oscuro** (`surfaceDark`, franja tricolor arriba): botón back circular flotante (`surface`) arriba-izq; badge cuadrado grande `brand` con `star.num` (blanco/900); a la der: fila `FlagAvatar(iso=teamFlag, code=…, size=20.dp)` + `teamName`, luego `star.name` grande (blanco/900) y chip `star.pos` (fondo verde translúcido, texto verde claro).
  - **EN EL MUNDIAL**: grid 2×2 de tarjetas blancas, cada una con valor grande (brand/900) y label debajo (textTertiary): `stats.pj` "Partidos", `stats.goals` "Goles", `stats.assists` "Asistencias", `stats.rating` "Valoración" (mostrar el rating con 1 decimal, ej. `"%.1f".format(stats.rating)`).
  - **FICHA**: tarjeta blanca con filas label↔valor: "Club" → `stats.club`, "Edad" → `"${stats.age}"`, "Valor" → `stats.marketValue`. (Omitir filas con valor vacío.)
  - Params: `viewModel`, `onBack`. Estados Loading/Error.

- [ ] **Step 3: `@Preview`** con `FakeTeamRepository()` y `"arg"`. Compilar `:app:compileDebugKotlin` · Expected: SUCCESS.
- [ ] **Step 4: Commit** — `git add app/src/main/java/com/mundial26/app/ui/playerdetail/ && git commit -m "feat(playerdetail): pantalla detalle de jugador + VM"`

---

## Task 6: Cableado de navegación (rutas + clicks + ocultar bottom nav)

**Files:**
- Modify: `app/src/main/java/com/mundial26/app/ui/navigation/Destinations.kt`
- Modify: `app/src/main/java/com/mundial26/app/MundialApp.kt`
- Modify: `app/src/main/java/com/mundial26/app/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/mundial26/app/ui/matches/MatchesScreen.kt`
- Modify: `app/src/main/java/com/mundial26/app/ui/teams/TeamsScreen.kt`
- Modify: `app/src/main/java/com/mundial26/app/ui/news/NewsScreen.kt`

- [ ] **Step 1: Completar `DetailRoutes` + builders** en `Destinations.kt` (reemplazar el `object DetailRoutes`):

```kotlin
/**
 * Rutas de detalle. La parte `{...}` es el nombre del argumento de navegación.
 */
object DetailRoutes {
    const val MATCH_ARG = "id"
    const val TEAM_ARG = "id"
    const val PLAYER_ARG = "teamId"
    const val NEWS_ARG = "id"

    const val MATCH_DETAIL = "matchDetail/{id}"
    const val TEAM_DETAIL = "teamDetail/{id}"
    const val PLAYER_DETAIL = "playerDetail/{teamId}"
    const val NEWS_DETAIL = "newsDetail/{id}"

    fun matchDetail(id: String) = "matchDetail/$id"
    fun teamDetail(id: String) = "teamDetail/$id"
    fun playerDetail(teamId: String) = "playerDetail/$teamId"
    fun newsDetail(id: String) = "newsDetail/$id"

    /** ¿La ruta actual es una pantalla de detalle? (para ocultar la bottom nav) */
    fun isDetail(route: String?): Boolean =
        route != null && (route.startsWith("matchDetail/") ||
            route.startsWith("teamDetail/") ||
            route.startsWith("playerDetail/") ||
            route.startsWith("newsDetail/"))
}
```

- [ ] **Step 2: Agregar params de navegación a las 4 pantallas raíz y cablear los hooks.** En cada una, agregar el/los parámetro(s) y reemplazar los placeholders `{}`/`{ }`:

  - **HomeScreen.kt** — firma: agregar `onMatchClick: (String) -> Unit` y `onNewsClick: (String) -> Unit`. Pasar `onClick = { onMatchClick(match.id) }` a cada `LiveMatchCard` y `MatchRow`; `onClick = { onNewsClick(item.id) }` a cada `NewsCardCompact`. (Revisar los call-sites actuales dentro de `HomeScreen`.)
  - **MatchesScreen.kt** — agregar `onMatchClick: (String) -> Unit`; propagarlo a `MatchesContent` → `MatchesListContent`/`CalendarContent` y pasar `onClick = { onMatchClick(match.id) }` en cada `MatchRow` (líneas 153, 159, 165, 184). (El bracket no navega en v2.)
  - **TeamsScreen.kt** — agregar `onTeamClick: (String) -> Unit`; propagarlo a `TeamsContent` → `TeamsListContent` → `TeamCard(team, onClick = { onTeamClick(team.id) })`. Reemplazar el `.clickable { }` de `TeamCard` (línea 230) por `.clickable { onClick() }`, agregando `onClick: () -> Unit` al `TeamCard`.
  - **NewsScreen.kt** — agregar `onNewsClick: (String) -> Unit`; pasar `onClick = { onNewsClick(item.id) }` al `NewsCardLarge` (línea 113).

- [ ] **Step 3: Registrar rutas + ocultar bottom nav en `MundialApp.kt`.** (a) Envolver el `BottomNav` para que no se muestre en detalle:

```kotlin
val currentRouteStr = currentRoute?.destination?.route
Scaffold(
    bottomBar = {
        if (!DetailRoutes.isDetail(currentRouteStr)) {
            BottomNav(
                currentRoute = currentRouteStr,
                onNavigate = { dest -> navController.navigate(dest.route) {
                    launchSingleTop = true; restoreState = true
                    popUpTo(TopDestination.HOME.route) { saveState = true }
                } }
            )
        }
    }
) { innerPadding -> /* NavHost … */ }
```

  (b) Pasar callbacks a las raíz dentro del `NavHost` (ej. Home):

```kotlin
HomeScreen(
    viewModel = homeViewModel,
    onNavigateToTab = { route -> navController.navigate(route) {
        launchSingleTop = true; restoreState = true
        popUpTo(TopDestination.HOME.route) { saveState = true }
    } },
    onMatchClick = { id -> navController.navigate(DetailRoutes.matchDetail(id)) },
    onNewsClick = { id -> navController.navigate(DetailRoutes.newsDetail(id)) }
)
```

  Análogo para `MatchesScreen(onMatchClick=…)`, `TeamsScreen(onTeamClick = { id -> navController.navigate(DetailRoutes.teamDetail(id)) })`, `NewsScreen(onNewsClick=…)`.

  (c) Registrar las 4 composables de detalle (leyendo el argumento con `backStackEntry.arguments?.getString(...)`):

```kotlin
composable(
    route = DetailRoutes.MATCH_DETAIL,
    arguments = listOf(navArgument(DetailRoutes.MATCH_ARG) { type = NavType.StringType })
) { entry ->
    val id = entry.arguments?.getString(DetailRoutes.MATCH_ARG).orEmpty()
    val vm: MatchDetailViewModel = viewModel(
        factory = MatchDetailViewModel.Factory(container.matchRepository, id)
    )
    MatchDetailScreen(viewModel = vm, onBack = { navController.popBackStack() })
}
composable(
    route = DetailRoutes.NEWS_DETAIL,
    arguments = listOf(navArgument(DetailRoutes.NEWS_ARG) { type = NavType.StringType })
) { entry ->
    val id = entry.arguments?.getString(DetailRoutes.NEWS_ARG).orEmpty()
    val vm: NewsDetailViewModel = viewModel(
        factory = NewsDetailViewModel.Factory(container.newsRepository, id)
    )
    NewsDetailScreen(viewModel = vm, onBack = { navController.popBackStack() })
}
composable(
    route = DetailRoutes.TEAM_DETAIL,
    arguments = listOf(navArgument(DetailRoutes.TEAM_ARG) { type = NavType.StringType })
) { entry ->
    val id = entry.arguments?.getString(DetailRoutes.TEAM_ARG).orEmpty()
    val vm: TeamDetailViewModel = viewModel(
        factory = TeamDetailViewModel.Factory(container.teamRepository, id)
    )
    TeamDetailScreen(
        viewModel = vm,
        onBack = { navController.popBackStack() },
        onPlayerClick = { teamId -> navController.navigate(DetailRoutes.playerDetail(teamId)) }
    )
}
composable(
    route = DetailRoutes.PLAYER_DETAIL,
    arguments = listOf(navArgument(DetailRoutes.PLAYER_ARG) { type = NavType.StringType })
) { entry ->
    val teamId = entry.arguments?.getString(DetailRoutes.PLAYER_ARG).orEmpty()
    val vm: PlayerDetailViewModel = viewModel(
        factory = PlayerDetailViewModel.Factory(container.teamRepository, teamId)
    )
    PlayerDetailScreen(viewModel = vm, onBack = { navController.popBackStack() })
}
```

  Agregar imports: `androidx.navigation.NavType`, `androidx.navigation.navArgument`, y los 8 símbolos de las pantallas/VMs de detalle. La `startDestination` y las 4 composables raíz **no cambian**.

- [ ] **Step 4: Compilar** — `.\gradlew.bat :app:compileDebugKotlin --console=plain` · Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Recorrido en emulador** — `.\gradlew.bat :app:installDebug` (o Run en Android Studio). Verificar todos los caminos:
  - Inicio → tap en tarjeta En vivo → **detalle de partido** (stats + eventos). Back vuelve a Inicio.
  - Inicio → tap en noticia → **detalle de noticia**. Back vuelve.
  - Partidos (las 3 vistas) → tap en fila → **detalle de partido**.
  - Equipos (grid) → tap en selección → **detalle de equipo** → tap en "Estrella a destacar" → **detalle de jugador**. Back encadena bien.
  - Noticias → tap en tarjeta → **detalle de noticia**.
  - En todas las pantallas de detalle la **bottom nav NO aparece**; en las raíz sí.

- [ ] **Step 6: Commit** — `git add -A && git commit -m "feat(nav): navegación a las 4 pantallas de detalle + ocultar bottom nav"`

---

## Task 7: Verificación final de v2

- [ ] **Step 1: Tests** — `.\gradlew.bat :app:testDebugUnitTest --console=plain` · Expected: PASS.
- [ ] **Step 2: Build de sanity** — `.\gradlew.bat :app:assembleDebug --console=plain` · Expected: BUILD SUCCESSFUL.
- [ ] **Step 3: Comparación visual** — abrir `screens/08…11` al lado del emulador; ajustar spacing/colores/tipografía hasta criterio **igual o mejor**.
- [ ] **Step 4: Verificar que v1 no se rompió** — recorrer las 4 raíz + segmentados/filtros; banderas cargan; ninguna pantalla crashea.
- [ ] **Step 5: Commit final** — `git commit -am "chore: v2 detalles completa y verificada"`

---

## Definición de "Hecho" (v2)
- Las 4 pantallas de detalle existen, navegables con argumentos desde las raíz.
- `StarStats` poblado para la estrella destacada; tests de repos/lookups en verde.
- Bottom nav oculta en detalle; back funciona en todos los caminos (incl. equipo→jugador).
- Cada detalle verificado visualmente contra `screens/08…11` (igual o mejor).
- Sin dependencias nuevas; listo para v3 (API-Football) sin refactor de UI.
```