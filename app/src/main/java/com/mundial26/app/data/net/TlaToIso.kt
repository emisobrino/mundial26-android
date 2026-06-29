package com.mundial26.app.data.net

import com.mundial26.app.domain.model.Confederation

/**
 * Datos de referencia de las 48 selecciones del Mundial 2026:
 * código FIFA (TLA) → bandera ISO-2 (flagcdn) + confederación.
 */
private data class TeamRef(val iso: String, val conf: Confederation)

private val TEAMS: Map<String, TeamRef> = mapOf(
    "ALG" to TeamRef("dz", Confederation.CAF),
    "ARG" to TeamRef("ar", Confederation.CONMEBOL),
    "AUS" to TeamRef("au", Confederation.AFC),
    "AUT" to TeamRef("at", Confederation.UEFA),
    "BEL" to TeamRef("be", Confederation.UEFA),
    "BIH" to TeamRef("ba", Confederation.UEFA),
    "BRA" to TeamRef("br", Confederation.CONMEBOL),
    "CAN" to TeamRef("ca", Confederation.CONCACAF),
    "CIV" to TeamRef("ci", Confederation.CAF),
    "COD" to TeamRef("cd", Confederation.CAF),
    "COL" to TeamRef("co", Confederation.CONMEBOL),
    "CPV" to TeamRef("cv", Confederation.CAF),
    "CRO" to TeamRef("hr", Confederation.UEFA),
    "CUW" to TeamRef("cw", Confederation.CONCACAF),
    "CZE" to TeamRef("cz", Confederation.UEFA),
    "ECU" to TeamRef("ec", Confederation.CONMEBOL),
    "EGY" to TeamRef("eg", Confederation.CAF),
    "ENG" to TeamRef("gb-eng", Confederation.UEFA),
    "ESP" to TeamRef("es", Confederation.UEFA),
    "FRA" to TeamRef("fr", Confederation.UEFA),
    "GER" to TeamRef("de", Confederation.UEFA),
    "GHA" to TeamRef("gh", Confederation.CAF),
    "HAI" to TeamRef("ht", Confederation.CONCACAF),
    "IRN" to TeamRef("ir", Confederation.AFC),
    "IRQ" to TeamRef("iq", Confederation.AFC),
    "JOR" to TeamRef("jo", Confederation.AFC),
    "JPN" to TeamRef("jp", Confederation.AFC),
    "KOR" to TeamRef("kr", Confederation.AFC),
    "KSA" to TeamRef("sa", Confederation.AFC),
    "MAR" to TeamRef("ma", Confederation.CAF),
    "MEX" to TeamRef("mx", Confederation.CONCACAF),
    "NED" to TeamRef("nl", Confederation.UEFA),
    "NOR" to TeamRef("no", Confederation.UEFA),
    "NZL" to TeamRef("nz", Confederation.OFC),
    "PAN" to TeamRef("pa", Confederation.CONCACAF),
    "PAR" to TeamRef("py", Confederation.CONMEBOL),
    "POR" to TeamRef("pt", Confederation.UEFA),
    "QAT" to TeamRef("qa", Confederation.AFC),
    "RSA" to TeamRef("za", Confederation.CAF),
    "SCO" to TeamRef("gb-sct", Confederation.UEFA),
    "SEN" to TeamRef("sn", Confederation.CAF),
    "SUI" to TeamRef("ch", Confederation.UEFA),
    "SWE" to TeamRef("se", Confederation.UEFA),
    "TUN" to TeamRef("tn", Confederation.CAF),
    "TUR" to TeamRef("tr", Confederation.UEFA),
    "URU" to TeamRef("uy", Confederation.CONMEBOL),
    "USA" to TeamRef("us", Confederation.CONCACAF),
    "UZB" to TeamRef("uz", Confederation.AFC),
)

/** FIFA TLA (3 letras) → código ISO-2 para flagcdn. Fallback: minúscula. */
fun tlaToIso(tla: String): String = TEAMS[tla.uppercase()]?.iso ?: tla.lowercase()

/** FIFA TLA → confederación real. Fallback: OTHER. */
fun confOf(tla: String): Confederation = TEAMS[tla.uppercase()]?.conf ?: Confederation.OTHER
