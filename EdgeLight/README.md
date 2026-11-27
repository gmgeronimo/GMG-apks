# Edge Light

App Android que acende uma linha fina de luz percorrendo a borda da tela, e um anel em volta da câmera frontal, quando chega uma notificação.

## Como abrir e instalar

1. Instale o **Android Studio** (versão Ladybug ou mais nova).
2. Em *File › Open*, abra esta pasta (`EdgeLight`). Na primeira vez, o Android Studio baixa o Gradle e as bibliotecas sozinho, o que leva alguns minutos.
3. No celular, ative o **modo desenvolvedor** e a **depuração USB**. No HyperOS: *Configurações › Sobre o telefone › toque 7 vezes em "Versão do SO"*. Depois: *Configurações adicionais › Opções do desenvolvedor › Depuração USB* (e também *Instalar via USB*).
4. Conecte o celular no cabo e aperte ▶ (*Run*) no Android Studio.

Para gerar um APK e instalar sem cabo: *Build › Build App Bundle(s) / APK(s) › Build APK(s)*.

## Permissões no celular

O próprio app mostra o que falta e leva até a tela certa:

- **Acesso às notificações**: para saber quando chega uma notificação.
- **Acessibilidade › Edge Light**: é o que permite desenhar a luz por cima de tudo, inclusive da tela de bloqueio. O serviço não lê nada da tela.
  - Se a opção aparecer bloqueada (Android 13+ com app instalado fora da loja): *Informações do app › menu ⋮ › Permitir configurações restritas*, e tente de novo.

No HyperOS, para o sistema não fechar o app em segundo plano:

- *Informações do app › Início automático*: ligado.
- *Informações do app › Economia de bateria*: **Sem restrições**.

## O que já funciona

- Luz da borda com sentido (horário ou anti-horário), cor sólida ou degradê de 4 cores, espessura, comprimento do traço, tempo de volta, número de voltas, brilho e rastro.
- Ritmo constante, acelerando ou desacelerando.
- Anel em volta da câmera frontal, com sentido, cor, espessura, comprimento e velocidade próprios. O app encontra o furo da câmera pelo próprio sistema. Em aparelhos com entalhe largo (notch) ou sem furo, o anel fica desativado.
- Cantos da luz acompanhando o arredondamento real da tela (Android 12+).
- Temas do app: Noturno, Papel e Sistema (cores do papel de parede, Android 12+), além da opção de mudar junto com o modo claro/escuro.
- Notificações ignoradas: as fixas (música, download, chamada), resumos de grupo, silenciosas e as do próprio app. Entre uma luz e outra há um intervalo mínimo de 1,5 s.

## Limites desta versão

- Com a **tela apagada** a luz não aparece. Acender a tela ou desenhar sobre o Always-On Display é o próximo passo.
- O código não foi compilado no ambiente em que foi escrito. Se o Android Studio apontar algum erro na primeira compilação, é só me mandar a mensagem.

## Estrutura

| Arquivo | O que faz |
| --- | --- |
| `Settings.kt` | Todos os ajustes e como são guardados |
| `LightRenderer.kt` | Desenha a luz e o anel (usado no overlay e na prévia) |
| `EdgeLightView.kt` | Overlay de tela cheia; acha o furo da câmera |
| `EdgeLightService.kt` | Serviço de acessibilidade que coloca o overlay na tela |
| `NotifListener.kt` | Recebe as notificações e decide quando acender |
| `ui/Theme.kt` | Os três temas do app |
| `ui/Components.kt` | Botões conectados, sliders, cores, prévia animada |
| `MainActivity.kt` | Tela de ajustes |

Fontes Mona Sans e Nunito: licença SIL Open Font License. Elas são baixadas na hora da compilação (veja `tools/fontes.py`).
