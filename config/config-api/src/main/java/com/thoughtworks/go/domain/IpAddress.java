/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.domain;

import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Understands the network address for a machine
 */
public class IpAddress implements Comparable<IpAddress> {
    private InetAddress address;

    public IpAddress(InetAddress address) {
        this.address = address;
    }

    public static IpAddress create(String address) {
        try {
            if (StringUtils.isEmpty(address)) {
                return new NullIpAddress();
            }
            return new IpAddress(InetAddress.getByName(address));
        } catch (UnknownHostException e) {
            throw new RuntimeException("IpAddress '" + address + "' is not a valid IP address");
        }
    }

    @Override
    public String toString() {
        return address.getHostAddress();
    }

    @Override
    public int compareTo(IpAddress other) {
        byte[] myAddressInBytes = address.getAddress();
        byte[] otherAddressInBytes = other.address.getAddress();

        // general ordering: ipv4 before ipv6
        if (myAddressInBytes.length < otherAddressInBytes.length) return -1;
        if (myAddressInBytes.length > otherAddressInBytes.length) return 1;

        // we have 2 ips of the same type, so we have to compare each byte
        for (int i = 0; i < myAddressInBytes.length; i++) {
            int b1 = unsignedByteToInt(myAddressInBytes[i]);
            int b2 = unsignedByteToInt(otherAddressInBytes[i]);
            if (b1 == b2)
                continue;
            if (b1 < b2)
                return -1;
            else
                return 1;
        }
        return 0;
    }


    private int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }

    private static class NullIpAddress extends IpAddress {
        public NullIpAddress() throws UnknownHostException {
            super(InetAddress.getByName("0.0.0.0"));
        }

        @Override
        public String toString() {
            return "";
        }


        @SuppressWarnings("ComparatorMethodParameterNotUsed")
        @Override
        public int compareTo(IpAddress other) {
            return other instanceof NullIpAddress ? 0 : -1;
        }
    }
}
