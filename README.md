# Smart-Garden
- The system consists of two software components: C++ for the ESP32 server in the Arduino IDE and Java programming in Android Studio for the Android app.
- In this project, a prototype for a smart garden system will be developed and tested. The prototype enables the collection, control and visualization of environmental data for irrigation and lighting in the garden.
The prototype consists of a central control unit ESP32, which is connected to various sensors and actuators. These sensors are designed to measure environmental parameters such as soil moisture, temperature and humidity. The collected data is recorded and analyzed by the ESP32 microcontroller. The actuators in this prototype enable the control of lighting and irrigation in the garden. They can be controlled depending on the environmental data collected to ensure optimal conditions for plant growth.
Additionally, the prototype has an interface that allows the user to view the data in real time on an ILI9341 TFT LCD display or an Android app.

![Software_Abbildung](https://github.com/Boulmani96/Smart-Garden/assets/74252189/7b4760e6-1112-498e-9435-74518aa328ca)
# Flow diagram
![Diagramm drawio](https://github.com/Boulmani96/Smart-Garden/assets/74252189/dc19e00c-a363-4231-8677-03ad46e9a8ba)

# Communication between components
![System_Diagram](https://github.com/Boulmani96/Smart-Garden/assets/74252189/225ecb3f-78cb-4f8e-afcf-57282b76d0c3)

# Description and mapping of the prototype
In the first phase of the study, a prototype for a smart garden system will be developed and tested. The prototype enables the recording, control and visualization of environmental data for irrigation and lighting in the garden.
The prototype consists of a central control unit ESP32, which is connected to various sensors and actuators. These sensors are designed to measure environmental parameters such as soil moisture, temperature, humidity, water level and ambient brightness. The collected data is recorded by the ESP32 server.
The actuators in this prototype make it possible to control the lighting and irrigation in the Smart Garden System. They can be controlled according to the recorded environmental data to ensure optimal conditions for plant growth.
In addition, the prototype has an interface that allows the user to display the data in real time on an ILI9341 TFT LCD display or an Android app.

# Demonstration of the developed prototype system
The image shows the prototype in brightness. The LDR light sensor measures the brightness and controls the LED plant lamp. The pump switches on automatically when the soil moisture sensor reports that the soil is dry. The ultrasonic water level sensor shows how much water is in the tank. The Android app and the display show all data.
![Helles Umgebungslicht](https://github.com/Boulmani96/Smart-Garden/assets/74252189/66ed9903-708e-475d-b063-f92011dd72a6)

The image shows the prototype in the dark with the LED lighting activated. In this situation, the brightness sensor detects a low ambient brightness, which leads to the activation of the LED plant lamp.
