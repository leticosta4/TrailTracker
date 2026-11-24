# TrailTracker

Aplicativo Android em Java para registrar, consultar e visualizar trilhas (percursos) com GPS
e Google Maps. Trabalho da **Avaliação 3** de Computação para Dispositivos Móveis.

O app tem **português e inglês**: o idioma é escolhido automaticamente a partir do idioma do
aparelho (inglês é o padrão para qualquer outro idioma).

## Telas

| Atividade | Função |
|---|---|
| `MainActivity` | Mapa e três botões: Minhas trilhas, Iniciar trilha, Configurações |
| `ConfiguracaoActivity` | Tipo de mapa (vetorial / satélite) e navegação (North Up / Course Up) |
| `RegistrarTrilhaActivity` | Posição no mapa (marcador personalizado + círculo de acurácia), velocidade, velocidade máxima, cronômetro, distância e gravação da trilha |
| `TrilhasActivity` | Lista de trilhas; renomear; apagar uma, um período ou todas |
| `VisualizarTrilhaActivity` | Percurso no mapa e, sobreposto, início, velocidade média e máxima, distância e duração |

## Como executar

1. Obtenha uma chave do Google Maps SDK for Android:
   <https://developers.google.com/maps/documentation/android-sdk/get-api-key>
2. No arquivo `local.properties` (na raiz do projeto, **não vai para o Git**), acrescente:
   ```
   MAPS_API_KEY=AIza...sua_chave...
   ```
3. Abra no Android Studio e execute em um dispositivo ou emulador **com Google Play Services**
   (no emulador, use uma imagem "Google APIs" ou "Google Play").
4. No emulador, simule o deslocamento em *Extended controls > Location* (por exemplo, carregando
   uma rota GPX) para testar a gravação da trilha.

Sem a chave o app abre, mas o mapa aparece em branco.

## Decisões de projeto

- **SQLite para as trilhas**: o enunciado exige duas tabelas. `trilhas` guarda os dados
  gerais (nome, início, fim, velocidade média, velocidade máxima, distância) e `pontos` guarda o
  percurso (latitude, longitude, instante), ligada à trilha por `trilha_id`. Cada ponto é gravado
  assim que chega, então o percurso não se perde se o app for interrompido.
- **SharedPreferences para as configurações**: são só dois valores simples (tipo de mapa e
  navegação), lidos por várias telas. Também guarda a última posição, para centralizar o mapa
  inicial.
- **Velocidade média = distância total ÷ duração.** Fica estável mesmo com leituras de
  velocidade ruins ou com o usuário parado.
- **Leituras com acurácia pior que 50 m** e deslocamentos menores que 2 m (tremor do GPS) não
  entram na distância nem no percurso.
- **Marcador personalizado**: uma seta desenhada com `Canvas` e entregue ao mapa como `Bitmap`.
- **Navegação** North Up / Course Up vale na tela de registro. Nas telas Principal e Visualizar o
  mapa não acompanha deslocamento do usuário, então o norte fica sempre para cima.

## Limitações conhecidas

- **Tela ligada durante a gravação.** Sem um serviço em primeiro plano (fora do conteúdo da
  disciplina), o Android reduz as atualizações de localização quando o app vai para segundo plano.
  Por isso a tela fica acesa enquanto grava.
- **SDK mínimo 33** (herdado do projeto base). O enunciado cita "SDK min 17", mas as bibliotecas
  de localização e mapas atuais exigem no mínimo a API 21.

## Estrutura

```
app/src/main/java/com/example/trailtracker
├── MainActivity, ConfiguracaoActivity, RegistrarTrilhaActivity,
│   TrilhasActivity, VisualizarTrilhaActivity
└── model/  Trilha, Ponto, TrilhaDbHelper (SQLite)
```

O plano de implementação, dividido em etapas, está em [`docs/plano-implementacao.md`](docs/plano-implementacao.md).
