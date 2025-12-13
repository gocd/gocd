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
package com.thoughtworks.go.remote;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.Objects;

public class AgentIdentifier implements Serializable {
    @Expose
    private String hostName;
    @Expose
    private String ipAddress;
    @Expose
    private String uuid;

    public AgentIdentifier(String hostName, String ipAddress, String uuid) {
        this.hostName = hostName;
        this.ipAddress = ipAddress;
        this.uuid = uuid;
    }

    public String getHostName() {
        return hostName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return String.format("Agent [%s, %s, %s]", hostName, ipAddress, uuid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AgentIdentifier that = (AgentIdentifier) o;

        if (!Objects.equals(hostName, that.hostName)) {
            return false;
        }
        if (!Objects.equals(ipAddress, that.ipAddress)) {
            return false;
        }
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        int result = hostName != null ? hostName.hashCode() : 0;
        result = 31 * result + (ipAddress != null ? ipAddress.hashCode() : 0);
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        return result;
    }

}
