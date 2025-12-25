#ifndef Display_h
#define Display_h
#include <Arduino.h>
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
    Image images[MAX_IMAGES];
    int imageCount;
    ColorResult colorResult;

    static Image parseImageFromString(const String& csvString);
    bool addImage(const Image& img);
    static void calculateColors(int rpm, const Image& img, ColorResult& result);

  public:
    struct ColorResult {
      uint8_t red[14];
      uint8_t green[14];
      uint8_t blue[14];
      uint8_t blinkRate[14];
    };

    Display();
    void writeImagesToEEPROM();
    bool readImagesFromEEPROM();
    void processRPM(int rpm);
    bool addImageFromString(const String& csvString);
    void clearImages();


};

#endif // Display_h