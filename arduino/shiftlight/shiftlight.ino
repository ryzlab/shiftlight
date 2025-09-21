
#include <SPI.h>
#include "mcp_can.h"
#include "RPMReader.h"
#include <ArduinoJson.h>
#include <Adafruit_NeoPixel.h>

const int CS_PIN = 10;   // MCP2515 CS pin
const int INT_PIN = 2;   // MCP2515 INT pin

RPMReader rpmReader(CS_PIN, INT_PIN);

#define LED_PIN 4
#define NUM_LEDS 13

Adafruit_NeoPixel strip = Adafruit_NeoPixel(NUM_LEDS, LED_PIN, NEO_GRB + NEO_KHZ800);

void setup() {
  Serial.begin(9600);
  while (!Serial) {}
  //rpmReader.init();
  strip.begin();
  strip.show(); // Initialize all pixels to 'off'
}

unsigned long last = millis();

String inputString = "";  // A String to hold incoming data

void loop() {
  /*rpmReader.loop();
  if (millis() - last > 10) {
    last = millis();
    int currentRpm = rpmReader.getCurrentRpm();
    Serial.println(currentRpm);
  }*/

  // Read from serial until a new-line character is encountered
  while (Serial.available()) {
    char inChar = (char)Serial.read();
    inputString += inChar;
    if (inChar == '\n') {
      parseAndHandleString((byte*)inputString.c_str(), inputString.length());
      inputString = "";  // Clear the string for the next input
    }
  }
}

void parseAndHandleString(byte* byteArray, size_t length) {
  // Convert byte array to String
  String inputString = String((char*)byteArray);
  inputString.trim(); // Remove trailing new-line

  if (inputString == "{}") {
    handleEmptyJson();
  } else if (inputString.startsWith("{\"rpm\":")) {
    StaticJsonDocument<200> doc;
    DeserializationError error = deserializeJson(doc, inputString);
    if (!error) {
      int rpm = doc["rpm"];
      handleRpmJson(rpm);
    }
  } else {
    StaticJsonDocument<200> doc;
    DeserializationError error = deserializeJson(doc, inputString);
    if (!error) {
      int ring1 = doc["ring1"];
      int ring2 = doc["ring2"];
      int ring3 = doc["ring3"];
      int ring4 = doc["ring4"];
      int offset = doc["offset"];
      handleComplexJson(ring1, ring2, ring3, ring4, offset);
    }
  }
}

void handleEmptyJson() {
  delay(500);
  // Generate JSON and print it
  String jsonString = generateJson();
  //setLedBrightness(3000);
  Serial.println(jsonString);
}

void handleRpmJson(int rpm) {
  // Call setLedBrightness with the RPM value
  Serial.print("# Setting led brightness to ");
  Serial.println(rpm);
  setLedBrightness(rpm);
  Serial.println("{}");
}

void handleComplexJson(int ring1, int ring2, int ring3, int ring4, int offset) {
  Serial.print("# Received rings ");
  Serial.print(ring1);
  Serial.print(", ");
  Serial.print(ring2);
  Serial.print(", ");
  Serial.print(ring3);
  Serial.print(", ");
  Serial.print(ring4);
  Serial.print(", offset: ");
  Serial.println(offset);
  Serial.println("{}");
  // Implement the function to handle complex JSON
}

void setLedBrightness(int value) {
  int numLedsToLight = map(value, 0, 5000, 0, NUM_LEDS);
  for (int i = 0; i < NUM_LEDS; i++) {
    if (i < numLedsToLight) {
      strip.setPixelColor(i, strip.Color(255, 0, 0)); // Red color
    } else {
      strip.setPixelColor(i, strip.Color(0, 0, 0)); // Off
    }
  }
  strip.show();
}

String generateJson() {
  StaticJsonDocument<200> doc;
  doc["ring1"] = 1234;
  doc["ring2"] = 1234;
  doc["ring3"] = 1234;
  doc["ring4"] = 1234;
  doc["offset"] = 234;
  String jsonString;
  serializeJson(doc, jsonString);
  return jsonString;
}