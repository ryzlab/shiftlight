#include "Display.h"
#include <EEPROM.h>

Display::Display() {
  imageCount = 0;
}

Display::Image Display::parseImageFromString(const String& csvString) {
  Image img = {0, 0, 0, 0, 0, 0, 0, 0, 0};
  
  // Find the bitmask section in brackets
  int bracketStart = csvString.indexOf('[');
  int bracketEnd = csvString.indexOf(']');
  
  if (bracketStart >= 0 && bracketEnd > bracketStart) {
    // Extract the content inside brackets
    String bitmaskStr = csvString.substring(bracketStart + 1, bracketEnd);
    
    // Parse comma-separated indices and build bitmask
    int startIdx = 0;
    while (startIdx < bitmaskStr.length()) {
      int commaIdx = bitmaskStr.indexOf(',', startIdx);
      if (commaIdx < 0) commaIdx = bitmaskStr.length();
      
      String numStr = bitmaskStr.substring(startIdx, commaIdx);
      numStr.trim();
      int bitIndex = numStr.toInt();
      
      if (bitIndex >= 0 && bitIndex < 32) {
        img.bitmask |= (1U << bitIndex);
      }
      
      startIdx = commaIdx + 1;
    }
  }
  
  // Extract the rest of the CSV after the closing bracket
  int csvStart = csvString.indexOf(']') + 1;
  String csvPart = csvString.substring(csvStart);
  csvPart.trim();
  
  // Remove leading comma if present
  if (csvPart.startsWith(",")) {
    csvPart = csvPart.substring(1);
  }
  
  // Parse the remaining CSV values
  int values[8];
  int valueIndex = 0;
  int startPos = 0;
  
  while (startPos < csvPart.length() && valueIndex < 8) {
    int commaPos = csvPart.indexOf(',', startPos);
    if (commaPos < 0) commaPos = csvPart.length();
    
    String valStr = csvPart.substring(startPos, commaPos);
    valStr.trim();
    values[valueIndex++] = valStr.toInt();
    
    startPos = commaPos + 1;
  }
  
  // Assign values to struct
  if (valueIndex >= 8) {
    img.startRPM = values[0];
    img.endRPM = values[1];
    img.startRed = values[2];
    img.startGreen = values[3];
    img.startBlue = values[4];
    img.endRed = values[5];
    img.endGreen = values[6];
    img.endBlue = values[7];
  }
  
  return img;
}

bool Display::isValidImageString(const String& csvString) {
  // Check for opening and closing brackets
  int bracketStart = csvString.indexOf('[');
  int bracketEnd = csvString.indexOf(']');
  
  // Must have both brackets and closing bracket must come after opening
  if (bracketStart < 0 || bracketEnd < 0 || bracketEnd <= bracketStart) {
    return false;
  }
  
  // Extract the part after the closing bracket
  int csvStart = bracketEnd + 1;
  String csvPart = csvString.substring(csvStart);
  csvPart.trim();
  
  // Remove leading comma if present
  if (csvPart.startsWith(",")) {
    csvPart = csvPart.substring(1);
  }
  
  // Count commas - we need exactly 7 commas for 8 values
  int commaCount = 0;
  for (int i = 0; i < csvPart.length(); i++) {
    if (csvPart.charAt(i) == ',') {
      commaCount++;
    }
  }
  
  // Must have exactly 7 commas (for 8 values)
  return commaCount == 7;
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