package com.manuelserrano.tfg.models;

import com.google.firebase.firestore.GeoPoint;

public class FirebaseBuilding {

    private String uidUser;
    private String name;
    private String imgName;
    private GeoPoint position;

    public FirebaseBuilding() {
    }

    public String getUidUser() {
        return uidUser;
    }

    public void setUidUser(String uidUser) {
        this.uidUser = uidUser;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImgName() {
        return imgName;
    }

    public void setImgName(String imgName) {
        this.imgName = imgName;
    }

    public GeoPoint getPosition() {
        return position;
    }

    public void setPosition(GeoPoint position) {
        this.position = position;
    }
}
