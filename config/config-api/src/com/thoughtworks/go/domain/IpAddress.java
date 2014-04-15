/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang.StringUtils;
import com.jpeterson.util.Unsigned;

/**
 * @understands the network address for a machine
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

    public String toString() {
        return address.getHostAddress();
    }

    public int compareTo(IpAddress other) {
        byte[] address1 = address.getAddress();
        byte[] address2 = other.address.getAddress();
        int compareLen = address1.length - address2.length;
        if(compareLen!=0) return compareLen;
        for (int i = 0; i < address1.length; i++) {
            long compareByte = Unsigned.unsigned(address1[i]) - Unsigned.unsigned(address2[i]);
            if(compareByte == 0) continue;
            return compareByte > 0 ? 1 : -1; 
        }
        return 0;
    }


    private static class NullIpAddress extends IpAddress {
        public NullIpAddress() throws UnknownHostException {
            super(InetAddress.getByName("0.0.0.0"));
        }
        public String toString() {
            return "";
        }
        public int compareTo(IpAddress other) {
            return -1;
        }
    }
}
