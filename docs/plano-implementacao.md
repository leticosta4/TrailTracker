# Plano de Implementação — Avaliação 3

**Aplicativo "TrailTracker"** · Computação para Dispositivos Móveis
Entrega e apresentação: **26/11/2026**

---

## 1. Resumo do que precisa ser entregue

Um app Android em **Java** (projeto Android Studio) com **uma atividade por tela**:

| Atividade | O que faz | Peso |
|---|---|---|
| `MainActivity` + `ConfiguracaoActivity` | Tela inicial (mapa + 3 botões) e tela de configurações | 30% |
| `RegistrarTrilhaActivity` | Mostra a posição no mapa e registra a trilha | 30% |
| `TrilhasActivity` + `VisualizarTrilhaActivity` | Lista, edita, apaga e consulta trilhas | 40% |

Requisitos do enunciado, um por um:

1. **Configuração:** tipo de mapa (vetorial = padrão / satélite) e navegação (North Up = padrão / Course Up). Valem para **todas** as atividades "quando pertinente".
2. **Registrar trilha**, sobreposto ao mapa: velocidade instantânea, velocidade máxima, cronômetro, distância total.
3. **Marcador personalizado** (imagem que não seja o marcador padrão do Google) + **círculo semitransparente** com a acurácia.
4. Toda trilha tem **nome, início, fim, velocidade média, velocidade máxima e o percurso**, em **pelo menos 2 tabelas SQLite**.
5. **Consultar trilhas:** lista; **apagar** (uma, um intervalo ou todas), **editar** o nome, **consultar** (trajeto no mapa + texto com início, vel. média, vel. máxima, distância e duração).
6. Referência visual: <https://screenshot-scout-maps.lovable.app/> (mapa, "Minhas trilhas", "Iniciar trilha", "Configurações").
7. **Extra pedido pela dupla:** textos em **português e inglês**, conforme o idioma do aparelho.
8. SDK mínimo e defesa oral: ver seção 2.

## 2. Regra de ouro: só o que foi ensinado

Cada tecnologia usada tem origem num slide. O que **não** aparece nos slides **não entra**.

| Necessidade | Ferramenta usada | Slide |
|---|---|---|
| Telas e navegação | `AppCompatActivity`, `Intent`, `startActivity`, `finish` | 3, 4 |
| Layouts | `ConstraintLayout`, `LinearLayout`, `Button`, `TextView`, `EditText`, `RadioGroup`/`RadioButton` | 4, 5 |
| Eventos | `setOnClickListener` com classe anônima | 4 |
| Configurações | `SharedPreferences` (`getSharedPreferences`, `Editor`, `apply`) | 5 |
| Idiomas | `strings.xml` + `values-pt/strings.xml` | 4 |
| Desenho do marcador | `Canvas`, `Paint`, `onDraw`-style drawing | 6 |
| Localização | `FusedLocationProviderClient`, `LocationRequest.Builder`, `LocationCallback`, permissão em tempo de execução | 7 |
| Mapa | `SupportMapFragment`, `GoogleMap`, `addMarker`, `addCircle`, `addPolyline`, `CameraUpdateFactory`, `CameraPosition`, `setMapType`, `UiSettings` | 8 |
| Banco | `SQLiteOpenHelper`, `SQLiteDatabase`, `ContentValues`, `Cursor` | 9 |

**Fora do escopo, de propósito:** Room, ViewModel/LiveData, Service em segundo plano, RecyclerView, bibliotecas de terceiros, Kotlin, lambdas.

**Duas observações que precisam constar na defesa:**

- **SDK mínimo:** o enunciado diz "SDK min 17", mas o esqueleto entregue já está com `minSdk 33` (igual à Av2), e `play-services-location 21.x` / `play-services-maps 19.x` não rodam abaixo da API 21. **Mantivemos `minSdk 33`.**
- **Chave do Google Maps:** o slide 8 traz uma chave do professor. **Não a usamos.** A chave fica em `local.properties` (`MAPS_API_KEY=...`), que não vai para o Git.

## 3. Decisões de projeto

### 3.1 Telas

```
MainActivity ──► ConfiguracaoActivity
     │
     ├──────────► RegistrarTrilhaActivity
     │
     └──────────► TrilhasActivity ──► VisualizarTrilhaActivity
```

- **MainActivity** copia o protótipo: cabeçalho "TRAIL TRACKER", mapa ocupando a tela e, embaixo, três botões: *Minhas trilhas* · **Iniciar trilha** · *Configurações*. O mapa respeita o tipo de mapa configurado e abre na última posição conhecida (gravada em `SharedPreferences` pela tela de registro; padrão: UCSAL, do slide 8).
- **RegistrarTrilhaActivity** (travada em retrato): mapa + painel de métricas no topo + painel inferior com campo de nome e botão **Iniciar / Parar**.
- **TrilhasActivity**: lista com uma linha por trilha (nome, data, distância) e botões *Ver*, *Editar*, *Apagar*; barra de apagar intervalo / apagar todas.
- **VisualizarTrilhaActivity**: mapa com a polilinha, marcadores de início/fim e cartão sobreposto com as informações em texto.

### 3.2 Configurações (`SharedPreferences`, arquivo `"Config"`)

| Chave | Tipo | Valores |
|---|---|---|
| `tipo_mapa` | int | `0` = vetorial (padrão) · `1` = satélite |
| `navegacao` | int | `0` = North Up (padrão) · `1` = Course Up |
| `ultima_lat` / `ultima_lon` | float | última posição, só para centrar o mapa inicial |

Onde cada configuração é respeitada:

| Atividade | Tipo de mapa | Navegação |
|---|---|---|
| Principal | sim | não pertinente (mapa estático) |
| Registrar | sim | **sim**: North Up → `bearing 0`; Course Up → `bearing` do deslocamento |
| Visualizar | sim | não pertinente (percurso já feito, fica norte para cima) |

### 3.3 Banco SQLite (`trailtracker.db`, `SQLiteOpenHelper`)

```
trilhas                               pontos
───────────────────────               ──────────────────────
id            INTEGER PK AUTOINC  ◄── trilha_id   INTEGER (FK)
nome          TEXT                    id          INTEGER PK AUTOINC
inicio        INTEGER (millis)        latitude    REAL
fim           INTEGER (millis)        longitude   REAL
vel_media     REAL (m/s)              instante    INTEGER (millis)
vel_maxima    REAL (m/s)
distancia     REAL (m)
```

- Velocidades guardadas em **m/s** (unidade do `Location.getSpeed()`); convertidas para **km/h** só na hora de exibir.
- **Velocidade média = distância total ÷ duração** (não a média das leituras instantâneas, que ficam distorcidas por leituras ruins e paradas).
- O ponto é gravado **assim que chega** (como no slide 9), então uma trilha interrompida não perde o percurso.
- Apagar uma trilha apaga seus pontos na mesma transação (`beginTransaction`/`endTransaction`).
- Apagar por intervalo usa a data de **início** da trilha, de 00:00 do dia inicial até 23:59:59 do dia final.

### 3.4 Registro da trilha (regras)

- A tela **abre e já mostra a posição** (marcador + círculo); o registro só começa em **Iniciar**.
- Cada `Location` novo: atualiza marcador, círculo, velocidade instantânea e câmera; se estiver gravando, soma `distanceTo` ao ponto anterior, atualiza a velocidade máxima e grava o ponto.
- **Filtro de ruído:** leituras com acurácia pior que 50 m não entram na distância nem no percurso.
- **Marcador personalizado:** uma seta desenhada com `Canvas` num `Bitmap` (técnica do slide 6) e entregue com `BitmapDescriptorFactory.fromBitmap`. Em *North Up* a seta gira com o rumo; em *Course Up* o mapa é que gira, e a seta fica sempre para cima.
- **Cronômetro:** o widget `Chronometer`.
- **Sair da tela gravando** finaliza e salva a trilha (`onDestroy`), para não deixar trilha incompleta.
- **Limitação assumida:** sem serviço em primeiro plano (não faz parte da matéria), a coleta depende da tela ligada. Por isso a tela usa `keepScreenOn` enquanto grava.

### 3.5 Idiomas

- `res/values/strings.xml` → **inglês** (padrão, usado para qualquer idioma que não seja português).
- `res/values-pt/strings.xml` → **português**.
- Zero texto escrito direto em layout ou Java; datas formatadas com `DateFormat` do idioma do aparelho; números via `getString(R.string...)`, que respeita o idioma.

### 3.6 Arquitetura de pacotes

Poucas classes, no nível do que foi feito em aula: cada tela lê o `SharedPreferences` direto (como o `ConfigActivity` da aula) e só o banco tem classes de modelo (como no slide 9).

```
com.example.trailtracker
├── MainActivity.java
├── ConfiguracaoActivity.java
├── RegistrarTrilhaActivity.java
├── TrilhasActivity.java
├── VisualizarTrilhaActivity.java
└── model
    ├── Trilha.java            uma trilha (dados da tabela trilhas)
    ├── Ponto.java             um ponto do percurso
    └── TrilhaDbHelper.java    SQLiteOpenHelper + consultas
```

## 4. Plano passo a passo

São **8 etapas**. Cada uma termina em algo que dá para ver no aparelho ou provar. Só se avança quando o "Pronto quando" da anterior vale.

| Etapa | O que fica pronto | O que dá para ver |
|---|---|---|
| 1. Base | Dependências, chave do Maps, tema, cores, strings EN/PT, insets | App abre com cabeçalho verde "TrailTracker", sem erro de build |
| 2. Configuração | `ConfiguracaoActivity` (SharedPreferences) | Escolher mapa/navegação, fechar e reabrir: escolha mantida |
| 3. Tela principal | `MainActivity` com mapa e 3 botões | Mapa (vetorial ou satélite) e navegação para as telas |
| 4. Banco | `Trilha`, `Ponto`, `TrilhaDbHelper` | Inserir, listar, editar e apagar trilhas e pontos |
| 5. Registrar trilha | `RegistrarTrilhaActivity` | Marcador, círculo, métricas ao vivo, trilha gravada |
| 6. Minhas trilhas | `TrilhasActivity` | Lista; editar nome; apagar uma/intervalo/todas |
| 7. Ver trilha | `VisualizarTrilhaActivity` | Percurso no mapa e cartão com as informações |
| 8. Acabamento e defesa | Comentários, README, testes, ensaio | Checklist da seção 5 completo |

---

### Etapa 1 — Base do projeto

**Arquivos:** `gradle/libs.versions.toml` (`play-services-location 21.3.0`, `play-services-maps 19.0.0`), `app/build.gradle.kts` (lê `MAPS_API_KEY` do `local.properties` e injeta no manifesto), `AndroidManifest.xml` (permissões, `meta-data` da chave, 5 atividades), `colors.xml`, `themes.xml`, `strings.xml` (EN) e `values-pt/strings.xml`, as bordas do sistema tratadas em cada tela.

**Pronto quando:** `./gradlew assembleDebug` compila; nenhum texto fixo em layout; a barra de status não cobre o cabeçalho.

### Etapa 2 — Configuração

**Arquivos:** `ConfiguracaoActivity.java`, `layout/activity_configuracao.xml` (dois `RadioGroup` + botão *Aplicar*, como no `ConfiguracaoActivity` da aula).

**Pronto quando:** padrões são *vetorial* e *North Up*; a escolha sobrevive a fechar o app; *Aplicar* mostra `Toast` e volta.

### Etapa 3 — Tela principal

**Arquivos:** `MainActivity.java`, `layout/activity_main.xml` (cabeçalho, `SupportMapFragment`, botões).

**Pronto quando:** o mapa muda para satélite depois de configurar e voltar (`onResume`); os 3 botões abrem as telas.

### Etapa 4 — Banco de dados

**Arquivos:** `model/Trilha.java`, `model/Ponto.java`, `model/TrilhaDbHelper.java` (`onCreate`, `onUpgrade`, `inserirTrilha`, `finalizarTrilha`, `inserirPonto`, `listarTrilhas`, `buscarTrilha`, `listarPontos`, `renomearTrilha`, `apagarTrilha`, `apagarIntervalo`, `apagarTodas`).

**Pronto quando:** apagar uma trilha não deixa pontos órfãos; dados persistem depois de fechar o app.

### Etapa 5 — Registrar trilha

**Arquivos:** `RegistrarTrilhaActivity.java`, `layout/activity_registrar_trilha.xml`.

Roteiro (slide 7): permissão → cliente → `LocationRequest` (alta precisão, 1 s) → `LocationCallback` → `requestLocationUpdates`; parar com `removeLocationUpdates`.

**Pronto quando:** o círculo muda de tamanho com a acurácia; a velocidade máxima nunca diminui; o cronômetro e a distância param ao tocar em *Parar*; a trilha aparece no banco com média, máxima e pontos.

### Etapa 6 — Minhas trilhas

**Arquivos:** `TrilhasActivity.java`, `layout/activity_trilhas.xml`, `layout/item_trilha.xml` (linhas inflados dentro de um `ScrollView`, como foi feito na Av2).

**Pronto quando:** lista vazia mostra mensagem; editar pede o novo nome num diálogo; apagar sempre pede confirmação; apagar intervalo funciona com um único dia e com datas invertidas (é rejeitado com `Toast`).

### Etapa 7 — Visualizar trilha

**Arquivos:** `VisualizarTrilhaActivity.java`, `layout/activity_visualizar_trilha.xml`.

**Pronto quando:** o percurso inteiro cabe na tela (`LatLngBounds`); trilha com 0 ou 1 ponto não derruba o app; o cartão mostra início, vel. média, vel. máxima, distância e duração.

### Etapa 8 — Acabamento e defesa

- Comentários nas partes difíceis (callback de localização, cálculo de distância, SQL, marcador no Canvas).
- `README.md` com como configurar a chave, como rodar e as justificativas.
- Teste manual completo (seção 5) em inglês **e** em português.
- Ensaio da defesa (seção 6).

## 5. Roteiro de testes (checklist final)

| # | Cenário | Esperado |
|---|---|---|
| 1 | Primeira abertura | Mapa vetorial; configurações em *vetorial* / *North Up* |
| 2 | Configurar *satélite* + *Course Up*, fechar e reabrir | Escolha mantida; mapa principal em satélite |
| 3 | Registrar: negar a permissão | `Toast` e volta à tela anterior |
| 4 | Registrar: conceder a permissão | Marcador (seta) e círculo de acurácia aparecem |
| 5 | Iniciar e mover (emulador: rota simulada) | Velocidade, máxima, cronômetro e distância sobem |
| 6 | *Course Up* durante o deslocamento | Mapa gira com o rumo; seta fica para cima |
| 7 | Parar | Trilha salva; `Toast`; volta |
| 8 | Minhas trilhas | A trilha aparece com nome, data e distância |
| 9 | Editar nome (vazio / válido) | Vazio é recusado; válido atualiza a lista |
| 10 | Ver trilha | Polilinha completa, início/fim, cartão com os 5 dados |
| 11 | Apagar uma trilha | Pede confirmação; some da lista e do banco (e seus pontos) |
| 12 | Apagar intervalo e apagar todas | Só as trilhas do intervalo / todas somem |
| 13 | Trocar o idioma do aparelho (EN ↔ PT) | Todos os textos, datas e botões mudam |
| 14 | Sair da tela de registro gravando | A trilha é finalizada e salva |

## 6. Perguntas prováveis na defesa

- Por que `FusedLocationProviderClient` e não `LocationManager`? O que é `LocationRequest`/`LocationCallback`?
- Por que a permissão é pedida em tempo de execução? O que acontece se for negada?
- Como a velocidade média é calculada e por quê? Como a distância é somada (`distanceTo`)?
- Como o marcador personalizado é criado? Por que `Canvas`?
- Como funciona *Course Up* (`CameraPosition` com `bearing`)? E *North Up*?
- Por que duas tabelas? Qual a chave estrangeira? Como apagar sem deixar pontos órfãos?
- Para que serve `onUpgrade`? O que é `ContentValues`? Por que fechar o `Cursor`?
- Como o app escolhe o idioma?
- Por que `SharedPreferences` nas configurações e SQLite nas trilhas?

## 7. Cronograma sugerido (hoje: 23/11 → entrega: 26/11)

| Dia | Atividade |
|---|---|
| Seg 23/11 | Etapas 1 a 4 |
| Ter 24/11 | Etapas 5 e 6 |
| Qua 25/11 | Etapas 7 e 8, teste no aparelho em EN e PT, ensaio |
| Qui 26/11 | **Entrega e defesa** |
