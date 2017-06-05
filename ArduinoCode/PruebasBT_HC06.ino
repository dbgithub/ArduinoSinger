/*  ----------------------------------------------------------------
    www.prometec.net
    Prog_70_1
    
    En viando y recibiendo datos del Bluettoth
    con un modulo HC-06
--------------------------------------------------------------------  
*/

#include <SoftwareSerial.h>

SoftwareSerial BT1(3,2); // RX, TX

void setup() 
{
    Serial.begin(9600);
    Serial.println("Enter AT commands:");
    
    BT1.begin(9600);
}

void loop()
{
    if (BT1.available())
           Serial.write(BT1.read());
           
    if (Serial.available())
      {  
        BT1.write(Serial.read());
        //BT1.print("AT+VERSION");
        //delay(4000);
      }   
}

String GetLine()  
    {   String S = "" ;
        if (Serial.available())
            {    char c = Serial.read(); ;
                 while ( c != '\n')                           //Hasta que el character sea intro
                       {     S = S + c ;
                             delay(25) ;
                             c = Serial.read();
                       }
                 return( S + '\n') ;
            }
    }
