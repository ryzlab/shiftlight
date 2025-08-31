#include <CAN.h> // the OBD2 library depends on the CAN library
#include <OBD2.h> // Install ODB2 by Sandeep Mistry
#include <math.h>

/*
+------------------+---------+
| Microchip MCP2515| Arduino |
+------------------+---------+
| VCC              | 5V      |
| GND              | GND     |
| SCK              | SCK     |
| SO               | MISO    |
| SI               | MOSI    |
| CS               | 10      |
| INT              | 2       |
+------------------+---------+
*/


#define LED_COUNT 13
#define LED_PIN   4

void setup() {
  Serial.begin(9600);
  while (!Serial);

  while (true) {
    Serial.print("Connecting to ODB2 CAN bus..2.");

    if (!OBD2.begin()) {
      Serial.println(" failed! Sleep and retry.");

      delay(1000);
    } else {
      Serial.println(" OK");
      break;
    }
  }

  Serial.println();
}

void loop() {
  Serial.print("RPM: ");
  Serial.println(readRpm());
}

float readRpm() {
  Serial.print(OBD2.pidName(ENGINE_RPM));
  float rpm = OBD2.pidRead(ENGINE_RPM);
  if (isnan(rpm)) {
    return NAN;
  }
  return rpm;
}
