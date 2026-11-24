package com.example.trailtracker.model;

import com.google.android.gms.maps.model.LatLng;

//classe para representar um ponto do percurso (uma linha da tabela pontos)
public class Ponto {

    double latitude;
    double longitude;
    long instante;

    public Ponto(double latitude, double longitude, long instante) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.instante = instante;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getInstante() {
        return instante;
    }

    public LatLng toLatLng() {
        return new LatLng(latitude, longitude);
    }
}
