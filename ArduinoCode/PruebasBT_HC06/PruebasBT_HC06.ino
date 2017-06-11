/*  ----------------------------------------------------------------
    by Aitor De Blas
    
    Envio y recibo de datos mediante modulo Bluetooth HC-06
--------------------------------------------------------------------  
*/

#include <SoftwareSerial.h>

SoftwareSerial BT1(3,2); // RX, TX

char lyric[500]; // Desgraciadamente no he conseguido hacerlo con una variable de tipo String. Hay que reservar tamaño suficiente para guardar la cancion mas larga.
int idx=0; // Indice para recorrer el array de chars
char* hola; // Puntero a chars, variable final que necesitamos para luego reproducir la cancion.

void setup() 
{
    hola = "";
    Serial.begin(9600);
    Serial.println("Enter AT commands:");
    BT1.begin(9600);
}

void loop()
{
    /*
    // Esto escribe en pantalla lo que recibe por el modulo BT. Caracter a caracter. Cuando nos comunicamos solo para enviar comandos AT, poner la consola en modo: "No Line Ending".
    if (BT1.available())
           Serial.write(BT1.read());
    */
    /*   
    // Esto escribe en el canal del modulo BT aquello que lee en el cuadro de texto de comandos. 
    if (Serial.available())
      BT1.write(Serial.read());
    */  
    
    // Esto es lo mismo que el trozo comentado de abajo pero con un IF en vez de un WHILE
    if (BT1.available()) { 
      char readChar = BT1.read();
      lyric[idx]=readChar;
      Serial.print(lyric[idx]); // El error estaba en que haciamos un println en vez de print, y es posible que eso no imprimiera visualmente todo, a pesar de que el ARRAY si que estuviera completo
      idx++;
    } else {
      if (lyric[0] != '\0') {
        hola = &lyric[0]; Serial.print("Chars pointer: "); Serial.println(hola);
        lyric[0] = '\0';
      }
    }
    
    /*
    // Esto es lo mismo que el de arriba pero con un WHILE en vez de con un IF.
    while (BT1.available()) { 
      char readChar = BT1.read();
      lyric[idx]=readChar;
      Serial.print(lyric[idx]); // El error estaba en que haciamos un println en vez de print, y es posible que eso no imprimiera visualmente todo, a pesar de que el ARRAY si que estuviera completo
      idx++;
    } 
    if (!BT1.available()) {
      if (lyric[0] != '\0') {
        hola = &lyric[0]; Serial.print("Chars pointer: "); Serial.println(hola);
        lyric[0] = '\0';
      }
    }
    */
}
