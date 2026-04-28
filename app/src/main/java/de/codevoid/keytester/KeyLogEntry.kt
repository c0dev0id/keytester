package de.codevoid.keytester

data class KeyLogEntry(
    val id: Long,
    val wallTimeMs: Long,
    val source: String,       // "hw" or the broadcast action for broadcast events
    val action: String,       // "DOWN", "UP", "MULTIPLE", or raw int
    val keyCode: Int?,
    val keySymbol: String?,
    val deviceName: String?,
    val deviceClass: String?,
    val inputSource: String?,
    val durationMs: Long?,    // null when no matching down/up pair
    val rawExtras: String?    // set for unrecognised broadcast payloads
)
