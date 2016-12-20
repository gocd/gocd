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


public class BuildTestCase implements Serializable {
    private String name;
    private String duration;
    private String classname;
    private String message;
    private BuildTestCaseResult result;

    public BuildTestCase(String name, String duration, String classname, String message, String messageBody,
                         BuildTestCaseResult result) {
        this.name = name;
        this.duration = duration;
        this.classname = classname;
        this.messageBody = messageBody;
        this.message = message;
        this.result = result;
    }

    private String messageBody;

    public String getName() {
        return name;
    }

    public String getDuration() {
        return duration;
    }

    public String getClassname() {
        return classname;
    }

    public boolean didError() {
        return true;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public String getMessage() {
        return message;
    }

    public BuildTestCaseResult getResult() {
        return result;
    }
}
