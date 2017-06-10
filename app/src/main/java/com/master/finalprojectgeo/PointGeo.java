package com.master.finalprojectgeo;

/**
 * Clase en la que se almacenarán los datos del punto
 */
class PointGeo {
    private double lat;
    private double lon;
    private int radius;
    private String message;
    private int id;
    private String device_id;

    public PointGeo(double lat, double lon, int radius, String message,  String device_id) {
        this.lat = lat;
        this.lon = lon;
        this.radius = radius;
        this.message = message;
        this.device_id = device_id;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }
}
