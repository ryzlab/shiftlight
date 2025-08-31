
#include <SPI.h>
#include "mcp_can.h"
#include "RPMReader.h"

const int CS_PIN = 10;   // MCP2515 CS pin
const int INT_PIN = 2;   // MCP2515 INT pin

RPMReader rpmReader(CS_PIN, INT_PIN);

void setup() {
  Serial.begin(115200);
  while (!Serial) {}
  rpmReader.init();
}

unsigned long last = millis();

void loop() {
  rpmReader.loop();
  if (millis() - last > 10) {
    last = millis();
    int currentRpm = rpmReader.getCurrentRpm();
    Serial.println(currentRpm);
  }
}