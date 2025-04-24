package com.example.flex;

public class User {
    String name;
    double latitude;
    double longitude;
    boolean online;

    public User() {}

    public User(String name, double latitude, double longitude, boolean online) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.online = online;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {

        return longitude;
    }

    public boolean isOnline() {
        return online;
    }
}
