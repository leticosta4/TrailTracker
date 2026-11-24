package com.example.trailtracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback {

    GoogleMap mMap;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //preferencias onde ficam as configuracoes
        sharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE);

        //instanciando os botoes
        Button trilhasBtn = findViewById(R.id.button_trilhas);
        Button iniciarBtn = findViewById(R.id.button_iniciar);
        Button configBtn = findViewById(R.id.button_configuracao);

        trilhasBtn.setOnClickListener(this);
        iniciarBtn.setOnClickListener(this);
        configBtn.setOnClickListener(this);

        //pega o mapa e avisa quando ele estiver pronto
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        configuraMapa();
    }

    //quando volta da tela de configuracao o tipo do mapa pode ter mudado
    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            configuraMapa();
        }
    }

    public void configuraMapa() {
        //0 = vetorial e 1 = satelite
        if (sharedPreferences.getInt("tipo_mapa", 0) == 1) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        //ultima posicao conhecida (se nao tiver, usa a UCSAL)
        float lat = sharedPreferences.getFloat("ultima_lat", -12.94825f);
        float lon = sharedPreferences.getFloat("ultima_lon", -38.41334f);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 15));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_trilhas) {
            Intent trilhasIntent = new Intent(this, TrilhasActivity.class);
            startActivity(trilhasIntent);
        } else if (v.getId() == R.id.button_iniciar) {
            Intent registrarIntent = new Intent(this, RegistrarTrilhaActivity.class);
            startActivity(registrarIntent);
        } else if (v.getId() == R.id.button_configuracao) {
            Intent configIntent = new Intent(this, ConfiguracaoActivity.class);
            startActivity(configIntent);
        }
    }
}
