
package com.intelmob.trackme.db;

import com.google.android.gms.maps.model.LatLng;

public class TravelPoint {

    public LatLng latLng;
    public float speedKPH;

    public TravelPoint(LatLng latLng, float speedKPH) {
        this.latLng = latLng;
        this.speedKPH = speedKPH;
    }
}
