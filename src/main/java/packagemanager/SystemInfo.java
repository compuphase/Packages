/*
 * @author Guido Daniel Wolff
 * Copyright 2021, 2022 CompuPhase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package packagemanager;

import java.net.NetworkInterface;
import java.net.InetAddress;

public class SystemInfo {

    public static String javaVersion() {
        return System.getProperty("java.version");
    }

    public static String javafxVersion() {
        return System.getProperty("javafx.version");
    }

    // Attempts to create a "machine id" number that uniquely identifies the
    // workstation. The function uses the MAC address, but mixes the 6 bytes
    // so that it fits in a 32-bit integer (and the sign bit is dropped).
    public static int machineId() {
        int id = 0;
        try{
            InetAddress localHost = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(localHost);
            byte[] hardwareAddress = ni.getHardwareAddress();
            for (int i = 0; i < hardwareAddress.length; i++) {
                id = ((id << 8) | ((id >> 24) & 0xff)) ^ hardwareAddress[i];
            }
        } catch(Exception e) {
            id = -1;
        }
        return id & 0x7fffffff;
    }
}