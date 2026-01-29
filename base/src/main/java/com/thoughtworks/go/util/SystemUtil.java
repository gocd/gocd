/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;

public class SystemUtil {
    private static final Logger LOG = LoggerFactory.getLogger(SystemUtil.class);
    private static final List<NetworkInterface> localInterfaces;
    private static String hostName;

    static {
        //This must be done only once
        //This method call is extremely slow on JDK 1.7 + Windows combination
        try {
            localInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch (SocketException e) {
            throw new RuntimeException("Could not retrieve local network interfaces", e);
        }
    }

    public static String getLocalhostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized String getLocalhostNameOrRandomNameIfNotFound() {
        if (hostName != null) {
            return hostName;
        }
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostName = "unknown-host-" + Math.abs(new Random(System.currentTimeMillis()).nextInt()) % 1000;
            LOG.warn("Unable to resolve hostname: {}. Using: {}", e.getMessage(), hostName);
        }
        return hostName;
    }

    public static String getFirstLocalNonLoopbackIpAddress() {
        SortedSet<String> addresses = new TreeSet<>();
        for (NetworkInterface networkInterface : localInterfaces) {
            Enumeration<InetAddress> inetAddressEnumeration = networkInterface.getInetAddresses();
            while (inetAddressEnumeration.hasMoreElements()) {
                InetAddress address = inetAddressEnumeration.nextElement();
                if (!address.isLoopbackAddress() && !address.getHostAddress().contains(":")) {
                    addresses.add(address.getHostAddress());
                }
            }
        }
        if (addresses.isEmpty()) {
            throw new RuntimeException("Failed to get non-loopback local ip address!");
        }
        return addresses.first();
    }

    public static boolean isLocalIpAddress(String ipAddress) {
        try {
            InetAddress[] allAddresses = InetAddress.getAllByName(ipAddress);
            boolean isLocal = false;
            for (InetAddress address : allAddresses) {
                isLocal = isLocal || address.isLoopbackAddress();
            }
            return isLocal || isLocalhostWithNonLoopbackIpAddress(ipAddress);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    static boolean isLocalhost(String hostname, String ipAddress) {
        return isLocalhostWithLoopbackIpAddress(hostname, ipAddress) || isLocalhostWithNonLoopbackIpAddress(ipAddress);
    }

    public static boolean isLocalhost(String ipAddress) {
        return isLocalhost(ipAddress, ipAddress);
    }

    private static boolean isLocalhostWithNonLoopbackIpAddress(String ipAddress) {
        for (NetworkInterface networkInterface : localInterfaces) {
            Enumeration<InetAddress> inetAddressEnumeration = networkInterface.getInetAddresses();
            while (inetAddressEnumeration.hasMoreElements()) {
                InetAddress address = inetAddressEnumeration.nextElement();
                if (!address.isLoopbackAddress() && ipAddress.equalsIgnoreCase(address.getHostAddress())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isLocalhostWithLoopbackIpAddress(String forAddress, final String ipAddress) {
        InetAddress[] allMatchingAddresses;
        try {
            allMatchingAddresses = InetAddress.getAllByName(forAddress);
        } catch (UnknownHostException e) {
            return false;
        }
        for (InetAddress inetAddress : allMatchingAddresses) {
            if (inetAddress.isLoopbackAddress() && inetAddress.getHostAddress().equals(ipAddress)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLocalhostReachable(int port) {
        return reachable(null, port);
    }

    @SuppressWarnings("try")
    public static boolean reachable(String name, int port) {
        try (Socket ignored = new Socket(InetAddress.getByName(name), port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String currentWorkingDirectory() {
        String location;
        File file = new File(".");
        try {
            location = file.getCanonicalPath();
        } catch (IOException e) {
            location = file.getAbsolutePath();
        }
        return location;

    }

    public static int getIntProperty(String propertyName, int defaultValue) {
        try {
            return Integer.parseInt(System.getProperty(propertyName));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getClientIp(String serviceUrl) {
        try {
            URL url = URI.create(serviceUrl).toURL();
            try (Socket socket = new Socket(url.getHost(), url.getPort() == -1 ? url.getDefaultPort() : url.getPort())) {
                return socket.getLocalAddress().getHostAddress();
            }
        } catch (Exception e) {
            return SystemUtil.getFirstLocalNonLoopbackIpAddress();
        }
    }
}
