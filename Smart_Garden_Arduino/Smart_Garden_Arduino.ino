#include <SPI.h>
#include <TFT_eSPI.h> // Hardware-specific library
#include <SPIFFS.h>   // Include the SPIFFS library

#include "DHT.h"

#define DHTPIN 5       // Digital pin connected to the DHT sensor
#define DHTTYPE DHT22   // DHT 22  (AM2302), AM2321

#define LDR_PIN 14      // Digital pin connected to the LDR sensor

#define LED_PIN 12     // Digital pin connected to the LED

#define WATERPUMP_PIN 25 // Digital pin connected to the Waterpump

#define MOISTURE_PIN 34 // AOUT pin of moisture sensor
#define wet 3000
#define dry 700
#define echoPin 27 // Returned signal is recieved from pin 27
#define trigPin 26 // Signal is send from pin 26
int pingtime;      // Time to travel the signal
int distance;
std::string waterLevel;

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// Define the UUID for the BLE service, which transmits the data from the Smart Garden System
#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
// Define the UUIDs for the BLE characteristics that represent the various sensors and actuators of the Smart Garden System
#define CHARACTERISTIC_UUID_TEMPERATURE "beb5483e-36e1-4688-b7f5-ea07361b26a8"
#define CHARACTERISTIC_UUID_HUMIDITY "1c95d5e3-d8f7-413a-bf3d-7a2e5d7be87e"
#define CHARACTERISTIC_UUID_LED_STATUS "cad5807a-4b4c-11ee-be56-0242ac120002"
#define CHARACTERISTIC_UUID_LED_CONTROL "50fdb428-6ce1-11ee-b962-0242ac120002"
#define CHARACTERISTIC_UUID_LDR_PHOTORESISTOR "0adbfb76-6da4-11ee-b962-0242ac120002"
#define CHARACTERISTIC_UUID_SOILMOISTURE "2f905938-7003-11ee-b962-0242ac120002"
#define CHARACTERISTIC_UUID_WATERLEVEL "cd2d1f72-7004-11ee-b962-0242ac120002"
#define CHARACTERISTIC_UUID_WATERPUMP_STATUS "4e12fff8-7005-11ee-b962-0242ac120002"
#define CHARACTERISTIC_UUID_WATERPUMP_CONTROL "54da7e24-7005-11ee-b962-0242ac120002"

#include "Thermometer.h"
#include "Humidity.h"
#include "WaterLevel.h"
#include "Lighting.h"
#include "WaterPump.h"
#include "LDRSensor.h"
#include "SoilMoisture.h"

TFT_eSPI tft = TFT_eSPI();       // Invoke custom library

DHT dht(DHTPIN, DHTTYPE);     // Initialize DHT sensor.

BLEServer* pServer = NULL;
BLECharacteristic* temperatureCharacteristic = NULL;
BLECharacteristic* humidityCharacteristic = NULL;
BLECharacteristic* LDRCharacteristic = NULL;
BLECharacteristic* LEDStatusCharacteristic = NULL;
BLECharacteristic* LEDControlCharacteristic = NULL;
BLECharacteristic* SoilMoistureCharacteristic = NULL;
BLECharacteristic* WaterLevelCharacteristic = NULL;
BLECharacteristic* WaterPumpStatusCharacteristic = NULL;
BLECharacteristic* WaterPumpControlCharacteristic = NULL;

// Pointer to BLE2902 of Characteristics
BLE2902 *pBLE2902_temperature;       
BLE2902 *pBLE2902_humidity; 
BLE2902 *pBLE2902_LDR;                              
BLE2902 *pBLE2902_ledStatus;                              
BLE2902 *pBLE2902_moisture; 
BLE2902 *pBLE2902_waterLevel; 
BLE2902 *pBLE2902_waterPumpStatus; 

bool deviceConnected = false;
bool oldDeviceConnected = false;

int LDR_value = 0;
int soilMoistureValue = 0; 
int soilMoisture_int = 0;

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println(F("onConnect"));
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial.println(F("onDisconnect"));
    }
};

void setup(void) {
  Serial.begin(115200);

  // Create the BLE Device
  BLEDevice::init("ESP32SERVER");

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE Service
  BLEService *pService = pServer->createService(BLEUUID(SERVICE_UUID), 32);

  // Create a BLE Temperature Characteristic
  temperatureCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID_TEMPERATURE,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY 
                    );
  // Create a BLE Humidity Characteristic
  humidityCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID_HUMIDITY,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY 
                    );
  // Create a BLE LDR Characteristic
  LDRCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID_LDR_PHOTORESISTOR,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY
                    );
  // Create a BLE LED Status Characteristic
  LEDStatusCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID_LED_STATUS,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY
                    );
  // Create a BLE Control LED Characteristic
  LEDControlCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID_LED_CONTROL,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE 
                    );
  // Create a BLE Water Pump Status Characteristic
  WaterPumpStatusCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID_WATERPUMP_STATUS,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY
                    );
  // Create a BLE Control Water pump Characteristic
  WaterPumpControlCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID_WATERPUMP_CONTROL,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE 
                    );
  // Create a BLE Soil Moisture Characteristic
  SoilMoistureCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID_SOILMOISTURE,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY
                    );
  // Create a BLE Water Level Characteristic
  WaterLevelCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID_WATERLEVEL,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY
                    );

  // Create a BLE Descriptor  
  /*pDescr_1 = new BLEDescriptor((uint16_t)0x2901);
  pDescr_1->setValue("A very interesting variable");
  temperatureCharacteristic->addDescriptor(pDescr_1);*/

  // Add the BLE2902 Descriptor because we are using "PROPERTY_NOTIFY"
  pBLE2902_temperature = new BLE2902();
  pBLE2902_temperature->setNotifications(true);                 
  temperatureCharacteristic->addDescriptor(pBLE2902_temperature);

  pBLE2902_humidity = new BLE2902();
  pBLE2902_humidity->setNotifications(true);
  humidityCharacteristic->addDescriptor(pBLE2902_humidity);

  pBLE2902_LDR = new BLE2902();
  pBLE2902_LDR->setNotifications(true);
  LDRCharacteristic->addDescriptor(pBLE2902_LDR);

  pBLE2902_ledStatus = new BLE2902();
  pBLE2902_ledStatus->setNotifications(true);
  LEDStatusCharacteristic->addDescriptor(pBLE2902_ledStatus);

  pBLE2902_moisture = new BLE2902();
  pBLE2902_moisture->setNotifications(true);
  SoilMoistureCharacteristic->addDescriptor(pBLE2902_moisture);

  pBLE2902_waterLevel = new BLE2902();
  pBLE2902_waterLevel->setNotifications(true);
  WaterLevelCharacteristic->addDescriptor(pBLE2902_waterLevel);

  pBLE2902_waterPumpStatus = new BLE2902();
  pBLE2902_waterPumpStatus->setNotifications(true);
  WaterPumpStatusCharacteristic->addDescriptor(pBLE2902_waterPumpStatus);

  tft.init(); // Initialize the display
  tft.setRotation(2);
  // Swap the colour byte order when rendering
  tft.setSwapBytes(true);
  tft.fillScreen(TFT_WHITE); // Set the initial background color to white

  // Draw the icons
  tft.pushImage(0, 40, thermometerWidth, thermometerHeight, thermometer);
  tft.pushImage(160, 50, humidityWidth, humidityHeight, humidity);
  tft.pushImage(45, 180, waterlevelWidth, waterlevelHeight, waterlevel);
  tft.pushImage(80, 350, lightingWidth, lightingHeight, lighting);
  tft.pushImage(200, 350, waterpumpWidth, waterpumpHeight, waterpump);
  tft.pushImage(170, 170, soilmoistureWidth, soilmoistureHeight, soilmoisture);

  tft.setTextColor(TFT_MAGENTA);
  tft.setTextSize(3);

  // Air
  tft.setCursor(10, 10);
  tft.println("Air");

  // Water
  tft.setCursor(10, 120);
  tft.println("Water");

  // Light
  tft.setCursor(10, 325);
  tft.println("Light");

  // Pump
  tft.setCursor(190, 325);
  tft.println("Pump");

  tft.setTextColor(TFT_BLACK);
  tft.setTextSize(2);

  // Air Temperature Text
  tft.setCursor(10, 85);
  tft.println("Temperature");

  // Humidity Text
  tft.setCursor(200, 85);
  tft.println("Humiditiy");

  // Water level Text
  tft.setCursor(10, 150);
  tft.println("Water Level");

  // Soil Moisture Text
  tft.setCursor(155, 260);
  tft.println("Soil Moisture");

  // Intensity Text
  tft.setCursor(10, 420);
  tft.println("The LED is");

  // Pump water 
  tft.setCursor(180, 420);
  tft.print("The Pump is");
  tft.setTextSize(2);

  // Start the service
  pService->start();

  // Start advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(false);
  pAdvertising->setMinPreferred(0x0);
  BLEDevice::startAdvertising();
  Serial.println("Waiting a client connection ...");

  pinMode(LED_PIN, OUTPUT);

  pinMode(trigPin, OUTPUT);  // Fixing the pin to output pin
  pinMode(echoPin, INPUT);   // Fixing the pin to input pin

  pinMode(WATERPUMP_PIN, OUTPUT);

  dht.begin();
}

void loop() {
  // Reading temperature or humidity takes about 250 milliseconds!
  float h = dht.readHumidity();
  // Read temperature as Celsius (the default)
  float t = dht.readTemperature();
  // Read temperature as Fahrenheit (isFahrenheit = true)
  //float f = dht.readTemperature(true);

  // Check if any reads failed and exit early (to try again).
  if (isnan(h) || isnan(t)) {
    Serial.println(F("Failed to read from DHT sensor!"));
    return;
  }

  Serial.print(F("%  Temperature: "));
  Serial.print(t, 1);
  Serial.print(F("°C "));
  Serial.print(F("Humidity: "));
  Serial.println(h, 1);
  
  LDR_value = 4095-analogRead(LDR_PIN); // light intensity
  if (LDR_value < 0 || LDR_value > 4095) {
    Serial.println(F("Failed to read from LDR sensor!"));
    return;
  }
  
  if(LDR_value < 900)
  {
    digitalWrite(LED_PIN, HIGH);  // The LED turns ON in Dark.
    delay(10);                    // Wait 10 milliseconds
  }
  else
  {
    digitalWrite(LED_PIN, LOW);   //The LED turns OFF in Light.
    delay(10);                    // Wait 10 milliseconds
  }

  Serial.print("LDR value: ");
  Serial.println(LDR_value);

  soilMoistureValue = 4095-analogRead(MOISTURE_PIN); // read the analog value from sensor
  if (soilMoistureValue < 0 || soilMoistureValue > 4095) {
    Serial.println(F("Failed to read from Soil Moisture sensor!"));
    return;
  }
  double soilMoisture_pre = map(soilMoistureValue, wet, dry, 100, 0); // convert the soil moisture into percent
  soilMoisture_int = (int)soilMoisture_pre; // convert to int

  if(soilMoisture_int <= 50) // If the soil moisture is below 50%, turn on the water pump.
  {
    digitalWrite(WATERPUMP_PIN, HIGH);
    delay(10); // Wait 10 milliseconds
  }
  else // If the soil moisture is 50% or higher, turn off the water pump.
  {
    digitalWrite(WATERPUMP_PIN, LOW);
    delay(10); // Wait 10 milliseconds
  }

  Serial.print("Moisture value: ");
  Serial.print(soilMoistureValue);
  Serial.print("   soilMoisture_pre: ");
  Serial.println(soilMoisture_int);
  
  digitalWrite(trigPin,LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin,LOW);

  pingtime = pulseIn(echoPin, HIGH);
  //Serial.print("pingtime: ");
 // Serial.print(pingtime);
  distance = (pingtime*2)*0.03429;
  Serial.print(", Distance: ");
  Serial.print(distance);
  Serial.println(" cm");

  tft.setTextColor(TFT_BLACK);
  tft.setTextSize(3);

  // Temperature value
  tft.setCursor(45, 50);
  tft.fillRect(45, 50, 100, 30, TFT_WHITE);  // Adjust x, y, width, and height as needed
  tft.print(t, 1);
  tft.print((char)247);
  tft.println("C");

  // Humidity value
  tft.setCursor(200, 50);
  tft.fillRect(200, 50, 100, 30, TFT_WHITE);  // Adjust x, y, width, and height as needed
  tft.print(h, 1);
  tft.println("%");

  // Water level (Empty: Red (TFT_CYAN), Low: Yellow (TFT_NAVY), Medium: Orange (TFT_BLUE), High: Green (TFT_MAGENTA)) and Full: Blue (TFT_YELLOW))
  tft.setTextSize(2);
  tft.setCursor(45, 260);
  tft.fillRect(45, 260, 100, 30, TFT_WHITE);  // Adjust x, y, width, and height as needed
  tft.setTextColor(TFT_MAGENTA);
  if (14 <= distance) 
  {
    tft.setTextColor(TFT_CYAN);
    tft.print("Empty");
    waterLevel = "Empty";
  }
  else if (10 <= distance && 14 > distance) 
  {
    tft.setTextColor(TFT_NAVY);
    tft.print("Low");
    waterLevel = "Low";

  } else if (7 <= distance && 10 > distance) 
  {
    tft.setTextColor(TFT_BLUE);
    tft.print("Medium");
    waterLevel = "Medium";

  } else if (4 <= distance && 7 > distance) 
  {
    tft.setTextColor(TFT_MAGENTA);
    tft.print("High");
    waterLevel = "High";

  } else if (3 >= distance) 
  {
    tft.setTextColor(TFT_YELLOW);
    tft.print("Full");
    waterLevel = "Full";
  }

  Serial.print("Waterlevel: ");
  Serial.println(waterLevel.c_str());

  tft.setTextColor(TFT_BLACK);

  // Soil Moisture value
  tft.setTextSize(3);
  tft.setCursor(240, 190);
  tft.fillRect(240, 190, 100, 30, TFT_WHITE);  // Adjust x, y, width, and height as needed
  tft.print(soilMoisture_int);
  tft.println("%");

  // Light brightness 
  tft.setCursor(10, 370);
  tft.fillRect(10, 370, 70, 30, TFT_WHITE);  // Adjust x, y, width, and height as needed
  tft.print(LDR_value);

  // LED Light Status
  tft.setCursor(55, 450);
  tft.fillRect(55, 450, 100, 30, TFT_WHITE);  // Adjust x, y, width, and height as needed
  if (digitalRead(LED_PIN) == HIGH)
  {
      tft.setTextColor(TFT_MAGENTA);
      tft.print("ON");
  } 
  else 
  {
      tft.setTextColor(TFT_CYAN);
      tft.print("OFF");
  }

  // Water pump Status /(TFT_MAGENTA = Grün) / (TFT_CYAN = Red)
  tft.setCursor(225, 450);
  tft.fillRect(225, 450, 100, 30, TFT_WHITE);  // Adjust x, y, width, and height as needed
  if (digitalRead(WATERPUMP_PIN) == HIGH) 
  {
      tft.setTextColor(TFT_MAGENTA);
      tft.print("ON");
  } 
  else 
  {
      tft.setTextColor(TFT_CYAN);
      tft.print("OFF");
  } 
  
  if (deviceConnected) {
    uint8_t temperature[4]; // A float is 4 bytes in size
    uint8_t humidity[4]; // A float is 4 bytes in size

    // Convert the float value to a byte array
    memcpy(temperature, &t, sizeof(t));
    // Notify the BLE client of the new temperature value
    temperatureCharacteristic->setValue(temperature, 4);
    temperatureCharacteristic->notify();

    memcpy(humidity, &h, sizeof(h));
    humidityCharacteristic->setValue(humidity, 4); // Die Größe des Wertes ist 4
    humidityCharacteristic->notify();

    // Notify the BLE client of the new LDR value
    byte LDR_value_bytes[2]; // An integer is 2 bytes in size
    // Convert the integer value to a byte array
    LDR_value_bytes[0] = (LDR_value >> 8) & 0xFF;
    LDR_value_bytes[1] = LDR_value & 0xFF;

    // Set and send the value
    LDRCharacteristic->setValue(LDR_value_bytes, 2);
    LDRCharacteristic->notify();

    // Notify the BLE client of the new water level value
    WaterLevelCharacteristic->setValue(waterLevel);
    WaterLevelCharacteristic->notify();

    // Notify the BLE client of the new moisture value
    byte soilMoisture_bytes[2];
    soilMoisture_bytes[0] = (soilMoisture_int >> 8) & 0xFF;
    soilMoisture_bytes[1] = soilMoisture_int & 0xFF;
    SoilMoistureCharacteristic->setValue(soilMoisture_bytes, 2); // Die Größe des Wertes ist 2
    SoilMoistureCharacteristic->notify();

    // Read the current status of the LED
    bool currentLedStatus = digitalRead(LED_PIN);
    int LEDStatus = currentLedStatus ? 1 : 0; // Convert current status to an integer
    byte LEDStatus_bytes[2];
    LEDStatus_bytes[0] = (LEDStatus >> 8) & 0xFF;
    LEDStatus_bytes[1] = LEDStatus & 0xFF;
    // Send to the client Android App the status of the LED
    LEDStatusCharacteristic->setValue(LEDStatus_bytes, 2);
    LEDStatusCharacteristic->notify();

    // Read the current status of the water pump
    bool currentWaterPumpStatus = digitalRead(WATERPUMP_PIN);
    int waterPumpStatus = currentWaterPumpStatus ? 1 : 0; // Convert current status to an integer
    byte waterPumpStatus_bytes[2];
    waterPumpStatus_bytes[0] = (waterPumpStatus >> 8) & 0xFF;
    waterPumpStatus_bytes[1] = waterPumpStatus & 0xFF;
    // Send to the client Android App the status of the LED
    WaterPumpStatusCharacteristic->setValue(waterPumpStatus_bytes, 2);
    WaterPumpStatusCharacteristic->notify();

    // Get the command LED from the Android App
    std::string controlLEDValue = LEDControlCharacteristic->getValue();
    if (controlLEDValue.find("ON") != std::string::npos) 
    {
      Serial.println("User Light ON");
      digitalWrite(LED_PIN, HIGH);
      tft.setCursor(55, 450);
      tft.fillRect(55, 450, 100, 30, TFT_WHITE);
      tft.setTextColor(TFT_MAGENTA);
      tft.print("ON");
      delay(10);
    } 
    else if (controlLEDValue.find("OFF") != std::string::npos) 
    {
      Serial.println("User Light OFF");
      digitalWrite(LED_PIN, LOW);
      tft.setCursor(55, 450);
      tft.fillRect(55, 450, 100, 30, TFT_WHITE);
      tft.setTextColor(TFT_CYAN);
      tft.print("OFF");
    } 
    
    // Get the command water pump from the Android App
    std::string controlPumpValue = WaterPumpControlCharacteristic->getValue();
    if (controlPumpValue.find("ON") != std::string::npos) 
    {
      Serial.println("User Pump ON");
      digitalWrite(WATERPUMP_PIN, HIGH);
      tft.setCursor(225, 450);
      tft.fillRect(225, 450, 100, 30, TFT_WHITE);
      tft.setTextColor(TFT_MAGENTA);
      tft.print("ON");
      delay(10);
    } 
    else if (controlPumpValue.find("OFF") != std::string::npos) 
    {
      Serial.println("User Pump OFF");
      digitalWrite(WATERPUMP_PIN, LOW);
      tft.setCursor(225, 450);
      tft.fillRect(225, 450, 100, 30, TFT_WHITE);
      tft.setTextColor(TFT_CYAN);
      tft.print("OFF");
    }
  }

  // disconnecting
  if (!deviceConnected && oldDeviceConnected) {
      delay(500); // give the bluetooth stack the chance to get things ready
      pServer->startAdvertising(); // restart advertising
      Serial.println("start advertising");
      oldDeviceConnected = deviceConnected;
  }

  // connecting
  if (deviceConnected && !oldDeviceConnected) {
      // do stuff here on connecting
      oldDeviceConnected = deviceConnected;
  }
  delay(400);
}