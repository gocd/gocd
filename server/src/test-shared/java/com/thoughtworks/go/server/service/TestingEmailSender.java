/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.server.messaging.SendEmailMessage;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

public class TestingEmailSender implements EmailSender {
    private String sentMessage = NO_MESSAGE;
    public static final String NO_MESSAGE = "No Message Sent";
    private List<SendEmailMessage> messages = new ArrayList<>();

    @Override
    public void sendEmail(SendEmailMessage message) {
        this.messages.add(message);
        this.sentMessage = message.toString();
    }

    public String getSentMessage() {
        return sentMessage;
    }

    public int size() {
        return messages.size();
    }

    public void clear() {
        messages.clear();
        sentMessage = NO_MESSAGE;
    }

    public SendEmailMessage firstMessage() {
        return messages.get(0);
    }

    public SendEmailMessage messageContainsSubject(String subject) {
        for (SendEmailMessage message : messages) {
            if (message.getSubject().contains(subject)) {
                return message;
            }
        }
        return null;
    }

    public void assertHasMessageContaining(String body) {
        for (SendEmailMessage message : messages) {
            if (message.toString().contains(body)) {
                return;
            }
        }
        fail(String.format("Expected to have a message with string '%s'. Actual messages are: '%s'", body, messages));
    }
}
