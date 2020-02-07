package com.github.florent37.rxbeacon


data class RuuviData(
        /**
         * The air humidity in % age
         */
        val humidity: Int = 0,

        /**
         * The airPressure in hPa
         */
        val airPressure: Int = 0,

        /**
         * The temperature in C°
         */
        val temperatue: Int = 0
)