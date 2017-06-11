// Movilidad y computacion ubicua 
// Final Project | Last assignment: Arduino & Android Project
// 12 · 06 · 2017

// Authors: Aitor De Blas, Jorge Sarabia

#include <SPI.h>
#include <HttpClient.h>
#include <WiFi.h>
#include <aJSON.h>
#include <Tone.h>
#include <EEPROM.h>
#include <SoftwareSerial.h> // this is for BT module. To make some PINs behave as Serial.

char ssid[] = "MCU";      //  your network SSID (name) 
char pass[] = "nomorgan";   // your network password

// Name of the server we want to connect to
const char hostname[] = "json.internetdelascosas.es";
// Path to use
const char pathGet[] = "/arduino/getlast.php?&nitems=1&device_id=4&data_name=";
const char pathAdd[] = "/arduino/add.php?device_id=4&data_name=";
const char path_dvalue[] = "&data_value=";
// Full URL after attaching all values either for uploading or retrieving.
char fullPath[110] = "";

// Max length of the reply content
#define CONTENT_MAX_LENGTH 300
char responseContent[CONTENT_MAX_LENGTH] = "";

// Number of milliseconds to wait without receiving any data before we give up
const int networkTimeout = 30*1000;
// Number of milliseconds to wait if no data is available before trying again
const int networkDelay = 1000;

// PINs (LED + button + motor + Bluetooth HC-06 + potentiometer)
const int ledPIN = 5;
const int buttonPIN = 6;
const int motorPIN = 9;
const int potentiometerPIN = A0;

// Wi-Fi and Http client:
WiFiClient c;
HttpClient http(c);

// Variables related to BT module (HC-06):
SoftwareSerial BT1(3,2); // RX of BT module -> PIN 02 of Arduino  || TX of BT module -> PIN 03 of Arduino
char lyric[500]; // A certain amount of memory is reserved for toring the lyric obtained from BT module.
int idx=0; // Indice para recorrer el array de chars
char* lyricPointer; // Puntero a chars, variable final que necesitamos para luego reproducir la cancion.

// Value read from EEPROM memory:
int numTonesEEPROM;

// Variables and directives related to Tone.h and RTTTL format (buzzer):
Tone tone1;
#define OCTAVE_OFFSET 0
int notes[] = { 0,
NOTE_C4, NOTE_CS4, NOTE_D4, NOTE_DS4, NOTE_E4, NOTE_F4, NOTE_FS4, NOTE_G4, NOTE_GS4, NOTE_A4, NOTE_AS4, NOTE_B4,
NOTE_C5, NOTE_CS5, NOTE_D5, NOTE_DS5, NOTE_E5, NOTE_F5, NOTE_FS5, NOTE_G5, NOTE_GS5, NOTE_A5, NOTE_AS5, NOTE_B5,
NOTE_C6, NOTE_CS6, NOTE_D6, NOTE_DS6, NOTE_E6, NOTE_F6, NOTE_FS6, NOTE_G6, NOTE_GS6, NOTE_A6, NOTE_AS6, NOTE_B6,
NOTE_C7, NOTE_CS7, NOTE_D7, NOTE_DS7, NOTE_E7, NOTE_F7, NOTE_FS7, NOTE_G7, NOTE_GS7, NOTE_A7, NOTE_AS7, NOTE_B7
};


// TO DELETE:
char* songName = "takeonme";
int myCounter;

// SETUP is executed just once
void setup()
{
  // Initialize serial communications at 9600 bps:
  Serial.begin(9600);
  BT1.begin(9600); // for BT module
  
  // Check for the presence of the shield:
  if (WiFi.status() == WL_NO_SHIELD) {
    Serial.println("[WARNING]: WiFi shield not present"); 
    // Don't continue. We stuck the program in an infinite loop:
    while(true);
  } 

  // The four EEPROM values are read now and assigned to global variables.
  numTonesEEPROM = EEPROM.read(0);
  Serial.print("EEPROM.read(0) = ");Serial.println(numTonesEEPROM);
  
  tone1.begin(8); // The buzzer works through PIN 8, here is used!
  // Set to OUTPUT the PINs for LEDs:
  pinMode(ledPIN, OUTPUT);
  pinMode(motorPIN, OUTPUT);
  
  lyricPointer = "";

  //connectToWiFi();
  
  // TO DELETE!!!!!! :
  myCounter = 0;
  
}

void loop()
{
  while (BT1.available()) { 
      char readChar = BT1.read();
      lyric[idx]=readChar;
      Serial.print(lyric[idx]); // El error estaba en que haciamos un println en vez de print, y es posible que eso no imprimiera visualmente todo, a pesar de que el ARRAY si que estuviera completo
      idx++;
    } 
   
   if (BT1.available() <= 0) {
      if (lyric[0] != '\0') {
        lyricPointer = &lyric[0]; Serial.print("Chars pointer: "); Serial.println(lyricPointer);        
        // Now play song and start the actuators PARTY!           
        digitalWrite(motorPIN, HIGH); // Run the motor!
        play_rtttl(lyricPointer); // Play the song
        digitalWrite(motorPIN, LOW); // Stop the motor!
        digitalWrite(ledPIN, LOW); // Stop lighting!
        idx = 0;
        memset(lyric,'\0',sizeof(lyric));
      }
    }
  //getSendValuesToServer(0,myCounter,songName);
  //myCounter++;
  
}

// Gets from or Sends to the backend the updated value of hoy many times the song has been played
// The URL contains three query strings: 
// · device_id = The ID of the device
// · data_name = the name of the song from which we will update the number of times played
// · data_value = the counter
// 'Operation' parameter meaning:
// 0 => this means that we are doing GET (retrieve value)
// 1 => this means that we are doing "POST" (send value)
/*
void getSendValuesToServer(int operation, int counter, char* songname) {
  float obtainedValue;
  int err = 0;
  
  if (operation == 0) {
    sprintf(fullPath,"%s%s",pathGet,songname);
  } else if (operation == 1) {
    sprintf(fullPath,"%s%s%s%d",pathAdd,songname,path_dvalue,counter);
  }
   
  Serial.print("Requesting URL: ");
  Serial.print(fullPath);
  Serial.println("...");
   
  err = http.get(hostname, fullPath);
  if (err == 0) {
    Serial.println("startedRequest ok");

    err = http.responseStatusCode();
    if (err >= 0) {
      Serial.print("Got status code: ");
      Serial.print(err);

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
                if (operation == 0) {
                  // Add read char to the respone content
                  int lastPos = strlen(responseContent);
                  responseContent[lastPos] = c;
                  responseContent[++lastPos] = '\0';              
                } else if (operation == 1) {
                  // Print out this character
                  Serial.print(c);
                }
                
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
        if (operation == 0) {
          // End of while, responseContent fully formed
          Serial.println(responseContent);
          Serial.print("responseContent: ");
          //obtainedValue = getValueFromJson(responseContent);
          //Serial.print("obtainedValue = "); Serial.println(obtainedValue);
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
*/

// Connects to WiFi when demanded. This program features a resilient WiFi connection.
// After loosing signal, the device can search and re-connect to the network without rebooting.
void connectToWiFi(){
  int status = WiFi.status();
  // attempt to connect to Wifi network:
  while ( WiFi.status() != WL_CONNECTED) { 
    Serial.print("Attempting to connect to SSID: "); Serial.print(ssid); Serial.println("...");
    // Connect to WPA/WPA2 network. Change this line if using open or WEP network:    
    status = WiFi.begin(ssid, pass);

    // wait 10 seconds for connection:
    delay(10000);
  } 
  // you're connected now, so print out the status:
  printWifiStatus();
  Serial.println("---------------------------------------------------");
  Serial.println("---------------------------------------------------");
}

// Prints Wifi status
void printWifiStatus() {
  // Print the SSID of the network you're attached to:
  Serial.print("SSID: ");
  Serial.print(WiFi.SSID());

  // Print your WiFi shield's IP address:
  IPAddress ip = WiFi.localIP();
  Serial.print(" IP Address: ");
  Serial.print(ip);

  // Print the received signal strength:
  long rssi = WiFi.RSSI();
  Serial.print(" Signal strength (RSSI):");
  Serial.print(rssi);
  Serial.println(" dBm");
}

// Util function to parse JSON and extract desired information. In this case, just the counter.
/*
float getValueFromJson(char* json_string){
  float value;
  aJsonObject* root = aJson.parse(json_string);
  aJsonObject* first = aJson.getArrayItem(root, 0);
  aJsonObject* json_value = aJson.getObjectItem(first, "data_value");
  
  if(json_value->type == aJson_Int)
    value = json_value->valueint;
   else
    value = json_value->valuefloat;

  // Super-important. Once done, deleteto root element to free memory!!
  aJson.deleteItem(root);
  return value;
}
*/



// -----------------------------------------
// METHODS/FUNCTIONS RELATED TO PLAYING SONG:
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

    // NOW, we check whether we have to stop from playing the song depending on whether the user has pushed the button or not.
    int val = digitalRead(buttonPIN); // read input value
    if (val == LOW) { // check if the input is LOW
      Serial.print("Button value: "); Serial.print(val); Serial.println(" (pressed!)");
      break;
    }
    
    // Now play the note
    if(note)
    {
      //Serial.print("eeprom: "); Serial.println(EEPROM.read(0));
      /*Serial.print("Playing: ");
      Serial.print(scale, 10); Serial.print(' ');
      Serial.print(note, 10); Serial.print(" (");
      Serial.print(notes[(scale - 4) * 12 + note], 10);
      Serial.print(") ");
      Serial.println(duration, 10);*/
      tone1.play(notes[(scale - 4) * 12 + note + numTonesEEPROM/2 -(analogRead(potentiometerPIN)/(1000/numTonesEEPROM))]); // Here is where we can change the TONE of the song based on the value of the potentiometer
      //Serial.print("thisNote: "); Serial.println(thisNote);
      if (thisNote%2==0){
        digitalWrite(ledPIN, HIGH);
      }
      else if (thisNote%2==1){    
        digitalWrite(ledPIN, LOW);
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
