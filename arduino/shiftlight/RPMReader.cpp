#include "RPMReader.h"

volatile bool RPMReader::canMsgReceived = false;

RPMReader::RPMReader(byte csPin, byte intPin) : CAN(csPin), intPin(intPin) {
  currentRpm = -1;
  pollInterval = 100;
  lastRequest = 0; 
}

bool RPMReader::init() {
  if (CAN.begin(MCP_STDEXT, CAN_500KBPS, MCP_8MHZ) == CAN_OK) {
    Serial.println("CAN init OK");
    CAN.setMode(MCP_NORMAL);
    delay(100);

    // Attach INT pin
    pinMode(intPin, INPUT);
    attachInterrupt(digitalPinToInterrupt(intPin), RPMReader::MCP_ISR, FALLING);
  } else {
    Serial.println("CAN init FAIL");
    while (1);
  }
}

void RPMReader::requestRPM() {
  // Mode 01, PID 0C = RPM
  byte txData[8] = {0x02, 0x01, 0x0C, 0, 0, 0, 0, 0};
  CAN.sendMsgBuf(0x7DF, 0, 8, txData);
}

int RPMReader::getCurrentRpm() {
  return currentRpm;
}

void RPMReader::loop() {
  // Poll ECU at max safe rate (~10 Hz)
  if (millis() - lastRequest >= pollInterval) {
    requestRPM();
    lastRequest = millis();
  }

  // Handle incoming CAN frames
  if (canMsgReceived) {
    canMsgReceived = false;

    long unsigned rxId;
    unsigned char len;
    unsigned char buf[8];

    while (CAN.checkReceive() == CAN_MSGAVAIL) {
      if (CAN.readMsgBuf(&rxId, &len, buf) == CAN_OK) {

        // Filter for RPM responses
        if (rxId >= 0x7E8 && rxId <= 0x7EF) {
          if (len >= 5 && buf[1] == 0x41 && buf[2] == 0x0C) {
            int rpm = ((buf[3] * 256) + buf[4]) / 4;
            currentRpm = rpm;
          }
        }

      }
    }
  }
}

// Interrupt service routine
static void RPMReader::MCP_ISR() {
  canMsgReceived = true;
}



