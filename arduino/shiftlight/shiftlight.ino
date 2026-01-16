
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
      Serial.print(ERROR_PREFIX);
      Serial.println("Line too long, discarded");
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

    // Read images from EEPROM at startup
    display.readImagesFromEEPROM();
  
    // Process initial RPM (0) to display the pattern
    display.processRPM(0);
    Serial.println("READY");
  
}

unsigned long last = millis();
unsigned long lastBlinkTime = 0;
bool blinkState = false;  // false = off, true = on
unsigned long pulseStartTime = 0;  // For tracking pulse cycle
char line[MAX_LINE_LENGTH + 1];
  
void loop() {
  /*rpmReader.loop();
  if (millis() - last > 10) {
    last = millis();
    int currentRpm = rpmReader.getCurrentRpm();
    Serial.println(currentRpm);
  }*/

  // Handle blinking timing
  unsigned long currentTime = millis();
  unsigned long blinkInterval = 500;  // 500ms for blinkRate = 1
  unsigned long pulseCycleTime = 2000;  // 2 seconds for full pulse cycle (blinkRate = 2)
  
  // Check if it's time to toggle blink state for blinkRate = 1
  if (currentTime - lastBlinkTime >= blinkInterval) {
    blinkState = !blinkState;
    lastBlinkTime = currentTime;
  }

  // Calculate pulse brightness for blinkRate = 2 (0-255)
  // Use a triangle wave: goes from 0 to 255 and back to 0
  unsigned long pulsePhase = currentTime % pulseCycleTime;
  uint8_t pulseBrightness;
  if (pulsePhase < pulseCycleTime / 2) {
    // Fading in: 0 to 255
    pulseBrightness = (uint8_t)((pulsePhase * 255) / (pulseCycleTime / 2));
  } else {
    // Fading out: 255 to 0
    pulseBrightness = (uint8_t)(255 - ((pulsePhase - pulseCycleTime / 2) * 255) / (pulseCycleTime / 2));
  }

  // Display the current ColorResult on the LEDs with blinking
  const ColorResult& colorResult = display.getColorResult();
  for (int i = 0; i < NUM_LEDS; i++) {
    uint8_t blinkRate = colorResult.blinkRate[i];
    
    if (blinkRate == 0) {
      // No blinking - always show the color
      strip.setPixelColor(i, colorResult.red[i], colorResult.green[i], colorResult.blue[i]);
    } else if (blinkRate == 1) {
      // Blink rate 1: 500ms on, 500ms off
      if (blinkState) {
        strip.setPixelColor(i, colorResult.red[i], colorResult.green[i], colorResult.blue[i]);
      } else {
        strip.setPixelColor(i, 0, 0, 0);  // Off
      }
    } else if (blinkRate == 2) {
      // Blink rate 2: pulse (slow fade in and out)
      // Apply pulse brightness to the color
      uint8_t pulsedRed = (colorResult.red[i] * pulseBrightness) / 255;
      uint8_t pulsedGreen = (colorResult.green[i] * pulseBrightness) / 255;
      uint8_t pulsedBlue = (colorResult.blue[i] * pulseBrightness) / 255;
      strip.setPixelColor(i, pulsedRed, pulsedGreen, pulsedBlue);
    }
  }
  /*for (int i = 0; i < NUM_LEDS; i++) {
        strip.setPixelColor(i, random(255), random(255), random(255));
  }*/
  strip.show();

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
          Serial.println("OK");
        } else {
          Serial.print(ERROR_PREFIX);
          Serial.print("RPM out of range: ");
          Serial.print(rpm);
          Serial.print(" (valid range: 0-");
          Serial.print(MAX_RPM);
          Serial.println(")");
        }
      } else {
        Serial.print(ERROR_PREFIX);
        Serial.println("Invalid RPM command format");
      }
    }
    // Check for BEGIN command (case-insensitive)
    else if (strcasecmp(line, "BEGIN") == 0) {
      display.clearImages();
      readingImages = true;
      Serial.println("OK");
    }
    // Check for END command (case-insensitive)
    else if (strcasecmp(line, "END") == 0) {
      if (readingImages) {
        int imagesWritten = display.writeImagesToEEPROM();
        readingImages = false;
        Serial.println("OK");
        //Serial.print(" wrote ");
        //Serial.print(imagesWritten);
        //Serial.println(" rows");
      }
    }
    // Check for LIST command (case-insensitive)
    else if (strcasecmp(line, "LIST") == 0) {
      Serial.println("BEGIN");
      display.printAllImages();
      Serial.println("END");
      Serial.println("OK");
    }
    // Check if line contains HELLO (case-insensitive)
    else if (strcasestr(line, "HELLO") != nullptr) {
      Serial.println("OK");
    }
    // If in reading mode, parse and add image
    else if (readingImages) {
      if (display.addImageFromString(line)) {
        Serial.println("OK");
      }
    }
    // Unknown command
    else {
      Serial.print(ERROR_PREFIX);
      Serial.print("Unknown command: ");
      Serial.println(line);
    }
  }
}

