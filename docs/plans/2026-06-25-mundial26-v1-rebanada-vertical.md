# Mundial 26 — v1 (Rebanada Vertical) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** App Android que compila y corre con las 4 pantallas principales (Inicio, Partidos, Equipos, Noticias), navegación, theming Material 3 fiel al prototipo y datos mock detrás de interfaces `Repository`.

**Architecture:** Una sola Activity, 100% Jetpack Compose, MVVM + Repository. UI depende solo de `domain/` (modelos + interfaces). `AppContainer` (DI manual) inyecta repos **mock** en v1; en fases siguientes se cambian por Retrofit sin tocar la UI.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3, Navigation-Compose, ViewModel + StateFlow, Coil, Gradle (Kotlin DSL) + version catalog. (Retrofit/kotlinx-serialization se agregan en v3.)

> **Nota de versiones:** los números abajo son estables a ene-2026. Al ejecutar, abrir el proyecto en Android Studio y aceptar cualquier sugerencia de actualización del AGP/Kotlin/BOM si aparece. Verificar contra doc oficial vía Context7 si algo no resuelve.

> **Referencia visual:** cada pantalla se compara contra su captura en
> `C:\Users\emiso\OneDrive\Desktop\design_handoff_mundial2026\screens\` (`01-inicio.png` … `07-noticias.png`).
> Spec completa: `docs/specs/2026-06-25-mundial26-android-design.md`.

---

## Estructura de archivos (v1)

```
mundial26-android/
├── settings.gradle.kts
├── build.gradle.kts                      (root)
├── gradle/libs.versions.toml             (version catalog)
├── gradle.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/
        │   ├── font/                      (Archivo: archivo_medium.ttf … archivo_black.ttf)
        │   └── values/{strings,themes}.xml
        └── java/com/mundial26/app/
            ├── MainActivity.kt
            ├── MundialApp.kt              (Scaffold + NavHost + BottomNav host)
            ├── di/AppContainer.kt
            ├── domain/
            │   ├── model/Models.kt        (Team, Match, Group, BracketGame, NewsItem, …)
            │   └── repository/Repositories.kt
            ├── data/mock/
            │   ├── MockData.kt            (datos del prototipo)
            │   └── FakeRepositories.kt
            ├── ui/theme/
            │   ├── Color.kt  Type.kt  Shape.kt  Theme.kt
            ├── ui/components/
            │   ├── FlagAvatar.kt  BrandLogo26.kt  SegmentedControl.kt
            │   ├── LivePulseDot.kt  SectionHeader.kt  MatchRow.kt
            │   ├── LiveMatchCard.kt  StandingsTable.kt  NewsCard.kt
            ├── ui/navigation/
            │   ├── Destinations.kt  BottomNav.kt
            ├── ui/home/    HomeScreen.kt  HomeViewModel.kt
            ├── ui/matches/ MatchesScreen.kt  MatchesViewModel.kt
            ├── ui/teams/   TeamsScreen.kt  TeamsViewModel.kt
            └── ui/news/     NewsScreen.kt  NewsViewModel.kt
```

---

## Task 0: Scaffold del proyecto Gradle que compila

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/java/com/mundial26/app/MainActivity.kt`

- [ ] **Step 1: `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
coreKtx = "1.15.0"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2024.12.01"
navigation = "2.8.5"
coil = "2.7.0"
junit = "4.13.2"
coroutinesTest = "1.9.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

- [ ] **Step 2: `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google { content { includeGroupByRegex("com\\.android.*"); includeGroupByRegex("androidx.*"); includeGroupByRegex("com\\.google.*") } }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Mundial26"
include(":app")
```

- [ ] **Step 3: root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

- [ ] **Step 4: `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 5: `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mundial26.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mundial26.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 6: `app/proguard-rules.pro`** (vacío con comentario)

```pro
# Reglas ProGuard (vacío en v1; minify desactivado).
```

- [ ] **Step 7: `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Mundial26">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Mundial26">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 8: `res/values/strings.xml` y `res/values/themes.xml`**

```xml
<!-- strings.xml -->
<resources><string name="app_name">Mundial 26</string></resources>
```

```xml
<!-- themes.xml -->
<resources>
    <style name="Theme.Mundial26" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 9: `MainActivity.kt` mínima (pantalla vacía)**

```kotlin
package com.mundial26.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { Text("Mundial 26") } }
    }
}
```

- [ ] **Step 10: Verificar compilación**

Run: `./gradlew :app:assembleDebug` (en `C:\Users\emiso\mundial26-android`; usar `gradlew.bat` o el wrapper de Android Studio)
Expected: `BUILD SUCCESSFUL`. (Si no existe el wrapper, generarlo con `gradle wrapper` o abrir el proyecto en Android Studio para que lo cree.)

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "chore: scaffold proyecto Gradle/Compose que compila"
```

---

## Task 1: Modelos de dominio

**Files:**
- Create: `app/src/main/java/com/mundial26/app/domain/model/Models.kt`

- [ ] **Step 1: Escribir los data classes** (del README, sección "Modelo de datos")

```kotlin
package com.mundial26.app.domain.model

enum class MatchStatus { LIVE, UPCOMING, FINISHED }
enum class Confederation(val id: String, val label: String) {
    UEFA("uefa", "UEFA"), CONMEBOL("conmebol", "CONMEBOL"),
    CONCACAF("concacaf", "CONCACAF"), OTHER("other", "Otra")
}

data class Star(val num: Int, val name: String, val pos: String, val note: String)

data class Team(
    val id: String, val name: String, val code: String,
    val conf: Confederation, val rank: Int, val tag: String,
    val flag: String,            // código ISO-2 para flagcdn (ej. "ar")
    val styleLabel: String,
    val star: Star,
    val pros: List<String>, val cons: List<String>,
    val form: List<Char>         // 'V' | 'E' | 'D'
)

data class MatchStat(val label: String, val a: Int, val b: Int)
enum class EventKind { GOAL, YELLOW, RED, SUB }
data class MatchEvent(val min: Int, val kind: EventKind, val side: Char, val player: String) // side 'A'|'B'

data class Match(
    val id: String, val status: MatchStatus, val minute: Int?, val time: String?,
    val day: String?, val group: String, val stadium: String,
    val teamA: String, val teamB: String,      // códigos (ARG, MEX…)
    val flagA: String, val flagB: String,      // ISO-2
    val scoreA: Int?, val scoreB: Int?,
    val stats: List<MatchStat> = emptyList(),
    val events: List<MatchEvent> = emptyList()
)

data class GroupRow(val code: String, val flag: String, val pj: Int, val g: Int, val e: Int, val p: Int, val gf: Int, val gc: Int, val pts: Int) {
    val dg: Int get() = gf - gc
}
data class Group(val name: String, val rows: List<GroupRow>)

data class BracketMatch(val a: String, val b: String, val flagA: String, val flagB: String, val sa: Int?, val sb: Int?, val done: Boolean)
data class BracketRound(val round: String, val games: List<BracketMatch>)

data class NewsItem(val id: String, val tag: String, val colorHex: Long, val time: String, val title: String, val summary: String, val body: List<String>)
```

- [ ] **Step 2: Compilar** — Run: `./gradlew :app:compileDebugKotlin` · Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 3: Commit** — `git add -A && git commit -m "feat(domain): modelos de dominio"`

---

## Task 2: Interfaces Repository

**Files:**
- Create: `app/src/main/java/com/mundial26/app/domain/repository/Repositories.kt`

- [ ] **Step 1: Definir las interfaces**

```kotlin
package com.mundial26.app.domain.repository

import com.mundial26.app.domain.model.*

interface MatchRepository {
    fun liveMatches(): List<Match>
    fun resultsAndUpcoming(): List<Match>
    fun byDay(): Map<String, List<Match>>      // calendario
    fun bracket(): List<BracketRound>
    fun match(id: String): Match?
}

interface TeamRepository {
    fun teams(): List<Team>
    fun featured(): Team
    fun groups(): List<Group>
    fun team(id: String): Team?
}

interface NewsRepository {
    fun news(): List<NewsItem>
    fun item(id: String): NewsItem?
}
```

- [ ] **Step 2: Compilar** — Run: `./gradlew :app:compileDebugKotlin` · Expected: SUCCESS.
- [ ] **Step 3: Commit** — `git commit -am "feat(domain): interfaces Repository"`

---

## Task 3: Datos mock + Fake repositories (TDD)

**Files:**
- Create: `app/src/main/java/com/mundial26/app/data/mock/MockData.kt`, `.../data/mock/FakeRepositories.kt`
- Test: `app/src/test/java/com/mundial26/app/data/mock/FakeRepositoriesTest.kt`

> Los datos salen del `<script type="text/x-dc">` de `Mundial 2026.dc.html` (selecciones, partidos, grupos, bracket, noticias). Portar valores reales del prototipo.

- [ ] **Step 1: Test que falla**

```kotlin
package com.mundial26.app.data.mock
import com.mundial26.app.domain.model.MatchStatus
import org.junit.Assert.*
import org.junit.Test

class FakeRepositoriesTest {
    private val matches = FakeMatchRepository()
    private val teams = FakeTeamRepository()
    private val news = FakeNewsRepository()

    @Test fun hay_partidos_en_vivo() {
        assertTrue(matches.liveMatches().isNotEmpty())
        assertTrue(matches.liveMatches().all { it.status == MatchStatus.LIVE })
    }
    @Test fun hay_cuatro_grupos() = assertEquals(4, teams.groups().size)
    @Test fun equipo_destacado_existe() = assertNotNull(teams.featured())
    @Test fun hay_noticias() = assertTrue(news.news().isNotEmpty())
    @Test fun lookup_por_id_funciona() {
        val first = teams.teams().first()
        assertEquals(first, teams.team(first.id))
    }
}
```

- [ ] **Step 2: Correr el test, verificar que falla** — Run: `./gradlew :app:testDebugUnitTest` · Expected: FAIL (clases no existen).

- [ ] **Step 3: Implementar `MockData.kt`** — objeto con listas (`teams`, `liveMatches`, `results`, `calendarByDay`, `bracket`, `groups`, `news`) portando los datos del prototipo. Mínimo: 8+ equipos, 2 partidos live, grupos A–D completos, 4 rondas de bracket, 5 noticias. Usar códigos ISO-2 reales para `flag` (ar, br, fr, mx, es, gb-eng, pt, de, nl, uy, jp, sn, hr, ca, ma, us).

- [ ] **Step 4: Implementar `FakeRepositories.kt`**

```kotlin
package com.mundial26.app.data.mock
import com.mundial26.app.domain.model.*
import com.mundial26.app.domain.repository.*

class FakeMatchRepository : MatchRepository {
    override fun liveMatches() = MockData.matches.filter { it.status == MatchStatus.LIVE }
    override fun resultsAndUpcoming() = MockData.matches.filter { it.status != MatchStatus.LIVE }
    override fun byDay() = MockData.matches.filter { it.day != null }.groupBy { it.day!! }
    override fun bracket() = MockData.bracket
    override fun match(id: String) = MockData.matches.find { it.id == id }
}
class FakeTeamRepository : TeamRepository {
    override fun teams() = MockData.teams
    override fun featured() = MockData.teams.first { it.id == MockData.featuredTeamId }
    override fun groups() = MockData.groups
    override fun team(id: String) = MockData.teams.find { it.id == id }
}
class FakeNewsRepository : NewsRepository {
    override fun news() = MockData.news
    override fun item(id: String) = MockData.news.find { it.id == id }
}
```

- [ ] **Step 5: Correr el test, verificar PASS** — Run: `./gradlew :app:testDebugUnitTest` · Expected: PASS.
- [ ] **Step 6: Commit** — `git commit -am "feat(data): datos mock + fake repos (con tests)"`

---

## Task 4: AppContainer (DI manual)

**Files:**
- Create: `app/src/main/java/com/mundial26/app/di/AppContainer.kt`

- [ ] **Step 1: Implementar el contenedor**

```kotlin
package com.mundial26.app.di
import com.mundial26.app.data.mock.*
import com.mundial26.app.domain.repository.*

interface AppContainer {
    val matchRepository: MatchRepository
    val teamRepository: TeamRepository
    val newsRepository: NewsRepository
}

class DefaultAppContainer : AppContainer {
    // v1: implementaciones mock. En v3 se sustituyen por las de data/remote.
    override val matchRepository: MatchRepository by lazy { FakeMatchRepository() }
    override val teamRepository: TeamRepository by lazy { FakeTeamRepository() }
    override val newsRepository: NewsRepository by lazy { FakeNewsRepository() }
}
```

- [ ] **Step 2: Application class + registro** — Create `app/.../MundialApplication.kt`:

```kotlin
package com.mundial26.app
import android.app.Application
import com.mundial26.app.di.AppContainer
import com.mundial26.app.di.DefaultAppContainer

class MundialApplication : Application() {
    lateinit var container: AppContainer
    override fun onCreate() { super.onCreate(); container = DefaultAppContainer() }
}
```

Modificar `AndroidManifest.xml`: agregar `android:name=".MundialApplication"` al `<application>`.

- [ ] **Step 3: Compilar** — `./gradlew :app:compileDebugKotlin` · Expected: SUCCESS.
- [ ] **Step 4: Commit** — `git commit -am "feat(di): AppContainer manual + Application"`

---

## Task 5: Theme Material 3 fiel (Color / Type / Shape / Theme)

**Files:**
- Create: `ui/theme/Color.kt`, `ui/theme/Type.kt`, `ui/theme/Shape.kt`, `ui/theme/Theme.kt`
- Assets: `res/font/archivo_medium.ttf`, `archivo_semibold.ttf`, `archivo_bold.ttf`, `archivo_extrabold.ttf`, `archivo_black.ttf` (descargar de Google Fonts: Archivo, pesos 500/600/700/800/900).

- [ ] **Step 1: `Color.kt`** — todos los tokens del README como `val ... = Color(0xFF......)` y un `data class MundialColors(...)` + `val LocalMundialColors = staticCompositionLocalOf { lightMundialColors }`.

```kotlin
package com.mundial26.app.ui.theme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Brand = Color(0xFF2A398D)
val Live = Color(0xFFE61D25)
val Win = Color(0xFF3CAC3B)
val Yellow = Color(0xFFE8B500)
val TextPrimary = Color(0xFF1C1E1E)
val TextSecondary = Color(0xFF474A4A)
val TextTertiary = Color(0xFF6B6F6F)
val TextFaint = Color(0xFF9A9E9E)
val AppBg = Color(0xFFF2F3F3)
val Surface = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF2A2C2C)
val BorderLight = Color(0xFFEAEBEB)
val SegmentInactive = Color(0xFFEEF0F4)
val GreenAccentLight = Color(0xFF9CE8A0)

data class MundialColors(
    val brand: Color = Brand, val live: Color = Live, val win: Color = Win, val yellow: Color = Yellow,
    val textPrimary: Color = TextPrimary, val textSecondary: Color = TextSecondary,
    val textTertiary: Color = TextTertiary, val textFaint: Color = TextFaint,
    val appBg: Color = AppBg, val surface: Color = Surface, val surfaceDark: Color = SurfaceDark,
    val border: Color = BorderLight, val segmentInactive: Color = SegmentInactive
)
val LocalMundialColors = staticCompositionLocalOf { MundialColors() }
```

- [ ] **Step 2: `Type.kt`** — `FontFamily` Archivo + `Typography` con los tamaños/pesos/letter-spacing del README (título 24sp/900/-0.6, sección 15sp/900, label 13sp/900/0.3 uppercase, cuerpo 14–15sp, marcador 46sp/900/-2, etc.).

- [ ] **Step 3: `Shape.kt`** — `Shapes(small=RoundedCornerShape(12.dp), medium=16.dp, large=20.dp)` + constantes pill/círculo.

- [ ] **Step 4: `Theme.kt`** — `MundialTheme(content)` que arma un `lightColorScheme(primary=Brand, background=AppBg, surface=Surface, …)`, provee `LocalMundialColors`, `Typography`, `Shapes`, y expone un objeto `MundialTheme { val colors @Composable get() = LocalMundialColors.current }`.

- [ ] **Step 5: Usar el theme en MainActivity** — envolver el contenido en `MundialTheme { ... }`.
- [ ] **Step 6: Preview de sanity** — `@Preview` mostrando un `Text` con `MaterialTheme.typography.titleLarge` y color brand. Compilar: `./gradlew :app:compileDebugKotlin` · Expected: SUCCESS.
- [ ] **Step 7: Commit** — `git commit -am "feat(theme): Material 3 + tokens + fuente Archivo"`

---

## Task 6: Componentes reutilizables

**Files:**
- Create: `ui/components/FlagAvatar.kt`, `BrandLogo26.kt`, `SegmentedControl.kt`, `LivePulseDot.kt`, `SectionHeader.kt`, `MatchRow.kt`, `LiveMatchCard.kt`, `StandingsTable.kt`, `NewsCard.kt`

- [ ] **Step 1: `FlagAvatar.kt`** — `AsyncImage` (Coil) circular desde `https://flagcdn.com/w160/{iso}.png`, con placeholder: círculo `border`/gris y el `code` en 9–11sp/900 centrado. Param: `iso: String`, `code: String`, `size: Dp = 36.dp`.

- [ ] **Step 2: `LivePulseDot.kt`** — punto rojo con animación de pulso (`rememberInfiniteTransition`, scale/alpha 1.2s infinite) — el único motion conservado del prototipo.

- [ ] **Step 3: `BrandLogo26.kt`** — `Box` redondeado `surfaceDark` con "26" (italic, 900, blanco) y franja tricolor inferior (3 segmentos iguales verde/rojo/azul).

- [ ] **Step 4: `SegmentedControl.kt`** — contenedor `segmentInactive` radio 13dp; N segmentos; activo fondo `brand`, texto blanco, radio 10dp. Param: `options: List<String>`, `selected: Int`, `onSelect: (Int) -> Unit`.

- [ ] **Step 5: `SectionHeader.kt`** — fila título de sección (15sp/900) + link opcional ("Ver todo"/"Ver tabla") a la derecha con `onClick`.

- [ ] **Step 6: `MatchRow.kt`** — fila compacta: fecha/estado coloreado, banderas+códigos, marcador o "vs". Para live: borde izquierdo rojo 4px + minuto.

- [ ] **Step 7: `LiveMatchCard.kt`** — tarjeta carrusel (ancho 268dp): grupo, badge `min'` con `LivePulseDot`, banderas+códigos, marcador 30sp/900 centrado, estadio.

- [ ] **Step 8: `StandingsTable.kt`** — tabla de grupo: cabecera (#, EQUIPO, PJ, G, E, P, DG, PTS), filas con marcador de posición (barra verde top-2, gris resto), bandera, nombre, stats. Param: `group: Group`, `compact: Boolean = false` (modo compacto Home muestra solo #, EQUIPO, PJ, DG, PTS).

- [ ] **Step 9: `NewsCard.kt`** — tarjeta con banda de tinte por categoría (`colorHex`), badge de tag, título, resumen/hora, `onClick`.

- [ ] **Step 10: Previews** — cada componente con un `@Preview` usando datos de `MockData`. Compilar: `./gradlew :app:compileDebugKotlin` · Expected: SUCCESS.
- [ ] **Step 11: Commit** — `git commit -am "feat(ui): componentes reutilizables fieles al prototipo"`

---

## Task 7: Navegación + Bottom nav

**Files:**
- Create: `ui/navigation/Destinations.kt`, `ui/navigation/BottomNav.kt`, `MundialApp.kt`
- Modify: `MainActivity.kt`

- [ ] **Step 1: `Destinations.kt`** — sealed/enum de rutas raíz (home, matches, teams, news) con `route`, `label`, `icon` (Material icons line: Home, SportsSoccer, Shield, Article). Rutas de detalle como constantes con `{id}` (para fases siguientes).

- [ ] **Step 2: `BottomNav.kt`** — `NavigationBar` M3: activo `brand`/peso 800, inactivo `#A2A6A6`/600. 4 destinos.

- [ ] **Step 3: `MundialApp.kt`** — `Scaffold(bottomBar = { BottomNav(...) }) { NavHost(startDestination = home) { composable(home){HomeScreen(...)}; matches; teams; news } }`. Recibe `AppContainer`.

- [ ] **Step 4: `MainActivity.kt`** — obtener `container` de `MundialApplication`, `setContent { MundialTheme { MundialApp(container) } }`.

- [ ] **Step 5: Correr la app** — instalar en emulador: `./gradlew :app:installDebug` o Run en Android Studio. Verificar que la bottom nav cambia entre 4 pantallas (placeholders por ahora). 
- [ ] **Step 6: Commit** — `git commit -am "feat(nav): scaffold de navegación + bottom nav"`

---

## Task 8: Pantalla Inicio (Home) + ViewModel

**Files:**
- Create: `ui/home/HomeViewModel.kt`, `ui/home/HomeScreen.kt`
- Reference: `screens/01-inicio.png`

- [ ] **Step 1: `HomeViewModel.kt`** — expone `StateFlow<HomeUiState>` (Loading/Success(live, results, featured, groupB, news)/Error). En v1 carga sincrónico desde repos dentro de `viewModelScope`; estructura lista para suspend en v3. Factory que recibe los 3 repos del container.

- [ ] **Step 2: `HomeScreen.kt`** — `LazyColumn` con: cabecera (BrandLogo26 + "Mundial 26 / USA · MÉXICO · CANADÁ" + campana con punto rojo); sección "En vivo" (`LazyRow` de `LiveMatchCard` + link "Ver todo"); "Resultados & próximos" (`MatchRow`); "Equipo destacado" (tarjeta oscura); "Posiciones · Grupo B" (`StandingsTable` compact); "Noticias" (4 `NewsCard`). Links llaman `onNavigate(tab)`.

- [ ] **Step 3: Wire en `MundialApp`** — `composable(home) { HomeScreen(viewModel(factory=...), onNavigate=...) }`.
- [ ] **Step 4: Verificación visual** — correr en emulador, comparar contra `01-inicio.png`. Ajustar spacing/colores hasta que coincida (criterio: igual o mejor).
- [ ] **Step 5: Commit** — `git commit -am "feat(home): pantalla Inicio + VM"`

---

## Task 9: Pantalla Partidos + ViewModel

**Files:**
- Create: `ui/matches/MatchesViewModel.kt`, `ui/matches/MatchesScreen.kt`
- Reference: `screens/02-partidos.png`, `03-calendario.png`, `04-llaves.png`

- [ ] **Step 1: `MatchesViewModel.kt`** — estado: `matchesView: StateFlow<MatchesView>` (LIST/CALENDAR/BRACKET) + datos (live/upcoming/finished, byDay, bracket). `selectView(v)`.

- [ ] **Step 2: `MatchesScreen.kt`** — título "Partidos" + `SegmentedControl(["Partidos","Calendario","Llaves"])`. Según selección:
  - LIST: secciones En vivo (cards borde rojo) / Próximos / Finalizados (`MatchRow`).
  - CALENDAR: agrupado por día (Hoy, Mañana, …) con fecha.
  - BRACKET: rondas verticales (Octavos→Final); cruce = tarjeta 2 filas; ganador 900/oscuro, pendiente gris + "—".

- [ ] **Step 3: Wire + verificación visual** contra las 3 capturas. 
- [ ] **Step 4: Commit** — `git commit -am "feat(matches): Partidos/Calendario/Llaves + VM"`

---

## Task 10: Pantalla Equipos + ViewModel

**Files:**
- Create: `ui/teams/TeamsViewModel.kt`, `ui/teams/TeamsScreen.kt`
- Reference: `screens/05-selecciones.png`, `06-grupos.png`

- [ ] **Step 1: `TeamsViewModel.kt`** — estado: `teamsView` (LIST/STANDINGS) + `conf` (ALL/UEFA/CONMEBOL/CONCACAF). `teamsFiltered` deriva de `conf`. `selectView`, `selectConf`.

- [ ] **Step 2: `TeamsScreen.kt`** — título "Selecciones" + `SegmentedControl(["Selecciones","Grupos"])`.
  - LIST: chips pill de confederación (Todos/UEFA/CONMEBOL/CONCACAF); grid 2 columnas (`LazyVerticalGrid`) de tarjetas de equipo (bandera, ranking #N, nombre, conf, chip estilo).
  - STANDINGS: 4 `StandingsTable` (A–D) + leyenda verde "Clasifican a la siguiente fase".

- [ ] **Step 3: Wire + verificación visual** contra `05`/`06`. 
- [ ] **Step 4: Commit** — `git commit -am "feat(teams): Selecciones/Grupos + filtros + VM"`

---

## Task 11: Pantalla Noticias + ViewModel

**Files:**
- Create: `ui/news/NewsViewModel.kt`, `ui/news/NewsScreen.kt`
- Reference: `screens/07-noticias.png`

- [ ] **Step 1: `NewsViewModel.kt`** — `StateFlow` con `List<NewsItem>` desde `NewsRepository`.
- [ ] **Step 2: `NewsScreen.kt`** — título "Noticias" + `LazyColumn` de `NewsCard` grandes (banda de tinte, badge, título, resumen, hora).
- [ ] **Step 3: Wire + verificación visual** contra `07-noticias.png`.
- [ ] **Step 4: Commit** — `git commit -am "feat(news): pantalla Noticias + VM"`

---

## Task 12: Verificación final del v1

- [ ] **Step 1: Tests** — Run: `./gradlew :app:testDebugUnitTest` · Expected: PASS.
- [ ] **Step 2: Build release de sanity** — Run: `./gradlew :app:assembleDebug` · Expected: BUILD SUCCESSFUL.
- [ ] **Step 3: Recorrido manual en emulador** — navegar las 4 pestañas + los 3 segmentados (matches) + 2 segmentados/filtros (teams). Confirmar que ninguna pantalla crashea y que las banderas cargan (con placeholder si no hay red).
- [ ] **Step 4: Comparación visual** — abrir `screens/01..07` al lado del emulador; anotar diferencias y ajustar. Criterio de cierre: **igual o mejor** que cada captura.
- [ ] **Step 5: Commit final** — `git commit -am "chore: v1 rebanada vertical completa y verificada"`

---

## Definición de "Hecho" (v1)
- App compila e instala; 4 pantallas navegables con bottom nav.
- Theming M3 fiel (tokens, fuente Archivo, componentes).
- Datos mock detrás de `Repository`; tests de repos en verde.
- Cada pantalla verificada visualmente contra su captura.
- Listo para v2 (detalles) y v3 (API-Football) sin refactor de UI.
```
