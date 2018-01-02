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

import java.io.Serializable;

public class MessageLevel implements Serializable {
    public static final MessageLevel INFO = new MessageLevel("info");
    public static final MessageLevel WARN = new MessageLevel("warn");
    public static final MessageLevel ERROR = new MessageLevel("error");
    public static final MessageLevel DEBUG = new MessageLevel("debug");
    public static final MessageLevel UNKNOWN = new MessageLevel("unknown");

    private String displayName;

    public MessageLevel(String displayName) {
        this.displayName = displayName;
    }

    public static MessageLevel getLevelForPriority(String displayName) {
        if ("debug".equalsIgnoreCase(displayName)) {
            return DEBUG;
        } else if ("info".equalsIgnoreCase(displayName)) {
            return INFO;
        } else if ("warn".equalsIgnoreCase(displayName)) {
            return WARN;
        } else if ("error".equalsIgnoreCase(displayName)) {
            return ERROR;
        } else {
            return UNKNOWN;
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public String toString() {
        return getDisplayName();
    }
}
