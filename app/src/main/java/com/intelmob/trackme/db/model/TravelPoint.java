
package com.intelmob.trackme.db.model;

import com.google.android.gms.maps.model.LatLng;

public class TravelPoint {

    public LatLng latLng;
    public long timestamp;
    public float speedKPH;

    public TravelPoint(LatLng latLng, long timestamp, float speedKPH) {
        this.latLng = latLng;
        this.timestamp = timestamp;
        this.speedKPH = speedKPH;
    }
}
