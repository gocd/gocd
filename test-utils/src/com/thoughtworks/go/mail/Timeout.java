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

package com.thoughtworks.go.mail;

import static org.junit.Assert.fail;

public class Timeout {
    private int time;
    private long startTime;
    private String message;

    public Timeout(String message, int time) {
        this.time = time;
        this.startTime = System.currentTimeMillis();
        this.message = message;
    }

    public boolean check() {
        boolean timeout = (System.currentTimeMillis() - startTime) > time;
        if (timeout) {
            fail(message + " timed out after " + (time/1000) + "seconds.");
        }
        return true;
    }
}
