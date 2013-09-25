/************************************************************************
 * ORGANIZATION  :  SavageHomeAutomation
 * PROJECT       :  Access Control using Pi4J & Raspberry Pi
 * FILENAME      :  AccessControlWebSocket.java
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
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;


@WebSocket
public class AccessControlWebSocket {

    @OnWebSocketMessage
    public void onText(Session session, String message) {
        try {
            // handle 'unlock' request
            if(message.equals("unlock")){
                session.getRemote().sendString(AccessControl.unlock() ? "unlock=success" : "unlock=fail");
            }

            // handle 'violation' request
            else if(message.equals("violation")){
                session.getRemote().sendString("violation=" + AccessControl.getSecurityViolation().getName());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}