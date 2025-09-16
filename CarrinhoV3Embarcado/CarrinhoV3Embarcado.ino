#include "BluetoothSerial.h"
#include <ESP32Servo.h>
#include <L298N.h>
#include <cstdint>

// Nome do Bluetooth:
String deviceName = "Carrinho Kamikase";

// --- PINOS ---
// Servo motor
const int pinoServo = 15;
// Ponte H L298N para o motor DC
const int pinoEN = 14;
const int pinoINA = 12;
const int pinoINB = 13;
// Sensor Ultrassônico HC-SR04
const int pinoTrig = 2;
const int pinoEcho = 4;

// --- CONFIGURAÇÕES DO SENSOR ---
const float VELOCIDADE_SOM = 0.0343; // cm/µs
const long INTERVALO_MEDICAO = 100; // Medir a cada 100ms
volatile long tempoInicioEcho = 0;
volatile long tempoFimEcho = 0;
volatile boolean novaMedicaoPronta = false;

// --- VARIÁVEIS GLOBAIS ---
BluetoothSerial SerialBT;
Servo servo;
L298N motorDC(pinoEN, pinoINA, pinoINB);
float distanciaAtual = 100.0; // Armazena a última distância medida
long tempoUltimaMedicao = 0;

// --- FUNÇÕES AUXILIARES ---

// ISR (Rotina de Interrupção) para capturar o pulso do sensor
// Executa em paralelo, sem travar o código principal
void IRAM_ATTR isr_echo() {
  if (digitalRead(pinoEcho) == HIGH) {
    tempoInicioEcho = micros();
  } else {
    tempoFimEcho = micros();
    novaMedicaoPronta = true;
  }
}

// Controla o motor DC (frente, ré, parar)
void controlarMotor(int16_t velocidade) {
  if (velocidade > 0) {
    motorDC.forward();
    motorDC.setSpeed(velocidade);
  } else if (velocidade < 0) {
    motorDC.backward();
    motorDC.setSpeed(-velocidade);
  } else {
    motorDC.stop();
  }
}

// --- LÓGICA PRINCIPAL DO SENSOR ULTRASSÔNICO ---
// Esta função faz tudo relacionado ao sensor: dispara, calcula e atualiza a distância.
void atualizarLeituraUltrassonica() {
  // 1. Dispara um novo pulso a cada 'INTERVALO_MEDICAO' milissegundos
  if (millis() - tempoUltimaMedicao >= INTERVALO_MEDICAO) {
    tempoUltimaMedicao = millis();
    
    digitalWrite(pinoTrig, LOW);
    delayMicroseconds(2);
    digitalWrite(pinoTrig, HIGH);
    delayMicroseconds(10);
    digitalWrite(pinoTrig, LOW);
  }

  // 2. Se a interrupção avisou que uma medição terminou, calcula a distância
  if (novaMedicaoPronta) {
    long duracao = tempoFimEcho - tempoInicioEcho;
    distanciaAtual = (duracao * VELOCIDADE_SOM) / 2.0;
    novaMedicaoPronta = false; // Prepara para a próxima leitura

    // (Opcional) Imprime no monitor serial para depuração
    Serial.print("Distancia: ");
    Serial.print(distanciaAtual);
    Serial.println(" cm");
  }
}

// --- LÓGICA DE CONTROLE VIA BLUETOOTH ---
// Esta função lê os comandos e decide o que fazer com o carrinho.
void processarComandosBluetooth() {
  if (SerialBT.available() >= 3) {
    uint8_t buffer[3];
    SerialBT.readBytes(buffer, 3);

    int16_t velocidade = (buffer[0] << 8) | buffer[1];
    uint8_t direcao = buffer[2];

    servo.write(direcao);

    // LÓGICA DE DECISÃO PRINCIPAL (SIMPLIFICADA)
    // Se a distância for menor ou igual a 5 cm E o comando for para FRENTE...
    if (distanciaAtual <= 5.0 && velocidade > 0) {
      // ...então, impeça o movimento, parando o motor.
      motorDC.stop();
    } else {
      // Para qualquer outra situação (sem obstáculo, ou andando de ré), obedeça o comando.
      controlarMotor(velocidade);
    }
  }
}

// --- SETUP E LOOP ---

void setup() {
  Serial.begin(115200);
  SerialBT.begin(deviceName);
  Serial.println("Carrinho pronto.");

  servo.attach(pinoServo);
  pinMode(pinoTrig, OUTPUT);
  pinMode(pinoEcho, INPUT);

  // Configura a interrupção que vai medir o tempo de resposta do sensor
  attachInterrupt(digitalPinToInterrupt(pinoEcho), isr_echo, CHANGE);
  
  motorDC.setSpeed(0);
}

void loop() {
  // A lógica agora é simples:
  // 1. Cuide do sensor.
  atualizarLeituraUltrassonica();
  
  // 2. Cuide dos comandos.
  processarComandosBluetooth();
}
