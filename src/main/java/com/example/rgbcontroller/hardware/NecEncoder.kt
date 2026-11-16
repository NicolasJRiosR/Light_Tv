package com.example.rgbcontroller.hardware

/** Builds a raw NEC-family waveform for Android ConsumerIrManager. */
object NecEncoder {
    const val CARRIER_HZ = 38_000

    fun encode(address: Int, command: Int, extendedAddress: Boolean): IntArray {
        require(address in 0..0xFFFF) { "La dirección debe tener 16 bits." }
        require(command in 0..0xFF) { "El comando debe tener 8 bits." }

        val addressLow = address and 0xFF
        val addressHighOrInverse = if (extendedAddress) (address shr 8) and 0xFF else addressLow xor 0xFF
        val bytes = intArrayOf(addressLow, addressHighOrInverse, command, command xor 0xFF)
        val pattern = ArrayList<Int>(67)
        pattern += 9_000
        pattern += 4_500
        bytes.forEach { byte ->
            for (bit in 0 until 8) {
                pattern += 560
                pattern += if (((byte shr bit) and 1) == 1) 1_690 else 560
            }
        }
        // A NEC frame ends with a mark, so a valid alternating pattern has an odd length.
        pattern += 560
        return pattern.toIntArray()
    }
}
