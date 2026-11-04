# Guía de estudio Android con Kotlin

Arquitectura y tecnologías de Mundial 26

Un recorrido práctico desde la interfaz Compose hasta la red, Room y las notificaciones, explicado con ejemplos del código del proyecto.

> **Propósito:** Entender qué tecnologías utiliza la app, qué responsabilidad cumple cada una y cómo colaboran en un flujo real.

*Material para desarrolladores Kotlin/Android. Los fragmentos están basados en el repositorio y las explicaciones indican cuándo se muestra solo una parte del código.*

## Índice de temas

- [01  Mapa del proyecto](#1-mapa-del-proyecto)

- [02  Kotlin, Gradle y dependencias](#2-kotlin-gradle-y-dependencias)

- [03  Jetpack Compose y UI declarativa](#3-jetpack-compose-y-ui-declarativa)

- [04  MVVM y estado de pantalla](#4-mvvm-y-estado-de-pantalla)

- [05  Inyección manual de dependencias (DI)](#5-inyección-manual-de-dependencias-di)

- [06  Repository: contrato y fuente de verdad de la UI](#6-repository-contrato-y-fuente-de-verdad-de-la-ui)

- [07  Room y persistencia local](#7-room-y-persistencia-local)

- [08  Sincronización, caché y resiliencia](#8-sincronización-caché-y-resiliencia)

- [09  Coroutines y concurrencia](#9-coroutines-y-concurrencia)

- [10  Retrofit, OkHttp, serialización y APIs](#10-retrofit-okhttp-serialización-y-apis)

- [11  AlarmManager, BroadcastReceiver y notificaciones](#11-alarmmanager-broadcastreceiver-y-notificaciones)

- [12  Foreground Service para goles en vivo](#12-foreground-service-para-goles-en-vivo)

- [13  Pruebas JUnit y doubles](#13-pruebas-junit-y-doubles)

- [14  Fuentes de datos y modelos: qué es real y qué es estimado](#14-fuentes-de-datos-y-modelos-qué-es-real-y-qué-es-estimado)

- [15  Otras piezas relevantes](#15-otras-piezas-relevantes)

- [16  Flujo completo de ejemplo: cargar la pantalla de partidos](#16-flujo-completo-de-ejemplo-cargar-la-pantalla-de-partidos)

- [17  Resumen para entrevista](#17-resumen-para-entrevista)

- [18  Archivos para seguir leyendo](#18-archivos-para-seguir-leyendo)

- [19  Un partido desde la API hasta la pantalla](#19-un-partido-desde-la-api-hasta-la-pantalla)

- [20  Ejercicios de comprensión](#20-ejercicios-de-comprensión)

- [21  Glosario corto](#21-glosario-corto)

- [22  Mini guía de lectura del código](#22-mini-guía-de-lectura-del-código)

*Cómo estudiar: lee la explicación, recorre el archivo enlazado desde el proyecto, reconstruye el flujo y responde las preguntas de repaso sin mirar la solución.*

Esta guía describe el código actual del repositorio y usa sus nombres reales. Es útil como recorrido de arquitectura y como material de entrevista. Las referencias de archivos apuntan al código fuente de cada ejemplo.

## Cómo usar este material

Recórrelo como un laboratorio: abre el enlace del archivo, lee el fragmento, predice el resultado y luego despliega la explicación. Los bloques details se pueden abrir/cerrar en visores Markdown compatibles. Los ejercicios no modifican el proyecto.

Ruta sugerida: primero sigue “Un partido desde la API hasta la pantalla”; después estudia cada tecnología; termina con el repaso y las preguntas. Si ya conoces Kotlin, salta a arquitectura y fuentes de datos.

### Mapa de lectura

- Vista general y tecnologías
- Compose y navegación
- MVVM y Repository
- DI manual
- Room, sync y caché
- Coroutines
- Retrofit y OkHttp
- Alarmas y servicio foreground
- JUnit
- Recorrido guiado de punta a punta
- Ejercicios autocorregibles
## 1. Mapa del proyecto

La app es un único módulo Android (app) construido con Kotlin. Su flujo principal es:

```text
Compose Screen → ViewModel → interfaz Repository → DefaultRepository
                                             ├─ Room DAO / SQLite
                                             ├─ SyncManager → Retrofit/OkHttp → football-data.org
                                             ├─ OkHttp directo → FIFA, RSS, YouTube, etc.
                                             └─ datos editoriales / mocks como fallback
```

- ui/: pantallas, componentes Compose, ViewModels, tema y navegación.
- domain/model/: modelos que consume la app (Match, Team, Player, NewsItem…), además de interfaces de repositorio en domain/repository/.
- data/local/db/: Room: entidades, DAOs, base MundialDatabase y mapeos entre filas y modelos de dominio.
- data/remote/: DTOs, clientes y adaptadores a servicios externos.
- data/repository/: implementaciones que coordinan caché, red, DB y fallback.
- data/sync/: semilla inicial, sincronización y TTL de seis horas.
- di/: contenedor manual de dependencias.
- notifications/: alarmas, notificaciones y seguimiento de goles en servicio foreground.
## 2. Kotlin, Gradle y dependencias

La configuración central está en [app/build.gradle.kts](../app/build.gradle.kts) y las versiones en [gradle/libs.versions.toml](../gradle/libs.versions.toml). Usa Kotlin 2.1.0, Android Gradle Plugin 8.7.3, SDK de compilación/target 35 y minSdk = 26 (Android 8). El bytecode JVM apunta a 11. Gradle Kotlin DSL expresa la configuración con Kotlin.

El catálogo de versiones permite declarar alias (libs.retrofit, libs.androidx.room.runtime) en vez de repetir coordenadas y versiones. KSP ejecuta el compilador de Room. También se activan compose, buildConfig y el plugin de Kotlin Serialization.

Kotlin visible en el código: clases data class, interfaces, nullabilidad explícita, funciones de extensión (por ejemplo toDomain()), sealed interface para estados, parámetros con valor por defecto, lambdas, object singleton y funciones suspend.

## 3. Jetpack Compose y UI declarativa

Las pantallas son funciones @Composable: describen la UI a partir del estado actual. Cuando el estado observado cambia, Compose recompone las partes que lo leen. La navegación está centralizada en [MundialApp.kt](../app/src/main/java/com/mundial26/app/MundialApp.kt) con Navigation Compose y un NavHost; la barra inferior se oculta en rutas de detalle. [Theme.kt](../app/src/main/java/com/mundial26/app/ui/theme/Theme.kt) define MaterialTheme, colores oscuros, tipografía y formas; ui/components/ contiene piezas reutilizables.

Ejemplo representativo de consumo del estado (en pantallas se usa collectAsStateWithLifecycle):

```kotlin
@Composable
fun TeamDetailScreen(viewModel: TeamDetailViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // La pantalla dibuja Loading / Success / Error según state.
}
```

La fuente está en [TeamDetailScreen.kt](../app/src/main/java/com/mundial26/app/ui/teamdetail/TeamDetailScreen.kt). collectAsStateWithLifecycle adapta un Flow al estado Compose y evita recolectar innecesariamente cuando la pantalla no está en un estado activo del ciclo de vida. rememberNavController() conserva el controlador durante recomposiciones. Las rutas transportan IDs, y el ViewModel usa esos IDs para pedir el modelo al repositorio.

Compose UI relevante: LazyColumn en listas largas, componentes propios de tarjetas/filas, Material 3, iconos extendidos, Coil para imágenes, animaciones Compose para elementos como pulsos/transiciones, Scaffold, Modifier para layout y LaunchedEffect para efectos ligados a la composición. En MundialApp, LaunchedEffect(Unit) crea canales y reprograma alarmas al arrancar; no conviene usar un efecto como sustituto de lógica de negocio.

## 4. MVVM y estado de pantalla

Un ViewModel representa el estado y las acciones de una pantalla. Por ejemplo, [HomeViewModel.kt](../app/src/main/java/com/mundial26/app/ui/home/HomeViewModel.kt) depende de interfaces MatchRepository, TeamRepository y NewsRepository, carga sus datos y publica HomeUiState.

```kotlin
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val live: List<Match>, /* otros datos */ val news: List<NewsItem>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

private fun load() {
    _uiState.value = HomeUiState.Loading
    viewModelScope.launch { loadData() }
}
```

El MutableStateFlow queda privado y solo se expone como StateFlow: la UI puede observar, pero no modificar el estado interno. Los estados sellados hacen explícito qué ramas debe representar la pantalla. viewModelScope cancela sus coroutines cuando el ViewModel se destruye. También hay estados separados para isRefreshing y selección de vista.

Los ViewModels tienen Factory con viewModelFactory { initializer { ... } } porque no se usa Hilt. En las rutas de [MundialApp.kt](../app/src/main/java/com/mundial26/app/MundialApp.kt) se construye cada factory inyectando el repositorio correcto.

## 5. Inyección manual de dependencias (DI)

La DI conecta dependencias desde el borde de la app hacia las pantallas. MundialApplication.kt crea una instancia de DefaultAppContainer; AppContainer.kt declara lo que se puede pedir y construye DB, APIs y repositorios.

```kotlin
interface AppContainer {
    val matchRepository: MatchRepository
    val teamRepository: TeamRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext
    private val db = MundialDatabase.build(appContext)
    private val fdApi = footballDataRetrofit().create(FootballDataApi::class.java)
    private val ofApi = openFootballRetrofit().create(OpenFootballApi::class.java)
    private val sync = SyncManager(
        fdApi, db.teamDao(), db.playerDao(), db.standingDao(), db.matchDao(), db.syncMetaDao()
    )
    override val matchRepository by lazy {
        DefaultMatchRepository(sync, db.matchDao(), ofApi)
    }
}
```

El ejemplo está reducido a los objetos de partidos/equipos para facilitar la lectura; el contenedor real también proporciona el repositorio de noticias y la fuente compartida de highlights.

La app pasa container.matchRepository al Factory del ViewModel. Es una forma sencilla de controlar el grafo de objetos sin framework; by lazy posterga la creación del repositorio hasta que se use. applicationContext evita retener una Activity accidentalmente. A diferencia de Hilt, no hay generación automática de factories ni scopes declarativos: el contenedor mantiene esas responsabilidades explícitas.

## 6. Repository: contrato y fuente de verdad de la UI

Las interfaces en [Repositories.kt](../app/src/main/java/com/mundial26/app/domain/repository/Repositories.kt) forman el contrato consumido por ViewModels. Las implementaciones, por ejemplo [DefaultMatchRepository.kt](../app/src/main/java/com/mundial26/app/data/repository/DefaultMatchRepository.kt), esconden detalles de persistencia y fuentes externas.

```kotlin
interface MatchRepository {
    suspend fun liveMatches(): List<Match>
    suspend fun resultsAndUpcoming(): List<Match>
    suspend fun match(id: String): Match?
    suspend fun refresh() {}
}

override suspend fun liveMatches() = allWithLive().filter { it.status == MatchStatus.LIVE }
```

La implementación hace sync.ensureFresh(), lee Room, transforma entidades a modelos de dominio, superpone datos en vivo de FIFA y usa mocks cuando la tabla está vacía. En detalle de partido combina eventos FIFA con OpenFootball como respaldo, y estadísticas oficiales con estimaciones editoriales si faltan. Este patrón centraliza reglas de selección/fallback y hace que la UI no tenga que saber cuál proveedor respondió.

## 7. Room y persistencia local

MundialDatabase.kt declara una base Room mundial.db, versión 6. Las entidades están en entities/Entities.kt: TeamEntity, PlayerEntity, StandingEntity, MatchEntity y SyncMeta. @PrimaryKey define claves; StandingEntity usa clave compuesta grupo+selección. Los DAOs en dao/Daos.kt exponen operaciones SQL seguras y suspend, como @Upsert y @Query.

```kotlin
@Dao
interface MatchDao {
    @Upsert suspend fun upsertAll(items: List<MatchEntity>)
    @Query("SELECT * FROM matches ORDER BY utcDate")
    suspend fun all(): List<MatchEntity>
    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun byId(id: String): MatchEntity?
}
```

Room genera la implementación en compilación mediante KSP. RoomDatabase administra SQLite; room-ktx integra operaciones suspendidas con coroutines. Mappers en DbMappers.kt convierten Entity ↔ Domain: la entidad representa el almacenamiento y Match/Team son el formato de negocio.

Decisión relevante: fallbackToDestructiveMigration() hace que, si Room no encuentra una migración al cambiar la versión, borre y recree la base. Es simple durante desarrollo, pero puede perder datos locales. No hay migraciones versionadas actualmente (exportSchema = false).

## 8. Sincronización, caché y resiliencia

[SyncManager.kt](../app/src/main/java/com/mundial26/app/data/sync/SyncManager.kt) implementa una semilla y refresco remoto. Lee sync_meta para evaluar TTL (6 horas), obtiene equipos/tabla/partidos/goleadores y los persiste usando DAOs. Mutex.withLock serializa operaciones concurrentes: varias pantallas pueden invocar ensureFresh() al inicio sin ejecutar simultáneamente la semilla.

```kotlin
suspend fun ensureFresh() = mutex.withLock {
    seedLocked()
    refreshLocked(force = false)
}

private suspend fun stale(key: String): Boolean {
    val last = metaDao.lastSync(key) ?: return true
    return last < now() - ttlMs
}
```

El timestamp se persiste por categoría (matches, standings, scorers). Las llamadas remotas están envueltas con runCatching; ante fallos, el flujo continúa con los datos disponibles/fallback. Es una estrategia orientada a disponibilidad, aunque puede ocultar la causa del fallo y no constituye sincronización en segundo plano programada: la sincronización sucede cuando la app consulta y en el refresh manual.

La app tiene varios niveles de caché: Room para equipos/planteles/tabla/partidos; memoria con TTL para noticias; SharedPreferences para selecciones de notificaciones y caché persistente de videos; cachés en memoria en fuentes y repositorios. La cache de partidos de Room no es observable como Flow: el repositorio entrega snapshots por llamadas suspendidas.

## 9. Coroutines y concurrencia

suspend identifica operaciones que pueden suspender sin bloquear el hilo llamador; no crea por sí solo un hilo. viewModelScope.launch inicia trabajo de pantalla. Las APIs Retrofit declaradas suspend se integran directamente. Las llamadas OkHttp directas usan withContext(Dispatchers.IO) para sacar I/O del hilo principal.

```kotlin
private suspend fun resolveHighlights(recent: List<Match>) = coroutineScope {
    recent.take(6).map { match ->
        async { highlightsSource.firstVideoId(/* consulta */) }
    }.awaitAll()
}
```

En Home, varios resúmenes se resuelven concurrentemente con async/awaitAll; el contenido principal se muestra antes de que termine ese proceso. Un loop del ViewModel refresca datos cada 30 segundos mientras la pantalla vive. El servicio foreground tiene su propio CoroutineScope(SupervisorJob() + Dispatchers.IO) y lo cancela en onDestroy. Mutex protege la sección crítica del sync. runCatching ofrece degradación ante error, con el cuidado de que también captura excepciones de cancelación si se aplica indiscriminadamente; las llamadas del proyecto deben mantener la cancelación cooperativa cuando se refactoricen.

## 10. Retrofit, OkHttp, serialización y APIs

[HttpClients.kt](../app/src/main/java/com/mundial26/app/data/net/HttpClients.kt) crea dos instancias Retrofit: Football Data y OpenFootball. OkHttp agrega X-Auth-Token a la primera; el valor se lee desde local.properties y se incorpora a BuildConfig. AppJson configura Kotlin Serialization para ignorar campos desconocidos y coaccionar entradas inválidas.

```kotlin
@GET("competitions/{c}/matches")
suspend fun matches(@Path("c") c: String = "WC"): FdMatchesResponse
```

Retrofit convierte interfaces anotadas a peticiones HTTP. @GET, @Path y @Query describen ruta/parámetros; DTOs y @Serializable modelan respuestas. [FootballDataApi.kt](../app/src/main/java/com/mundial26/app/data/remote/footballdata/FootballDataApi.kt) proporciona equipos, partidos, tablas, goleadores y perfiles; OpenFootballApi aporta eventos alternativos.

OkHttp también se usa directamente para fuentes que no usan Retrofit: FIFA/FIFA Data Hub, RSS de Google News, búsqueda y oEmbed de YouTube, y TheSportsDB. Las fuentes parsean JSON/XML/HTML y mapean a modelos. FIFA/OpenFootball se combinan en repositorios por campo: el feed FIFA es prioritario para minuto/marcador/eventos y OpenFootball es fallback de eventos. Hay parsing manual con kotlinx.serialization.json y expresiones regulares en RSS/HTML; no son APIs tipadas Retrofit.

Credenciales: local.properties no se debe versionar ni publicar. BuildConfig evita escribir el token en el código fuente, pero un secreto compilado dentro de una aplicación cliente puede extraerse del APK; no equivale a un secreto guardado en backend. Revisa .gitignore y no compartas el valor.

## 11. AlarmManager, BroadcastReceiver y notificaciones

[MatchAlarms.kt](../app/src/main/java/com/mundial26/app/notifications/MatchAlarms.kt) calcula el inicio UTC de próximos partidos y agenda PendingIntent a AlarmReceiver. Hay dos acciones: recordatorio 10 minutos antes y arranque del monitor de goles al kickoff. setAndAllowWhileIdle(RTC_WAKEUP, ...) permite una alarma inexacta en Doze; el aviso puede retrasarse y no requiere alarma exacta. Las claves programadas se guardan en SharedPreferences para cancelar y regenerar las anteriores.

```text
am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pendingIntent)
```

Notifications.kt crea canales (recordatorios, goles, seguimiento), construye notificaciones y prepara un PendingIntent a MainActivity con matchId. MundialApp consume ese ID y navega al detalle. En [NotificationsScreen.kt](../app/src/main/java/com/mundial26/app/ui/notifications/NotificationsScreen.kt), activar cualquiera de los switches solicita POST_NOTIFICATIONS en Android 13+; el envío también comprueba si las notificaciones están habilitadas. El callback de permiso no revierte el switch si el usuario lo deniega, por lo que el servicio puede seguir corriendo aunque el sistema no muestre avisos.

Las alarmas se resincronizan al abrir la app y al cambiar switches. El propio código señala que no hay receiver de BOOT_COMPLETED: tras reiniciar, se recuperan al reabrir. También se guardan preferencias de switches y goles ya notificados mediante SharedPreferences; dos flags no justificaron añadir DataStore.

## 12. Foreground Service para goles en vivo

[LiveGoalsService.kt](../app/src/main/java/com/mundial26/app/notifications/LiveGoalsService.kt) es un Service Android promovido a foreground con tipo dataSync. Su CoroutineScope consulta partidos cada 15 segundos mientras la preferencia de goles siga activa. Detecta un gol primero por cambio de marcador (rápido) y luego actualiza la notificación con goleador/minuto cuando está disponible la timeline. Evita avisos falsos en el primer barrido, cancela un aviso provisional si el marcador retrocede por VAR, y se detiene tras una racha sin partidos en vivo.

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    ServiceCompat.startForeground(
        this, Notif.ONGOING_ID, Notif.ongoing(this, "Siguiendo los goles en vivo"),
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    )
    if (!running) { running = true; loop() }
    return START_STICKY
}
```

Un foreground service debe mostrar notificación persistente y está sujeto a límites de Android y políticas de plataforma; no es garantía de ejecución infinita. El manifiesto declara FOREGROUND_SERVICE y FOREGROUND_SERVICE_DATA_SYNC. El código no usa WorkManager; AlarmManager inicia el seguimiento en el kickoff y el servicio mantiene el sondeo en vivo.

## 13. Pruebas JUnit y doubles

Las dependencias de prueba incluyen JUnit 4, kotlinx-coroutines-test, Room testing, Robolectric y AndroidX Test Core. Las pruebas bajo app/src/test cubren:

- SyncManagerTest: seed, persistencia y actualización desde un FakeApi, con Room en memoria y Robolectric.
- DaoTest, DbMappersTest: consultas y transformación DB ↔ modelo.
- mappers de Football Data/OpenFootball, parsing de FIFA, RSS, YouTube, SportsDB y códigos de país.
- NameMatchTest: comparación tolerante de nombres de jugadores.
- FakeRepositoriesTest: datos falsos e interfaces de repositorio.
Ejemplo: la prueba del SyncManager inyecta un FootballDataApi falso y una función now controlada, de modo que verifica reglas sin depender de Internet ni del reloj real. Las pruebas de parseadores son pruebas unitarias deterministas. Algunas pruebas de DB requieren entorno Android simulado; Robolectric provee APIs Android JVM y Room en memoria evita escribir la base de usuario.

El estilo de aserción JUnit que se encuentra en el proyecto es directo:

```kotlin
@Test fun seed_y_refresh() = runTest {
    sync.seedIfEmpty()
    assertTrue(db.teamDao().count() > 0)
    sync.refreshIfStale()
    assertEquals(5, db.playerDao().topScorers(1)[0].goals)
}
```

runTest habilita una prueba de coroutine; assertTrue y assertEquals expresan lo esperado. El ejemplo real está en data/sync/SyncManagerTest.kt y usa un FakeApi.

Los FakeMatchRepository, FakeTeamRepository y FakeNewsRepository implementan contratos con MockData. No confundirlos con los fallbacks productivos dentro de repositorios: ambos permiten datos de muestra, pero los fakes sirven para pruebas y los fallbacks ayudan a la app si no hay datos remotos/locales.

## 14. Fuentes de datos y modelos: qué es real y qué es estimado

La app integra Football Data (Retrofit, token), FIFA (OkHttp, endpoints públicos observados por el código), OpenFootball (Retrofit y GitHub raw), Google News RSS, YouTube (scraping de HTML + oEmbed), y TheSportsDB (consulta directa para club). Hay además EditorialSource (valoraciones/comentarios curados, correcciones y rankings) y MockData.

Los modelos de dominio están en domain/model/Models.kt; DTOs remotos y Entity DB permanecen separados. Este límite permite que un cambio en la forma del JSON no tenga que propagarse hasta Compose. Las estadísticas de partido pueden marcarse statsEstimated: la UI puede distinguir valores estimados de oficiales. Equipo destacado y análisis editorial no salen necesariamente de APIs.

## 15. Otras piezas relevantes

- Navegación: Navigation Compose, destinos en [Destinations.kt](../app/src/main/java/com/mundial26/app/ui/navigation/Destinations.kt), argumentos de rutas y navegación desde notificaciones.
- Imágenes y video: Coil Compose carga imágenes; Media3 ExoPlayer/UI y Android YouTube Player están declarados para video. El proyecto además busca videos de resumen y valida oEmbed.
- Preferencias: SharedPreferences se usa para preferencias de notificación y caché de YouTube. No hay DataStore.
- Acceso a red: Manifest declara INTERNET y ACCESS_NETWORK_STATE.
- Probabilidad: domain/prediction/WinProbability.kt contiene lógica de predicción local; no es un modelo ML ni una llamada a IA.
- Reproducibilidad: versiones centralizadas en catálogo Gradle. No se ve Hilt/Dagger, WorkManager, Paging, DataStore, Firebase ni una arquitectura multi-módulo.
## 16. Flujo completo de ejemplo: cargar la pantalla de partidos

1. MundialApp resuelve la ruta matches y crea MatchesViewModel.Factory(container.matchRepository).
1. MatchesViewModel.init llama load(), que lanza loadData() en viewModelScope.
1. El ViewModel solicita los partidos al contrato MatchRepository.
1. DefaultMatchRepository llama SyncManager.ensureFresh(): bajo Mutex, puede consultar Retrofit, mapear DTOs y hacer upsert en Room. Luego obtiene filas de MatchDao y las convierte a Match.
1. Añade estado live de FIFA cuando se puede; si Room está vacía usa MockData.matches.
1. El ViewModel agrupa y ordena partidos, publica MatchesUiState.Success en StateFlow.
1. Compose recolecta el flujo con collectAsStateWithLifecycle y dibuja lista/calendario.
1. Pull-to-refresh llama repository.refresh() y recarga. Cada 30 segundos el ViewModel vuelve a consultar mientras está vivo.
Este recorrido muestra por qué la pantalla no debería hacer llamadas HTTP ni SQL directamente: el ViewModel coordina el caso de uso de UI y el Repository resuelve las fuentes.

## 17. Resumen para entrevista

> **Para recordar:** “Es una app Android nativa de un módulo, escrita en Kotlin y Compose. La UI observa estados StateFlow desde ViewModels; estos dependen de contratos de repositorio. Los repositorios coordinan Room como almacenamiento local, sincronización Retrofit/OkHttp y fuentes auxiliares con fallbacks. Room modela entidades y DAOs, mientras los mappers separan datos persistidos/remotos de los modelos de dominio. Una inyección manual con Application crea un contenedor de dependencias. Las coroutines manejan I/O y ciclos de refresco; AlarmManager programa recordatorios y un Foreground Service sondea partidos en vivo para notificar goles. Hay pruebas JUnit de mappers, repositorios, DAOs y sync, con fakes, Room en memoria y Robolectric.”

## 18. Archivos para seguir leyendo

1. [MundialApp.kt](../app/src/main/java/com/mundial26/app/MundialApp.kt) — composición y rutas.
1. [HomeViewModel.kt](../app/src/main/java/com/mundial26/app/ui/home/HomeViewModel.kt) — MVVM, coroutines, estado.
1. [Repositories.kt](../app/src/main/java/com/mundial26/app/domain/repository/Repositories.kt) — contratos.
1. [DefaultMatchRepository.kt](../app/src/main/java/com/mundial26/app/data/repository/DefaultMatchRepository.kt) — composición de datos.
1. [SyncManager.kt](../app/src/main/java/com/mundial26/app/data/sync/SyncManager.kt) y Room en data/local/db/ — persistencia y sync.
1. [HttpClients.kt](../app/src/main/java/com/mundial26/app/data/net/HttpClients.kt) y data/remote/ — networking.
1. [MatchAlarms.kt](../app/src/main/java/com/mundial26/app/notifications/MatchAlarms.kt) y [LiveGoalsService.kt](../app/src/main/java/com/mundial26/app/notifications/LiveGoalsService.kt) — notificaciones y proceso en background.
1. app/src/test/ — ejemplos de pruebas.
## 19. Un partido desde la API hasta la pantalla

Este recorrido une las capas con una sola pregunta: “¿Cómo termina un partido remoto dibujado en la lista de Partidos?” Sigue los archivos en el orden indicado.

### Paso 1: declarar el endpoint y el formato de respuesta

En [FootballDataApi.kt](../app/src/main/java/com/mundial26/app/data/remote/footballdata/FootballDataApi.kt), Retrofit recibe una interfaz declarativa:

```kotlin
@GET("competitions/{c}/matches")
suspend fun matches(@Path("c") c: String = "WC"): FdMatchesResponse
```

1. @GET aporta el sufijo de la URL base.
1. {c} es un segmento variable; @Path lo reemplaza por WC si no se pasa otro valor.
1. suspend permite invocarla dentro de una coroutine sin bloquear el hilo UI.
1. Retrofit ejecuta HTTP y el converter Kotlin Serialization transforma JSON en FdMatchesResponse.
La respuesta no es todavía un Match de la UI. Es un DTO que refleja la forma de la API.

### Paso 2: crear transporte y autenticación

En [HttpClients.kt](../app/src/main/java/com/mundial26/app/data/net/HttpClients.kt), el cliente Retrofit usa https://api.football-data.org/v4/. Un interceptor de OkHttp agrega el encabezado X-Auth-Token; después el converter convierte JSON. Esta separación deja la autenticación/transporte en data/net y las rutas en FootballDataApi.

### Paso 3: transformar DTO remoto a entidad Room

En [FootballDataMappers.kt](../app/src/main/java/com/mundial26/app/data/remote/footballdata/FootballDataMappers.kt):

```kotlin
fun FdMatch.toEntity(): MatchEntity {
    val pens = score.penalties.takeIf { score.duration == "PENALTY_SHOOTOUT" }
    val home = if (pens != null) {
        (score.regularTime?.home ?: ((score.fullTime.home ?: 0) - (pens.home ?: 0))) +
            (score.extraTime?.home ?: 0)
    } else score.fullTime.home
    val away = if (pens != null) {
        (score.regularTime?.away ?: ((score.fullTime.away ?: 0) - (pens.away ?: 0))) +
            (score.extraTime?.away ?: 0)
    } else score.fullTime.away
    return MatchEntity(
        id = id.toString(), utcDate = utcDate, status = status, stage = stage,
        grp = group ?: "", homeTla = homeTeam.tla, awayTla = awayTeam.tla,
        scoreHome = home, scoreAway = away, penHome = pens?.home, penAway = pens?.away
    )
}
```

Este mapper es real y muestra una regla importante: el marcador del tiempo reglamentario/alargue se persiste aparte del resultado de penales. Si guardáramos solo fullTime, football-data puede incluir allí los penales y la UI terminaría mostrando un marcador como 4-5 en lugar del marcador del partido 1-1 más el desempate 3-4.

El mapper decide cómo representar un valor remoto en almacenamiento. El proyecto trata por separado el caso de penales para guardar el marcador del partido y el desempate en columnas distintas. Esta regla vive en el mapper, porque depende del formato de la API.

### Paso 4: sincronizar y guardar

SyncManager.ensureFresh() ejecuta seedLocked() y refreshLocked(false) bajo un Mutex. Si los datos están vencidos, solicita el endpoint, mapea DTOs a Entities y hace upsert con DAO. SyncMeta guarda cuándo se sincronizó cada categoría.

```text
DTO remoto → toEntity() → MatchEntity → MatchDao.upsertAll() → SQLite
```

El upsert inserta filas nuevas y actualiza las que ya existen según su clave. La clave id impide acumular múltiples filas del mismo partido.

### Paso 5: convertir almacenamiento a dominio

El repositorio lee MatchEntity y llama toDomain() de DbMappers.kt. Ese mapper traduce estado, fecha/hora UTC a zona local, banderas y nombres de etapa a un Match. Por ejemplo, las fechas se convierten con Instant.parse(...).atZone(zone) para mostrar la hora local del dispositivo.

```text
MatchEntity (persistencia) → toDomain() → Match (negocio/UI)
```

Separar los formatos evita que SQL o los nombres de campos de un proveedor contaminen la UI.

### Paso 6: componer fuentes y publicar estado

DefaultMatchRepository puede superponer el estado live de FIFA al snapshot de Room. Si no existen filas, puede devolver MockData.matches. Luego MatchesViewModel obtiene vivos, próximos, terminados y calendario; finalmente emite MatchesUiState.Success por StateFlow.

### Paso 7: dibujar y navegar

MatchesScreen recolecta el StateFlow con collectAsStateWithLifecycle, elige la representación de Loading, Error o Success, y dibuja filas Compose. Al tocar una fila ejecuta el callback onMatchClick(id). La navegación construye matchDetail/{id}; el destino crea MatchDetailViewModel con el repositorio y ese ID.

<details>
<summary>Comprueba el recorrido</summary>

¿Qué cambiarías si Football Data agregara un campo nuevo que la UI necesita?

**Respuesta:** revisar DTO, mapper a Entity o modelo, persistencia si hace falta, modelo de dominio y pantalla/ViewModel. Si el dato no debe persistirse, podría mapearse al modelo de dominio desde la fuente. No conviene poner el parseo directamente en Compose.

</details>

## 20. Ejercicios de comprensión

Intenta responder antes de desplegar cada solución. Estas preguntas prueban que entiendes el flujo, no que memorices nombres.

<details>
<summary>Ejercicio 1 — ¿Quién puede cambiar el estado de Home?</summary>

La pantalla recibe un StateFlow<HomeUiState>, ¿puede asignarle un HomeUiState.Error?

**Solución:** no debería. La UI recibe StateFlow de solo lectura. El ViewModel conserva el MutableStateFlow privado y modifica _uiState.value. Así la UI observa el estado y llama acciones (retry, refresh) en vez de escribirlo.

</details>

<details>
<summary>Ejercicio 2 — ¿Por qué no llamar a la API desde un Composable?</summary>

**Solución:** un Composable puede recomponerse muchas veces. Si el request viviera directamente en el cuerpo de la función, podría repetirse por una recomposición. La petición pertenece a un ViewModel/caso coordinador; Compose solo observa el resultado. Los efectos Compose sirven para acciones vinculadas al ciclo de composición, no para reemplazar la capa de datos.

</details>

<details>
<summary>Ejercicio 3 — Room está vacía y la red falla. ¿Qué puede verse?</summary>

**Solución:** varios repositorios devuelven MockData como fallback cuando la consulta local queda vacía. Si Room sí tiene datos, un error de red suele dejar disponible el snapshot almacenado. En noticias, la caché en memoria o mock puede ser la fuente. Por eso “la pantalla muestra datos” no implica necesariamente “la API respondió”.

</details>

<details>
<summary>Ejercicio 4 — ¿Qué garantiza el Mutex en SyncManager?</summary>

**Solución:** serializa las secciones críticas de sincronización dentro de esa instancia de SyncManager: dos llamadas concurrentes no ejecutan a la vez el seed/refresh. No evita duplicados entre procesos Android distintos ni reemplaza claves únicas de DB.

</details>

<details>
<summary>Ejercicio 5 — ¿Qué pasa con un dato al cambiar una entidad Room?</summary>

**Solución:** la versión de base debe avanzar y se necesita una migración que transforme datos existentes. La configuración actual usa fallbackToDestructiveMigration(), por lo que una ruta de migración inexistente puede borrar la DB local y recrearla. Esa estrategia es tolerable para prototipo, pero riesgosa para datos que el usuario deba conservar.

</details>

<details>
<summary>Ejercicio 6 — ¿AlarmManager sondea los goles cada 15 segundos?</summary>

**Solución:** no. AlarmManager programa el recordatorio y el inicio del seguimiento al kickoff. El LiveGoalsService ejecuta el bucle y consulta el repositorio cada 15 segundos durante el seguimiento. Son mecanismos con ciclos de vida distintos.

</details>

<details>
<summary>Ejercicio 7 — ¿Por qué existe el primer barrido silencioso del servicio?</summary>

**Solución:** al iniciar el servicio un partido ya puede tener goles. El primer recorrido guarda ese marcador y marca eventos existentes como vistos; si notificara todo lo encontrado, el usuario recibiría una ráfaga de goles antiguos como si acabaran de ocurrir.

</details>

<details>
<summary>Ejercicio 8 — ¿Qué aporta el FakeApi a SyncManagerTest?</summary>

**Solución:** controla la respuesta de red para que el test sea determinista, offline y rápido. Puede verificar que los datos se mapean/persisten sin depender del servicio externo. Room en memoria aísla la base de producción.

</details>

## 21. Glosario corto

| Término | En este proyecto significa |
| --- | --- |
| DTO | Objeto que representa la respuesta de una fuente externa. |
| Entity | Objeto que Room guarda como fila de tabla. |
| Modelo de dominio | Tipo Kotlin que representa el concepto usado por la aplicación. |
| DAO | Interfaz Room con consultas e inserciones para una tabla. |
| Mapper | Función que traduce un DTO o Entity a otro modelo. |
| Repository | Contrato/implementación que decide de dónde salen los datos y cómo se combinan. |
| StateFlow | Flujo que conserva el último estado y lo entrega a observadores. |
| Coroutine | Unidad de trabajo suspendible y cancelable que evita bloquear el hilo mientras espera I/O. |
| TTL | Tiempo máximo antes de considerar que una caché debe refrescarse. |
| PendingIntent | Token que permite a Android ejecutar más tarde una acción en nombre de la app. |
| Foreground Service | Servicio visible para el usuario mediante notificación persistente mientras realiza una tarea activa. |

## 22. Mini guía de lectura del código

Para entender una función nueva, haz estas preguntas en orden:

1. Quién la llama: busca la llamada desde Screen, ViewModel, Repository o Service.
2. Qué recibe y devuelve: distingue ID, DTO, Entity y modelo de dominio.
3. Dónde puede suspender: busca suspend, launch, async y withContext.
4. Qué pasa si falla: encuentra try/catch, runCatching, valores null, listas vacías y fallbacks.
5. Dónde persiste/cacha: busca DAO, SharedPreferences y mapas de caché.
6. Cómo se verifica: busca el test que cubre mapper, regla o sincronización.
Ejemplo de seguimiento práctico: desde MatchesScreen ve al MatchesViewModel; sigue matchRepository.resultsAndUpcoming() a DefaultMatchRepository; baja a SyncManager/MatchDao; después vuelve por el StateFlow a la UI. Ese recorrido enseña mejor las capas que leer todos los archivos de una carpeta de corrido.