package com.manuelserrano.tfg.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class BeaconBuilding {

    @SerializedName("lat")
    @Expose
    private Double lat;
    @SerializedName("lng")
    @Expose
    private Double lng;
    private Double distance;

    private String bluetoothAddress;

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    public void setBluetoothAddress(String bluetoothAddress) {
        this.bluetoothAddress = bluetoothAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeaconBuilding that = (BeaconBuilding) o;
        return bluetoothAddress.equals(that.bluetoothAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bluetoothAddress);
    }

    @Override
    public String toString() {
        return "[" +
                 distance +
                ",'" + bluetoothAddress + '\'' +
                ']';
    }
}
