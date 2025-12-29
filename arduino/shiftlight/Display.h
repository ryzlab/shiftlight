#ifndef Display_h
#define Display_h
#include <Arduino.h>
#include <Adafruit_NeoPixel.h>

// Forward declaration or define ColorResult before the class
struct ColorResult {
  uint8_t red[14];
  uint8_t green[14];
  uint8_t blue[14];
  uint8_t blinkRate[14];
};

class Display {
  private:
    struct Image {
      unsigned int bitmask;
      int startRPM;
      int endRPM;
      uint8_t startRed;
      uint8_t startGreen;
      uint8_t startBlue;
      uint8_t endRed;
      uint8_t endGreen;
      uint8_t endBlue;
    };

    static const int MAX_IMAGES = 32;
    static const unsigned int INVALID_BITMASK = 0xFFFF;  // Sentinel value for invalid images
    Image images[MAX_IMAGES];
    int imageCount;
    ColorResult colorResult;
    Adafruit_NeoPixel* strip;  // Private instance variable

    static Image parseImageFromString(const char* csvString);
    static bool isValidImage(const Image& img);
    bool addImage(const Image& img);
    static void calculateColors(int rpm, const Image& img, ColorResult& result);

  public:
    Display(Adafruit_NeoPixel* strip);
    void writeImagesToEEPROM();
    bool readImagesFromEEPROM();
    void processRPM(int rpm);
    bool addImageFromString(const char* csvString);
    void clearImages();


};

#endif // Display_h