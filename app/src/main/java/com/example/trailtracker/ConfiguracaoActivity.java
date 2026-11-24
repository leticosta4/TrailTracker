package com.example.trailtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ConfiguracaoActivity extends AppCompatActivity implements View.OnClickListener {

    //preferencias compartilhadas - armazenamento
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor sharedPreferencesEditor;

    RadioButton radioVetorial;
    RadioButton radioSatelite;
    RadioButton radioNorthUp;
    RadioButton radioCourseUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_configuracao);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button aplicarBtn = findViewById(R.id.button_aplicar);
        aplicarBtn.setOnClickListener(this);

        radioVetorial = findViewById(R.id.radio_vetorial);
        radioSatelite = findViewById(R.id.radio_satelite);
        radioNorthUp = findViewById(R.id.radio_north_up);
        radioCourseUp = findViewById(R.id.radio_course_up);

        sharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE);

        //tipo_mapa: 0 = vetorial (padrao) e 1 = satelite
        if (sharedPreferences.getInt("tipo_mapa", 0) == 0) {
            radioVetorial.setChecked(true);
        } else {
            radioSatelite.setChecked(true);
        }

        //navegacao: 0 = north up (padrao) e 1 = course up
        if (sharedPreferences.getInt("navegacao", 0) == 0) {
            radioNorthUp.setChecked(true);
        } else {
            radioCourseUp.setChecked(true);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_aplicar) {
            sharedPreferencesEditor = sharedPreferences.edit();

            if (radioVetorial.isChecked()) {
                sharedPreferencesEditor.putInt("tipo_mapa", 0);
            } else {
                sharedPreferencesEditor.putInt("tipo_mapa", 1);
            }

            if (radioNorthUp.isChecked()) {
                sharedPreferencesEditor.putInt("navegacao", 0);
            } else {
                sharedPreferencesEditor.putInt("navegacao", 1);
            }

            sharedPreferencesEditor.apply();
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
