package com.example.rgbcontroller.domain

enum class RemoteAction(val label: String) {
    POWER_TOGGLE("Encender / apagar"), POWER_ON("Encender"), POWER_OFF("Apagar"),
    BRIGHTER("Brillo +"), DARKER("Brillo −"),
    RED("Rojo"), REDDISH("Coral"), ORANGE("Naranja"), YELLOWISH("Ámbar"), YELLOW("Amarillo"),
    YELLOW_ALT("Amarillo · otro código"),
    GREEN("Verde"), GREENISH("Verde lima"), LIGHT_GREEN("Verde claro"),
    TURQUOISE("Turquesa"), CYAN("Cian"), AQUA("Aqua"),
    BLUE("Azul"), DEEPBLUE("Azul profundo"), PURPLE("Morado"), MAGENTA("Magenta"), PINK("Rosa"), PINK_ALT("Rosa · otro código"), WHITE("Blanco"),
    TANGERINE("Mandarina"), SKY("Celeste"), ROSE("Rosa claro"),
    FLASH("Flash"), STROBE("Strobe"), FADE("Fade"), SMOOTH("Smooth")
}

data class RgbRemotePreset(
    val id: String,
    val name: String,
    val address: Int,
    val extendedAddress: Boolean,
    val commands: Map<RemoteAction, Int>,
    val source: String
)

/**
 * Publicly documented community code tables, offered only as test candidates.
 * Different lamps can use different addresses, commands, or protocols.
 */
object RgbRemotePresets {
    val all = listOf(
        RgbRemotePreset(
            id = "rgb21-nec-ext",
            name = "RGB + blanco · NEC extendido · 21 teclas",
            address = 0xEF00,
            extendedAddress = true,
            commands = mapOf(
                RemoteAction.POWER_OFF to 0x02,
                RemoteAction.POWER_ON to 0x03,
                RemoteAction.BRIGHTER to 0x00,
                RemoteAction.DARKER to 0x01,
                RemoteAction.RED to 0x04,
                RemoteAction.GREEN to 0x05,
                RemoteAction.BLUE to 0x06,
                RemoteAction.WHITE to 0x07,
                RemoteAction.ORANGE to 0x08,
                RemoteAction.YELLOW to 0x0C,
                RemoteAction.PURPLE to 0x0A,
                RemoteAction.CYAN to 0x0D,
                RemoteAction.PINK to 0x0E,
                // Additional published EF00 sequence candidates; original mapped colors above stay intact.
                RemoteAction.LIGHT_GREEN to 0x09,
                RemoteAction.FLASH to 0x0B,
                RemoteAction.STROBE to 0x0F,
                RemoteAction.TANGERINE to 0x10,
                RemoteAction.SKY to 0x11,
                RemoteAction.ROSE to 0x12,
                RemoteAction.FADE to 0x13,
                RemoteAction.YELLOW_ALT to 0x14,
                RemoteAction.AQUA to 0x15,
                RemoteAction.PINK_ALT to 0x16,
                RemoteAction.SMOOTH to 0x17
            ),
            source = "Base existente conservada; tonos candidatos según la secuencia NEC EF00 publicada en github.com/eleksaggr/ir-remote"
        )
    )
}
