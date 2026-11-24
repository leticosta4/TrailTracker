package com.example.trailtracker;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.trailtracker.model.TrilhaDbHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.util.Date;

public class RegistrarTrilhaActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {

    private static final int REQUEST_LOCATION_UPDATES = 1; //request code para o gerenciamento de permissoes

    //objetos da API de localizacao
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;

    //componentes da tela
    TextView textoVelocidade;
    TextView textoVelocidadeMaxima;
    TextView textoDistancia;
    TextView textoStatusGps;
    Chronometer cronometro;
    EditText campoNome;
    Button botaoGravar;

    SharedPreferences sharedPreferences;
    TrilhaDbHelper db;

    //mapa
    GoogleMap mMap;
    Marker marcador;
    Circle circuloAcuracia;
    float rumoAtual = 0;
    boolean primeiraLeitura = true;

    //dados da gravacao
    boolean gravando = false;
    long trilhaId;
    long inicioMs;
    float distanciaTotal = 0; //metros
    float velocidadeMaxima = 0; //m/s
    Location ultimoPontoGravado;
    Location ultimaLocalizacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registrar_trilha);
        //aqui o teclado tambem entra nos insets para nao cobrir o campo de nome
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textoVelocidade = findViewById(R.id.texto_velocidade);
        textoVelocidadeMaxima = findViewById(R.id.texto_velocidade_maxima);
        textoDistancia = findViewById(R.id.texto_distancia);
        textoStatusGps = findViewById(R.id.texto_status_gps);
        cronometro = findViewById(R.id.cronometro);
        campoNome = findViewById(R.id.campo_nome);
        botaoGravar = findViewById(R.id.botao_gravar);
        botaoGravar.setOnClickListener(this);

        sharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE);
        db = new TrilhaDbHelper(this);

        mostraMetricas(0);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        //so comeca a pedir a localizacao quando o mapa esta pronto
        startLocationUpdate();
    }

    public void startLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            //evita registrar o escutador duas vezes
            if (locationCallback != null) {
                return;
            }

            // Cria o cliente (FusedLocationProviderClient)
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

            // Configura a solicitação de localizações (LocationRequest)
            long timeInterval = 1000;
            locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, timeInterval).build();

            // Programa o escutador para consumir as novas localizações geradas (LocationCallback)
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    for (Location location : locationResult.getLocations()) {
                        processaLocalizacao(location);
                    }
                }
            };

            // Manda o cliente começar a gerar atualizações de localização
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else { //solicita a permissao
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_UPDATES);
        }
    }

    public void stopLocationUpdate() {
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_UPDATES) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //o usuario acabou de dar a permissao
                startLocationUpdate();
            } else {
                Toast.makeText(this, R.string.record_no_permission, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    //chamado a cada nova localizacao
    public void processaLocalizacao(Location location) {
        ultimaLocalizacao = location;
        if (location.hasBearing()) {
            rumoAtual = location.getBearing();
        }
        float velocidade = 0;
        if (location.hasSpeed()) {
            velocidade = location.getSpeed();
        }

        atualizaMapa(location);

        //guarda a ultima posicao para centralizar o mapa da tela principal
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("ultima_lat", (float) location.getLatitude());
        editor.putFloat("ultima_lon", (float) location.getLongitude());
        editor.apply();

        textoStatusGps.setText(getString(R.string.record_accuracy, Math.round(location.getAccuracy())));

        if (gravando) {
            registraPonto(location);
        }
        mostraMetricas(velocidade);
    }

    public void atualizaMapa(Location location) {
        LatLng posicao = new LatLng(location.getLatitude(), location.getLongitude());
        boolean courseUp = sharedPreferences.getInt("navegacao", 0) == 1;

        //no north up a seta gira com o rumo, no course up o mapa e que gira
        float rotacao = rumoAtual;
        if (courseUp) {
            rotacao = 0;
        }

        //marcador com a imagem desenhada no canvas
        if (marcador == null) {
            marcador = mMap.addMarker(new MarkerOptions()
                    .position(posicao)
                    .title(getString(R.string.record_marker_title))
                    .icon(BitmapDescriptorFactory.fromBitmap(criaImagemMarcador()))
                    .anchor(0.5f, 0.5f)
                    .rotation(rotacao));
        } else {
            marcador.setPosition(posicao);
            marcador.setRotation(rotacao);
        }

        //circulo semitransparente com o raio da precisao
        if (circuloAcuracia == null) {
            circuloAcuracia = mMap.addCircle(new CircleOptions()
                    .center(posicao)
                    .radius(location.getAccuracy())
                    .strokeColor(ContextCompat.getColor(this, R.color.accuracy_stroke))
                    .fillColor(ContextCompat.getColor(this, R.color.accuracy_fill)));
        } else {
            circuloAcuracia.setCenter(posicao);
            circuloAcuracia.setRadius(location.getAccuracy());
        }

        //camera segue o usuario (so aplica o zoom inicial na primeira leitura)
        float zoom = mMap.getCameraPosition().zoom;
        if (primeiraLeitura) {
            zoom = 17;
            primeiraLeitura = false;
        }
        float bearing = 0;
        if (courseUp) {
            bearing = rumoAtual;
        }
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(posicao, zoom, 0, bearing)));
    }

    //desenha uma seta no canvas para usar como marcador
    public Bitmap criaImagemMarcador() {
        float densidade = getResources().getDisplayMetrics().density;
        int tamanho = Math.round(44 * densidade);

        Bitmap bitmap = Bitmap.createBitmap(tamanho, tamanho, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Path seta = new Path();
        seta.moveTo(tamanho * 0.50f, tamanho * 0.06f);
        seta.lineTo(tamanho * 0.88f, tamanho * 0.92f);
        seta.lineTo(tamanho * 0.50f, tamanho * 0.72f);
        seta.lineTo(tamanho * 0.12f, tamanho * 0.92f);
        seta.close();

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(this, R.color.accent));
        canvas.drawPath(seta, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3 * densidade);
        paint.setColor(ContextCompat.getColor(this, R.color.white));
        canvas.drawPath(seta, paint);

        return bitmap;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.botao_gravar) {
            if (gravando) {
                pararGravacao();
            } else {
                iniciarGravacao();
            }
        }
    }

    public void iniciarGravacao() {
        //sem localizacao ainda nao da para comecar
        if (ultimaLocalizacao == null) {
            Toast.makeText(this, R.string.record_waiting_gps, Toast.LENGTH_SHORT).show();
            return;
        }

        inicioMs = System.currentTimeMillis();
        String nome = campoNome.getText().toString().trim();
        if (nome.isEmpty()) {
            //se nao digitou nome usa a data e hora
            String dataHora = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(new Date(inicioMs));
            nome = getString(R.string.record_default_name, dataHora);
        }
        trilhaId = db.inserirTrilha(nome, inicioMs);

        distanciaTotal = 0;
        velocidadeMaxima = 0;
        ultimoPontoGravado = null;
        gravando = true;

        cronometro.setBase(SystemClock.elapsedRealtime());
        cronometro.start();

        botaoGravar.setText(R.string.record_stop);
        botaoGravar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.stop)));
        campoNome.setEnabled(false);
        //mantem a tela ligada porque nao tem servico em segundo plano
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        registraPonto(ultimaLocalizacao);
        mostraMetricas(0);
    }

    //soma a distancia, atualiza a velocidade maxima e grava o ponto no banco
    public void registraPonto(Location location) {
        //leitura com precisao ruim nao entra na trilha
        if (location.getAccuracy() > 50) {
            return;
        }
        if (location.hasSpeed() && location.getSpeed() > velocidadeMaxima) {
            velocidadeMaxima = location.getSpeed();
        }
        if (ultimoPontoGravado != null) {
            float deslocamento = ultimoPontoGravado.distanceTo(location);
            //menos de 2 metros e so o gps tremendo
            if (deslocamento < 2) {
                return;
            }
            distanciaTotal += deslocamento;
        }
        ultimoPontoGravado = location;
        db.inserirPonto(trilhaId, location.getLatitude(), location.getLongitude(), location.getTime());
    }

    public void pararGravacao() {
        finalizaGravacao();
        Toast.makeText(this, R.string.record_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    //grava no banco o fim, a velocidade media, a velocidade maxima e a distancia
    public void finalizaGravacao() {
        if (!gravando) {
            return;
        }
        gravando = false;
        cronometro.stop();

        long fimMs = System.currentTimeMillis();
        float segundos = (fimMs - inicioMs) / 1000f;
        //velocidade media = distancia total / tempo total
        float velocidadeMedia = 0;
        if (segundos > 0) {
            velocidadeMedia = distanciaTotal / segundos;
        }
        db.finalizarTrilha(trilhaId, fimMs, velocidadeMedia, velocidadeMaxima, distanciaTotal);
    }

    //velocidades chegam em m/s e sao mostradas em km/h
    public void mostraMetricas(float velocidade) {
        textoVelocidade.setText(getString(R.string.format_speed, velocidade * 3.6f));
        textoVelocidadeMaxima.setText(getString(R.string.format_speed, velocidadeMaxima * 3.6f));
        textoDistancia.setText(getString(R.string.format_distance_m, Math.round(distanciaTotal)));
    }

    @Override
    protected void onDestroy() {
        //se sair da tela gravando a trilha e salva
        finalizaGravacao();
        stopLocationUpdate();
        db.close();
        super.onDestroy();
    }
}
