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
        Nation("ARG", "Argentina", "ar"),
        Nation("FRA", "Francia", "fr"),
        Nation("BRA", "Brasil", "br"),
        Nation("ESP", "España", "es"),
        Nation("ENG", "Inglaterra", "gb-eng"),
        Nation("MEX", "México", "mx"),
        Nation("POR", "Portugal", "pt"),
        Nation("GER", "Alemania", "de"),
        Nation("NED", "Países Bajos", "nl"),
        Nation("URU", "Uruguay", "uy"),
        Nation("JPN", "Japón", "jp"),
        Nation("SEN", "Senegal", "sn"),
        Nation("CRO", "Croacia", "hr"),
        Nation("CAN", "Canadá", "ca"),
        Nation("MAR", "Marruecos", "ma"),
        Nation("USA", "Estados Unidos", "us"),
    ).associateBy { it.code }

    /** Nombre completo del país por código; si no se conoce, devuelve el propio código. */
    fun name(code: String): String = all[code]?.name ?: code

    /** Código ISO-2 (flagcdn) por código de selección; fallback al código en minúscula. */
    fun iso(code: String): String = all[code]?.iso ?: code.lowercase()
}
