package com.example.trailtracker.model;

//classe para representar uma trilha (uma linha da tabela trilhas)
public class Trilha {

    long id;
    String nome;
    long inicio;
    long fim;
    float velMedia;   //m/s
    float velMaxima;  //m/s
    float distancia;  //metros

    public Trilha(long id, String nome, long inicio, long fim,
                  float velMedia, float velMaxima, float distancia) {
        this.id = id;
        this.nome = nome;
        this.inicio = inicio;
        this.fim = fim;
        this.velMedia = velMedia;
        this.velMaxima = velMaxima;
        this.distancia = distancia;
    }

    public long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public long getInicio() {
        return inicio;
    }

    public long getFim() {
        return fim;
    }

    public float getVelMedia() {
        return velMedia;
    }

    public float getVelMaxima() {
        return velMaxima;
    }

    public float getDistancia() {
        return distancia;
    }
}
