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

package com.thoughtworks.go.server.messaging;

import java.util.ArrayList;

public class InMemoryEmailNotificationTopic extends EmailNotificationTopic {
    private ArrayList<SendEmailMessage> messages = new ArrayList<>();

    public InMemoryEmailNotificationTopic() {
        super(null);
    }

    public void post(SendEmailMessage message) {
        messages.add(message);
    }

    public String getSubject(String toAddress) {
        for (SendEmailMessage message : messages) {
            if (message.getTo().equals(toAddress)) {
                return message.getSubject();
            }
        }
        return null;
    }

    public String getBody(String toAddress) {
        for (SendEmailMessage message : messages) {
            if (message.getTo().equals(toAddress)) {
                return message.getBody();
            }
        }
        return null;
    }

    public int emailCount(String toAddress) {
        int cnt = 0;
        for (SendEmailMessage message : messages) {
            if (message.getTo().equals(toAddress)) {
                cnt++;
            }
        }
        return cnt;
    }

    public void reset() {
        messages.clear();
    }
}

