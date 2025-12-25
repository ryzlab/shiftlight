#include "Display.h"
#include <EEPROM.h>
#include <cstring> // Required for strchr, strncpy, strlen, atoi

Display::Display() {
  imageCount = 0;
}

Display::Image Display::parseImageFromString(const char* csvString) {
  Image img = {0, 0, 0, 0, 0, 0, 0, 0, 0};
  
  if (csvString == nullptr) {
    return img;
  }
  
  // Find the bitmask section in brackets
  const char* bracketStart = strchr(csvString, '[');
  const char* bracketEnd = strchr(csvString, ']');
  
  if (bracketStart != nullptr && bracketEnd != nullptr && bracketEnd > bracketStart) {
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
        
        int bitIndex = atoi(token);
        if (bitIndex >= 0 && bitIndex < 14) {
          img.bitmask |= (1U << bitIndex);
        }
        
        token = strtok(nullptr, ",");
      }
    }
  }
  
  // Extract the rest of the CSV after the closing bracket
  if (bracketEnd != nullptr) {
    const char* csvStart = bracketEnd + 1;
    
    // Skip leading whitespace
    while (*csvStart == ' ' || *csvStart == '\t') csvStart++;
    
    // Remove leading comma if present
    if (*csvStart == ',') {
      csvStart++;
    }
    
    // Parse the remaining CSV values (9 values: 8 existing + 1 blinkrate)
    int values[9];
    int valueIndex = 0;
    const char* pos = csvStart;
    
    while (*pos != '\0' && valueIndex < 9) {
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
    
    // Assign values to struct
    if (valueIndex >= 9) {
      img.startRPM = values[0];
      img.endRPM = values[1];
      img.startRed = values[2];
      img.startGreen = values[3];
      img.startBlue = values[4];
      img.endRed = values[5];
      img.endGreen = values[6];
      img.endBlue = values[7];
      
      // Extract the last value (0-3) and store in bits 14-15 of 16-bit bitmask
      int blinkValue = values[8];
      if (blinkValue >= 0 && blinkValue <= 3) {
        img.bitmask |= ((unsigned int)blinkValue << 14);
      }
    }
  }
  
  return img;
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

void Display::writeImagesToEEPROM() {
  int address = 0;
  
  // Write the image count first
  EEPROM.put(address, imageCount);
  address += sizeof(int);
  
  // Write each image
  for (int i = 0; i < imageCount; i++) {
    EEPROM.put(address, images[i]);
    address += sizeof(Image);
  }
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
  
  return true; // Successfully read
}

void Display::calculateColors(int rpm, const Image& img, ColorResult& result) {
  // Clamp RPM to 0-9999 range
  if (rpm < 0) rpm = 0;
  if (rpm > 9999) rpm = 9999;
  
  // Calculate proportion (0.0 to 1.0) based on RPM in 0-9999 range
  float proportion = (float)rpm / 9999.0;
  
  // Interpolate color values
  uint8_t calculatedRed = (uint8_t)(img.startRed + (img.endRed - img.startRed) * proportion);
  uint8_t calculatedGreen = (uint8_t)(img.startGreen + (img.endGreen - img.startGreen) * proportion);
  uint8_t calculatedBlue = (uint8_t)(img.startBlue + (img.endBlue - img.startBlue) * proportion);
  
  // Extract blink rate from bits 14-15
  uint8_t blinkRateValue = (uint8_t)((img.bitmask >> 14) & 0x3);
  
  // Extract bitmask (only bits 0-13, ignore bits 14-15 which are blinkrate)
  unsigned int bitmask = img.bitmask & 0x3FFF; // Mask to get only lower 14 bits
  
  // Set color values and blink rate at indices specified by the bitmask
  for (int i = 0; i < 14; i++) {
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
  for (int i = 0; i < 14; i++) {
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
  
  // Add the image to the array
  return addImage(img);
}