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

public class BuildMessage implements Serializable {
    private String message;

    private MessageLevel buildLevel;

    public BuildMessage(String message, MessageLevel buildLevel) {
        this.buildLevel = buildLevel;
        this.message = message.trim();
    }

    public String getMessage() {
        return message;
    }

    public String toString() {
        return "<BuildMessage " + buildLevel.getDisplayName() + ": " + message;
    }

    public MessageLevel getLevel() {
        return buildLevel;
    }
}
