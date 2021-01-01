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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BackgroundEmailSenderTest {

    GoMailSender neverReturns = new GoMailSender() {
        @Override
        public ValidationBean send(String subject, String body, String to) {
            try {
                Thread.sleep(10000000);
            } catch (InterruptedException e) {
            }
            return null;
        }

        @Override
        public ValidationBean send(SendEmailMessage message) {
            return send(message.getSubject(), message.getBody(), message.getTo());
        }

    };
    private GoMailSender sender;

    @Before
    public void setUp() {
        sender = mock(GoMailSender.class);
    }

    @Test
    public void shouldReturnNotValidIfSendingTimesout() {
        BackgroundMailSender background = new BackgroundMailSender(neverReturns, 1);
        ValidationBean validationBean = background.send("Subject", "body", "to@someone");
        assertThat(validationBean.isValid(), is(false));
        assertThat(validationBean.getError(), containsString("Failed to send an email. Please check the GoCD server logs for any extra information that might be present."));
    }

    @Test
    public void shouldReturnWhateverTheOtherSenderReturnsIfSendingDoesNotTimeout() {
        final ValidationBean validationBean = ValidationBean.valid();
        when(sender.send("Subject", "body", "to@someone")).thenReturn(validationBean);
        BackgroundMailSender background = new BackgroundMailSender(sender, 1000);
        ValidationBean returned = background.send("Subject", "body", "to@someone");
        assertThat(returned, is(sameInstance(validationBean)));
    }


}
