#include "BluetoothSerial.h"
#include <ESP32Servo.h>
#include <L298N.h>
#include <cstdint>

//Nome do Bluetooth:
String deviceName= "Carrinho Kamikase de Bruno";
// Definições de pinos:
int ServoPin = 15;
int EN = 14;
int INA = 12;
int INB = 13;

BluetoothSerial SerialBT;
Servo servo;
L298N PWM(EN, INA, INB);

void motor(int16_t velocidade){
  if (velocidade > 0){
    PWM.forward();
    PWM.setSpeed(velocidade);
  } else if (velocidade < 0 ){
    PWM.backward();
    PWM.setSpeed(-velocidade);
  } else {
    PWM.stop();
  }
}

void setup() {
  //Canal da comunicação serial
  Serial.begin(115200);
  //Inicialização do Bluetooth
  SerialBT.begin(deviceName);
  Serial.println("Bluetooth iniciado. Aguardando dados...");
  //Definições de parametros iniciais
  servo.attach(ServoPin);
  PWM.setSpeed(0);
}

void loop() {
  if (SerialBT.available() >= 3) {
    uint8_t buffer[3];
    SerialBT.readBytes(buffer, 3); // Lê os 3 bytes da mensagem

    int16_t velocidade = (buffer[0] << 8) | buffer[1]; // Reconstrói o int16_t
    uint8_t direção = buffer[2];                    // Lê o uint8_t

    Serial.print("Short velocidade recebido: ");
    Serial.println(velocidade);
    Serial.print("Byte direção recebido: ");
    Serial.println(direção);

    // Aqui estão os valores para controlar motores e servos.
    servo.write(direção);
    motor(velocidade);
  }
}
