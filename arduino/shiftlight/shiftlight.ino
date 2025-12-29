
#include <SPI.h>
#include "mcp_can.h"
#include "RPMReader.h"
#include <ArduinoJson.h>
#include <Adafruit_NeoPixel.h>
#include "Display.h"

// Error and success message prefixes
const char* ERROR_PREFIX = "ERR: ";

const int CS_PIN = 10;   // MCP2515 CS pin
const int INT_PIN = 2;   // MCP2515 INT pin

RPMReader rpmReader(CS_PIN, INT_PIN);

#define LED_PIN 4
#define NUM_LEDS 13

Adafruit_NeoPixel strip = Adafruit_NeoPixel(NUM_LEDS, LED_PIN, NEO_GRB + NEO_KHZ800);

Display display(&strip);  // Pass pointer to strip

const int MAX_LINE_LENGTH = 256;
char inputBuffer[MAX_LINE_LENGTH + 1];  // Buffer to hold incoming data
int inputIndex = 0;  // Current position in buffer
bool discardingLine = false;  // Flag to track if we're discarding a line
bool readingImages = false;  // Flag to track if we're in image reading mode

bool readSerialLine(char* line, int lineSize) {
  // Clear the output parameter
  if (line != nullptr && lineSize > 0) {
    line[0] = '\0';
  }
  
  // Only process one character per call (non-blocking)
  if (!Serial.available()) {
    return false;
  }
  
  char inChar = (char)Serial.read();
  
  if (inChar == '\n' || inChar == '\r') {
    if (discardingLine) {
      // Finished discarding the long line, reset flag
      discardingLine = false;
      inputIndex = 0;
      return false;
    }
    
    // Process the line if we have data
    if (inputIndex > 0) {
      // Null-terminate the buffer
      inputBuffer[inputIndex] = '\0';
      
      // Find start of non-whitespace
      int start = 0;
      while (start < inputIndex && (inputBuffer[start] == ' ' || inputBuffer[start] == '\t')) {
        start++;
      }
      
      // Find end of non-whitespace
      int end = inputIndex - 1;
      while (end >= start && (inputBuffer[end] == ' ' || inputBuffer[end] == '\t' || inputBuffer[end] == '\r' || inputBuffer[end] == '\n')) {
        end--;
      }
      
      // Calculate trimmed length
      int trimmedLength = end - start + 1;
      
      // Ignore empty lines (after trimming)
      if (trimmedLength <= 0) {
        inputIndex = 0;
        return false;
      }
      
      // Ignore lines starting with #
      if (inputBuffer[start] == '#') {
        inputIndex = 0;
        return false;
      }
      
      // Copy trimmed line to output buffer
      if (line != nullptr && lineSize > 0) {
        int copyLength = (trimmedLength < lineSize - 1) ? trimmedLength : lineSize - 1;
        strncpy(line, &inputBuffer[start], copyLength);
        line[copyLength] = '\0';
      }
      
      inputIndex = 0;
      return true;
    }
  } else {
    if (discardingLine) {
      // Continue discarding characters until newline
      return false;
    }
    
    // Add character to buffer if there's space
    if (inputIndex < MAX_LINE_LENGTH) {
      inputBuffer[inputIndex++] = inChar;
    } else {
      // Buffer full, start discarding
      inputIndex = 0;
      discardingLine = true;
    }
  }
  
  // No line ready yet
  return false;
}

void setup() {
  Serial.begin(9600);
  while (!Serial) {}
  //rpmReader.init();
  strip.begin();
  strip.show(); // Initialize all pixels to 'off'
}

unsigned long last = millis();

void loop() {
  /*rpmReader.loop();
  if (millis() - last > 10) {
    last = millis();
    int currentRpm = rpmReader.getCurrentRpm();
    Serial.println(currentRpm);
  }*/

  char line[MAX_LINE_LENGTH + 1];
  if (readSerialLine(line, sizeof(line))) {
    // Check for rpm command
    if (strncmp(line, "rpm=", 4) == 0) {
      const char* equalsPos = strchr(line, '=');
      if (equalsPos != nullptr) {
        // Parse the integer value (can be multi-digit, 0-MAX_RPM)
        int rpm = atoi(equalsPos + 1);
        // Validate range
        if (rpm >= 0 && rpm <= MAX_RPM) {
          display.processRPM(rpm);
          const ColorResult& colorResult = display.getColorResult();
          for (int i = 0; i < NUM_LEDS; i++) {
            strip.setPixelColor(i, colorResult.red[i], colorResult.green[i], colorResult.blue[i]);
          }
          strip.show();
        }
      }
    }
    // Check for BEGIN command
    else if (strcmp(line, "BEGIN") == 0) {
      display.clearImages();
      readingImages = true;
      Serial.println("OK");
      // Optionally send acknowledgment
      // Serial.println("# BEGIN - ready to receive images");
    }
    // Check for END command
    else if (strcmp(line, "END") == 0) {
      if (readingImages) {
        display.writeImagesToEEPROM();
        readingImages = false;
        Serial.println("OK");
        // Optionally send acknowledgment
        // Serial.println("# END - images written to EEPROM");
      }
    }
    // If in reading mode, parse and add image
    else if (readingImages) {
      if (display.addImageFromString(line)) {
        Serial.println("OK");
      }
    }
  }
}

