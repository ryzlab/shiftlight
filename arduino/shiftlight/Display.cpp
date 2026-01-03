#include "Display.h"
#include <EEPROM.h>
#include <string.h>  // Required for strchr, strncpy, strlen
#include <stdlib.h>   // Required for atoi

// External references to prefixes defined in main file
extern const char* ERROR_PREFIX;
extern const char* SUCCESS_PREFIX;

Display::Display(Adafruit_NeoPixel* strip) : strip(strip) {
  imageCount = 0;
}

Display::Image Display::parseImageFromString(const char* csvString) {
  Image img = {0, 0, 0, 0, 0, 0, 0, 0, 0};
  
  if (csvString == nullptr) {
    Serial.print(ERROR_PREFIX);
    Serial.println("Null CSV string");
    img.bitmask = INVALID_BITMASK;
    return img;
  }
  
  // Find the bitmask section in brackets
  const char* bracketStart = strchr(csvString, '[');
  const char* bracketEnd = strchr(csvString, ']');
  
  if (bracketStart == nullptr || bracketEnd == nullptr || bracketEnd <= bracketStart) {
    Serial.print(ERROR_PREFIX);
    Serial.println("Missing or invalid brackets");
    img.bitmask = INVALID_BITMASK;
    return img;
  }
  
  // Extract the content inside brackets
  int bitmaskLen = bracketEnd - bracketStart - 1;
  if (bitmaskLen > 0) {
    // Parse comma-separated indices and build bitmask (max 14 bits, so 0-13)
    char bitmaskBuffer[128];  // Temporary buffer for bitmask section
    int copyLen = (bitmaskLen < 127) ? bitmaskLen : 127;
    strncpy(bitmaskBuffer, bracketStart + 1, copyLen);
    bitmaskBuffer[copyLen] = '\0';
    
    char* token = strtok(bitmaskBuffer, ",");
    while (token != nullptr) {
      // Trim whitespace from token
      while (*token == ' ' || *token == '\t') token++;
      char* end = token + strlen(token) - 1;
      while (end > token && (*end == ' ' || *end == '\t')) {
        *end = '\0';
        end--;
      }
      
      // Check if token contains a range (dash)
      char* dashPos = strchr(token, '-');
      if (dashPos != nullptr) {
        // Parse range: start-end
        *dashPos = '\0';  // Split the token at the dash
        int rangeStart = atoi(token);
        int rangeEnd = atoi(dashPos + 1);
        
        // Validate range values are in range (0-MAX_BIT_INDEX)
        if (rangeStart < 0 || rangeStart > MAX_BIT_INDEX || rangeEnd < 0 || rangeEnd > MAX_BIT_INDEX) {
          Serial.print(ERROR_PREFIX);
          Serial.print("Invalid bitmask range: ");
          Serial.print(rangeStart);
          Serial.print("-");
          Serial.println(rangeEnd);
          img.bitmask = INVALID_BITMASK;
          return img;
        }
        
        // Ensure rangeStart <= rangeEnd
        if (rangeStart > rangeEnd) {
          int temp = rangeStart;
          rangeStart = rangeEnd;
          rangeEnd = temp;
        }
        
        // Set all bits in the range
        for (int i = rangeStart; i <= rangeEnd; i++) {
          img.bitmask |= (1U << i);
        }
      } else {
        // Single value - validate it's in range (0-MAX_BIT_INDEX)
        int bitIndex = atoi(token);
        if (bitIndex < 0 || bitIndex > MAX_BIT_INDEX) {
          Serial.print(ERROR_PREFIX);
          Serial.print("Invalid bitmask value: ");
          Serial.println(bitIndex);
          img.bitmask = INVALID_BITMASK;
          return img;
        }
        img.bitmask |= (1U << bitIndex);
      }
      
      token = strtok(nullptr, ",");
    }
  }
  
  // Extract the rest of the CSV after the closing bracket
  const char* csvStart = bracketEnd + 1;
  
  // Skip leading whitespace
  while (*csvStart == ' ' || *csvStart == '\t') csvStart++;
  
  // Remove leading comma if present
  if (*csvStart == ',') {
    csvStart++;
  }
  
  // Parse the remaining CSV values (must be exactly 9 values)
  int values[9];
  int valueIndex = 0;
  const char* pos = csvStart;
  
  while (*pos != '\0' && valueIndex < 10) {  // Check up to 10 to detect too many
    // Skip whitespace
    while (*pos == ' ' || *pos == '\t') pos++;
    
    if (*pos == '\0') break;
    
    // Find next comma or end of string
    const char* commaPos = strchr(pos, ',');
    int valueLen;
    if (commaPos != nullptr) {
      valueLen = commaPos - pos;
    } else {
      valueLen = strlen(pos);
    }
    
    // Extract value
    char valueBuffer[32];
    int copyLen = (valueLen < 31) ? valueLen : 31;
    strncpy(valueBuffer, pos, copyLen);
    valueBuffer[copyLen] = '\0';
    
    // Trim whitespace
    char* valStart = valueBuffer;
    while (*valStart == ' ' || *valStart == '\t') valStart++;
    char* valEnd = valStart + strlen(valStart) - 1;
    while (valEnd > valStart && (*valEnd == ' ' || *valEnd == '\t')) {
      *valEnd = '\0';
      valEnd--;
    }
    
    values[valueIndex++] = atoi(valStart);
    
    if (commaPos != nullptr) {
      pos = commaPos + 1;
    } else {
      break;
    }
  }
  
  // Validate exactly 9 values (not more, not less)
  if (valueIndex != 9) {
    Serial.print(ERROR_PREFIX);
    Serial.print("Invalid number of CSV values: ");
    Serial.print(valueIndex);
    Serial.println(" (expected 9)");
    img.bitmask = INVALID_BITMASK;
    return img;
  }
  
  // Assign values to struct
  img.startRPM = values[0];
  img.endRPM = values[1];
  img.startRed = values[2];
  img.startGreen = values[3];
  img.startBlue = values[4];
  img.endRed = values[5];
  img.endGreen = values[6];
  img.endBlue = values[7];
  
  // Extract the last value (0-2) and store in bits 14-15 of 16-bit bitmask
  int blinkValue = values[8];
  if (blinkValue < 0 || blinkValue > 2) {
    Serial.print(ERROR_PREFIX);
    Serial.print("Invalid blink value: ");
    Serial.print(blinkValue);
    Serial.println(" (expected 0-2)");
    img.bitmask = INVALID_BITMASK;
    return img;
  }
  img.bitmask |= ((unsigned int)blinkValue << 14);
  
  // Success - optionally print success message
  // Serial.print(SUCCESS_PREFIX);
  // Serial.println("Image parsed successfully");
  
  return img;
}

bool Display::isValidImage(const Image& img) {
  return img.bitmask != INVALID_BITMASK;
}

void Display::clearImages() {
  imageCount = 0;
}

bool Display::addImage(const Image& img) {
  if (imageCount >= MAX_IMAGES) {
    return false; // Array is full
  }
  images[imageCount++] = img;
  return true; // Successfully added
}

int Display::writeImagesToEEPROM() {
  int address = 0;
  
  // Write the image count first
  EEPROM.put(address, imageCount);
  address += sizeof(int);
  
  // Write each image
  for (int i = 0; i < imageCount; i++) {
    EEPROM.put(address, images[i]);
    address += sizeof(Image);
  }
  
  return imageCount;
}

bool Display::readImagesFromEEPROM() {
  int address = 0;
  
  // Read the image count first
  int readCount;
  EEPROM.get(address, readCount);
  address += sizeof(int);
  
  // Validate the count
  if (readCount < 0 || readCount > MAX_IMAGES) {
    imageCount = 0;
    return false; // Invalid count
  }
  
  // Read each image
  imageCount = readCount;
  for (int i = 0; i < imageCount; i++) {
    EEPROM.get(address, images[i]);
    address += sizeof(Image);
  }
  
  Serial.print("# Read ");
  Serial.print(imageCount);
  Serial.println(" images from EEPROM");
  
  return true; // Successfully read
}

void Display::calculateColors(int rpm, const Image& img, ColorResult& result) {
  // Clamp RPM to the image's RPM range
  if (rpm < img.startRPM) rpm = img.startRPM;
  if (rpm > img.endRPM) rpm = img.endRPM;
  
  // Calculate proportion (0.0 to 1.0) based on RPM between startRPM and endRPM
  float proportion = 0.0;
  if (img.endRPM != img.startRPM) {
    proportion = (float)(rpm - img.startRPM) / (float)(img.endRPM - img.startRPM);
  } else {
    // If startRPM == endRPM, use proportion 1.0 (end color)
    proportion = 1.0;
  }
  
  // Interpolate color values
  uint8_t calculatedRed = (uint8_t)(img.startRed + (img.endRed - img.startRed) * proportion);
  uint8_t calculatedGreen = (uint8_t)(img.startGreen + (img.endGreen - img.startGreen) * proportion);
  uint8_t calculatedBlue = (uint8_t)(img.startBlue + (img.endBlue - img.startBlue) * proportion);
  
  // Extract blink rate from bits 14-15
  uint8_t blinkRateValue = (uint8_t)((img.bitmask >> 14) & 0x3);
  
  // Extract bitmask (only bits 0-MAX_BIT_INDEX, ignore bits 14-15 which are blinkrate)
  unsigned int bitmask = img.bitmask & 0x3FFF; // Mask to get only lower 14 bits
  
  // Set color values and blink rate at indices specified by the bitmask
  for (int i = 0; i <= MAX_BIT_INDEX; i++) {
    if (bitmask & (1U << i)) {
      result.red[i] = calculatedRed;
      result.green[i] = calculatedGreen;
      result.blue[i] = calculatedBlue;
      result.blinkRate[i] = blinkRateValue;
    }
  }
}

void Display::processRPM(int rpm) {
  // Initialize all values in colorResult to 0
  for (int i = 0; i <= MAX_BIT_INDEX; i++) {
    colorResult.red[i] = 0;
    colorResult.green[i] = 0;
    colorResult.blue[i] = 0;
    colorResult.blinkRate[i] = 0;
  }
  
  // Go through all images and check if RPM matches
  for (int i = 0; i < imageCount; i++) {
    if (rpm >= images[i].startRPM && rpm <= images[i].endRPM) {
      calculateColors(rpm, images[i], colorResult);
    }
  }
}

bool Display::addImageFromString(const char* csvString) {
  // Parse the image from the CSV string
  Image img = parseImageFromString(csvString);
  
  // Check if the image is valid
  if (!isValidImage(img)) {
    return false;
  }
  
  // Add the image to the array
  return addImage(img);
}

const ColorResult& Display::getColorResult() const {
  return colorResult;
}

void Display::printAllImages() const {
  for (int i = 0; i < imageCount; i++) {
    const Image& img = images[i];
    
    // Extract bitmask (only bits 0-MAX_BIT_INDEX, ignore bits 14-15 which are blinkrate)
    unsigned int bitmask = img.bitmask & 0x3FFF; // Mask to get only lower 14 bits
    
    // Extract all bit indices that are set
    int bitIndices[MAX_BIT_INDEX + 1];
    int bitCount = 0;
    for (int j = 0; j <= MAX_BIT_INDEX; j++) {
      if (bitmask & (1U << j)) {
        bitIndices[bitCount++] = j;
      }
    }
    
    // Format bitmask with ranges
    Serial.print("[");
    if (bitCount > 0) {
      int rangeStart = bitIndices[0];
      int rangeEnd = bitIndices[0];
      bool firstOutput = true;
      
      for (int j = 1; j < bitCount; j++) {
        if (bitIndices[j] == rangeEnd + 1) {
          // Consecutive, extend range
          rangeEnd = bitIndices[j];
        } else {
          // Not consecutive, output current range/point
          if (!firstOutput) Serial.print(",");
          firstOutput = false;
          
          if (rangeStart == rangeEnd) {
            // Single value
            Serial.print(rangeStart);
          } else if (rangeEnd == rangeStart + 1) {
            // Two consecutive values, output separately
            Serial.print(rangeStart);
            Serial.print(",");
            Serial.print(rangeEnd);
          } else {
            // Range of 2+ consecutive values
            Serial.print(rangeStart);
            Serial.print("-");
            Serial.print(rangeEnd);
          }
          // Start new range
          rangeStart = bitIndices[j];
          rangeEnd = bitIndices[j];
        }
      }
      
      // Output the last range/point
      if (!firstOutput) Serial.print(",");
      if (rangeStart == rangeEnd) {
        Serial.print(rangeStart);
      } else if (rangeEnd == rangeStart + 1) {
        Serial.print(rangeStart);
        Serial.print(",");
        Serial.print(rangeEnd);
      } else {
        Serial.print(rangeStart);
        Serial.print("-");
        Serial.print(rangeEnd);
      }
    }
    Serial.print("]");
    
    // Extract blink rate from bits 14-15
    uint8_t blinkRateValue = (uint8_t)((img.bitmask >> 14) & 0x3);
    
    // Output the rest of the CSV values
    Serial.print(",");
    Serial.print(img.startRPM);
    Serial.print(",");
    Serial.print(img.endRPM);
    Serial.print(",");
    Serial.print(img.startRed);
    Serial.print(",");
    Serial.print(img.startGreen);
    Serial.print(",");
    Serial.print(img.startBlue);
    Serial.print(",");
    Serial.print(img.endRed);
    Serial.print(",");
    Serial.print(img.endGreen);
    Serial.print(",");
    Serial.print(img.endBlue);
    Serial.print(",");
    Serial.println(blinkRateValue);
  }
}