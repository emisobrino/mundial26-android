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
