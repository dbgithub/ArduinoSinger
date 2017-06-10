// (c) Copyright 2010-2012 MCQN Ltd.
// Released under Apache License, version 2.0
//
// Simple example to show how to use the HttpClient library
// Get's the web page given at http://<kHostname><kPath> and
// outputs the content to the serial port

#include <SPI.h>
#include <HttpClient.h>
#include <WiFi.h>
#include <aJSON.h>
#include <Tone.h>
#include <EEPROM.h>

char ssid[] = "MCU";      //  your network SSID (name) 
char pass[] = "nomorgan";   // your network password

// Name of the server we want to connect to
const char kHostname[] = "json.internetdelascosas.es";
// Path to use
const char kPath[] = "/arduino/getlast.php?device_id=4&data_name=TakeOnMe&nitems=1";
const char path[] = "/arduino/add.php?device_id=4&data_name=";
const char path_dvalue[] = "&data_value=";
// Full URL after attaching all values either for Temperature or Air Quality
char fullPath[200] = "";

// Max length of the reply content
#define CONTENT_MAX_LENGTH 300
char responseContent[CONTENT_MAX_LENGTH]="";

// Number of milliseconds to wait without receiving any data before we give up
const int kNetworkTimeout = 30*1000;
// Number of milliseconds to wait if no data is available before trying again
const int kNetworkDelay = 1000;
// Every 30 seconds the device should send the captured values to the server.
// Instead of using 'delay' function, we will be using delta time.
unsigned long serverDeliveryRecordedTime;
const int serverDeliveryTimeout = 10*1000;

const int ledPin = 5;
const int inputPin = 6;
const int motorPin = 9;

// Wi-Fi and Http client:
WiFiClient c;
HttpClient http(c);

Tone tone1;

#define OCTAVE_OFFSET 0

int notes[] = { 0,
NOTE_C4, NOTE_CS4, NOTE_D4, NOTE_DS4, NOTE_E4, NOTE_F4, NOTE_FS4, NOTE_G4, NOTE_GS4, NOTE_A4, NOTE_AS4, NOTE_B4,
NOTE_C5, NOTE_CS5, NOTE_D5, NOTE_DS5, NOTE_E5, NOTE_F5, NOTE_FS5, NOTE_G5, NOTE_GS5, NOTE_A5, NOTE_AS5, NOTE_B5,
NOTE_C6, NOTE_CS6, NOTE_D6, NOTE_DS6, NOTE_E6, NOTE_F6, NOTE_FS6, NOTE_G6, NOTE_GS6, NOTE_A6, NOTE_AS6, NOTE_B6,
NOTE_C7, NOTE_CS7, NOTE_D7, NOTE_DS7, NOTE_E7, NOTE_F7, NOTE_FS7, NOTE_G7, NOTE_GS7, NOTE_A7, NOTE_AS7, NOTE_B7
};

char* songName = "TakeOnMe";
char *song = "TakeOnMe:d=4,o=4,b=160:8f#5,8f#5,8f#5,8d5,8p,8b,8p,8e5,8p,8e5,8p,8e5,8g#5,8g#5,8a5,8b5,8a5,8a5,8a5,8e5,8p,8d5,8p,8f#5,8p,8f#5,8p,8f#5,8e5,8e5,8f#5,8e5,8f#5,8f#5,8f#5,8d5,8p,8b,8p,8e5,8p,8e5,8p,8e5,8g#5,8g#5,8a5,8b5,8a5,8a5,8a5,8e5,8p,8d5,8p,8f#5,8p,8f#5,8p,8f#5,8e5,8e5";

void setup()
{
  // initialize serial communications at 9600 bps:
  Serial.begin(9600);
  
  // check for the presence of the shield:
  if (WiFi.status() == WL_NO_SHIELD) {
    Serial.println("[WARNING]: WiFi shield not present"); 
    // Don't continue. We stuck the program in an infinite loop:
    while(true);
  } 

  tone1.begin(8);
  pinMode(ledPin, OUTPUT);
  pinMode(motorPin, OUTPUT);

  connectToWiFi();
  
  // We start counting 30 seconds from now.
  serverDeliveryRecordedTime = millis();
}

void loop()
{
  // When a certain amount of seconds have elapsed, we wend the values to the server:
  if (millis() - serverDeliveryRecordedTime > serverDeliveryTimeout) {
    digitalWrite(motorPin, HIGH);
    play_rtttl(song);
    digitalWrite(motorPin, LOW);
    digitalWrite(ledPin, LOW);
    sendValuesToServer(5,songName);
    serverDeliveryRecordedTime = millis();
  }
  
}

void sendValuesToServer(int value, char* song) {
  sprintf(fullPath,"%s%s%s",path,song,path_dvalue);
  char str_value[255];
  sprintf(str_value,"%d",value);
  strcat(fullPath,str_value); // Concat strfloat at the end of text
  
  Serial.print("Requesting URL: ");
  Serial.print(fullPath);
  Serial.println("...");
  
  int err = 0;
  err = http.get(kHostname, fullPath);
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
               ((millis() - timeoutStart) < kNetworkTimeout) )
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
                delay(kNetworkDelay);
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

void connectToWiFi(){
  int status = WiFi.status();
  // attempt to connect to Wifi network:
  while ( WiFi.status() != WL_CONNECTED) { 
    Serial.print("Attempting to connect to SSID: ");
    Serial.println(ssid);
    // Connect to WPA/WPA2 network. Change this line if using open or WEP network:    
    status = WiFi.begin(ssid, pass);

    // wait 10 seconds for connection:
    delay(10000);
  } 
  // you're connected now, so print out the status:
  printWifiStatus();
}

void printWifiStatus() {
  // print the SSID of the network you're attached to:
  Serial.print("SSID: ");
  Serial.println(WiFi.SSID());

  // print your WiFi shield's IP address:
  IPAddress ip = WiFi.localIP();
  Serial.print("IP Address: ");
  Serial.println(ip);

  // print the received signal strength:
  long rssi = WiFi.RSSI();
  Serial.print("Signal strength (RSSI):");
  Serial.print(rssi);
  Serial.println(" dBm");
}

#define isdigit(n) (n >= '0' && n <= '9')

void play_rtttl(char *p)
{
  // Absolutely no error checking in here

  byte default_dur = 4;
  byte default_oct = 6;
  int bpm = 63;
  int num;
  long wholenote;
  long duration;
  byte note;
  byte scale;
  int thisNote = 0;

  // format: d=N,o=N,b=NNN:
  // find the start (skip name, etc)

  while(*p != ':') p++;    // ignore name
  p++;                     // skip ':'

  // get default duration
  if(*p == 'd')
  {
    p++; p++;              // skip "d="
    num = 0;
    while(isdigit(*p))
    {
      num = (num * 10) + (*p++ - '0');
    }
    if(num > 0) default_dur = num;
    p++;                   // skip comma
  }

  //Serial.print("ddur: "); Serial.println(default_dur, 10);

  // get default octave
  if(*p == 'o')
  {
    p++; p++;              // skip "o="
    num = *p++ - '0';
    if(num >= 3 && num <=7) default_oct = num;
    p++;                   // skip comma
  }

  //Serial.print("doct: "); Serial.println(default_oct, 10);

  // get BPM
  if(*p == 'b')
  {
    p++; p++;              // skip "b="
    num = 0;
    while(isdigit(*p))
    {
      num = (num * 10) + (*p++ - '0');
    }
    bpm = num;
    p++;                   // skip colon
  }

  //Serial.print("bpm: "); Serial.println(bpm, 10);

  // BPM usually expresses the number of quarter notes per minute
  wholenote = (60 * 1000L / bpm) * 4;  // this is the time for whole note (in milliseconds)

  //Serial.print("wn: "); Serial.println(wholenote, 10);


  // now begin note loop
  while(*p)
  {
    // first, get note duration, if available
    num = 0;
    while(isdigit(*p))
    {
      num = (num * 10) + (*p++ - '0');
    }
   
    if(num) duration = wholenote / num;
    else duration = wholenote / default_dur;  // we will need to check if we are a dotted note after

    // now get the note
    note = 0;

    switch(*p)
    {
      case 'c':
        note = 1;
        break;
      case 'd':
        note = 3;
        break;
      case 'e':
        note = 5;
        break;
      case 'f':
        note = 6;
        break;
      case 'g':
        note = 8;
        break;
      case 'a':
        note = 10;
        break;
      case 'b':
        note = 12;
        break;
      case 'p':
      default:
        note = 0;
    }
    p++;

    // now, get optional '#' sharp
    if(*p == '#')
    {
      note++;
      p++;
    }

    // now, get optional '.' dotted note
    if(*p == '.')
    {
      duration += duration/2;
      p++;
    }
 
    // now, get scale
    if(isdigit(*p))
    {
      scale = *p - '0';
      p++;
    }
    else
    {
      scale = default_oct;
    }

    scale += OCTAVE_OFFSET;

    if(*p == ',')
      p++;       // skip comma for next note (or we may be at the end)

    int val = digitalRead(inputPin); // read input value
    Serial.print("val: "); Serial.println(val);
    if (val == LOW) { // check if the input is HIGH
      break;
    }
    
    // now play the note
    if(note)
    {
      Serial.print("eeprom: "); Serial.println(EEPROM.read(0));
      /*Serial.print("Playing: ");
      Serial.print(scale, 10); Serial.print(' ');
      Serial.print(note, 10); Serial.print(" (");
      Serial.print(notes[(scale - 4) * 12 + note], 10);
      Serial.print(") ");
      Serial.println(duration, 10);*/
      tone1.play(notes[(scale - 4) * 12 + note + EEPROM.read(0)/2 -(analogRead(0)/(1000/EEPROM.read(0)))]);
      //Serial.print("thisNote: "); Serial.println(thisNote);
      if (thisNote%2==0){
        digitalWrite(ledPin, HIGH);
      }
      else if (thisNote%2==1){    
        digitalWrite(ledPin, LOW);
      }
      thisNote++;
      delay(duration);
      tone1.stop();
    }
    else
    {
      /*Serial.print("Pausing: ");
      Serial.println(duration, 10);*/
      delay(duration);
    }
  }
}
