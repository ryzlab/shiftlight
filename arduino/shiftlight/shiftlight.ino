#include <WS2812FX.h>

#define LED_COUNT 13
#define LED_PIN   4
#define PWM_PIN   3

WS2812FX ws2812fx = WS2812FX(LED_COUNT, LED_PIN, NEO_RGB + NEO_KHZ800);

// Duty cycle measurement variables (shared with ISR)
volatile unsigned long highStart = 0;
volatile unsigned long lowStart = 0;
volatile unsigned long highTime = 0;
volatile unsigned long lowTime  = 0;
volatile float dutyCycle = 0.0f;
volatile unsigned long lastEdgeTime = 0;

// ISR for rising edge
void risingISR() {
  lastEdgeTime = micros();
  lowTime = micros() - lowStart;                 // just finished LOW period
  highStart = micros();
  unsigned long period = highTime + lowTime;
  if (period > 0) dutyCycle = (float)highTime / period;
  attachInterrupt(digitalPinToInterrupt(PWM_PIN), fallingISR, FALLING);
}

// ISR for falling edge
void fallingISR() {
  lastEdgeTime = micros();
  highTime = micros() - highStart;               // just finished HIGH period
  lowStart = micros();
  unsigned long period = highTime + lowTime;
  if (period > 0) dutyCycle = (float)highTime / period;
  attachInterrupt(digitalPinToInterrupt(PWM_PIN), risingISR, RISING);
}

void setup() {
  Serial.begin(115200);

  pinMode(PWM_PIN, INPUT);

  ws2812fx.init();
  ws2812fx.setSegment(0, 0, 7,  FX_MODE_STATIC);
  ws2812fx.setSegment(1, 8, 11, FX_MODE_STATIC);
  ws2812fx.setSegment(2, 12, 12, FX_MODE_STATIC);
  ws2812fx.setSpeed(0, 1);
  ws2812fx.setSpeed(1, 1);
  ws2812fx.setSpeed(2, 1);
  ws2812fx.setBrightness(100);
  ws2812fx.start();

  // Start by waiting for rising edge
  attachInterrupt(digitalPinToInterrupt(PWM_PIN), risingISR, RISING);
}

void loop() {

  // Timing getDutyCycle() equivalent (just read latest value from ISR)
  float currentDuty;
  noInterrupts();
  currentDuty = dutyCycle;
    unsigned long currentTime = micros();
  unsigned long last = currentTime - lastEdgeTime;

  interrupts();

  // Fade segments based on currentDuty
  float segLength = 1.0 / 3.0;

  float seg0Level = constrain(currentDuty / segLength, 0.0, 1.0);
  ws2812fx.setColor(0, ws2812fx.Color(seg0Level * 255, seg0Level * 255, seg0Level * 255));

  float seg1Level = constrain((currentDuty - segLength) / segLength, 0.0, 1.0);
  ws2812fx.setColor(1, ws2812fx.Color(seg1Level * 255, seg1Level * 255, seg1Level * 255));

  float seg2Level = constrain((currentDuty - 2 * segLength) / segLength, 0.0, 1.0);
  ws2812fx.setColor(2, ws2812fx.Color(seg2Level * 255, seg2Level * 255, seg2Level * 255));

  // Timing ws2812fx.service()
  ws2812fx.service();

  // Print timing
  /*Serial.print("lowTime: ");
  Serial.print(lowTime);
  Serial.print(", highTime: ");
  Serial.print(highTime);
  Serial.print(", last = ");*/
  /*Serial.print(last);
  Serial.print(", duty = ");
  Serial.println(currentDuty, 3);*/
}
