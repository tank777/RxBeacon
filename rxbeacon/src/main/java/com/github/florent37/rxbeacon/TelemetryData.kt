package com.github.florent37.rxbeacon

data class TelemetryData(

    /**
     * A numeric version of the version of the telemetry format.
     * This is currently always 0, as this is the first version of the telemetry format
     */
    val version: Long = 0,

    /**
     * A two byte indicator of the voltage of the battery on the beacon.
     * If the beacon does not have a battery (e.g. a USB powered beacon), this field is set to zero
     */
    val batteryMilliVolts: Long = 0,

    /**
     * A two byte field indicating the output of a temperature sensor on the beacon, if supported by the hardware.
     * Note, however, that beacon temperature sensors are often pretty inaccurate, and can be influenced by heating of adjacent electronic components.
     */
    val temperature: Float = 0F,

    /**
     * A count of how many advertising packets have been transmitted by the beacon since it was last powered on
     */
    val pduCount: Long = 0,

    /**
     * A four byte measurement of how many seconds the beacon has been powered.
     * Since most beacons are based on low-power hardware that do not contain
     */
    val uptime: Long = 0
) {

    companion object {
        fun getTemperatureFromTlmField(tmp: Float): Float {
            val ret = tmp / 256F

            if (ret == (1 shl 7).toFloat()) { // 0x8000
                return 0F
            }
            return if (ret > (1 shl 7)) ret - (1 shl 8) else ret
        }
    }
}

