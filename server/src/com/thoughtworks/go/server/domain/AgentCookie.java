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

import com.thoughtworks.go.domain.PersistentObject;

/**
 * @understands agent uuid to cookie mapping
 */
public class AgentCookie extends PersistentObject {
    private String uuid;
    private String cookie;

    public AgentCookie(String uuid, String cookie) {
        this.uuid = uuid;
        this.cookie = cookie;
    }

    @Deprecated //for hibernate
    private AgentCookie() {
    }

    public String getCookie() {
        return cookie;
    }

    public void updateCookie(String cookie) {
        this.cookie = cookie;
    }
}
