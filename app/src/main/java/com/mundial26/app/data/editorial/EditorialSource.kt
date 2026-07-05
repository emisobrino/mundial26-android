package com.mundial26.app.data.editorial

import com.mundial26.app.domain.model.*

/** Datos editoriales curados (lo que ninguna API gratis da), keyed por código FIFA (TLA). */
data class TeamEditorial(
    val conf: Confederation, val rank: Int, val tag: String, val styleLabel: String,
    val star: Star, val pros: List<String>, val cons: List<String>
)

object EditorialSource {
    const val featuredCode = "ARG"   // MockData.featuredTeamId == "arg" -> code "ARG"

    val byCode: Map<String, TeamEditorial> = mapOf(
        "ARG" to TeamEditorial(
            Confederation.CONMEBOL, 1, "Campeón defensor", "Posesión vertical",
            Star(10, "Lionel Messi", "Delantero / Capitán",
                "Cerebro creativo del equipo: maneja los tiempos y aparece en los momentos decisivos.",
                StarStats(6, 4, 3, 8.4, "Inter Miami", 38, "€20M")),
            listOf("Jerarquía y experiencia en partidos clave", "Mediocampo equilibrado y muy trabajador", "Definición letal de sus delanteros"),
            listOf("Defensa con poca velocidad", "Dependencia de Messi en la creación", "Plantel con varios veteranos")
        ),
        "FRA" to TeamEditorial(
            Confederation.UEFA, 2, "Candidato al título", "Transiciones rápidas",
            Star(10, "Kylian Mbappé", "Delantero",
                "Velocidad y gol desequilibrantes: capaz de decidir un partido en una sola jugada.",
                StarStats(6, 5, 2, 8.7, "Real Madrid", 27, "€180M")),
            listOf("Velocidad explosiva en ataque", "Profundidad de plantel enorme", "Solidez física en todas las líneas"),
            listOf("Altibajos de concentración defensiva", "Gestión de egos en el vestuario", "Arranques de partido lentos")
        ),
        "BRA" to TeamEditorial(
            Confederation.CONMEBOL, 3, "Candidato al título", "Ataque por bandas",
            Star(7, "Vinícius Jr.", "Extremo",
                "Desborde y uno contra uno: el principal arma de profundidad por la izquierda.",
                StarStats(6, 3, 4, 8.2, "Real Madrid", 25, "€150M")),
            listOf("Talento individual desbordante", "Peligro constante por las bandas", "Generación joven en ascenso"),
            listOf("Fragilidad defensiva ante contras", "Falta de un \"9\" fijo y fiable", "Presión histórica por el resultado")
        ),
        "ESP" to TeamEditorial(
            Confederation.UEFA, 5, "Candidato al título", "Posesión y presión alta",
            Star(19, "Lamine Yamal", "Extremo",
                "Regate y visión sorprendentes para su edad: el gran desequilibrante del proyecto."),
            listOf("Control del balón y del ritmo", "Presión alta muy coordinada", "Recambio joven de altísimo nivel"),
            listOf("Falta de un goleador nato", "Vulnerable a la contra rápida", "Poca experiencia en fases finales")
        ),
        "ENG" to TeamEditorial(
            Confederation.UEFA, 4, "Candidato al título", "Juego directo estructurado",
            Star(10, "Jude Bellingham", "Mediocampista",
                "Llegada y gol desde el medio: combina físico, técnica y jerarquía pese a su edad."),
            listOf("Gran talento ofensivo individual", "Fortaleza en el juego aéreo", "Profundidad en todas las posiciones"),
            listOf("Planteamientos conservadores", "Dudas bajo presión en penales", "Falta de un sistema definido")
        ),
        "MEX" to TeamEditorial(
            Confederation.CONCACAF, 14, "Selección anfitriona", "Intensidad y orden",
            Star(9, "Santiago Giménez", "Delantero",
                "Olfato goleador dentro del área: vive de la última jugada y aprovecha cada ocasión."),
            listOf("Empuje del público local", "Buen estado físico y presión", "Portería confiable"),
            listOf("Poca pegada ante rivales top", "Historial corto en fases decisivas", "Defensa expuesta a la velocidad")
        ),
        "POR" to TeamEditorial(
            Confederation.UEFA, 6, "Candidato al título", "Ataque asociativo",
            Star(10, "Rafael Leão", "Extremo",
                "Desequilibrio y potencia: cuando arranca a velocidad es casi imparable por su carril."),
            listOf("Generación dorada en el mediocampo", "Variedad de soluciones ofensivas", "Experiencia europea de élite"),
            listOf("Transición generacional en defensa", "Dependencia del estado anímico", "Gestión de minutos de veteranos")
        ),
        "GER" to TeamEditorial(
            Confederation.UEFA, 10, "Candidato al título", "Posesión moderna",
            Star(10, "Jamal Musiala", "Mediapunta",
                "Conducción y desequilibrio en espacios reducidos: el motor creativo de la nueva era."),
            listOf("Mediocampo creativo y joven", "Mentalidad competitiva histórica", "Variantes tácticas según el rival"),
            listOf("Reconstrucción defensiva reciente", "Irregularidad de resultados", "Falta de un \"9\" de garantías")
        ),
        "NED" to TeamEditorial(
            Confederation.UEFA, 7, "Candidato europeo", "Posesión y orden",
            Star(4, "Virgil van Dijk", "Defensa central",
                "Liderazgo y salida limpia desde el fondo: el ancla del equipo."),
            listOf("Defensa jerárquica y ordenada", "Salida de balón cuidada", "Experiencia en torneos grandes"),
            listOf("Falta de un extremo desequilibrante", "Mediocampo a veces lento", "Recambio ofensivo limitado")
        ),
        "CRO" to TeamEditorial(
            Confederation.UEFA, 9, "Veteranía de oro", "Control de mediocampo",
            Star(10, "Luka Modrić", "Mediocampista",
                "Pausa, último pase y jerarquía pese a la edad."),
            listOf("Mediocampo de gran calidad", "Sabe competir partidos cerrados", "Mentalidad de torneo"),
            listOf("Plantel envejecido", "Poca velocidad en defensa", "Dependencia de sus referentes")
        ),
        "URU" to TeamEditorial(
            Confederation.CONMEBOL, 11, "Garra y juventud", "Intensidad y transición",
            Star(9, "Darwin Núñez", "Delantero",
                "Potencia y profundidad constante en ataque."),
            listOf("Mezcla de juventud y experiencia", "Intensidad y presión alta", "Peligro a balón parado"),
            listOf("Definición irregular", "Indisciplina táctica por momentos", "Laterales expuestos")
        ),
        "JPN" to TeamEditorial(
            Confederation.OTHER, 17, "Sorpresa asiática", "Presión y velocidad",
            Star(8, "Takefusa Kubo", "Extremo",
                "Regate y conducción para romper líneas."),
            listOf("Colectivo muy trabajado", "Velocidad por las bandas", "Disciplina táctica"),
            listOf("Falta de físico en los duelos", "Poca pegada ante rivales top", "Inexperiencia en fases finales")
        ),
        "USA" to TeamEditorial(
            Confederation.CONCACAF, 13, "Coanfitrión", "Energía y transición",
            Star(10, "Christian Pulisic", "Extremo",
                "Desequilibrio y gol: la cara de la selección local."),
            listOf("Empuje de la localía", "Plantel joven curtido en Europa", "Buen estado físico"),
            listOf("Falta de un \"9\" fiable", "Defensa irregular", "Poca experiencia ganadora")
        ),
        "CAN" to TeamEditorial(
            Confederation.CONCACAF, 30, "Coanfitrión revelación", "Velocidad vertical",
            Star(19, "Alphonso Davies", "Lateral / Extremo",
                "Velocidad descomunal por el carril izquierdo."),
            listOf("Velocidad letal en transición", "Generación joven en alza", "Juega sin presión de favoritismo"),
            listOf("Inexperiencia en Mundiales", "Definición pobre", "Defensa frágil")
        ),
        "MAR" to TeamEditorial(
            Confederation.OTHER, 12, "Semifinalista histórico", "Bloque y contra",
            Star(2, "Achraf Hakimi", "Lateral",
                "Despliegue total por la derecha: defiende y ataca."),
            listOf("Solidez defensiva colectiva", "Contraataque letal", "Fuerte espíritu de equipo"),
            listOf("Generación ofensiva corta", "Depende del orden defensivo", "Pocos goles desde el medio")
        ),
        "SEN" to TeamEditorial(
            Confederation.OTHER, 18, "Potencia africana", "Físico y verticalidad",
            Star(9, "Nicolas Jackson", "Delantero",
                "Potencia y movilidad para atacar la espalda rival."),
            listOf("Físico superior en los duelos", "Velocidad arriba", "Portería confiable"),
            listOf("Imprecisión en el último pase", "Altibajos de concentración", "Dependencia del físico")
        ),
        "BEL" to TeamEditorial(
            Confederation.UEFA, 8, "", "Generación experimentada",
            Star(7, "Kevin De Bruyne", "Mediocampista", "Visión y último pase de élite: dirige el ataque del equipo.",
                StarStats(0, 0, 0, 8.3, "Napoli", 34, "€20M")),
            listOf("Mediocampo de jerarquía", "Pegada de media distancia", "Experiencia de élite"),
            listOf("Plantel veterano", "Defensa en reconstrucción", "Dependencia de De Bruyne")
        ),
        "COL" to TeamEditorial(
            Confederation.CONMEBOL, 12, "", "Talento ofensivo",
            Star(7, "Luis Díaz", "Extremo", "Desborde y gol por izquierda: el gran desequilibrante cafetero.",
                StarStats(0, 0, 0, 8.0, "Liverpool", 29, "€80M")),
            listOf("Talento individual arriba", "Buen pie a balón parado", "Mezcla de juventud y veteranía"),
            listOf("Irregularidad defensiva", "Dependencia de Díaz", "Altibajos de visitante")
        ),
        "NOR" to TeamEditorial(
            Confederation.UEFA, 28, "", "Vértigo y potencia",
            Star(9, "Erling Haaland", "Delantero", "Goleador implacable: cada ocasión dentro del área es peligro de gol.",
                StarStats(0, 0, 0, 8.6, "Manchester City", 25, "€180M")),
            listOf("Goleador de talla mundial", "Potencia física arriba", "Juego directo eficaz"),
            listOf("Poco rodaje mundialista", "Mediocampo corto", "Dependencia total de Haaland")
        ),
        "EGY" to TeamEditorial(
            Confederation.CAF, 33, "", "Dependencia de Salah",
            Star(10, "Mohamed Salah", "Delantero", "Desborde, gol y liderazgo: la referencia absoluta del equipo.",
                StarStats(0, 0, 0, 8.4, "Liverpool", 33, "€35M")),
            listOf("Estrella de nivel mundial", "Peligro en transición", "Experiencia europea"),
            listOf("Excesiva dependencia de Salah", "Poca pegada secundaria", "Defensa irregular")
        ),
        "KOR" to TeamEditorial(
            Confederation.AFC, 23, "", "Velocidad y presión",
            Star(7, "Son Heung-min", "Delantero / Capitán", "Velocidad y zurda letal: lidera dentro y fuera de la cancha.",
                StarStats(0, 0, 0, 8.2, "Tottenham", 33, "€25M")),
            listOf("Estrella desequilibrante", "Colectivo intenso", "Velocidad en ataque"),
            listOf("Físico inferior en duelos", "Dependencia de Son", "Defensa expuesta")
        ),
        "IRN" to TeamEditorial(
            Confederation.AFC, 20, "", "Bloque y contra",
            Star(9, "Mehdi Taremi", "Delantero", "Movilidad y olfato: fija centrales y aparece en el área.",
                StarStats(0, 0, 0, 7.6, "Inter", 33, "€10M")),
            listOf("Bloque defensivo sólido", "Peligro a la contra", "Experiencia del grupo"),
            listOf("Juego ofensivo limitado", "Dependencia de Taremi", "Poca creación")
        ),
        "SUI" to TeamEditorial(
            Confederation.UEFA, 19, "", "Orden y oficio",
            Star(10, "Granit Xhaka", "Mediocampista / Capitán", "Equilibrio y distribución: ordena el juego desde el medio.",
                StarStats(0, 0, 0, 7.8, "Bayer Leverkusen", 33, "€15M")),
            listOf("Bloque ordenado y compacto", "Experiencia en torneos", "Liderazgo en el medio"),
            listOf("Falta de un goleador fijo", "Plantel veterano", "Poca pólvora ofensiva")
        ),
        "ECU" to TeamEditorial(
            Confederation.CONMEBOL, 31, "", "Físico y juventud",
            Star(23, "Moisés Caicedo", "Mediocampista", "Recuperación y despliegue: el motor del mediocampo.",
                StarStats(0, 0, 0, 8.0, "Chelsea", 24, "€90M")),
            listOf("Mediocampo físico y joven", "Solidez defensiva", "Buen estado físico"),
            listOf("Pegada escasa", "Inexperiencia en fases finales", "Falta de un \"9\"")
        ),
        "AUT" to TeamEditorial(
            Confederation.UEFA, 25, "", "Presión intensa",
            Star(8, "David Alaba", "Defensa / Capitán", "Salida limpia y liderazgo: organiza al equipo desde el fondo.",
                StarStats(0, 0, 0, 7.9, "Real Madrid", 33, "€12M")),
            listOf("Presión alta agresiva", "Colectivo muy trabajado", "Versatilidad táctica"),
            listOf("Falta de un \"9\" top", "Dependencia de Alaba", "Recambio corto")
        ),
        "SWE" to TeamEditorial(
            Confederation.UEFA, 24, "", "Potencia y pelota parada",
            Star(11, "Alexander Isak", "Delantero", "Técnica y gol: delantero moderno y desequilibrante.",
                StarStats(0, 0, 0, 8.1, "Newcastle", 26, "€90M")),
            listOf("Goleador de nivel", "Juego aéreo", "Físico arriba"),
            listOf("Mediocampo corto", "Dependencia de Isak", "Poca creación")
        ),
        "TUR" to TeamEditorial(
            Confederation.UEFA, 26, "", "Talento joven y pegada",
            Star(10, "Hakan Çalhanoğlu", "Mediocampista", "Distribución y pegada: organiza y define de lejos.",
                StarStats(0, 0, 0, 8.0, "Inter", 32, "€30M")),
            listOf("Mediocampo talentoso", "Pegada de media distancia", "Generación joven"),
            listOf("Irregularidad", "Defensa frágil", "Altibajos de concentración")
        ),
        "CZE" to TeamEditorial(
            Confederation.UEFA, 36, "", "Orden y pelota parada",
            Star(9, "Patrik Schick", "Delantero", "Olfato de área y buen remate de cabeza.",
                StarStats(0, 0, 0, 7.6, "Bayer Leverkusen", 30, "€25M")),
            listOf("Peligro a balón parado", "Bloque ordenado", "Goleador confiable"),
            listOf("Creación limitada", "Dependencia de Schick", "Poca velocidad")
        ),
        "SCO" to TeamEditorial(
            Confederation.UEFA, 39, "", "Intensidad británica",
            Star(4, "Scott McTominay", "Mediocampista", "Llegada y gol desde el medio: pulmón del equipo.",
                StarStats(0, 0, 0, 7.7, "Napoli", 29, "€30M")),
            listOf("Intensidad y físico", "Peligro a pelota parada", "Fuerte espíritu de equipo"),
            listOf("Falta de un \"9\" top", "Creación limitada", "Pegada escasa")
        ),
        "AUS" to TeamEditorial(
            Confederation.AFC, 26, "", "Físico y entrega",
            Star(1, "Mathew Ryan", "Arquero / Capitán", "Seguridad bajo los tres palos: sostiene al equipo en los duelos.",
                StarStats(0, 0, 0, 7.2, "Roma", 34, "€2M")),
            listOf("Entrega física total", "Orden defensivo", "Fortaleza en el juego aéreo"),
            listOf("Poca generación ofensiva", "Lentitud en la salida", "Pegada limitada")
        ),
        "QAT" to TeamEditorial(
            Confederation.AFC, 35, "", "Oficio y pelota parada",
            Star(11, "Akram Afif", "Extremo", "Regate y asistencia: el faro ofensivo qatarí.",
                StarStats(0, 0, 0, 7.3, "Al-Sadd", 29, "€8M")),
            listOf("Colectivo muy trabajado", "Peligro a balón parado", "Talento de Afif"),
            listOf("Físico inferior en duelos", "Pegada limitada", "Inexperiencia ante rivales top")
        ),
        "ALG" to TeamEditorial(
            Confederation.CAF, 38, "", "Talento técnico",
            Star(7, "Riyad Mahrez", "Extremo", "Regate y zurda exquisita: crea peligro desde la derecha.",
                StarStats(0, 0, 0, 7.7, "Al-Ahli", 34, "€12M")),
            listOf("Talento individual arriba", "Buen pie a balón parado", "Velocidad por bandas"),
            listOf("Irregularidad colectiva", "Defensa frágil", "Dependencia de Mahrez")
        ),
        "CIV" to TeamEditorial(
            Confederation.CAF, 40, "", "Potencia africana",
            Star(8, "Franck Kessié", "Mediocampista", "Despliegue box-to-box: recupera y llega al área.",
                StarStats(0, 0, 0, 7.6, "Al-Ahli", 28, "€18M")),
            listOf("Físico en el medio", "Buen juego aéreo", "Plantel en su pico"),
            listOf("Creación limitada", "Definición irregular", "Altibajos de concentración")
        ),
        "TUN" to TeamEditorial(
            Confederation.CAF, 41, "", "Bloque y orden",
            Star(7, "Hannibal Mejbri", "Mediocampista", "Energía y conducción: el motor creativo tunecino.",
                StarStats(0, 0, 0, 7.0, "Burnley", 23, "€8M")),
            listOf("Bloque compacto", "Entrega física", "Peligro a pelota parada"),
            listOf("Pegada limitada", "Creación escasa", "Dependencia del orden colectivo")
        ),
        "PAN" to TeamEditorial(
            Confederation.CONCACAF, 41, "", "Intensidad y orden",
            Star(6, "Adalberto Carrasquilla", "Mediocampista", "Pausa y conducción: el cerebro del medio canalero.",
                StarStats(0, 0, 0, 7.1, "Houston Dynamo", 26, "€5M")),
            listOf("Bloque compacto", "Entrega física", "Peligro a balón parado"),
            listOf("Pegada limitada", "Inexperiencia en Mundiales", "Defensa expuesta a la velocidad")
        ),
        "PAR" to TeamEditorial(
            Confederation.CONMEBOL, 50, "", "Garra y orden",
            Star(10, "Miguel Almirón", "Mediocampista", "Velocidad y llegada desde el medio.",
                StarStats(0, 0, 0, 7.2, "Atlanta United", 31, "€10M")),
            listOf("Bloque sólido", "Intensidad y presión", "Peligro a pelota parada"),
            listOf("Pegada escasa", "Creación limitada", "Altibajos de visitante")
        ),
        "KSA" to TeamEditorial(
            Confederation.AFC, 56, "", "Orden y apoyo regional",
            Star(10, "Salem Al-Dawsari", "Extremo", "Zurda talentosa y gol: la referencia ofensiva saudí.",
                StarStats(0, 0, 0, 7.2, "Al-Hilal", 34, "€8M")),
            listOf("Bloque compacto", "Buen pie a balón parado", "Apoyo regional"),
            listOf("Físico inferior", "Pegada limitada", "Inexperiencia ante rivales top")
        ),
        "COD" to TeamEditorial(
            Confederation.CAF, 56, "", "Físico y velocidad",
            Star(11, "Cédric Bakambu", "Delantero", "Movilidad y olfato para atacar la espalda rival.",
                StarStats(0, 0, 0, 7.0, "Real Betis", 34, "€3M")),
            listOf("Velocidad arriba", "Físico en los duelos", "Espíritu colectivo"),
            listOf("Inexperiencia mundialista", "Creación escasa", "Defensa floja")
        ),
        "UZB" to TeamEditorial(
            Confederation.AFC, 57, "", "Debut físico",
            Star(9, "Eldor Shomurodov", "Delantero", "Potencia y movilidad como referencia de área.",
                StarStats(0, 0, 0, 7.0, "Roma", 31, "€6M")),
            listOf("Físico en ataque", "Bloque ordenado", "Juega sin presión"),
            listOf("Debutante absoluto", "Creación limitada", "Inexperiencia")
        ),
        "IRQ" to TeamEditorial(
            Confederation.AFC, 58, "", "Intensidad y orden",
            Star(10, "Aymen Hussein", "Delantero", "Físico y remate: la referencia ofensiva del equipo.",
                StarStats(0, 0, 0, 6.9, "Al-Qadisiyah", 29, "€2M")),
            listOf("Entrega física", "Orden defensivo", "Peligro a pelota parada"),
            listOf("Creación pobre", "Inexperiencia mundialista", "Pegada limitada")
        ),
        "RSA" to TeamEditorial(
            Confederation.CAF, 60, "", "Velocidad y juventud",
            Star(10, "Percy Tau", "Delantero", "Movilidad y desborde para generar peligro.",
                StarStats(0, 0, 0, 7.0, "Qatar SC", 31, "€3M")),
            listOf("Velocidad arriba", "Juventud en alza", "Espíritu colectivo"),
            listOf("Inexperiencia", "Definición irregular", "Defensa floja")
        ),
        "JOR" to TeamEditorial(
            Confederation.AFC, 62, "", "Sorpresa asiática",
            Star(7, "Mousa Al-Tamari", "Extremo", "Velocidad y desborde: el desequilibrante por banda.",
                StarStats(0, 0, 0, 7.2, "Montpellier", 28, "€6M")),
            listOf("Velocidad por bandas", "Bloque trabajado", "Juega sin presión"),
            listOf("Plantel modesto", "Debutante en la cita", "Definición irregular")
        ),
        "CPV" to TeamEditorial(
            Confederation.CAF, 65, "", "Sorpresa isleña",
            Star(10, "Ryan Mendes", "Delantero", "Experiencia y liderazgo ofensivo del proyecto.",
                StarStats(0, 0, 0, 6.8, "Libre", 35, "€1M")),
            listOf("Bloque unido", "Sin presión de favorito", "Orden defensivo"),
            listOf("Plantel modesto", "Poca pegada", "Debutante absoluto")
        ),
        "GHA" to TeamEditorial(
            Confederation.CAF, 68, "", "Velocidad y talento joven",
            Star(20, "Mohammed Kudus", "Mediapunta", "Conducción y desequilibrio en espacios reducidos.",
                StarStats(0, 0, 0, 7.8, "Tottenham", 25, "€60M")),
            listOf("Talento joven en alza", "Velocidad arriba", "Desborde individual"),
            listOf("Defensa irregular", "Inconsistencia", "Dependencia de Kudus")
        ),
        "BIH" to TeamEditorial(
            Confederation.UEFA, 74, "", "Físico y pelota parada",
            Star(11, "Edin Džeko", "Delantero", "Referencia de área y juego aéreo pese a la edad.",
                StarStats(0, 0, 0, 7.3, "Fenerbahçe", 39, "€3M")),
            listOf("Peligro a balón parado", "Físico en ataque", "Veteranía"),
            listOf("Plantel envejecido", "Poca velocidad", "Defensa vulnerable")
        ),
        "CUW" to TeamEditorial(
            Confederation.CONCACAF, 82, "", "Revelación caribeña",
            Star(8, "Leandro Bacuna", "Mediocampista", "Experiencia europea para conducir al grupo.",
                StarStats(0, 0, 0, 6.7, "Libre", 34, "€1M")),
            listOf("Jugadores con paso por Europa", "Juega sin presión", "Sorpresa táctica"),
            listOf("Plantel limitado", "Inexperiencia mundialista", "Profundidad corta")
        ),
        "HAI" to TeamEditorial(
            Confederation.CONCACAF, 83, "", "Garra caribeña",
            Star(9, "Frantzdy Pierrot", "Delantero", "Potencia y juego de espaldas como referencia de área.",
                StarStats(0, 0, 0, 6.6, "Gaziantep", 29, "€2M")),
            listOf("Entrega total", "Físico arriba", "Juega sin presión"),
            listOf("Plantel modesto", "Debutante", "Defensa frágil")
        ),
        "NZL" to TeamEditorial(
            Confederation.OFC, 89, "", "Físico y orden",
            Star(9, "Chris Wood", "Delantero", "Referencia de área y juego aéreo dominante.",
                StarStats(0, 0, 0, 7.1, "Nottingham Forest", 34, "€6M")),
            listOf("Juego aéreo fuerte", "Entrega total", "Orden defensivo"),
            listOf("Plantel modesto", "Lentitud", "Creación escasa")
        ),
    )

    fun forCode(code: String): TeamEditorial? = byCode[code.uppercase()]

    /**
     * Override editorial de asistidores: football-data.org (free) devuelve las asistencias
     * incompletas para el WC2026, así que curamos el ranking real (fuente: panel de estadísticas
     * de Google). Se actualiza editando esta lista tras cada fecha. Vacía = usar football-data.
     */
    val topAssists: List<AssistLeader> = listOf(
        AssistLeader("Michael Olise", "FRA", 5),
        AssistLeader("Bruno Guimarães", "BRA", 4),
        AssistLeader("Alexander Isak", "SWE", 3),
        AssistLeader("Florian Wirtz", "GER", 3),
        AssistLeader("Martin Ødegaard", "NOR", 3),
        AssistLeader("Roberto Alvarado", "MEX", 3),
    )

    /**
     * Lesionados curados por selección (TLA → apellidos, sin acentos, en minúsculas).
     * Ninguna API gratis da el parte médico, así que se mantiene a mano: editá este mapa
     * tras cada fecha. El plantel marca `injured = true` si el apellido del jugador aparece acá.
     */
    val injuredByCode: Map<String, Set<String>> = mapOf(
        "ARG" to setOf("lo celso"),
        "FRA" to setOf("dembele"),
    )

    /** Apellidos lesionados de una selección (vacío si no hay datos). */
    fun injured(tla: String): Set<String> = injuredByCode[tla.uppercase()] ?: emptySet()
}

/** Fila curada del ranking de asistidores (nombre + selección TLA + asistencias). */
data class AssistLeader(val name: String, val tla: String, val assists: Int)
