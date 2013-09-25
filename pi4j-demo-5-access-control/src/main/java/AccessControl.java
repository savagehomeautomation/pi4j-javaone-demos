/************************************************************************
 * ORGANIZATION  :  SavageHomeAutomation
 * PROJECT       :  Access Control using Pi4J & Raspberry Pi
 * FILENAME      :  AccessControl.java
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

import com.pi4j.component.light.LED;
import com.pi4j.component.light.impl.GpioLEDComponent;
import com.pi4j.component.relay.Relay;
import com.pi4j.component.relay.RelayListener;
import com.pi4j.component.relay.RelayState;
import com.pi4j.component.relay.RelayStateChangeEvent;
import com.pi4j.component.relay.impl.GpioRelayComponent;
import com.pi4j.component.sensor.Sensor;
import com.pi4j.component.sensor.SensorListener;
import com.pi4j.component.sensor.SensorState;
import com.pi4j.component.sensor.SensorStateChangeEvent;
import com.pi4j.component.sensor.impl.GpioSensorComponent;
import com.pi4j.component.switches.MomentarySwitch;
import com.pi4j.component.switches.SwitchListener;
import com.pi4j.component.switches.SwitchState;
import com.pi4j.component.switches.SwitchStateChangeEvent;
import com.pi4j.component.switches.impl.GpioMomentarySwitchComponent;
import com.pi4j.io.gpio.*;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialDataEvent;
import com.pi4j.io.serial.SerialDataListener;
import com.pi4j.io.serial.SerialFactory;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

/**
 * This example code demonstrates how to create an access control
 * system using a Raspberry Pi and the Pi4J Java library.
 *
 * Please see the complete article for parts list, wiring diagrams, etc. at
 * http://www.savagehomeautomation.com/pi4j-access-control
 *
 * ATTENTION : THIS CODE IS PROVIDED A AN EXAMPLE ONLY.  THIS SAMPLE
 *             DOES NOT COVER ALL POTENTIAL ACCESS CONTROL USE CASES
 *             AND SECURITY RISKS.  THIS CODE MAY BE USED AS A STARTING
 *             POINT TO CREATING YOUR OWN ACCESS CONTROL SYSTEM.
 *             USE AT YOUR OWN RISK.  AUTHORS AND/OR CONTRIBUTORS
 *             ASSUME NO LIABILITY AND ARE NOT LIABLE FOR ANY LOSSES
 *             RELATED TO THE USE OF THIS CODE AND/OR PROJECT.
 * 
 * @author Robert Savage
 */
public class AccessControl {

    public static final boolean EMAIL_ENABLED = false; // TODO : ENABLED EMAIL TO IFTTT HERE
    public static final String EMAIL_FROM = ""; //TODO : YOUR IFTTT EMAIL GOES HERE
    public static final String EMAIL_TO = "trigger@ifttt.com";
    public static final String EMAIL_SERVER = "aspmx.l.google.com";
    public static final int EMAIL_PORT = 25;

    public static final String IFTTT_TAG_SECURITY_ALERT = "#security-alert";
    public static final String IFTTT_TAG_ACCESS_LOG = "#access-log";

    // create GPIO controller
    private static final GpioController gpio = GpioFactory.getInstance();


    // ******************************************************************
    // INITIALIZE SENSORS, SWITCHES, RELAYS, etc.
    // ******************************************************************

    // create doorbell switch
    // (momentary push-button switch; activates when button is pushed)
    private static MomentarySwitch doorbellSwitch = new GpioMomentarySwitchComponent(
            gpio.provisionDigitalInputPin(RaspiPin.GPIO_01, PinPullResistance.PULL_UP),
            PinState.HIGH,
            PinState.LOW);   // this switch is configured to trigger ON with pin LOW

    // create manual override switch
    // (momentary push-button switch; activates when button is pushed)
    private static MomentarySwitch overrideSwitch = new GpioMomentarySwitchComponent(
            gpio.provisionDigitalInputPin(RaspiPin.GPIO_06, PinPullResistance.PULL_UP),
            PinState.HIGH,
            PinState.LOW);   // this switch is configured to trigger ON with pin LOW

    // create keypad tamper switch
    // (momentary push-button switch; activates when button is pushed)
    private static MomentarySwitch tamperSwitch = new GpioMomentarySwitchComponent(
            gpio.provisionDigitalInputPin(RaspiPin.GPIO_03, PinPullResistance.PULL_UP),
            PinState.LOW,
            PinState.HIGH);   // this switch is configured to trigger ON with pin LOW

    // create door sensor
    // (magnetic door sensor; senses door opened/closed events)
    private static Sensor doorSensor = new GpioSensorComponent(
            gpio.provisionDigitalInputPin(RaspiPin.GPIO_00, PinPullResistance.PULL_UP),
            PinState.HIGH,
            PinState.LOW);   // this sensor is configured as "Closed" when the pin goes LOW

    // create unlock sensor
    // (activates when keypad request unlock event)
    private static Sensor keypadUnlockSensor = new GpioSensorComponent(
            gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_UP),
            PinState.HIGH,
            PinState.LOW);   // this sensor is configured as "Open" when the pin goes LOW

    // create door lock relay controller
    // (when relay is latched, the door solenoid is actuated, thus unlocking the door)
    private static Relay lockRelay =  new GpioRelayComponent(
            gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, PinState.LOW));

    // create door opener request relay controller
    // (when relay is latched, a request is sent to the keypad controller to unlock the door)
    private static Relay keypadOpenerRelay =  new GpioRelayComponent(
            gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, PinState.LOW));

    // define components
    private static LED securityLed = new GpioLEDComponent(
            gpio.provisionDigitalOutputPin(RaspiPin.GPIO_07, PinState.LOW));

    // static state members
    private static SecurityViolation securityViolation = SecurityViolation.None;

    // create an instance of the serial communications class
    private static final Serial serial = SerialFactory.createInstance();

    private static final String LOADING = "A";
    private static final String SYSTEM_READY = "B";
    private static final String DOORBELL = "C";
    private static final String DOOR_OPENED = "D";
    private static final String DOOR_CLOSED = "E";
    private static final String APPROVED = "F";
    private static final String DENIED = "G";
    private static final String SECURITY = "H";
    private static final String EMPTY = "Y";

    public static void main(String args[]) throws Exception {

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
            .a("<--Pi4J--> Security Access Example ... started.\n")
            .a("-----------------------------------------------\n").reset());


        // ******************************************************************
        // INIT & CONFIGURE LED MESSAGE READER
        // ******************************************************************

        // open the default serial port provided on the GPIO header
        // (this is where our LED reader is connected)
        serial.open(Serial.DEFAULT_COM_PORT, 2400);  // 2400 BAUD, N, 8, 1

        // configure the LED message display
        // (setup pages of display text)
        configureMessage(LOADING, "<FD><CL>LOADING...");
        configureMessage(SYSTEM_READY, "<FD>SYSTEM READY");
        configureMessage(DOORBELL, "<FD><CQ>DOORBELL<FO>");
        configureMessage(DOOR_OPENED, "<FD><CK>OPENED");
        configureMessage(DOOR_CLOSED, "<FD><CE>CLOSED");
        configureMessage(APPROVED, "<FD><CL>APPROVED");
        configureMessage(DENIED, "<FD><CH>DENIED");
        configureMessage(SECURITY, "<FD><CC><SE>* SECURITY *<FO>");
        configureMessage(EMPTY, "<FD><CC> ");

        // create and register the serial data listener
        serial.addListener(new SerialDataListener() {
            @Override
            public void dataReceived(SerialDataEvent event) {
                // print out the data received to the console
                //System.out.print(event.getData());
            }
        });


        // ******************************************************************
        // INIT & START WEB SERVER  (using Jetty Web Server <embedded>)
        // ******************************************************************

        // create new jetty web server
        Server server = new Server(80);

        // create a resource handler to serve up the static html files
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setWelcomeFiles(new String[]{ "index.html" });
        resourceHandler.setResourceBase("html");

        // create a websocket handler to allow communication from the web pages
        WebSocketHandler websocketHandler = new WebSocketHandler()
        {
            @Override
            public void configure(WebSocketServletFactory factory)
            {
                factory.register(AccessControlWebSocket.class);
            }
        };

        // create a handler collection and include all the handlers
        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.setHandlers(new Handler[] {websocketHandler, resourceHandler, new DefaultHandler()});
        server.setHandler(handlerCollection);

        // start the jetty web server
        server.start();


        // ******************************************************************
        // DEFINE COMPONENT LISTENERS & EVENT LOGIC
        // ******************************************************************

        // create door sensor listener
        doorSensor.addListener(new SensorListener() {
            @Override
            public void onStateChange(SensorStateChangeEvent event) {

                // display console message
                Ansi message = Ansi.ansi().fg(Ansi.Color.WHITE).a("Door sensor event: ");
                if (event.getNewState() == SensorState.OPEN) {
                    message.fg(Ansi.Color.GREEN).a("--DOOR OPENED--");

                    // check for a security violation
                    // (this can occur if the lock solenoid is not currently engaged;
                    //  this may mean that the door was forcefully opened)
                    if (lockRelay.isOpen()) {
                        // set security violation to 'door-breach'
                        setSecurityViolation(SecurityViolation.DoorBreach);
                    }
                    else{
                        displayMessage(DOOR_OPENED);
                    }
                } else {
                    message.fg(Ansi.Color.YELLOW).a("--DOOR CLOSED--");
                    displayMessage(DOOR_CLOSED);
                }
                AnsiConsole.out().println(message.reset());
            }
        });

        // create doorbell switch listener
        doorbellSwitch.addListener(new SwitchListener() {
            public void onStateChange(SwitchStateChangeEvent event) {
                if (event.getNewState() == SwitchState.ON) {

                    // display console message
                    AnsiConsole.out().println(Ansi.ansi().fgBright(Ansi.Color.CYAN)
                            .a("DOORBELL event triggered").reset());

                    displayMessage(DOORBELL);
                }
            }
        });

        // create tamper switch listener
        tamperSwitch.addListener(new SwitchListener() {
            public void onStateChange(SwitchStateChangeEvent event) {
                // set security violation to 'tamper'
                setSecurityViolation(SecurityViolation.Tamper);
            }
        });

        // create a door lock solenoid relay listener to monitor lock/unlock events
        lockRelay.addListener(new RelayListener() {
            @Override
            public void onStateChange(RelayStateChangeEvent event) {
                // display console message
                Ansi message = Ansi.ansi().fg(Ansi.Color.WHITE).a("Door locking solenoid:    ");
                if (event.getNewState() == RelayState.CLOSED) {
                    message.fg(Ansi.Color.GREEN).a("--UNLOCKED--");
                } else {
                    message.fg(Ansi.Color.RED).a("--LOCKED--");
                }
                AnsiConsole.out().println(message.reset());
            }
        });

        // create keypad unlock sensor listener
        keypadUnlockSensor.addListener(new SensorListener() {
            @Override
            public void onStateChange(SensorStateChangeEvent event) {
                if (event.getNewState() == SensorState.CLOSED) {

                    // first check for any security violation;
                    // if a security violation exists, then we will not unlock the
                    // door until the security violation has been reset/restored
                    if (securityViolation != SecurityViolation.None) {

                        // display console message
                        AnsiConsole.out().println(Ansi.ansi().fg(Ansi.Color.WHITE)
                                .a("Unlock request received:  ")
                                .fgBright(Ansi.Color.RED)
                                .a("--ACCESS DENIED--").reset());

                        // display access denied message
                        displayMessage(DENIED);

                        AnsiConsole.out().println(Ansi.ansi().fgBright(Ansi.Color.YELLOW)
                                .a("A security violation has been detected; the door will ")
                                .a("not be unlocked until the violation is reset.")
                                .reset());

                        // send notification email to update access log
                        sendEmail(IFTTT_TAG_ACCESS_LOG, "ACCESS DENIED");
                    }

                    // if no security violation exists, then its safe to unlock the door
                    else {
                        // display console message
                        AnsiConsole.out().println(Ansi.ansi().fg(Ansi.Color.WHITE)
                                .a("Unlock request received:  ")
                                .fgBright(Ansi.Color.GREEN)
                                .a("--ACCESS GRANTED--").reset());

                        // display access approved message
                        displayMessage(APPROVED);

                        // determine if the door is open or shut
                        // (if the door is already open, then there is
                        //  no need to engage the lock solenoid relay)
                        if(doorSensor.isOpen()){
                            // display console message
                            AnsiConsole.out().println(Ansi.ansi().fg(Ansi.Color.YELLOW)
                                    .a("Unlock bypassed; the door is already open.").reset());
                        }
                        else{
                            // unlock the physical door by latching the
                            // solenoid relay for a few seconds
                            lockRelay.pulse(3000);
                        }

                        // send notification email to update access log
                        sendEmail(IFTTT_TAG_ACCESS_LOG, "ACCESS GRANTED");
                    }
                }
            }
        });

        // create override switch listener
        overrideSwitch.addListener(new SwitchListener() {
            public void onStateChange(SwitchStateChangeEvent event) {
                if (event.getNewState() == SwitchState.ON) {

                    // check for security violation
                    if (securityViolation != SecurityViolation.None) {

                        // display console message
                        AnsiConsole.out().println(Ansi.ansi().fgBright(Ansi.Color.WHITE)
                                .a("Override switch requesting --RESET--").reset());

                        // check for tamper switch security violations
                        if (tamperSwitch.isOn()) {
                            // display console message
                            AnsiConsole.out().println(Ansi.ansi().fgBright(Ansi.Color.YELLOW)
                                    .a("Unable to RESET security violation; ")
                                    .a("TAMPER switch is still reporting trouble.\n")
                                    .a("Tamper trouble must be resolved to reset.").reset());
                            return;
                        }

                        // check for drop open security violations
                        if (doorSensor.isOpen()) {
                            // display console message
                            AnsiConsole.out().println(Ansi.ansi().fgBright(Ansi.Color.YELLOW)
                                    .a("Unable to RESET security violation; DOOR is still open.\n")
                                    .a("Door must be closed to reset.").reset());
                            return;
                        }

                        // reset security violation
                        setSecurityViolation(SecurityViolation.Reset);
                    }
                    else {
                        AnsiConsole.out().println(Ansi.ansi().fg(Ansi.Color.WHITE)
                                .a("Override switch requesting --UNLOCK--").reset());
                        unlock();
                    }
                }
            }
        });

        // ******************************************************************
        // PROGRAM INIT LOGIC
        // ******************************************************************

        // display console message
        AnsiConsole.out().println(Ansi.ansi().fg(Ansi.Color.WHITE)
                .a("SYSTEM READY").reset());

        // post read message to LED reader sign
        serial.write("<ID01><RPB>\r\n");

        // check for tamper switch security violations
        if(tamperSwitch.isOn()){
            // set security violation to 'tamper'
            setSecurityViolation(SecurityViolation.Tamper);
        }

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

        // make sure the security LED is off
        securityLed.blink(0);
        securityLed.off();

        // shutdown jetty web server
        server.stop();

        // shutdown GPIO controller
        gpio.shutdown();

        // clear the display
        displayMessage(EMPTY);
        Thread.sleep(1000);

        // shutdown the serial controller
        serial.shutdown();
    }

    /**
     * Get the current security violation state
     *
     * @return SecurityViolation
     */
    public static SecurityViolation getSecurityViolation(){
        return securityViolation;
    }

    /**
     * Request to unlock door.
     *
     * @return 'true' if the door was successfully unlocked; otherwise 'false'
     */
    public static boolean unlock(){

        // check for security violation
        if (securityViolation == SecurityViolation.None) {
            AnsiConsole.out().println(Ansi.ansi().fg(Ansi.Color.WHITE)
                    .a("Requesting --UNLOCK--").reset());
            keypadOpenerRelay.pulse(500);
            return true;
        }
        return false;
    }

    /**
     * Display a pre-configured message (page) on the LED message reader board.
     *
     * @param messageId  The page identifier where the message is stored.
     */
    private static void displayMessage(String messageId)  {
        if(securityViolation == SecurityViolation.None ||
                messageId.equals(SECURITY)){
            serial.write("<ID01><RP" + messageId + ">\r\n");
        }
    }

    /**
     * Configure a display message for the LED message reader board.
     *
     * @param messageId  The page identifier where the message is stored.
     * @param message  The message text to be stored at the givin page location.
     * @throws InterruptedException
     */
    private static void configureMessage(String messageId, String message) throws InterruptedException {
        serial.write("<ID01><P" + messageId + ">" + message + "\r\n");
        Thread.sleep(250);
    }

    /**
     * Update the current system security violation status
     *
     * @param violation New security violation status
     */
    private static void setSecurityViolation(SecurityViolation violation){

        // ignore if same violation
        if(violation == securityViolation){
            return;
        }

        // determine if this is a violation RESET
        if(violation == SecurityViolation.Reset){

            // reset security violation tracking variable
            securityViolation = SecurityViolation.None;

            // display console message
            AnsiConsole.out().println(Ansi.ansi().fgBright(Ansi.Color.WHITE)
                    .a("The security violation has been ")
                    .fgBright(Ansi.Color.MAGENTA)
                    .a("--RESET--\n").reset()
                    .fg(Ansi.Color.WHITE)
                    .a("SYSTEM READY").reset());

            // stop security blinking LED
            securityLed.blink(0);
            securityLed.off();

            // send email notification to IFTTT
            sendEmail(IFTTT_TAG_SECURITY_ALERT,
                    "A security violation has been restored: " + violation.getName());

            // remove security message from LED message reader
            displayMessage(SYSTEM_READY);
        }
        else{
            // set security violation tracking variable
            securityViolation = violation;

            // display console message
            AnsiConsole.out().println(Ansi.ansi().fgBright(Ansi.Color.YELLOW)
                    .a("A security violation has been detected: ")
                    .fgBright(Ansi.Color.RED)
                    .a("--" + violation.getName() + "--").reset());

            AnsiConsole.out().println(Ansi.ansi().fgBright(Ansi.Color.RED)
                    .a("-----------------------------------------------\n")
                    .a("SECURITY VIOLATION :: " + violation.getName() + "\n")
                    .a("-----------------------------------------------").reset());

            // start blinking the security LED indicator light
            securityLed.blink(250);

            // send email notification to IFTTT
            sendEmail(IFTTT_TAG_SECURITY_ALERT,
                    "A security violation has been detected: " + violation.getName());

            // display the security violation on the LED message reader
            displayMessage(SECURITY);
        }
    }

    /**
     * Send email message for external notifications
     *
     * @param subject Email Subject Text
     * @param message Email Message Text
     */
    private static void sendEmail(String subject, String message){

        try{
            if(EMAIL_ENABLED && EMAIL_FROM != null && !EMAIL_FROM.isEmpty()){
                Email email = new SimpleEmail();
                email.setHostName(EMAIL_SERVER);
                email.setSmtpPort(EMAIL_PORT);
                email.setFrom(EMAIL_FROM);
                email.addTo(EMAIL_TO);
                email.setSubject(subject);
                email.setMsg(message);
                email.send();
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }
}

