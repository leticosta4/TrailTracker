package com.example.trailtracker;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.trailtracker.model.Trilha;
import com.example.trailtracker.model.TrilhaDbHelper;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TrilhasActivity extends AppCompatActivity implements View.OnClickListener {

    TrilhaDbHelper db;
    LinearLayout listaTrilhas;
    TextView listaVazia;
    LinearLayout painelPeriodo;
    Button botaoDataDe;
    Button botaoDataAte;

    //formatos de data no idioma do aparelho
    DateFormat formatoData = DateFormat.getDateInstance(DateFormat.SHORT);
    DateFormat formatoDataHora = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

    //periodo escolhido para apagar
    long dataDe;
    long dataAte;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trilhas);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = new TrilhaDbHelper(this);

        listaTrilhas = findViewById(R.id.lista_trilhas);
        listaVazia = findViewById(R.id.lista_vazia);
        painelPeriodo = findViewById(R.id.painel_periodo);
        botaoDataDe = findViewById(R.id.botao_data_de);
        botaoDataAte = findViewById(R.id.botao_data_ate);

        //instanciando os botoes
        Button apagarPeriodoBtn = findViewById(R.id.botao_apagar_periodo);
        Button apagarTodasBtn = findViewById(R.id.botao_apagar_todas);
        Button confirmarPeriodoBtn = findViewById(R.id.botao_confirmar_periodo);

        apagarPeriodoBtn.setOnClickListener(this);
        apagarTodasBtn.setOnClickListener(this);
        confirmarPeriodoBtn.setOnClickListener(this);
        botaoDataDe.setOnClickListener(this);
        botaoDataAte.setOnClickListener(this);

        //comeca com o periodo igual a hoje
        dataDe = System.currentTimeMillis();
        dataAte = dataDe;
        atualizaBotoesData();
    }

    //recarrega a lista sempre que a tela volta
    @Override
    protected void onResume() {
        super.onResume();
        carregaLista();
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    //monta uma linha (item_trilha.xml) para cada trilha
    public void carregaLista() {
        listaTrilhas.removeAllViews();
        List<Trilha> trilhas = db.listarTrilhas();

        if (trilhas.isEmpty()) {
            listaVazia.setVisibility(View.VISIBLE);
        } else {
            listaVazia.setVisibility(View.GONE);
        }

        for (Trilha trilha : trilhas) {
            adicionaLinha(trilha);
        }
    }

    public void adicionaLinha(final Trilha trilha) {
        View linha = getLayoutInflater().inflate(R.layout.item_trilha, listaTrilhas, false);
        TextView nome = linha.findViewById(R.id.item_nome);
        TextView resumo = linha.findViewById(R.id.item_resumo);
        Button verBtn = linha.findViewById(R.id.item_ver);
        Button editarBtn = linha.findViewById(R.id.item_editar);
        Button apagarBtn = linha.findViewById(R.id.item_apagar);

        nome.setText(trilha.getNome());
        resumo.setText(getString(R.string.trails_item_summary,
                formatoDataHora.format(new Date(trilha.getInicio())),
                getString(R.string.format_distance_m, Math.round(trilha.getDistancia()))));

        //consultar: abre a tela com o trajeto no mapa
        verBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent verIntent = new Intent(TrilhasActivity.this, VisualizarTrilhaActivity.class);
                verIntent.putExtra("trilha_id", trilha.getId());
                startActivity(verIntent);
            }
        });

        editarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostraDialogoRenomear(trilha);
            }
        });

        apagarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmaApagarUma(trilha);
            }
        });

        listaTrilhas.addView(linha);
    }

    //editar: altera o nome da trilha
    public void mostraDialogoRenomear(final Trilha trilha) {
        View conteudo = getLayoutInflater().inflate(R.layout.dialog_nome, null);
        final EditText campo = conteudo.findViewById(R.id.dialog_campo_nome);
        campo.setText(trilha.getNome());

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_edit_title)
                .setView(conteudo)
                .setPositiveButton(R.string.dialog_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String novoNome = campo.getText().toString().trim();
                        if (novoNome.isEmpty()) {
                            Toast.makeText(TrilhasActivity.this, R.string.trails_name_empty, Toast.LENGTH_SHORT).show();
                        } else {
                            db.renomearTrilha(trilha.getId(), novoNome);
                            carregaLista();
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    //apagar uma trilha especifica
    public void confirmaApagarUma(final Trilha trilha) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_one_title)
                .setMessage(getString(R.string.dialog_delete_one_message, trilha.getNome()))
                .setPositiveButton(R.string.dialog_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.apagarTrilha(trilha.getId());
                        carregaLista();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    //apagar todas as trilhas
    public void confirmaApagarTodas() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_all_title)
                .setMessage(R.string.dialog_delete_all_message)
                .setPositiveButton(R.string.dialog_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int apagadas = db.apagarTodas();
                        mostraTotalApagado(apagadas);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    //apagar as trilhas que comecaram dentro do periodo
    public void confirmaApagarPeriodo() {
        final long de = inicioDoDia(dataDe);
        final long ate = fimDoDia(dataAte);

        if (de > ate) {
            Toast.makeText(this, R.string.trails_range_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_range_title)
                .setMessage(getString(R.string.dialog_delete_range_message,
                        formatoData.format(new Date(de)), formatoData.format(new Date(ate))))
                .setPositiveButton(R.string.dialog_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int apagadas = db.apagarIntervalo(de, ate);
                        painelPeriodo.setVisibility(View.GONE);
                        mostraTotalApagado(apagadas);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    public void mostraTotalApagado(int apagadas) {
        Toast.makeText(this, getString(R.string.trails_deleted_count, apagadas), Toast.LENGTH_SHORT).show();
        carregaLista();
    }

    public void atualizaBotoesData() {
        botaoDataDe.setText(getString(R.string.trails_from, formatoData.format(new Date(dataDe))));
        botaoDataAte.setText(getString(R.string.trails_to, formatoData.format(new Date(dataAte))));
    }

    //abre o calendario e guarda a data escolhida em dataDe ou dataAte
    public void escolheData(final boolean inicio) {
        Calendar c = Calendar.getInstance();
        if (inicio) {
            c.setTimeInMillis(dataDe);
        } else {
            c.setTimeInMillis(dataAte);
        }

        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                Calendar escolhida = Calendar.getInstance();
                escolhida.set(year, month, dayOfMonth);
                if (inicio) {
                    dataDe = escolhida.getTimeInMillis();
                } else {
                    dataAte = escolhida.getTimeInMillis();
                }
                atualizaBotoesData();
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    //00:00:00 do dia
    public long inicioDoDia(long instante) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(instante);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    //23:59:59 do dia
    public long fimDoDia(long instante) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(instante);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.botao_apagar_periodo) {
            //mostra ou esconde o painel com as datas
            if (painelPeriodo.getVisibility() == View.VISIBLE) {
                painelPeriodo.setVisibility(View.GONE);
            } else {
                painelPeriodo.setVisibility(View.VISIBLE);
            }
        } else if (v.getId() == R.id.botao_apagar_todas) {
            confirmaApagarTodas();
        } else if (v.getId() == R.id.botao_data_de) {
            escolheData(true);
        } else if (v.getId() == R.id.botao_data_ate) {
            escolheData(false);
        } else if (v.getId() == R.id.botao_confirmar_periodo) {
            confirmaApagarPeriodo();
        }
    }
}
