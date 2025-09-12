#include <SPI.h>
#include "mcp_can.h"

const int CS_PIN = 10;   // Chip select pin for MCP2515
MCP_CAN CAN(CS_PIN);

unsigned long lastRequest = 0;
unsigned long lastResponse = 0;
bool ignitionOn = false;

void setup() {
  Serial.begin(115200);
  while (!Serial) {}

  Serial.println("Initializing CAN...");

  if (CAN.begin(MCP_STDEXT, CAN_500KBPS, MCP_8MHZ) == CAN_OK) {
    Serial.println("CAN init OK");
  } else {
    Serial.println("CAN init FAIL");
    while (1);
  }

  CAN.setMode(MCP_NORMAL);  // Go online
  delay(100);
}

void requestRPM() {
  // Mode 01, PID 0C = RPM
  byte txData[8] = {0x02, 0x01, 0x0C, 0, 0, 0, 0, 0};
  CAN.sendMsgBuf(0x7DF, 0, 8, txData);
}

void loop() {
  // Send request once per second
  if (millis() - lastRequest > 50) {
    requestRPM();
    lastRequest = millis();
  }

  // Poll for responses
  long unsigned rxId;
  unsigned char len;
  unsigned char rxBuf[8];

  if (CAN.checkReceive() == CAN_MSGAVAIL) {
    if (CAN.readMsgBuf(&rxId, &len, rxBuf) == CAN_OK) {
      if (rxId >= 0x7E8 && rxId <= 0x7EF) {
        if (rxBuf[1] == 0x41 && rxBuf[2] == 0x0C) {
          // Got a valid RPM response
          int rpm = ((rxBuf[3] * 256) + rxBuf[4]) / 4;
          Serial.print("RPM: ");
          Serial.println(rpm);

          ignitionOn = true;
          lastResponse = millis();
        }
      }
    }
  }

  // Check for timeout → ignition off
  if (ignitionOn && millis() - lastResponse > 3000) {
    ignitionOn = false;
    Serial.println("Ignition OFF detected");
  }
}
