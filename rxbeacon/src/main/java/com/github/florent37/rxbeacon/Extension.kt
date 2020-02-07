@file:JvmName("Extensions")
package com.github.florent37.rxbeacon

import org.altbeacon.beacon.Beacon

fun Collection<Beacon>.mapToBeaconSave(): Collection<BeaconSaved> {
    return this.map { BeaconSaved.createFromBeacon(it) }.toList()
}