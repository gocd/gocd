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

package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.domain.PersistentObject;

public class Agent extends PersistentObject {
    private String uuid;
    private String cookie;
    private String hostname;
    private String ipaddress;

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setIpaddress(String ipaddress) {
        this.ipaddress = ipaddress;
    }

    public Agent(String uuid, String cookie, String hostname, String ipaddress) {
        this.uuid = uuid;
        this.cookie = cookie;
        this.hostname = hostname;
        this.ipaddress = ipaddress;
    }

    public static Agent blankAgent(String uuid) {
        return new Agent(uuid, "Unknown", "Unknown", "Unknown");
    }

    public static Agent fromConfig(AgentConfig config) {
        return new Agent(config.getUuid(), "None", config.getHostNameForDisplay(), config.getIpAddress());
    }

    @Deprecated //for hibernate
    private Agent() {
    }

    public String getHostname() {
        return hostname;
    }

    public String getUuid() {
        return uuid;
    }

    public String getCookie() {
        return cookie;
    }

    public String getIpaddress() {
        return ipaddress;
    }

    public void update(String cookie, String hostname, String ipaddress) {
        this.cookie = cookie;
        this.hostname = hostname;
        this.ipaddress = ipaddress;
    }

}
