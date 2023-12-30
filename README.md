# Smart-Garden
- The system consists of two software components: C++ for the ESP32 server in the Arduino IDE and Java programming in Android Studio for the Android app.
- The system consists of two main software components, an ESP32 server and an Android app. The ESP32 server is responsible for managing the hardware components of the system, such as the sensors and actuators. The Android app is responsible 
  for the user interface and data visualization. It allows the user to display the sensor data, switch the LED plant lamp and the water pump on or off.
![Software_Abbildung](https://github.com/Boulmani96/Smart-Garden/assets/74252189/7b4760e6-1112-498e-9435-74518aa328ca)
# Flow diagram
![Diagramm drawio](https://github.com/Boulmani96/Smart-Garden/assets/74252189/dc19e00c-a363-4231-8677-03ad46e9a8ba)

# Communication between components
![System_Diagram](https://github.com/Boulmani96/Smart-Garden/assets/74252189/225ecb3f-78cb-4f8e-afcf-57282b76d0c3)

# Description and mapping of the prototype
In this project, a prototype for a smart garden system will be developed and tested. The prototype enables the recording, control and visualization of environmental data for irrigation and lighting in the garden.
The prototype consists of a central control unit ESP32, which is connected to various sensors and actuators. These sensors are designed to measure environmental parameters such as soil moisture, temperature, humidity, water level and ambient brightness. The collected data is recorded by the ESP32 server.
The actuators in this prototype make it possible to control the lighting and irrigation in the Smart Garden System. They can be controlled according to the recorded environmental data to ensure optimal conditions for plant growth.
In addition, the prototype has an interface that allows the user to display the data in real time on an ILI9341 TFT LCD display or an Android app.

# Demonstration of the developed prototype system
The image shows the prototype in brightness. The LDR light sensor measures the brightness and controls the LED plant lamp. The pump switches on automatically when the soil moisture sensor reports that the soil is dry. The ultrasonic water level sensor shows how much water is in the tank. The Android app and the display show all data.
![Helles Umgebungslicht](https://github.com/Boulmani96/Smart-Garden/assets/74252189/66ed9903-708e-475d-b063-f92011dd72a6)

The image shows the prototype in the dark with the LED lighting activated. In this situation, the brightness sensor detects a low ambient brightness, which leads to the activation of the LED plant lamp.
![Dunkles Umgebungslicht](https://github.com/Boulmani96/Smart-Garden/assets/74252189/bc58940c-2c4f-4b05-baca-2ad67091d133)
