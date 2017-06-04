package com.master.finalprojectgeo;

/**
 * Created by Hector on 04/06/2017.
 */

class PointGeo {
    private double lat;
    private double lon;
    private int radius;
    private String message;

    public PointGeo(double lat, double lon, int radius, String message) {
        this.lat = lat;
        this.lon = lon;
        this.radius = radius;
        this.message = message;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(float lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(float lon) {
        this.lon = lon;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


}