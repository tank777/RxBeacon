package com.github.florent37.rxbeacon;

import org.altbeacon.beacon.Region;

import java.util.Collection;

/**
 * Created by florentchampigny on 28/04/2017.
 */

public class RxBeaconRange {
    private final Collection<BeaconSaved> beacons;
    private final Region region;

    public RxBeaconRange(Collection<BeaconSaved> beacons, Region region) {
        this.beacons = beacons;
        this.region = region;
    }

    public Collection<BeaconSaved> getBeacons() {
        return beacons;
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public String toString() {
        return "RxBeaconRange{" +
                "beacons=" + beacons +
                ", region=" + region +
                '}';
    }
}
