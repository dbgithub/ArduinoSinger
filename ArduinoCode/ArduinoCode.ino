// Movilidad y computacion ubicua 
// Assignment 5 

// Authors: Aitor De Blas, Jorge Sarabia

#include <SPI.h>
#include <HttpClient.h>
#include <WiFi.h>
#include <stdlib.h> // Necessary for dtostrf()
#include <EEPROM.h> // necessary to work with EEPROM memory
#include <Servo.h> // library for Servo

// WiFi credentials for the router or AP.
char ssid[] = "MCU";      //  your network SSID (name) 
char pass[] = "nomorgan";   // your network password

// Name of the server we want to connect to:
const char hostname[] = "json.internetdelascosas.es";
// Prefix of the path for either temperature or air quality:
const char path[] = "/arduino/add.php?device_id=4&data_name=";
const char path_dvalue[] = "&data_value=";
// Full URL after attaching all values either for Temperature or Air Quality
char fullPath[200] = "";

// Number of milliseconds to wait without receiving any data before we give up
const int networkTimeout = 30*1000;
// Number of milliseconds to wait if no data is available before trying again
const int networkDelay = 1000;
// Every 30 seconds the device should send the captured values to the server.
// Instead of using 'delay' function, we will be using delta time.
unsigned long serverDeliveryRecordedTime;
const int serverDeliveryTimeout = 10*1000;

// PIN (temperature + light sensor + servo + RGB):
int temperaturePIN = A0;
int airqualityPIN = A2;
int servoPIN = 5; // Needs to be PWD
int rgbRedPIN = 9; // PWD pin: Pulse Width Modulation
int rgbGreenPIN = 6;  // PWD pin: Pulse Width Modulation
int rgbBluePIN = 3; // PWD pin: Pulse Width Modulation

// MIN and MAX recommended and calibrated global values (they will be read from EEPROM memory):
int min_calibrated_air;
int max_calibrated_air;
byte min_recom_temp;
byte max_recom_temp;

// Servo related variables:
Servo servo;

// Wi-Fi and Http client:
WiFiClient c;
HttpClient http(c);

// SETUP is executed just once
void setup()
{
  // initialize serial communications at 9600 bps:
  Serial.begin(9600);
  
  // Ataching the PIN to servo:
  servo.attach(servoPIN);
  servo.write(30); // re-set the initial position of the servo, just in case. 

  // check for the presence of the shield:
  if (WiFi.status() == WL_NO_SHIELD) {
    Serial.println("[WARNING]: WiFi shield not present"); 
    // Don't continue. We stuck the program in an infinite loop:
    while(true);
  } 

  connectToWiFi();
  // The four EEPROM values are read now and assigned to global variables.
  min_calibrated_air = EEPROM.read(0)*4; 
  max_calibrated_air = EEPROM.read(1)*4;
  min_recom_temp = EEPROM.read(2);
  max_recom_temp = EEPROM.read(3);
  Serial.print("EEPROM.read(0) = ");Serial.println(min_calibrated_air*4);Serial.print("EEPROM.read(1) = ");Serial.println(max_calibrated_air*4);Serial.print("EEPROM.read(2) = ");Serial.println(min_recom_temp);Serial.print("EEPROM.read(3) = ");Serial.println(max_recom_temp);
  
  // Set to OUTPUT the PINS of the LED RGB
  pinMode(rgbRedPIN, OUTPUT);
  pinMode(rgbGreenPIN, OUTPUT);
  pinMode(rgbBluePIN, OUTPUT);
  
  // We start counting 30 seconds from now.
  serverDeliveryRecordedTime = millis();
}

void loop()
{  
  // Temperature reading:
  float temperature = getVoltage(temperaturePIN);  //getting the voltage reading from the temperature sensor
  temperature = (temperature - .5) * 100;          //converting from 10 mv per degree wit 500 mV offset
                                                  //to degrees ((volatge - 500mV) times 100)
  char strFloatTemperature[10]; // The char array large enough to hold the string representation of the float                 
  dtostrf(temperature, 1, 2, strFloatTemperature); // store value in strfloat with 2 decimal positions, and total size at least 1 character. Note that the 3rd decimal will be rounded
  
  // Light sensor reading (air quality):
  int light = readAverageAnalogInput(airqualityPIN); // Reads the light level
  light = max_calibrated_air - light; // reverse the value so that it has logical sense
  int mappedLight = map(light, min_calibrated_air, max_calibrated_air, 0, 100); // maping from one range to another (a value to store in the server)
  int lightLevel = constrain(mappedLight, 0, 100);
  Serial.print("Air quality = ");
  Serial.print(lightLevel);
  Serial.print("% | Temperature = ");
  Serial.print(strFloatTemperature);
  Serial.println("*C");
  
  int caso = -1;
  if (min_calibrated_air <= lightLevel && lightLevel <= max_calibrated_air && min_recom_temp <= temperature && temperature <= max_recom_temp) {
    // GREEN PULSING SOFTLY
    analogWrite(rgbRedPIN, 0);
    analogWrite(rgbBluePIN, 0);
    // FADE IN:
    for(int fadeValue = 0 ; fadeValue <= 255; fadeValue +=5) {analogWrite(rgbGreenPIN, fadeValue);delay(30);} 
    // FADE OUT:
    for(int fadeValue = 255 ; fadeValue >= 0; fadeValue -=5) {analogWrite(rgbGreenPIN, fadeValue); delay(30);} 
  } else {
      // Checking temperature:
      if (temperature < min_recom_temp) {
        // FIXED YELLOW
        caso = 1;
      } else if (temperature > max_recom_temp) {
        // BLINKING YELLOW
        caso = 2;
      } 
      // Checking air quality;
      if (lightLevel < 20) {
        // FIXED RED
        caso = 3;
        if (lightLevel < 5) {
          // BLINKING RED
          caso = 4;
        }
      }
  } // END outside if
  
  switch (caso) {
    case 1:
      // FIXED YELLOW
      analogWrite(rgbRedPIN, 255);
      analogWrite(rgbGreenPIN, 255);
      analogWrite(rgbBluePIN, 0);
      break;
    case 2:
      // BLINKING YELLOW
      for (int k=0; k<5; k++) {analogWrite(rgbRedPIN, 255); analogWrite(rgbGreenPIN, 190); analogWrite(rgbBluePIN, 10);delay(300);analogWrite(rgbRedPIN, 0); analogWrite(rgbGreenPIN, 0); analogWrite(rgbBluePIN, 0);delay(300);}
      break;
    case 3:
      // FIXED RED
      analogWrite(rgbRedPIN, 255);
      analogWrite(rgbGreenPIN, 0);
      analogWrite(rgbBluePIN, 0);
      break;
    case 4: 
      // BLINKING RED
      for (int k=0; k<5; k++) {analogWrite(rgbRedPIN, 255); analogWrite(rgbGreenPIN, 0); analogWrite(rgbBluePIN, 0);delay(300);analogWrite(rgbRedPIN, 0); analogWrite(rgbGreenPIN, 0); analogWrite(rgbBluePIN, 0);delay(300);}
      break;
    default: 
      break;
  }
    
  // SERVO behaviour:
  if (lightLevel <= 20) {
    servo.write(120); // This means that the air purifier is OPEN
  } else {
    servo.write(30); // This means that the air purifier is CLOSE
  }
  
  // When a certain amount of seconds have elapsed, we wend the values to the server:
  if (millis() - serverDeliveryRecordedTime > serverDeliveryTimeout) {
    char str_lightlevel[255];
    sprintf(str_lightlevel,"%d",lightLevel);
    sendValuesToServer(0,strFloatTemperature);
    sendValuesToServer(1,str_lightlevel);  
    serverDeliveryRecordedTime = millis();
  }
  
}

// AUXILIARY METHODS -------------------------------------------------

// Connects to WiFi when demanded. This program features a resilient WiFi connection.
// After loosing signal, the device can search and re-connect to the network without rebooting.
void connectToWiFi(){
  int status = WiFi.status();
  // attempt to connect to Wifi network:
  while ( WiFi.status() != WL_CONNECTED) { 
    Serial.print("Attempting to connect to ");
    Serial.print(ssid);
    Serial.println("...");
    // Connect to WPA/WPA2 network. Change this line if using open or WEP network:    
    status = WiFi.begin(ssid, pass);

    // waiting for connection establishment (10 seconds):
    delay(10000);
  } 
  // you're connected now, now print out the status:
  printWifiStatus();
  Serial.println("---------------------------------------------------");
  Serial.println("---------------------------------------------------");
}

// Prints Wifi status
void printWifiStatus() {
  Serial.print("SSID: "); // SSID of the network you're attached to
  Serial.print(WiFi.SSID());

  IPAddress ip = WiFi.localIP(); // your WiFi shield's IP address
  Serial.print("IP Address: ");
  Serial.print(ip);

  long rssi = WiFi.RSSI(); // the received signal strength
  Serial.print("Signal strength (RSSI):");
  Serial.print(rssi);
  Serial.println(" dBm");
}

// Computes an average of different analog readings from the PIN passed as a parameter.
int readAverageAnalogInput(int pinInput){
  int sum = 0;
  for(int i=0;i<10;i++){
    sum += analogRead(pinInput);
    delay(100);
  }
  return sum/10;
}

/*
 * getVoltage() - returns the voltage on the analog input defined by parameter PIN
 */
float getVoltage(int pin){
 return (analogRead(pin) * .004882814); //converting from a 0 to 1023 digital range
                                        // to 0 to 5 volts (each 1 reading equals ~ 5 millivolts
}

// Sends temperature or light values captures from sensors to the server.
// It receives from parameter the value to attach to the query string as well as the data type.
// datatype can be:
// 0 => this means that the value is a temperature value
// 1 => this means that the value is a ligth sensor value
void sendValuesToServer(int datatype, char * value) {
    
  if (datatype == 0) {
    // 'value' is TEMPERATURE
    // Create final path:
    sprintf(fullPath,"%s%s%s",path,"temperature",path_dvalue);
    strcat(fullPath,value); // Concat strfloat at the end of text
  } else if (datatype == 1){
    // 'value' is LIGHT
    // Create final path:
    sprintf(fullPath,"%s%s%s",path, "airquality",path_dvalue);
    strcat(fullPath,value);
  }
  
  Serial.print("Requesting URL: ");
  Serial.print(fullPath); 
  Serial.println("...");
  
  int err = 0;
  err = http.get(hostname, fullPath);
  if (err == 0) {
    Serial.println("startedRequest ok");

    err = http.responseStatusCode();
    if (err >= 0) {
      Serial.print("Got status code: ");
      Serial.print(err);
      Serial.print(" | ");

      // Usually you'd check that the response code is 200 or a
      // similar "success" code (200-299) before carrying on,
      // but we'll print out whatever response we get

      err = http.skipResponseHeaders();
      if (err >= 0) {
        int bodyLen = http.contentLength();
        Serial.print("Content length is: ");
        Serial.println(bodyLen);
        Serial.println("Body returned follows:");
      
        // Now we've got to the body, so we can print it out
        unsigned long timeoutStart = millis();
        char c;
        // Whilst we haven't timed out & haven't reached the end of the body
        while ( (http.connected() || http.available()) &&
               ((millis() - timeoutStart) < networkTimeout) )
        {
            if (http.available())
            {
                c = http.read();
                // Print out this character
                Serial.print(c);
               
                bodyLen--;
                // We read something, reset the timeout counter
                timeoutStart = millis();
            }
            else {
                // We haven't got any data, so let's pause to allow some to
                // arrive
                delay(networkDelay);
            }
        }
      }
      else {
        Serial.print("Failed to skip response headers: ");
        Serial.println(err);
      }
    }
    else {    
      Serial.print("Getting response failed: ");
      Serial.println(err);
    }
  }
  else {
    Serial.print("Connect failed: ");
    Serial.println(err);
    if (WiFi.status() != WL_CONNECTED) {connectToWiFi();}
  }
  http.stop();
}
