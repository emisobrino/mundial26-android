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
    )

    fun forCode(code: String): TeamEditorial? = byCode[code.uppercase()]
}
