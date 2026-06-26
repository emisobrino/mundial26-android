package com.mundial26.app.data.mock

import com.mundial26.app.domain.model.*

/**
 * Datos hardcodeados portados del prototipo de diseño
 * (`Mundial 2026.dc.html`, clase Component extends DCLogic).
 * Sirven como fuente para las implementaciones Fake* de los repositorios.
 */
object MockData {

    /** Id del equipo destacado en Inicio (Argentina, campeón defensor). */
    const val featuredTeamId: String = "arg"

    // ---------------------------------------------------------------------
    // Selecciones
    // ---------------------------------------------------------------------
    val teams: List<Team> = listOf(
        Team(
            id = "arg", name = "Argentina", code = "ARG", conf = Confederation.CONMEBOL,
            rank = 1, tag = "Campeón defensor", flag = "ar",
            styleLabel = "Posesión vertical",
            star = Star(10, "Lionel Messi", "Delantero / Capitán",
                "Cerebro creativo del equipo: maneja los tiempos y aparece en los momentos decisivos."),
            pros = listOf(
                "Jerarquía y experiencia en partidos clave",
                "Mediocampo equilibrado y muy trabajador",
                "Definición letal de sus delanteros"
            ),
            cons = listOf(
                "Defensa con poca velocidad",
                "Dependencia de Messi en la creación",
                "Plantel con varios veteranos"
            ),
            form = listOf('V', 'V', 'E', 'V', 'V')
        ),
        Team(
            id = "fra", name = "Francia", code = "FRA", conf = Confederation.UEFA,
            rank = 2, tag = "Candidato al título", flag = "fr",
            styleLabel = "Transiciones rápidas",
            star = Star(10, "Kylian Mbappé", "Delantero",
                "Velocidad y gol desequilibrantes: capaz de decidir un partido en una sola jugada."),
            pros = listOf(
                "Velocidad explosiva en ataque",
                "Profundidad de plantel enorme",
                "Solidez física en todas las líneas"
            ),
            cons = listOf(
                "Altibajos de concentración defensiva",
                "Gestión de egos en el vestuario",
                "Arranques de partido lentos"
            ),
            form = listOf('V', 'V', 'V', 'D', 'V')
        ),
        Team(
            id = "bra", name = "Brasil", code = "BRA", conf = Confederation.CONMEBOL,
            rank = 3, tag = "Candidato al título", flag = "br",
            styleLabel = "Ataque por bandas",
            star = Star(7, "Vinícius Jr.", "Extremo",
                "Desborde y uno contra uno: el principal arma de profundidad por la izquierda."),
            pros = listOf(
                "Talento individual desbordante",
                "Peligro constante por las bandas",
                "Generación joven en ascenso"
            ),
            cons = listOf(
                "Fragilidad defensiva ante contras",
                "Falta de un \"9\" fijo y fiable",
                "Presión histórica por el resultado"
            ),
            form = listOf('V', 'E', 'V', 'V', 'D')
        ),
        Team(
            id = "esp", name = "España", code = "ESP", conf = Confederation.UEFA,
            rank = 5, tag = "Candidato al título", flag = "es",
            styleLabel = "Posesión y presión alta",
            star = Star(19, "Lamine Yamal", "Extremo",
                "Regate y visión sorprendentes para su edad: el gran desequilibrante del proyecto."),
            pros = listOf(
                "Control del balón y del ritmo",
                "Presión alta muy coordinada",
                "Recambio joven de altísimo nivel"
            ),
            cons = listOf(
                "Falta de un goleador nato",
                "Vulnerable a la contra rápida",
                "Poca experiencia en fases finales"
            ),
            form = listOf('V', 'V', 'E', 'V', 'V')
        ),
        Team(
            id = "eng", name = "Inglaterra", code = "ENG", conf = Confederation.UEFA,
            rank = 4, tag = "Candidato al título", flag = "gb-eng",
            styleLabel = "Juego directo estructurado",
            star = Star(10, "Jude Bellingham", "Mediocampista",
                "Llegada y gol desde el medio: combina físico, técnica y jerarquía pese a su edad."),
            pros = listOf(
                "Gran talento ofensivo individual",
                "Fortaleza en el juego aéreo",
                "Profundidad en todas las posiciones"
            ),
            cons = listOf(
                "Planteamientos conservadores",
                "Dudas bajo presión en penales",
                "Falta de un sistema definido"
            ),
            form = listOf('V', 'V', 'V', 'E', 'V')
        ),
        Team(
            id = "mex", name = "México", code = "MEX", conf = Confederation.CONCACAF,
            rank = 14, tag = "Selección anfitriona", flag = "mx",
            styleLabel = "Intensidad y orden",
            star = Star(9, "Santiago Giménez", "Delantero",
                "Olfato goleador dentro del área: vive de la última jugada y aprovecha cada ocasión."),
            pros = listOf(
                "Empuje del público local",
                "Buen estado físico y presión",
                "Portería confiable"
            ),
            cons = listOf(
                "Poca pegada ante rivales top",
                "Historial corto en fases decisivas",
                "Defensa expuesta a la velocidad"
            ),
            form = listOf('V', 'E', 'V', 'D', 'E')
        ),
        Team(
            id = "por", name = "Portugal", code = "POR", conf = Confederation.UEFA,
            rank = 6, tag = "Candidato al título", flag = "pt",
            styleLabel = "Ataque asociativo",
            star = Star(10, "Rafael Leão", "Extremo",
                "Desequilibrio y potencia: cuando arranca a velocidad es casi imparable por su carril."),
            pros = listOf(
                "Generación dorada en el mediocampo",
                "Variedad de soluciones ofensivas",
                "Experiencia europea de élite"
            ),
            cons = listOf(
                "Transición generacional en defensa",
                "Dependencia del estado anímico",
                "Gestión de minutos de veteranos"
            ),
            form = listOf('V', 'V', 'D', 'V', 'E')
        ),
        Team(
            id = "ger", name = "Alemania", code = "GER", conf = Confederation.UEFA,
            rank = 10, tag = "Candidato al título", flag = "de",
            styleLabel = "Posesión moderna",
            star = Star(10, "Jamal Musiala", "Mediapunta",
                "Conducción y desequilibrio en espacios reducidos: el motor creativo de la nueva era."),
            pros = listOf(
                "Mediocampo creativo y joven",
                "Mentalidad competitiva histórica",
                "Variantes tácticas según el rival"
            ),
            cons = listOf(
                "Reconstrucción defensiva reciente",
                "Irregularidad de resultados",
                "Falta de un \"9\" de garantías"
            ),
            form = listOf('E', 'V', 'D', 'V', 'V')
        ),
        Team(
            id = "ned", name = "Países Bajos", code = "NED", conf = Confederation.UEFA,
            rank = 7, tag = "Candidato europeo", flag = "nl",
            styleLabel = "Posesión y orden",
            star = Star(4, "Virgil van Dijk", "Defensa central",
                "Liderazgo y salida limpia desde el fondo: el ancla del equipo."),
            pros = listOf(
                "Defensa jerárquica y ordenada",
                "Salida de balón cuidada",
                "Experiencia en torneos grandes"
            ),
            cons = listOf(
                "Falta de un extremo desequilibrante",
                "Mediocampo a veces lento",
                "Recambio ofensivo limitado"
            ),
            form = listOf('V', 'V', 'E', 'D', 'V')
        ),
        Team(
            id = "cro", name = "Croacia", code = "CRO", conf = Confederation.UEFA,
            rank = 9, tag = "Veteranía de oro", flag = "hr",
            styleLabel = "Control de mediocampo",
            star = Star(10, "Luka Modrić", "Mediocampista",
                "Pausa, último pase y jerarquía pese a la edad."),
            pros = listOf(
                "Mediocampo de gran calidad",
                "Sabe competir partidos cerrados",
                "Mentalidad de torneo"
            ),
            cons = listOf(
                "Plantel envejecido",
                "Poca velocidad en defensa",
                "Dependencia de sus referentes"
            ),
            form = listOf('E', 'V', 'V', 'E', 'D')
        ),
        Team(
            id = "uru", name = "Uruguay", code = "URU", conf = Confederation.CONMEBOL,
            rank = 11, tag = "Garra y juventud", flag = "uy",
            styleLabel = "Intensidad y transición",
            star = Star(9, "Darwin Núñez", "Delantero",
                "Potencia y profundidad constante en ataque."),
            pros = listOf(
                "Mezcla de juventud y experiencia",
                "Intensidad y presión alta",
                "Peligro a balón parado"
            ),
            cons = listOf(
                "Definición irregular",
                "Indisciplina táctica por momentos",
                "Laterales expuestos"
            ),
            form = listOf('V', 'V', 'V', 'E', 'V')
        ),
        Team(
            id = "jpn", name = "Japón", code = "JPN", conf = Confederation.OTHER,
            rank = 17, tag = "Sorpresa asiática", flag = "jp",
            styleLabel = "Presión y velocidad",
            star = Star(8, "Takefusa Kubo", "Extremo",
                "Regate y conducción para romper líneas."),
            pros = listOf(
                "Colectivo muy trabajado",
                "Velocidad por las bandas",
                "Disciplina táctica"
            ),
            cons = listOf(
                "Falta de físico en los duelos",
                "Poca pegada ante rivales top",
                "Inexperiencia en fases finales"
            ),
            form = listOf('V', 'D', 'V', 'V', 'E')
        ),
        Team(
            id = "usa", name = "Estados Unidos", code = "USA", conf = Confederation.CONCACAF,
            rank = 13, tag = "Coanfitrión", flag = "us",
            styleLabel = "Energía y transición",
            star = Star(10, "Christian Pulisic", "Extremo",
                "Desequilibrio y gol: la cara de la selección local."),
            pros = listOf(
                "Empuje de la localía",
                "Plantel joven curtido en Europa",
                "Buen estado físico"
            ),
            cons = listOf(
                "Falta de un \"9\" fiable",
                "Defensa irregular",
                "Poca experiencia ganadora"
            ),
            form = listOf('V', 'E', 'D', 'V', 'V')
        ),
        Team(
            id = "can", name = "Canadá", code = "CAN", conf = Confederation.CONCACAF,
            rank = 30, tag = "Coanfitrión revelación", flag = "ca",
            styleLabel = "Velocidad vertical",
            star = Star(19, "Alphonso Davies", "Lateral / Extremo",
                "Velocidad descomunal por el carril izquierdo."),
            pros = listOf(
                "Velocidad letal en transición",
                "Generación joven en alza",
                "Juega sin presión de favoritismo"
            ),
            cons = listOf(
                "Inexperiencia en Mundiales",
                "Definición pobre",
                "Defensa frágil"
            ),
            form = listOf('D', 'V', 'E', 'D', 'V')
        ),
        Team(
            id = "mar", name = "Marruecos", code = "MAR", conf = Confederation.OTHER,
            rank = 12, tag = "Semifinalista histórico", flag = "ma",
            styleLabel = "Bloque y contra",
            star = Star(2, "Achraf Hakimi", "Lateral",
                "Despliegue total por la derecha: defiende y ataca."),
            pros = listOf(
                "Solidez defensiva colectiva",
                "Contraataque letal",
                "Fuerte espíritu de equipo"
            ),
            cons = listOf(
                "Generación ofensiva corta",
                "Depende del orden defensivo",
                "Pocos goles desde el medio"
            ),
            form = listOf('V', 'V', 'V', 'D', 'E')
        ),
        Team(
            id = "sen", name = "Senegal", code = "SEN", conf = Confederation.OTHER,
            rank = 18, tag = "Potencia africana", flag = "sn",
            styleLabel = "Físico y verticalidad",
            star = Star(9, "Nicolas Jackson", "Delantero",
                "Potencia y movilidad para atacar la espalda rival."),
            pros = listOf(
                "Físico superior en los duelos",
                "Velocidad arriba",
                "Portería confiable"
            ),
            cons = listOf(
                "Imprecisión en el último pase",
                "Altibajos de concentración",
                "Dependencia del físico"
            ),
            form = listOf('V', 'E', 'D', 'V', 'V')
        ),
    )

    private fun flagOf(code: String): String =
        teams.find { it.code == code }?.flag ?: code.lowercase()

    // ---------------------------------------------------------------------
    // Partidos
    // ---------------------------------------------------------------------
    val matches: List<Match> = listOf(
        Match(
            id = "m1", status = MatchStatus.LIVE, minute = 67, time = null,
            day = null, group = "Grupo A", stadium = "Estadio Azteca · Ciudad de México",
            teamA = "ARG", teamB = "MEX", flagA = flagOf("ARG"), flagB = flagOf("MEX"),
            scoreA = 2, scoreB = 1,
            stats = listOf(
                MatchStat("Posesión", 58, 42),
                MatchStat("Remates", 11, 6),
                MatchStat("A puerta", 5, 2),
                MatchStat("Córners", 6, 3),
                MatchStat("Faltas", 8, 12),
            ),
            events = listOf(
                MatchEvent(12, EventKind.GOAL, 'A', "L. Messi"),
                MatchEvent(28, EventKind.GOAL, 'B', "S. Giménez"),
                MatchEvent(45, EventKind.YELLOW, 'B', "E. Álvarez"),
                MatchEvent(54, EventKind.GOAL, 'A', "J. Álvarez"),
                MatchEvent(61, EventKind.SUB, 'A', "Di María ⟶ Garnacho"),
            )
        ),
        Match(
            id = "m2", status = MatchStatus.LIVE, minute = 34, time = null,
            day = null, group = "Grupo C", stadium = "MetLife Stadium · Nueva Jersey",
            teamA = "FRA", teamB = "GER", flagA = flagOf("FRA"), flagB = flagOf("GER"),
            scoreA = 1, scoreB = 1,
            stats = listOf(
                MatchStat("Posesión", 47, 53),
                MatchStat("Remates", 5, 7),
                MatchStat("A puerta", 2, 3),
                MatchStat("Córners", 2, 4),
                MatchStat("Faltas", 6, 5),
            ),
            events = listOf(
                MatchEvent(12, EventKind.GOAL, 'B', "J. Musiala"),
                MatchEvent(25, EventKind.GOAL, 'A', "K. Mbappé"),
                MatchEvent(31, EventKind.YELLOW, 'A', "A. Tchouaméni"),
            )
        ),
        Match(
            id = "m3", status = MatchStatus.FINISHED, minute = 90, time = null,
            day = null, group = "Grupo D", stadium = "SoFi Stadium · Los Ángeles",
            teamA = "BRA", teamB = "ESP", flagA = flagOf("BRA"), flagB = flagOf("ESP"),
            scoreA = 3, scoreB = 1,
            stats = listOf(
                MatchStat("Posesión", 51, 49),
                MatchStat("Remates", 14, 9),
                MatchStat("A puerta", 7, 3),
                MatchStat("Córners", 7, 5),
                MatchStat("Faltas", 10, 9),
            ),
            events = listOf(
                MatchEvent(9, EventKind.GOAL, 'A', "Vinícius Jr."),
                MatchEvent(34, EventKind.GOAL, 'B', "L. Yamal"),
                MatchEvent(52, EventKind.GOAL, 'A', "Rodrygo"),
                MatchEvent(77, EventKind.GOAL, 'A', "Vinícius Jr."),
                MatchEvent(84, EventKind.YELLOW, 'B', "Rodri"),
            )
        ),
        Match(
            id = "m4", status = MatchStatus.UPCOMING, minute = null, time = "20:00",
            day = "Hoy", group = "Grupo C", stadium = "BC Place · Vancouver",
            teamA = "ENG", teamB = "POR", flagA = flagOf("ENG"), flagB = flagOf("POR"),
            scoreA = null, scoreB = null
        ),
        Match(
            id = "m5", status = MatchStatus.UPCOMING, minute = null, time = "18:00",
            day = "Mañana", group = "Octavos", stadium = "AT&T Stadium · Dallas",
            teamA = "ARG", teamB = "BRA", flagA = flagOf("ARG"), flagB = flagOf("BRA"),
            scoreA = null, scoreB = null
        ),
        Match(
            id = "m6", status = MatchStatus.UPCOMING, minute = null, time = "17:00",
            day = "Mañana", group = "Grupo A", stadium = "Estadio Akron · Guadalajara",
            teamA = "MEX", teamB = "JPN", flagA = flagOf("MEX"), flagB = flagOf("JPN"),
            scoreA = null, scoreB = null
        ),
        Match(
            id = "m7", status = MatchStatus.UPCOMING, minute = null, time = "20:00",
            day = "Mañana", group = "Grupo A", stadium = "NRG Stadium · Houston",
            teamA = "NED", teamB = "URU", flagA = flagOf("NED"), flagB = flagOf("URU"),
            scoreA = null, scoreB = null
        ),
        Match(
            id = "m8", status = MatchStatus.UPCOMING, minute = null, time = "16:00",
            day = "Viernes", group = "Grupo D", stadium = "Lumen Field · Seattle",
            teamA = "ESP", teamB = "POR", flagA = flagOf("ESP"), flagB = flagOf("POR"),
            scoreA = null, scoreB = null
        ),
        Match(
            id = "m9", status = MatchStatus.UPCOMING, minute = null, time = "19:00",
            day = "Viernes", group = "Grupo D", stadium = "Levi's Stadium · San Francisco",
            teamA = "BRA", teamB = "GER", flagA = flagOf("BRA"), flagB = flagOf("GER"),
            scoreA = null, scoreB = null
        ),
        Match(
            id = "m10", status = MatchStatus.UPCOMING, minute = null, time = "16:00",
            day = "Sábado", group = "Grupo B", stadium = "Mercedes-Benz Stadium · Atlanta",
            teamA = "ARG", teamB = "CRO", flagA = flagOf("ARG"), flagB = flagOf("CRO"),
            scoreA = null, scoreB = null
        ),
        Match(
            id = "m11", status = MatchStatus.UPCOMING, minute = null, time = "19:00",
            day = "Sábado", group = "Grupo B", stadium = "Hard Rock Stadium · Miami",
            teamA = "SEN", teamB = "CAN", flagA = flagOf("SEN"), flagB = flagOf("CAN"),
            scoreA = null, scoreB = null
        ),
        Match(
            id = "m12", status = MatchStatus.UPCOMING, minute = null, time = "15:00",
            day = "Domingo", group = "Grupo C", stadium = "Arrowhead Stadium · Kansas City",
            teamA = "ENG", teamB = "MAR", flagA = flagOf("ENG"), flagB = flagOf("MAR"),
            scoreA = null, scoreB = null
        ),
        Match(
            id = "m13", status = MatchStatus.UPCOMING, minute = null, time = "18:00",
            day = "Domingo", group = "Grupo C", stadium = "Gillette Stadium · Boston",
            teamA = "FRA", teamB = "USA", flagA = flagOf("FRA"), flagB = flagOf("USA"),
            scoreA = null, scoreB = null
        ),
    )

    // ---------------------------------------------------------------------
    // Grupos (posiciones)
    // ---------------------------------------------------------------------
    val groups: List<Group> = listOf(
        Group(
            "Grupo A", listOf(
                GroupRow("MEX", flagOf("MEX"), pj = 3, g = 2, e = 1, p = 0, gf = 5, gc = 2, pts = 7),
                GroupRow("NED", flagOf("NED"), pj = 3, g = 2, e = 0, p = 1, gf = 4, gc = 3, pts = 6),
                GroupRow("URU", flagOf("URU"), pj = 3, g = 1, e = 1, p = 1, gf = 4, gc = 4, pts = 4),
                GroupRow("JPN", flagOf("JPN"), pj = 3, g = 0, e = 0, p = 3, gf = 1, gc = 5, pts = 0),
            )
        ),
        Group(
            "Grupo B", listOf(
                GroupRow("ARG", flagOf("ARG"), pj = 3, g = 3, e = 0, p = 0, gf = 7, gc = 1, pts = 9),
                GroupRow("SEN", flagOf("SEN"), pj = 3, g = 1, e = 1, p = 1, gf = 4, gc = 4, pts = 4),
                GroupRow("CRO", flagOf("CRO"), pj = 3, g = 1, e = 0, p = 2, gf = 3, gc = 5, pts = 3),
                GroupRow("CAN", flagOf("CAN"), pj = 3, g = 0, e = 1, p = 2, gf = 2, gc = 6, pts = 1),
            )
        ),
        Group(
            "Grupo C", listOf(
                GroupRow("FRA", flagOf("FRA"), pj = 3, g = 2, e = 1, p = 0, gf = 6, gc = 2, pts = 7),
                GroupRow("ENG", flagOf("ENG"), pj = 3, g = 2, e = 0, p = 1, gf = 5, gc = 3, pts = 6),
                GroupRow("MAR", flagOf("MAR"), pj = 3, g = 1, e = 1, p = 1, gf = 3, gc = 3, pts = 4),
                GroupRow("USA", flagOf("USA"), pj = 3, g = 0, e = 0, p = 3, gf = 1, gc = 7, pts = 0),
            )
        ),
        Group(
            "Grupo D", listOf(
                GroupRow("BRA", flagOf("BRA"), pj = 3, g = 2, e = 1, p = 0, gf = 7, gc = 3, pts = 7),
                GroupRow("ESP", flagOf("ESP"), pj = 3, g = 2, e = 0, p = 1, gf = 6, gc = 4, pts = 6),
                GroupRow("POR", flagOf("POR"), pj = 3, g = 1, e = 1, p = 1, gf = 4, gc = 4, pts = 4),
                GroupRow("GER", flagOf("GER"), pj = 3, g = 0, e = 0, p = 3, gf = 2, gc = 8, pts = 0),
            )
        ),
    )

    // ---------------------------------------------------------------------
    // Llaves (eliminatorias)
    // ---------------------------------------------------------------------
    val bracket: List<BracketRound> = listOf(
        BracketRound(
            "Octavos de final", listOf(
                BracketMatch("ARG", "MEX", flagOf("ARG"), flagOf("MEX"), 2, 1, done = true),
                BracketMatch("BRA", "ESP", flagOf("BRA"), flagOf("ESP"), 3, 1, done = true),
                BracketMatch("FRA", "GER", flagOf("FRA"), flagOf("GER"), null, null, done = false),
                BracketMatch("ENG", "POR", flagOf("ENG"), flagOf("POR"), null, null, done = false),
            )
        ),
        BracketRound(
            "Cuartos de final", listOf(
                BracketMatch("ARG", "BRA", flagOf("ARG"), flagOf("BRA"), null, null, done = false),
                BracketMatch("FRA", "ENG", flagOf("FRA"), flagOf("ENG"), null, null, done = false),
            )
        ),
        BracketRound(
            "Semifinal", listOf(
                BracketMatch("—", "—", "", "", null, null, done = false),
            )
        ),
        BracketRound(
            "Final", listOf(
                BracketMatch("—", "—", "", "", null, null, done = false),
            )
        ),
    )

    // ---------------------------------------------------------------------
    // Noticias
    // ---------------------------------------------------------------------
    val news: List<NewsItem> = listOf(
        NewsItem(
            id = "n1", tag = "LESIÓN", colorHex = 0xFFE61D25, time = "hace 12 min",
            title = "Pedri, baja de última hora ante Brasil",
            summary = "El mediocampista español sufre una molestia muscular y queda fuera del duelo de cuartos de final.",
            body = listOf(
                "El seleccionador de España confirmó que Pedri no estará disponible para el partido de cuartos de final ante Brasil tras sentir una molestia en el isquiotibial durante el último entrenamiento.",
                "El cuerpo médico prefiere no arriesgar al jugador pensando en una posible semifinal. En su lugar entraría un mediocampista de corte más físico, lo que podría modificar el plan de presión alta del equipo.",
                "La baja obliga a repensar la salida de balón, una de las grandes fortalezas del conjunto en lo que va del torneo.",
            )
        ),
        NewsItem(
            id = "n2", tag = "SORPRESA", colorHex = 0xFF3CAC3B, time = "hace 1 h",
            title = "Marruecos elimina a Bélgica en los penales",
            summary = "Los africanos vuelven a dar la campanada y se meten entre los ocho mejores tras una tanda dramática.",
            body = listOf(
                "Marruecos volvió a ser la gran sorpresa del Mundial al eliminar a Bélgica en una tanda de penales cargada de tensión, con una actuación estelar de su portero, figura del partido.",
                "El equipo africano sostuvo el orden defensivo durante todo el encuentro y llevó el duelo a los penales pese a la diferencia de nombres sobre el papel.",
                "Con este resultado, Marruecos confirma que su histórica semifinal anterior no fue casualidad y se ilusiona con repetir gesta.",
            )
        ),
        NewsItem(
            id = "n3", tag = "TÁCTICA", colorHex = 0xFF6B5BD2, time = "hace 2 h",
            title = "El cambio de Scaloni que destrabó el partido",
            summary = "El ingreso de Garnacho cambió la dinámica y Argentina se llevó tres puntos vitales.",
            body = listOf(
                "Con el partido trabado, el entrenador de Argentina apostó por el ingreso de un extremo de profundidad para atacar la espalda de los laterales rivales.",
                "La jugada le salió redonda: pocos minutos después llegó el gol que destrabó el encuentro y le dio aire a un equipo que sufría para generar.",
                "El movimiento confirma la lectura de partido del cuerpo técnico, una de las claves de la campaña.",
            )
        ),
        NewsItem(
            id = "n4", tag = "RÉCORD", colorHex = 0xFF474A4A, time = "hace 3 h",
            title = "Mbappé llega a 50 goles con Francia",
            summary = "El capitán francés se acerca a la cima de los goleadores históricos de su selección.",
            body = listOf(
                "Kylian Mbappé alcanzó los 50 goles con la camiseta de Francia y se consolida como una de las grandes referencias ofensivas del torneo.",
                "El delantero, capitán y líder del proyecto, sigue escalando posiciones en la tabla histórica de goleadores de su país a un ritmo notable para su edad.",
                "Su estado de forma es una de las principales razones por las que Francia aparece entre los grandes favoritos.",
            )
        ),
        NewsItem(
            id = "n5", tag = "VAR", colorHex = 0xFF2A398D, time = "hace 4 h",
            title = "Polémica en el Azteca por un penal anulado",
            summary = "La acción del minuto 70 dejó reclamos en el banquillo local tras la revisión del videoarbitraje.",
            body = listOf(
                "Una acción dentro del área en el minuto 70 generó una fuerte polémica luego de que el árbitro, tras la revisión en el monitor, decidiera anular el penal señalado inicialmente.",
                "El banquillo local reclamó con vehemencia la decisión, que cambió el rumbo emocional del partido en un momento clave.",
                "El debate sobre los criterios del videoarbitraje vuelve a instalarse en el centro de la conversación del Mundial.",
            )
        ),
    )
}
