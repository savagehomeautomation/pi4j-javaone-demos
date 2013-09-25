/************************************************************************
 * ORGANIZATION  :  SavageHomeAutomation
 * PROJECT       :  Access Control using Pi4J & Raspberry Pi
 * FILENAME      :  SecurityViolation.java
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
public enum SecurityViolation {

    None("NONE"),
    Reset("RESET"),
    Tamper("KEYPAD TAMPER"),
    DoorBreach("DOOR BREACH");

    private final String name;

    private SecurityViolation(final String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

    @Override
    public String toString(){
        return getName();
    }
}
