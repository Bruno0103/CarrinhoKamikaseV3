# CarrinhoV3

Projeto de controle de um carrinho robô com interface Android e firmware ESP32.

## Visão Geral

Este projeto reúne duas partes principais:

- `CarrinhoV3App/`: app Android em Jetpack Compose para controlar o carrinho via Bluetooth.
- `CarrinhoV3Embarcado/`: firmware Arduino para ESP32 que recebe comandos Bluetooth e controla motor DC, servo e sensor ultrassônico.

## Recursos

- Controle de velocidade do motor via slider.
- Controle da direção do servo via slider.
- Conexão Bluetooth SPP (Serial Port Profile) com dispositivo emparelhado.
- Leitura de distância com sensor ultrassônico (HC-SR04) para evitar colisões.
- Suporte básico a motor L298N e servo motor.

## Estrutura do Projeto

- `CarrinhoV3App/`
  - `app/`: módulo Android principal.
  - `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`: configuração do projeto Gradle.

- `CarrinhoV3Embarcado/`
  - `CarrinhoV3Embarcado.ino`: sketch Arduino para ESP32.

## Requisitos

- Android Studio
- SDK Android 36
- Kotlin e Jetpack Compose
- Java 11
- ESP32 com suporte Bluetooth
- Biblioteca Arduino: `BluetoothSerial`, `ESP32Servo`, `L298N`, `NewPing`

## Como usar

### Android

1. Abra `CarrinhoV3App` no Android Studio.
2. Sincronize o Gradle.
3. Execute o app em um dispositivo Android com Bluetooth habilitado.
4. Emparelhe seu ESP32 no sistema Android antes de conectar.
5. No app, selecione o dispositivo emparelhado e pressione conectar.
6. Ajuste os sliders para enviar comandos de velocidade e direção.

### Firmware ESP32

1. Abra `CarrinhoV3Embarcado/CarrinhoV3Embarcado.ino` na Arduino IDE ou no VS Code com PlatformIO.
2. Instale as bibliotecas necessárias se ainda não estiverem instaladas.
3. Compile e carregue para a placa ESP32.
4. Conecte o servo, motor DC via L298N e sensores nos pinos definidos no sketch.

## Observações

- O app já possui as permissões de Bluetooth declaradas no `AndroidManifest.xml`, incluindo `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` e permissões de localização.
- O método de envio de comandos está preparado para enviar 3 bytes: dois bytes de velocidade e um byte de direção.
- A lógica de segurança do firmware para evitar colisões aciona o motor de ré quando a distância é menor ou igual a 10 cm.
- O código atual ainda contém `@SuppressLint("MissingPermission")` para simplificar testes; deve-se tratar permissões runtime adequadamente em produção.

## Melhoria futura

- Adicionar solicitação de permissão de Bluetooth em runtime.
- Implementar feedback de status de conexão mais detalhado.
- Mostrar valores de distância e status de sensor no app.
- Refatorar para maior separação entre UI e lógica de conexão.
