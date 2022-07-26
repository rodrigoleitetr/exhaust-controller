//This example code is in the Public Domain (or CC0 licensed, at your option.)
//By Evandro Copercini - 2018
//
//This example creates a bridge between Serial and Classical Bluetooth (SPP)
//and also demonstrate that SerialBT have the same functionalities of a normal Serial

#include "BluetoothSerial.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

BluetoothSerial SerialBT;

const int relay_1 = 32;
const int relay_2 = 33;

String message = "";
String sizeStr = "";
char incomingChar;
int lastKnownvalvePosition = 0;

void setup() {
  Serial.begin(115200);
  SerialBT.begin("Exhaust_Controller");
  Serial.println("The device started, now you can pair it with bluetooth!");

  pinMode(relay_1, OUTPUT);
  pinMode(relay_2, OUTPUT);
}

void loop() {
  if (SerialBT.available()) {
    char incomingChar = SerialBT.read();
    
    // Reading the message
    if (incomingChar != '\n'){
      message += String(incomingChar);
    }
    else
    {      
        Serial.println(message);
        
        if (message.indexOf("move_") >= 0){
          hanglePartialMove();
        }
        
        message = "";
      }
    }        
  delay(20);
}

void closeValve()
{
  digitalWrite(relay_1, LOW);
  digitalWrite(relay_2, HIGH);
  delay(400);
  digitalWrite(relay_2, LOW);
  lastKnownvalvePosition = 0;
}

void halfOpenValve()
{
  digitalWrite(relay_2, LOW);
  digitalWrite(relay_1, HIGH);
  delay(180);
  digitalWrite(relay_1, LOW);
  lastKnownvalvePosition = 3;
}

void hanglePartialMove()
{
  int valvePosition = message.substring(5, 6).toInt();
  if(valvePosition == 0 && lastKnownvalvePosition != 0){
    closeValve();
  } else if(valvePosition == 6 && lastKnownvalvePosition != 6){
    openValve();
  } else {
    closeValve();
    customOpenValve(valvePosition);
  }
}

void customOpenValve(int refVal)
{
  digitalWrite(relay_2, LOW);
  digitalWrite(relay_1, HIGH);
  int moveTime = 80;
  switch (refVal) {
    case 1:
      moveTime = 80;
      break;
    case 2:
      moveTime = 120;
      break;
    case 3:
      moveTime = 160;
      break;
    case 4:
      moveTime = 200;
      break;
    case 5:
      moveTime = 240;
      break;
    default:
      moveTime = 400;
      break;
  }
  delay(moveTime);
  digitalWrite(relay_1, LOW);
  lastKnownvalvePosition = refVal;
}

void openValve()
{
  digitalWrite(relay_2, LOW);
  digitalWrite(relay_1, HIGH);
  delay(400);
  digitalWrite(relay_1, LOW);
  lastKnownvalvePosition = 5;
}
