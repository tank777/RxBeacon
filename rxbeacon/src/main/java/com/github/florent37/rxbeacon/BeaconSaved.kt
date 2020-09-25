package com.github.florent37.rxbeacon

import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor
import java.util.*

data class BeaconSaved(
        val hashcode: Int = 0,
        val beaconType: String? = null,
        val beaconAddress: String? = null, // MAC address of the bluetooth emitter
        val manufacturer: Int = 0,
        val txPower: Int = 0,
        val rssi: Int = 0,
        val distance: Double = 0.toDouble(),
        val lastSeen: Long = 0,
        val beaconName:String? = null,

        /**
     * Specialized field for every beacon type
     */
    val ibeaconData: IbeaconData? = null,

        val eddystoneUrlData: EddystoneUrlData? = null,

        val eddystoneUidData: EddystoneUidData? = null,

        val telemetryData: TelemetryData? = null,
        val ruuviData: RuuviData? = null
) {
    companion object {
        const val TYPE_EDDYSTONE_UID = "eddystone_uid"
        const val TYPE_EDDYSTONE_URL = "eddystone_url"
        const val TYPE_ALTBEACON = "altbeacon"
        const val TYPE_IBEACON = "ibeacon"
        const val TYPE_RUUVITAG = "ruuvitag"

        @JvmStatic
        fun createFromBeacon(beacon: Beacon) : BeaconSaved {
            // Common fields to every beacons
            var hashcode = beacon.hashCode()
            val lastSeen = Date().time
            val beaconAddress = beacon.bluetoothAddress
            val manufacturer = beacon.manufacturer
            val rssi = beacon.rssi
            val txPower = beacon.txPower
            val bluetoothName = beacon.bluetoothName
            val distance = if (beacon.distance.isInfinite()) {
                (-1).toDouble()
            } else {
                beacon.distance
            }

            var beaconType: String? = null
            var ibeaconData: IbeaconData? = null
            var eddystoneUrlData: EddystoneUrlData? = null
            var eddystoneUidData: EddystoneUidData? = null
            var telemetryData: TelemetryData? = null
            var ruuviData: RuuviData? = null

            if (beacon.serviceUuid == 0xFEAA) { // This is an Eddystone format

                // Do we have telemetry data?
                if (beacon.extraDataFields.size >= 5) {
                    telemetryData = TelemetryData(beacon.extraDataFields[0],
                            beacon.extraDataFields[1],
                            TelemetryData.getTemperatureFromTlmField(beacon.extraDataFields[2].toFloat()),
                            beacon.extraDataFields[3],
                            beacon.extraDataFields[4])
                }

                when (beacon.beaconTypeCode) {
                    0x00 -> { // This is a Eddystone-UID frame
                        beaconType = TYPE_EDDYSTONE_UID
                        eddystoneUidData = EddystoneUidData(beacon.id1.toString(), beacon.id2.toString())
                    }
                    0x10 -> { // This is a Eddystone-URL frame
                        beaconType = TYPE_EDDYSTONE_URL
                        val url = UrlBeaconUrlCompressor.uncompress(beacon.id1.toByteArray())
                        eddystoneUrlData = EddystoneUrlData(url)

                        if (url?.startsWith("https://ruu.vi/#") == true) { // This is a RuuviTag
                            val hash = url.split("#").get(1)

                            // We manually set the hashcode of the RuuviTag so it only appears once per address
                            hashcode = beaconAddress?.hashCode() ?: -1
                            beaconType = TYPE_RUUVITAG
                            val ruuviParser = RuuviParser(hash)

                            ruuviData = RuuviData(ruuviParser.humidity, ruuviParser.airPressure, ruuviParser.temp)
                        }
                    }
                }
            } else { // This is an iBeacon or ALTBeacon
                beaconType = if (beacon.beaconTypeCode == 0xBEAC) TYPE_ALTBEACON else TYPE_IBEACON // 0x4c000215 is iBeacon
                ibeaconData = IbeaconData(beacon.id1.toString(), beacon.id2.toString(), beacon.id3.toString())
            }

            return BeaconSaved(
                    hashcode = hashcode,
                    lastSeen = lastSeen,
                    manufacturer = manufacturer,
                    rssi = rssi,
                    txPower = txPower,
                    distance = distance,
                    beaconType = beaconType,
                    ibeaconData = ibeaconData,
                    eddystoneUrlData = eddystoneUrlData,
                    eddystoneUidData = eddystoneUidData,
                    telemetryData = telemetryData,
                    ruuviData = ruuviData,
                    beaconName = bluetoothName
            )
        }
    }
}
