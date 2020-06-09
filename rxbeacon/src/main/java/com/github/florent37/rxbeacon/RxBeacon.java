package com.github.florent37.rxbeacon;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by florentchampigny on 28/04/2017.
 */

public class RxBeacon {
    @Nullable
    BeaconConsumer beaconConsumer;
    private Context application;
    private Region region = null;
    private BeaconManager beaconManager;
    private long backgroundScanPeriod = 10000L;
    private long foregroundScanPeriod = 10000L;

    private static final String RUUVI_LAYOUT = "m:0-2=0499,i:4-19,i:20-21,i:22-23,p:24-24"; // TBD
    private static final String IBEACON_LAYOUT = "m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24";
    private static final String ALTBEACON_LAYOUT = BeaconParser.ALTBEACON_LAYOUT;
    private static final String EDDYSTONE_UID_LAYOUT = BeaconParser.EDDYSTONE_UID_LAYOUT;
    private static final String EDDYSTONE_URL_LAYOUT = BeaconParser.EDDYSTONE_URL_LAYOUT;
    private static final String EDDYSTONE_TLM_LAYOUT = BeaconParser.EDDYSTONE_TLM_LAYOUT;

    private RxBeacon(Builder builder) {
        this.application = builder.context.getApplicationContext();
        this.beaconManager = BeaconManager.getInstanceForApplication(application);
        beaconManager.setBackgroundBetweenScanPeriod(builder.backgroundScanPeriod);
        beaconManager.setForegroundBetweenScanPeriod(builder.foregroundScanPeriod);
        beaconManager.setBackgroundScanPeriod(builder.backgroundScanPeriod);
        beaconManager.setForegroundScanPeriod(builder.foregroundScanPeriod);

        // Add all the beacon types we want to discover
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(EDDYSTONE_UID_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(EDDYSTONE_URL_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(EDDYSTONE_TLM_LAYOUT));
    }

    public static class Builder {
        private long backgroundScanPeriod = 10000L;
        private long foregroundScanPeriod = 10000L;
        private Context context;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder addBackgroundScanPeriod(long backgroundScanPeriod) {
            this.backgroundScanPeriod = backgroundScanPeriod;
            return this;
        }

        public Builder addForegroundScanPeriod(long foregroundScanPeriod) {
            this.foregroundScanPeriod = foregroundScanPeriod;
            return this;
        }

        public RxBeacon build() {
            return new RxBeacon(this);
        }
    }

    private Region getRegion() {
        if (this.region == null) {
            this.region = new Region("myMonitoringUniqueId", null, null, null);
        }
        return region;
    }

    public RxBeacon addBeaconParser(String parser) {
        beaconManager.getBeaconParsers()
                .add(new BeaconParser().
                        setBeaconLayout(parser));

        return this;
    }

    public RxBeacon region(Region region) {
        this.region = region;
        return this;
    }

    private Observable<Boolean> startup() {
        return Observable
                .create(new ObservableOnSubscribe<Boolean>() {
                    @Override
                    public void subscribe(@NonNull final ObservableEmitter<Boolean> objectObservableEmitter) throws Exception {
                        beaconConsumer = new BeaconConsumer() {
                            @Override
                            public void onBeaconServiceConnect() {
                                objectObservableEmitter.onNext(true);
                                objectObservableEmitter.onComplete();
                            }

                            @Override
                            public Context getApplicationContext() {
                                return application;
                            }

                            @Override
                            public void unbindService(ServiceConnection serviceConnection) {
                                application.unbindService(serviceConnection);
                            }

                            @Override
                            public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
                                return application.bindService(intent, serviceConnection, i);
                            }
                        };
                        beaconManager.bind(beaconConsumer);
                    }
                })
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        if (beaconConsumer != null) {
                            beaconManager.unbind(beaconConsumer);
                        }
                    }
                });

    }

    public Observable<RxBeaconRange> beaconsInRegion() {
        return startup()
                .flatMap(new Function<Boolean, ObservableSource<RxBeaconRange>>() {
                    @Override
                    public ObservableSource<RxBeaconRange> apply(@NonNull Boolean aBoolean) throws Exception {
                        return Observable.create(new ObservableOnSubscribe<RxBeaconRange>() {
                            @Override
                            public void subscribe(@NonNull final ObservableEmitter<RxBeaconRange> objectObservableEmitter) throws Exception {
                                beaconManager.addRangeNotifier(new RangeNotifier() {
                                    @Override
                                    public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
                                        objectObservableEmitter.onNext(new RxBeaconRange(Extensions.mapToBeaconSave(collection), region));
                                    }
                                });
                                beaconManager.startRangingBeaconsInRegion(getRegion());
                            }
                        });
                    }
                });
    }


    public Observable<RxBeaconMonitor> monitor() {
        return startup()
                .flatMap(new Function<Boolean, ObservableSource<RxBeaconMonitor>>() {
                    @Override
                    public ObservableSource<RxBeaconMonitor> apply(@NonNull Boolean aBoolean) throws Exception {
                        return Observable.create(new ObservableOnSubscribe<RxBeaconMonitor>() {
                            @Override
                            public void subscribe(@NonNull final ObservableEmitter<RxBeaconMonitor> observableEmitter) throws Exception {
                                beaconManager.addMonitorNotifier(new MonitorNotifier(){
                                    @Override
                                    public void didEnterRegion(Region region) {
                                        observableEmitter.onNext(new RxBeaconMonitor(RxBeaconMonitor.State.ENTER, region));
                                    }

                                    @Override
                                    public void didExitRegion(Region region) {
                                        observableEmitter.onNext(new RxBeaconMonitor(RxBeaconMonitor.State.EXIT, region));
                                    }

                                    @Override
                                    public void didDetermineStateForRegion(int i, Region region) {
                                        observableEmitter.onNext(new RxBeaconMonitor(null, region));
                                    }
                                });
                                beaconManager.startMonitoringBeaconsInRegion(getRegion());
                            }
                        });
                    }
                });
    }

    public void stopScan() {
        if (beaconConsumer != null) {
            beaconManager.unbind(beaconConsumer);
        }
    }
}
