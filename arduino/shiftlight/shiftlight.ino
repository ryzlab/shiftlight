
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

Adafruit_NeoPixel strip = Adafruit_NeoPixel(NUM_LEDS, LED_PIN, NEO_RGB + NEO_KHZ800);

Display display(&strip);  // Pass pointer to strip

const int MAX_LINE_LENGTH = 80;
static int inputIndex = 0;  // Current position in buffer
static bool discardingLine = false;  // Flag to track if we're discarding a line
bool readingImages = false;  // Flag to track if we're in image reading mode

bool readSerialLine(char* line, int lineSize) {
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
      if (line != nullptr && lineSize > 0) {
        line[0] = '\0';
      }
      return false;
    }
    
    // Process the line if we have data
    if (inputIndex > 0 && line != nullptr && lineSize > 0) {
      // Null-terminate the buffer
      line[inputIndex] = '\0';
      
      // Find start of non-whitespace
      int start = 0;
      while (start < inputIndex && (line[start] == ' ' || line[start] == '\t')) {
        start++;
      }
      
      // Find end of non-whitespace
      int end = inputIndex - 1;
      while (end >= start && (line[end] == ' ' || line[end] == '\t' || line[end] == '\r' || line[end] == '\n')) {
        end--;
      }
      
      // Calculate trimmed length
      int trimmedLength = end - start + 1;
      
      // Ignore empty lines (after trimming)
      if (trimmedLength <= 0) {
        inputIndex = 0;
        line[0] = '\0';
        return false;
      }
      
      // Ignore lines starting with #
      if (line[start] == '#') {
        inputIndex = 0;
        line[0] = '\0';
        return false;
      }
      
      // Shift trimmed content to start of buffer if needed
      if (start > 0) {
        int copyLength = (trimmedLength < lineSize - 1) ? trimmedLength : lineSize - 1;
        for (int i = 0; i < copyLength; i++) {
          line[i] = line[start + i];
        }
        line[copyLength] = '\0';
      } else {
        // Just truncate at the end
        int truncateLength = (trimmedLength < lineSize - 1) ? trimmedLength : lineSize - 1;
        line[truncateLength] = '\0';
      }
      
      inputIndex = 0;
      return true;
    }
    
    // No data or invalid parameters
    inputIndex = 0;
    if (line != nullptr && lineSize > 0) {
      line[0] = '\0';
    }
    return false;
  } else {
    if (discardingLine) {
      // Continue discarding characters until newline
      return false;
    }
    
    // Add character to buffer if there's space
    if (line != nullptr && inputIndex < lineSize - 1 && inputIndex < MAX_LINE_LENGTH) {
      line[inputIndex++] = inChar;
    } else {
      // Buffer full, start discarding
      inputIndex = 0;
      discardingLine = true;
      if (line != nullptr && lineSize > 0) {
        line[0] = '\0';
      }
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

  // Display the current ColorResult on the LEDs with blinking
  const ColorResult& colorResult = display.getColorResult();
  for (int i = 0; i < NUM_LEDS; i++) {
    uint8_t blinkRate = colorResult.blinkRate[i];
    if (blinkRate == 0) {
      // No blinking - always show the color
      strip.setPixelColor(i, colorResult.red[i], colorResult.green[i], colorResult.blue[i]);
    } else if (blinkRate == 1) {
      Color blinkColor = display.calculateBlink(colorResult, i, currentTime);
      strip.setPixelColor(i, blinkColor.red, blinkColor.green, blinkColor.blue);
    } else if (blinkRate == 2) {
      Color pulseColor = display.calculatePulse(colorResult, i, currentTime);
      strip.setPixelColor(i, pulseColor.red, pulseColor.green, pulseColor.blue);
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

