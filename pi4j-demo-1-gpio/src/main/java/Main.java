/************************************************************************
 * ORGANIZATION  :  SavageHomeAutomation
 * PROJECT       :  Simple GPIO Sample using Pi4J & Raspberry Pi
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

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

/**
 * This example code demonstrates how to create a simple Java program
 * to listen for button presses to control an LED using a Raspberry Pi
 * and the Pi4J Java library.
 *
 * @author Robert Savage
 */
public class Main {

    // create GPIO controller
    private static final GpioController gpio = GpioFactory.getInstance();


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
            .a("<--Pi4J--> GPIO Example ... started.\n")
            .a("-----------------------------------------------\n").reset());


        // ******************************************************************
        // INITIALIZE
        // ******************************************************************

        // momentary push-button switch; activates when button is pushed
        final GpioPinDigitalInput buttonPin = gpio.provisionDigitalInputPin(RaspiPin.GPIO_06, PinPullResistance.PULL_UP);

        // led; illuminates when GPIO is HI
        final GpioPinDigitalOutput ledPin =  gpio.provisionDigitalOutputPin(RaspiPin.GPIO_07, PinState.LOW);

        // make sure the LED is turned off when program shuts down
        gpio.setShutdownOptions(true, PinState.LOW, ledPin);


        // ******************************************************************
        // GPIO EVENT LISTENER(S)
        // ******************************************************************

        // create button event listener
        buttonPin.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {

                // display console message
                AnsiConsole.out().println(Ansi.ansi().fg(Ansi.Color.WHITE)
                        .a("Button pressed: state = " + event.getState()).reset());

                if(event.getState().isHigh()){
                    // turn off LED pin
                    ledPin.setState(PinState.LOW);
                }
                else{
                    // turn on LED pin
                    ledPin.setState(PinState.HIGH);
                }
            }
        });

        // display console message when LED pin state changes
        ledPin.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if(event.getState().isHigh()){
                    AnsiConsole.out().println(Ansi.ansi().fg(Ansi.Color.GREEN).a("LED is ON").reset());
                }
                else{
                    AnsiConsole.out().println(Ansi.ansi().fg(Ansi.Color.RED).a("LED is OFF").reset());
                }
            }
        });


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

        // shutdown GPIO controller
        gpio.shutdown();
    }
}

