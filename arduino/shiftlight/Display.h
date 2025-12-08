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

  public:
    Display();
    static Image parseImageFromString(const String& csvString);
    static bool isValidImageString(const String& csvString);
    void clearImages();
    bool addImage(const Image& img);
    void writeImagesToEEPROM();
    bool readImagesFromEEPROM();


};

#endif // Display_h