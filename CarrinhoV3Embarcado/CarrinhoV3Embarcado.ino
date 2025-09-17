#include "BluetoothSerial.h"
#include <ESP32Servo.h>
#include <L298N.h>
#include <cstdint>
#include <NewPing.h>

// --- PINOS ---
const int pinoServo = 15;
const int pinoEN = 14;
const int pinoINA = 12;
const int pinoINB = 13;
const int pinoTrig = 2;
const int pinoEcho = 4;

// --- CONFIGURAÇÕES ---
String deviceName = "Carrinho Kamikase";
const int MAX_DISTANCIA = 50; // Distância máxima a ser medida (em cm)

// --- OBJETOS E VARIÁVEIS GLOBAIS ---
BluetoothSerial SerialBT;
Servo servo;
L298N motorDC(pinoEN, pinoINA, pinoINB);

// CRIAR O OBJETO DO SENSOR COM A NewPing
NewPing sonar(pinoTrig, pinoEcho, MAX_DISTANCIA);

// --- FUNÇÕES ---

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

void setup() {
  Serial.begin(115200);
  SerialBT.begin(deviceName);
  Serial.println("Carrinho pronto.");

  servo.attach(pinoServo);
  motorDC.setSpeed(0);
}

void loop() {
 
  //Serial.print("Distancia: ");
  //Serial.println(sonar.ping_cm());
  int distanciaAtual = sonar.ping_cm();
  
  // --- PROCESSAR COMANDOS BLUETOOTH ---
  if (SerialBT.available() >= 3) {
    uint8_t buffer[3];
    SerialBT.readBytes(buffer, 3);

    int16_t velocidade = (buffer[0] << 8) | buffer[1];
    uint8_t direcao = buffer[2];

    servo.write(direcao);

    // Lógica de decisão principal
    if (distanciaAtual <= 20 && distanciaAtual != 0 && velocidade > 0) {
      motorDC.stop();
    } else {
      controlarMotor(velocidade);
    }
  }
  if (distanciaAtual <= 10 && distanciaAtual != 0) {
    motorDC.backward();
    delay(1000);
    motorDC.stop();
  }
}