package com.example.trailtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.trailtracker.model.Ponto;
import com.example.trailtracker.model.Trilha;
import com.example.trailtracker.model.TrilhaDbHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VisualizarTrilhaActivity extends AppCompatActivity implements OnMapReadyCallback {

    TrilhaDbHelper db;
    SharedPreferences sharedPreferences;
    Trilha trilha;
    GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_visualizar_trilha);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = new TrilhaDbHelper(this);
        sharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE);

        //pega a trilha que a tela anterior mandou pelo intent
        long id = getIntent().getLongExtra("trilha_id", -1);
        trilha = db.buscarTrilha(id);
        if (trilha == null) {
            Toast.makeText(this, R.string.view_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mostraInformacoes();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    //informacoes em texto sobrepostas ao mapa
    public void mostraInformacoes() {
        TextView nome = findViewById(R.id.info_nome);
        TextView inicio = findViewById(R.id.info_inicio);
        TextView velMedia = findViewById(R.id.info_vel_media);
        TextView velMaxima = findViewById(R.id.info_vel_maxima);
        TextView distancia = findViewById(R.id.info_distancia);
        TextView duracao = findViewById(R.id.info_duracao);

        nome.setText(trilha.getNome());

        DateFormat formato = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        inicio.setText(formato.format(new Date(trilha.getInicio())));

        //velocidades estao em m/s e sao mostradas em km/h
        velMedia.setText(getString(R.string.format_speed, trilha.getVelMedia() * 3.6f));
        velMaxima.setText(getString(R.string.format_speed, trilha.getVelMaxima() * 3.6f));
        distancia.setText(getString(R.string.format_distance_m, Math.round(trilha.getDistancia())));

        //duracao em horas:minutos:segundos
        long segundos = (trilha.getFim() - trilha.getInicio()) / 1000;
        duracao.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d",
                segundos / 3600, (segundos % 3600) / 60, segundos % 60));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //0 = vetorial e 1 = satelite
        if (sharedPreferences.getInt("tipo_mapa", 0) == 1) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        desenhaPercurso();
    }

    public void desenhaPercurso() {
        List<Ponto> pontos = db.listarPontos(trilha.getId());
        if (pontos.isEmpty()) {
            Toast.makeText(this, R.string.view_no_points, Toast.LENGTH_SHORT).show();
            return;
        }

        List<LatLng> caminho = new ArrayList<>();
        LatLngBounds.Builder limites = new LatLngBounds.Builder();
        for (Ponto ponto : pontos) {
            caminho.add(ponto.toLatLng());
            limites.include(ponto.toLatLng());
        }

        //polilinha ligando todos os pontos
        mMap.addPolyline(new PolylineOptions()
                .addAll(caminho)
                .color(ContextCompat.getColor(this, R.color.route))
                .width(10));

        //marcador de inicio (verde)
        mMap.addMarker(new MarkerOptions()
                .position(caminho.get(0))
                .title(getString(R.string.view_marker_start))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        //marcador de fim (vermelho)
        if (caminho.size() > 1) {
            mMap.addMarker(new MarkerOptions()
                    .position(caminho.get(caminho.size() - 1))
                    .title(getString(R.string.view_marker_end))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }

        //ajusta a camera para o percurso inteiro caber na tela
        if (caminho.size() == 1) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(caminho.get(0), 17));
        } else {
            int largura = getResources().getDisplayMetrics().widthPixels;
            int altura = getResources().getDisplayMetrics().heightPixels;
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(limites.build(), largura, altura, 150));
        }
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
