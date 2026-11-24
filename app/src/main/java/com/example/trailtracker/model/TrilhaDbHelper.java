package com.example.trailtracker.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

//duas tabelas: trilhas (dados gerais) e pontos (o percurso de cada trilha)
public class TrilhaDbHelper extends SQLiteOpenHelper {

    static final String DB_NAME = "trailtracker.db";
    static final int DB_VERSION = 1;

    public TrilhaDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE trilhas ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nome TEXT NOT NULL, "
                + "inicio INTEGER NOT NULL, "
                + "fim INTEGER NOT NULL, "
                + "vel_media REAL NOT NULL DEFAULT 0, "
                + "vel_maxima REAL NOT NULL DEFAULT 0, "
                + "distancia REAL NOT NULL DEFAULT 0)");

        //trilha_id liga o ponto a sua trilha
        db.execSQL("CREATE TABLE pontos ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "trilha_id INTEGER NOT NULL, "
                + "latitude REAL NOT NULL, "
                + "longitude REAL NOT NULL, "
                + "instante INTEGER NOT NULL, "
                + "FOREIGN KEY (trilha_id) REFERENCES trilhas(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS pontos");
        db.execSQL("DROP TABLE IF EXISTS trilhas");
        onCreate(db);
    }

    //cria a trilha quando comeca a gravar e devolve o id
    public long inserirTrilha(String nome, long inicio) {
        ContentValues values = new ContentValues();
        values.put("nome", nome);
        values.put("inicio", inicio);
        values.put("fim", inicio);
        return getWritableDatabase().insert("trilhas", null, values);
    }

    //quando para a gravacao preenche o fim e as estatisticas
    public void finalizarTrilha(long id, long fim, float velMedia, float velMaxima, float distancia) {
        ContentValues values = new ContentValues();
        values.put("fim", fim);
        values.put("vel_media", velMedia);
        values.put("vel_maxima", velMaxima);
        values.put("distancia", distancia);
        getWritableDatabase().update("trilhas", values, "id = ?", new String[]{String.valueOf(id)});
    }

    //adiciona uma posicao ao percurso
    public void inserirPonto(long trilhaId, double latitude, double longitude, long instante) {
        ContentValues values = new ContentValues();
        values.put("trilha_id", trilhaId);
        values.put("latitude", latitude);
        values.put("longitude", longitude);
        values.put("instante", instante);
        getWritableDatabase().insert("pontos", null, values);
    }

    public void renomearTrilha(long id, String novoNome) {
        ContentValues values = new ContentValues();
        values.put("nome", novoNome);
        getWritableDatabase().update("trilhas", values, "id = ?", new String[]{String.valueOf(id)});
    }

    //lista as trilhas da mais recente para a mais antiga
    public List<Trilha> listarTrilhas() {
        List<Trilha> trilhas = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query("trilhas", null, null, null, null, null, "inicio DESC");
        while (cursor.moveToNext()) {
            trilhas.add(lerTrilha(cursor));
        }
        cursor.close();
        return trilhas;
    }

    //devolve null se nao achar a trilha
    public Trilha buscarTrilha(long id) {
        Cursor cursor = getReadableDatabase().query("trilhas", null, "id = ?",
                new String[]{String.valueOf(id)}, null, null, null);
        Trilha trilha = null;
        if (cursor.moveToFirst()) {
            trilha = lerTrilha(cursor);
        }
        cursor.close();
        return trilha;
    }

    public List<Ponto> listarPontos(long trilhaId) {
        List<Ponto> pontos = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query("pontos", null, "trilha_id = ?",
                new String[]{String.valueOf(trilhaId)}, null, null, "id ASC");
        while (cursor.moveToNext()) {
            double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
            double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
            long instante = cursor.getLong(cursor.getColumnIndexOrThrow("instante"));
            pontos.add(new Ponto(latitude, longitude, instante));
        }
        cursor.close();
        return pontos;
    }

    //transforma a linha atual do cursor em um objeto Trilha
    public Trilha lerTrilha(Cursor cursor) {
        return new Trilha(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("nome")),
                cursor.getLong(cursor.getColumnIndexOrThrow("inicio")),
                cursor.getLong(cursor.getColumnIndexOrThrow("fim")),
                cursor.getFloat(cursor.getColumnIndexOrThrow("vel_media")),
                cursor.getFloat(cursor.getColumnIndexOrThrow("vel_maxima")),
                cursor.getFloat(cursor.getColumnIndexOrThrow("distancia")));
    }

    //apaga primeiro os pontos e depois a trilha
    public void apagarTrilha(long id) {
        SQLiteDatabase db = getWritableDatabase();
        String[] args = {String.valueOf(id)};
        db.delete("pontos", "trilha_id = ?", args);
        db.delete("trilhas", "id = ?", args);
    }

    //apaga as trilhas que comecaram entre os dois instantes, devolve quantas foram
    public int apagarIntervalo(long de, long ate) {
        SQLiteDatabase db = getWritableDatabase();
        String[] args = {String.valueOf(de), String.valueOf(ate)};
        db.delete("pontos", "trilha_id IN (SELECT id FROM trilhas WHERE inicio BETWEEN ? AND ?)", args);
        return db.delete("trilhas", "inicio BETWEEN ? AND ?", args);
    }

    public int apagarTodas() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("pontos", null, null);
        return db.delete("trilhas", null, null);
    }
}
