package com.mundial26.app.domain.model

/**
 * Referencia estable de selecciones: mapea el código (ARG, MEX…) a su nombre completo
 * y su código ISO-2 para banderas (flagcdn). Es dato de referencia del dominio —no
 * contenido de muestra— por lo que vive acá y no en la capa de datos. Reutilizable al
 * integrar la API real (v3) para resolver nombre/bandera por código.
 */
object Nations {
    data class Nation(val code: String, val name: String, val iso: String)

    val all: Map<String, Nation> = listOf(
        Nation("ALG", "Argelia", "dz"),
        Nation("ARG", "Argentina", "ar"),
        Nation("AUS", "Australia", "au"),
        Nation("AUT", "Austria", "at"),
        Nation("BEL", "Bélgica", "be"),
        Nation("BIH", "Bosnia y Herzegovina", "ba"),
        Nation("BRA", "Brasil", "br"),
        Nation("CAN", "Canadá", "ca"),
        Nation("CIV", "Costa de Marfil", "ci"),
        Nation("COD", "RD del Congo", "cd"),
        Nation("COL", "Colombia", "co"),
        Nation("CPV", "Cabo Verde", "cv"),
        Nation("CRO", "Croacia", "hr"),
        Nation("CUW", "Curazao", "cw"),
        Nation("CZE", "Chequia", "cz"),
        Nation("ECU", "Ecuador", "ec"),
        Nation("EGY", "Egipto", "eg"),
        Nation("ENG", "Inglaterra", "gb-eng"),
        Nation("ESP", "España", "es"),
        Nation("FRA", "Francia", "fr"),
        Nation("GER", "Alemania", "de"),
        Nation("GHA", "Ghana", "gh"),
        Nation("HAI", "Haití", "ht"),
        Nation("IRN", "Irán", "ir"),
        Nation("IRQ", "Irak", "iq"),
        Nation("JOR", "Jordania", "jo"),
        Nation("JPN", "Japón", "jp"),
        Nation("KOR", "Corea del Sur", "kr"),
        Nation("KSA", "Arabia Saudita", "sa"),
        Nation("MAR", "Marruecos", "ma"),
        Nation("MEX", "México", "mx"),
        Nation("NED", "Países Bajos", "nl"),
        Nation("NOR", "Noruega", "no"),
        Nation("NZL", "Nueva Zelanda", "nz"),
        Nation("PAN", "Panamá", "pa"),
        Nation("PAR", "Paraguay", "py"),
        Nation("POR", "Portugal", "pt"),
        Nation("QAT", "Catar", "qa"),
        Nation("RSA", "Sudáfrica", "za"),
        Nation("SCO", "Escocia", "gb-sct"),
        Nation("SEN", "Senegal", "sn"),
        Nation("SUI", "Suiza", "ch"),
        Nation("SWE", "Suecia", "se"),
        Nation("TUN", "Túnez", "tn"),
        Nation("TUR", "Turquía", "tr"),
        Nation("URU", "Uruguay", "uy"),
        Nation("USA", "Estados Unidos", "us"),
        Nation("UZB", "Uzbekistán", "uz"),
    ).associateBy { it.code }

    /** Nombre completo del país por código; si no se conoce, devuelve el propio código. */
    fun name(code: String): String = all[code]?.name ?: code

    /** Código ISO-2 (flagcdn) por código de selección; fallback al código en minúscula. */
    fun iso(code: String): String = all[code]?.iso ?: code.lowercase()
}
