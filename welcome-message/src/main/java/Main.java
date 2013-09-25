/************************************************************************
 * ORGANIZATION  :  SavageHomeAutomation
 * PROJECT       :  Simple RS232 Sample using Pi4J & Raspberry Pi
 * FILENAME      :  Main.java
 * **********************************************************************
 *
 * Copyright (C) 2013 Robert Savage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialDataEvent;
import com.pi4j.io.serial.SerialDataListener;
import com.pi4j.io.serial.SerialFactory;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

/**
 * This example code demonstrates how to create a simple Java program
 * to send and receive data via RS232 on the Raspberry Pi using
 * the Pi4J Java library.  This example is using a Pro-Lite LED
 * message sign (PL-M1014)
 *
 * @author Robert Savage
 */
public class Main {

    public static void main(String args[]) throws InterruptedException {

        // init JANSI
        // (using jansi for color console output)
        AnsiConsole.systemInstall();

        // display welcome message
        AnsiConsole.out().println(Ansi.ansi().fg(Ansi.Color.CYAN).a(Ansi.Attribute.INTENSITY_BOLD)
            .a("-----------------------------------------------\n")
            .a("\n")
            .a("Welcome to JavaOne 2013 - [[ CON7968 ]]\n")
            .a("\n")
            .a("  Let's Get Physical: I/O Programming with\n")
            .a("  Java on the Raspberry Pi with Pi4J\n")
            .a("\n")
            .a("-----------------------------------------------\n")
            .a("<--Pi4J--> Welcome Message ... started.\n")
            .a("-----------------------------------------------\n").reset());


        // ******************************************************************
        // INITIALIZE
        // ******************************************************************

        // create an instance of the serial communications class
        final Serial serial = SerialFactory.createInstance();

        // open the default serial port provided on the GPIO header
        // (this is where our LED reader is connected)
        serial.open(Serial.DEFAULT_COM_PORT, 2400);  // 2400 BAUD, N, 8, 1


        // ******************************************************************
        // SERIAL DATA EVENT LISTENER
        // ******************************************************************

        // create and register the serial data listener
        serial.addListener(new SerialDataListener() {
            @Override
            public void dataReceived(SerialDataEvent event) {
                // print out the data received to the console
                AnsiConsole.out().println(Ansi.ansi()
                        .fg(Ansi.Color.GREEN)
                        .a("[DATA RECEIVED] ")
                        .fg(Ansi.Color.YELLOW)
                        .a(event.getData())
                        .reset());
            }
        });

        // send "Welcome" message to sign
        serial.writeln("<ID01><PY>Welcome!  --  [ CON7968 ] -- Let's Get Physical: I/O Programming with Java on the Raspberry Pi & Pi4J -- ");
        Thread.sleep(1000);
        serial.writeln("<ID01><RPY>");
        Thread.sleep(1000);


        // ******************************************************************
        // PROGRAM TERMINATION
        // ******************************************************************

        // wait for user input to terminate program
        AnsiConsole.out().println(Ansi.ansi()
                .fg(Ansi.Color.BLACK)
                .bg(Ansi.Color.CYAN)
                .a("PRESS [ENTER] TO EXIT")
                .reset());
        System.console().readLine();

        // clear the display
        serial.writeln("<ID01><PY><FD>");
        Thread.sleep(1000);

        // shutdown the serial controller
        serial.shutdown();
    }
}

