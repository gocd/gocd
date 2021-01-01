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

import com.thoughtworks.go.server.messaging.EmailNotificationTopic;
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class UniqueEmailSenderTest {

    @Test
    public void shouldSendEmailIfNotEnoughSpaceForFirstTime() {
        final EmailNotificationTopic topic = mock(EmailNotificationTopic.class);
        final SendEmailMessage message = new SendEmailMessage("pavan", "hu kai", "someone");

        EmailSender sender = new AsynchronousEmailSender(topic);
        sender.sendEmail(message);
        verify(topic).post(message);
    }

    @Test
    public void shouldBeAbleToSend2Emails() {
        final EmailNotificationTopic topic = mock(EmailNotificationTopic.class);
        final SendEmailMessage message = new SendEmailMessage("pavan", "hu kai", "someone");

        EmailSender sender = new AsynchronousEmailSender(topic);
        sender.sendEmail(message);
        sender.sendEmail(message);
        verify(topic, times(2)).post(message);
    }

}
