#ifndef RPMReader_h
#define RPMReader_h

#include <SPI.h>
#include "mcp_can.h"

class RPMReader {
  private:
    MCP_CAN CAN;
    volatile int currentRpm;
    byte intPin;
    static volatile bool canMsgReceived;
    long pollInterval;
    unsigned long lastRequest;

  public:
    RPMReader(byte csPin, byte intPin);
    bool init();
    int getCurrentRpm();
    void loop();

  private:
    static void MCP_ISR();
    void requestRPM();
};

#endif